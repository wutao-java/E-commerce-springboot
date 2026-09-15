import { FormEvent, useState } from "react";
import { ArrowRight, LockKeyhole, PackageCheck, ShieldCheck, Store, UserPlus, WalletCards } from "lucide-react";

import { authApi } from "../api";
import type { User } from "../types";

type AuthScreenProps = {
  onAuthenticated: (user: User) => void;
};

export function AuthScreen({ onAuthenticated }: AuthScreenProps) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [form, setForm] = useState({ username: "buyer", password: "buyer123", displayName: "", phone: "" });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      const user = mode === "login"
        ? await authApi.login(form.username, form.password)
        : await authApi.register(form);
      onAuthenticated(user);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "登录失败");
    } finally {
      setSubmitting(false);
    }
  }

  function switchMode(next: "login" | "register") {
    setMode(next);
    setError("");
    setForm(next === "login"
      ? { username: "buyer", password: "buyer123", displayName: "", phone: "" }
      : { username: "", password: "", displayName: "", phone: "" });
  }

  return (
    <main className="auth-page">
      <section className="auth-showcase">
        <div className="auth-brand"><span className="brand-mark"><Store size={20} /></span> Agent Store</div>
        <div className="auth-statement">
          <span className="section-kicker">LOCAL COMMERCE / 2026</span>
          <h1>一套可以完整走通的本地商城</h1>
          <p>从选品到售后，每个状态都由 Spring Boot 与 MySQL 真实记录。</p>
        </div>
        <div className="auth-flow" aria-label="商城流程">
          <span><PackageCheck size={18} /> 下单与发货</span>
          <span><WalletCards size={18} /> 余额支付</span>
          <span><ShieldCheck size={18} /> 售后退款</span>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-form-wrap">
          <div className="auth-tabs" role="tablist">
            <button type="button" className={mode === "login" ? "active" : ""} onClick={() => switchMode("login")}>登录</button>
            <button type="button" className={mode === "register" ? "active" : ""} onClick={() => switchMode("register")}>注册</button>
          </div>
          <div className="auth-heading">
            {mode === "login" ? <LockKeyhole size={24} /> : <UserPlus size={24} />}
            <div><h2>{mode === "login" ? "欢迎回来" : "创建商城账户"}</h2><p>{mode === "login" ? "登录后继续处理购物与订单" : "注册成功后会自动登录"}</p></div>
          </div>
          <form className="auth-form" onSubmit={submit}>
            <label><span>用户名</span><input required minLength={4} maxLength={30} value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} autoComplete="username" /></label>
            <label><span>密码</span><input required minLength={6} maxLength={50} type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} autoComplete={mode === "login" ? "current-password" : "new-password"} /></label>
            {mode === "register" && (
              <>
                <label><span>姓名</span><input required maxLength={50} value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></label>
                <label><span>手机号</span><input required pattern="[0-9+ -]{6,30}" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} /></label>
              </>
            )}
            {error && <div className="form-error" role="alert">{error}</div>}
            <button className="primary-button auth-submit" type="submit" disabled={submitting}>
              {submitting ? "正在处理" : mode === "login" ? "登录商城" : "完成注册"}<ArrowRight size={17} />
            </button>
          </form>
          {mode === "login" && (
            <div className="demo-accounts">
              <span>演示账号</span>
              <button type="button" onClick={() => setForm({ ...form, username: "buyer", password: "buyer123" })}>买家 buyer / buyer123</button>
              <button type="button" onClick={() => setForm({ ...form, username: "admin", password: "admin123" })}>管理员 admin / admin123</button>
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
