const views = document.querySelectorAll(".view");
const navItems = document.querySelectorAll(".nav-item");

const samples = {
  askGraph: "Client-A가 사용 중인 제품을 알려줘",
  askSql: "2025년 3분기 총 매출액은 얼마야?",
  askDoc: "최근 서버 장애 사례와 원인을 알려줘",
  sqlSales: "제품별 총 계약 금액을 큰 순서로 보여줘",
  sqlContract: "현재 활성 상태인 계약 수는 몇 개야?",
  documents: "장애 원인",
  nodes: "product",
};

navItems.forEach((item) => {
  item.addEventListener("click", () => activateView(item.dataset.view));
});

document.querySelectorAll("[data-jump]").forEach((button) => {
  button.addEventListener("click", () => activateView(button.dataset.jump));
});

document.querySelectorAll("[data-sample]").forEach((button) => {
  button.addEventListener("click", () => {
    const key = button.dataset.sample;
    if (key.startsWith("ask")) {
      document.querySelector("#ask-question").value = samples[key];
    }
    if (key.startsWith("sql")) {
      document.querySelector("#sql-question").value = samples[key];
    }
    if (key === "documents") {
      document.querySelector("#document-query").value = samples.documents;
    }
    if (key === "nodes") {
      document.querySelector("#node-type").value = samples.nodes;
      document.querySelector("#node-name").value = "";
    }
  });
});

document.querySelector("#ask-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const result = document.querySelector("#ask-result");
  const question = document.querySelector("#ask-question").value.trim();
  if (!question) {
    renderError(result, "질문을 입력해 주세요.");
    return;
  }

  renderLoading(result, "질문을 분석하고 MCP 도구를 실행하는 중입니다.");
  try {
    const data = await postJson("/api/ask", { question });
    result.className = "result";
    result.innerHTML = `
      <span class="badge">${escapeHtml(data.selectedTool || "unknown")}</span>
      <span class="badge">${data.success ? "success" : "failed"}</span>
      <span class="badge">${escapeHtml(String(data.elapsedMillis || 0))}ms</span>
      <h3>최종 답변</h3>
      <div>${escapeHtml(data.answer || "답변이 없습니다.")}</div>
      <h3>선택 이유</h3>
      <div>${escapeHtml(data.routingReason || "선택 이유가 없습니다.")}</div>
      <h3>도구 실행 결과</h3>
      <pre>${escapeHtml(JSON.stringify(data.toolResult, null, 2))}</pre>
    `;
  } catch (error) {
    renderError(result, error.message);
  }
});

document.querySelector("#sql-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const result = document.querySelector("#sql-result");
  const question = document.querySelector("#sql-question").value.trim();
  if (!question) {
    renderError(result, "질문을 입력해 주세요.");
    return;
  }

  renderLoading(result, "자연어 질문을 SQL로 변환하고 PostgreSQL을 조회하는 중입니다.");
  try {
    const data = await postJson("/api/nl2sql", { question });
    result.className = "result";
    result.innerHTML = `
      <span class="badge">nl2sql</span>
      <h3>생성 SQL</h3>
      <pre>${escapeHtml(data.sql || "")}</pre>
      <h3>조회 결과</h3>
      ${renderTable(data.rows || [])}
    `;
  } catch (error) {
    renderError(result, error.message);
  }
});

document.querySelector("#import-documents").addEventListener("click", async () => {
  const result = document.querySelector("#document-result");
  renderLoading(result, "문서 chunk를 생성하고 임베딩을 적재하는 중입니다.");
  try {
    const data = await postJson("/internal/documents/import", {});
    result.className = "result";
    result.innerHTML = `<h3>문서 적재 완료</h3><div>문서 ${data.documentCount}건, chunk ${data.chunkCount}개를 적재했습니다.</div>`;
  } catch (error) {
    renderError(result, error.message);
  }
});

document.querySelector("#document-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const result = document.querySelector("#document-result");
  const query = document.querySelector("#document-query").value.trim();
  const limit = Number(document.querySelector("#document-limit").value || 3);
  const documentType = document.querySelector("#document-type").value.trim();
  if (!query) {
    renderError(result, "검색어를 입력해 주세요.");
    return;
  }

  renderLoading(result, "pgvector 유사도 검색을 실행하는 중입니다.");
  try {
    const payload = { query, limit };
    if (documentType) {
      payload.documentType = documentType;
    }
    const data = await postJson("/internal/documents/search", payload);
    result.className = "result";
    result.innerHTML = renderDocumentResults(data.results || []);
  } catch (error) {
    renderError(result, error.message);
  }
});

document.querySelector("#import-graph").addEventListener("click", async () => {
  const result = document.querySelector("#graph-result");
  renderLoading(result, "그래프 노드와 엣지를 적재하는 중입니다.");
  try {
    const data = await postJson("/internal/graph/import", {});
    result.className = "result";
    result.innerHTML = `<h3>그래프 적재 완료</h3><div>노드 ${data.nodeCount}개, 관계 ${data.edgeCount}개를 적재했습니다.</div>`;
  } catch (error) {
    renderError(result, error.message);
  }
});

