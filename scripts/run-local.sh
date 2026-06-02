#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -f "$ROOT_DIR/.env.local" ]]; then
  set -a
  source "$ROOT_DIR/.env.local"
  set +a
fi

cd "$ROOT_DIR"
mvn -Dmaven.repo.local=.m2/repository spring-boot:run
