module.exports = {
  devServer: {
    // 8082 已被 ai-service 占用，后台改走 8083，避免抢端口
    port: 8083,
    proxy: {
      // ponytail: /api 必须排在 /upload 之前——本版 http-proxy-middleware 按路径包含匹配，
      // /api/file/upload 含 /upload，顺序反了上传接口会被转给 nginx 变成 404
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 商品图在 Ubuntu 文件服务器上，由那边 nginx 直出
      '/upload': {
        target: process.env.UPLOAD_BASE || 'http://localhost',
        changeOrigin: true
      }
    }
  }
}



