#!/bin/sh
# Runs before remove/upgrade on both .deb (dpkg) and .rpm (rpm).
#
# Disarm the updater timer while its unit file still exists — `systemctl
# disable` after the file is gone cannot clean up, and would leave a dangling
# symlink in timers.target.wants. An upgrade must keep it enabled (dpkg passes
# `upgrade`, rpm passes `1`); postinstall re-arms it either way.
set -e

case "${1:-}" in
    remove|purge|0)
        if command -v systemctl >/dev/null 2>&1; then
            systemctl disable --now speculum-update.timer >/dev/null 2>&1 || true
        fi
        ;;
esac
