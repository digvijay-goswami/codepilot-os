import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // The more specific path must come first
      '/api/ai': 'http://127.0.0.1:8000',
      '/api': 'http://127.0.0.1:8080',
    },
  },
})