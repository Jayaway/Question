const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

const state = {
  surveyId: 1,
  survey: null,
  questions: [],
  currentView: "answer",
};

const typeNames = {
  1: "单选题",
  2: "多选题",
  3: "文本题",
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2600);
}

function setStatus(kind, text) {
  const strip = document.querySelector(".status-strip");
  strip.classList.remove("ok", "error");
  if (kind) strip.classList.add(kind);
  $("#statusText").textContent = text;
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });

  const payload = await response.json().catch(() => null);
  if (!response.ok || !payload || payload.code !== 200) {
    throw new Error(payload?.message || "请求失败，请检查后端服务");
  }
  return payload.data;
}

function parseOptions(options) {
  if (!options) return [];
  if (Array.isArray(options)) return options;
  try {
    const parsed = JSON.parse(options);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return String(options)
      .split(/[,\n]/)
      .map((item) => item.trim().replace(/^"|"$/g, ""))
      .filter(Boolean);
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function switchView(view) {
  state.currentView = view;
  $$(".nav-tab").forEach((tab) => tab.classList.toggle("active", tab.dataset.view === view));
  $$(".view").forEach((panel) => panel.classList.remove("active"));
  $(`#${view}View`).classList.add("active");
  if (view === "stats") loadStats();
}

async function loadSurvey() {
  const id = Number($("#surveyId").value || 1);
  if (!Number.isFinite(id) || id <= 0) {
    showToast("请输入有效的问卷 ID");
    return;
  }

  setStatus("", "连接中");
  try {
    const data = await request(`/api/survey/${id}`);
    state.surveyId = id;
    state.survey = data.survey;
    state.questions = data.questions || [];
    renderSurveyHeader();
    renderAnswerForm();
    renderQuestionTable();
    if (state.currentView === "stats") loadStats();
    setStatus("ok", "已连接");
  } catch (error) {
    setStatus("error", "读取失败");
    showToast(error.message);
  }
}

function renderSurveyHeader() {
  $("#surveyTitle").textContent = state.survey?.title || `问卷 #${state.surveyId}`;
  $("#surveyDescription").textContent =
    state.survey?.description || "暂无问卷说明，请在数据库或后端接口中补充描述。";
}

function renderAnswerForm() {
  const form = $("#answerForm");
  if (!state.questions.length) {
    form.innerHTML = `<div class="empty-state">当前问卷还没有题目。</div>`;
    return;
  }

  const cards = state.questions.map((question, index) => {
    const options = parseOptions(question.options);
    const required = question.required === 1 ? "required" : "";
    const requiredText = question.required === 1 ? `<span class="required">必填</span>` : "选填";

    let control = "";
    if (question.type === 3) {
      control = `
        <textarea
          name="q_${question.id}"
          rows="4"
          ${required}
          placeholder="写下你的想法"
        ></textarea>`;
    } else {
      const inputType = question.type === 2 ? "checkbox" : "radio";
      control = `
        <div class="option-list">
          ${options
            .map(
              (option) => `
                <label class="option-item">
                  <input
                    type="${inputType}"
                    name="q_${question.id}"
                    value="${escapeHtml(option)}"
                  />
                  <span>${escapeHtml(option)}</span>
                </label>
              `,
            )
            .join("")}
        </div>`;
    }

    return `
      <article class="question-card">
        <div class="question-meta">
          <span class="badge">${typeNames[question.type] || "题目"}</span>
          <span>第 ${index + 1} 题</span>
          <span>${requiredText}</span>
        </div>
        <h3>${escapeHtml(question.title)}</h3>
        ${control}
      </article>`;
  });

  form.innerHTML = `${cards.join("")}<div class="submit-row"><button type="submit">提交答卷</button></div>`;
}

function collectAnswers() {
  const answers = {};
  for (const question of state.questions) {
    const name = `q_${question.id}`;
    if (question.type === 2) {
      const values = $$(`input[name="${name}"]:checked`).map((input) => input.value);
      if (question.required === 1 && !values.length) {
        throw new Error(`请完成必填题：${question.title}`);
      }
      if (values.length) answers[question.id] = values.join(",");
    } else if (question.type === 1) {
      const selected = $(`input[name="${name}"]:checked`);
      if (question.required === 1 && !selected) {
        throw new Error(`请完成必填题：${question.title}`);
      }
      if (selected) answers[question.id] = selected.value;
    } else {
      const value = $(`[name="${name}"]`)?.value.trim() || "";
      if (question.required === 1 && !value) {
        throw new Error(`请完成必填题：${question.title}`);
      }
      if (value) answers[question.id] = value;
    }
  }
  return answers;
}

async function submitAnswers(event) {
  event.preventDefault();
  try {
    const answers = collectAnswers();
    await request(`/api/survey/${state.surveyId}/submit`, {
      method: "POST",
      body: JSON.stringify({ answers }),
    });
    showToast("提交成功，感谢你的反馈");
    $("#answerForm").reset();
    switchView("stats");
  } catch (error) {
    showToast(error.message);
  }
}

async function loadStats() {
  const list = $("#statsList");
  list.innerHTML = `<div class="empty-state">正在读取统计数据...</div>`;
  try {
    const data = await request(`/api/survey/${state.surveyId}/stats`);
    renderStats(data);
  } catch (error) {
    list.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
  }
}

function renderStats(data) {
  const stats = data?.stats || [];
  const total = Number(data?.total || 0);
  const choiceCount = stats.filter((item) => item.type !== 3).length;
  const textCount = stats.reduce((sum, item) => sum + (item.texts?.length || 0), 0);

  $("#statsSummary").innerHTML = `
    <div class="metric"><strong>${total}</strong><span>估算答卷数</span></div>
    <div class="metric"><strong>${choiceCount}</strong><span>选择题数量</span></div>
    <div class="metric"><strong>${textCount}</strong><span>文本反馈数</span></div>`;

  if (!stats.length) {
    $("#statsList").innerHTML = `<div class="empty-state">暂无统计数据。</div>`;
    return;
  }

  $("#statsList").innerHTML = stats
    .map((item, index) => {
      if (item.type === 3) {
        const texts = item.texts || [];
        return `
          <article class="stat-card">
            <div class="question-meta">
              <span class="badge">文本题</span>
              <span>第 ${index + 1} 题</span>
            </div>
            <h3>${escapeHtml(item.questionTitle)}</h3>
            ${
              texts.length
                ? `<ul class="text-answer-list">${texts
                    .slice(-8)
                    .reverse()
                    .map((text) => `<li>${escapeHtml(text)}</li>`)
                    .join("")}</ul>`
                : `<div class="empty-state">还没有文本回答。</div>`
            }
          </article>`;
      }

      const options = item.options || [];
      const max = Math.max(...options.map((option) => Number(option.count || 0)), 1);
      return `
        <article class="stat-card">
          <div class="question-meta">
            <span class="badge">${typeNames[item.type] || "选择题"}</span>
            <span>第 ${index + 1} 题</span>
          </div>
          <h3>${escapeHtml(item.questionTitle)}</h3>
          <div class="bar-list">
            ${options
              .map((option) => {
                const count = Number(option.count || 0);
                const width = Math.max(4, Math.round((count / max) * 100));
                return `
                  <div>
                    <div class="bar-label">
                      <span>${escapeHtml(option.option)}</span>
                      <strong>${count}</strong>
                    </div>
                    <div class="bar-track"><div class="bar-fill" style="width: ${width}%"></div></div>
                  </div>`;
              })
              .join("")}
          </div>
        </article>`;
    })
    .join("");
}

function resetQuestionForm() {
  $("#editingId").value = "";
  $("#questionForm").reset();
  $("#questionType").value = "1";
  $("#sortOrder").value = String((state.questions.length || 0) + 1);
  $("#requiredFlag").checked = true;
  $("#cancelEdit").style.display = "none";
}

function renderQuestionTable() {
  const table = $("#questionTable");
  if (!state.questions.length) {
    table.innerHTML = `<div class="empty-state">当前问卷还没有题目，可以从上方新增。</div>`;
    resetQuestionForm();
    return;
  }

  table.innerHTML = state.questions
    .map((question, index) => {
      const options = parseOptions(question.options);
      return `
        <div class="table-row">
          <div>
            <div class="row-title">
              <span class="badge">${typeNames[question.type] || "题目"}</span>
              <strong>${index + 1}. ${escapeHtml(question.title)}</strong>
            </div>
            <div class="row-options">
              ${question.required === 1 ? "必填" : "选填"}
              ${options.length ? ` · ${options.map(escapeHtml).join(" / ")}` : ""}
            </div>
          </div>
          <div class="row-actions">
            <button type="button" data-edit="${question.id}">编辑</button>
            <button class="danger-button" type="button" data-delete="${question.id}">删除</button>
          </div>
        </div>`;
    })
    .join("");

  resetQuestionForm();
}

function fillQuestionForm(id) {
  const question = state.questions.find((item) => String(item.id) === String(id));
  if (!question) return;
  $("#editingId").value = question.id;
  $("#questionTitle").value = question.title || "";
  $("#questionType").value = String(question.type || 1);
  $("#sortOrder").value = String(question.sortOrder || 0);
  $("#requiredFlag").checked = question.required === 1;
  $("#questionOptions").value = parseOptions(question.options).join("\n");
  $("#cancelEdit").style.display = "inline-flex";
  $("#questionTitle").focus();
}

async function saveQuestion(event) {
  event.preventDefault();
  const type = Number($("#questionType").value);
  const rawOptions = $("#questionOptions").value
    .split("\n")
    .map((item) => item.trim())
    .filter(Boolean);

  if (type !== 3 && rawOptions.length < 2) {
    showToast("选择题至少需要两个选项");
    return;
  }

  const payload = {
    surveyId: state.surveyId,
    type,
    title: $("#questionTitle").value.trim(),
    options: type === 3 ? null : JSON.stringify(rawOptions),
    sortOrder: Number($("#sortOrder").value || 0),
    required: $("#requiredFlag").checked ? 1 : 0,
  };

  const editingId = $("#editingId").value;
  try {
    if (editingId) {
      await request(`/api/question/${editingId}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      showToast("题目已更新");
    } else {
      await request("/api/question", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      showToast("题目已新增");
    }
    await loadSurvey();
  } catch (error) {
    showToast(error.message);
  }
}

async function deleteQuestion(id) {
  if (!window.confirm("确定删除这道题吗？已有答案统计可能会受到影响。")) return;
  try {
    await request(`/api/question/${id}`, { method: "DELETE" });
    showToast("题目已删除");
    await loadSurvey();
  } catch (error) {
    showToast(error.message);
  }
}

document.addEventListener("click", (event) => {
  const tab = event.target.closest(".nav-tab");
  if (tab) switchView(tab.dataset.view);

  const editButton = event.target.closest("[data-edit]");
  if (editButton) fillQuestionForm(editButton.dataset.edit);

  const deleteButton = event.target.closest("[data-delete]");
  if (deleteButton) deleteQuestion(deleteButton.dataset.delete);
});

$("#loadSurvey").addEventListener("click", loadSurvey);
$("#refreshStats").addEventListener("click", loadStats);
$("#answerForm").addEventListener("submit", submitAnswers);
$("#resetAnswer").addEventListener("click", () => $("#answerForm").reset());
$("#questionForm").addEventListener("submit", saveQuestion);
$("#cancelEdit").addEventListener("click", resetQuestionForm);
$("#questionType").addEventListener("change", () => {
  $("#questionOptions").disabled = $("#questionType").value === "3";
});

resetQuestionForm();
loadSurvey();
