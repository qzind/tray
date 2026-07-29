#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

# Manual desktop smoke for native Linux notifications
# This needs a live user session bus and notification daemon
# Should be ideally kept out of automated tests because CI may be headless

ANT_HOME="${ANT_HOME:-/usr/share/ant}"
MONITOR_LOG="${MONITOR_LOG:-/tmp/qz-linux-notifications-smoke.log}"

# Use a real QZ image so desktop daemons exercise appIcon handling
# Override this when checking distro-specific icon behavior
ICON_PATH="${ICON_PATH:-$(pwd)/src/qz/ui/resources/qz-default-32.png}"

# Compile checks are part of the work item, but not this smoke
# This script talks to d-bus directly and does not load QZ classes
# Commands left here so paired checks can be run if needed
# "$ANT_HOME/bin/ant" compile-socket
# "$ANT_HOME/bin/ant" compile-tests

echo "== Notification service =="
# Fail early when the desktop has no Freedesktop notification service
busctl --user list | grep -F "org.freedesktop.Notifications"

# Fail early when the selected icon resource cannot be sent
test -f "$ICON_PATH"

echo "== D-Bus Notify smoke =="
rm -f "$MONITOR_LOG"

# Capture the real Notify payload instead of trusting busctl output alone
timeout 8s dbus-monitor --session \
  "type='method_call',interface='org.freedesktop.Notifications',member='Notify'" \
  > "$MONITOR_LOG" 2>&1 &
monitor_pid=$!

sleep 1

# Exercise the same Freedesktop Notify contract used by LinuxNotifications
busctl --user call \
  org.freedesktop.Notifications \
  /org/freedesktop/Notifications \
  org.freedesktop.Notifications \
  Notify \
  susssasa{sv}i \
  "QZ Tray" \
  0 \
  "$ICON_PATH" \
  "QZ Tray" \
  "Linux notification smoke test" \
  0 \
  0 \
  -- \
  -1

sleep 1
kill "$monitor_pid" 2>/dev/null || true
wait "$monitor_pid" 2>/dev/null || true

# Prove the notification call, icon path, and body reached D-Bus
grep -F "member=Notify" "$MONITOR_LOG"
grep -F "$ICON_PATH" "$MONITOR_LOG"
grep -F "Linux notification smoke test" "$MONITOR_LOG"

echo "Notification smoke passed"