<template>
<div id="app">
  <h1>Hello App!</h1>
  <p>
    <!-- 使用 router-link 组件来导航. -->
    <!-- 通过传入 `to` 属性指定链接. -->
    <!-- <router-link> 默认会被渲染成一个 `<a>` 标签 -->
    <h1><router-link to="/apriori">关联规则</router-link></h1>
    <h1><router-link to="/kmeans">聚类分析</router-link></h1>
    <h1><router-link to="/dt">分类分析</router-link></h1>
    <h1><router-link to="/lr">回归分析</router-link></h1>
  </p>
  <!-- 路由出口 -->
  <!-- 路由匹配到的组件将渲染在这里 -->
  <router-view></router-view>
</div>
</template>

<script>
export default {
  name: 'sideView'
}

</script>