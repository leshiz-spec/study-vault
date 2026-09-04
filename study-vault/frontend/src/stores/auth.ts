import { defineStore } from 'pinia'
import { apiRequest } from '../api'

export type User = { id: number; username: string; email: string; createdAt?: string; updatedAt?: string }

export const useAuthStore = defineStore('auth', {
  state: () => ({ user: null as User | null, loading: false, initialized: false, error: '' }),
  getters: { isAuthenticated: (state) => state.user !== null },
  actions: {
    async restore() { if (this.initialized) return; this.loading = true; this.error = ''; try { this.user = await apiRequest<User>('/auth/me') } catch { this.user = null } finally { this.initialized = true; this.loading = false } },
    async login(usernameOrEmail: string, password: string) { this.loading = true; this.error = ''; try { this.user = await apiRequest<User>('/auth/login', { method: 'POST', body: JSON.stringify({ usernameOrEmail, password }) }); return true } catch (error) { this.error = error instanceof Error ? error.message : 'Unable to sign in'; this.user = null; return false } finally { this.initialized = true; this.loading = false } },
    async register(username: string, email: string, password: string) { this.loading = true; this.error = ''; try { this.user = await apiRequest<User>('/auth/register', { method: 'POST', body: JSON.stringify({ username, email, password }) }); return true } catch (error) { const apiError = error as { code?: string; message?: string }; this.error = apiError.code === 'USERNAME_ALREADY_EXISTS' ? 'Username is already registered' : apiError.code === 'EMAIL_ALREADY_EXISTS' ? 'Email is already registered' : (apiError.code ? `${apiError.code}: ${apiError.message || 'Registration failed'}` : (error instanceof Error ? error.message : 'Unable to register')); this.user = null; return false } finally { this.initialized = true; this.loading = false } },
    async logout() { this.loading = true; this.error = ''; try { await apiRequest<null>('/auth/logout', { method: 'POST' }) } catch (error) { this.error = error instanceof Error ? error.message : 'Unable to sign out' } finally { this.user = null; this.initialized = true; this.loading = false } }
  }
})
