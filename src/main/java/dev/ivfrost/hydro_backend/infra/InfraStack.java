package dev.ivfrost.hydro_backend.infra;

import java.util.List;
import java.util.Map;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.IpAddresses;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.DeploymentCircuitBreaker;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.HealthCheck;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.elasticache.CfnServerlessCache;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.rds.StorageType;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

public class InfraStack extends Stack {

  public InfraStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    final String BUCKET_NAME = "hydrolink-storage";
    final String USER_POOL_ID = "eu-west-1_qJxtdtpP9";
    final String USER_POOL_CLIENT_ID = "2i6g183l48dtpiamlsih0tcahl";
    final String IDENTITY_POOL_ID = "eu-west-1:45bcf106-35e7-4651-97c8-d97b07c5a108";
    final String CERT_ARN =
        "arn:aws:acm:eu-west-1:468241617065:certificate/c0a2efe1-e73c-4f6c-aa46-455c4f07c030";
    // Image tag from CDK context: `cdk deploy -c imageTag=$(git rev-parse --short HEAD)`.
    // Required on purpose: without a changing tag, an unchanged task definition means
    // `cdk deploy` will not roll out a newly pushed image (the stale-:latest trap), and a
    // silent `latest` fallback would hide the mistake. Fail the synth instead.
    final String IMAGE_TAG = (String) this.getNode().tryGetContext("imageTag");
    if (IMAGE_TAG == null || IMAGE_TAG.isBlank()) {
      throw new IllegalArgumentException(
          "Missing required CDK context 'imageTag'. Deploy with "
              + "-c imageTag=$(git rev-parse --short HEAD) so each push rolls out a new revision.");
    }

    IBucket storageBucket = Bucket.fromBucketName(this, "HydrolinkStorage", BUCKET_NAME);
    Queue deviceStatusDLQ = Queue.Builder.create(this, "DeviceStatusDLQ")
        .queueName("hydrolink-device-status-dlq")
        .retentionPeriod(Duration.days(14))
        .removalPolicy(RemovalPolicy.DESTROY)
        .build();

    Queue deviceStatusQueue = Queue.Builder.create(this, "HydrolinkDeviceStatusQueue")
        .queueName("hydrolink-device-status-queue")
        .visibilityTimeout(Duration.seconds(30))
        .deadLetterQueue(
            DeadLetterQueue.builder().queue(deviceStatusDLQ).maxReceiveCount(5).build())
        .removalPolicy(RemovalPolicy.DESTROY)
        .build();

    Vpc vpc = Vpc.Builder.create(this, "HydrolinkVPC")
        .vpcName("hydrolink-vpc")
        .ipAddresses(IpAddresses.cidr("10.0.0.0/16"))
        .maxAzs(2)
        .natGateways(1)
        .subnetConfiguration(List.of(
            SubnetConfiguration.builder()
                .name("public").subnetType(SubnetType.PUBLIC).cidrMask(24).build(),
            SubnetConfiguration.builder()
                .name("private").subnetType(SubnetType.PRIVATE_WITH_EGRESS).cidrMask(24).build()
        ))
        .enableDnsHostnames(true)
        .enableDnsSupport(true)
        .build();

    SubnetSelection privateSubnets =
        SubnetSelection.builder()
            .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
            .build();

