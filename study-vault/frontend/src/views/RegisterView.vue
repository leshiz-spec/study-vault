<script setup lang="ts">
import { ref } from 'vue'; import { useRouter } from 'vue-router'; import { useAuthStore } from '../stores/auth'
const auth = useAuthStore(); const router = useRouter(); const username = ref(''); const email = ref(''); const password = ref(''); const submitting = ref(false)
async function submit() { submitting.value = true; const ok = await auth.register(username.value, email.value, password.value); submitting.value = false; if (ok) router.push('/dashboard') }
</script>
<template><main class="auth-page"><form class="auth-card" @submit.prevent="submit"><h1>StudyVault</h1><h2>Create account</h2><p v-if="auth.error" class="error">{{ auth.error }}</p><label>Username<input v-model="username" required minlength="3" autocomplete="username" /></label><label>Email<input v-model="email" required type="email" autocomplete="email" /></label><label>Password<input v-model="password" required minlength="8" type="password" autocomplete="new-password" /></label><button :disabled="submitting">{{ submitting ? 'Creating account…' : 'Register' }}</button><p>Already registered? <RouterLink to="/login">Sign in</RouterLink></p></form></main></template>
