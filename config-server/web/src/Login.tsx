import { useState } from "react";
import { api, setToken } from "./api";
import { Wordmark } from "./Logo";

export function Login({ onLogin }: { onLogin: () => void }) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const { token } = await api.login(password);
      setToken(token);
      onLogin();
    } catch {
      setError("Invalid password");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="center">
      <form className="card login" onSubmit={submit}>
        <Wordmark />
        <p className="muted small">Configuration admin</p>
        <input
          type="password"
          placeholder="Password"
          value={password}
          autoFocus
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && <div className="error">{error}</div>}
        <button disabled={busy || !password}>{busy ? "…" : "Sign in"}</button>
      </form>
    </div>
  );
}