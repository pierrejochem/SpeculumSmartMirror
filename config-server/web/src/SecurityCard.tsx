import { useState } from "react";
import { api } from "./api";

const MIN_LEN = 4;

/** Admin password change. Posts to /api/password; the new password takes effect
 *  immediately (existing session token stays valid). */
export function SecurityCard() {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");

  const mismatch = confirm.length > 0 && next !== confirm;
  const tooShort = next.length > 0 && next.length < MIN_LEN;
  const canSubmit =
    !busy && current && next.length >= MIN_LEN && next === confirm;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!canSubmit) return;
    setBusy(true);
    setStatus("");
    setError("");
    try {
      const res = await api.changePassword(current, next);
      setStatus(res.message);
      setCurrent("");
      setNext("");
      setConfirm("");
    } catch (e: any) {
      setError(e.message || "Could not change password");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card">
      <h2>Security</h2>
      <form className="pw" onSubmit={submit}>
        <div className="row">
          <label>Current password
            <input type="password" autoComplete="current-password"
              value={current} onChange={(e) => setCurrent(e.target.value)} />
          </label>
          <label>New password
            <input type="password" autoComplete="new-password"
              value={next} onChange={(e) => setNext(e.target.value)} />
          </label>
          <label>Confirm new
            <input type="password" autoComplete="new-password"
              value={confirm} onChange={(e) => setConfirm(e.target.value)} />
          </label>
        </div>
        <div className="pw-foot">
          <button disabled={!canSubmit}>{busy ? "…" : "Change password"}</button>
          {tooShort && <span className="muted small">Minimum {MIN_LEN} characters.</span>}
          {mismatch && <span className="error small">Passwords don’t match.</span>}
          {status && <span className="ok small">{status}</span>}
          {error && <span className="error small">{error}</span>}
        </div>
      </form>
    </section>
  );
}