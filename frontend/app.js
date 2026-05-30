const form = document.querySelector("#chatForm");
const messageInput = document.querySelector("#messageInput");
const patientIdInput = document.querySelector("#patientId");
const apiBaseUrlInput = document.querySelector("#apiBaseUrl");
const messages = document.querySelector("#messages");
const sendButton = document.querySelector("#sendButton");
const statusText = document.querySelector("#statusText");
const samplePatients = document.querySelector("#samplePatients");
const sampleMedication = document.querySelector("#sampleMedication");
const sampleObservation = document.querySelector("#sampleObservation");

const sessionId = document.querySelector("#sessionId");
const intent = document.querySelector("#intent");
const answerSource = document.querySelector("#answerSource");
const toolName = document.querySelector("#toolName");
const responsePatient = document.querySelector("#responsePatient");
const responseScope = document.querySelector("#responseScope");
const observationType = document.querySelector("#observationType");
const evidenceList = document.querySelector("#evidenceList");
const usageBlock = document.querySelector("#usageBlock");

let currentSessionId = null;

samplePatients.addEventListener("click", () => {
  messageInput.value = "Danh sách bệnh nhân hiện có";
  messageInput.focus();
});

sampleMedication.addEventListener("click", () => {
  messageInput.value = `Bệnh nhân ${patientIdInput.value.trim() || "001"} đang dùng thuốc gì?`;
  messageInput.focus();
});

sampleObservation.addEventListener("click", () => {
  messageInput.value = `Cho tôi xem huyết áp gần nhất của bệnh nhân ${patientIdInput.value.trim() || "001"}`;
  messageInput.focus();
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();

  const message = messageInput.value.trim();
  const patientId = patientIdInput.value.trim();

  if (!message) {
    return;
  }

  await submitChat(message, patientId, message);
});

async function submitChat(message, patientId, displayText) {
  const apiBaseUrl = apiBaseUrlInput.value.trim().replace(/\/$/, "");

  if (!message || !apiBaseUrl) {
    return;
  }

  appendMessage("user", displayText || message);
  messageInput.value = "";
  setStatus("Đang gửi", "loading");
  setFormDisabled(true);

  try {
    const payload = {
      message,
      patient_id: patientId || null,
    };

    if (currentSessionId) {
      payload.session_id = currentSessionId;
    }

    const response = await fetch(`${apiBaseUrl}/api/chat`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(payload),
    });

    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(data.detail || `Yêu cầu thất bại với HTTP ${response.status}`);
    }

    currentSessionId = data.session_id || currentSessionId;
    appendMessage("assistant", data.answer || "Không có câu trả lời.", data);
    renderDetails(data);
    setStatus("Sẵn sàng", "ready");
  } catch (error) {
    appendMessage("error", error.message || "Yêu cầu thất bại.");
    setStatus("Lỗi", "error");
  } finally {
    setFormDisabled(false);
    messageInput.focus();
  }
}

function appendMessage(role, text, data) {
  const article = document.createElement("article");
  article.className = `message ${role}`;

  const label = document.createElement("div");
  label.className = "message-label";
  label.textContent = role === "user" ? "Bạn" : role === "error" ? "Lỗi" : "Trợ lý";

  const body = role === "assistant"
    ? renderAssistantAnswer(text)
    : renderPlainMessage(text);

  article.append(label, body);
  if (role === "assistant") {
    const candidates = renderPatientCandidates(data);
    if (candidates) {
      article.append(candidates);
    }
  }
  messages.append(article);
  messages.scrollTop = messages.scrollHeight;
}

function renderPatientCandidates(data) {
  if (!data || !data.needs_patient_selection || !Array.isArray(data.patient_candidates)) {
    return null;
  }

  const candidates = data.patient_candidates.filter((item) => item && item.id);
  if (candidates.length === 0) {
    return null;
  }

  const panel = document.createElement("div");
  panel.className = "candidate-panel";

  const title = document.createElement("div");
  title.className = "candidate-title";
  title.textContent = "Chọn đúng bệnh nhân để tiếp tục";
  panel.append(title);

  for (const candidate of candidates) {
    const row = document.createElement("div");
    row.className = "candidate-row";

    const info = document.createElement("div");
    info.className = "candidate-info";

    const name = document.createElement("strong");
    name.textContent = `${candidate.name || "Không rõ tên"} (${candidate.id})`;

    const detail = document.createElement("span");
    detail.textContent = [
      candidate.birth_date ? `Sinh: ${candidate.birth_date}` : null,
      candidate.phone ? `SĐT: ${candidate.phone}` : null,
      candidate.gender ? `Giới tính: ${formatGender(candidate.gender)}` : null,
    ].filter(Boolean).join(" | ");

    info.append(name, detail);

    const button = document.createElement("button");
    button.type = "button";
    button.className = "candidate-button";
    button.textContent = "Chọn";
    button.addEventListener("click", () => {
      patientIdInput.value = candidate.id;
      const question = data.pending_question || messageInput.value.trim();
      const label = `Chọn Patient/${candidate.id} - ${candidate.name || "không rõ tên"}`;
      submitChat(question, candidate.id, label);
    });

    row.append(info, button);
    panel.append(row);
  }

  return panel;
}

function formatGender(gender) {
  if (gender === "male") {
    return "nam";
  }
  if (gender === "female") {
    return "nữ";
  }
  if (gender === "other") {
    return "khác";
  }
  return "không rõ";
}

