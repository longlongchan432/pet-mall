module.exports = {
  devServer: {
    port: 8081,
    proxy: {
      // ponytail: /ai 与 /api 都必须排在 /upload 之前——本版 http-proxy-middleware 按路径包含匹配，
      // /ai/api/... 含 /api、/api/file/upload 含 /upload，顺序反了就会被后一条抢走（404）
      '/ai': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        pathRewrite: { '^/ai': '' }
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 图片已迁到 Ubuntu 文件服务器，由那边 nginx 直出；换域名/CDN 只改这一处
      '/upload': {
        target: process.env.UPLOAD_BASE || 'http://localhost',
        changeOrigin: true
      }
    }
  }
}



