#!/bin/bash

set -euo pipefail

echo "Notification Dispatcher - Smoke Tests"
echo "===================================="

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

BASE_URL="${BASE_URL:-http://localhost:8080}"
NODE_BASE_URL="${NODE_BASE_URL:-http://localhost:3000}"
CLIENT_ID="${CLIENT_ID:-portfolio-client-001}"

assert_non_empty() {
    local value="$1"
    local message="$2"

    if [[ -z "$value" ]]; then
        echo -e "${RED}${message}${NC}"
        exit 1
    fi
}

assert_equals() {
    local expected="$1"
    local actual="$2"
    local message="$3"

    if [[ "$expected" != "$actual" ]]; then
        echo -e "${RED}${message}${NC}"
        echo "Expected: $expected"
        echo "Actual:   $actual"
        exit 1
    fi
}

extract_notification_id() {
    local payload="$1"
    echo "$payload" | grep -o '"notificationId":"[^"]*' | cut -d'"' -f4
}

echo -e "${BLUE}Checking Java service health${NC}"
JAVA_HEALTH=$(curl -fsS "${BASE_URL}/api/v1/notifications/health")
if [[ "$JAVA_HEALTH" != *'"status":"UP"'* ]]; then
    echo -e "${RED}Java service returned an unexpected health payload: ${JAVA_HEALTH}${NC}"
    exit 1
fi
echo -e "${GREEN}Java service is healthy${NC}"

echo -e "\n${BLUE}Checking Node.js service health${NC}"
NODE_HEALTH=$(curl -fsS "${NODE_BASE_URL}/api/v1/health")
if [[ "$NODE_HEALTH" != *'"status":"UP"'* ]]; then
    echo -e "${RED}Node.js service returned an unexpected health payload: ${NODE_HEALTH}${NC}"
    exit 1
fi
echo -e "${GREEN}Node.js service is healthy${NC}"

IDEMPOTENCY_KEY=$(uuidgen)
PUSH_PAYLOAD=$(cat <<EOF
{
  "userId": "user-demo",
  "channel": "push",
  "subject": "Welcome to Portfolio!",
  "body": "This is a real-time push notification via Socket.IO"
}
EOF
)

echo -e "\n${BLUE}Sending push notification${NC}"
PUSH_RESPONSE=$(curl -fsS -X POST "${BASE_URL}/api/v1/notifications/send" \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: ${CLIENT_ID}" \
  -H "Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  -d "$PUSH_PAYLOAD")
PUSH_ID=$(extract_notification_id "$PUSH_RESPONSE")
assert_non_empty "$PUSH_ID" "Push notification did not return a notificationId"
echo -e "${GREEN}Push accepted with notificationId: ${PUSH_ID}${NC}"

echo -e "\n${BLUE}Replaying push notification with the same idempotency key${NC}"
PUSH_REPLAY_RESPONSE=$(curl -fsS -X POST "${BASE_URL}/api/v1/notifications/send" \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: ${CLIENT_ID}" \
  -H "Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  -d "$PUSH_PAYLOAD")
PUSH_REPLAY_ID=$(extract_notification_id "$PUSH_REPLAY_RESPONSE")
assert_equals "$PUSH_ID" "$PUSH_REPLAY_ID" "Idempotency replay returned a different notificationId"
echo -e "${GREEN}Idempotency returned the cached notificationId${NC}"

EMAIL_RESPONSE=$(curl -fsS -X POST "${BASE_URL}/api/v1/notifications/send" \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: ${CLIENT_ID}" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"userId":"user-email-demo","channel":"email","subject":"Welcome Email from Portfolio System","body":"Hello {{name}}, this is a template-rendered email notification"}')
EMAIL_ID=$(extract_notification_id "$EMAIL_RESPONSE")
assert_non_empty "$EMAIL_ID" "Email notification did not return a notificationId"
echo -e "${GREEN}Email accepted with notificationId: ${EMAIL_ID}${NC}"

SMS_RESPONSE=$(curl -fsS -X POST "${BASE_URL}/api/v1/notifications/send" \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: ${CLIENT_ID}" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"userId":"user-sms-demo","channel":"sms","subject":"SMS Notification","body":"Quick notification via SMS channel"}')
SMS_ID=$(extract_notification_id "$SMS_RESPONSE")
assert_non_empty "$SMS_ID" "SMS notification did not return a notificationId"
echo -e "${GREEN}SMS accepted with notificationId: ${SMS_ID}${NC}"

echo -e "\n${BLUE}Verifying rate limiting${NC}"
RATE_LIMIT_TRIGGERED=0
for i in {1..15}; do
    RESPONSE=$(curl -sS -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/notifications/send" \
      -H "Content-Type: application/json" \
      -H "X-Client-Id: rate-limit-test" \
      -H "Idempotency-Key: $(uuidgen)" \
      -d "{\"userId\":\"user-$i\",\"channel\":\"push\",\"subject\":\"Test $i\",\"body\":\"Rate limit test message\"}")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    if [[ "$HTTP_CODE" == "429" ]]; then
        RATE_LIMIT_TRIGGERED=1
        break
    fi
done

if [[ "$RATE_LIMIT_TRIGGERED" -ne 1 ]]; then
    echo -e "${RED}Expected at least one HTTP 429 during rate limit verification${NC}"
    exit 1
fi

echo -e "${GREEN}Rate limiting produced HTTP 429 as expected${NC}"
echo -e "\n${GREEN}All smoke tests passed${NC}"
