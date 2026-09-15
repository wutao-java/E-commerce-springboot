import { FormEvent, useState } from "react";
import { Bot, Send, X } from "lucide-react";

import { commerceApi } from "../api";

type Message = { from: "user" | "agent"; text: string; fallback?: boolean };

export function CustomerServiceDrawer({ open, context, onClose }: {
  open: boolean;
  context: Record<string, unknown>;
  onClose: () => void;
}) {
  const [messages, setMessages] = useState<Message[]>([
    { from: "agent", text: "您好，我是商城客服助手，请问有什么可以帮您？" },
  ]);
  const [draft, setDraft] = useState("");
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  async function send(event: FormEvent) {
    event.preventDefault();
    const text = draft.trim();
    if (!text || sending) return;
    setMessages((current) => [...current, { from: "user", text }]);
    setDraft("");
    setSending(true);
    try {
      const response = await commerceApi.chat(text, sessionId, context);
      setSessionId(response.sessionId);
      setMessages((current) => [...current, { from: "agent", text: response.answer, fallback: response.fallback }]);
    } catch {
      setMessages((current) => [...current, { from: "agent", text: "客服服务暂时繁忙，您可以稍后再试，或联系人工客服继续处理。", fallback: true }]);
    } finally {
      setSending(false);
    }
  }

  if (!open) return null;
  return (
    <aside className="service-drawer" aria-label="商城客服">
      <header><div><Bot size={20} /><span><strong>商城客服</strong><small>{String(context.type || "HOME")}</small></span></div><button className="icon-button" onClick={onClose} title="关闭客服" aria-label="关闭客服"><X size={18} /></button></header>
      <div className="service-messages">
        {messages.map((message, index) => <div className={`service-message ${message.from} ${message.fallback ? "fallback" : ""}`} key={`${message.from}-${index}`}>{message.text}</div>)}
        {sending && <div className="service-message agent">正在处理...</div>}
      </div>
      <form onSubmit={send}><input value={draft} onChange={(event) => setDraft(event.target.value)} disabled={sending} placeholder="输入商品、订单或售后问题" /><button className="primary-button icon-button" disabled={sending || !draft.trim()} title="发送" aria-label="发送"><Send size={17} /></button></form>
    </aside>
  );
}
