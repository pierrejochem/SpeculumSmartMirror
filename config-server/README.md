# Config Server — web admin

Password-protected web UI + REST backend for editing the mirror configuration.
The desktop app and this server share one `config.json`; edit it here and the
mirror **reloads automatically** — it watches the file and reboots its engine
in-process on save (no restart).

- **Backend** — Ktor (Kotlin/JVM), `org.speculum.configserver`
- **Frontend** — React + Vite + TypeScript, in [`web/`](web)

## Run (development)

Two processes — backend (API) + Vite dev server (UI with hot reload):

```bash
# 1) backend on :8080 (from repo root, so it shares config.json + plugins/)
MIRROR_ADMIN_PASSWORD=secret ./gradlew :config-server:run

# 2) UI on :5173 (proxies /api → :8080)
cd config-server/web && npm install && npm run dev
# open http://localhost:5173
```

## Run (production / single origin)

Build the UI, then the backend serves it on `:8080`:

```bash
cd config-server/web && npm install && npm run build   # → web/dist
./gradlew :config-server:run                            # serves UI + API on :8080
# open http://localhost:8080
```

## Configuration (env vars)

| Var                     | Default                         | Purpose                              |
|-------------------------|---------------------------------|--------------------------------------|
| `MIRROR_ADMIN_PASSWORD` | `admin`                         | **Bootstrap** password (see below)   |
| `MIRROR_ADMIN_SECRET`   | built-in string                 | HMAC key for tokens — **set in prod**|
| `MIRROR_ADMIN_PORT`     | `8080`                          | HTTP port                            |
| `MIRROR_CONFIG`         | `config.json` (cwd)             | Config file (shared with the app)    |
| `MIRROR_PLUGINS`        | `plugins/` (or app resources)   | Where to scan modules for the picker |

## Password & security

Change the admin password from the UI (**Security** card) or via
`POST /api/password`. Once changed it is persisted as a salted
**PBKDF2-HMAC-SHA256** hash in `admin-auth.json`, next to the config file
(`~/.speculum/` by default) — the plaintext is never stored.

`MIRROR_ADMIN_PASSWORD` is only a **bootstrap**: it applies until the first
change writes `admin-auth.json`, after which the stored hash wins and the env var
is ignored. Delete `admin-auth.json` to revert to the bootstrap password. New
passwords need ≥ 4 characters. Changing the password does **not** revoke existing
tokens — they expire on their own 12 h TTL.

## API

| Method | Path            | Auth | Body / Result                                  |
|--------|-----------------|------|------------------------------------------------|
| POST   | `/api/login`    | —    | `{password}` → `{token}` (12h)                 |
| POST   | `/api/password` | yes  | `{currentPassword, newPassword}` → `{message}` |
| GET    | `/api/modules`  | yes  | discovered modules + default configs           |
| GET    | `/api/config`   | yes  | effective `MirrorConfig`                       |
| PUT    | `/api/config`   | yes  | save `MirrorConfig` → writes `config.json`     |

Auth = `Authorization: Bearer <token>` (HMAC-signed, time-limited).
`POST /api/password` → 401 if `currentPassword` wrong, 400 if `newPassword` too short.

## How config resolves

A **non-empty** `modules` list is authoritative. An **empty** list means "use
every installed plugin's default placement" — the out-of-box mirror. So the
admin's first load shows all modules; saving captures an explicit, editable set.