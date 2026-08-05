#!/usr/bin/env bash
set -euo pipefail

JAVA_PID="${JAVA_PID:?JAVA_PID is required}"
DURATION="${DURATION:-60s}"
OUTPUT="${OUTPUT:-image-upload.jfr}"
NAME="devlog11-image-upload"

runtime_version="$(jcmd "${JAVA_PID}" VM.version | sed -n '2p')"
echo "Target runtime: ${runtime_version:-unknown}"
if [[ "${runtime_version}" != *"21."* ]]; then
  echo "warning: devlog-11 pinning comparison expects JDK 21" >&2
fi

jcmd "${JAVA_PID}" JFR.start name="${NAME}" settings=profile duration="${DURATION}" filename="${OUTPUT}"
echo "JFR recording scheduled: ${OUTPUT} (${DURATION})"
echo "JDK 21 default threshold: jdk.VirtualThreadPinned > 20ms"
echo "After it finishes:       jfr view pinned-threads ${OUTPUT}"
echo "Raw events with stacks:  jfr print --events jdk.VirtualThreadPinned ${OUTPUT}"
