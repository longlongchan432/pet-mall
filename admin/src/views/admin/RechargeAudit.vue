<template>
  <div class="admin-recharge-audit">
    <el-card shadow="never" class="page-card">
      <!-- 筛选栏（匹配你的Element UI版本） -->
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="用户ID">
            <el-input v-model="searchForm.userId" placeholder="请输入用户ID"></el-input>
          </el-form-item>
          <el-form-item label="充值状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态">
              <el-option label="待审核" value="0"></el-option>
              <el-option label="审核通过" value="1"></el-option>
              <el-option label="审核拒绝" value="2"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchRechargeList">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 记录列表（适配你的字段） -->
      <el-table :data="rechargeList" border stripe style="width: 100%;margin-top:20px">
        <el-table-column prop="id" label="记录ID" width="80"></el-table-column>
        <el-table-column prop="userId" label="用户ID" width="80"></el-table-column>
        <el-table-column prop="rechargeNo" label="充值单号" min-width="180"></el-table-column>
        <el-table-column prop="amount" label="充值金额" width="120">
          <template slot-scope="scope">¥{{ scope.row.amount }}</template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="100">
          <template slot-scope="scope">
            <span v-if="scope.row.paymentMethod === 1">支付宝</span>
            <span v-else-if="scope.row.paymentMethod === 2">微信</span>
            <span v-else>银行卡</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success">审核通过</el-tag>
            <el-tag v-else type="danger">审核拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button
                v-if="scope.row.status === 0"
                type="success"
                size="small"
                @click="auditRecord(scope.row.id, 1)">审核通过</el-button>
            <el-button
                v-if="scope.row.status === 0"
                type="danger"
                size="small"
                @click="auditRecord(scope.row.id, 2)">审核拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页（匹配你的Element UI用法） -->
      <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalCount"
          style="margin-top:20px;text-align:right">
      </el-pagination>
    </el-card>
  </div>
</template>

<script>
import api from '../../api/index.js' // 适配你的api路径

export default {
  name: 'AdminRechargeAudit',
  data() {
    return {
      searchForm: {
        userId: '',
        status: ''
      },
      rechargeList: [],
      pageNum: 1,
      pageSize: 10,
      totalCount: 0
    }
  },
  mounted() {
    this.fetchRechargeList()
  },
  methods: {
    // 时间格式化（适配你的日期格式）
    formatTime(t) {
      if (!t) return ''
      const d = new Date(t)
      return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    },
    // 查询充值记录（适配你的api请求方式）
    fetchRechargeList() {
      api.get('/user/admin/recharge/list', {
        params: {
          userId: this.searchForm.userId || undefined,
          status: this.searchForm.status || undefined,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
      }).then(res => {
        this.rechargeList = res.data.list || []
        this.totalCount = res.data.total || 0
        this.pageNum = res.data.pageNum || 1
        this.pageSize = res.data.pageSize || 10
      }).catch(() => {
        this.rechargeList = []
        this.totalCount = 0
      })
    },
    // 重置筛选
    resetSearch() {
      this.searchForm = { userId: '', status: '' }
      this.pageNum = 1
      this.fetchRechargeList()
    },
    // 分页大小变更
    handleSizeChange(val) {
      this.pageSize = val
      this.fetchRechargeList()
    },
    // 页码变更
    handleCurrentChange(val) {
      this.pageNum = val
      this.fetchRechargeList()
    },
    // 审核充值记录（适配你的api请求方式）
    auditRecord(recordId, status) {
      const tip = status === 1 ? '通过' : '拒绝'
      this.$confirm(`确定要${tip}该充值申请吗？`, '审核确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: status === 1 ? 'success' : 'warning'
      }).then(() => {
        api.put('/user/admin/recharge/audit', {
          id: recordId,
          status: status
        }).then(res => {
          this.$message.success(res.message || `充值申请审核${tip}成功`)
          this.fetchRechargeList()
        }).catch(() => {
          this.$message.error(`充值申请审核${tip}失败`)
        })
      })
    }
  }
}
</script>

<style scoped>
.admin-recharge-audit {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}
.page-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}
.search-bar {
  margin-bottom: 10px;
}
</style>