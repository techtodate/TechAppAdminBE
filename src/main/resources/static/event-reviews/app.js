const DATA_URL = "data/events.json";
const STORAGE_KEY = "techapp-admin-event-reviews-v1";

const state = { events: [], selectedId: null, search: "" };
const elements = {
  list: document.querySelector("#eventList"),
  details: document.querySelector("#detailsPanel"),
  empty: document.querySelector("#emptyState"),
  search: document.querySelector("#searchInput"),
  pending: document.querySelector("#pendingCount"),
  approved: document.querySelector("#approvedCount"),
  rejected: document.querySelector("#rejectedCount"),
  reset: document.querySelector("#resetButton"),
  toast: document.querySelector("#toast")
};

const escapeHtml = (value = "") => String(value).replace(/[&<>'"]/g, character => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", "'":"&#39;", '"':"&quot;" })[character]);
const formatDate = value => value ? new Intl.DateTimeFormat("en-IN", { dateStyle:"medium", timeStyle:"short" }).format(new Date(value)) : "Not recorded";

async function loadEvents(forceJson = false) {
  if (!forceJson) {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try { state.events = JSON.parse(saved); render(); return; } catch { localStorage.removeItem(STORAGE_KEY); }
    }
  }
  const response = await fetch(DATA_URL, { cache:"no-store" });
  if (!response.ok) throw new Error("Could not load event review data.");
  state.events = await response.json();
  persist();
  render();
}

function persist() { localStorage.setItem(STORAGE_KEY, JSON.stringify(state.events)); }
function underReviewEvents() { return state.events.filter(event => event.status === "UNDER_REVIEW"); }

function render() {
  const query = state.search.trim().toLowerCase();
  const pending = underReviewEvents();
  const visible = pending.filter(event => [event.title, event.organizer, event.location, event.id].some(value => String(value).toLowerCase().includes(query)));
  elements.pending.textContent = pending.length;
  elements.approved.textContent = state.events.filter(event => event.status === "APPROVED").length;
  elements.rejected.textContent = state.events.filter(event => event.status === "REJECTED").length;
  elements.empty.hidden = visible.length > 0;
  elements.list.innerHTML = visible.map(event => `
    <button class="event-card ${event.id === state.selectedId ? "selected" : ""}" data-id="${escapeHtml(event.id)}" type="button">
      <div class="event-card-top"><h3>${escapeHtml(event.title)}</h3><span class="badge">UNDER REVIEW</span></div>
      <div class="meta"><span>${escapeHtml(event.id)}</span><span>${escapeHtml(event.organizer)}</span><span>${formatDate(event.submittedAt)}</span></div>
    </button>`).join("");
  if (state.selectedId && !pending.some(event => event.id === state.selectedId)) state.selectedId = null;
  renderDetails();
}

function renderDetails() {
  const event = state.events.find(item => item.id === state.selectedId && item.status === "UNDER_REVIEW");
  if (!event) {
    elements.details.innerHTML = '<div class="empty details-empty">Select an event to inspect all submitted details.</div>';
    return;
  }
  elements.details.innerHTML = `
    <div class="detail-header"><div><p class="eyebrow">${escapeHtml(event.id)}</p><h3>${escapeHtml(event.title)}</h3></div><span class="badge">UNDER REVIEW</span></div>
    <p class="description">${escapeHtml(event.description)}</p>
    <dl class="detail-grid">
      <div><dt>Organizer</dt><dd>${escapeHtml(event.organizer)}</dd></div>
      <div><dt>Contact</dt><dd>${escapeHtml(event.organizerEmail)}</dd></div>
      <div><dt>Format & location</dt><dd>${event.isOnline ? "Online" : escapeHtml(event.location)}</dd></div>
      <div><dt>Level</dt><dd>${escapeHtml(event.level)}</dd></div>
      <div><dt>Event starts</dt><dd>${formatDate(event.startDate)}</dd></div>
      <div><dt>Event ends</dt><dd>${formatDate(event.endDate)}</dd></div>
      <div><dt>Registration opens</dt><dd>${formatDate(event.registrationStartDate)}</dd></div>
      <div><dt>Registration closes</dt><dd>${formatDate(event.registrationEndDate)}</dd></div>
      <div><dt>Price</dt><dd>${escapeHtml(event.price)}</dd></div>
      <div><dt>Source</dt><dd><a href="${escapeHtml(event.sourceUrl)}" target="_blank" rel="noopener noreferrer">Open submitted page</a></dd></div>
    </dl>
    <section class="timeline"><h4>Submission timeline</h4>
      <div class="timeline-row"><span>Draft created</span><strong>${formatDate(event.createdAt)}</strong></div>
      <div class="timeline-row"><span>Submitted</span><strong>${formatDate(event.submittedAt)}</strong></div>
      <div class="timeline-row"><span>Review started</span><strong>${formatDate(event.reviewStartedAt)}</strong></div>
    </section>
    <form id="decisionForm" class="decision">
      <label for="reviewReason">Review reason</label>
      <textarea id="reviewReason" minlength="10" maxlength="1000" required placeholder="Explain why this event should be approved or rejected…"></textarea>
      <p class="hint">Required for both decisions · 10–1,000 characters · decision time is captured automatically</p>
      <div class="actions"><button class="button button-reject" name="decision" value="REJECTED" type="submit">Reject event</button><button class="button button-approve" name="decision" value="APPROVED" type="submit">Approve event</button></div>
    </form>`;
}

function decide(eventId, decision, reason) {
  const event = state.events.find(item => item.id === eventId);
  if (!event || event.status !== "UNDER_REVIEW") return;
  event.status = decision;
  event.reviewReason = reason.trim();
  event.reviewedAt = new Date().toISOString();
  state.selectedId = null;
  persist();
  render();
  showToast(`${event.title} was ${decision.toLowerCase()}.`);
}

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.add("show");
  window.setTimeout(() => elements.toast.classList.remove("show"), 2600);
}

elements.list.addEventListener("click", ({ target }) => {
  const card = target.closest("[data-id]");
  if (card) { state.selectedId = card.dataset.id; render(); }
});
elements.details.addEventListener("submit", event => {
  if (event.target.id !== "decisionForm") return;
  event.preventDefault();
  const decision = event.submitter?.value;
  const reason = event.target.querySelector("#reviewReason").value;
  if (decision && event.target.reportValidity()) decide(state.selectedId, decision, reason);
});
elements.search.addEventListener("input", event => { state.search = event.target.value; render(); });
elements.reset.addEventListener("click", async () => { localStorage.removeItem(STORAGE_KEY); state.selectedId = null; await loadEvents(true); showToast("Demo data restored."); });

loadEvents().catch(error => { elements.empty.hidden = false; elements.empty.textContent = error.message; });
