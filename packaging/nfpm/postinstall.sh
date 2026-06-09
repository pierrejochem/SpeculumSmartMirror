#!/bin/sh
# Runs after install/upgrade on both .deb (apt/dpkg) and .rpm (dnf/rpm).
set -e

# Pick up the newly installed unit. Guard the call so the package still installs
# on systems without systemd (e.g. a container or a non-systemd init).
if command -v systemctl >/dev/null 2>&1; then
    systemctl daemon-reload >/dev/null 2>&1 || true
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