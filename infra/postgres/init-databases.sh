#!/bin/sh
set -eu

psql --set ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set gym_password="$GYM_DB_PASSWORD" \
  --set workout_password="$WORKOUT_DB_PASSWORD" \
  --set assistant_password="$ASSISTANT_DB_PASSWORD" \
  --set keycloak_password="$KEYCLOAK_DB_PASSWORD" <<-'EOSQL'
CREATE USER gym_service WITH PASSWORD :'gym_password';
CREATE USER workout_service WITH PASSWORD :'workout_password';
CREATE USER assistant_service WITH PASSWORD :'assistant_password';
CREATE USER keycloak_service WITH PASSWORD :'keycloak_password';

CREATE DATABASE gym_db OWNER gym_service;
CREATE DATABASE workout_db OWNER workout_service;
CREATE DATABASE assistant_db OWNER assistant_service;
CREATE DATABASE keycloak_db OWNER keycloak_service;
EOSQL

