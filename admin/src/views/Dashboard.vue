<template>
  <div class="dashboard">
    <h2>数据概览</h2>
    <el-row :gutter="20" v-loading="loading">
      <el-col :span="6" v-if="isAdmin">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon user-icon">
              <i class="el-icon-user"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="isAdmin ? 6 : 8">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon product-icon">
              <i class="el-icon-goods"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalProducts }}</div>
              <div class="stat-label">{{ isAdmin ? '总商品数' : '我的商品数' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="isAdmin ? 6 : 8">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon order-icon">
              <i class="el-icon-s-order"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalOrders }}</div>
              <div class="stat-label">{{ isAdmin ? '总订单数' : '我的订单数' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="isAdmin ? 6 : 8">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon sales-icon">
              <i class="el-icon-money"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatSales(stats.totalSales) }}</div>
              <div class="stat-label">{{ isAdmin ? '总销售额' : '我的销售额' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import api from '../api'
import { mapState } from 'vuex'

export default {
  name: 'Dashboard',
  data() {
    return {
      stats: {
        totalUsers: 0,
        totalProducts: 0,
        totalOrders: 0,
        totalSales: 0
      },
      loading: false
    }
  },
  computed: {
    ...mapState({
      userInfo: state => state.user.info
    }),
    isAdmin() {
      return this.userInfo?.role === 2
    },
    isMerchant() {
      return this.userInfo?.role === 1
    }
  },
  mounted() {
    this.loadStats()
  },
  methods: {
    async loadStats() {
      this.loading = true
      try {
        const res = await api.get('/stats/dashboard')
        if (res.code === 200) {
          this.stats = res.data
        }
      } catch (e) {
        this.$message.error('加载统计数据失败')
      } finally {
        this.loading = false
      }
    },
    formatSales(amount) {
      if (!amount) return '¥0.00'
      return '¥' + parseFloat(amount).toFixed(2)
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.dashboard h2 {
  margin-bottom: 20px;
  color: #333;
}

.stat-card {
  transition: all 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 28px;
  color: #fff;
}

.user-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.product-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.order-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.sales-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  line-height: 1;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}
</style>

