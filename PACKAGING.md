# Packaging for Raspberry Pi (Raspbian / Raspberry Pi OS)

The app builds a native `.deb` (Debian package) from a Compose Desktop + `jpackage`
app-image, packaged with [`nfpm`](https://nfpm.goreleaser.com). The `.deb` bundles
a JRE, all module plugin JARs, **and** the web admin UI. One process runs both the
fullscreen mirror and the config server (on `:8080`), so the Pi needs no separate
Java install and the admin works out of the box.

## Important: build on the Pi (no cross-compilation)

`jpackage` only builds the app-image for the **OS and CPU architecture it runs
on**, so you cannot build a Raspberry Pi `.deb` from macOS or x86 Linux. Build it
on the Pi itself, or on any **arm64** Linux machine / container. (nfpm wrapping
the image is pure-Go and arch-agnostic, but the JRE inside the image is not.)

Use **64-bit Raspberry Pi OS (arm64)** — the Compose renderer (Skiko) ships
`linux-arm64` natives but **not** 32-bit `armhf`. A Pi 3/4/5 on the 64-bit OS works.

## Prerequisites on the Pi

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version          # should report 17 (arm64)
```

That JDK is all `createDistributable` needs. nfpm builds the `.deb`/`.rpm` in
pure Go — no `fakeroot`/`dpkg`/`rpmbuild` — install it from
<https://nfpm.goreleaser.com/install/> (a single static binary).

## Build the package

The `.deb`/`.rpm` are built in two steps: `jpackage` produces the self-contained
**app-image** (launcher + bundled JRE + plugins + web admin under `/opt/speculum`),
then [`nfpm`](https://nfpm.goreleaser.com) wraps that image into a `.deb` and a
`.rpm`. nfpm — not jpackage's installer — adds the `/usr/bin/speculum` symlink
(so the launcher is on `PATH`), installs the systemd unit at its proper path,
and runs the post-install scripts. This is the same app-image the Arch `PKGBUILD`
wraps, so all three formats behave alike.

```bash
git clone <your-repo> && cd Speculum
(cd config-server/web && npm install && npm run build)   # build the admin UI
./gradlew :composeApp:createDistributable                # → the app-image

# install nfpm once (https://nfpm.goreleaser.com/install/), then:
PKG_VERSION=1.0.0 PKG_ARCH=arm64 \
  nfpm pkg --config packaging/nfpm/nfpm.yaml --packager deb --target dist/
PKG_VERSION=1.0.0 PKG_ARCH=arm64 \
  nfpm pkg --config packaging/nfpm/nfpm.yaml --packager rpm --target dist/
```

`createDistributable` first builds every module → copies the JARs + the web UI
build into the app resources (`bundlePlugins` / `bundleWeb`) → assembles the
app-image with a bundled JRE and the embedded config server. `PKG_ARCH` is the
Debian arch (`arm64` / `amd64`); nfpm maps it to the rpm arch automatically.

> Build the web UI first. If `config-server/web/dist` is missing the package
> still works as a mirror, but the admin page won't be served.

Output:

```
dist/speculum_1.0.0_arm64.deb
dist/speculum-1.0.0.aarch64.rpm
```

(`./gradlew packageDeb` still works for ad-hoc local builds, but its output goes
to `/opt/speculum/bin/speculum` only — no `PATH` symlink and no systemd unit
installed. Use the nfpm flow for anything you ship.)

### Validate the nfpm config locally

If `nfpm` is installed you can sanity-check `nfpm.yaml` — and the symlink,
systemd unit and scripts it injects — without an app-image present. The
`${PKG_ARCH}`/`${PKG_VERSION}` env vars must be set or nfpm errors on the empty
fields:

```bash
# Parse + validate the schema only (no package written). Fails on bad keys,
# missing required fields, or unresolved env vars.
PKG_VERSION=0.0.0 PKG_ARCH=arm64 \
  nfpm package --config packaging/nfpm/nfpm.yaml --packager deb --target /dev/null
```

`createDistributable` need not have run for this — nfpm validates the config
before reading the `contents` sources, so it reports config errors immediately.
To also confirm the *contents* (the `/opt/speculum` tree, symlink, unit), build
the app-image first, produce a real `.deb`, then inspect it:

```bash
./gradlew :composeApp:createDistributable
PKG_VERSION=0.0.0 PKG_ARCH=arm64 \
  nfpm package --config packaging/nfpm/nfpm.yaml --packager deb --target dist/

dpkg-deb -c dist/speculum_0.0.0_arm64.deb     # list files → expect /usr/bin/speculum
                                              #   symlink + the .service unit
dpkg-deb -e dist/speculum_0.0.0_arm64.deb /tmp/spec-ctl && cat /tmp/spec-ctl/postinst
```

(No local `nfpm`? Install it from <https://nfpm.goreleaser.com/install/>, or just
push a `v*` tag and let the `linux-deb-rpm` CI job build + validate.)

## Install & run

```bash
sudo apt install ./dist/speculum_1.0.0_arm64.deb     # or: sudo dnf install ./dist/speculum-1.0.0.aarch64.rpm
speculum        # now on PATH — fullscreen mirror + admin on :8080
```

(Install path is `/opt/speculum`; nfpm symlinks `/usr/bin/speculum` to it. A menu
entry "Speculum" is also created.)

One launch starts **both** the mirror and the config web admin. Open the admin
from any device on the network at `http://<pi-ip>:8080` (default password
`admin`). Saving there hot-reloads the running mirror — no restart.

