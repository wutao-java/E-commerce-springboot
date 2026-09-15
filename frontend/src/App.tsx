import { useEffect, useState } from "react";

import { ApiError, authApi } from "./api";
import { AdminApp } from "./components/AdminApp";
import { AuthScreen } from "./components/AuthScreen";
import { ShopApp } from "./components/ShopApp";
import type { User } from "./types";

export default function App() {
  const [user, setUser] = useState<User | null>();
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    authApi.me().then(setUser).catch(() => setUser(null));
  }, []);

  async function logout() {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  }

  function showNotice(message: string) {
    setNotice(message);
    window.setTimeout(() => setNotice(null), 2600);
  }

  function showError(requestError: unknown) {
    if (requestError instanceof ApiError && requestError.status === 401) {
      setUser(null);
      return;
    }
    setError(requestError instanceof Error ? requestError.message : "请求失败，请稍后重试");
    window.setTimeout(() => setError(null), 4200);
  }

  if (user === undefined) return <div className="startup-screen">正在连接本地商城...</div>;
  if (user === null) return <AuthScreen onAuthenticated={setUser} />;

  return (
    <>
      {user.role === "ADMIN"
        ? <AdminApp user={user} onLogout={logout} showNotice={showNotice} showError={showError} />
        : <ShopApp user={user} onUserChange={setUser} onLogout={logout} showNotice={showNotice} showError={showError} />}
      {(notice || error) && <div className={`toast ${error ? "error" : ""}`} role="status">{error || notice}</div>}
    </>
  );
}
