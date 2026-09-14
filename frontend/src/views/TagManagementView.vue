<script setup lang="ts">
import { onMounted, ref } from "vue";
import { createTag, deleteTag, listTags, updateTag, type Tag } from "../api";
import { TAG_PALETTE, tagColor } from "../tagPalette";
const palette = TAG_PALETTE;
const tags = ref<Tag[]>([]);
const name = ref("");
const selectedColor = ref(palette[0]);
const loading = ref(true);
const error = ref("");
async function load() {
  try {
    tags.value = await listTags();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to load tags";
  } finally {
    loading.value = false;
  }
}
async function add() {
  if (!name.value.trim()) return;
  try {
    tags.value.push(
      await createTag({
        name: name.value.trim(),
        color: selectedColor.value.name,
      }),
    );
    name.value = "";
    selectedColor.value = palette[0];
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to create tag";
  }
}
async function rename(tag: Tag) {
  const next = window.prompt("Tag name", tag.name)?.trim();
  if (!next || next === tag.name) return;
  try {
    Object.assign(
      tag,
      await updateTag(tag.id, { name: next, color: tag.color || undefined }),
    );
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to update tag";
  }
}
async function remove(tag: Tag) {
  if (!window.confirm(`Delete #${tag.name}?`)) return;
  try {
    await deleteTag(tag.id);
    tags.value = tags.value.filter((item) => item.id !== tag.id);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Unable to delete tag";
  }
}
function colorValue(name?: string | null) {
  return tagColor(name);
}
onMounted(load);
</script>
<template>
  <main class="tag-page">
    <header class="notes-header">
      <div>
        <RouterLink class="back-link" to="/notes">← Back to notes</RouterLink
        ><span class="eyebrow">ORGANIZE</span>
        <h1>Tag management</h1>
        <p>Create and maintain your personal note labels.</p>
      </div>
    </header>
    <p v-if="loading" class="state">Loading tags…</p>
    <div v-else class="tag-manager">
      <p v-if="error" class="error">{{ error }}</p>
      <form class="tag-create" @submit.prevent="add">
        <input v-model="name" required maxlength="80" placeholder="Tag name" />
        <fieldset class="color-picker">
          <legend>Choose a color</legend>
          <button
            v-for="color in palette"
            :key="color.name"
            type="button"
            class="color-swatch"
            :class="{ selected: selectedColor.name === color.name }"
            :style="{ backgroundColor: color.value }"
            :aria-label="color.name"
            :title="color.name"
            @click="selectedColor = color"
          >
            <span v-if="selectedColor.name === color.name">✓</span></button
          ><small>Selected: {{ selectedColor.name }}</small>
        </fieldset>
        <button class="button">Create tag</button>
      </form>
      <div v-if="!tags.length" class="empty state">
        No tags yet. Create one above.
      </div>
      <ul v-else class="tag-list">
        <li v-for="tag in tags" :key="tag.id">
          <span
            class="tag-chip management-tag-chip"
            :style="{ backgroundColor: colorValue(tag.color), color: '#fff' }"
            ># {{ tag.name }}</span
          ><span
            ><button @click="rename(tag)">Edit</button
            ><button class="danger-link" @click="remove(tag)">
              Delete
            </button></span
          >
        </li>
      </ul>
    </div>
  </main>
</template>
