// 文件作用：Babel 转译配置，让 Vue CLI 按浏览器兼容目标处理较新的 JavaScript 语法。
// 关联文件：package.json 中的 browserslist 决定这里的默认预设需要兼容哪些浏览器。
module.exports = {
  // 逐词注释：
  // module.exports 是 Node.js 导出配置对象的写法。
  // presets 是 Babel 预设列表；@vue/cli-plugin-babel/preset 是 Vue CLI 默认转译规则。
  // 使用 Vue CLI 默认 Babel 预设，按 browserslist 自动决定需要转译的语法。
  presets: [
    '@vue/cli-plugin-babel/preset',
  ],
}
