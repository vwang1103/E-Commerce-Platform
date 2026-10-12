# AWS deployment

This folder deploys the project to AWS with TypeScript CDK and CloudFormation. It has one fixed
configuration and one deployment command.

> AWS resources cost money. Delete the deployment when you finish using it.

## Quick start

### 1. Configure an AWS CLI profile

For AWS SSO:

```bash
aws configure sso --profile ecommerce
```

For access-key credentials:

```bash
aws configure --profile ecommerce
```

Credentials are saved by the AWS CLI outside this repository. Never put access keys in a project
file.

### 2. Create the account settings file

From the repository root:

```bash
cp deploy/aws/aws-account.env.example deploy/aws/aws-account.env
```

Edit `deploy/aws/aws-account.env`:

```text
AWS_PROFILE=ecommerce
AWS_ACCOUNT_ID=123456789012
AWS_REGION=us-east-2
AWS_SSO_LOGIN=true
```

- `AWS_PROFILE`: profile created in step 1.
- `AWS_ACCOUNT_ID`: your 12-digit AWS account ID.
- `AWS_REGION`: region for EC2 and the data services.
- `AWS_SSO_LOGIN`: `true` for SSO; otherwise `false`.

The file is ignored by Git and does not contain secret credentials. To find your account ID, run:

```bash
aws sso login --profile ecommerce   # SSO only
aws sts get-caller-identity --profile ecommerce
```

### 3. Deploy

Start Docker and run:

```bash
./deploy/aws/scripts/deploy.sh
```

The script logs in when necessary, verifies the account ID, builds the application, prepares CDK,
and deploys all resources. Review and approve the security changes when CDK asks.

Deployment can take a while because AWS must create databases, Kafka, and OpenSearch. When it
finishes, open the `StorefrontUrl` printed for the `Ecommerce-Edge` stack.

## Architecture

```text
Browser
  |
  v
CloudFront + WAF
  |-- website --> S3
  `-- /api/* --> Load Balancer --> 2 EC2 instances
                                      |
                         Gateway, Catalog, Inventory,
                         Order, and Consul
                                      |
                    MySQL + Redis + Kafka + OpenSearch
```

The two EC2 instances run in separate Availability Zones. The load balancer distributes requests,
and Auto Scaling replaces an unhealthy instance.

The load balancer is private. Its security group accepts port 80 only from AWS's managed CloudFront
origin-facing prefix list, so clients cannot bypass CloudFront and WAF by calling the ALB directly.

The data services use small settings and are not fully highly available.

## AWS services used

| Service | Purpose |
| --- | --- |
| EC2 and Auto Scaling | Run and replace backend servers |
| ALB | Balance API requests across EC2 instances |
| S3 and CloudFront | Store and deliver the Next.js website |
| WAF | Block common attacks and excessive traffic |
| RDS MySQL | Store catalog, inventory, and sharded orders |
| ElastiCache Redis | Inventory reservations and rate limits |
| MSK Kafka | Product and order events |
| OpenSearch | Product search |
| Secrets Manager | Store the generated database password |
| CloudWatch | Container logs |
| VPC | Keep backend and data services private |

## CDK project structure

The four stacks follow their dependency order:

```text
Network -> Data -> Compute -> Edge
```

| File | Responsibility |
| --- | --- |
| `bin/ecommerce-platform.ts` | Connects the stacks |
| `lib/config.ts` | Resource sizes |
| `lib/network-stack.ts` | VPC, subnets, and security groups |
| `lib/data-stack.ts` | MySQL, Redis, Kafka, OpenSearch, and secrets |
| `lib/compute-stack.ts` | Backend image, EC2, Auto Scaling, and ALB |
| `lib/edge-stack.ts` | S3, CloudFront, and WAF |

CDK generates CloudFormation, and CloudFormation creates and tracks the AWS resources.

## Generate CloudFormation without deploying

```bash
./deploy/aws/scripts/synth.sh
```

The generated templates appear in `deploy/aws/cdk.out/`. This command creates no application
resources.

## Delete the deployment

From `deploy/aws`:

```bash
npx cdk destroy --all
```

Afterward, check CloudFormation in the AWS Console and confirm all four `Ecommerce-*`
stacks are gone.
