#!/bin/sh
# Unix bootstrap for the same pinned Maven 3.9.8 distribution as mvnw.cmd.
set -eu
task_root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
task_cache="${MAVEN_BOOTSTRAP_CACHE:-$task_root/.mvn/.cache}"
task_maven="$task_cache/apache-maven-3.9.8/bin/mvn"
if [ ! -x "$task_maven" ]; then
    mkdir -p "$task_cache"
    task_stage=$(mktemp -d "$task_cache/download.XXXXXX")
    task_archive="$task_stage/maven.tar.gz"
    curl --fail --location --proto '=https' --tlsv1.2 \
        https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.8/apache-maven-3.9.8-bin.tar.gz \
        --output "$task_archive"
    task_expected=7d171def9b85846bf757a2cec94b7529371068a0670df14682447224e57983528e97a6d1b850327e4ca02b139abaab7fcb93c4315119e6f0ffb3f0cbc0d0b9a2
    if command -v sha512sum >/dev/null 2>&1; then
        task_actual=$(sha512sum "$task_archive" | cut -d ' ' -f 1)
    else
        task_actual=$(shasum -a 512 "$task_archive" | cut -d ' ' -f 1)
    fi
    if [ "$task_actual" != "$task_expected" ]; then
        echo "Maven SHA-512 verification failed; refusing to execute." >&2
        exit 1
    fi
    tar -xzf "$task_archive" -C "$task_stage"
    # Only a completely downloaded and verified distribution becomes executable.
    if [ ! -d "$task_cache/apache-maven-3.9.8" ]; then
        mv "$task_stage/apache-maven-3.9.8" "$task_cache/apache-maven-3.9.8"
    fi
fi
exec "$task_maven" "$@"