- Plugin JARs + web UI load from `compose.application.resources.dir`
  (`/opt/speculum/lib/app/resources/{plugins,web}`), independent of cwd.
- The config lives at `~/.speculum/config.json` (writable; shared by the
  mirror and the admin). Override with `-Dmirror.config=…` or `MIRROR_CONFIG`.
- Set the admin password via `MIRROR_ADMIN_PASSWORD`; disable the server
  entirely with `MIRROR_ADMIN_DISABLED=1`.

## Run on boot (kiosk)

The mirror runs as a **desktop-session autostart** — it launches fullscreen when
your desktop logs in. This is the most reliable kiosk on Raspberry Pi OS: the
session already owns the display, seat and runtime dir, so there's none of the
seat / `XDG_RUNTIME_DIR` fragility of a bare Wayland compositor. Every package
ships a one-shot helper:

```bash
# run as your normal desktop user (NOT root)
speculum-kiosk-enable        # installs a per-user XDG autostart entry
# log out and back in (or reboot) — the mirror comes up fullscreen

speculum-kiosk-disable       # undo
```

`speculum-kiosk-enable` writes `~/.config/autostart/speculum.desktop` pointing at
`/opt/speculum/libexec/speculum-kiosk-run`, a wrapper that also disables screen
blanking and hides the idle cursor (X11/Xwayland, best-effort) before launching
the mirror. Enable **desktop autologin** (`raspi-config` → System Options → Boot /
Auto Login → *Desktop Autologin*) so the kiosk comes up unattended after a reboot.

The entry is honoured by the Pi OS desktops (LXDE and the Wayland `labwc` /
`wayfire` sessions) via the XDG autostart spec; Speculum is an X11/AWT app and
runs through Xwayland automatically on the Wayland sessions. On a pure Wayland
compositor the `xset`/`unclutter` hardening doesn't apply — disable blanking in
the compositor or in `raspi-config` → Display Options instead.

## Updating modules without rebuilding the app

Two options on the installed Pi:

1. **Drop-in folder** — run the app from a directory that contains a `plugins/`
   folder; JARs there are loaded (the working-dir fallback in `PluginLoader`).
   Handy for testing a new module JAR without re-packaging.
2. **Re-package** — add the module subproject (see `DEVELOPER_GUIDE.md`), then
   `./gradlew packageDeb` and reinstall.

## Customising the package

The `.deb`/`.rpm` metadata, files, symlinks, dependencies and maintainer scripts
live in **[`packaging/nfpm/nfpm.yaml`](packaging/nfpm/nfpm.yaml)** (plus
`postinstall.sh` / `postremove.sh` beside it) — edit there to change the install
layout, add a file, or adjust per-distro `depends`.

The app-image itself is configured in `composeApp/build.gradle.kts` →
`nativeDistributions`:

- `linux { … }` — menu group, icon. The icon is set from
  `iconFile.set(layout.projectDirectory.file("icons/speculum.png"))`
  (`composeApp/icons/speculum.png`, generated from `Images/speculum-icon.svg`).
- `includeAllModules = true` bundles a complete JRE (simplest). To shrink the
  image, remove it and list `modules(...)` explicitly, or use `suggestModules`
  (`./gradlew suggestModules`) to detect the needed JDK modules.
- `appVersion` (top of the file) sets the running app's reported version; the
  *package* version comes from `PKG_VERSION` passed to nfpm (the release tag in
  CI). Keep them in sync for releases.

## Releases (CI)

[`.github/workflows/release.yml`](.github/workflows/release.yml) runs on every
`v*` tag push, in parallel:

- **`linux-deb-rpm`** (matrix: `arm64`, `amd64`) — builds the app-image
  (`createDistributable`) then wraps it with nfpm into the `.deb` **and** `.rpm`
  for each arch and attaches them to the GitHub Release.
- **`arch`** (matrix: `aarch64`, `x86_64`) — builds the Arch `.pkg.tar.*` (via
  the PKGBUILD) for each arch and attaches it.
- **`publish-mirror-api`** — publishes `org.speculum:mirror-api:<tag-without-v>`
  to **GitHub Packages** (`maven.pkg.github.com`) using the workflow's
  `GITHUB_TOKEN` (`packages: write`). See the README's *Using `mirror-api` as a
  dependency* section for consumer setup.
- **`checksums`** — after the package jobs, writes a `SHA256SUMS` over every
  package and attaches it (plus detached GPG signatures — see below).

