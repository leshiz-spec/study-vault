import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // APP_PORT is commonly set to 8080 for local Docker Compose to avoid a
  // conflict on port 80. Override this with VITE_API_PROXY_TARGET when using
  // a different Nginx or backend host/port.
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    server: { port: 5173, proxy: { '/api': apiProxyTarget } }
  }
})
