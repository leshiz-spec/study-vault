const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
export type ApiError = Error & { code?: string };
export type Note = {
  id: number;
  title: string;
  content: string;
  summary?: string | null;
  status: string;
  favorite: boolean;
  reviewStatus: string;
  createdAt?: string;
  updatedAt?: string;
  tags?: Tag[];
};
export type NoteInput = { title: string; content: string };
export type NoteSearchResult = {
  content: Note[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
export type Tag = {
  id: number;
  name: string;
  color?: string | null;
  createdAt?: string;
};
export type NoteRevision = {
  id: number;
  noteId: number;
  title: string;
  content: string;
  createdAt?: string;
};
export type ReviewStatus = "not_started" | "learning" | "review" | "mastered";
export type StudyTaskStatus = "todo" | "in_progress" | "done";
export type StudyTask = {
  id: number;
  title: string;
  dueDate?: string | null;
  status: StudyTaskStatus;
  noteId?: number | null;
  noteTitle?: string | null;
  noteIds: number[];
  noteTitles: string[];
  createdAt?: string;
  updatedAt?: string;
};

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type"))
    headers.set("Content-Type", "application/json");
  const response = await fetch(`${API_BASE_URL}/api${path}`, {
    ...options,
    headers,
    credentials: "include",
    cache: "no-store",
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(
      payload?.error?.message || "Request failed",
    ) as ApiError;
    error.code = payload?.error?.code;
    throw error;
  }
  return payload.data as T;
}

export const listNotes = async () =>
  (await apiRequest<Note[]>("/notes")).filter(
    (note) => note.status === "active",
  );
export const searchNotes = (params: {
  q?: string;
  tag?: string;
  favorite?: boolean;
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
}) => {
  const query = new URLSearchParams();
  if (params.q?.trim()) query.set("q", params.q.trim());
  if (params.tag?.trim()) query.set("tag", params.tag.trim());
  if (params.favorite !== undefined)
    query.set("favorite", String(params.favorite));
  if (params.status?.trim()) query.set("status", params.status.trim());
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 10));
  query.set("sort", params.sort ?? "updatedAt,desc");
  return apiRequest<NoteSearchResult>(`/search?${query.toString()}`);
};
export const getNote = (id: number) => apiRequest<Note>(`/notes/${id}`);
export const createNote = (input: NoteInput) =>
  apiRequest<Note>("/notes", { method: "POST", body: JSON.stringify(input) });
