import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

function noStoreDevCache() {
  return {
    name: 'no-store-dev-cache',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        delete req.headers['if-none-match']
        delete req.headers['if-modified-since']
        res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
        res.setHeader('Pragma', 'no-cache')
        res.setHeader('Expires', '0')
        next()
      })
    }
  }
}

export default defineConfig({
  plugins: [vue(), noStoreDevCache()],
  server: {
    headers: {
      'Cache-Control': 'no-store, no-cache, must-revalidate, max-age=0',
      'Pragma': 'no-cache',
      'Expires': '0'
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              proxyRes.headers['Cache-Control'] = 'no-cache'
              proxyRes.headers['X-Accel-Buffering'] = 'no'
            }
          })
        }
      }
    }
  }
})
