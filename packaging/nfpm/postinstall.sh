#!/bin/sh
# Runs after install/upgrade on both .deb (apt/dpkg) and .rpm (dnf/rpm). nfpm
# uses one script for both packagers, so we detect deb-vs-rpm from the host's
# package tooling (see the format marker below) rather than an arg.
set -e

# Pick up the newly installed unit. Guard the call so the package still installs
# on systems without systemd (e.g. a container or a non-systemd init).
if command -v systemctl >/dev/null 2>&1; then
    systemctl daemon-reload >/dev/null 2>&1 || true
fi

# --- in-app updater wiring ---------------------------------------------------
# Format marker the privileged updater reads to pick apt vs dnf. dpkg-based
# systems have `dpkg`; rpm-based have `rpm` (and not dpkg). Default to rpm.
if command -v dpkg >/dev/null 2>&1; then
    echo deb > /opt/speculum/.install-format
else
    echo rpm > /opt/speculum/.install-format
fi

# Make the privileged helper executable (nfpm preserves mode, but be explicit).
[ -f /opt/speculum/libexec/speculum-apply-update ] && \
    chmod 0755 /opt/speculum/libexec/speculum-apply-update || true

# The updater stages packages under the mirror user's own ~/.speculum/update at
# runtime — no root-owned staging dir to provision here.

cat <<'EOF'
Speculum installed. Run it now with:  speculum
Kiosk on desktop login (autostart fullscreen):
  speculum-kiosk-enable        # run as your desktop user, then log out/in
  speculum-kiosk-disable       # to undo
EOF