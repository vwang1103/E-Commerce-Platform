import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";

test("CDK uses one fixed deployment configuration", () => {
  const source = readFileSync(new URL("../lib/config.ts", import.meta.url), "utf8");
  assert.match(source, /export const deploymentConfig/);
  assert.doesNotMatch(source, /isProduction|development/);
});

test("the CDK app defines all four architecture stacks", () => {
  const source = readFileSync(new URL("../bin/ecommerce-platform.ts", import.meta.url), "utf8");
  for (const name of ["NetworkStack", "DataStack", "ComputeStack", "EdgeStack"]) {
    assert.match(source, new RegExp(name));
  }
});
