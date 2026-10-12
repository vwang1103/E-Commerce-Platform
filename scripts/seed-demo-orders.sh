#!/usr/bin/env bash
# Places a few completed demo orders for one local username so order history is not empty.
# Safe to repeat: every request uses a fixed idempotency key, so reruns return the same orders.
set -euo pipefail

api_url="${SEED_API_BASE_URL:-http://localhost:${GATEWAY_PORT:-8080}}"
username="${SEED_USERNAME:-demo-shopper}"

# product ID|name|quantity
items=(
  "1004|Drift Wireless Headphones|1"
  "1018|Field Notes Weekly Planner|3"
  "1009|Skyline 1000-Piece Puzzle|2"
)

field() {
  python3 -c 'import json,sys; print(json.load(sys.stdin).get(sys.argv[1], ""))' "$1"
}

call() {
  curl --silent --show-error --header "X-User-Id: ${user_id}" "$@"
}

user_id="$(curl --fail --silent --show-error --request POST "${api_url}/api/v1/login" \
  --header 'Content-Type: application/json' \
  --data "{\"username\":\"${username}\"}" | field userId)"

wait_for_status() {
  local order_id="$1" expected="$2" status=""
  for ((attempt = 1; attempt <= 60; attempt++)); do
    status="$(call "${api_url}/api/v1/orders/${order_id}" | field status)"
    [[ "${status}" == "${expected}" ]] && return 0
    sleep 1
  done
  echo "Order ${order_id} stayed ${status:-unknown}; expected ${expected}." >&2
  return 1
}

for item in "${items[@]}"; do
  IFS='|' read -r product_id name quantity <<<"${item}"
  order="$(call --request POST "${api_url}/api/v1/orders" \
    --header 'Content-Type: application/json' \
    --header "Idempotency-Key: seed-${user_id}-${product_id}" \
    --data "{\"items\":[{\"productId\":${product_id},\"quantity\":${quantity}]}")"
  order_id="$(field orderId <<<"${order}")"
  if [[ -z "${order_id}" ]]; then
    echo "Skipping product ${product_id}: $(field detail <<<"${order}")" >&2
    continue
  fi

  current="$(call "${api_url}/api/v1/orders/${order_id}" | field status)"
  if [[ "${current}" == "ACCEPTED" || "${current}" == "PENDING_PAYMENT" ]]; then
    wait_for_status "${order_id}" PENDING_PAYMENT
    call --request POST "${api_url}/api/v1/orders/${order_id}/payment" \
      --header "Idempotency-Key: payment-${order_id}" >/dev/null
    wait_for_status "${order_id}" PAID
  fi
  echo "${username}: ${name} x${quantity} -> order ${order_id} ($(call "${api_url}/api/v1/orders/${order_id}" | field status))"
done
