import { Stack, type StackProps } from "aws-cdk-lib";
import {
  IpAddresses,
  Port,
  Peer,
  SecurityGroup,
  SubnetType,
  Vpc,
} from "aws-cdk-lib/aws-ec2";
import {
  AwsCustomResource,
  AwsCustomResourcePolicy,
  PhysicalResourceId,
} from "aws-cdk-lib/custom-resources";
import type { Construct } from "constructs";

export class NetworkStack extends Stack {
  readonly vpc: Vpc;
  readonly loadBalancerSecurityGroup: SecurityGroup;
  readonly applicationSecurityGroup: SecurityGroup;
  readonly dataSecurityGroup: SecurityGroup;

  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    this.vpc = new Vpc(this, "Vpc", {
      ipAddresses: IpAddresses.cidr("10.20.0.0/16"),
      availabilityZones: [`${this.region}a`, `${this.region}b`],
      natGateways: 1,
      subnetConfiguration: [
        { name: "public", subnetType: SubnetType.PUBLIC, cidrMask: 24 },
        { name: "application", subnetType: SubnetType.PRIVATE_WITH_EGRESS, cidrMask: 24 },
        { name: "data", subnetType: SubnetType.PRIVATE_ISOLATED, cidrMask: 24 },
      ],
    });

    this.loadBalancerSecurityGroup = new SecurityGroup(this, "LoadBalancerSecurityGroup", {
      vpc: this.vpc,
      description: "CloudFront VPC origin to the internal application load balancer",
      allowAllOutbound: true,
    });
    const cloudFrontPrefixList = new AwsCustomResource(this, "CloudFrontPrefixList", {
      onCreate: {
        service: "EC2",
        action: "describeManagedPrefixLists",
        parameters: {
          Filters: [
            {
              Name: "prefix-list-name",
              Values: ["com.amazonaws.global.cloudfront.origin-facing"],
            },
          ],
        },
        physicalResourceId: PhysicalResourceId.of("cloudfront-origin-facing-prefix-list"),
      },
      policy: AwsCustomResourcePolicy.fromSdkCalls({ resources: ["*"] }),
      installLatestAwsSdk: false,
    });
    this.loadBalancerSecurityGroup.addIngressRule(
      Peer.prefixList(cloudFrontPrefixList.getResponseField("PrefixLists.0.PrefixListId")),
      Port.tcp(80),
      "Allow HTTP only from CloudFront origin servers",
    );

    this.applicationSecurityGroup = new SecurityGroup(this, "ApplicationSecurityGroup", {
      vpc: this.vpc,
      description: "Spring services and Consul agents",
      allowAllOutbound: true,
    });
    this.applicationSecurityGroup.addIngressRule(this.loadBalancerSecurityGroup, Port.tcp(8080));
    this.applicationSecurityGroup.addIngressRule(
      this.applicationSecurityGroup,
      Port.tcpRange(8080, 8083),
    );
    this.applicationSecurityGroup.addIngressRule(this.applicationSecurityGroup, Port.tcpRange(8300, 8302));
    this.applicationSecurityGroup.addIngressRule(this.applicationSecurityGroup, Port.tcp(8500));
    this.applicationSecurityGroup.addIngressRule(this.applicationSecurityGroup, Port.udp(8301));
    this.applicationSecurityGroup.addIngressRule(this.applicationSecurityGroup, Port.udp(8302));

    this.dataSecurityGroup = new SecurityGroup(this, "DataSecurityGroup", {
      vpc: this.vpc,
      description: "Private data services reachable only from application instances",
      allowAllOutbound: true,
    });
    for (const port of [3306, 6379, 9092, 443]) {
      this.dataSecurityGroup.addIngressRule(this.applicationSecurityGroup, Port.tcp(port));
    }
  }
}
