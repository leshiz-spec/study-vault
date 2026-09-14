<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { marked } from "marked";
import DOMPurify from "dompurify";
import {
  addTagToNote,
  createNote,
  getNote,
  listNoteRevisions,
  listTags,
  removeTagFromNote,
  restoreNoteRevision,
  saveNoteSummary,
  summarizeNote,
  updateNote,
  updateNoteReviewStatus,
  type ReviewStatus,
  type NoteRevision,
  type Tag,
} from "../api";
import { useAuthStore } from "../stores/auth";
import { hasShortcutModifier, shortcutModifier } from "../keyboard";
import { tagColor as colorForTag } from "../tagPalette";
const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const id = route.params.id ? Number(route.params.id) : null;
const title = ref("");
const content = ref("");
const reviewStatus = ref<ReviewStatus>("not_started");
const noteTags = ref<Tag[]>([]);
const persistedTagIds = ref<number[]>([]);
const availableTags = ref<Tag[]>([]);
const mode = ref<"edit" | "preview">("edit");
const loading = ref(Boolean(id));
const saving = ref(false);
const error = ref("");
const autosaveState = ref<"idle" | "saving" | "saved" | "failed">("idle");
const manuallySaved = ref(false);
const summaryDraft = ref("");
const summaryLoading = ref(false);
const summarySaving = ref(false);
const summaryError = ref("");
const summarySaved = ref(false);
const revisions = ref<NoteRevision[]>([]);
const revisionLoading = ref(false);
const revisionError = ref("");
const selectedRevision = ref<NoteRevision | null>(null);
const restoringRevision = ref(false);
const renderedMarkdown = computed(() =>
  DOMPurify.sanitize(marked.parse(content.value, { async: false }) as string),
);
const renderedRevisionMarkdown = computed(() =>
  selectedRevision.value
    ? DOMPurify.sanitize(
        marked.parse(selectedRevision.value.content, {
          async: false,
        }) as string,
      )
    : "",
);
function tagColor(tag?: Tag) {
  return colorForTag(tag?.color);
}
let persistedTitle = "";
let persistedContent = "";
let persistedReviewStatus: ReviewStatus = "not_started";
let autosaveTimer: ReturnType<typeof setTimeout> | undefined;
const draftKey = (noteId: number | null = id) =>
  `studyvault:note-draft:${auth.user?.id ?? auth.user?.username ?? "anonymous"}:${noteId ?? "new"}`;
