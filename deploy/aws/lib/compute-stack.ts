import * as path from "node:path";
import {
  CfnOutput,
  CfnParameter,
  Duration,
  RemovalPolicy,
  Stack,
  type StackProps,
} from "aws-cdk-lib";
import { DockerImageAsset, Platform } from "aws-cdk-lib/aws-ecr-assets";
import {
  InstanceType,
  MachineImage,
  SubnetType,
  UserData,
  type ISecurityGroup,
  type IVpc,
} from "aws-cdk-lib/aws-ec2";
import {
  Effect,
  ManagedPolicy,
  PolicyStatement,
  Role,
  ServicePrincipal,
} from "aws-cdk-lib/aws-iam";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import {
  ApplicationLoadBalancer,
  ApplicationProtocol,
  ApplicationTargetGroup,
  ListenerAction,
  TargetType,
} from "aws-cdk-lib/aws-elasticloadbalancingv2";
import {
  AdditionalHealthCheckType,
  AutoScalingGroup,
  HealthChecks,
} from "aws-cdk-lib/aws-autoscaling";
import type { Construct } from "constructs";
import { deploymentConfig } from "./config";
import type { DataStack } from "./data-stack";

export interface ComputeStackProps extends StackProps {
  readonly vpc: IVpc;
  readonly loadBalancerSecurityGroup: ISecurityGroup;
  readonly applicationSecurityGroup: ISecurityGroup;
  readonly data: DataStack;
}

export class ComputeStack extends Stack {
  readonly loadBalancer: ApplicationLoadBalancer;
  readonly autoScalingGroup: AutoScalingGroup;
  readonly applicationLogGroup: LogGroup;

