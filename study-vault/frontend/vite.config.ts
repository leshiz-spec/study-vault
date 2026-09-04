import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // Docker exposes the API through Nginx on port 80; backend:8080 is
  // internal to the Compose network and is not reachable from the host.
  server: { port: 5173, proxy: { '/api': 'http://localhost' } }
})
