#!/bin/sh
# Runs after remove/upgrade on both .deb (dpkg) and .rpm (rpm). The mirror is no
# longer a systemd service (it autostarts in the desktop session), so there's
# nothing to stop here — just refresh systemd for the removed updater unit.
set -e

if command -v systemctl >/dev/null 2>&1; then
    systemctl daemon-reload >/dev/null 2>&1 || true
fi

# Drop the install-format marker postinstall.sh writes. The package manager does
# not own that file, so without this /opt/speculum survives a purge:
#   dpkg: warning: while removing speculum, directory '/opt/speculum' not empty
# Only on a real removal — dpkg passes `remove`/`purge` and rpm passes `0`. An
# upgrade (dpkg `upgrade`, rpm `1`) must keep the marker: rpm runs the new
# package's %post *before* the old %postun, so deleting it there would leave the
# in-app updater with no marker to read.
case "${1:-}" in
    remove|purge|0)
        rm -f /opt/speculum/.install-format
        # Collapse the now-empty tree. rpm, unlike dpkg, leaves the package's
        # directories behind, so a plain `rmdir /opt/speculum` is not enough
        # there. Both forms only ever remove *empty* directories, so anything a
        # user or another package left under /opt/speculum is preserved.
        if command -v find >/dev/null 2>&1; then
            # `-exec … \;` (not `+`): each directory must be removed before
            # its parent is tested, or the parent still looks non-empty.
            find /opt/speculum -depth -type d -empty -exec rmdir {} \; 2>/dev/null || true
        else
            rmdir /opt/speculum 2>/dev/null || true
        fi
        ;;
esac
