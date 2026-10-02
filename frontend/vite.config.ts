import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Dev only: /api/films -> http://localhost:8080/films, so no CORS is needed.
      // The backend has no /api prefix; it exists so page URLs like /films/1 stay
      // with React on refresh instead of being sent to the backend.
      '/api': {
        target: 'http://localhost:8080',
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
