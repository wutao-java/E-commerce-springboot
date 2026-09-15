import { BookOpenCheck, CircleHelp } from "lucide-react";

import type { AfterSalePolicy, FaqEntry } from "../types";

export function PolicyView({ policies, faqs, loading }: {
  policies: AfterSalePolicy[];
  faqs: FaqEntry[];
  loading: boolean;
}) {
  if (loading) return <div className="state-panel">正在读取服务政策...</div>;
  return (
    <main className="content-page policy-view">
      <div className="page-heading">
        <div><span className="section-kicker">服务边界公开透明</span><h1>服务政策与常见问题</h1></div>
      </div>
      <section className="policy-list" aria-label="售后政策">
        {policies.map((policy) => (
          <article className="surface-panel policy-item" key={policy.id}>
            <BookOpenCheck size={20} />
            <div><h2>{policy.title}</h2><p>{policy.content}</p>
              <dl><dt>适用条件</dt><dd>{policy.applicableConditions}</dd><dt>不适用</dt><dd>{policy.exclusionConditions}</dd><dt>所需材料</dt><dd>{policy.requiredEvidence}</dd></dl>
            </div>
          </article>
        ))}
      </section>
      <section className="surface-panel faq-list">
        <div className="panel-title"><CircleHelp size={19} /><h2>常见问题</h2></div>
        {faqs.map((faq) => <details key={faq.id}><summary><span>{faq.category}</span>{faq.question}</summary><p>{faq.answer}</p></details>)}
      </section>
    </main>
  );
}
