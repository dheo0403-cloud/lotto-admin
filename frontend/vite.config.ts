import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/lotto/',
  server: {
    proxy: {
      // 백엔드 context-path(/lotto)와 포트(8082)에 맞춤
      '/lotto/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      }
    }
  }
})
