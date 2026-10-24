const $ = (id) => document.getElementById(id);
let editingId = null;
let toastTimer;

function formBody(form) { return new URLSearchParams(new FormData(form)); }
async function api(url, options = {}) {
  const response = await fetch(url, options);
  const data = await response.json();
  return { response, data };
}
function notify(message, success = true) {
  const el = $("toast");
  el.textContent = message;
  el.className = "toast " + (success ? "success" : "error");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.add("hidden"), 4500);
}
function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
}
function showApp() {
  $("loginView").classList.add("hidden");
  $("appView").classList.remove("hidden");
  loadStudents();
}
function showSection(section) {
  ["overview", "manage", "records"].forEach(s => $(s + "Section").classList.toggle("hidden", s !== section));
  document.querySelectorAll(".nav-item").forEach(b => b.classList.toggle("active", b.dataset.section === section));
  $("pageTitle").textContent = section === "overview" ? "Overview" : section === "manage" ? "Manage students" : "All records";
  if (section === "records") loadStudents();
}
document.querySelectorAll("[data-section]").forEach(btn => btn.addEventListener("click", () => showSection(btn.dataset.section)));
document.querySelectorAll("[data-goto]").forEach(btn => btn.addEventListener("click", () => showSection(btn.dataset.goto)));

$("loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("loginMessage").textContent = "Checking credentials…";
  try {
    const { response, data } = await api("/api/login", { method: "POST", body: formBody(event.target) });
    $("loginMessage").textContent = data.message;
    if (response.ok && data.success) showApp();
  } catch { $("loginMessage").textContent = "Could not connect to the Java server. Is it running?"; }
});
$("logoutBtn").addEventListener("click", () => {
  $("appView").classList.add("hidden");
  $("loginView").classList.remove("hidden");
  $("loginForm").reset();
  $("loginMessage").textContent = "You have signed out.";
  showSection("overview");
});

function studentTable(students, withActions = false) {
  if (!students.length) return '<div class="empty-state">No student records yet. Add a student to get started.</div>';
  return `<div class="table-wrap"><table><thead><tr><th>Student</th><th>ID</th><th>Course</th><th>Email</th>${withActions ? '<th>Actions</th>' : ''}</tr></thead><tbody>${students.map(s => `
    <tr><td><div class="record-name"><span class="record-avatar">${esc((s.name || "?").charAt(0).toUpperCase())}</span><strong>${esc(s.name)}</strong></div></td>
    <td><span class="id-pill">${esc(s.id)}</span></td><td>${esc(s.course)}</td><td>${esc(s.email)}</td>
    ${withActions ? `<td><button class="table-action" data-edit="${esc(s.id)}">Edit</button><button class="table-action delete" data-delete="${esc(s.id)}">Delete</button></td>` : ''}</tr>`).join("")}</tbody></table></div>`;
}
async function loadStudents() {
  try {
    const { response, data } = await api("/api/students?action=all");
    if (!response.ok) throw new Error("Could not load records.");
    const students = data.students || [];
    $("studentCount").textContent = students.length;
    $("recentTable").innerHTML = studentTable(students.slice(0, 4));
    $("allTable").innerHTML = studentTable(students, true);
    $("finalSummary").textContent = `Final display complete: ${students.length} student record(s) currently stored.`;
    $("allTable").querySelectorAll("[data-edit]").forEach(btn => btn.addEventListener("click", () => startEdit(students.find(s => s.id === btn.dataset.edit))));
    $("allTable").querySelectorAll("[data-delete]").forEach(btn => btn.addEventListener("click", () => deleteStudent(btn.dataset.delete)));
  } catch (error) { notify(error.message, false); }
}
$("studentForm").addEventListener("submit", async event => {
  event.preventDefault();
  const formData = formBody(event.target);
  formData.set("action", editingId ? "update" : "add");
  try {
    const { response, data } = await api("/api/students", { method: "POST", body: formData });
    notify(data.message, response.ok && data.success);
    if (response.ok && data.success) {
      resetStudentForm();
      await loadStudents();
    }
  } catch { notify("Could not connect to the Java server.", false); }
});
function startEdit(student) {
  if (!student) return;
  editingId = student.id;
  $("studentId").value = student.id;
  $("studentId").readOnly = true;
  $("studentName").value = student.name;
  $("studentCourse").value = student.course;
  $("studentEmail").value = student.email;
  $("formTitle").textContent = "Update student";
  $("saveStudentBtn").textContent = "Save changes";
  $("cancelEditBtn").classList.remove("hidden");
  showSection("manage");
  window.scrollTo({ top: 0, behavior: "smooth" });
}
function resetStudentForm() {
  editingId = null;
  $("studentForm").reset();
  $("studentId").readOnly = false;
  $("formTitle").textContent = "Add a student";
  $("saveStudentBtn").textContent = "Add student";
  $("cancelEditBtn").classList.add("hidden");
}
$("cancelEditBtn").addEventListener("click", resetStudentForm);
async function deleteStudent(id) {
  if (!confirm(`Delete student ${id}? This cannot be undone during this session.`)) return;
  const body = new URLSearchParams({ action: "delete", id });
  try {
    const { response, data } = await api("/api/students", { method: "POST", body });
    notify(data.message, response.ok && data.success);
    if (response.ok) await loadStudents();
  } catch { notify("Could not connect to the Java server.", false); }
}
$("searchForm").addEventListener("submit", async event => {
  event.preventDefault();
  const params = new URLSearchParams(formBody(event.target));
  params.set("action", "search");
  try {
    const { response, data } = await api("/api/students?" + params.toString());
    if (!response.ok || !data.student) {
      $("searchResult").className = "search-result empty-state";
      $("searchResult").textContent = data.message || "Student not found.";
      return;
    }
    const s = data.student;
    $("searchResult").className = "search-result";
    $("searchResult").innerHTML = `<div class="student-detail"><div><span>Student ID</span><b>${esc(s.id)}</b></div><div><span>Full name</span><b>${esc(s.name)}</b></div><div><span>Course</span><b>${esc(s.course)}</b></div><div><span>Email</span><b>${esc(s.email)}</b></div></div>`;
  } catch { $("searchResult").textContent = "Could not connect to the Java server."; }
});
$("refreshBtn").addEventListener("click", async () => { await loadStudents(); notify("All records refreshed."); });
