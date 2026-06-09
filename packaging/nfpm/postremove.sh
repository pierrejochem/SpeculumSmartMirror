#!/bin/sh
# Runs after remove/upgrade on both .deb (dpkg: $1 = remove|purge|upgrade) and
# .rpm (rpm: $1 = 0 on final removal, 1 on upgrade). Only tear the service down
# on a real removal — never on an upgrade, which would stop a running mirror.
set -e

case "$1" in
    upgrade|1)
        # Upgrade: leave the (possibly running/enabled) service alone.
        ;;
    *)
        if command -v systemctl >/dev/null 2>&1; then
            systemctl disable --now speculum.service >/dev/null 2>&1 || true
        fi
        ;;
esac

if command -v systemctl >/dev/null 2>&1; then
    systemctl daemon-reload >/dev/null 2>&1 || true
fi