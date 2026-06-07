# Packaging for Raspberry Pi (Raspbian / Raspberry Pi OS)

The app builds a native `.deb` (Debian package) via Compose Desktop + `jpackage`.
The `.deb` bundles a JRE, all module plugin JARs, **and** the web admin UI. One
process runs both the fullscreen mirror and the config server (on `:8080`), so
the Pi needs no separate Java install and the admin works out of the box.

## Important: build on the Pi (no cross-compilation)

`jpackage` only produces installers for the **OS and CPU architecture it runs
on**. You cannot build a Raspberry Pi `.deb` from macOS or x86 Linux. Build it
on the Pi itself, or on any **arm64** Linux machine / container.

Use **64-bit Raspberry Pi OS (arm64)** — the Compose renderer (Skiko) ships
`linux-arm64` natives but **not** 32-bit `armhf`. A Pi 3/4/5 on the 64-bit OS works.

## Prerequisites on the Pi

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk fakeroot binutils dpkg
java -version          # should report 17 (arm64)
```

`fakeroot` + `dpkg` are what `jpackage` uses to assemble the `.deb`.

## Build the package

```bash
git clone <your-repo> && cd Speculum
(cd config-server/web && npm install && npm run build)   # build the admin UI
./gradlew packageDeb
```

This runs, in order: build every module → copy the JARs + the web UI build into
the app resources (`bundlePlugins` / `bundleWeb`) → `jpackage` the app with a
bundled JRE and the embedded config server.

> Build the web UI first. If `config-server/web/dist` is missing the package
> still works as a mirror, but the admin page won't be served.

Output:

```
composeApp/build/compose/binaries/main/deb/speculum_1.0.0-1_arm64.deb
```

## Install & run

```bash
sudo apt install ./composeApp/build/compose/binaries/main/deb/speculum_1.0.0-1_arm64.deb
/opt/speculum/bin/speculum        # fullscreen mirror + admin on :8080
```

(Install path is `/opt/<packageName>`. A menu entry "Speculum" is also created.)

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

Two options depending on whether you run a desktop session or Pi OS Lite.

### A. systemd service (Pi OS Lite / headless — recommended)

A ready unit ships with every package. It uses [`cage`](https://github.com/cage-kiosk/cage),
a minimal Wayland kiosk compositor, to run the mirror fullscreen on the console
with no desktop environment.

```bash
sudo apt install -y cage          # or: sudo pacman -S cage

# Arch already installs the unit at /usr/lib/systemd/system/speculum.service.
# deb/rpm ship it under the app resources — copy it into place:
sudo cp /opt/speculum/lib/app/resources/speculum.service /etc/systemd/system/

sudoedit /etc/systemd/system/speculum.service   # set User= to your account (e.g. pi)
sudo systemctl daemon-reload
sudo systemctl enable --now speculum.service     # starts now + on every boot
journalctl -u speculum -f                         # follow logs
```

### B. Desktop (X11) autostart

With the full desktop session, autostart it instead:

```bash
mkdir -p ~/.config/autostart
cat > ~/.config/autostart/speculum.desktop <<'EOF'
[Desktop Entry]
Type=Application
Name=Speculum
Exec=/opt/speculum/bin/speculum
X-GNOME-Autostart-enabled=true
EOF
```

Hide the cursor and disable screen blanking for a true kiosk:

```bash
sudo apt install -y unclutter
# in ~/.config/lxsession/LXDE-pi/autostart or the autostart above:
#   @unclutter -idle 0
#   @xset s off; @xset -dpms; @xset s noblank
```

## Updating modules without rebuilding the app

Two options on the installed Pi:

1. **Drop-in folder** — run the app from a directory that contains a `plugins/`
   folder; JARs there are loaded (the working-dir fallback in `PluginLoader`).
   Handy for testing a new module JAR without re-packaging.
2. **Re-package** — add the module subproject (see `DEVELOPER_GUIDE.md`), then
   `./gradlew packageDeb` and reinstall.

## Customising the package

In `composeApp/build.gradle.kts` → `nativeDistributions`:

- `packageVersion` — bump for each release.
- `targetFormats(TargetFormat.Deb, TargetFormat.Rpm)` — builds `.deb` + `.rpm`
  (run `./gradlew packageDeb packageRpm`; `.rpm` needs `rpmbuild` installed).
- `linux { … }` — package name, menu group, icon. The icon is set from
  `iconFile.set(layout.projectDirectory.file("icons/speculum.png"))`
  (`composeApp/icons/speculum.png`, generated from `Images/speculum-icon.svg`).
- `includeAllModules = true` bundles a complete JRE (simplest). To shrink the
  image, remove it and list `modules(...)` explicitly, or use `suggestModules`
  (`./gradlew suggestModules`) to detect the needed JDK modules.

## Releases (CI)

[`.github/workflows/release.yml`](.github/workflows/release.yml) runs on every
`v*` tag push, in parallel:

- **`linux-deb-rpm`** (matrix: `arm64`, `amd64`) — builds the `.deb` **and**
  `.rpm` for each arch and attaches them to the GitHub Release.
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

```bash
docker run --rm --platform linux/arm64 -v "$PWD":/src -w /src \
  eclipse-temurin:17-jdk bash -c \
  "apt-get update && apt-get install -y fakeroot binutils dpkg && ./gradlew packageDeb"
```

(Uses QEMU emulation; slower, but produces the arm64 `.deb` without a physical Pi.)

## Low-power devices (Raspberry Pi 3B and similar)

Measured idle footprint: ~45 MB live heap, ~390 MB RSS with default JVM sizing,
a few % CPU (spiking when the clock redraws each second). On a 1 GB Pi 3B this is
tight and the VideoCore IV GPU has weak desktop-OpenGL support, so Compose/Skiko
likely renders in **software** — usable for static content, not smooth animation.

Already applied in `nativeDistributions.jvmArgs`: `-Xmx160m -XX:+UseSerialGC
-XX:MaxMetaspaceSize=96m` (keeps RSS roughly halved). Further steps for a 3B:

- **Turn off the seconds** — set the clock's `displaySeconds=false` (web admin)
  so it recomposes once a minute instead of every second. Biggest CPU win.
- Run **64-bit Pi OS Lite** with a minimal compositor (e.g. `cage`/Wayland or
  bare X), not the full desktop, to free RAM.
- Enable the KMS GL driver and give the GPU some memory (`raspi-config`) — may let
  Skiko use hardware GL instead of software.
- Prefer a **Pi 4 (2–4 GB)**; the 3B works but is marginal.

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