document.querySelector("#graph-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const result = document.querySelector("#graph-result");
  const nodeId = document.querySelector("#graph-node").value.trim();
  const direction = document.querySelector("#graph-direction").value;
  const relation = document.querySelector("#graph-relation").value;
  if (!nodeId) {
    renderError(result, "노드 ID를 입력해 주세요.");
    return;
  }

  const params = new URLSearchParams({ nodeId, depth: "1", direction });
  if (relation) {
    params.set("relation", relation);
  }

  renderLoading(result, "그래프 관계를 탐색하는 중입니다.");
  try {
    const data = await getJson(`/internal/graph/relations?${params.toString()}`);
    result.className = "result";
    result.innerHTML = renderRelationResults(data.results || []);
  } catch (error) {
    renderError(result, error.message);
  }
});

document.querySelector("#node-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const result = document.querySelector("#node-result");
  const name = document.querySelector("#node-name").value.trim();
  const type = document.querySelector("#node-type").value;
  const params = new URLSearchParams();
  if (name) {
    params.set("name", name);
  }
  if (type) {
    params.set("type", type);
  }

  renderLoading(result, "그래프 노드를 검색하는 중입니다.");
  try {
    const data = await getJson(`/internal/graph/nodes?${params.toString()}`);
    result.className = "result";
    result.innerHTML = renderNodeResults(data.results || []);
  } catch (error) {
    renderError(result, error.message);
  }
});

function activateView(viewName) {
  navItems.forEach((nav) => nav.classList.toggle("active", nav.dataset.view === viewName));
  views.forEach((view) => view.classList.toggle("active", view.id === `view-${viewName}`));
  document.querySelector(".workspace").scrollIntoView({ block: "start", behavior: "smooth" });
}

async function postJson(url, body) {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return readResponse(response);
}

async function getJson(url) {
  const response = await fetch(url);
  return readResponse(response);
}

async function readResponse(response) {
  const text = await response.text();
  let data = {};
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }
  if (!response.ok) {
    throw new Error(data.message || data.error || `요청 실패: ${response.status}`);
  }
  return data;
}

function renderDocumentResults(items) {
  if (items.length === 0) {
    return "검색 결과가 없습니다.";
  }
  return items
    .map(
      (item) => `
        <div class="result-row">
          <span class="badge">${escapeHtml(item.docId || "document")}</span>
          <span class="badge">similarity ${formatScore(item.similarity)}</span>
          <h3>${escapeHtml(parseTitle(item.metadata) || "문서 chunk")}</h3>
          <div>${escapeHtml(item.content || "")}</div>
        </div>
      `,
    )
    .join("");
}

function renderRelationResults(items) {
  if (items.length === 0) {
    return "관계 결과가 없습니다.";
  }
  return items
    .slice(0, 20)
    .map((item) => {
      const source = item.source?.name || item.source?.id || "";
      const target = item.target?.name || item.target?.id || "";
      return `
        <div class="result-row">
          <span class="badge">${escapeHtml(item.relation || "")}</span>
          <h3>${escapeHtml(source)} -> ${escapeHtml(target)}</h3>
          <div>${escapeHtml(JSON.stringify(item.properties || {}, null, 2))}</div>
        </div>
      `;
    })
    .join("");
}

function renderNodeResults(items) {
  if (items.length === 0) {
    return "노드 검색 결과가 없습니다.";
  }
  return items
    .map(
      (item) => `
        <div class="result-row">
          <span class="badge">${escapeHtml(item.type || "")}</span>
          <h3>${escapeHtml(item.name || item.id || "")}</h3>
          <div>${escapeHtml(item.id || "")}</div>
        </div>
      `,
    )
    .join("");
}

function renderTable(rows) {
  if (rows.length === 0) {
    return "<div>조회 결과가 없습니다.</div>";
  }

  const headers = Object.keys(rows[0]);
  return `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>${headers.map((header) => `<th>${escapeHtml(header)}</th>`).join("")}</tr>
        </thead>
        <tbody>
          ${rows
            .map(
              (row) => `
                <tr>
                  ${headers.map((header) => `<td>${escapeHtml(row[header] ?? "")}</td>`).join("")}
                </tr>
              `,
            )
            .join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderLoading(node, message) {
  node.className = "result empty";
  node.textContent = message;
}

function renderError(node, message) {
  node.className = "result error";
  node.textContent = message;
}

function parseTitle(metadata) {
  if (!metadata) {
    return "";
  }
  try {
    return JSON.parse(metadata).title || "";
  } catch {
    return "";
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatScore(value) {
  if (typeof value !== "number") {
    return "-";
  }
  return value.toFixed(3);
}
