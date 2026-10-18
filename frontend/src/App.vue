<template>
  <div id="app">
    <router-view/>
    <AiChat v-if="showAi"/>
  </div>
</template>

<script>
import AiChat from './components/AiChat.vue'

export default {
  name: 'App',
  components: { AiChat },
  computed: {
    // 登录/注册页不显示 AI 助手：未认证页面不应泄露历史对话
    showAi() {
      return !['Login', 'Register'].includes(this.$route.name)
    }
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style>
