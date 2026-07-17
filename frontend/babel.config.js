module.exports = {
  // 逐词注释：
  // module.exports 是 Node.js 导出配置对象的写法。
  // presets 是 Babel 预设列表；@vue/cli-plugin-babel/preset 是 Vue CLI 默认转译规则。
  // 使用 Vue CLI 默认 Babel 预设，按 browserslist 自动决定需要转译的语法。
  presets: [
    '@vue/cli-plugin-babel/preset',
  ],
}