    DatabaseInstance db = DatabaseInstance.Builder.create(this, "HydroLinkDb")
        .instanceIdentifier("hydrolink-db")
        .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
            .version(PostgresEngineVersion.VER_16).build()))
        .instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.MICRO))
        // Secret fully managed by RDS
        .databaseName("hydrodb")
        .credentials(Credentials.fromGeneratedSecret("hydro"))
        .allocatedStorage(20)
        .storageType(StorageType.GP3)
        .storageEncrypted(true)
        .backupRetention(Duration.days(7))
        .multiAz(false)
        .publiclyAccessible(false)
        .vpc(vpc)
        .vpcSubnets(privateSubnets)
        .removalPolicy(RemovalPolicy.DESTROY)
        .build();

    // Serverless ElastiCache only has L1 construct, requiring manual SG creation
    SecurityGroup cacheSg = SecurityGroup.Builder.create(this, "CacheSg")
        .vpc(vpc)
        .description("Security group for ElastiCache serverless cache")
        .allowAllOutbound(true)
        .build();

    CfnServerlessCache cache = CfnServerlessCache.Builder.create(this, "HydrolinkCache")
        .serverlessCacheName("hydrolink-cache")
        .engine("redis")
        .majorEngineVersion("7")
        .subnetIds(vpc.selectSubnets(privateSubnets).getSubnetIds())
        .securityGroupIds(List.of(cacheSg.getSecurityGroupId()))
        .build();

    SecurityGroup apiSg = SecurityGroup.Builder.create(this, "ApiSg")
        .vpc(vpc)
        .description("Security group for hydrolink API")
        .allowAllOutbound(true)
        .build();

    // API reaches the cache on the Redis port, and the database on its own port.
    cacheSg.addIngressRule(apiSg, Port.tcp(6379), "Redis from API");
    db.getConnections().allowDefaultPortFrom(apiSg, "Postgres from API");

    Role executionRole = Role.Builder.create(this, "HydroLinkTaskExecutionRole")
        .roleName("hydrolink-api-exec")
        .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build())
        // Attach the AWS-managed ECS task execution policy
        .managedPolicies(List.of(
            ManagedPolicy.fromAwsManagedPolicyName(
                "service-role/AmazonECSTaskExecutionRolePolicy")
        ))
        // Add the inline secrets-read policy
        .inlinePolicies(Map.of(
            "hydrolink-secrets-read",
            PolicyDocument.Builder.create()
                .statements(List.of(
                    PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(List.of("secretsmanager:GetSecretValue"))
                        .resources(List.of(
                            "arn:aws:secretsmanager:" + this.getRegion() + ":" + this.getAccount()
                                + ":secret:hydrolink/*"
                        ))
                        .build()
                ))
                .build()
        ))
        .build();

    Role taskRole = Role.Builder.create(this, "HydroLinkTaskRole")
        .roleName("hydrolink-api-task")
        .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build())
        .inlinePolicies(Map.of(
            "hydrolink-s3-read-write",
            PolicyDocument.Builder.create()
                .statements(List.of(
                    PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(List.of("s3:GetObject", "s3:PutObject", "s3:ListBucket"))
                        .resources(List.of(
                            storageBucket.getBucketArn(),
                            storageBucket.getBucketArn() + "/*"
                        ))
                        .build()
                ))
                .build(),
            "hydrolink-sqs-read",
            PolicyDocument.Builder.create()
                .statements(List.of(
                    PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(
                            List.of("sqs:ReceiveMessage", "sqs:DeleteMessage",
                                "sqs:GetQueueAttributes", "sqs:GetQueueUrl"))
                        .resources(
                            List.of(deviceStatusQueue.getQueueArn()))
                        .build()
                ))
                .build(),
            "hydrolink-iot-publish",
            PolicyDocument.Builder.create()
                .statements(List.of(
                    PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(List.of("iot:Publish"))
                        .resources(List.of(
                            "arn:aws:iot:" + this.getRegion() + ":" + this.getAccount()
                                + ":topic/hydro/*/command",
                            "arn:aws:iot:" + this.getRegion() + ":" + this.getAccount()
                                + ":topic/hydro/*/announce"
                        ))
                        .build()
                ))
                .build(),
            "hydrolink-cognito-admin",
            PolicyDocument.Builder.create()
                .statements(List.of(
                    PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(List.of("cognito-idp:AdminGetUser", "cognito-idp:AdminCreateUser",
                            "cognito-idp:AdminSetUserPassword",
                            "cognito-idp:AdminUpdateUserAttributes"))
                        .resources(List.of(
                            "arn:aws:cognito-idp:" + this.getRegion() + ":" + this.getAccount()
                                + ":userpool/" + USER_POOL_ID
                        ))
                        .build()
                ))
                .build()
        ))
        .build();

    FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
        .create(this, "HydroLinkTaskDef")
        .family("hydrolink-api")
        .cpu(512)
        .memoryLimitMiB(1024)
        .executionRole(executionRole)
        .taskRole(taskRole)
        .build();

    IRepository repo = Repository.fromRepositoryName(this, "HydroLinkRepo", "hydrolink-api");

    Secret deviceKeySecret = Secret.Builder.create(this, "DeviceKeySecret")
        .secretName("hydrolink/device-key-secret")
        .generateSecretString(SecretStringGenerator.builder().passwordLength(32).build())
        .build();

    Secret deviceProvisioningSecret = Secret.Builder.create(this, "DeviceProvisioningSecret")
        .secretName("hydrolink/device-provisioning-secret")
        .generateSecretString(SecretStringGenerator.builder().passwordLength(32).build())
        .build();

    taskDefinition.addContainer("Api", ContainerDefinitionOptions.builder()
        .containerName("api")
        .image(ContainerImage.fromEcrRepository(repo, IMAGE_TAG))
        .portMappings(List.of(PortMapping.builder()
            .containerPort(8080)
            .protocol(Protocol.TCP)
            .build()))
        .environment(Map.ofEntries(
            Map.entry("SPRING_PROFILES_ACTIVE", "prod"),
            Map.entry("AWS_REGION", this.getRegion()),
            Map.entry("AWS_USER_POOL_ID", USER_POOL_ID),
            Map.entry("AWS_USER_POOL_CLIENT_ID", USER_POOL_CLIENT_ID),
            Map.entry("AWS_ACCOUNT_ID", this.getAccount()),
            Map.entry("AWS_IDENTITY_POOL_ID", IDENTITY_POOL_ID),
            Map.entry("DB_URL",
                "jdbc:postgresql://" + db.getDbInstanceEndpointAddress()
                    + ":" + db.getDbInstanceEndpointPort() + "/hydrodb"),
            Map.entry("DB_USERNAME", "hydro"),
            Map.entry("REDIS_HOST", cache.getAttrEndpointAddress()),
            Map.entry("REDIS_PORT", "6379"),
            Map.entry("REDIS_SSL_ENABLED", "true"),
            Map.entry("MINIO_BUCKET_REGION", this.getRegion()),
            Map.entry("MINIO_BUCKET_NAME", BUCKET_NAME),
            Map.entry("DEVICE_STATUS_QUEUE", deviceStatusQueue.getQueueName()),
            Map.entry("MINIO_URL", "https://s3.eu-west-1.amazonaws.com"),
            Map.entry("MINIO_EXT_URL",
                "https://hydrolink-storage.s3.eu-west-1.amazonaws.com"),
            Map.entry("CORS_ALLOWED_ORIGINS", ""),
            Map.entry("SPRINGDOC_API_DOCS_ENABLED", "false"),
            Map.entry("SPRINGDOC_SWAGGER_UI_ENABLED", "false")
        ))
        .secrets(Map.of(
            "DB_PASSWORD",
            software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(db.getSecret(),
                "password"),
            "DEVICE_KEY_SECRET",
            software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(deviceKeySecret),
            "DEVICE_PROVISIONING_SECRET",
            software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(deviceProvisioningSecret)
        ))
        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
            .streamPrefix("api")
            .logGroup(LogGroup.Builder.create(this, "ApiLogGroup")
                .logGroupName("/ecs/hydrolink-api")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build())
            .build()))
        .healthCheck(HealthCheck.builder()
            .command(List.of("CMD-SHELL",
                "wget -qO- http://localhost:8080/actuator/health/liveness || exit 1"))
            .interval(Duration.seconds(30))
            .timeout(Duration.seconds(5))
            .retries(3)
            .startPeriod(Duration.seconds(90))
            .build())
        .build());

    Cluster cluster = Cluster.Builder.create(this, "HydroLinkCluster")
        .vpc(vpc)
        .clusterName("hydrolink-cluster")
        .build();

    ICertificate cert = Certificate.fromCertificateArn(this, "HydroLinkCert", CERT_ARN);

    ApplicationLoadBalancedFargateService service =
        ApplicationLoadBalancedFargateService.Builder.create(this, "HydroLinkService")
            .cluster(cluster)
            .taskDefinition(taskDefinition)
            .desiredCount(1)
            .publicLoadBalancer(true)
            .certificate(cert)
            .redirectHttp(true)
            .taskSubnets(privateSubnets)
            .securityGroups(List.of(apiSg))
            .circuitBreaker(DeploymentCircuitBreaker.builder()
                .rollback(true)
                .build())
            .build();

    // The ECS pattern only derives the container port for the target group; it leaves the
    // ALB health check on the AWS default of "/", which the app does not serve. Point it at
    // the actuator liveness probe so targets become healthy.
    service.getTargetGroup().configureHealthCheck(
        software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
            .path("/actuator/health/liveness")
            .port("8080")
            .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.HTTP)
            .healthyThresholdCount(2)
            .unhealthyThresholdCount(3)
            .interval(Duration.seconds(30))
            .timeout(Duration.seconds(5))
            .build());

    CfnOutput.Builder.create(this, "AlbUrl")
        .value("https://" + service.getLoadBalancer().getLoadBalancerDnsName())
        .description("Public URL of the hydrolink API")
        .build();
  }
}
