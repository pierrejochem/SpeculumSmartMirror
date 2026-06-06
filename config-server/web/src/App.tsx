import { useState } from "react";
import { getToken, clearToken } from "./api";
import { Login } from "./Login";
import { ConfigEditor } from "./ConfigEditor";

export function App() {
  const [authed, setAuthed] = useState(!!getToken());

  if (!authed) return <Login onLogin={() => setAuthed(true)} />;

  return (
    <ConfigEditor
      onLogout={() => {
        clearToken();
        setAuthed(false);
      }}
    />
  );
}