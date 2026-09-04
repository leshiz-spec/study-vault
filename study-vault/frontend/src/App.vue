<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
const auth = useAuthStore(); const router = useRouter()
async function logout() { await auth.logout(); router.push('/login') }
</script>
<template>
  <div class="site-shell">
    <header class="site-header">
      <RouterLink class="site-brand" to="/dashboard"><span class="brand-mark">S</span><span><strong>StudyVault</strong><small>Personal knowledge base</small></span></RouterLink>
      <nav v-if="auth.isAuthenticated" class="site-nav"><RouterLink to="/dashboard">Dashboard</RouterLink><RouterLink to="/notes">Notes</RouterLink><RouterLink to="/trash">Trash</RouterLink><span class="site-user">{{ auth.user?.username }}</span><button class="header-logout" @click="logout" :disabled="auth.loading">{{ auth.loading ? '…' : 'Log out' }}</button></nav>
    </header>
    <RouterView />
    <footer class="site-footer"><span>StudyVault</span><span>Keep learning, one note at a time.</span></footer>
  </div>
</template>
