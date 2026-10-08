#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
mkdir -p .run

if [[ ! -f .env ]]; then
  cp .env.example .env
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

export AI_PROVIDER=demo
export GYM_SERVICE_URL=http://localhost:8081
export WORKOUT_SERVICE_URL=http://localhost:8082
export ASSISTANT_SERVICE_URL=http://localhost:8083

wait_url() {
  local name="$1"
  local url="$2"
  for _ in {1..90}; do
    if curl --fail --silent --max-time 2 "$url" >/dev/null; then
      echo "$name ready"
      return 0
    fi
    sleep 2
  done
  echo "$name did not become ready at $url" >&2
  return 1
}

docker compose up -d
wait_url Keycloak http://localhost:8080/realms/gymflow/.well-known/openid-configuration
wait_url RabbitMQ http://localhost:15672

./mvnw --batch-mode -DskipTests package

nohup java -jar services/gym-service/target/gym-service-0.1.0.jar >.run/gym-service.log 2>&1 &
echo $! >.run/gym-service.pid
nohup java -jar services/workout-service/target/workout-service-0.1.0.jar >.run/workout-service.log 2>&1 &
echo $! >.run/workout-service.pid
nohup java -jar services/assistant-service/target/assistant-service-0.1.0.jar >.run/assistant-service.log 2>&1 &
echo $! >.run/assistant-service.pid
(cd frontend && nohup npm start -- --host 0.0.0.0 >../.run/frontend.log 2>&1 & echo $! >../.run/frontend.pid)

wait_url gym-service http://localhost:8081/actuator/health
wait_url workout-service http://localhost:8082/actuator/health
wait_url assistant-service http://localhost:8083/actuator/health
wait_url frontend http://localhost:4200
