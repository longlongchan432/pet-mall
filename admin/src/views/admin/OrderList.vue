<template>
  <div class="order-list">
    <div class="container">
      <h2>订单管理</h2>
      <el-table :data="orders" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号"></el-table-column>
        <el-table-column prop="userName" label="用户"></el-table-column>
        <el-table-column prop="merchantName" label="商家"></el-table-column>
        <el-table-column prop="totalAmount" label="金额">
          <template slot-scope="scope">¥{{ scope.row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template slot-scope="scope">{{ getStatusText(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间"></el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import api from '../../api'

export default {
  name: 'OrderList',
  data() {
    return {
      orders: []
    }
  },
  mounted() {
    this.loadOrders()
  },
  methods: {
    async loadOrders() {
      try {
        const res = await api.get('/order/admin/list')
        if (res.code === 200) {
          this.orders = res.data
        }
      } catch (e) {
        this.$message.error('加载订单失败')
      }
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
    }
  }
}
</script>

<style scoped>
.order-list {
  padding: 20px;
}
</style>



