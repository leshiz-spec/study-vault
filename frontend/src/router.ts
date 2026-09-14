import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "./stores/auth";
import LoginView from "./views/LoginView.vue";
import RegisterView from "./views/RegisterView.vue";
import DashboardView from "./views/DashboardView.vue";
import NotesView from "./views/NotesView.vue";
import NoteEditorView from "./views/NoteEditorView.vue";
import TagManagementView from "./views/TagManagementView.vue";
import TrashView from "./views/TrashView.vue";
import StudyTasksView from "./views/StudyTasksView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/dashboard" },
    { path: "/login", component: LoginView, meta: { guestOnly: true } },
    { path: "/register", component: RegisterView, meta: { guestOnly: true } },
    {
      path: "/dashboard",
      component: DashboardView,
      meta: { requiresAuth: true },
    },
    { path: "/notes", component: NotesView, meta: { requiresAuth: true } },
    {
      path: "/notes/new",
      component: NoteEditorView,
      meta: { requiresAuth: true },
    },
    {
      path: "/notes/:id",
      component: NoteEditorView,
      meta: { requiresAuth: true },
    },
    {
      path: "/tags",
      component: TagManagementView,
      meta: { requiresAuth: true },
    },
    { path: "/trash", component: TrashView, meta: { requiresAuth: true } },
    { path: "/tasks", component: StudyTasksView, meta: { requiresAuth: true } },
  ],
});
router.beforeEach(async (to) => {
  const auth = useAuthStore();
  await auth.restore();
  if (to.meta.requiresAuth && !auth.isAuthenticated)
    return { path: "/login", query: { redirect: to.fullPath } };
  if (to.meta.guestOnly && auth.isAuthenticated) return "/dashboard";
});
export default router;
