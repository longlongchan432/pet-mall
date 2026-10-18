<template>
  <el-container class="layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">宠旅驿站</div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <i class="el-icon-s-home"></i>
          <span>首页</span>
        </el-menu-item>
        <!-- 商家菜单 -->
        <template v-if="userRole === 1">
          <el-menu-item index="/merchant/dashboard">
            <i class="el-icon-data-line"></i>
            <span>数据看板</span>
          </el-menu-item>
          <el-submenu index="product">
            <template slot="title">
              <i class="el-icon-goods"></i>
              <span>商品管理</span>
            </template>
            <el-menu-item index="/merchant/products">商品列表</el-menu-item>
            <el-menu-item index="/merchant/product/add">添加商品</el-menu-item>
          </el-submenu>
          <el-menu-item index="/merchant/orders">
            <i class="el-icon-s-order"></i>
            <span>订单管理</span>
          </el-menu-item>
        </template>
        <!-- 管理员菜单 -->
        <template v-if="userRole === 2">
          <el-menu-item index="/admin/users">
            <i class="el-icon-user"></i>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/merchants">
            <i class="el-icon-s-shop"></i>
            <span>商家管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/products">
            <i class="el-icon-goods"></i>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/orders">
            <i class="el-icon-s-order"></i>
            <span>订单管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/categories">
            <i class="el-icon-menu"></i>
            <span>分类管理</span>
          </el-menu-item>
          <!-- 新增：充值审核菜单（关键！） -->
          <el-menu-item index="/admin/recharge-audit" v-if="userRole === 2">
            <i class="el-icon-money"></i> <!-- 金额图标，适配风格 -->
            <span slot="title">充值审核</span>
          </el-menu-item>
          <el-menu-item index="/admin/review-audit" v-if="userRole === 2">
            <i class="el-icon-chat-dot-round"></i>
            <span slot="title">评价审核</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-right">
          <span>欢迎，{{ userInfo?.username }}</span>
          <el-button type="text" @click="showPasswordDialog = true">修改密码</el-button>
          <el-button type="text" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      
      <!-- 修改密码对话框 -->
      <el-dialog
        title="修改密码"
        :visible.sync="showPasswordDialog"
        width="500px"
        :close-on-click-modal="false"
      >
        <el-form
          :model="passwordForm"
          :rules="passwordRules"
          ref="passwordForm"
          label-width="100px"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              type="password"
              v-model="passwordForm.oldPassword"
              placeholder="请输入原密码"
              show-password
            ></el-input>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              type="password"
              v-model="passwordForm.newPassword"
              placeholder="请输入新密码"
              show-password
            ></el-input>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              type="password"
              v-model="passwordForm.confirmPassword"
              placeholder="请再次输入新密码"
              show-password
            ></el-input>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button @click="showPasswordDialog = false">取消</el-button>
          <el-button type="primary" @click="handleUpdatePassword" :loading="passwordLoading">确定</el-button>
        </div>
      </el-dialog>
      <el-main class="main-content">
        <router-view/>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { mapState } from 'vuex'
import api from '../api'

export default {
  name: 'Layout',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      showPasswordDialog: false,
      passwordLoading: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapState({
      userInfo: state => state.user.info
    }),
    userRole() {
      return this.userInfo?.role || null
    },
    activeMenu() {
      return this.$route.path
    }
  },
  mounted() {
    // 如果用户信息不存在，尝试获取
    if (!this.userInfo && this.$store.state.user.token) {
      this.$store.dispatch('getUserInfo')
    }
  },
  methods: {
    handleLogout() {
      this.$store.dispatch('logout')
      this.$router.push('/login')
    },
    handleUpdatePassword() {
      this.$refs.passwordForm.validate(async (valid) => {
        if (!valid) {
          return false
        }
        this.passwordLoading = true
        try {
          const res = await api.put('/user/password', {
            oldPassword: this.passwordForm.oldPassword,
            newPassword: this.passwordForm.newPassword
          })
          if (res.code === 200) {
            this.$message.success('密码修改成功')
            this.showPasswordDialog = false
            this.passwordForm = {
              oldPassword: '',
              newPassword: '',
              confirmPassword: ''
            }
            this.$refs.passwordForm.resetFields()
          }
        } catch (e) {
          // 错误信息已在拦截器中显示
        } finally {
          this.passwordLoading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  background-color: #2b3a4a;
}

.header {
  background: #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,.1);
  line-height: 60px;
  margin-left: 200px;
}

.header-right {
  float: right;
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-right .el-button {
  padding: 0;
}

.main-content {
  margin-left: 200px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}
</style>