function renderPlainMessage(text) {
  const paragraph = document.createElement("p");
  paragraph.textContent = text;
  return paragraph;
}

function renderAssistantAnswer(text) {
  const answer = String(text || "").trim();
  const body = document.createElement("div");
  body.className = "message-body";

  const patientList = renderPatientGroupedAnswer(answer);
  if (patientList) {
    body.append(patientList);
    return body;
  }

  const delimitedList = renderDelimitedAnswer(answer);
  if (delimitedList) {
    body.append(delimitedList);
    return body;
  }

  for (const line of answer.split(/\n+/).map((item) => item.trim()).filter(Boolean)) {
    const paragraph = document.createElement("p");
    paragraph.textContent = line;
    body.append(paragraph);
  }

  if (body.childElementCount === 0) {
    const paragraph = document.createElement("p");
    paragraph.textContent = "Không có câu trả lời.";
    body.append(paragraph);
  }

  return body;
}

function renderPatientGroupedAnswer(text) {
  const patientMatches = [...text.matchAll(/Patient\/[A-Za-z0-9.-]+/g)];
  if (patientMatches.length < 2) {
    return null;
  }

  const firstPatientIndex = patientMatches[0].index ?? 0;
  const introText = text.slice(0, firstPatientIndex).replace(/[:\s]+$/, "");
  const listText = text.slice(firstPatientIndex).replace(/\.$/, "");
  const items = listText
    .split(/;\s*(?=Patient\/[A-Za-z0-9.-]+)/)
    .map((item) => item.trim())
    .filter(Boolean);

  if (items.length < 2) {
    return null;
  }

  const fragment = document.createDocumentFragment();
  if (introText) {
    const intro = document.createElement("p");
    intro.className = "answer-intro";
    intro.textContent = introText;
    fragment.append(intro);
  }

  fragment.append(buildAnswerList(items.map(parsePatientLine)));
  return fragment;
}

function renderDelimitedAnswer(text) {
  const colonIndex = text.indexOf(":");
  if (colonIndex < 0) {
    return null;
  }

  const introText = text.slice(0, colonIndex).trim();
  const rawItems = text
    .slice(colonIndex + 1)
    .replace(/\.$/, "")
    .split(/;\s*/)
    .map((item) => item.trim())
    .filter(Boolean);

  if (rawItems.length < 2) {
    return null;
  }

  const fragment = document.createDocumentFragment();
  const intro = document.createElement("p");
  intro.className = "answer-intro";
  intro.textContent = introText;
  fragment.append(intro);
  fragment.append(buildAnswerList(rawItems.map((item) => ({ title: "", detail: item }))));
  return fragment;
}

function buildAnswerList(items) {
  const list = document.createElement("ul");
  list.className = "answer-list";

  for (const item of items) {
    const node = document.createElement("li");
    node.className = "answer-item";

    if (item.title) {
      const title = document.createElement("strong");
      title.textContent = item.title;
      node.append(title);
    }

    if (item.detail) {
      const detail = document.createElement("span");
      detail.textContent = item.detail;
      node.append(detail);
    }

    list.append(node);
  }

  return list;
}

function parsePatientLine(text) {
  const match = text.match(/^(Patient\/[A-Za-z0-9.-]+)\s*(?:-\s*|\(([^)]*)\):\s*|:\s*)?(.*)$/);
  if (!match) {
    return { title: "", detail: text };
  }

  const patientId = match[1];
  const patientName = match[2] ? ` (${match[2]})` : "";
  const detail = (match[3] || "").trim();
  return {
    title: `${patientId}${patientName}`,
    detail,
  };
}

function renderDetails(data) {
  sessionId.textContent = data.session_id || "-";
  intent.textContent = data.intent || "-";
  answerSource.textContent = data.answer_source || "-";
  toolName.textContent = data.tool_name || "-";
  responsePatient.textContent = data.patient_id || "-";
  responseScope.textContent = data.all_patients ? "Tất cả bệnh nhân" : "Một bệnh nhân";
  observationType.textContent = data.observation_type || "-";
  usageBlock.textContent = JSON.stringify(data.usage || {}, null, 2);

  evidenceList.replaceChildren();
  const evidence = Array.isArray(data.evidence) ? data.evidence : [];
  if (evidence.length === 0) {
    const empty = document.createElement("p");
    empty.className = "muted";
    empty.textContent = "Không có dữ liệu tham chiếu.";
    evidenceList.append(empty);
    return;
  }

  for (const item of evidence) {
    const node = document.createElement("div");
    node.className = "evidence-item";

    const title = document.createElement("strong");
    title.textContent = `${item.resource_type || "Tài nguyên"} ${item.id || ""}`.trim();

    const summary = document.createElement("p");
    summary.textContent = item.summary || "-";

    node.append(title, summary);

    if (item.data) {
      const details = document.createElement("details");
      details.className = "evidence-json";

      const detailsTitle = document.createElement("summary");
      detailsTitle.textContent = "Dữ liệu chi tiết";

      const json = document.createElement("pre");
      json.textContent = JSON.stringify(item.data, null, 2);

      details.append(detailsTitle, json);
      node.append(details);
    }

    evidenceList.append(node);
  }
}

function setStatus(text, state) {
  statusText.textContent = text;
  statusText.className = `status ${state || ""}`.trim();
}

function setFormDisabled(disabled) {
  sendButton.disabled = disabled;
  samplePatients.disabled = disabled;
  sampleMedication.disabled = disabled;
  sampleObservation.disabled = disabled;
  messageInput.disabled = disabled;
}
