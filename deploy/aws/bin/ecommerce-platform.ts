#!/usr/bin/env node
import { App, Tags } from "aws-cdk-lib";
import { ComputeStack } from "../lib/compute-stack";
import { DataStack } from "../lib/data-stack";
import { EdgeStack } from "../lib/edge-stack";
import { NetworkStack } from "../lib/network-stack";

const app = new App();
const account = process.env.CDK_DEFAULT_ACCOUNT;
const applicationRegion =
  process.env.CDK_DEFAULT_REGION ?? process.env.AWS_REGION ?? "us-east-2";
const applicationEnvironment = { account, region: applicationRegion };
const edgeEnvironment = { account, region: "us-east-1" };

const network = new NetworkStack(app, "Ecommerce-Network", {
  env: applicationEnvironment,
});
const data = new DataStack(app, "Ecommerce-Data", {
  env: applicationEnvironment,
  vpc: network.vpc,
  dataSecurityGroup: network.dataSecurityGroup,
});
const compute = new ComputeStack(app, "Ecommerce-Compute", {
  env: applicationEnvironment,
  vpc: network.vpc,
  loadBalancerSecurityGroup: network.loadBalancerSecurityGroup,
  applicationSecurityGroup: network.applicationSecurityGroup,
  data,
  crossRegionReferences: true,
});
const edge = new EdgeStack(app, "Ecommerce-Edge", {
  env: edgeEnvironment,
  loadBalancer: compute.loadBalancer,
  crossRegionReferences: true,
});
edge.addStackDependency(compute);

for (const stack of [network, data, compute, edge]) {
  Tags.of(stack).add("Project", "ecommerce-platform");
  Tags.of(stack).add("ManagedBy", "aws-cdk");
}
