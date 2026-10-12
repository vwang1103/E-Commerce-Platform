#!/usr/bin/env bash
set -euo pipefail

base_url="${GATEWAY_BASE_URL:-http://localhost:8080}"
storefront_url="${STOREFRONT_BASE_URL:-http://localhost:8084}"
api_url="${SMOKE_API_BASE_URL:-${storefront_url}}"
run_token="$(date +%s)$$"
product_name="SmokeLaptop${run_token}"
seller_id=""
user_id=""
work_dir="$(mktemp -d)"
product_id=""

cleanup() {
  if [[ -n "${product_id}" ]]; then
    curl --fail --silent \
      --request PUT "${api_url}/api/v1/products/${product_id}" \
      --header 'Content-Type: application/json' \
      --header "X-User-Id: ${seller_id}" \
      --data "{\"categoryId\":100,\"name\":\"${product_name}\",\"description\":\"End-to-end smoke product\",\"priceMinor\":129900,\"currency\":\"USD\",\"status\":\"INACTIVE\",\"version\":0}" \
      >/dev/null || true
  fi
  rm -rf "${work_dir}"
}

trap cleanup EXIT

json_field() {
  python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))[sys.argv[2]])' "$1" "$2"
}

wait_for_value() {
  local url="$1"
  local field="$2"
  local expected="$3"
  local output="$4"
  local attempts="${5:-60}"
  local value=""
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl --fail --silent --show-error \
      --header "X-User-Id: ${user_id}" \
      "${url}" --output "${output}"; then
      value="$(json_field "${output}" "${field}")"
      if [[ "${value}" == "${expected}" ]]; then
        return 0
      fi
    fi
    sleep 1
  done
  echo "Timed out waiting for ${field}=${expected} from ${url}; last value=${value}" >&2
  return 1
}

wait_for_http() {
  local url="$1"
  local output="$2"
  local attempts="${3:-60}"
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl --fail --silent --show-error \
      --header "X-User-Id: ${user_id}" \
      "${url}" --output "${output}"; then
      return 0
    fi
    sleep 1
  done
  echo "Timed out waiting for ${url}" >&2
  return 1
}

echo "Waiting for gateway readiness..."
wait_for_value "${base_url}/actuator/health/readiness" status UP "${work_dir}/health.json" 90

echo "Creating passwordless seller and buyer sessions through the backend..."
curl --fail --silent --show-error \
  --request POST "${api_url}/api/v1/login" \
  --header 'Content-Type: application/json' \
  --data "{\"username\":\"smoke-seller-${run_token}\"}" \
  --output "${work_dir}/seller.json"
seller_id="$(json_field "${work_dir}/seller.json" userId)"
curl --fail --silent --show-error \
  --request POST "${api_url}/api/v1/login" \
  --header 'Content-Type: application/json' \
  --data "{\"username\":\"smoke-buyer-${run_token}\"}" \
  --output "${work_dir}/buyer.json"
user_id="$(json_field "${work_dir}/buyer.json" userId)"

echo "Waiting for the Next.js storefront..."
for ((attempt = 1; attempt <= 60; attempt++)); do
  if curl --fail --silent --show-error "${storefront_url}/" --output "${work_dir}/storefront.html" \
    && grep --quiet "<title>Arcade</title>" "${work_dir}/storefront.html"; then
    break
  fi
  sleep 1
done
grep --quiet "<title>Arcade</title>" "${work_dir}/storefront.html"

echo "Waiting for service discovery to expose the inventory route..."
wait_for_http \
  "${api_url}/api/v1/inventory/1001" "${work_dir}/seed-inventory.json" 90