Every package arch builds on its **own native runner** — `ubuntu-24.04-arm` for
arm64/aarch64, `ubuntu-latest` for amd64/x86_64 (both free for public repos).
jpackage can't cross-compile, and native runners avoid QEMU (whose emulated
`systemd-detect-virt` crashes pacman).

So `git tag v1.2.3 && git push --tags` produces, for both arm64 and x86-64: a
`.deb`, an `.rpm`, and an Arch package — plus the published API artifact. Bump
`appVersion` in `composeApp/build.gradle.kts` to match the tag for the package
filenames.

## Verifying downloads

Each release ships a `SHA256SUMS` file. Verify an asset:

```bash
sha256sum -c SHA256SUMS --ignore-missing
```

If repository secrets `GPG_PRIVATE_KEY` (ASCII-armored private key) and
`GPG_PASSPHRASE` are set, the `checksums` job also attaches a detached
`.asc` signature for every asset and for `SHA256SUMS`. Verify with your public
key imported:

```bash
gpg --verify SHA256SUMS.asc SHA256SUMS
gpg --verify speculum_1.2.3-1_arm64.deb.asc speculum_1.2.3-1_arm64.deb
```

Without those secrets the build still succeeds — it just skips signing and ships
checksums only.

## Arch Linux ARM (aarch64)

A [`PKGBUILD`](packaging/arch/PKGBUILD) builds a pacman package. Compose Desktop
has no native Arch target, so it installs the **app-image** (`createDistributable`:
launcher + bundled JRE + module plugins + web admin) under `/opt/speculum`, with a
`/usr/bin/speculum` symlink, a `.desktop` entry and the icon.

Build **on an aarch64 Arch box** (jpackage/Skiko can't cross-compile):

```bash
cd packaging/arch
makepkg -si          # builds + installs speculum-<ver>-1-aarch64.pkg.tar.zst
```

`makepkg` pulls the tagged source tarball, runs `npm run build` for the admin UI,
then `:composeApp:createDistributable`. Needs `jdk17-openjdk nodejs npm git`
(listed as `makedepends`); the runtime deps (`fontconfig`, X libs) are declared in
the PKGBUILD. Bump `pkgver` to the release tag you're packaging.

Run it the same way as the `.deb`: `speculum` (or the menu entry) starts the
fullscreen mirror + admin on `:8080`; config lives at `~/.speculum/config.json`.

## Build on x86 with Docker (alternative to building on the Pi)

Build the arm64 app-image under emulation, then run nfpm on the host (it's
arch-agnostic):

```bash
docker run --rm --platform linux/arm64 -v "$PWD":/src -w /src \
  eclipse-temurin:17-jdk bash -c \
  "(cd config-server/web && npm ci && npm run build) && ./gradlew :composeApp:createDistributable"

PKG_VERSION=1.0.0 PKG_ARCH=arm64 \
  nfpm pkg --config packaging/nfpm/nfpm.yaml --packager deb --target dist/
```

(The build uses QEMU emulation; slower, but produces the arm64 `.deb` without a
physical Pi.)

## Low-power devices (Raspberry Pi 3B and similar)

Measured idle footprint: ~45 MB live heap, ~390 MB RSS with default JVM sizing,
a few % CPU (spiking when the clock redraws each second). On a 1 GB Pi 3B this is
tight and the VideoCore IV GPU has weak desktop-OpenGL support, so Compose/Skiko
likely renders in **software** — usable for static content, not smooth animation.

Already applied in `nativeDistributions.jvmArgs`: `-Xmx160m -XX:+UseSerialGC
-XX:MaxMetaspaceSize=96m` (keeps RSS roughly halved). Further steps for a 3B:

- **Turn off the seconds** — set the clock's `displaySeconds=false` (web admin)
  so it recomposes once a minute instead of every second. Biggest CPU win.
- Use a **lightweight desktop** (LXDE, or the Wayland `labwc` session) rather than
  a heavy one — the mirror autostarts into it.
- Enable the KMS GL driver and give the GPU some memory (`raspi-config`) — may let
  Skiko use hardware GL instead of software.
- Prefer a **Pi 4 (2–4 GB)**; the 3B works but is marginal.

## Emulating Raspbian Bullseye with QEMU (arm64, raspi3b) on Ubuntu

https://github.com/KhalilOuali/Raspbian-Bullseye-with-QEMU

## Troubleshooting

**`Value '…/corretto-21…/Home' given for org.gradle.java.home … is invalid`**
A machine-specific JDK path leaked into the build (it must not be committed in
the project `gradle.properties`). Put it only in your *user* config:
`~/.gradle/gradle.properties` → `org.gradle.java.home=/path/to/jdk`. The project
file stays machine-agnostic, and containers/Pis use their own JDK 17. Inside the
Docker container the `eclipse-temurin:17` JDK is used automatically — no override
needed (and don't mount your host `~/.gradle`).

**Gradle won't start (unsupported class file / needs JDK ≤ 21)**
Gradle 8.9 runs on JDK 17–21. If your default `java` is newer, point
`org.gradle.java.home` (user config) or `JAVA_HOME` at a 17/21 JDK.