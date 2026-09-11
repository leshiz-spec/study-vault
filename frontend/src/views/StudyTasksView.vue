<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import {
  createTask,
  deleteTask,
  listNotes,
  listTasks,
  updateTask,
  type Note,
  type StudyTask,
  type StudyTaskStatus,
} from "../api";

const tasks = ref<StudyTask[]>([]);
const notes = ref<Note[]>([]);
const loading = ref(true);
const saving = ref(false);
const error = ref("");
const statusFilter = ref<StudyTaskStatus | "">("");
const dueDateFilter = ref("");
const editingId = ref<number | null>(null);
const title = ref("");
const dueDate = ref("");
const status = ref<StudyTaskStatus>("todo");
const noteIds = ref<number[]>([]);

function today() {
  const date = new Date();
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 10);
}

const isEditing = computed(() => editingId.value !== null);

function resetForm() {
  editingId.value = null;
  title.value = "";
  dueDate.value = "";
  status.value = "todo";
  noteIds.value = [];
}

function editTask(task: StudyTask) {
  editingId.value = task.id;
  title.value = task.title;
  dueDate.value = task.dueDate || "";
  status.value = task.status;
  noteIds.value = [...(task.noteIds || (task.noteId ? [task.noteId] : []))];
  window.scrollTo({ top: 0, behavior: "smooth" });
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    tasks.value = await listTasks({
      status: statusFilter.value || undefined,
      dueDate: dueDateFilter.value || undefined,
    });
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to load study tasks";
  } finally {
    loading.value = false;
  }
}

async function submit() {
  saving.value = true;
  error.value = "";
  try {
    const input = {
      title: title.value,
      dueDate: dueDate.value || null,
      status: status.value,
      noteIds: noteIds.value,
    };
    if (editingId.value === null) await createTask(input);
    else await updateTask(editingId.value, input);
    resetForm();
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to save study task";
  } finally {
    saving.value = false;
  }
}

async function complete(task: StudyTask) {
  try {
    await updateTask(task.id, {
      title: task.title,
      dueDate: task.dueDate,
      status: task.status === "done" ? "todo" : "done",
      noteIds: task.noteIds || (task.noteId ? [task.noteId] : []),
    });
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to update study task";
  }
}

async function remove(task: StudyTask) {
  if (!window.confirm(`Delete “${task.title}”?`)) return;
  try {
    await deleteTask(task.id);
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to delete study task";
  }
}

function isOverdue(task: StudyTask) {
  return Boolean(task.dueDate && task.dueDate < today() && task.status !== "done");
}

function formatDueDate(value?: string | null) {
  return value ? new Date(`${value}T00:00:00`).toLocaleDateString() : "No due date";
}

onMounted(async () => {
  try {
    notes.value = await listNotes();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to load notes for linking";
  }
  await load();
});
</script>
<template>
  <main class="tasks-page">
    <header class="notes-header">
      <div>
        <RouterLink class="back-link" to="/dashboard">← Back to dashboard</RouterLink>
        <span class="eyebrow">STUDY PLAN</span>
        <h1>Study tasks</h1>
        <p>Turn notes into small, manageable actions.</p>
      </div>
    </header>
    <section class="task-form editor-card" aria-label="Study task form">
      <h2>{{ isEditing ? "Edit task" : "Create a task" }}</h2>
      <p v-if="error" class="error">{{ error }}</p>
      <form class="task-form-grid" @submit.prevent="submit">
        <label>Task title<input v-model="title" required maxlength="255" placeholder="Review chapter 3" /></label>
        <label>Due date<input v-model="dueDate" type="date" /></label>
        <label>Status<select v-model="status"><option value="todo">To do</option><option value="in_progress">In progress</option><option value="done">Done</option></select></label>
        <fieldset class="task-note-picker">
          <legend>Related notes</legend>
          <p v-if="!notes.length">No notes available</p>
          <label v-for="note in notes" :key="note.id">
            <input v-model="noteIds" type="checkbox" :value="note.id" />
            <span>{{ note.title }}</span>
          </label>
        </fieldset>
        <div class="task-form-actions"><button class="button" :disabled="saving">{{ saving ? "Saving…" : isEditing ? "Update task" : "Add task" }}</button><button v-if="isEditing" type="button" class="button secondary" @click="resetForm">Cancel</button></div>
      </form>
    </section>
    <section class="task-filters" aria-label="Filter study tasks">
      <label>Status<select v-model="statusFilter" @change="load"><option value="">All statuses</option><option value="todo">To do</option><option value="in_progress">In progress</option><option value="done">Done</option></select></label>
      <label>Due on<input v-model="dueDateFilter" type="date" @change="load" /></label>
      <button v-if="statusFilter || dueDateFilter" type="button" class="button secondary" @click="statusFilter = ''; dueDateFilter = ''; load()">Clear filters</button>
    </section>
    <p v-if="loading" class="state">Loading study tasks…</p>
    <div v-else-if="!tasks.length" class="empty state"><h2>No study tasks</h2><p>Create a task above to plan your next review.</p></div>
    <ul v-else class="study-task-list">
      <li v-for="task in tasks" :key="task.id" class="study-task" :class="{ overdue: isOverdue(task), completed: task.status === 'done' }">
        <div class="study-task-main"><strong>{{ task.title }}</strong><div v-if="task.noteTitles?.length" class="task-linked-notes"><span v-for="(noteTitle, index) in task.noteTitles" :key="`${task.id}:${task.noteIds[index]}`">{{ noteTitle }}</span></div><span class="task-meta"><span>{{ task.status === 'in_progress' ? 'In progress' : task.status === 'done' ? 'Done' : 'To do' }}</span><span>{{ formatDueDate(task.dueDate) }}</span><span v-if="isOverdue(task)" class="overdue-label">Overdue</span></span></div>
        <div class="study-task-actions"><button type="button" class="button secondary" @click="complete(task)">{{ task.status === "done" ? "Reopen" : "Complete" }}</button><button type="button" class="link-button" @click="editTask(task)">Edit</button><button type="button" class="danger-link" @click="remove(task)">Delete</button></div>
      </li>
    </ul>
  </main>
</template>