echo "Creating a catalog product through the gateway..."
curl --fail --silent --show-error \
  --request POST "${api_url}/api/v1/products" \
  --header 'Content-Type: application/json' \
  --header "X-User-Id: ${seller_id}" \
  --header "X-Correlation-Id: smoke-${run_token}" \
  --dump-header "${work_dir}/product.headers" \
  --data "{\"categoryId\":100,\"name\":\"${product_name}\",\"description\":\"End-to-end smoke product\",\"priceMinor\":129900,\"currency\":\"USD\",\"status\":\"ACTIVE\"}" \
  --output "${work_dir}/product.json"
grep --ignore-case --quiet "^X-Correlation-Id: smoke-${run_token}" "${work_dir}/product.headers"
product_id="$(json_field "${work_dir}/product.json" productId)"
curl --fail --silent --show-error \
  --header "X-User-Id: ${user_id}" \
  "${api_url}/api/v1/products/${product_id}" \
  --output "${work_dir}/product-read.json"

echo "Waiting for the Kafka-to-Elasticsearch projection..."
for ((attempt = 1; attempt <= 60; attempt++)); do
  curl --fail --silent --show-error \
    --header "X-User-Id: ${user_id}" \
    "${api_url}/api/v1/search?q=${product_name}&page=0&size=20" \
    --output "${work_dir}/search.json"
  if [[ "$(json_field "${work_dir}/search.json" totalElements)" != "0" ]]; then
    break
  fi
  sleep 1
done
if [[ "$(json_field "${work_dir}/search.json" totalElements)" == "0" ]]; then
  echo "Product never appeared in Elasticsearch" >&2
  exit 1
fi

echo "Creating inventory and submitting an order through Consul/OpenFeign..."
curl --fail --silent --show-error \
  --request PUT "${api_url}/api/v1/inventory/${product_id}" \
  --header 'Content-Type: application/json' \
  --header "X-User-Id: ${user_id}" \
  --data '{"totalQuantity":10}' \
  --output "${work_dir}/inventory.json"

idempotency_key="smoke-order-${run_token}"
curl --fail --silent --show-error \
  --request POST "${api_url}/api/v1/orders" \
  --header 'Content-Type: application/json' \
  --header "X-User-Id: ${user_id}" \
  --header "Idempotency-Key: ${idempotency_key}" \
  --data "{\"items\":[{\"productId\":${product_id},\"quantity\":2}]}" \
  --output "${work_dir}/order.json"
order_id="$(json_field "${work_dir}/order.json" orderId)"

curl --fail --silent --show-error \
  --request POST "${api_url}/api/v1/orders" \
  --header 'Content-Type: application/json' \
  --header "X-User-Id: ${user_id}" \
  --header "Idempotency-Key: ${idempotency_key}" \
  --data "{\"items\":[{\"productId\":${product_id},\"quantity\":2}]}" \
  --output "${work_dir}/order-duplicate.json"
if [[ "$(json_field "${work_dir}/order-duplicate.json" orderId)" != "${order_id}" ]]; then
  echo "Order idempotency check returned a different order" >&2
  exit 1
fi

echo "Waiting for the asynchronous order workflow, then paying..."
wait_for_value \
  "${api_url}/api/v1/orders/${order_id}" status PENDING_PAYMENT "${work_dir}/order-pending.json"
curl --fail --silent --show-error \
  --request POST "${api_url}/api/v1/orders/${order_id}/payment" \
  --header "X-User-Id: ${user_id}" \
  --header "Idempotency-Key: smoke-payment-${run_token}" \
  --output "${work_dir}/order-paid.json"
wait_for_value \
  "${api_url}/api/v1/inventory/reservations/${order_id}" status CONFIRMED "${work_dir}/reservation.json"
wait_for_value \
  "${api_url}/api/v1/orders/${order_id}" status PAID "${work_dir}/order-confirmed.json"

curl --fail --silent --show-error \
  --header "X-User-Id: ${user_id}" \
  "${api_url}/api/v1/orders?page=0&size=20" \
  --output "${work_dir}/orders.json"

echo "Smoke test passed: product=${product_id}, order=${order_id}"
