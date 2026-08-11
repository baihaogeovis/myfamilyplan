import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    },
    // 误把 API 地址当页面访问(GET /api/approval 根路径)时, 重定向到前端审批页, 避免直接暴露 403/404
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.method === 'GET' && req.url === '/api/approval') {
          res.statusCode = 302
          res.setHeader('Location', '/approval')
          res.end()
          return
        }
        next()
      })
    }
  },
  allowedHosts: true
})
