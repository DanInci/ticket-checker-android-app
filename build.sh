#!/usr/bin/env bash

set -eux -o pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

docker-compose build --pull
if [[ ${#} -gt 0 ]]; then
    command docker-compose up "${@}"
else
    command docker-compose up
fi

docker cp ticket-checker-android-app-build:/build/app/build/outputs/apk/release/app-release-unsigned.apk .