<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useThemeStore } from './stores/theme'
import { hasShortcutModifier, isEditableTarget } from './keyboard'
const auth = useAuthStore(); const theme = useThemeStore(); const router = useRouter()
theme.initialize()
async function logout() { await auth.logout(); router.push('/login') }
function focusSearch() {
  if (router.currentRoute.value.path === '/notes') {
    window.dispatchEvent(new Event('studyvault:focus-search'))
    return
  }
  router.push('/notes').then(() => {
    window.setTimeout(() => window.dispatchEvent(new Event('studyvault:focus-search')), 0)
  })
}
function handleGlobalShortcut(event: KeyboardEvent) {
  if (!auth.isAuthenticated || !hasShortcutModifier(event)) return
  if (isEditableTarget(event.target)) return
  const key = event.key.toLowerCase()
  if (key === 'k') {
    event.preventDefault()
    focusSearch()
  } else if (key === 'n') {
    event.preventDefault()
    router.push('/notes/new')
  }
}
onMounted(() => window.addEventListener('keydown', handleGlobalShortcut))
onUnmounted(() => window.removeEventListener('keydown', handleGlobalShortcut))
</script>
<template>
  <div class="site-shell">
    <header class="site-header">
      <RouterLink class="site-brand" to="/dashboard"><span class="brand-mark">S</span><span><strong>StudyVault</strong><small>Personal knowledge base</small></span></RouterLink>
      <nav v-if="auth.isAuthenticated" class="site-nav" aria-label="Primary navigation">
        <RouterLink to="/dashboard">Dashboard</RouterLink>
        <RouterLink to="/notes">Notes</RouterLink>
        <RouterLink to="/tasks">Tasks</RouterLink>
        <RouterLink to="/trash">Trash</RouterLink>
      </nav>
      <div class="site-header-actions">
        <div v-if="auth.isAuthenticated" class="account-menu">
          <span class="user-avatar" aria-hidden="true">{{ auth.user?.username?.charAt(0).toUpperCase() }}</span>
          <span class="site-user">{{ auth.user?.username }}</span>
          <button class="header-logout" @click="logout" :disabled="auth.loading">{{ auth.loading ? '…' : 'Log out' }}</button>
        </div>
        <button class="theme-toggle" type="button" :aria-pressed="theme.mode === 'dark'" :aria-label="theme.mode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'" :title="theme.mode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'" @click="theme.toggle()">
          <span aria-hidden="true">{{ theme.mode === 'dark' ? '☀' : '☾' }}</span>
          <span class="theme-label">{{ theme.mode === 'dark' ? 'Light' : 'Dark' }}</span>
        </button>
      </div>
    </header>
    <RouterView />
    <footer class="site-footer"><span>StudyVault</span><span>Keep learning, one note at a time.</span></footer>
  </div>
</template>
