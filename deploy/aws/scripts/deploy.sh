#!/usr/bin/env bash
set -euo pipefail

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "$script_directory/../../.." && pwd)"
account_file="$repository_root/deploy/aws/aws-account.env"

if [[ ! -f "$account_file" ]]; then
  echo "Missing deploy/aws/aws-account.env." >&2
  echo "Copy deploy/aws/aws-account.env.example to deploy/aws/aws-account.env and edit it." >&2
  exit 2
fi

# shellcheck disable=SC1090
source "$account_file"

: "${AWS_PROFILE:?Set AWS_PROFILE in deploy/aws/aws-account.env}"
: "${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID in deploy/aws/aws-account.env}"
: "${AWS_REGION:?Set AWS_REGION in deploy/aws/aws-account.env}"
: "${AWS_SSO_LOGIN:?Set AWS_SSO_LOGIN to true or false in deploy/aws/aws-account.env}"

if [[ ! "$AWS_ACCOUNT_ID" =~ ^[0-9]{12}$ ]]; then
  echo "AWS_ACCOUNT_ID must contain exactly 12 digits." >&2
  exit 2
fi
if [[ "$AWS_SSO_LOGIN" != "true" && "$AWS_SSO_LOGIN" != "false" ]]; then
  echo "AWS_SSO_LOGIN must be true or false." >&2
  exit 2
fi

export AWS_PROFILE AWS_REGION
export AWS_DEFAULT_REGION="$AWS_REGION"

if [[ "$AWS_SSO_LOGIN" == "true" ]]; then
  echo "Opening AWS SSO login for profile $AWS_PROFILE..."
  aws sso login --profile "$AWS_PROFILE"
fi

authenticated_account="$(aws sts get-caller-identity --query Account --output text)"
if [[ "$authenticated_account" != "$AWS_ACCOUNT_ID" ]]; then
  echo "Account mismatch: aws-account.env says $AWS_ACCOUNT_ID but AWS CLI authenticated to $authenticated_account." >&2
  exit 2
fi

aws_region="$AWS_REGION"
aws_account="$authenticated_account"

export CDK_DEFAULT_REGION="$aws_region"
export CDK_DEFAULT_ACCOUNT="$aws_account"

echo "Building the static storefront..."
npm --prefix "$repository_root/frontend" ci
npm --prefix "$repository_root/frontend" run build

echo "Installing and verifying the CDK application..."
cd "$repository_root/deploy/aws"
npm ci
npm test

echo "Bootstrapping CDK in account $aws_account, application region $aws_region..."
npx cdk bootstrap "aws://$aws_account/$aws_region"
if [[ "$aws_region" != "us-east-1" ]]; then
  echo "Bootstrapping us-east-1 for CloudFront and AWS WAF..."
  npx cdk bootstrap "aws://$aws_account/us-east-1"
fi

echo "Deploying the e-commerce platform..."
npx cdk deploy --all \
  --require-approval broadening

