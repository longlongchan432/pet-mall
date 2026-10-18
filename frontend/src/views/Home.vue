<template>
  <div class="home">
    <el-header class="header">
      <div class="container">
        <div class="header-content">
          <div class="logo" @click="$router.push('/')">
            <h1>宠旅驿站</h1>
          </div>
          <div class="nav-menu">
            <el-menu
              :default-active="activeMenu"
              mode="horizontal"
              class="nav-menu-list"
              background-color="transparent"
              text-color="#fff"
              active-text-color="#ffd04b"
              @select="handleMenuSelect"
            >
              <el-menu-item index="home">
                <i class="el-icon-house"></i>
                <span>首页</span>
              </el-menu-item>
              <el-menu-item index="categories">
                <i class="el-icon-menu"></i>
                <span>商品分类</span>
              </el-menu-item>
            </el-menu>
          </div>
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品..."
              class="search-input"
              @keyup.enter="handleSearch"
            >
              <i slot="prefix" class="el-input__icon el-icon-search"></i>
              <el-button slot="append" type="primary" @click="handleSearch" plain>
                <i class="el-icon-search"></i>
              </el-button>
            </el-input>
          </div>
          <div class="header-right">
            <el-button v-if="!userInfo" @click="$router.push('/login')">登录</el-button>
            <el-button v-if="!userInfo" @click="$router.push('/register')">注册</el-button>
            <el-badge v-if="userInfo" :value="cartCount" class="cart-badge">
              <el-button icon="el-icon-shopping-cart-full" @click="$router.push('/cart')">购物车</el-button>
            </el-badge>
            <el-dropdown v-if="userInfo" @command="handleCommand">
              <span class="user-info">
                <i class="el-icon-user"></i>
                {{ userInfo.username }}
                <i class="el-icon-arrow-down"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="user">个人中心</el-dropdown-item>
                <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </div>
      </div>
    </el-header>
    <router-view/>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import api from '../api'

export default {
  name: 'Home',
  data() {
    return {
      cartCount: 0,
      activeMenu: 'home',
      searchKeyword: ''
    }
  },
  computed: {
    ...mapState({
      userInfo: state => state.user.info
    })
  },
  mounted() {
    if (this.userInfo) {
      this.loadCartCount()
    }
    this.updateActiveMenu()
  },
  watch: {
    '$route'(to) {
      this.updateActiveMenu()
    }
  },
  methods: {
    updateActiveMenu() {
      const path = this.$route.path
      if (path === '/' || path === '') {
        this.activeMenu = 'home'
      } else if (path.includes('product') || path.includes('search')) {
        this.activeMenu = 'categories'
      } else {
        this.activeMenu = ''
      }
    },
    handleMenuSelect(index) {
      if (index === 'home') {
        this.$router.push('/').then(() => {
          this.$nextTick(() => {
            window.scrollTo({ top: 0, behavior: 'smooth' })
          })
        })
      } else if (index === 'categories') {
        this.$router.push('/').then(() => {
          this.$nextTick(() => {
            const categorySection = document.getElementById('category-section')
            if (categorySection) {
              categorySection.scrollIntoView({ behavior: 'smooth', block: 'start' })
            }
          })
        })
      }
    },
    handleCommand(command) {
      if (command === 'logout') {
        this.$store.dispatch('logout')
        if (this.$route.path !== '/') {
          this.$router.push('/').catch(err => {
            if (err.name !== 'NavigationDuplicated') {
              console.error(err)
            }
          })
        }
      } else if (command === 'user') {
        if (this.$route.path !== '/user') {
          this.$router.push('/user').catch(err => {
            if (err.name !== 'NavigationDuplicated') {
              console.error(err)
            }
          })
        }
      } else if (command === 'orders') {
        if (this.$route.path !== '/order-list') {
          this.$router.push('/order-list').catch(err => {
            if (err.name !== 'NavigationDuplicated') {
              console.error(err)
            }
          })
        }
      }
    },
    async loadCartCount() {
      try {
        const res = await api.get('/cart/list')
        if (res.code === 200) {
          this.cartCount = res.data.length
        }
      } catch (e) {
        console.error(e)
      }
    },
    handleSearch() {
      if (this.searchKeyword.trim()) {
        this.$router.push({
          name: 'Search',
          query: { keyword: this.searchKeyword.trim() }
        })
      }
    }
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 12px rgba(0,0,0,.15);
  line-height: 70px;
  position: sticky;
  top: 0;
  z-index: 1000;
  color: #fff;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-box {
  flex: 1;
  max-width: 400px;
  margin: 0 40px;
}

.search-input {
  width: 100%;
}

.search-input .el-input__inner {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  border: none;
  height: 36px;
}

.search-input .el-input__inner::placeholder {
  color: #909399;
}

.search-input .el-button {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
  height: 36px;
  padding: 0 15px;
}

.search-input .el-button:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.5);
}

.logo {
  cursor: pointer;
}

.logo h1 {
  color: #fff;
  font-size: 26px;
  margin: 0;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0,0,0,.2);
}

.nav-menu {
  flex: 1;
  display: flex;
  justify-content: center;
  margin: 0 40px;
}

.nav-menu-list {
  border: none;
}

.nav-menu-list .el-menu-item {
  border-bottom: none;
  height: 70px;
  line-height: 70px;
  font-size: 16px;
  padding: 0 30px;
}

.nav-menu-list .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

.nav-menu-list .el-menu-item.is-active {
  border-bottom: 3px solid #ffd04b;
  background-color: rgba(255, 255, 255, 0.1) !important;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-right .el-button {
  background: rgba(255,255,255,.2);
  border-color: rgba(255,255,255,.3);
  color: #fff;
  border-radius: 20px;
  padding: 8px 20px;
}

.header-right .el-button:hover {
  background: rgba(255,255,255,.3);
  border-color: rgba(255,255,255,.5);
}

.user-info {
  color: #fff;
  font-size: 14px;
}

.cart-badge {
  margin-right: 10px;
}

.cart-badge ::v-deep .el-badge__content {
  top: 8px;
}

.user-info {
  cursor: pointer;
  padding: 0 10px;
}
</style>

