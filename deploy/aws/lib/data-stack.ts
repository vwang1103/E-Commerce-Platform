import {
  CfnOutput,
  Duration,
  RemovalPolicy,
  Stack,
  type StackProps,
} from "aws-cdk-lib";
import { InstanceType, SubnetType, type IVpc, type ISecurityGroup } from "aws-cdk-lib/aws-ec2";
import { CfnReplicationGroup, CfnSubnetGroup } from "aws-cdk-lib/aws-elasticache";
import { AnyPrincipal, PolicyStatement } from "aws-cdk-lib/aws-iam";
import { CfnCluster } from "aws-cdk-lib/aws-msk";
import { Domain, EngineVersion } from "aws-cdk-lib/aws-opensearchservice";
import {
  Credentials,
  DatabaseInstance,
  DatabaseInstanceEngine,
  MysqlEngineVersion,
  StorageType,
} from "aws-cdk-lib/aws-rds";
import { Secret } from "aws-cdk-lib/aws-secretsmanager";
import {
  AwsCustomResource,
  AwsCustomResourcePolicy,
  PhysicalResourceId,
} from "aws-cdk-lib/custom-resources";
import type { Construct } from "constructs";
import { deploymentConfig } from "./config";

export interface DataStackProps extends StackProps {
  readonly vpc: IVpc;
  readonly dataSecurityGroup: ISecurityGroup;
}

export interface DatabaseEndpoint {
  readonly hostname: string;
  readonly port: string;
  readonly databaseName: string;
}

export class DataStack extends Stack {
  readonly databaseSecret: Secret;
  readonly catalogDatabase: DatabaseEndpoint;
  readonly inventoryDatabase: DatabaseEndpoint;
  readonly orderDatabase0: DatabaseEndpoint;
  readonly orderDatabase1: DatabaseEndpoint;
  readonly redisHost: string;
  readonly redisPort: string;
  readonly kafkaBootstrapServers: string;
  readonly searchEndpoint: string;

