import { useEffect, useState } from "react";
import { api, UpdateProgress, UpdateStatus } from "./api";

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/**
 * One-click updater. Shows whether a newer signed release exists for this
 * install and, when it does, downloads + verifies + installs it via the backend.
 * The install restarts the mirror (and this server) — we expect the connection
 * to drop and reconnect via /api/version on the new version.
 */
export function UpdatesCard() {
  const [status, setStatus] = useState<UpdateStatus | null>(null);
  const [progress, setProgress] = useState<UpdateProgress | null>(null);
  const [running, setRunning] = useState(false);
  const [reconnecting, setReconnecting] = useState(false);
  const [done, setDone] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    api.getUpdateStatus().then(setStatus).catch((e) => setError(e.message));
  }, []);

  async function runUpdate() {
    setError("");
    setDone("");
    try {
      await api.startUpdate(); // 202, or throws on 409 / not-updatable
    } catch (e: any) {
      setError(e.message || "Could not start the update");
      return;
    }
    setRunning(true);

    let target = status?.latestVersion ?? null;

    // Phase 1: poll progress until the process is about to restart (or ends).
    while (true) {
      await sleep(1000);
      let p: UpdateProgress;
      try {
        p = await api.getUpdateProgress();
      } catch {
        // Server may already be restarting — move to reconnect.
        break;
      }
      setProgress(p);
      if (p.targetVersion) target = p.targetVersion;
      if (p.phase === "error") {
        setError(p.message || "Update failed");
        setRunning(false);
        return;
      }
      if (p.phase === "installed") {
        // Manual launch (no service): installed, but the old process lives on.
        setDone(p.message || "Update installed — relaunch Speculum to apply.");
        setRunning(false);
        return;
      }
      if (p.phase === "installing" || p.phase === "restarting") break;
    }

    // Phase 2: the mirror is restarting — wait for the new version to answer.
    setReconnecting(true);
    const deadline = Date.now() + 90_000;
    while (Date.now() < deadline) {
      await sleep(2000);
      try {
        const v = await api.getVersion();
        if (target && v.version === target) {
          setDone(`Updated to v${target}.`);
          setRunning(false);
          setReconnecting(false);
          return;
        }
      } catch {
        // Expected while the server is down mid-restart — keep waiting.
      }
    }
    setError("Update is taking longer than expected — check the mirror.");
    setRunning(false);
    setReconnecting(false);
  }

  function body() {
    if (error && !status) return <span className="error small">{error}</span>;
    if (!status) return <span className="muted small">Checking for updates…</span>;

    if (running || reconnecting) {
      const pct = progress?.pct;
      const label = reconnecting
        ? "Restarting — this page will reconnect automatically…"
        : progress?.message || "Working…";
      return (
        <div className="update-busy" aria-live="polite">
          <span>{label}</span>
          {typeof pct === "number" && <span className="muted small"> {pct}%</span>}
        </div>
      );
    }

    if (done) return <span className="ok small">{done}</span>;
    if (error) return <span className="error small">{error}</span>;

    if (status.updatable && status.latestVersion) {
      return (
        <div className="update-row" aria-live="polite">
          <span>
            Update available: <strong>v{status.currentVersion}</strong> →{" "}
            <strong>v{status.latestVersion}</strong>
          </span>
          <button onClick={runUpdate}>Update now</button>
        </div>
      );
    }

    // Not updatable: explain why, link to the release when one exists.
    if (status.updateAvailable) {
      return (
        <span className="muted small">
          {status.reason || "A newer release is available."}{" "}
          {status.releaseUrl && (
            <a href={status.releaseUrl} target="_blank" rel="noreferrer">View release</a>
          )}
        </span>
      );
    }
    return <span className="muted small">Up to date (v{status.currentVersion}).</span>;
  }

  return (
    <section className="card">
      <h2>Updates</h2>
      {body()}
    </section>
  );
}
