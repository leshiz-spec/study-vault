<script setup lang="ts">
import { onMounted, ref } from "vue";
import { listTrash, permanentlyDeleteNote, restoreNote, type Note } from "../api";

const notes = ref<Note[]>([]);
const loading = ref(true);
const error = ref("");
const expandedSections = ref<Set<string>>(new Set());
const NOTE_PREVIEW_LIMIT = 280;
const SUMMARY_PREVIEW_LIMIT = 360;

function previewContent(content: string) {
  return content
    .replace(/<img\b[^>]*\balt="([^"]*)"[^>]*>/gi, (_, name: string) => `[Image: ${name || "embedded photo"}]`)
    .replace(/<img\b[^>]*>/gi, "[Image: embedded photo]")
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, (_, name: string) => `[Image: ${name || "embedded photo"}]`)
    .replace(/\s+/g, " ")
    .trim();
}
function sectionKey(noteId: number, section: "content" | "summary") {
  return `${noteId}:${section}`;
}
function isExpanded(noteId: number, section: "content" | "summary") {
  return expandedSections.value.has(sectionKey(noteId, section));
}
function isLong(text: string | null | undefined, limit: number) {
  return Boolean(text && text.length > limit);
}
function excerpt(text: string | null | undefined, limit: number, noteId: number, section: "content" | "summary") {
  const value = text || "";
  if (!isLong(value, limit) || isExpanded(noteId, section)) return value;
  return `${value.slice(0, limit).trimEnd()}…`;
}
function toggleExpanded(noteId: number, section: "content" | "summary") {
  const key = sectionKey(noteId, section);
  const next = new Set(expandedSections.value);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  expandedSections.value = next;
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const result = await listTrash();
    expandedSections.value = new Set();
    notes.value = result.map((note) => ({ ...note, content: previewContent(note.content) }));
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to load trash";
  } finally {
    loading.value = false;
  }
}
async function restore(note: Note) {
  try {
    await restoreNote(note.id);
    notes.value = notes.value.filter((item) => item.id !== note.id);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to restore note";
  }
}
async function permanentlyDelete(note: Note) {
  if (!window.confirm(`Permanently delete “${note.title}”? This cannot be undone.`)) return;
  try {
    await permanentlyDeleteNote(note.id);
    notes.value = notes.value.filter((item) => item.id !== note.id);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to permanently delete note";
  }
}
onMounted(load);
</script>

<template>
  <main class="notes-page">
    <header class="notes-header">
      <div>
        <RouterLink class="back-link" to="/notes">← Back to notes</RouterLink>
        <span class="eyebrow">TRASH</span>
        <h1>Trash</h1>
        <p>Notes here are hidden from your active workspace until restored.</p>
      </div>
    </header>
    <p v-if="loading" class="state">Loading trash…</p>
    <div v-else-if="error" class="error state">
      {{ error }} <button class="link-button" @click="load">Retry</button>
    </div>
    <div v-else-if="!notes.length" class="empty state">
      <h2>Trash is empty</h2>
      <p>Deleted notes will appear here.</p>
    </div>
    <section v-else class="notes-list">
      <article v-for="note in notes" :key="note.id" class="note-card">
        <div class="note-main">
          <h2>{{ note.title }}</h2>
          <p>{{ excerpt(note.content || "No content", NOTE_PREVIEW_LIMIT, note.id, "content") }}</p>
          <span
            v-if="isLong(note.content, NOTE_PREVIEW_LIMIT)"
            class="view-more-link"
            role="button"
            tabindex="0"
            @click="toggleExpanded(note.id, 'content')"
            @keydown.enter="toggleExpanded(note.id, 'content')"
            @keydown.space.prevent="toggleExpanded(note.id, 'content')"
          >
            {{ isExpanded(note.id, "content") ? "View less" : "View more" }}
          </span>
          <p v-if="note.summary" class="note-summary-preview">
            <strong>Summary:</strong>
            {{ excerpt(note.summary, SUMMARY_PREVIEW_LIMIT, note.id, "summary") }}
          </p>
          <span
            v-if="isLong(note.summary, SUMMARY_PREVIEW_LIMIT)"
            class="view-more-link summary-view-more"
            role="button"
            tabindex="0"
            @click="toggleExpanded(note.id, 'summary')"
            @keydown.enter="toggleExpanded(note.id, 'summary')"
            @keydown.space.prevent="toggleExpanded(note.id, 'summary')"
          >
            {{ isExpanded(note.id, "summary") ? "View less" : "View more" }}
          </span>
        </div>
        <div class="note-card-actions">
          <button class="restore-link" @click="restore(note)">Restore</button>
          <button class="danger-link" @click="permanentlyDelete(note)">Delete permanently</button>
        </div>
      </article>
    </section>
  </main>
</template>
