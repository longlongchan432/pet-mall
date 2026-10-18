<template>
  <div class="product-detail">
    <el-container>
      <el-main>
        <div class="container">
          <div v-if="product" class="detail-content">
            <el-row :gutter="40">
              <el-col :span="12">
                <div class="product-image">
                  <img :src="getImageUrl(product.image)" :alt="product.name" @error="handleImageError">
                </div>
              </el-col>
              <el-col :span="12">
                <div class="product-info">
                  <h1>{{ product.name }}</h1>
                  <div class="merchant-info" v-if="product.merchantName">
                    <i class="el-icon-shop"></i>
                    <span>店铺：{{ product.merchantName }}</span>
                  </div>
                  <p class="price">¥{{ product.price }}</p>
                  <p class="stock">库存：{{ product.stock }}</p>
                  <p class="description">{{ product.description }}</p>
                  <div class="quantity">
                    <span>数量：</span>
                    <el-input-number v-model="quantity" :min="1" :max="product.stock"></el-input-number>
                  </div>
                  <div class="actions">
                    <el-button type="primary" size="large" @click="addToCart">加入购物车</el-button>
                    <el-button type="danger" size="large" @click="buyNow">立即购买</el-button>
                  </div>
                </div>
              </el-col>
            </el-row>
            <!-- 用户评价 -->
            <div class="reviews-section" v-if="product">
              <h3>用户评价（{{ reviews.length }}）</h3>
              <div v-if="reviews.length === 0" class="no-reviews">暂无评价，快来抢沙发~</div>
              <div v-else class="reviews-list">
                <div v-for="r in reviews" :key="r.id" class="review-card">
                  <div class="review-head">
                    <el-rate v-model="r.rating" disabled show-score text-color="#ff9900"></el-rate>
                    <span class="user-name">{{ r.userName || '用户' }}</span>
                    <span class="review-time">{{ formatTime(r.createTime) }}</span>
                  </div>
                  <p class="review-content">{{ r.content || '该用户未填写评价内容' }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'ProductDetail',
  data() {
    return {
      product: null,
      quantity: 1,
      reviews: []
    }
  },
  mounted() {
    this.loadProduct()
  },
  methods: {
    formatTime(t) {
      if (!t) return ''
      const d = new Date(t)
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0') + ' ' + String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
    },
    getImageUrl(imagePath) {
      if (!imagePath) {
        return '/images/default.jpg'
      }
      if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
        return imagePath
      }
      return imagePath // 相对路径：由 dev 代理 /upload 转发到文件服务器 nginx
    },
    handleImageError(e) {
      e.target.src = '/images/default.jpg'
    },
    async loadProduct() {
      try {
        const res = await api.get(`/product/public/${this.$route.params.id}`)
        if (res.code === 200) {
          this.product = res.data
          this.loadReviews()
        }
      } catch (e) {
        this.$message.error('加载商品失败')
      }
    },
    async loadReviews() {
      if (!this.product || !this.product.id) return
      try {
        const res = await api.get(`/review/product/${this.product.id}`)
        if (res.code === 200) this.reviews = res.data || []
      } catch (e) {
        this.reviews = []
      }
    },
    async addToCart() {
      if (!this.$store.state.user.token) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      try {
        const res = await api.post('/cart/add', {
          productId: this.product.id,
          quantity: this.quantity
        })
        if (res.code === 200) {
          this.$message.success('已加入购物车')
        }
      } catch (e) {
        this.$message.error('加入购物车失败')
      }
    },
    buyNow() {
      if (!this.$store.state.user.token) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      this.$router.push({
        name: 'Order',
        query: {
          productId: this.product.id,
          quantity: this.quantity
        }
      })
    }
  }
}
</script>

<style scoped>
.product-detail {
  min-height: calc(100vh - 60px);
  background: #f5f5f5;
  padding: 20px 0;
}

.detail-content {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,.08);
}

.product-image {
  background: #f5f7fa;
  border-radius: 12px;
  overflow: hidden;
  padding: 20px;
}

.product-image img {
  width: 100%;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,.1);
}

.product-info h1 {
  font-size: 24px;
  margin-bottom: 15px;
}

.merchant-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  padding: 10px 15px;
  background: #f5f7fa;
  border-radius: 8px;
  color: #606266;
  font-size: 14px;
}

.merchant-info i {
  color: #409EFF;
  font-size: 16px;
}

.price {
  font-size: 32px;
  color: #f56c6c;
  font-weight: bold;
  margin-bottom: 10px;
}

.stock {
  color: #666;
  margin-bottom: 20px;
}

.description {
  color: #666;
  line-height: 1.8;
  margin-bottom: 30px;
}

.quantity {
  margin-bottom: 30px;
}

.actions {
  display: flex;
  gap: 20px;
  margin-top: 30px;
}

.actions .el-button {
  padding: 15px 40px;
  font-size: 16px;
  border-radius: 25px;
}

.reviews-section {
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #ebeef5;
}

.reviews-section h3 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #303133;
}

.no-reviews {
  color: #909399;
  padding: 30px 0;
  text-align: center;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.review-head .user-name {
  color: #606266;
  font-size: 14px;
}

.review-head .review-time {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.review-content {
  color: #606266;
  line-height: 1.6;
  margin: 0;
  font-size: 14px;
}
</style>

