import * as path from "node:path";
import {
  CfnOutput,
  RemovalPolicy,
  Stack,
  type StackProps,
} from "aws-cdk-lib";
import {
  AllowedMethods,
  CachePolicy,
  Distribution,
  Function,
  FunctionCode,
  FunctionEventType,
  OriginRequestPolicy,
  PriceClass,
  ResponseHeadersPolicy,
  ViewerProtocolPolicy,
} from "aws-cdk-lib/aws-cloudfront";
import { S3BucketOrigin, VpcOrigin } from "aws-cdk-lib/aws-cloudfront-origins";
import type { IApplicationLoadBalancer } from "aws-cdk-lib/aws-elasticloadbalancingv2";
import {
  BlockPublicAccess,
  Bucket,
  BucketEncryption,
} from "aws-cdk-lib/aws-s3";
import { BucketDeployment, Source } from "aws-cdk-lib/aws-s3-deployment";
import { CfnWebACL } from "aws-cdk-lib/aws-wafv2";
import type { Construct } from "constructs";

export interface EdgeStackProps extends StackProps {
  readonly loadBalancer: IApplicationLoadBalancer;
}

export class EdgeStack extends Stack {
  readonly distribution: Distribution;
  readonly storefrontBucket: Bucket;

  constructor(scope: Construct, id: string, props: EdgeStackProps) {
    super(scope, id, props);

    this.storefrontBucket = new Bucket(this, "StorefrontBucket", {
      blockPublicAccess: BlockPublicAccess.BLOCK_ALL,
      encryption: BucketEncryption.S3_MANAGED,
      enforceSSL: true,
      removalPolicy: RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
    });

    const webAcl = new CfnWebACL(this, "WebAcl", {
      scope: "CLOUDFRONT",
      defaultAction: { allow: {} },
      visibilityConfig: {
        cloudWatchMetricsEnabled: true,
        metricName: "ecommerce-edge",
        sampledRequestsEnabled: true,
      },
      rules: [
        {
          name: "AWSManagedCommonRules",
          priority: 0,
          overrideAction: { none: {} },
          statement: {
            managedRuleGroupStatement: {
              name: "AWSManagedRulesCommonRuleSet",
              vendorName: "AWS",
            },
          },
          visibilityConfig: {
            cloudWatchMetricsEnabled: true,
            metricName: "managed-common",
            sampledRequestsEnabled: true,
          },
        },
        {
          name: "IpRateLimit",
          priority: 1,
          action: { block: {} },
          statement: {
            rateBasedStatement: {
              aggregateKeyType: "IP",
              limit: 1_000,
            },
          },
          visibilityConfig: {
            cloudWatchMetricsEnabled: true,
            metricName: "ip-rate-limit",
            sampledRequestsEnabled: true,
          },
        },
      ],
    });

    const staticRewrite = new Function(this, "StaticRouteRewrite", {
      code: FunctionCode.fromInline(
        "function handler(event) { var r = event.request; if (r.uri.endsWith('/')) r.uri += 'index.html'; else if (!r.uri.includes('.')) r.uri += '/index.html'; return r; }",
      ),
    });
    const apiOrigin = VpcOrigin.withApplicationLoadBalancer(props.loadBalancer);

    this.distribution = new Distribution(this, "Distribution", {
      defaultRootObject: "index.html",
      defaultBehavior: {
        origin: S3BucketOrigin.withOriginAccessControl(this.storefrontBucket),
        viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
        cachePolicy: CachePolicy.CACHING_OPTIMIZED,
        responseHeadersPolicy: ResponseHeadersPolicy.SECURITY_HEADERS,
        functionAssociations: [
          { function: staticRewrite, eventType: FunctionEventType.VIEWER_REQUEST },
        ],
      },
      additionalBehaviors: {
        "api/*": {
          origin: apiOrigin,
          viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
          allowedMethods: AllowedMethods.ALLOW_ALL,
          cachePolicy: CachePolicy.CACHING_DISABLED,
          originRequestPolicy: OriginRequestPolicy.ALL_VIEWER_EXCEPT_HOST_HEADER,
        },
      },
      errorResponses: [
        { httpStatus: 403, responseHttpStatus: 404, responsePagePath: "/404.html" },
      ],
      webAclId: webAcl.attrArn,
      priceClass: PriceClass.PRICE_CLASS_100,
    });

    new BucketDeployment(this, "StorefrontDeployment", {
      destinationBucket: this.storefrontBucket,
      sources: [Source.asset(path.resolve(__dirname, "../../../frontend/out"))],
      distribution: this.distribution,
      distributionPaths: ["/*"],
      prune: true,
    });

    new CfnOutput(this, "StorefrontUrl", {
      value: `https://${this.distribution.distributionDomainName}`,
    });
  }
}
