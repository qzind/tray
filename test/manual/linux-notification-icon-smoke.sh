#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

# Manual icon matrix for Freedesktop notification appIcon
# This checks what the desktop daemon actually renders

STAMP="${STAMP:-$(date +%Y%m%d-%H%M%S)}"
ARTIFACT_DIR="${ARTIFACT_DIR:-/tmp/qz-notification-icon-smoke-$STAMP}"
MONITOR_LOG="$ARTIFACT_DIR/dbus-monitor.log"
SUMMARY="$ARTIFACT_DIR/summary.txt"
PNG_PATH="$(pwd)/src/qz/ui/resources/qz-default-32.png"
SVG_PATH="$(pwd)/src/qz/ui/resources/qz-tray-symbolic.svg"
PAUSE_SECONDS="${PAUSE_SECONDS:-4}"
EXPIRE_TIMEOUT="${EXPIRE_TIMEOUT:--1}"
# Keep monitor alive long enough for all delayed variants
MONITOR_SECONDS="${MONITOR_SECONDS:-$((PAUSE_SECONDS * 6 + 8))}"

# Keep the generated logs in tmp
mkdir -p "$ARTIFACT_DIR"

echo "== Notification server =="
# Record the daemon because icon behavior is implementation-specific
SERVER_INFO="$(busctl --user call \
  org.freedesktop.Notifications \
  /org/freedesktop/Notifications \
  org.freedesktop.Notifications \
  GetServerInformation)"
echo "$SERVER_INFO"

# Fail early if the resources being compared are missing
test -f "$PNG_PATH"
test -f "$SVG_PATH"

echo "== Start D-Bus monitor =="
# Capture appIcon values actually sent over D-Bus
timeout "$MONITOR_SECONDS"s dbus-monitor --session \
  "type='method_call',interface='org.freedesktop.Notifications',member='Notify'" \
  > "$MONITOR_LOG" 2>&1 &
monitor_pid=$!

cleanup() {
  kill "$monitor_pid" 2>/dev/null || true
  wait "$monitor_pid" 2>/dev/null || true
}
trap cleanup EXIT

send_notify() {
  local icon="$1"
  local label="$2"

  # Use distinct labels so visual comparison is easy
  busctl --user call \
    org.freedesktop.Notifications \
    /org/freedesktop/Notifications \
    org.freedesktop.Notifications \
    Notify \
    susssasa{sv}i \
    "QZ Tray" \
    0 \
    "$icon" \
    "QZ Tray $label" \
    "Icon feasibility smoke: $label" \
    0 \
    0 \
    -- \
    "$EXPIRE_TIMEOUT"

  sleep "$PAUSE_SECONDS"
}

sleep 1

echo "== Send icon variants =="
# Current conservative appIcon choice
send_notify "$PNG_PATH" "PNG path"
# Spec-shaped form of the same PNG file
send_notify "file://$PNG_PATH" "PNG file URI"
# Direct SVG file path for daemon rendering checks
send_notify "$SVG_PATH" "SVG path"
# Spec-shaped SVG file URI
send_notify "file://$SVG_PATH" "SVG file URI"
# Bare themed name tests daemon icon-theme resolution
send_notify "qz-tray-symbolic" "symbolic name"

cleanup
trap - EXIT

# Prove each variant reached the notification daemon
grep -F "$PNG_PATH" "$MONITOR_LOG"
grep -F "file://$PNG_PATH" "$MONITOR_LOG"
grep -F "$SVG_PATH" "$MONITOR_LOG"
grep -F "file://$SVG_PATH" "$MONITOR_LOG"
grep -F "qz-tray-symbolic" "$MONITOR_LOG"

{
  echo "Artifact directory: $ARTIFACT_DIR"
  echo "Notification server: $SERVER_INFO"
  echo "PNG path: $PNG_PATH"
  echo "PNG URI: file://$PNG_PATH"
  echo "SVG path: $SVG_PATH"
  echo "SVG URI: file://$SVG_PATH"
  echo "Symbolic name: qz-tray-symbolic"
  echo "Pause seconds: $PAUSE_SECONDS"
  echo "Expire timeout: $EXPIRE_TIMEOUT"
  echo "Monitor seconds: $MONITOR_SECONDS"
  echo "D-Bus monitor log: $MONITOR_LOG"
  echo "Visual check: inspect rendered icons during the run"
} > "$SUMMARY"

cat "$SUMMARY"
echo "Notification icon smoke passed"