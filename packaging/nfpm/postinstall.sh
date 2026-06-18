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

# Staging dir the unprivileged mirror writes verified packages into, then the
# root oneshot reads. root-owned, group `pi`-writable (setgid). If the `pi`
# group is absent, fall back to root-only and the updater stays disabled.
if getent group pi >/dev/null 2>&1; then
    install -d -o root -g pi -m 2770 /var/lib/speculum/update
else
    install -d -o root -g root -m 0700 /var/lib/speculum/update
fi

# Not auto-enabled: speculum.service ships with User=pi and runs a fullscreen
# Wayland kiosk via cage, so it needs the operator to set the display user and
# install cage first. Point them at the one-time setup.
cat <<'EOF'
Speculum installed. Run it now with:  speculum
Kiosk on boot (Pi OS Lite / headless):
  sudo apt install cage              # or: sudo dnf install cage
  sudo systemctl edit --full speculum.service   # set User= to your account
  sudo systemctl enable --now speculum.service
EOF