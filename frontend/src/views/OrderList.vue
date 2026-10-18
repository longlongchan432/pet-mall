<template>
  <div class="order-list">
    <el-container>
      <el-main>
        <div class="container">
          <h2>我的订单</h2>
          <el-tabs v-model="activeTab" @tab-click="handleTabClick">
            <el-tab-pane label="全部" name="all"></el-tab-pane>
            <el-tab-pane label="待支付" name="0"></el-tab-pane>
            <el-tab-pane label="待发货" name="1"></el-tab-pane>
            <el-tab-pane label="待收货" name="2"></el-tab-pane>
            <el-tab-pane label="已完成" name="3"></el-tab-pane>
            <el-tab-pane label="退款中" name="5"></el-tab-pane>
            <el-tab-pane label="已退款" name="6"></el-tab-pane>
          </el-tabs>
          <div v-if="orders.length === 0" class="empty">
            <el-empty description="暂无订单"></el-empty>
          </div>
          <div v-else>
            <div v-for="order in orders" :key="order.id" class="order-card">
              <div class="order-header">
                <span>订单号：{{ order.orderNo }}</span>
                <span class="status">{{ getStatusText(order.status) }}</span>
              </div>
              <div v-for="item in order.orderItems" :key="item.id" class="order-item">
                <img :src="getImageUrl(item.productImage)" alt="">
                <div class="item-info">
                  <h4>{{ item.productName }}</h4>
                  <p>¥{{ item.price }} x {{ item.quantity }}</p>
                  <el-button v-if="order.status === 3" type="text" size="small" @click="openReview(order.id, item)">评价</el-button>
                </div>
              </div>
              <div class="order-footer">
                <span class="total">总计：¥{{ order.totalAmount }}</span>
                <div class="actions">
                  <el-button v-if="order.status === 0" type="primary" @click="openPayDialog(order)">支付</el-button>
                  <el-button v-if="order.status === 0" @click="cancelOrder(order.id)">取消</el-button>
                  <el-button v-if="order.status === 2" type="success" @click="receiveOrder(order.id)">确认收货</el-button>
                  <el-button v-if="order.status === 2 || order.status === 3" @click="openTrack(order)">查看物流</el-button>
                  <el-button v-if="order.status === 3" type="primary" plain @click="openOrderReview(order)">评价</el-button>
                  <el-button v-if="order.status === 3" type="warning" @click="refundOrder(order.id)">申请退款</el-button>
                  <span v-if="order.status === 5" class="refund-status">等待商家处理退款</span>
                  <span v-if="order.status === 6" class="refund-status refunded">退款已完成</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 支付方式弹窗 -->
        <el-dialog title="选择支付方式" :visible.sync="payDialogVisible" width="450px">
          <el-radio-group v-model="payForm.paymentMethod" style="display: block;">
            <div class="pay-option"><el-radio :label="4" :disabled="!userBalance || userBalance < payableAmount()">余额支付 (¥{{ (userBalance || 0).toFixed(2) }})</el-radio></div>
          </el-radio-group>
          <el-select v-model="selectedCouponId" clearable placeholder="选择优惠券（可选）" style="width: 100%; margin-top: 8px;">
            <el-option v-for="c in myCoupons" :key="c.id" :value="c.id" :label="couponLabel(c)"></el-option>
          </el-select>
          <p class="pay-total">订单金额：¥{{ (payForm.orderAmount || 0).toFixed(2) }}<span v-if="selectedCouponId !== null">，券后应付：<b>¥{{ payableAmount().toFixed(2) }}</b></span></p>
          <span slot="footer">
            <el-button @click="payDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmPay">确认支付</el-button>
          </span>
        </el-dialog>
        <!-- 订单评价弹窗：本订单包含的商品（根据订单号已带出） -->
        <el-dialog title="订单评价" :visible.sync="orderReviewDialogVisible" width="500px">
          <p class="order-review-tip">本订单包含以下商品，点击「去评价」对对应商品进行评价</p>
          <div v-if="currentOrderForReview" class="order-review-items">
            <div v-for="item in currentOrderForReview.orderItems" :key="item.id" class="order-review-item">
              <img :src="getImageUrl(item.productImage)" alt="" class="item-thumb">
              <span class="item-name">{{ item.productName }}</span>
              <el-button type="text" size="small" @click="goReviewItem(item)">去评价</el-button>
            </div>
          </div>
          <span slot="footer">
            <el-button @click="orderReviewDialogVisible = false">关闭</el-button>
          </span>
        </el-dialog>
        <!-- 评价弹窗 -->
        <el-dialog title="评价商品" :visible.sync="reviewDialogVisible" width="500px">
          <el-form :model="reviewForm" label-width="80px">
            <el-form-item label="商品">
              <span>{{ reviewForm.productName }}</span>
            </el-form-item>
            <el-form-item label="评分">
              <el-rate v-model="reviewForm.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']"></el-rate>
            </el-form-item>
            <el-form-item label="评价内容">
              <el-input type="textarea" v-model="reviewForm.content" :rows="4" placeholder="说说你的使用体验吧~"></el-input>
            </el-form-item>
          </el-form>
          <span slot="footer">
            <el-button @click="reviewDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitReview">提交评价</el-button>
          </span>
        </el-dialog>
        <!-- 物流跟踪弹窗 -->
        <el-dialog title="物流跟踪" :visible.sync="trackDialogVisible" width="520px">
          <div v-if="trackList.length === 0" class="track-empty">暂无物流信息，商家发货后可查看</div>
          <el-timeline v-else>
            <el-timeline-item
              v-for="(t, i) in trackList"
              :key="i"
              :timestamp="t.time"
              placement="top"
              :color="i === 0 ? '#67C23A' : '#909399'"
            >
              {{ t.description }}
            </el-timeline-item>
          </el-timeline>
          <span slot="footer">
            <el-button @click="trackDialogVisible = false">关闭</el-button>
          </span>
        </el-dialog>
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
      </el-main>
    </el-container>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'OrderList',
  data() {
    return {
      orders: [],
      activeTab: 'all',
      reviewDialogVisible: false,
      reviewForm: {
        orderId: null,
        productId: null,
        productName: '',
        rating: 5,
        content: ''
      },
      payDialogVisible: false,
      payForm: { orderId: null, orderAmount: 0, paymentMethod: 1 },
      myCoupons: [],
      selectedCouponId: null,
      userBalance: null,
      passwordDialogVisible: false,
      passwordForm: { password: '' },
      orderReviewDialogVisible: false,
      currentOrderForReview: null,
      trackDialogVisible: false,
      trackList: []
    }
  },
  mounted() {
    this.loadOrders()
    this.loadUserBalance()
  },
  methods: {
    getImageUrl(imagePath) {
      if (!imagePath) return '/images/default.jpg'
      if (imagePath.startsWith('http')) return imagePath
      return imagePath // 相对路径：由 dev 代理 /upload 转发到文件服务器 nginx
    },
    async openTrack(order) {
      try {
        const res = await api.get(`/order/track/${order.id}`)
        this.trackList = res.code === 200 ? res.data : []
      } catch (e) {
        console.error(e)
        this.trackList = []
      }
      this.trackDialogVisible = true
    },
    /** 根据订单号直接带出该订单商品：单件直接评价，多件再选 */
    openOrderReview(order) {
      const items = order.orderItems || []
      if (items.length === 0) {
        this.$message.warning('该订单暂无商品信息')
        return
      }
      if (items.length === 1) {
        // 只买了一件，根据订单号就知道是哪个商品，直接打开评价
        this.openReview(order.id, items[0])
        return
      }
      this.currentOrderForReview = order
      this.orderReviewDialogVisible = true
    },
    goReviewItem(item) {
      if (!this.currentOrderForReview) return
      this.orderReviewDialogVisible = false
      this.openReview(this.currentOrderForReview.id, item)
    },
    openReview(orderId, item) {
      this.reviewForm = {
        orderId,
        productId: item.productId,
        productName: item.productName,
        rating: 5,
        content: ''
      }
      this.reviewDialogVisible = true
    },
    async submitReview() {
      if (!this.reviewForm.rating || this.reviewForm.rating < 1) {
        this.$message.warning('请选择评分')
        return
      }
      try {
        await api.post('/review/add', {
          orderId: this.reviewForm.orderId,
          productId: this.reviewForm.productId,
          rating: this.reviewForm.rating,
          content: this.reviewForm.content || ''
        })
        this.$message.success('评价成功')
        this.reviewDialogVisible = false
      } catch (e) {
        this.$message.error('评价失败')
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
    openPayDialog(order) {
      this.payForm = {
        orderId: order.id,
        orderAmount: parseFloat(order.totalAmount),
        paymentMethod: (this.userBalance || 0) >= parseFloat(order.totalAmount) ? 4 : 1
      }
      this.selectedCouponId = null
      this.loadUsableCoupons(parseFloat(order.totalAmount))
      this.payDialogVisible = true
    },
    async loadUsableCoupons(amount) {
      try {
        const res = await api.get('/coupon/my')
        this.myCoupons = res.code === 200
          ? (res.data || []).filter(c => c.status === 0 && parseFloat(c.threshold) <= amount)
          : []
      } catch (e) {
        this.myCoupons = []
      }
    },
    couponLabel(c) {
      return c.type === 1
        ? `${c.name}（满${parseFloat(c.threshold)}减${parseFloat(c.value)}）`
        : `${c.name}（${(parseFloat(c.value) * 10).toFixed(1)}折）`
    },
    payableAmount() {
      const c = this.myCoupons.find(x => x.id === this.selectedCouponId)
      if (!c) return this.payForm.orderAmount
      const paid = c.type === 1
        ? this.payForm.orderAmount - parseFloat(c.value)
        : this.payForm.orderAmount * parseFloat(c.value)
      return Math.max(0, paid)
    },
    async confirmPay() {
      if (this.payForm.paymentMethod === 4 && (this.userBalance || 0) < this.payableAmount()) {
        this.$message.warning('余额不足')
        return
      }
      if (this.payForm.paymentMethod === 4) {
        // 余额支付，弹出密码验证
        this.passwordDialogVisible = true
      } else {
        // 其他支付方式，直接支付
        await this.doPay()
      }
    },
    async doPay() {
      try {
        await api.post('/order/pay', {
          orderId: this.payForm.orderId,
          paymentMethod: this.payForm.paymentMethod,
          password: this.passwordForm.password,
          couponUserId: this.selectedCouponId || null
        })
        this.$message.success('支付成功')
        this.payDialogVisible = false
        this.passwordDialogVisible = false
        this.loadOrders()
        this.loadUserBalance()
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
    },
    async loadOrders() {
      try {
        const res = await api.get('/order/list')
        if (res.code === 200) {
          this.orders = res.data
          if (this.activeTab !== 'all') {
            this.orders = this.orders.filter(o => o.status === parseInt(this.activeTab))
          }
        }
      } catch (e) {
        this.$message.error('加载订单失败')
      }
    },
    handleTabClick(tab) {
      this.activeTab = tab.name
      this.loadOrders()
    },
    getStatusText(status) {
      const statusMap = {
        0: '待支付',
        1: '待发货',
        2: '待收货',
        3: '已完成',
        4: '已取消',
        5: '退款中',
        6: '已退款'
      }
      return statusMap[status] || '未知'
    },
    async cancelOrder(orderId) {
      try {
        await api.post('/order/cancel', { orderId })
        this.$message.success('取消成功')
        this.loadOrders()
      } catch (e) {
        this.$message.error('取消失败')
      }
    },
    async receiveOrder(orderId) {
      try {
        await api.post('/order/receive', { orderId })
        this.$message.success('确认收货成功')
        this.loadOrders()
      } catch (e) {
        this.$message.error('确认收货失败')
      }
    },
    async refundOrder(orderId) {
      try {
        await api.post('/order/refund', { orderId })
        this.$message.success('退款申请成功')
        this.loadOrders()
      } catch (e) {
        this.$message.error('退款申请失败')
      }
    }
  }
}
</script>

<style scoped>
.order-list {
  min-height: calc(100vh - 60px);
  background: #f5f5f5;
  padding: 20px 0;
}

.order-card {
  background: #fff;
  margin-bottom: 20px;
  padding: 20px;
  border-radius: 8px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.status {
  color: #409EFF;
  font-weight: bold;
}

.order-item {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.order-item img {
  width: 80px;
  height: 80px;
  object-fit: cover;
}

.item-info h4 {
  margin-bottom: 10px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.total {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.empty {
  background: #fff;
  padding: 100px;
  text-align: center;
}

.pay-option {
  margin-bottom: 12px;
}

.pay-total {
  margin-top: 16px;
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

.order-review-tip {
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
}

.order-review-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-review-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.order-review-item .item-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 12px;
}

.order-review-item .item-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.refund-status {
  color: #E6A23C;
  font-size: 14px;
  font-weight: bold;
}

.refund-status.refunded {
  color: #67C23A;
}
</style>



