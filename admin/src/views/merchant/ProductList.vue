<template>
  <div class="product-list">
    <div class="container">
      <div class="header">
        <h2>商品管理</h2>
        <el-button type="primary" @click="$router.push('/merchant/product/add')">添加商品</el-button>
      </div>
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
        <el-table-column prop="price" label="价格">
          <template slot-scope="scope">¥{{ scope.row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存"></el-table-column>
        <el-table-column prop="sales" label="销量"></el-table-column>
        <el-table-column prop="status" label="状态">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button size="mini" @click="editProduct(scope.row)">编辑</el-button>
            <el-button
              size="mini"
              :type="scope.row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? '下架' : '上架' }}
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
        const res = await api.get('/product/merchant/list')
        if (res.code === 200) {
          this.products = res.data
        }
      } catch (e) {
        this.$message.error('加载商品失败')
      }
    },
    editProduct(product) {
      this.$router.push(`/merchant/product/edit/${product.id}`)
    },
    async toggleStatus(product) {
      try {
        const res = await api.put('/product/merchant/status', {
          id: product.id,
          status: product.status === 1 ? 0 : 1
        })
        if (res.code === 200) {
          this.$message.success('操作成功')
          this.loadProducts()
        }
      } catch (e) {
        this.$message.error('操作失败')
      }
    }
  }
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
}
</style>

