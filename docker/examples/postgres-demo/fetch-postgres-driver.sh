#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
driver_url="${POSTGRES_DRIVER_URL:-https://jdbc.postgresql.org/download/postgresql-42.5.4.jar}"
target_dir="${script_dir}/lib-extra"
target_file="${target_dir}/$(basename "${driver_url}")"

mkdir -p "${target_dir}"
curl -fsSL "${driver_url}" -o "${target_file}"
echo "Downloaded ${target_file}"
