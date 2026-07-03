#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

# Manual runtime smoke for the real qz.App entry point
# Uses the normal user profile to match a manual launch

STAMP="${STAMP:-$(date +%Y%m%d-%H%M%S)}"
ARTIFACT_DIR="${ARTIFACT_DIR:-/tmp/qz-app-runtime-smoke-$STAMP}"
MONITOR_LOG="$ARTIFACT_DIR/dbus-monitor.log"
APP_STDOUT="$ARTIFACT_DIR/qz-app.stdout.log"
PROBE_LOG="$ARTIFACT_DIR/linux-sni-probe.log"
LAUNCH_CMD="$ARTIFACT_DIR/launch-command.txt"
DBUS_SNIPPET="$ARTIFACT_DIR/dbus-notify-snippet.txt"
SMOKE_SUMMARY="$ARTIFACT_DIR/summary.txt"
CP="out/build/qz-tray:deps/ivy/*:deps/manual/*"

mkdir -p "$ARTIFACT_DIR"

echo "== Linux SNI probe =="
java -cp "$CP" qz.ui.tray.linux.LinuxSniProbe > "$PROBE_LOG" 2>&1
cat "$PROBE_LOG"

cat > "$LAUNCH_CMD" <<EOF
timeout 25s java \\
  -Dtray.notifications=true \\
  -cp "$CP" \\
  qz.App
EOF

echo "== Start D-Bus monitor =="
timeout 30s dbus-monitor --session \
  "type='method_call',interface='org.freedesktop.Notifications',member='Notify'" \
  > "$MONITOR_LOG" 2>&1 &
monitor_pid=$!

cleanup() {
  kill "$monitor_pid" 2>/dev/null || true
  wait "$monitor_pid" 2>/dev/null || true
}
trap cleanup EXIT

sleep 1

echo "== Launch qz.App =="
set +e
timeout 25s java \
  -Dtray.notifications=true \
  -cp "$CP" \
  qz.App > "$APP_STDOUT" 2>&1
app_status=$?
set -e

sleep 1
cleanup
trap - EXIT

# qz.App is a server process, so timeout 124 is the expected smoke exit
if [ "$app_status" -ne 124 ]; then
  echo "qz.App exited unexpectedly with status $app_status"
  tail -n 120 "$APP_STDOUT" || true
  exit "$app_status"
fi

echo "== Validate captured notification =="
grep -F "member=Notify" "$MONITOR_LOG"
grep -F 'string "QZ Tray"' "$MONITOR_LOG"
grep -F "qz-tray.png" "$MONITOR_LOG"
grep -F "Server started on port(s)" "$MONITOR_LOG"
grep -F "int32 -1" "$MONITOR_LOG"
grep -F "byte 1" "$MONITOR_LOG"

if grep -F "SNAPSHOT" "$MONITOR_LOG"; then
  echo "Unexpected versioned snapshot text in native notification"
  exit 1
fi

if grep -E "Unable to (connect to|send) Linux desktop notifications" "$APP_STDOUT"; then
  echo "Notification D-Bus failure found in stdout"
  exit 1
fi

grep -E 'member=Notify|string "QZ Tray"|qz-tray.png|Server started on port\\(s\\)|int32 -1|byte 1' \
  "$MONITOR_LOG" > "$DBUS_SNIPPET"

{
  echo "Artifact directory: $ARTIFACT_DIR"
  echo "qz.App timeout status: $app_status"
  echo "Probe log: $PROBE_LOG"
  echo "Launch command: $LAUNCH_CMD"
  echo "D-Bus monitor log: $MONITOR_LOG"
  echo "D-Bus snippet: $DBUS_SNIPPET"
  echo "App stdout log: $APP_STDOUT"
  echo "Native notification title: QZ Tray"
  echo "Native notification icon: generated absolute qz-tray.png path"
} > "$SMOKE_SUMMARY"

cat "$SMOKE_SUMMARY"
echo "qz.App runtime smoke passed"
