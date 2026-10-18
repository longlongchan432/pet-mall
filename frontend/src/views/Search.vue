<template>
  <div class="search">
    <el-container>
      <el-main>
        <div class="container">
          <h2>搜索结果：{{ keyword }}</h2>
          <div class="product-list">
            <el-row :gutter="20">
              <el-col
                v-for="product in products"
                :key="product.id"
                :xs="12"
                :sm="8"
                :md="6"
                :lg="6"
              >
                <div class="product-card" @click="goToDetail(product.id)">
                  <div class="product-image">
                    <img :src="getImageUrl(product.image)" :alt="product.name" @error="handleImageError">
                  </div>
                  <div class="product-info">
                    <h3 class="product-name">{{ product.name }}</h3>
                    <p class="product-price">¥{{ product.price }}</p>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'Search',
  data() {
    return {
      keyword: '',
      products: []
    }
  },
  mounted() {
    this.keyword = this.$route.query.keyword || ''
    if (this.keyword) {
      this.search()
    }
  },
  watch: {
    '$route'(to) {
      this.keyword = to.query.keyword || ''
      if (this.keyword) {
        this.search()
      }
    }
  },
  methods: {
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
    async search() {
      try {
        const res = await api.get('/product/public/search', {
          params: { keyword: this.keyword }
        })
        if (res.code === 200) {
          this.products = res.data
        }
      } catch (e) {
        this.$message.error('搜索失败')
      }
    },
    goToDetail(id) {
      this.$router.push({ name: 'ProductDetail', params: { id } })
    }
  }
}
</script>

<style scoped>
.search {
  min-height: calc(100vh - 60px);
  background: #f5f5f5;
  padding: 20px 0;
}

.product-list {
  margin: 20px 0;
}

.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.product-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,.15);
  transform: translateY(-2px);
}

.product-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 16px;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
}
</style>

