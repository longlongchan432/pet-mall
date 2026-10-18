<template>
  <div class="order">
    <el-container>
      <el-main>
        <div class="container">
          <h2>确认订单</h2>
          <el-form :model="orderForm" label-width="100px">
            <el-form-item label="常用地址" v-if="addressList.length">
              <el-select v-model="selectedAddressId" placeholder="选择已保存的收货地址" style="width: 100%;" @change="applyAddress">
                <el-option v-for="a in addressList" :key="a.id" :value="a.id"
                  :label="a.receiverName + ' ' + a.receiverPhone + ' — ' + a.detail"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="收货地址">
              <el-input v-model="orderForm.address" placeholder="请输入收货地址"></el-input>
            </el-form-item>
            <el-form-item label="收货人">
              <el-input v-model="orderForm.receiverName" placeholder="请输入收货人姓名"></el-input>
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="orderForm.receiverPhone" placeholder="请输入联系电话"></el-input>
            </el-form-item>
            <el-form-item label="商品信息">
              <el-table :data="orderItems" style="width: 100%">
                <el-table-column label="商品" prop="productName"></el-table-column>
                <el-table-column label="单价" prop="price">
                  <template slot-scope="scope">¥{{ scope.row.price }}</template>
                </el-table-column>
                <el-table-column label="数量" prop="quantity"></el-table-column>
                <el-table-column label="小计">
                  <template slot-scope="scope">¥{{ (scope.row.price * scope.row.quantity).toFixed(2) }}</template>
                </el-table-column>
              </el-table>
            </el-form-item>
            <el-form-item label="支付方式">
              <el-radio-group v-model="paymentMethod" style="display: block;">
                <div class="pay-option"><el-radio :label="4" :disabled="!userBalance || (userBalance || 0) < totalAmount">余额支付 (¥{{ (userBalance || 0).toFixed(2) }})</el-radio></div>
              </el-radio-group>
              <p v-if="paymentMethod === 4 && (userBalance || 0) < totalAmount" class="balance-tip">余额不足，请先<a href="/user" @click.prevent="$router.push('/user')">充值</a></p>
            </el-form-item>
            <el-form-item>
              <div class="total-amount">
                总计：¥{{ totalAmount.toFixed(2) }}
              </div>
              <el-button type="primary" size="large" @click="submitOrder" :loading="loading">提交订单</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-main>
    </el-container>
    <!-- 余额支付密码验证弹窗 -->
    <el-dialog title="余额支付验证" :visible.sync="passwordDialogVisible" width="400px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="登录密码">
          <el-input v-model="passwordForm.password" type="password" placeholder="请输入登录密码" show-password></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPassword">确认支付</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'Order',
  data() {
    return {
      orderForm: {
        address: '',
        receiverName: '',
        receiverPhone: ''
      },
      orderItems: [],
      addressList: [],
      selectedAddressId: null,
      paymentMethod: 4,
      loading: false,
      userBalance: null,
      passwordDialogVisible: false,
      passwordForm: { password: '' },
      currentOrderId: null
    }
  },
  computed: {
    totalAmount() {
      return this.orderItems.reduce((sum, item) => {
        return sum + item.price * item.quantity
      }, 0)
    }
  },
  mounted() {
    this.loadOrderData()
    this.loadUserBalance()
    this.loadAddresses()
  },
  methods: {
    async loadAddresses() {
      try {
        const res = await api.get('/address/list')
        if (res.code === 200 && res.data && res.data.length) {
          this.addressList = res.data
          const def = res.data.find(a => a.isDefault === 1) || res.data[0]
          this.selectedAddressId = def.id
          this.applyAddress(def.id)
        }
      } catch (e) { /* 没有地址就保持手填 */ }
    },
    applyAddress(id) {
      const a = this.addressList.find(x => x.id === id)
      if (a) {
        this.orderForm.address = a.detail
        this.orderForm.receiverName = a.receiverName
        this.orderForm.receiverPhone = a.receiverPhone
      }
    },
    async loadUserBalance() {
      try {
        const res = await api.get('/user/info')
        if (res.code === 200 && res.data) this.userBalance = res.data.balance
      } catch (e) {
        this.userBalance = 0
      }
    },
    async loadOrderData() {
      const { productId, quantity } = this.$route.query
      if (productId) {
        // 立即购买
        try {
          const res = await api.get(`/product/public/${productId}`)
          if (res.code === 200) {
            this.orderItems = [{
              productId: res.data.id,
              productName: res.data.name,
              productImage: res.data.image,
              price: res.data.price,
              quantity: parseInt(quantity) || 1
            }]
          }
        } catch (e) {
          this.$message.error('加载商品失败')
        }
      } else {
        // 从购物车结算
        try {
          const res = await api.get('/cart/list')
          if (res.code === 200) {
            this.orderItems = res.data.map(item => ({
              productId: item.productId,
              productName: item.productName,
              productImage: item.productImage,
              price: item.price,
              quantity: item.quantity
            }))
          }
        } catch (e) {
          this.$message.error('加载购物车失败')
        }
      }
    },
    async submitOrder() {
      if (!this.orderForm.address || !this.orderForm.receiverName || !this.orderForm.receiverPhone) {
        this.$message.warning('请填写完整的收货信息')
        return
      }
      if (this.paymentMethod === 4 && (this.userBalance || 0) < this.totalAmount) {
        this.$message.warning('余额不足，请选择其他支付方式或先充值')
        return
      }
      this.loading = true
      try {
        const orderData = {
          address: this.orderForm.address,
          receiverName: this.orderForm.receiverName,
          receiverPhone: this.orderForm.receiverPhone,
          orderItems: this.orderItems
        }
        const res = await api.post('/order/create', orderData)
        if (res.code === 200) {
          this.currentOrderId = res.data.id
          if (this.paymentMethod === 4) {
            // 余额支付，弹出密码验证
            this.passwordDialogVisible = true
          } else {
            // 其他支付方式，直接支付
            await this.doPay()
          }
        }
      } catch (e) {
        this.$message.error(e.message || '创建订单失败')
      } finally {
        this.loading = false
      }
    },
    async doPay() {
      try {
        await api.post('/order/pay', {
          orderId: this.currentOrderId,
          paymentMethod: this.paymentMethod,
          password: this.passwordForm.password
        })
        this.$message.success('订单创建成功')
        this.passwordDialogVisible = false
        this.$router.push('/order-list')
      } catch (e) {
        this.$message.error(e.message || '支付失败')
      }
    },
    async confirmPassword() {
      if (!this.passwordForm.password) {
        this.$message.warning('请输入密码')
        return
      }
      try {
        await this.doPay()
      } catch (e) {
        this.$message.error(e.message || '密码错误')
      } finally {
        this.passwordForm.password = ''
      }
    }
  }
}
</script>

<style scoped>
.order {
  min-height: calc(100vh - 60px);
  background: #f5f5f5;
  padding: 20px 0;
}

.order h2 {
  margin-bottom: 20px;
}

.total-amount {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 20px;
}

.balance-tip {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 8px;
}

.balance-tip a {
  color: #409eff;
}

.pay-option {
  margin-bottom: 12px;
}
</style>



