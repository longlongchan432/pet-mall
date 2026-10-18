<template>
  <div class="cart">
    <el-container>
      <el-main>
        <div class="container">
          <h2>购物车</h2>
          <div v-if="cartList.length === 0" class="empty-cart">
            <el-empty description="购物车是空的"></el-empty>
          </div>
          <div v-else>
            <!-- 按店铺分组显示 -->
            <div v-for="(merchantGroup, merchantId) in groupedCart" :key="merchantId" class="merchant-group">
              <div class="merchant-header">
                <i class="el-icon-shop"></i>
                <span class="merchant-name">{{ merchantGroup.merchantName || '未知店铺' }}</span>
              </div>
              <el-table :data="merchantGroup.items" style="width: 100%" class="cart-table">
                <el-table-column type="selection" width="55"></el-table-column>
                <el-table-column label="商品" width="350">
                  <template slot-scope="scope">
                    <div class="product-cell">
                      <img :src="getImageUrl(scope.row.productImage)" :alt="scope.row.productName" @error="handleImageError" class="product-img">
                      <div class="product-info">
                        <div class="product-name">{{ scope.row.productName }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="单价" width="120" align="center">
                  <template slot-scope="scope">
                    <span class="price-text">¥{{ scope.row.price }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="数量" width="150" align="center">
                  <template slot-scope="scope">
                    <el-input-number 
                      v-model="scope.row.quantity" 
                      :min="1" 
                      size="small"
                      @change="updateCart(scope.row)"
                    ></el-input-number>
                  </template>
                </el-table-column>
                <el-table-column label="小计" width="120" align="center">
                  <template slot-scope="scope">
                    <span class="subtotal-text">¥{{ (scope.row.price * scope.row.quantity).toFixed(2) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center">
                  <template slot-scope="scope">
                    <el-button type="text" size="small" @click="deleteCart(scope.row.id)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="merchant-total">
                店铺小计：¥{{ getMerchantTotal(merchantGroup.items).toFixed(2) }}
              </div>
            </div>
            <div class="cart-footer">
              <div class="total">
                总计：¥{{ totalAmount.toFixed(2) }}
              </div>
              <el-button type="primary" size="large" @click="checkout">去结算</el-button>
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
  name: 'Cart',
  data() {
    return {
      cartList: []
    }
  },
  computed: {
    totalAmount() {
      return this.cartList.reduce((sum, item) => {
        return sum + item.price * item.quantity
      }, 0)
    },
    groupedCart() {
      const groups = {}
      this.cartList.forEach(item => {
        const merchantId = item.merchantId || 'unknown'
        if (!groups[merchantId]) {
          groups[merchantId] = {
            merchantId: item.merchantId,
            merchantName: item.merchantName,
            items: []
          }
        }
        groups[merchantId].items.push(item)
      })
      return groups
    }
  },
  mounted() {
    this.loadCart()
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
    getMerchantTotal(items) {
      return items.reduce((sum, item) => {
        return sum + item.price * item.quantity
      }, 0)
    },
    async loadCart() {
      try {
        const res = await api.get('/cart/list')
        if (res.code === 200) {
          this.cartList = res.data
        }
      } catch (e) {
        this.$message.error('加载购物车失败')
      }
    },
    async updateCart(item) {
      try {
        await api.put('/cart/update', item)
      } catch (e) {
        this.$message.error('更新失败')
      }
    },
    async deleteCart(id) {
      try {
        await api.delete(`/cart/${id}`)
        this.$message.success('删除成功')
        this.loadCart()
      } catch (e) {
        this.$message.error('删除失败')
      }
    },
    checkout() {
      if (this.cartList.length === 0) {
        this.$message.warning('购物车是空的')
        return
      }
      this.$router.push('/order')
    }
  }
}
</script>

<style scoped>
.cart {
  min-height: calc(100vh - 60px);
  background: #f5f5f5;
  padding: 20px 0;
}

.cart h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.empty-cart {
  background: #fff;
  padding: 100px;
  text-align: center;
  border-radius: 8px;
}

.merchant-group {
  background: #fff;
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,.1);
}

.merchant-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 15px 20px;
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 10px;
}

.merchant-header i {
  font-size: 18px;
}

.merchant-name {
  flex: 1;
}

.cart-table {
  margin: 0;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
}

.product-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #eee;
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 5px;
}

.price-text {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.subtotal-text {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.merchant-total {
  background: #f8f9fa;
  padding: 15px 20px;
  text-align: right;
  font-size: 16px;
  font-weight: bold;
  color: #666;
  border-top: 1px solid #eee;
}

.cart-footer {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 25px 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,.1);
}

.total {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
}

.cart-footer .el-button {
  padding: 12px 40px;
  font-size: 16px;
}
</style>