const contentInput = ref<HTMLTextAreaElement | null>(null);
const imageInput = ref<HTMLInputElement | null>(null);
const imagePattern = /<img\b[^>]*data-image-id="([^"]+)"[^>]*>/gi;
const images = computed(() => {
  const found: {
    id: string;
    name: string;
    width: number;
    height: number;
    src: string;
    index: number;
  }[] = [];
  let match: RegExpExecArray | null;
  imagePattern.lastIndex = 0;
  while ((match = imagePattern.exec(content.value))) {
    const tag = match[0];
    const widthMatch = tag.match(/\bwidth="?(\d+)/i);
    const heightMatch = tag.match(/\bheight="?(\d+)/i);
    const srcMatch = tag.match(/\bsrc="([^"]+)"/i);
    const nameMatch = tag.match(/\balt="([^"]*)"/i);
    found.push({
      id: match[1],
      name: nameMatch?.[1] || "embedded photo",
      width: widthMatch ? Number(widthMatch[1]) : 480,
      height: heightMatch ? Number(heightMatch[1]) : 320,
      src: srcMatch?.[1] || "",
      index: match.index,
    });
  }
  return found;
});
const cropImageId = ref<string | null>(null);
const cropX = ref(0);
const cropY = ref(0);
const cropWidth = ref(0);
const cropHeight = ref(0);
function openImagePicker() {
  imageInput.value?.click();
}
function insertImage(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (!file.type.startsWith("image/")) {
    error.value = "Please choose an image file.";
    input.value = "";
    return;
  }
  const reader = new FileReader();
  reader.onload = () => {
    const id = `img-${Date.now()}`;
    const image = `<img src="${reader.result as string}" data-image-id="${id}" width="640" height="480" alt="${file.name.replace(/"/g, "")}">`;
    const element = contentInput.value;
    const start = element?.selectionStart ?? content.value.length;
    const end = element?.selectionEnd ?? start;
    content.value = `${content.value.slice(0, start)}\n${image}\n${content.value.slice(end)}`;
    nextTick(() => {
      if (element) {
        const cursor = start + image.length + 2;
        element.focus();
        element.setSelectionRange(cursor, cursor);
      }
    });
    input.value = "";
  };
  reader.readAsDataURL(file);
}
function updateImageDimension(
  imageId: string,
  dimension: "width" | "height",
  event: Event,
) {
  const value = Math.max(
    1,
    Math.round(Number((event.target as HTMLInputElement).value) || 1),
  );
  const pattern = new RegExp(
    `(<img\\b[^>]*data-image-id="${imageId}"[^>]*\\b${dimension}=")\\d+`,
    "i",
  );
  content.value = content.value.replace(
    pattern,
    (_match, prefix: string) => `${prefix}${value}`,
  );
}
function updateImageName(imageId: string, event: Event) {
  const value =
    (event.target as HTMLInputElement).value.replace(/"/g, "").trim() ||
    "embedded photo";
  const pattern = new RegExp(
    `(<img\\b[^>]*data-image-id="${imageId}"[^>]*\\balt=")[^"]*`,
    "i",
  );
  content.value = content.value.replace(
    pattern,
    (_match, prefix: string) => `${prefix}${value}`,
  );
}
function beginCrop(image: { id: string; width: number; height: number }) {
  cropImageId.value = image.id;
  cropX.value = 0;
  cropY.value = 0;
  cropWidth.value = image.width;
  cropHeight.value = image.height;
}
function applyCrop(image: { id: string; src: string }) {
  const x = Math.max(0, cropX.value);
  const y = Math.max(0, cropY.value);
  const width = Math.max(1, cropWidth.value);
  const height = Math.max(1, cropHeight.value);
  const source = new Image();
  source.onload = () => {
    const sx = Math.min(x, Math.max(0, source.naturalWidth - 1));
    const sy = Math.min(y, Math.max(0, source.naturalHeight - 1));
    const sw = Math.min(width, source.naturalWidth - sx);
    const sh = Math.min(height, source.naturalHeight - sy);
    const canvas = document.createElement("canvas");
    canvas.width = sw;
    canvas.height = sh;
    canvas.getContext("2d")?.drawImage(source, sx, sy, sw, sh, 0, 0, sw, sh);
    const data = canvas.toDataURL("image/png");
    const pattern = new RegExp(
      `(<img\\b[^>]*data-image-id="${image.id}"[^>]*\\bsrc=")[^"]+`,
      "i",
    );
    content.value = content.value
      .replace(pattern, `$1${data}`)
      .replace(
        new RegExp(
          `(<img\\b[^>]*data-image-id="${image.id}"[^>]*\\bwidth=")\\d+`,
          "i",
        ),
        `$1${sw}`,
      )
      .replace(
        new RegExp(
          `(<img\\b[^>]*data-image-id="${image.id}"[^>]*\\bheight=")\\d+`,
          "i",
        ),
        `$1${sh}`,
      );
    cropImageId.value = null;
  };
  source.src = image.src;
}
function moveImage(imageId: string, direction: -1 | 1) {
  const lines = content.value.split("\n");
  const index = lines.findIndex((line) =>
    line.includes(`data-image-id="${imageId}"`),
  );
  if (index < 0) return;
  const target = index + direction;
  if (target < 0 || target >= lines.length) return;
  [lines[index], lines[target]] = [lines[target], lines[index]];
  content.value = lines.join("\n");
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    availableTags.value = await listTags();
    if (id) {
      const note = await getNote(id);
      title.value = note.title;
      content.value = note.content;
      reviewStatus.value = (note.reviewStatus || "not_started") as ReviewStatus;
      summaryDraft.value = note.summary || "";
      noteTags.value = note.tags || [];
      persistedTagIds.value = noteTags.value.map((tag) => tag.id);
      const draft = localStorage.getItem(draftKey(id));
      if (draft) {
        const parsed = JSON.parse(draft) as {
          title?: string;
          content?: string;
        };
        title.value = parsed.title || "";
        content.value = parsed.content || "";
        autosaveState.value = "saved";
      }
      persistedTitle = note.title;
      persistedContent = note.content;
      persistedReviewStatus = reviewStatus.value;
      await loadRevisions();
    } else {
      const draft = localStorage.getItem(draftKey(null));
      if (draft) {
        const parsed = JSON.parse(draft) as {
          title?: string;
          content?: string;
        };
        title.value = parsed.title || "";
        content.value = parsed.content || "";
        persistedTitle = title.value;
        persistedContent = content.value;
        autosaveState.value = "saved";
      }
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to load note";
  } finally {
    loading.value = false;
  }
}
async function loadRevisions() {
  if (!id) return;
  revisionLoading.value = true;
  revisionError.value = "";
  try {
    revisions.value = await listNoteRevisions(id);
    if (selectedRevision.value) {
      selectedRevision.value =
        revisions.value.find(
          (revision) => revision.id === selectedRevision.value?.id,
        ) || null;
    }
  } catch (e) {
    revisionError.value =
      e instanceof Error ? e.message : "Unable to load version history";
  } finally {
    revisionLoading.value = false;
  }
}
function selectRevision(revision: NoteRevision) {
  selectedRevision.value = revision;
}
async function restoreRevision() {
  if (!id || !selectedRevision.value) return;
  if (
    !window.confirm(
      `Restore version from ${formatRevisionDate(selectedRevision.value.createdAt)}?`,
    )
  )
    return;
  restoringRevision.value = true;
  revisionError.value = "";
  try {
    const restored = await restoreNoteRevision(id, selectedRevision.value.id);
    title.value = restored.title;
    content.value = restored.content;
    persistedTitle = restored.title;
    persistedContent = restored.content;
    localStorage.removeItem(draftKey(id));
    autosaveState.value = "saved";
    await loadRevisions();
    selectedRevision.value = null;
  } catch (e) {
    revisionError.value =
      e instanceof Error ? e.message : "Unable to restore version";
  } finally {
    restoringRevision.value = false;
  }
}
function formatRevisionDate(value?: string) {
  return value ? new Date(value).toLocaleString() : "Unknown time";
}
async function generateSummary() {
  if (!id) return;
  summaryLoading.value = true;
  summaryError.value = "";
  summarySaved.value = false;
  try {
    const result = await summarizeNote(id);
    summaryDraft.value = result.summary;
    summarySaving.value = true;
    const saved = await saveNoteSummary(id, result.summary);
    summaryDraft.value = saved.summary || result.summary;
    summarySaved.value = true;
  } catch (e) {
    summaryError.value =
      e instanceof Error ? e.message : "Unable to generate and save summary";
  } finally {
    summaryLoading.value = false;
    summarySaving.value = false;
  }
}
async function assignTag(tagId: number) {
  if (!id) return;
  const tag = availableTags.value.find((candidate) => candidate.id === tagId);
  if (tag && !noteTags.value.some((current) => current.id === tagId))
    noteTags.value = [...noteTags.value, tag];
}
async function removeTag(tag: Tag) {
  if (!id) return;
  noteTags.value = noteTags.value.filter((item) => item.id !== tag.id);
}
async function saveTags(noteId: number) {
  const saved = new Set(persistedTagIds.value);
  const selected = new Set(noteTags.value.map((tag) => tag.id));
  for (const tagId of selected) {
    if (!saved.has(tagId)) await addTagToNote(noteId, tagId);
  }
  for (const tagId of saved) {
    if (!selected.has(tagId)) await removeTagFromNote(noteId, tagId);
  }
  persistedTagIds.value = [...selected];
}
function persistDraft() {
  if (
    loading.value ||
    (title.value === persistedTitle && content.value === persistedContent)
  )
    return;
  autosaveState.value = "saving";
  try {
    if (!title.value.trim() && !content.value.trim())
      localStorage.removeItem(draftKey());
    else
      localStorage.setItem(
        draftKey(),
        JSON.stringify({ title: title.value, content: content.value }),
      );
    persistedTitle = title.value;
    persistedContent = content.value;
    autosaveState.value = "saved";
  } catch (e) {
    autosaveState.value = "failed";
    error.value = e instanceof Error ? e.message : "Autosave failed";
  }
}
function scheduleAutosave() {
  if (loading.value) return;
  autosaveState.value = "idle";
  if (autosaveTimer) clearTimeout(autosaveTimer);
  autosaveTimer = setTimeout(persistDraft, 800);
}
async function save() {
  if (autosaveTimer) clearTimeout(autosaveTimer);
  saving.value = true;
  error.value = "";
  try {
    if (id) {
      await updateNote(id, { title: title.value, content: content.value });
      await saveTags(id);
      if (reviewStatus.value !== persistedReviewStatus) {
        await updateNoteReviewStatus(id, reviewStatus.value);
        persistedReviewStatus = reviewStatus.value;
      }
    } else {
      const created = await createNote({
        title: title.value,
        content: content.value,
      });
      if (reviewStatus.value !== "not_started")
        await updateNoteReviewStatus(created.id, reviewStatus.value);
    }
    manuallySaved.value = true;
    localStorage.removeItem(draftKey());
    autosaveState.value = "saved";
    await router.push("/notes");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to save note";
  } finally {
    saving.value = false;
  }
}
function handleEditorShortcut(event: KeyboardEvent) {
  if (hasShortcutModifier(event) && event.key.toLowerCase() === "s") {
    event.preventDefault();
    if (!id || saving.value) return;
    void save();
    return;
  }
  if (event.key !== "Escape") return;
  if (cropImageId.value) {
    cropImageId.value = null;
  } else if (selectedRevision.value) {
    selectedRevision.value = null;
  } else if (mode.value === "preview") {
    mode.value = "edit";
  }
}
onMounted(load);
onMounted(() => window.addEventListener("keydown", handleEditorShortcut));
onUnmounted(() => {
  window.removeEventListener("keydown", handleEditorShortcut);
  if (autosaveTimer) clearTimeout(autosaveTimer);
  if (
    !manuallySaved.value &&
    !loading.value &&
    (title.value.trim() || content.value.trim())
  ) {
    try {
      localStorage.setItem(
        draftKey(),
        JSON.stringify({ title: title.value, content: content.value }),
      );
    } catch {}
  }
});
watch([title, content], scheduleAutosave);
</script>
<template>
  <main class="editor-page">
    <header class="notes-header">
      <div>
        <RouterLink class="back-link" to="/notes">← Back to notes</RouterLink
        ><span class="eyebrow">NOTE EDITOR</span>
        <h1>{{ id ? "Edit note" : "New note" }}</h1>
        <p>
          {{
            id
              ? "Refine your thoughts and keep them useful."
              : "Turn a thought into something you can return to."
          }}
        </p>
        <div class="shortcut-hints" aria-label="Keyboard shortcuts">
          <span v-if="id"
            ><kbd>{{ shortcutModifier }} S</kbd> Save note</span
          >
          <span><kbd>Esc</kbd> Close preview</span>
        </div>
      </div>
    </header>
    <p v-if="loading" class="state">Loading note…</p>
    <form v-else class="editor-card" @submit.prevent="save">
      <p v-if="error" class="error">{{ error }}</p>
      <label
        >Title<input
          v-model="title"
          required
          maxlength="255"
          placeholder="Note title"
      /></label>
      <label class="review-status-field"
        >Review status<select v-model="reviewStatus" aria-label="Review status">
          <option value="not_started">Not started</option>
          <option value="learning">Learning</option>
          <option value="review">Review</option>
          <option value="mastered">Mastered</option>
        </select></label
      >
      <div v-if="id" class="assigned-tags">
        <span
          v-for="tag in noteTags"
          :key="tag.id"
          class="tag-chip"
          :style="{ backgroundColor: tagColor(tag), color: '#fff' }"
          @click="removeTag(tag)"
          ># {{ tag.name }} ×</span
        ><select
          class="tag-select"
          @change="
            assignTag(Number(($event.target as HTMLSelectElement).value));
            ($event.target as HTMLSelectElement).value = '';
          "
          aria-label="Assign tag"
        >
          <option value="">+ Add tag</option>
          <option
            v-for="tag in availableTags.filter(
              (candidate) =>
                !noteTags.some((current) => current.id === candidate.id),
            )"
            :key="tag.id"
            :value="tag.id"
          >
            {{ tag.name }}
          </option></select
        ><RouterLink class="tag-manage-link" to="/tags">Manage tags</RouterLink>
      </div>
      <div class="mode-tabs" role="tablist">
        <button
          type="button"
          :class="{ active: mode === 'edit' }"
          @click="mode = 'edit'"
        >
          Edit</button
        ><button
          type="button"
          :class="{ active: mode === 'preview' }"
          @click="mode = 'preview'"
        >
          Preview
        </button>
      </div>
      <div v-if="mode === 'edit'" class="markdown-editor">
        <label
          >Content<textarea
            ref="contentInput"
            v-model="content"
            required
            placeholder="Write your note in Markdown"
          ></textarea>
        </label>
        <div
          class="autosave-indicator"
          :class="`autosave-${autosaveState}`"
          aria-live="polite"
        >
          {{
            autosaveState === "saving"
              ? "Saving..."
              : autosaveState === "failed"
                ? "Save failed"
                : autosaveState === "saved"
                  ? "Saved"
                  : ""
          }}
        </div>
        <div class="image-toolbar">
          <input
            ref="imageInput"
            class="visually-hidden"
            type="file"
            accept="image/*"
            @change="insertImage"
          /><button
            type="button"
            class="button secondary"
            @click="openImagePicker"
          >
            Add photo</button
          ><small
            >Photos are inserted at the cursor. Use the controls below to
            resize, crop, or move them.</small
          >
        </div>
        <div v-if="images.length" class="image-list">
          <div
            v-for="(image, index) in images"
            :key="image.id"
            class="image-control"
          >
            <span>Photo {{ index + 1 }}</span
            ><label class="image-name"
              >Name
              <input
                type="text"
                :value="image.name"
                maxlength="255"
                @change="updateImageName(image.id, $event)" /></label
            ><label
              >W
              <input
                type="number"
                min="1"
                :value="image.width"
                @change="
                  updateImageDimension(image.id, 'width', $event)
                " /></label
            ><label
              >H
              <input
                type="number"
                min="1"
                :value="image.height"
                @change="
                  updateImageDimension(image.id, 'height', $event)
                " /></label
            ><button type="button" @click="beginCrop(image)">Crop</button
            ><button
              type="button"
              @click="moveImage(image.id, -1)"
              :disabled="index === 0"
            >
              Move up</button
            ><button
              type="button"
              @click="moveImage(image.id, 1)"
              :disabled="index === images.length - 1"
            >
              Move down
            </button>
          </div>
        </div>
        <div v-if="cropImageId" class="crop-panel">
          <strong>Crop selected photo</strong>
          <div class="crop-fields">
            <label
              >X <input v-model.number="cropX" type="number" min="0" /></label
            ><label
              >Y <input v-model.number="cropY" type="number" min="0" /></label
            ><label
              >Width
              <input v-model.number="cropWidth" type="number" min="1" /></label
            ><label
              >Height <input v-model.number="cropHeight" type="number" min="1"
            /></label>
          </div>
          <div class="crop-actions">
            <button type="button" @click="cropImageId = null">
              Cancel crop</button
            ><button
              type="button"
              class="button"
              @click="
                applyCrop(images.find((image) => image.id === cropImageId)!)
              "
            >
              Apply crop
            </button>
          </div>
        </div>
      </div>
      <div
        v-else
        class="markdown-preview"
        v-html="renderedMarkdown"
        aria-label="Markdown preview"
      ></div>
      <section
        v-if="id"
        class="revision-panel"
        aria-label="Note version history"
      >
        <div class="revision-header">
          <div>
            <strong>Version History</strong>
            <small>Current content stays in the editor above.</small>
          </div>
        </div>
        <p v-if="revisionError" class="error">{{ revisionError }}</p>
        <p v-if="revisionLoading" class="autosave-indicator autosave-saving">
          Loading history...
        </p>
        <p v-else-if="!revisions.length" class="revision-empty">
          No earlier versions yet. Edit and save this note to create one.
        </p>
        <div v-else class="revision-layout">
          <div class="revision-list">
            <button
              v-for="revision in revisions"
              :key="revision.id"
              type="button"
              class="revision-item"
              :class="{ selected: selectedRevision?.id === revision.id }"
              @click="selectRevision(revision)"
            >
              <strong>{{ formatRevisionDate(revision.createdAt) }}</strong>
              <span>{{ revision.title }}</span>
            </button>
          </div>
          <div v-if="selectedRevision" class="revision-preview">
            <span class="revision-label">Historical version</span>
            <h3>{{ selectedRevision.title }}</h3>
            <div
              class="markdown-preview"
              v-html="renderedRevisionMarkdown"
            ></div>
            <button
              type="button"
              class="button"
              :disabled="restoringRevision"
              @click="restoreRevision"
            >
              {{ restoringRevision ? "Restoring..." : "Restore this version" }}
            </button>
          </div>
          <p v-else class="revision-empty">Select a version to preview it.</p>
        </div>
      </section>
      <section v-if="id" class="summary-panel" aria-label="AI note summary">
        <div class="summary-header">
          <strong>AI summary</strong>
          <button
            type="button"
            class="button secondary"
            :disabled="summaryLoading || summarySaving"
            @click="generateSummary"
          >
            {{ summaryLoading ? "Generating..." : "Generate Summary" }}
          </button>
        </div>
        <p v-if="summaryError" class="error">{{ summaryError }}</p>
        <p v-if="summaryLoading" class="autosave-indicator autosave-saving">
          Generating summary...
        </p>
        <textarea
          v-if="summaryDraft"
          v-model="summaryDraft"
          readonly
          class="summary-draft"
          aria-label="Generated summary"
        ></textarea>
        <div v-if="summaryDraft" class="summary-actions">
          <span v-if="summarySaved" class="autosave-indicator autosave-saved"
            >Summary saved.</span
          >
          <span
            v-else-if="summarySaving"
            class="autosave-indicator autosave-saving"
            >Saving summary...</span
          >
        </div>
      </section>
      <div class="editor-actions">
        <RouterLink class="button secondary" to="/notes">Cancel</RouterLink
        ><button class="button save-button" :disabled="saving">
          <span>{{ saving ? "Saving…" : "Save note" }}</span>
          <kbd v-if="id">{{ shortcutModifier }} S</kbd>
        </button>
      </div>
    </form>
  </main>
</template>
