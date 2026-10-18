<template>
  <div class="login">
    <div class="login-container">
      <h2>后台管理系统</h2>
      <el-form :model="form" :rules="rules" ref="loginForm" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input type="password" v-model="form.password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" style="width: 100%">登录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false
    }
  },
  methods: {
    async handleLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (valid) {
          this.loading = true
          try {
            const success = await this.$store.dispatch('login', this.form)
            if (success) {
              const userInfo = this.$store.state.user.info
              if (userInfo && (userInfo.role === 1 || userInfo.role === 2)) {
                this.$message.success('登录成功')
                this.$router.push('/dashboard')
              } else {
                this.$message.error('无权限访问后台系统')
                this.$store.dispatch('logout')
              }
            } else {
              this.$message.error('用户名或密码错误')
            }
          } catch (e) {
            this.$message.error('登录失败')
          } finally {
            this.loading = false
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-container {
  background: #fff;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,.1);
  width: 400px;
}

.login-container h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #409EFF;
}
</style>

