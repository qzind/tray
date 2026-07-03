#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

# Manual regression smoke for the SNI menu after notification send
# Uses the normal user profile to match a manual launch

STAMP="${STAMP:-$(date +%Y%m%d-%H%M%S)}"
ARTIFACT_DIR="${ARTIFACT_DIR:-/tmp/qz-app-menu-smoke-$STAMP}"
APP_STDOUT="$ARTIFACT_DIR/qz-app.stdout.log"
MENU_LOG="$ARTIFACT_DIR/menu-introspect.log"
LAUNCH_CMD="$ARTIFACT_DIR/launch-command.txt"
SMOKE_SUMMARY="$ARTIFACT_DIR/summary.txt"
CP="out/build/qz-tray:deps/ivy/*:deps/manual/*"

mkdir -p "$ARTIFACT_DIR"

cat > "$LAUNCH_CMD" <<EOF
java \\
  -Dtray.notifications=true \\
  -cp "$CP" \\
  qz.App
EOF

echo "== Launch qz.App =="
java \
  -Dtray.notifications=true \
  -cp "$CP" \
  qz.App > "$APP_STDOUT" 2>&1 &
app_pid=$!

cleanup() {
  kill "$app_pid" 2>/dev/null || true
  wait "$app_pid" 2>/dev/null || true
}
trap cleanup EXIT

echo "== Wait for notification send =="
for _ in $(seq 1 45); do
  if grep -q "Sent Linux desktop notification" "$APP_STDOUT"; then
    break
  fi
  sleep 1
done

if ! grep -q "Sent Linux desktop notification" "$APP_STDOUT"; then
  echo "Timed out waiting for Linux desktop notification"
  tail -n 160 "$APP_STDOUT" || true
  exit 1
fi

service=$(sed -n 's/.*Registered StatusNotifier item \([^ ]*\) at.*/\1/p' "$APP_STDOUT" | tail -n 1)
if [ -z "$service" ]; then
  echo "No StatusNotifier item service found"
  tail -n 160 "$APP_STDOUT" || true
  exit 1
fi

echo "== Introspect SNI menu =="
busctl --user introspect "$service" /MenuBar com.canonical.dbusmenu > "$MENU_LOG"
grep -F "GetLayout" "$MENU_LOG"
grep -F "Event" "$MENU_LOG"
grep -F "AboutToShow" "$MENU_LOG"

{
  echo "Artifact directory: $ARTIFACT_DIR"
  echo "Launch command: $LAUNCH_CMD"
  echo "App stdout log: $APP_STDOUT"
  echo "Menu introspection log: $MENU_LOG"
  echo "StatusNotifier service: $service"
  echo "SNI menu status: /MenuBar reachable after notification send"
} > "$SMOKE_SUMMARY"

cat "$SMOKE_SUMMARY"
echo "qz.App SNI menu smoke passed"
