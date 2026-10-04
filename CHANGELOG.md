# Changelog

All notable changes to this project are documented here.
This file is generated from [Conventional Commits](https://www.conventionalcommits.org).
## 1.5.4 — 2026-10-04

### Documentation
- Update for v1.5.3

## 1.5.3 — 2026-10-04

### Bug Fixes
- Stop the timer deleting a download that is still in flight

### Documentation
- Update for v1.5.2

## 1.5.2 — 2026-10-04

### Documentation
- Update for v1.5.1

## 1.5.1 — 2026-10-04

### Documentation
- Update for v1.5.0

## 1.5.0 — 2026-10-04

### Features
- Apply staged updates from a root timer, not a polkit grant

### Bug Fixes
- Refuse a signed downgrade unless root opts in
- Report why the privileged trigger failed, and never hang in restarting

### Documentation
- Update for v1.4.1

## 1.4.1 — 2026-10-04

### Bug Fixes
- Surface update failures that happen after the install starts

### Documentation
- Regenerate the changelog and record 1.4.0 as the published version

## 1.4.0 — 2026-10-04

### Bug Fixes
- Point catalog at Example Module v1.1.0 and show plugin version

### Documentation
- Explain settingsSchema() for module authors
- Record the published mirror-api and app version as 1.3.0

## 1.3.0 — 2026-10-04

### Features
- Declare module settings in plugin code, not the admin console

### Documentation
- Correct the stale repository URL in the social preview image
- Add canonical URLs, sitemap, structured data; fix stale repo links

## 1.2.4 — 2026-10-04

### Bug Fixes
- Clean up /opt/speculum on uninstall

## 1.2.3 — 2026-10-04

### Bug Fixes
- Dedupe distribution jars for Compose 1.12

## 1.2.1 — 2026-07-24

### Documentation
- Refresh config editor screenshot for the redesigned admin UI

## 1.2.0 — 2026-07-05

### Features
- Optimize ui
- Add plugin store

### Bug Fixes
- Remove deprecated code
- Correct plugin url and improve error handling

### Documentation
- Add runbook to pages

## 1.1.0 — 2026-06-23

### Features
- Change kiosk auto start and introduce kiosk helpers **[breaking]**

### Bug Fixes
- Handle restart more robust
- Drop ProtectHome from updater unit so it can write home staging
- Prevent updater download timeout on large packages

### Refactor
- Extract example-module into standalone repo **[breaking]**
- Extract example-module into standalone repo **[breaking]**

### Other
- Remove github link from navigation
- Update github pages content to current state

## 1.0.0 — 2026-06-19

### Features
- Change kiosk auto start and introduce kiosk helpers **[breaking]**

### Other
- Remove github link from navigation
- Update github pages content to current state

## 0.5.6 — 2026-06-18

### Bug Fixes
- Handle restart more robust
- Drop ProtectHome from updater unit so it can write home staging

## 0.5.4 — 2026-06-18

### Bug Fixes
- Prevent updater download timeout on large packages

## 0.5.2 — 2026-06-18

### Bug Fixes
- Solve repository name and permission issue with updater download

## 0.5.0 — 2026-06-18

### Features
- One-click updater

### Bug Fixes
- Solve JcaPGPKeyPair' is deprecated

## 0.4.2 — 2026-06-18

### Bug Fixes
- Improve version check logic

## 0.4.1 — 2026-06-14

### Bug Fixes
- Use non-reserved var for arch pkgver in release workflow

## 0.4.0 — 2026-06-14

### Features
- Create changelog via conventional commits
- Improve config-server card design
- Improve responsiveness for config-server ui
- Improve accessibility in config-server

## 0.3.6 — 2026-06-14

### Bug Fixes
- Correct repository url after renaming
- Adjust news text to be in 2 lines and centered

## 0.3.4 — 2026-06-12

### Features
- Improve text in modules

## 0.3.3 — 2026-06-09

### Bug Fixes
- Solve startup failure

## 0.3.2 — 2026-06-09

### Bug Fixes
- Pam unable to dlopen
- Unit has no user session and cage aborts

## 0.3.1 — 2026-06-09

### Bug Fixes
- Create missing dir for package build

## 0.3.0 — 2026-06-09

### Features
- Optimize installation packages
- Add github pages documentation

### Bug Fixes
- Add software render

## 0.2.0 — 2026-06-07

### Features
- Add more platforms and fingerprint security validation

## 0.1.0 — 2026-06-07

### Features
- Auto detect version
- Show current version in update notifier
- Add update notifier module
- Add arch linux arm64 build
- Add arch linux arm64 build
- Add LICENSE.md
- Responsiveness optimizations

### Bug Fixes
- Adjust path for arch
- Disable pacman sandbox
- Optimize and fix arm64 builds
- Remove warnings and deprecations

### Other
- Bulk dependencies update

## 0.0.1 — 2026-06-06


