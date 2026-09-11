import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark'

const STORAGE_KEY = 'studyvault:theme'

function systemTheme(): ThemeMode {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function readStoredTheme(): ThemeMode | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored === 'dark' || stored === 'light' ? stored : null
  } catch {
    return null
  }
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    mode: 'light' as ThemeMode,
    initialized: false,
  }),
  actions: {
    initialize() {
      if (this.initialized) return
      const initial = readStoredTheme() || document.documentElement.dataset.theme as ThemeMode || systemTheme()
      this.setMode(initial, false)
      this.initialized = true
    },
    setMode(mode: ThemeMode, persist = true) {
      this.mode = mode
      document.documentElement.dataset.theme = mode
      document.documentElement.style.colorScheme = mode
      if (persist) {
        try {
          localStorage.setItem(STORAGE_KEY, mode)
        } catch {
          // The theme still applies for this session when storage is unavailable.
        }
      }
    },
    toggle() {
      this.setMode(this.mode === 'dark' ? 'light' : 'dark')
    },
  },
})
