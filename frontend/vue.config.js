const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  // Vue CLI 是否转译依赖包；保持 true 可以兼容更多浏览器环境。
  transpileDependencies: true,
  devServer: {
    // 固定本地地址和端口，便于后端 CORS 白名单和文档说明保持一致。
    host: '127.0.0.1',
    port: 8080,
    client: {
      overlay: {
        // 运行时错误不弹全屏遮罩，避免 ResizeObserver 等提示干扰课堂演示。
        runtimeErrors: false,
      },
    },
    proxy: {
      // 开发环境把前端 /api 请求代理到 Spring Boot 后端。
      '/api': {
        target: 'http://127.0.0.1:5000',
        changeOrigin: true,
      },
    },
  },
})
