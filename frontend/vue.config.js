// 文件作用：Vue CLI 项目配置，主要控制开发服务器地址、端口、错误遮罩和 /api 代理。
// 关联文件：api/request.js 发出的 /api 请求会在开发环境经这里转发到后端服务。
const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  // 逐词注释：
  // defineConfig 用来给 Vue CLI 配置提供更友好的结构提示。
  // transpileDependencies 表示是否转译依赖包；devServer 表示开发服务器配置；proxy 表示接口代理。
  transpileDependencies: true, // 保持 true 可以兼容更多浏览器环境。
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
        target: 'http://127.0.0.1:5000', // 后端 Spring Boot/接口服务地址。
        changeOrigin: true, // 修改请求来源头，减少后端跨域校验干扰。
      },
    },
  },
})
