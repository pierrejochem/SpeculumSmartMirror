#!/bin/sh
# Runs after remove/upgrade on both .deb (dpkg) and .rpm (rpm). The mirror is no
# longer a systemd service (it autostarts in the desktop session), so there's
# nothing to stop here — just refresh systemd for the removed updater unit.
set -e

if command -v systemctl >/dev/null 2>&1; then
    systemctl daemon-reload >/dev/null 2>&1 || true
fi
