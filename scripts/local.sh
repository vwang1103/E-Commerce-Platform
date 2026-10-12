#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
compose_file="${repository_root}/deploy/local/compose.yml"
env_file="${repository_root}/.env"
env_example="${repository_root}/.env.example"
compose=(docker compose --env-file "${env_file}" -f "${compose_file}")

usage() {
  cat <<'EOF'
Usage:
  ./scripts/local.sh start [backend|all] [--skip-tests] [--no-build] [--smoke]
  ./scripts/local.sh dev [--skip-tests] [--no-build] [--skip-install]
  ./scripts/local.sh stop
  ./scripts/local.sh status
  ./scripts/local.sh logs [service]
  ./scripts/local.sh smoke
  ./scripts/local.sh seed-orders

Commands:
  start backend  Run infrastructure, four backend services, and API docs. Gateway: http://localhost:8080
  start all      Run backend, API docs, and the Dockerized storefront. Storefront: http://localhost:8084
  dev            Run the Docker backend, then Next.js in the foreground. Storefront: http://localhost:3000
  stop           Stop the complete Compose project while retaining all data volumes.
  status         Show containers, health, and one-shot migration results.
  logs           Follow logs for every service, or for one named Compose service.
  smoke          Run the complete buyer-journey smoke test against the running stack.
  seed-orders    Place a few paid demo orders using the local username-only identity.

Options:
  --skip-tests   Skip backend unit tests while building the Docker image.
  --no-build     Reuse existing Docker images without rebuilding them.
  --smoke        Run the buyer-journey smoke test after startup (start all only).
  --skip-install Reuse frontend/node_modules for dev instead of running npm ci.

Tests are enabled by default when the backend image must be rebuilt. Docker layer caching
may reuse a previously verified build when no relevant source file changed.
EOF
}

ensure_environment() {
  if [[ ! -f "${env_file}" ]]; then
    cp "${env_example}" "${env_file}"
    echo "Created .env from .env.example (local development values only)."
  fi

  if ! docker info >/dev/null 2>&1; then
    echo "Docker is not running. Start Docker Desktop and retry." >&2
    exit 1
  fi
}

wait_for_url() {
  local name="$1"
  local url="$2"
  local attempts="${3:-120}"

  echo "Waiting for ${name}..."
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl --fail --silent --show-error "${url}" >/dev/null 2>&1; then
      echo "${name} is ready: ${url}"
      return 0
    fi
    sleep 1
  done

  echo "Timed out waiting for ${name} at ${url}. Run ./scripts/local.sh logs for details." >&2
  return 1
}

start_platform() {
  local target="$1"
  local skip_tests="$2"
  local build_images="$3"
  local run_smoke="$4"
  local -a up_args=(up -d)

  ensure_environment

  if [[ "${build_images}" == "true" ]]; then
    up_args+=(--build)
  elif [[ "${skip_tests}" == "true" ]]; then
    echo "Note: --skip-tests has no effect with --no-build because no Maven build runs."
  fi

  if [[ "${skip_tests}" == "true" ]]; then
    export MAVEN_TEST_ARGS="-DskipUnitTests=true -DskipITs=true"
    echo "Backend image build: tests skipped by request."
  else
    unset MAVEN_TEST_ARGS || true
    echo "Backend image build: unit tests enabled."
  fi

  if [[ "${target}" == "backend" ]]; then
    "${compose[@]}" "${up_args[@]}" gateway-service swagger-ui
  else
    "${compose[@]}" "${up_args[@]}"
  fi

  wait_for_url "Gateway" "http://localhost:${GATEWAY_PORT:-8080}/actuator/health/readiness"
  wait_for_url "Swagger UI" "http://localhost:${SWAGGER_UI_PORT:-8085}/"
  if [[ "${target}" == "all" ]]; then
    wait_for_url "Storefront" "http://localhost:${STOREFRONT_PORT:-8084}/"
  fi

  "${compose[@]}" ps -a

  if [[ "${run_smoke}" == "true" ]]; then
    if [[ "${target}" != "all" ]]; then
      echo "--smoke requires 'start all' because the smoke test verifies the storefront." >&2
      exit 2
    fi
    "${repository_root}/scripts/smoke-test.sh"
  fi
}

command="${1:-help}"
if [[ $# -gt 0 ]]; then
  shift
fi

case "${command}" in
  start)
    target="all"
    skip_tests="false"
    build_images="true"
    run_smoke="false"

    if [[ $# -gt 0 && ("$1" == "backend" || "$1" == "all") ]]; then
      target="$1"
      shift
    fi

    while [[ $# -gt 0 ]]; do
      case "$1" in
        --skip-tests) skip_tests="true" ;;
        --no-build) build_images="false" ;;
        --smoke) run_smoke="true" ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown option for start: $1" >&2; usage; exit 2 ;;
      esac
      shift
    done

    start_platform "${target}" "${skip_tests}" "${build_images}" "${run_smoke}"
    ;;
  dev)
    skip_tests="false"
    build_images="true"
    install_dependencies="true"

    while [[ $# -gt 0 ]]; do
      case "$1" in
        --skip-tests) skip_tests="true" ;;
        --no-build) build_images="false" ;;
        --skip-install) install_dependencies="false" ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown option for dev: $1" >&2; usage; exit 2 ;;
      esac
      shift
    done

    start_platform backend "${skip_tests}" "${build_images}" false
    cd "${repository_root}/frontend"
    if [[ "${install_dependencies}" == "true" ]]; then
      npm ci
    elif [[ ! -d node_modules ]]; then
      echo "frontend/node_modules is missing; omit --skip-install for the first run." >&2
      exit 1
    fi
    echo "Starting Next.js at http://localhost:3000. Ctrl+C stops Next.js; the Docker backend stays running."
    exec npm run dev
    ;;
  stop)
    ensure_environment
    "${compose[@]}" down
    ;;
  status)
    ensure_environment
    "${compose[@]}" ps -a
    ;;
  logs)
    ensure_environment
    if [[ $# -gt 1 ]]; then
      echo "logs accepts at most one Compose service name." >&2
      exit 2
    elif [[ $# -eq 1 ]]; then
      "${compose[@]}" logs --follow "$1"
    else
      "${compose[@]}" logs --follow
    fi
    ;;
  smoke)
    ensure_environment
    "${repository_root}/scripts/smoke-test.sh"
    ;;
  seed-orders)
    ensure_environment
    "${repository_root}/scripts/seed-demo-orders.sh"
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    echo "Unknown command: ${command}" >&2
    usage
    exit 2
    ;;
esac
