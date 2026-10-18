<template>
  <div class="product-list">
    <div class="container">
      <h2>商品管理</h2>
      <el-table :data="products" style="width: 100%">
        <el-table-column label="商品图片" width="100">
          <template slot-scope="scope">
            <img 
              :src="getImageUrl(scope.row.image)" 
              alt="商品图片" 
              class="product-image"
              @error="handleImageError"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称"></el-table-column>
        <el-table-column prop="merchantName" label="商家"></el-table-column>
        <el-table-column prop="price" label="价格">
          <template slot-scope="scope">¥{{ scope.row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存"></el-table-column>
        <el-table-column prop="status" label="状态">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="danger"
              @click="deleteProduct(scope.row)"
            >
              下架
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import api from '../../api'

export default {
  name: 'ProductList',
  data() {
    return {
      products: []
    }
  },
  mounted() {
    this.loadProducts()
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
    async loadProducts() {
      try {
        const res = await api.get('/product/admin/list')
        if (res.code === 200) {
          this.products = res.data
        }
      } catch (e) {
        this.$message.error('加载商品失败')
      }
    },
    async deleteProduct(product) {
      this.$confirm('确定要下架该商品吗？', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await api.delete(`/product/admin/${product.id}`)
          if (res.code === 200) {
            this.$message.success('下架成功')
            this.loadProducts()
          }
        } catch (e) {
          this.$message.error('下架失败')
        }
      })
    }
  }
}
</script>

<style scoped>
.product-list {
  padding: 20px;
}

.product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
}
</style>

