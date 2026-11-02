#!/bin/bash
set -euo pipefail

aws --endpoint-url=http://localhost:4566 --region us-east-1 sqs create-queue \
  --queue-name auction-payment-events \
  --attributes VisibilityTimeout=30,MessageRetentionPeriod=86400

aws --endpoint-url=http://localhost:4566 --region us-east-1 secretsmanager create-secret \
  --name auction-engine/payment-gateway \
  --secret-string '{"provider":"mock","apiKey":"local-only"}' \
  || true
