<template>
  <div class="order-list">
    <div class="container">
      <h2>订单管理</h2>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="全部" name="all"></el-tab-pane>
        <el-tab-pane label="待发货" name="1"></el-tab-pane>
        <el-tab-pane label="待收货" name="2"></el-tab-pane>
        <el-tab-pane label="已完成" name="3"></el-tab-pane>
      </el-tabs>
      <el-table :data="orders" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号"></el-table-column>
        <el-table-column prop="userName" label="用户"></el-table-column>
        <el-table-column prop="totalAmount" label="金额">
          <template slot-scope="scope">¥{{ scope.row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template slot-scope="scope">{{ getStatusText(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间"></el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button
              v-if="scope.row.status === 1"
              size="mini"
              type="primary"
              @click="showShipDialog(scope.row)"
            >
              发货
            </el-button>
            <el-button
              v-if="scope.row.status === 5"
              size="mini"
              type="success"
              @click="processRefund(scope.row, true)"
            >
              同意退款
            </el-button>
            <el-button
              v-if="scope.row.status === 5"
              size="mini"
              type="danger"
              @click="processRefund(scope.row, false)"
            >
              拒绝退款
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-dialog title="发货" :visible.sync="shipDialogVisible">
        <el-form :model="shipForm" label-width="100px">
          <el-form-item label="物流单号">
            <el-input v-model="shipForm.logisticsNo"></el-input>
          </el-form-item>
          <el-form-item label="物流公司">
            <el-input v-model="shipForm.logisticsCompany"></el-input>
          </el-form-item>
        </el-form>
        <div slot="footer">
          <el-button @click="shipDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="shipOrder">确定</el-button>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import api from '../../api'

export default {
  name: 'OrderList',
  data() {
    return {
      orders: [],
      activeTab: 'all',
      shipDialogVisible: false,
      shipForm: {
        orderId: null,
        logisticsNo: '',
        logisticsCompany: ''
      }
    }
  },
  mounted() {
    this.loadOrders()
  },
  methods: {
    async loadOrders() {
      try {
        const res = await api.get('/order/merchant/list')
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
    showShipDialog(order) {
      this.shipForm.orderId = order.id
      this.shipDialogVisible = true
    },
    async shipOrder() {
      try {
        const res = await api.post('/order/merchant/ship', this.shipForm)
        if (res.code === 200) {
          this.$message.success('发货成功')
          this.shipDialogVisible = false
          this.loadOrders()
        }
      } catch (e) {
        this.$message.error('发货失败')
      }
    },
    async processRefund(order, agree) {
      const actionText = agree ? '同意' : '拒绝'
      try {
        await this.$confirm(`确定要${actionText}该退款申请吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: agree ? 'success' : 'warning'
        })
        const res = await api.post('/order/merchant/refund-process', { orderId: order.id, agree })
        if (res.code === 200) {
          this.$message.success(res.data || `${actionText}退款成功`)
          this.loadOrders()
        }
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('退款处理失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.order-list {
  padding: 20px;
}
</style>



