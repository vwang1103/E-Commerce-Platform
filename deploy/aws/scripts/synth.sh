#!/usr/bin/env bash
set -euo pipefail

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "$script_directory/../../.." && pwd)"

npm --prefix "$repository_root/frontend" ci
npm --prefix "$repository_root/frontend" run build
cd "$repository_root/deploy/aws"
npm ci
npm test
npx cdk synth