  constructor(scope: Construct, id: string, props: ComputeStackProps) {
    super(scope, id, props);

    this.applicationLogGroup = new LogGroup(this, "ApplicationLogs", {
      logGroupName: "/ecommerce/application",
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });

    const role = new Role(this, "ApplicationRole", {
      assumedBy: new ServicePrincipal("ec2.amazonaws.com"),
      managedPolicies: [
        ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryReadOnly"),
      ],
    });
    role.addToPolicy(
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ["ec2:DescribeInstances", "autoscaling:DescribeAutoScalingInstances"],
        resources: ["*"],
      }),
    );
    props.data.databaseSecret.grantRead(role);
    this.applicationLogGroup.grantWrite(role);

    const backendImage = this.createBackendImage(role);
    const asgName = "ecommerce-applications";
    const userData = this.createUserData(props, backendImage, asgName);
    const machineImageParameter = new CfnParameter(this, "AmazonLinuxImageId", {
      type: "AWS::SSM::Parameter::Value<AWS::EC2::Image::Id>",
      default: "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64",
      description: "Amazon Linux 2023 AMI resolved by CloudFormation in the deployment region",
    });

    this.autoScalingGroup = new AutoScalingGroup(this, "ApplicationAsg", {
      autoScalingGroupName: asgName,
      vpc: props.vpc,
      vpcSubnets: { subnetType: SubnetType.PRIVATE_WITH_EGRESS },
      instanceType: new InstanceType(deploymentConfig.applicationInstanceType),
      machineImage: MachineImage.genericLinux({
        [this.region]: machineImageParameter.valueAsString,
      }),
      role,
      securityGroup: props.applicationSecurityGroup,
      userData,
      minCapacity: deploymentConfig.applicationInstances,
      maxCapacity: deploymentConfig.maximumApplicationInstances,
      healthChecks: HealthChecks.withAdditionalChecks({
        additionalTypes: [AdditionalHealthCheckType.ELB],
        gracePeriod: Duration.minutes(15),
      }),
    });

    this.loadBalancer = new ApplicationLoadBalancer(this, "LoadBalancer", {
      vpc: props.vpc,
      internetFacing: false,
      vpcSubnets: { subnetType: SubnetType.PRIVATE_WITH_EGRESS },
      securityGroup: props.loadBalancerSecurityGroup,
    });
    const targetGroup = new ApplicationTargetGroup(this, "GatewayTargets", {
      vpc: props.vpc,
      targetType: TargetType.INSTANCE,
      protocol: ApplicationProtocol.HTTP,
      port: 8080,
      healthCheck: {
        enabled: true,
        path: "/actuator/health/readiness",
        healthyHttpCodes: "200",
        interval: Duration.seconds(30),
        timeout: Duration.seconds(5),
      },
      deregistrationDelay: Duration.seconds(30),
    });
    this.autoScalingGroup.attachToApplicationTargetGroup(targetGroup);
    this.loadBalancer.addListener("HttpListener", {
      port: 80,
      protocol: ApplicationProtocol.HTTP,
      defaultAction: ListenerAction.forward([targetGroup]),
    });

    this.autoScalingGroup.scaleOnCpuUtilization("CpuScaling", {
      targetUtilizationPercent: 60,
      cooldown: Duration.minutes(3),
    });
    new CfnOutput(this, "LoadBalancerDnsName", {
      value: this.loadBalancer.loadBalancerDnsName,
    });
    new CfnOutput(this, "AutoScalingGroupName", {
      value: this.autoScalingGroup.autoScalingGroupName,
    });
  }

  private createBackendImage(role: Role): string {
    const asset = new DockerImageAsset(this, "BackendImage", {
      directory: path.resolve(__dirname, "../../.."),
      file: "deploy/local/backend.Dockerfile",
      platform: Platform.LINUX_AMD64,
      buildArgs: {
        MAVEN_TEST_ARGS: "-DskipUnitTests=true -DskipITs=true",
      },
    });
    asset.repository.grantPull(role);
    return asset.imageUri;
  }

  private createUserData(
    props: ComputeStackProps,
    backendImage: string,
    asgName: string,
  ): UserData {
    const data = props.data;
    const userData = UserData.forLinux();
    const environmentFile = "/etc/ecommerce/platform.env";
    const region = Stack.of(this).region;

    userData.addCommands(
      "set -euo pipefail",
      "dnf install -y docker jq",
      "systemctl enable --now docker",
      "mkdir -p /etc/ecommerce /var/lib/consul",
      `SECRET_JSON="$(aws secretsmanager get-secret-value --region '${region}' --secret-id '${data.databaseSecret.secretArn}' --query SecretString --output text)"`,
      'DB_USERNAME="$(printf \'%s\' "$SECRET_JSON" | jq -r .username)"',
      'DB_PASSWORD="$(printf \'%s\' "$SECRET_JSON" | jq -r .password)"',
      'TOKEN="$(curl --fail --silent --request PUT --header \'X-aws-ec2-metadata-token-ttl-seconds: 21600\' http://169.254.169.254/latest/api/token)"',
      'INSTANCE_ID="$(curl --fail --silent --header "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id)"',
      'WORKER_ID="$((16#${INSTANCE_ID: -3} % 1024))"',
      `install -m 600 /dev/null ${environmentFile}`,
      `printf '%s\n' \
        "CONSUL_ENABLED=true" \
        "CONSUL_HOST=127.0.0.1" \
        "CONSUL_PORT=8500" \
        "INSTANCE_ID=$INSTANCE_ID" \
        "CATALOG_DB_URL=jdbc:mysql://${data.catalogDatabase.hostname}:${data.catalogDatabase.port}/${data.catalogDatabase.databaseName}?useSSL=true&serverTimezone=UTC" \
        "CATALOG_DB_USER=$DB_USERNAME" \
        "CATALOG_DB_PASSWORD=$DB_PASSWORD" \
        "INVENTORY_DB_URL=jdbc:mysql://${data.inventoryDatabase.hostname}:${data.inventoryDatabase.port}/${data.inventoryDatabase.databaseName}?useSSL=true&serverTimezone=UTC" \
        "INVENTORY_DB_USER=$DB_USERNAME" \
        "INVENTORY_DB_PASSWORD=$DB_PASSWORD" \
        "ORDER_DB_SHARD_0_URL=jdbc:mysql://${data.orderDatabase0.hostname}:${data.orderDatabase0.port}/${data.orderDatabase0.databaseName}?useSSL=true&serverTimezone=UTC" \
        "ORDER_DB_SHARD_1_URL=jdbc:mysql://${data.orderDatabase1.hostname}:${data.orderDatabase1.port}/${data.orderDatabase1.databaseName}?useSSL=true&serverTimezone=UTC" \
        "ORDER_DB_USER=$DB_USERNAME" \
        "ORDER_DB_PASSWORD=$DB_PASSWORD" \
        "REDIS_HOST=${data.redisHost}" \
        "REDIS_PORT=${data.redisPort}" \
        "KAFKA_BOOTSTRAP_SERVERS=${data.kafkaBootstrapServers}" \
        "ELASTICSEARCH_URL=${data.searchEndpoint}" \
        "CATALOG_EVENTS_ENABLED=true" \
        "CATALOG_SEARCH_ENABLED=true" \
        "ORDER_EVENTS_ENABLED=true" \
        "ORDER_ID_WORKER_ID=$WORKER_ID" \
        "GATEWAY_RATE_LIMIT_USE_FORWARDED_FOR=true" \
        "GATEWAY_RATE_LIMIT_TRUSTED_PROXY_COUNT=2" \
        "GATEWAY_CORS_ALLOWED_ORIGIN_PATTERNS=https://*.cloudfront.net" \
        > ${environmentFile}`,
      `REGISTRY="$(printf '%s' '${backendImage}' | cut -d/ -f1)"`,
      `aws ecr get-login-password --region '${region}' | docker login --username AWS --password-stdin "$REGISTRY"`,
      `docker pull '${backendImage}'`,
      `docker run -d --name consul --restart unless-stopped --network host \
        -v /var/lib/consul:/consul/data \
        hashicorp/consul:1.22.7 agent -server -ui -client=0.0.0.0 \
        -bootstrap-expect=${deploymentConfig.applicationInstances} \
        -retry-join="provider=aws tag_name=aws:autoscaling:groupName tag_value=${asgName}" \
        -node="$INSTANCE_ID"`,
      "until curl --fail --silent http://127.0.0.1:8500/v1/status/leader | grep -qE '[0-9]+\\.[0-9]+'; do sleep 2; done",
    );

    for (const [name, jar] of [
      ["gateway", "gateway-service.jar"],
      ["catalog", "catalog-service.jar"],
      ["inventory", "inventory-service.jar"],
      ["order", "order-service.jar"],
    ]) {
      userData.addCommands(
        `docker run -d --name ${name} --restart unless-stopped --network host \
          --env-file ${environmentFile} \
          --log-driver=awslogs \
          --log-opt awslogs-region='${region}' \
          --log-opt awslogs-group='${this.applicationLogGroup.logGroupName}' \
          --log-opt awslogs-stream="$INSTANCE_ID/${name}" \
          '${backendImage}' /app/${jar}`,
      );
    }
    return userData;
  }
}
