package dev.ivfrost.hydro_backend.devices;

/**
 * Published inside the provisioning transaction. An after-commit listener creates the
 * AWS IoT policy, so provisioning does not wait on AWS.
 */
public record DeviceProvisionedEvent(String deviceKey) {

}
