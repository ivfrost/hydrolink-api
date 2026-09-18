package dev.ivfrost.hydro_backend.devices.internal;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AttachPolicyRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.DeletePolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.DetachPolicyRequest;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsResponse;
import software.amazon.awssdk.services.iot.model.PolicyVersion;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceControlPlaneService {

  private static final String POLICY_NAME = "hydro-device-%s-policy";
  private static final int MAX_POLICY_VERSIONS = 5;
  private final IotClient iotClient;
  @Value("${spring.cloud.aws.region.static}") String region;
  @Value("${aws.account.id}") String accountId;

  public void createDevicePolicy(String deviceKey) {
    String policyName = POLICY_NAME.formatted(deviceKey);
    String policyDocument = policyDocument(deviceKey);

    try {
      iotClient.createPolicy(CreatePolicyRequest.builder()
          .policyName(policyName)
          .policyDocument(policyDocument)
          .build());
    } catch (ResourceAlreadyExistsException _) {
      // Re-provisioning the same device key: publish the current document as a new
      // default version so an older policy on an already-provisioned device is updated
      // rather than frozen at whatever it was when the device first appeared.
      publishPolicyVersion(policyName, policyDocument);
    }
  }

  /** Builds the read-only policy for one device: connect as the identity, subscribe to its topics. */
  private String policyDocument(String deviceKey) {
    return """
      {
        "Version": "2012-10-17",
        "Statement": [
          {
            "Sid": "ConnectOwnIdentity",
            "Effect": "Allow",
            "Action": ["iot:Connect"],
            "Resource": "arn:aws:iot:%s:%s:client/${cognito-identity.amazonaws.com:sub}:*"
          },
          {
            "Sid": "SubOwnedDevice",
            "Effect": "Allow",
            "Action": ["iot:Subscribe", "iot:Receive"],
            "Resource": [
              "arn:aws:iot:%s:%s:topicfilter/hydro/%s/*",
              "arn:aws:iot:%s:%s:topic/hydro/%s/*"
            ]
          }
        ]
      }
      """.formatted(region, accountId, region, accountId, deviceKey, region, accountId, deviceKey);
  }

  /**
   * Publishes the document as a new default version, pruning the oldest non-default
   * version first because AWS IoT keeps at most five versions per policy.
   */
  private void publishPolicyVersion(String policyName, String policyDocument) {
    ListPolicyVersionsResponse versions = iotClient.listPolicyVersions(
        ListPolicyVersionsRequest.builder().policyName(policyName).build());

    List<PolicyVersion> oldVersions = versions.policyVersions().stream()
        .filter(version -> !Boolean.TRUE.equals(version.isDefaultVersion()))
        .sorted(Comparator.comparing(PolicyVersion::createDate))
        .toList();

    if (oldVersions.size() >= MAX_POLICY_VERSIONS - 1) {
      iotClient.deletePolicyVersion(DeletePolicyVersionRequest.builder()
          .policyName(policyName)
          .policyVersionId(oldVersions.getFirst().versionId())
          .build());
    }

    String versionId = iotClient.createPolicyVersion(CreatePolicyVersionRequest.builder()
        .policyName(policyName)
        .policyDocument(policyDocument)
        .setAsDefault(true)
        .build()).policyVersionId();

    log.info("Published policy {} version {} as default", policyName, versionId);
  }

  /**
   * Creates the device policy if missing, then attaches it to the identity. Idempotent:
   * safe to call on link even when the policy was already created at provision time, and
   * it covers rows that never went through provisioning (for example seeded devices).
   */
  public void ensureAndAttachDevicePolicy(String deviceKey, String identityId) {
    createDevicePolicy(deviceKey);
    attachDevicePolicy(deviceKey, identityId);
  }

  public void attachDevicePolicy(String deviceKey, String identityId) {
    String policyName = POLICY_NAME.formatted(deviceKey);
    iotClient.attachPolicy(AttachPolicyRequest.builder()
        .policyName(policyName)
        .target(cognitoTarget(identityId))
        .build());
  }

  public void detachDevicePolicy(String deviceKey, String identityId) {
    String policyName = POLICY_NAME.formatted(deviceKey);
    try {
      iotClient.detachPolicy(DetachPolicyRequest.builder()
          .policyName(policyName)
          .target(cognitoTarget(identityId))
          .build());
    } catch (ResourceNotFoundException e) {
      // The policy or the attachment is already gone. Treat as success so an unlink
      // can converge: without this, a device that was linked but never successfully
      // attached (a post-commit attach failure) could never be unlinked.
      log.info("IoT policy {} already absent for identity {}; treating detach as done",
          policyName, identityId);
    }
  }

  /**
   * IoT policy targets for a Cognito identity must be qualified as
   * {@code <region>:<identityId>}. A bare identity id is rejected as an invalid target.
   * If the value already carries the region prefix, it is returned unchanged.
   */
  private String cognitoTarget(String identityId) {
    if (identityId == null || identityId.isBlank()) {
      throw new IllegalArgumentException("Cognito identity id is required to target an IoT policy");
    }
    return identityId.contains(":") ? identityId : region + ":" + identityId;
  }
}
