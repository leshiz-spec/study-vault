<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import {
  addTagToNote,
  deleteNote,
  exportAllNotes,
  exportNote,
  importMarkdown,
  listTags,
  removeTagFromNote,
  searchNotes,
  toggleFavorite,
  type Note,
  type Tag,
} from "../api";
import { shortcutModifier } from "../keyboard";
import { tagColor as colorForTag } from "../tagPalette";
const router = useRouter();
const notes = ref<Note[]>([]);
const tags = ref<Tag[]>([]);
const loading = ref(true);
const error = ref("");
const selectedTag = ref<number | null>(null);
const search = ref("");
const favoriteOnly = ref(false);
const sort = ref("updatedAt,desc");
const page = ref(0);
const size = ref(10);
const totalElements = ref(0);
const totalPages = ref(0);
const importInput = ref<HTMLInputElement | null>(null);
const searchInput = ref<HTMLInputElement | null>(null);
const transferState = ref("");
const transferError = ref("");
const expandedSections = ref<Set<string>>(new Set());
const NOTE_PREVIEW_LIMIT = 280;
const SUMMARY_PREVIEW_LIMIT = 360;
function tagColor(tag?: Tag) {
  return colorForTag(tag?.color);
}
function previewContent(content: string) {
  return content
    .replace(
      /<img\b[^>]*\balt="([^"]*)"[^>]*>/gi,
      (_, name: string) => `[Image: ${name || "embedded photo"}]`,
    )
    .replace(/<img\b[^>]*>/gi, "[Image: embedded photo]")
    .replace(
      /!\[([^\]]*)\]\([^)]*\)/g,
      (_, name: string) => `[Image: ${name || "embedded photo"}]`,
    )
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
function excerpt(
  text: string | null | undefined,
  limit: number,
  noteId: number,
  section: "content" | "summary",
) {
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
const hasSearch = computed(() =>
  Boolean(search.value.trim() || selectedTag.value || favoriteOnly.value),
);
async function load(resetPage = false) {
  if (resetPage) page.value = 0;
  loading.value = true;
  error.value = "";
  try {
    const result = await searchNotes({
      q: search.value,
      tag: selectedTag.value ? String(selectedTag.value) : undefined,
      favorite: favoriteOnly.value ? true : undefined,
      status: "active",
      page: page.value,
      size: size.value,
      sort: sort.value,
    });
    expandedSections.value = new Set();
    notes.value = result.content.map((note) => ({
      ...note,
      content: previewContent(note.content),
    }));
    totalElements.value = result.totalElements;
    totalPages.value = result.totalPages;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to load notes";
  } finally {
    loading.value = false;
  }
}
async function loadTags() {
  try {
    tags.value = await listTags();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to load tags";
  }
}
async function remove(note: Note) {
  if (!window.confirm(`Delete “${note.title}”?`)) return;
  try {
    await deleteNote(note.id);
    notes.value = notes.value.filter((item) => item.id !== note.id);
    totalElements.value = Math.max(0, totalElements.value - 1);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to delete note";
  }
}
async function assign(note: Note, tagId: number) {
  try {
    const updated = await addTagToNote(note.id, tagId);
    note.tags = updated.tags || [
      ...(note.tags || []),
      tags.value.find((tag) => tag.id === tagId)!,
    ];
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to assign tag";
  }
}
async function unassign(note: Note, tag: Tag) {
  try {
    await removeTagFromNote(note.id, tag.id);
    note.tags = (note.tags || []).filter((item) => item.id !== tag.id);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to remove tag";
  }
}
async function favorite(note: Note) {
  try {
    const updated = await toggleFavorite(note.id);
    note.favorite = updated.favorite;
    if (favoriteOnly.value && !note.favorite)
      notes.value = notes.value.filter((item) => item.id !== note.id);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to update favorite";
  }
}
function saveDownload(file: { blob: Blob; filename: string }) {
  const url = URL.createObjectURL(file.blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = file.filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Let the browser start the download before releasing the temporary Blob URL.
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
async function downloadSingle(note: Note) {
  transferState.value = "";
  transferError.value = "";
  try {
    saveDownload(await exportNote(note.id));
    transferState.value = "Note exported successfully.";
  } catch (e) {
    transferError.value =
      e instanceof Error ? e.message : "Unable to export note";
  }
}
async function downloadAll() {
  transferState.value = "";
  transferError.value = "";
  try {
    saveDownload(await exportAllNotes());
    transferState.value = "All notes exported successfully.";
  } catch (e) {
    transferError.value =
      e instanceof Error ? e.message : "Unable to export notes";
  }
}
function openImportPicker() {
  importInput.value?.click();
}
async function handleImport(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;
  transferState.value = "";
  transferError.value = "";
  if (!file.name.toLowerCase().endsWith(".md")) {
    transferError.value = "Only .md Markdown files can be imported.";
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    transferError.value = "Markdown files must be at most 5 MB.";
    return;
  }
  try {
    await importMarkdown(file);
    transferState.value = "Markdown note imported successfully.";
    await load(true);
  } catch (e) {
    transferError.value =
      e instanceof Error ? e.message : "Unable to import Markdown file";
  }
}
function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString() : "";
}
function goToPage(next: number) {
  if (next < 0 || next >= totalPages.value || next === page.value) return;
  page.value = next;
  load();
}
function focusSearch() {
  searchInput.value?.focus();
  searchInput.value?.select();
}
watch([search, selectedTag, favoriteOnly, sort, size], () => load(true));
onMounted(async () => {
  window.addEventListener("studyvault:focus-search", focusSearch);
  await Promise.all([loadTags(), load()]);
});
onUnmounted(() =>
  window.removeEventListener("studyvault:focus-search", focusSearch),
);
</script>
<template>
  <main class="notes-page">
    <header class="notes-header">
      <div>
        <span class="eyebrow">YOUR WORKSPACE</span>
        <h1>Notes</h1>
        <p>Capture ideas, lessons, and everything worth remembering.</p>
        <div class="shortcut-hints" aria-label="Keyboard shortcuts">
          <span
            ><kbd>{{ shortcutModifier }} K</kbd> Search</span
          >
          <span
            ><kbd>{{ shortcutModifier }} N</kbd> New note</span
          >
        </div>
      </div>
      <div class="notes-actions">
        <input
          ref="importInput"
          class="visually-hidden"
          type="file"
          accept=".md,text/markdown,text/plain"
          @change="handleImport"
        />
        <button
          class="button secondary"
          type="button"
          @click="openImportPicker"
        >
          Import Markdown
        </button>
        <button class="button secondary" type="button" @click="downloadAll">
          Export All
        </button>
        <RouterLink class="button secondary" to="/tags">Manage tags</RouterLink
        ><RouterLink class="button" to="/notes/new">New note</RouterLink>
      </div>
    </header>
    <p v-if="transferState" class="transfer-state">{{ transferState }}</p>
    <p v-if="transferError" class="error transfer-state">{{ transferError }}</p>
    <section class="search-toolbar" aria-label="Search and filter notes">
      <label class="search-field"
        ><span>Search notes</span
        ><input
          ref="searchInput"
          v-model="search"
          type="search"
          placeholder="Search title or content…" /></label
      ><label
        ><span>Tag</span
        ><select v-model="selectedTag">
          <option :value="null">All tags</option>
          <option v-for="tag in tags" :key="tag.id" :value="tag.id">
            {{ tag.name }}
          </option>
        </select></label
      ><label class="checkbox-filter"
        ><input v-model="favoriteOnly" type="checkbox" /> Favorites</label
      ><label
        ><span>Sort</span
        ><select v-model="sort">
          <option value="updatedAt,desc">Recently updated</option>
          <option value="updatedAt,asc">Oldest updated</option>
          <option value="createdAt,desc">Recently created</option>
          <option value="createdAt,asc">Oldest created</option>
        </select></label
      >
    </section>
    <div v-if="tags.length" class="tag-filter">
      <button
        class="tag-filter-all"
        :class="{ active: !selectedTag }"
        @click="selectedTag = null"
      >
        All</button
      ><button
        v-for="tag in tags"
        :key="tag.id"
        class="tag-filter-pill"
        :style="{ backgroundColor: tagColor(tag), color: '#fff' }"
        @click="selectedTag = tag.id"
      >
        # {{ tag.name }}
      </button>
    </div>
    <div v-if="!loading && !error" class="notes-summary">
      <div>
        <strong>{{ totalElements }}</strong
        ><span>{{ hasSearch ? "Matching notes" : "Active notes" }}</span>
      </div>
      <div><strong>∞</strong><span>Your ideas, organized</span></div>
      <p>
        {{
          hasSearch
            ? "Refine your filters to find exactly what you need."
            : "Keep your knowledge growing with a new note today."
        }}
      </p>
    </div>
    <p v-if="loading" class="state">Loading notes…</p>
    <div v-else-if="error" class="error state">
      {{ error }} <button class="link-button" @click="load()">Retry</button>
    </div>
    <div v-else-if="notes.length === 0" class="empty state">
      <div class="empty-icon">✦</div>
      <h2>{{ hasSearch ? "No matching notes" : "No notes yet" }}</h2>
      <p>
        {{
          hasSearch
            ? "Try a different search or filter."
            : "Create your first note to get started."
        }}
      </p>
      <RouterLink v-if="!hasSearch" class="button" to="/notes/new"
        >Create note</RouterLink
      >
    </div>
    <section v-else class="notes-list">
      <article v-for="note in notes" :key="note.id" class="note-card">
        <div
          class="note-main"
          role="button"
          tabindex="0"
          @click="router.push(`/notes/${note.id}`)"
          @keydown.enter="router.push(`/notes/${note.id}`)"
          @keydown.space.prevent="router.push(`/notes/${note.id}`)"
        >
          <h2>{{ note.title }}</h2>
          <p>
            {{
              excerpt(
                note.content || "No content",
                NOTE_PREVIEW_LIMIT,
                note.id,
                "content",
              )
            }}
          </p>
          <span
            v-if="isLong(note.content, NOTE_PREVIEW_LIMIT)"
            class="view-more-link"
            role="button"
            tabindex="0"
            @click.stop="toggleExpanded(note.id, 'content')"
            @keydown.enter.stop="toggleExpanded(note.id, 'content')"
            @keydown.space.prevent.stop="toggleExpanded(note.id, 'content')"
          >
            {{ isExpanded(note.id, "content") ? "View less" : "View more" }}
          </span>
          <p v-if="note.summary" class="note-summary-preview">
            <strong>Summary:</strong>
            {{
              excerpt(note.summary, SUMMARY_PREVIEW_LIMIT, note.id, "summary")
            }}
          </p>
          <span
            v-if="isLong(note.summary, SUMMARY_PREVIEW_LIMIT)"
            class="view-more-link summary-view-more"
            role="button"
            tabindex="0"
            @click.stop="toggleExpanded(note.id, 'summary')"
            @keydown.enter.stop="toggleExpanded(note.id, 'summary')"
            @keydown.space.prevent.stop="toggleExpanded(note.id, 'summary')"
          >
            {{ isExpanded(note.id, "summary") ? "View less" : "View more" }}
          </span>
          <div class="note-tags">
            <button
              v-for="tag in note.tags || []"
              :key="tag.id"
              class="tag-chip"
              :style="{ backgroundColor: tagColor(tag), color: '#fff' }"
              @click.stop="unassign(note, tag)"
            >
              # {{ tag.name }} ×</button
            ><select
              class="tag-select"
              @change="
                assign(
                  note,
                  Number(($event.target as HTMLSelectElement).value),
                );
                ($event.target as HTMLSelectElement).value = '';
              "
              @click.stop
              aria-label="Assign tag"
            >
              <option value="">+ tag</option>
              <option
                v-for="tag in tags.filter(
                  (candidate) =>
                    !(note.tags || []).some(
                      (current) => current.id === candidate.id,
                    ),
                )"
                :key="tag.id"
                :value="tag.id"
              >
                {{ tag.name }}
              </option>
            </select>
          </div>
          <small>Updated {{ formatDate(note.updatedAt) }}</small>
        </div>
        <div class="note-card-actions">
          <button
            type="button"
            class="favorite-button"
            :class="{ active: note.favorite }"
            @click="favorite(note)"
            :aria-label="note.favorite ? 'Remove favorite' : 'Add favorite'"
          >
            {{ note.favorite ? "★" : "☆" }}
          </button>
          <div class="note-edit-actions">
            <button
              class="button secondary note-action-button"
              type="button"
              @click.stop="downloadSingle(note)"
            >
              Export</button
            ><RouterLink
              class="button secondary note-action-button"
              :to="`/notes/${note.id}`"
              >Edit</RouterLink
            ><button
              class="button secondary note-action-button note-delete-button"
              type="button"
              @click="remove(note)"
            >
              Delete
            </button>
          </div>
        </div>
      </article>
    </section>
    <nav
      v-if="!loading && !error && totalPages > 1"
      class="pagination"
      aria-label="Notes pagination"
    >
      <button :disabled="page === 0" @click="goToPage(page - 1)">
        ← Previous</button
      ><span>Page {{ page + 1 }} of {{ totalPages }}</span
      ><button :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">
        Next →</button
      ><label
        >Per page
        <select v-model.number="size">
          <option :value="5">5</option>
          <option :value="10">10</option>
          <option :value="20">20</option>
        </select></label
      >
    </nav>
  </main>
</template>
