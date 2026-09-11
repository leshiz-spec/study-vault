<script setup lang="ts">
import { ref } from 'vue'; import { useRoute, useRouter } from 'vue-router'; import { useAuthStore } from '../stores/auth'
const auth = useAuthStore(); const router = useRouter(); const route = useRoute(); const usernameOrEmail = ref(''); const password = ref(''); const submitting = ref(false)
async function submit() { submitting.value = true; const ok = await auth.login(usernameOrEmail.value, password.value); submitting.value = false; if (ok) router.push(typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard') }
</script>
<template><main class="auth-page"><form class="auth-card" @submit.prevent="submit"><h1>StudyVault</h1><h2>Sign in</h2><p v-if="auth.error" class="error">{{ auth.error }}</p><label>Username or email<input v-model="usernameOrEmail" required autocomplete="username" /></label><label>Password<input v-model="password" required type="password" autocomplete="current-password" /></label><button :disabled="submitting">{{ submitting ? 'Signing in…' : 'Sign in' }}</button><p>Need an account? <RouterLink to="/register">Create one</RouterLink></p></form></main></template>