export const updateNote = (id: number, input: NoteInput) =>
  apiRequest<Note>(`/notes/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
export const updateNoteReviewStatus = (
  id: number,
  reviewStatus: ReviewStatus,
) =>
  apiRequest<Note>(`/notes/${id}/review-status`, {
    method: "PUT",
    body: JSON.stringify({ reviewStatus }),
  });
export const listNoteRevisions = (id: number) =>
  apiRequest<NoteRevision[]>(`/notes/${id}/revisions`);
export const getNoteRevision = (id: number, revisionId: number) =>
  apiRequest<NoteRevision>(`/notes/${id}/revisions/${revisionId}`);
export const restoreNoteRevision = (id: number, revisionId: number) =>
  apiRequest<Note>(`/notes/${id}/revisions/${revisionId}/restore`, {
    method: "POST",
  });
export const summarizeNote = (id: number) =>
  apiRequest<{ noteId: number; summary: string }>(`/notes/${id}/summarize`, {
    method: "POST",
  });
export const saveNoteSummary = (id: number, summary: string) =>
  apiRequest<Note>(`/notes/${id}/summary`, {
    method: "PUT",
    body: JSON.stringify({ summary }),
  });
export const deleteNote = (id: number) =>
  apiRequest<null>(`/notes/${id}`, { method: "DELETE" });
export const permanentlyDeleteNote = (id: number) =>
  apiRequest<null>(`/notes/${id}/permanent`, { method: "DELETE" });
export const listTrash = () => apiRequest<Note[]>("/notes/trash");
export const toggleFavorite = (id: number) =>
  apiRequest<Note>(`/notes/${id}/favorite`, { method: "POST" });
export const restoreNote = (id: number) =>
  apiRequest<Note>(`/notes/${id}/restore`, { method: "POST" });
export const listTags = () => apiRequest<Tag[]>("/tags");
export const createTag = (input: { name: string; color?: string }) =>
  apiRequest<Tag>("/tags", { method: "POST", body: JSON.stringify(input) });
export const updateTag = (
  id: number,
  input: { name: string; color?: string },
) =>
  apiRequest<Tag>(`/tags/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
export const deleteTag = (id: number) =>
  apiRequest<null>(`/tags/${id}`, { method: "DELETE" });
export const addTagToNote = (noteId: number, tagId: number) =>
  apiRequest<Note>(`/notes/${noteId}/tags`, {
    method: "POST",
    body: JSON.stringify({ tagId }),
  });
export const removeTagFromNote = (noteId: number, tagId: number) =>
  apiRequest<null>(`/notes/${noteId}/tags/${tagId}`, { method: "DELETE" });
export type StudyTaskFilters = {
  status?: StudyTaskStatus;
  dueDate?: string;
  dueAfter?: string;
  dueBefore?: string;
};
export const listTasks = (filters: StudyTaskFilters = {}) => {
  const query = new URLSearchParams();
  if (filters.status) query.set("status", filters.status);
  if (filters.dueDate) query.set("dueDate", filters.dueDate);
  if (filters.dueAfter) query.set("dueAfter", filters.dueAfter);
  if (filters.dueBefore) query.set("dueBefore", filters.dueBefore);
  const suffix = query.toString();
  return apiRequest<StudyTask[]>(`/tasks${suffix ? `?${suffix}` : ""}`);
};
export type StudyTaskInput = {
  title: string;
  dueDate?: string | null;
  status?: StudyTaskStatus;
  noteIds?: number[];
};
export const createTask = (input: StudyTaskInput) =>
  apiRequest<StudyTask>("/tasks", {
    method: "POST",
    body: JSON.stringify(input),
  });
export const updateTask = (id: number, input: StudyTaskInput) =>
  apiRequest<StudyTask>(`/tasks/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
export const deleteTask = (id: number) =>
  apiRequest<null>(`/tasks/${id}`, { method: "DELETE" });

export async function downloadFile(
  path: string,
  fallbackName: string,
): Promise<{ blob: Blob; filename: string }> {
  const response = await fetch(`${API_BASE_URL}/api${path}`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}));
    const error = new Error(
      payload?.error?.message || "Download failed",
    ) as ApiError;
    error.code = payload?.error?.code;
    throw error;
  }
  const disposition = response.headers.get("Content-Disposition") || "";
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  const plain =
    disposition.match(/filename="([^"]+)"/i)?.[1] ||
    disposition.match(/filename=([^;]+)/i)?.[1]?.trim();
  let filename = plain || fallbackName;
  if (encoded) {
    try {
      filename = decodeURIComponent(encoded);
    } catch {
      // Fall back to the server's plain filename when a malformed header is received.
    }
  }
  return { blob: await response.blob(), filename };
}

export async function importMarkdown(file: File): Promise<Note> {
  const form = new FormData();
  form.append("file", file);
  const response = await fetch(`${API_BASE_URL}/api/notes/import`, {
    method: "POST",
    body: form,
    credentials: "include",
    cache: "no-store",
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(
      payload?.error?.message || "Import failed",
    ) as ApiError;
    error.code = payload?.error?.code;
    throw error;
  }
  return payload.data as Note;
}

export const exportNote = (id: number) =>
  downloadFile(`/notes/${id}/export`, `note-${id}.md`);
export const exportAllNotes = () =>
  downloadFile("/notes/export", "studyvault-notes.zip");
