<script setup lang="ts">
import { onMounted, ref } from "vue";
import { listTasks, type StudyTask } from "../api";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const upcomingTasks = ref<StudyTask[]>([]);
const tasksLoading = ref(true);
const tasksError = ref("");

function today() {
  const date = new Date();
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 10);
}

function formatDueDate(value?: string | null) {
  return value ? new Date(`${value}T00:00:00`).toLocaleDateString() : "No due date";
}

async function loadUpcomingTasks() {
  tasksLoading.value = true;
  tasksError.value = "";
  try {
    upcomingTasks.value = (await listTasks({ dueAfter: today() }))
      .filter((task) => task.status !== "done")
      .slice(0, 5);
  } catch (error) {
    tasksError.value = error instanceof Error ? error.message : "Unable to load study tasks";
  } finally {
    tasksLoading.value = false;
  }
}

onMounted(loadUpcomingTasks);
</script>
<template>
  <main class="dashboard">
    <div class="dashboard-panel">
      <section class="dashboard-hero">
        <div>
          <span class="eyebrow">YOUR LEARNING SPACE</span>
          <h1>Welcome back, {{ auth.user?.username }}.</h1>
          <p>Keep your ideas close, revisit what matters, and build a knowledge base that grows with you.</p>
        </div>
        <RouterLink class="button" to="/notes">Open notes <span aria-hidden="true">→</span></RouterLink>
      </section>
      <section class="dashboard-grid">
        <article class="dashboard-card accent"><span class="dashboard-stat">✦</span><h2>Your notes</h2><p>Capture a thought or continue refining something you started.</p><RouterLink class="card-link" to="/notes">View workspace →</RouterLink></article>
        <article class="dashboard-card"><span class="dashboard-stat">#</span><h2>Organize ideas</h2><p>Use personal tags to keep related notes easy to find.</p><RouterLink class="card-link" to="/tags">Manage tags →</RouterLink></article>
        <article class="dashboard-card"><span class="dashboard-stat">↺</span><h2>Recently removed</h2><p>Restore notes from the trash whenever you need them again.</p><RouterLink class="card-link" to="/trash">Open trash →</RouterLink></article>
      </section>
      <section class="dashboard-card dashboard-tasks-card" aria-label="Upcoming study tasks">
        <div class="dashboard-section-heading"><div><span class="eyebrow">STUDY PLAN</span><h2>Upcoming tasks</h2></div><RouterLink class="card-link" to="/tasks">View all →</RouterLink></div>
        <p v-if="tasksLoading" class="dashboard-task-state">Loading tasks…</p>
        <p v-else-if="tasksError" class="error">{{ tasksError }}</p>
        <p v-else-if="!upcomingTasks.length" class="dashboard-task-state">No upcoming tasks. Add one when you are ready to study.</p>
        <ul v-else class="upcoming-task-list">
          <li v-for="task in upcomingTasks" :key="task.id">
            <div><strong>{{ task.title }}</strong><small v-if="task.noteTitles?.length">Linked notes: {{ task.noteTitles.join(", ") }}</small></div>
            <time :datetime="task.dueDate || undefined">{{ formatDueDate(task.dueDate) }}</time>
          </li>
        </ul>
      </section>
    </div>
  </main>
</template>
