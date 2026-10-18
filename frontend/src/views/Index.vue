<template>
  <div class="index">
    <el-container>
      <el-main>
        <div class="container">
          <!-- 轮播图 -->
          <div class="banner-section">
            <el-carousel :interval="4000" type="card" height="400px" indicator-position="outside">
              <el-carousel-item v-for="(item, index) in bannerList" :key="index">
                <div class="banner-item" :style="{ backgroundImage: `url(${item.image})` }">
                  <div class="banner-content">
                    <h2>{{ item.title }}</h2>
                    <p>{{ item.description }}</p>
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>
          </div>

          <!-- 商品推荐 -->
          <div class="recommend-section">
            <div class="section-title">
              <i class="el-icon-star-on"></i>
              <span>热门推荐</span>
            </div>
            <div class="recommend-products">
              <el-carousel :interval="5000" type="card" height="300px" indicator-position="outside">
                <el-carousel-item v-for="product in recommendProducts" :key="product.id">
                  <div class="product-card recommend-card" @click="goToDetail(product.id)">
                    <div class="product-image">
                      <img :src="getImageUrl(product.image)" :alt="product.name" @error="handleImageError">
                      <div class="hot-badge" v-if="product.sales > 100">
                        <i class="el-icon-star-on"></i>
                        热销
                      </div>
                    </div>
                    <div class="product-info">
                      <h3 class="product-name">{{ product.name }}</h3>
                      <p class="product-price">¥{{ product.price }}</p>
                      <p class="product-sales">已售{{ product.sales || 0 }}</p>
                    </div>
                  </div>
                </el-carousel-item>
              </el-carousel>
            </div>
          </div>

          <!-- 分类导航 -->
          <div class="category-nav" id="category-section">
            <div class="category-title">
              <i class="el-icon-menu"></i>
              <span>商品分类</span>
            </div>
            <div class="category-items">
              <div 
                class="category-item" 
                :class="{ active: activeCategory === '0' }"
                @click="loadProducts(null)"
              >
                全部
              </div>
              <div
                v-for="category in categories"
                :key="category.id"
                class="category-item"
                :class="{ active: activeCategory === String(category.id) }"
                @click="loadProducts(category.id)"
              >
                {{ category.name }}
              </div>
            </div>
          </div>

          <!-- 商品列表 -->
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
                    <p class="product-sales">已售{{ product.sales || 0 }}</p>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- 分页 -->
          <el-pagination
            v-if="total > 0"
            @current-change="handlePageChange"
            :current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            class="pagination"
          ></el-pagination>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'Index',
  data() {
    return {
      categories: [],
      allProducts: [],
      products: [],
      recommendProducts: [],
      activeCategory: '0',
      currentPage: 1,
      pageSize: 12,
      total: 0,
      bannerList: [
        {
          image: require('../images/微信图片_20260118144754.jpg'),
          title: '宠旅驿站',
          description: '为您的爱宠提供最优质的服务'
        },
        {
          image: require('../images/微信图片_20260118144822.jpg'),
          title: '精选商品',
          description: '品质保证，值得信赖'
        },
        {
          image: require('../images/微信图片_20260118144827.jpg'),
          title: '贴心服务',
          description: '专业团队，用心呵护'
        }
      ]
    }
  },
  mounted() {
    this.loadCategories()
    this.loadProducts()
    this.loadRecommendProducts()
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
    async loadCategories() {
      try {
        const res = await api.get('/category/public/list')
        if (res.code === 200) {
          this.categories = res.data
        }
      } catch (e) {
        console.error(e)
      }
    },
    async loadProducts(categoryId) {
      try {
        this.activeCategory = categoryId ? String(categoryId) : '0'
        const params = {
          status: 1
        }
        if (categoryId) {
          params.categoryId = categoryId
        }
        const res = await api.get('/product/public/list', { params })
        if (res.code === 200) {
          this.allProducts = res.data
          this.total = res.data.length
          this.currentPage = 1
          this.updatePage()
        }
      } catch (e) {
        console.error(e)
      }
    },
    updatePage() {
      const start = (this.currentPage - 1) * this.pageSize
      this.products = this.allProducts.slice(start, start + this.pageSize)
    },
    handlePageChange(page) {
      this.currentPage = page
      this.updatePage()
    },
    goToDetail(id) {
      this.$router.push({ name: 'ProductDetail', params: { id } })
    },
    async loadRecommendProducts() {
      try {
        const res = await api.get('/product/public/list', { 
          params: { status: 1 } 
        })
        if (res.code === 200) {
          // 随机选择5个商品进行轮播
          const shuffled = res.data.sort(() => 0.5 - Math.random())
          this.recommendProducts = shuffled.slice(0, 5)
        }
      } catch (e) {
        console.error(e)
      }
    }
  }
}
</script>

<style scoped>
.index {
  min-height: calc(100vh - 60px);
  background: #f5f5f5;
}

/* 轮播图样式 */
.banner-section {
  margin: 20px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,.1);
}

.banner-item {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.banner-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
}

.banner-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
  padding: 40px;
}

.banner-content h2 {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 20px;
  text-shadow: 2px 2px 4px rgba(0,0,0,.5);
}

.banner-content p {
  font-size: 24px;
  text-shadow: 1px 1px 2px rgba(0,0,0,.5);
}

/* 商品推荐样式 */
.recommend-section {
  margin: 40px 0;
  background: #fff;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 3px solid #667eea;
}

.section-title i {
  color: #f56c6c;
  font-size: 28px;
}

.recommend-products {
  margin-top: 20px;
}

.recommend-card {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.el-carousel__item {
  display: flex;
  align-items: center;
  justify-content: center;
}

.el-carousel__item.is-active {
  z-index: 1;
}

.recommend-products .el-carousel {
  margin-top: 20px;
}

.hot-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: linear-gradient(135deg, #f56c6c 0%, #ff8c8c 100%);
  color: #fff;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.4);
}

.hot-badge i {
  font-size: 14px;
}

.category-nav {
  background: #fff;
  margin: 20px 0;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
}

.category-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.category-title i {
  color: #409EFF;
  font-size: 18px;
}

.category-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-item {
  padding: 8px 20px;
  background: #f5f7fa;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  color: #606266;
  border: 1px solid transparent;
}

.category-item:hover {
  background: #ecf5ff;
  color: #409EFF;
  border-color: #b3d8ff;
}

.category-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
}

.product-list {
  margin: 20px 0;
}

.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
  border: 1px solid #f0f0f0;
}

.product-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,.15);
  transform: translateY(-4px);
  border-color: #409EFF;
}

.product-image {
  width: 100%;
  height: 220px;
  overflow: hidden;
  background: #f5f7fa;
  position: relative;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .product-image img {
  transform: scale(1.05);
}

.product-info {
  padding: 18px;
}

.product-name {
  font-size: 15px;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
}

.product-price {
  color: #f56c6c;
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 8px;
}

.product-sales {
  color: #909399;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.pagination {
  text-align: center;
  margin: 40px 0;
}
</style>

