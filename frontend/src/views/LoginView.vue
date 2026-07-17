<!--
  文件作用：登录/注册页面。
  项目位置：前端 views 层，是用户进入系统前看到的第一个页面。
  交互关系：调用 api/request.js 中的登录注册接口；成功登录后通过 auth.js 保存状态，并跳转到实验页面。

  逐词注释：
  v-model 表示双向绑定输入值；v-model.trim 会自动去掉首尾空格。
  :options 把 JS 数组传给分段控件；:loading 控制按钮加载态；@click 绑定点击事件。
  ref 创建响应式变量；async/await 用同步写法表达异步请求；try/catch/finally 分别处理成功、异常和收尾。
-->
<template>
  <main class="login-page">
    <el-card class="login-card" shadow="always">
      <p class="eyebrow">KDD System</p>
      <h1>数据挖掘与分析系统</h1>

      <!-- mode 保存当前模式；modeOptions 提供“登录/注册”两个选项。 -->
      <el-segmented v-model="mode" :options="modeOptions" class="login-switch" />

      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model.trim="username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="submit"
          />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="message"
        :title="message"
        :type="messageType"
        show-icon
        :closable="false"
        class="login-alert"
      />

      <!-- loading 为 true 时按钮显示加载状态，并避免用户误以为没有响应。 -->
      <el-button type="primary" class="login-button" :loading="loading" @click="submit">
        {{ mode === 'login' ? '登录系统' : '注册账号' }}
      </el-button>
    </el-card>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginUser, registerUser } from '../api/request'
import { setLoggedIn } from '../auth'

const router = useRouter() // 获取路由实例，用于登录成功后跳转页面。
const mode = ref('login') // 当前表单模式：login 表示登录，register 表示注册。
const modeOptions = [
  { label: '登录', value: 'login' },
  { label: '注册', value: 'register' },
]
const username = ref('') // 账号输入框内容。
const password = ref('') // 密码输入框内容。
const message = ref('') // 提示消息文本，空字符串时不显示 el-alert。
const messageType = ref('error') // 提示类型：error 或 success。
const loading = ref(false) // 请求进行中标记，控制按钮加载状态。

// 登录和注册共用同一个提交按钮，根据 mode 决定调用哪个接口。
async function submit() {
  // 每次提交前先清空旧消息，避免上一轮提示干扰当前操作。
  message.value = ''
  messageType.value = 'error'
  if (!username.value || !password.value) {
    message.value = '请输入账号和密码'
    return
  }

  loading.value = true
  try {
    // mode.value 是 ref 的真实值；模板里可以省略 .value，脚本里不能省。
    if (mode.value === 'register') {
      await register()
    } else {
      await login()
    }
  } catch (error) {
    message.value = error.response?.data?.message || '操作失败，请确认后端和 MySQL 已启动'
  } finally {
    loading.value = false
  }
}

// 注册成功后不直接进入系统，而是切回登录模式，让用户重新登录。
async function register() {
  await registerUser(username.value, password.value)
  mode.value = 'login'
  password.value = ''
  messageType.value = 'success'
  message.value = '注册成功，请登录'
}

// 登录成功后保存本地登录态，并跳转到默认实验页面。
async function login() {
  const response = await loginUser(username.value, password.value)
  setLoggedIn(response.data.user)
  router.push('/association')
}
</script>