  constructor(scope: Construct, id: string, props: DataStackProps) {
    super(scope, id, props);

    this.databaseSecret = new Secret(this, "DatabaseCredentials", {
      description: "Shared application credentials for the four platform MySQL databases",
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: "ecommerce" }),
        generateStringKey: "password",
        excludePunctuation: true,
        passwordLength: 32,
      },
    });
    this.databaseSecret.applyRemovalPolicy(RemovalPolicy.DESTROY);

    this.catalogDatabase = this.createDatabase("CatalogDatabase", "catalog_db", props);
    this.inventoryDatabase = this.createDatabase("InventoryDatabase", "inventory_db", props);
    this.orderDatabase0 = this.createDatabase("OrderDatabase0", "order_db", props);
    this.orderDatabase1 = this.createDatabase("OrderDatabase1", "order_db", props);

    const dataSubnets = props.vpc.selectSubnets({ subnetType: SubnetType.PRIVATE_ISOLATED });
    const redisSubnets = new CfnSubnetGroup(this, "RedisSubnetGroup", {
      description: "Private subnets for platform Redis",
      subnetIds: dataSubnets.subnetIds,
    });
    const redis = new CfnReplicationGroup(this, "Redis", {
      replicationGroupDescription: "E-commerce inventory and gateway rate-limit Redis",
      engine: "redis",
      engineVersion: "7.1",
      cacheNodeType: deploymentConfig.redisInstanceType,
      numCacheClusters: 1,
      automaticFailoverEnabled: false,
      multiAzEnabled: false,
      atRestEncryptionEnabled: true,
      transitEncryptionEnabled: false,
      cacheSubnetGroupName: redisSubnets.ref,
      securityGroupIds: [props.dataSecurityGroup.securityGroupId],
    });
    redis.addResourceDependency(redisSubnets);
    redis.applyRemovalPolicy(RemovalPolicy.DESTROY);
    this.redisHost = redis.attrPrimaryEndPointAddress;
    this.redisPort = redis.attrPrimaryEndPointPort;

    const kafka = new CfnCluster(this, "Kafka", {
      clusterName: "ecommerce",
      kafkaVersion: deploymentConfig.kafkaVersion,
      numberOfBrokerNodes: 2,
      brokerNodeGroupInfo: {
        clientSubnets: dataSubnets.subnetIds,
        instanceType: deploymentConfig.kafkaInstanceType,
        securityGroups: [props.dataSecurityGroup.securityGroupId],
        storageInfo: { ebsStorageInfo: { volumeSize: 50 } },
      },
      encryptionInfo: {
        encryptionAtRest: { dataVolumeKmsKeyId: "alias/aws/kafka" },
        encryptionInTransit: { clientBroker: "PLAINTEXT", inCluster: true },
      },
      enhancedMonitoring: "DEFAULT",
    });
    const kafkaBrokers = new AwsCustomResource(this, "KafkaBootstrapBrokers", {
      onCreate: {
        service: "Kafka",
        action: "getBootstrapBrokers",
        parameters: { ClusterArn: kafka.attrArn },
        physicalResourceId: PhysicalResourceId.of(kafka.attrArn),
      },
      onUpdate: {
        service: "Kafka",
        action: "getBootstrapBrokers",
        parameters: { ClusterArn: kafka.attrArn },
        physicalResourceId: PhysicalResourceId.of(kafka.attrArn),
      },
      policy: AwsCustomResourcePolicy.fromSdkCalls({
        resources: [kafka.attrArn],
      }),
      installLatestAwsSdk: false,
    });
    kafkaBrokers.node.addDependency(kafka);
    this.kafkaBootstrapServers = kafkaBrokers.getResponseField("BootstrapBrokerString");

    const search = new Domain(this, "Search", {
      version: EngineVersion.OPENSEARCH_3_3,
      vpc: props.vpc,
      vpcSubnets: [{ subnetType: SubnetType.PRIVATE_ISOLATED }],
      securityGroups: [props.dataSecurityGroup],
      capacity: {
        dataNodeInstanceType: deploymentConfig.searchInstanceType,
        dataNodes: 1,
        multiAzWithStandbyEnabled: false,
      },
      ebs: { enabled: true, volumeSize: 20 },
      encryptionAtRest: { enabled: true },
      nodeToNodeEncryption: true,
      enforceHttps: true,
      accessPolicies: [
        new PolicyStatement({
          principals: [new AnyPrincipal()],
          actions: ["es:ESHttp*"],
          resources: ["*"],
        }),
      ],
      removalPolicy: RemovalPolicy.DESTROY,
    });
    this.searchEndpoint = `https://${search.domainEndpoint}`;

    new CfnOutput(this, "DatabaseSecretArn", { value: this.databaseSecret.secretArn });
    new CfnOutput(this, "SearchEndpoint", { value: this.searchEndpoint });
  }

  private createDatabase(
    id: string,
    databaseName: string,
    props: DataStackProps,
  ): DatabaseEndpoint {
    const database = new DatabaseInstance(this, id, {
      engine: DatabaseInstanceEngine.mysql({ version: MysqlEngineVersion.VER_8_0_46 }),
      credentials: Credentials.fromPassword(
        "ecommerce",
        this.databaseSecret.secretValueFromJson("password"),
      ),
      databaseName,
      instanceType: new InstanceType(deploymentConfig.databaseInstanceType),
      vpc: props.vpc,
      vpcSubnets: { subnetType: SubnetType.PRIVATE_ISOLATED },
      securityGroups: [props.dataSecurityGroup],
      allocatedStorage: 20,
      maxAllocatedStorage: 100,
      storageEncrypted: true,
      storageType: StorageType.GP3,
      multiAz: false,
      publiclyAccessible: false,
      backupRetention: Duration.days(1),
      deletionProtection: false,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    return {
      hostname: database.dbInstanceEndpointAddress,
      port: database.dbInstanceEndpointPort,
      databaseName,
    };
  }
}
