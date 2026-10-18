<template>
  <div class="admin-review-audit">
    <el-card shadow="never" class="page-card">
      <!-- 筛选栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="商品ID">
            <el-input v-model="searchForm.productId" placeholder="请输入商品ID"></el-input>
          </el-form-item>
          <el-form-item label="评价状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态">
              <el-option label="待审核" value="0"></el-option>
              <el-option label="审核通过" value="1"></el-option>
              <el-option label="审核拒绝" value="2"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchReviewList">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 评价列表 -->
      <el-table :data="reviewList" border stripe style="width: 100%;margin-top:20px">
        <el-table-column prop="id" label="评价ID" width="80"></el-table-column>
        <el-table-column prop="orderId" label="订单ID" width="100"></el-table-column>
        <el-table-column prop="productId" label="商品ID" width="100"></el-table-column>
        <el-table-column prop="userName" label="评价用户" width="100"></el-table-column>
        <el-table-column prop="rating" label="评分" width="80">
          <template slot-scope="scope">
            <el-rate v-model="scope.row.rating" max="5" disabled></el-rate>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200"></el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="审核状态" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === '0'" type="warning">待审核</el-tag>
            <el-tag v-if="scope.row.status === '1'" type="success">已通过</el-tag>
            <el-tag v-if="scope.row.status === '2'" type="danger">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" v-if="searchForm.status === '0'">
          <template slot-scope="scope">
            <el-button type="success" size="small" @click="passReview(scope.row.id)">审核通过</el-button>
            <el-button type="danger" size="small" @click="rejectReview(scope.row.id)">拒绝并删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
import api from '../../api/index.js'

export default {
  name: 'AdminReviewAudit',
  data() {
    return {
      searchForm: {
        productId: '',
        status: '0' // 默认查待审核
      },
      reviewList: [],
      pageNum: 1,
      pageSize: 10,
      totalCount: 0
    }
  },
  mounted() {
    this.fetchReviewList()
  },
  methods: {
    formatTime(t) {
      if (!t) return ''
      const d = new Date(t)
      return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    },
    fetchReviewList() {
      api.get('/review/admin/list', {
        params: {
          status: this.searchForm.status,
          productId: this.searchForm.productId || undefined,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
      }).then(res => {
        this.reviewList = res.data.list || []
        this.totalCount = res.data.total || 0
      }).catch(() => {
        this.reviewList = []
        this.totalCount = 0
      })
    },
    resetSearch() {
      this.searchForm = { productId: '', status: 0 }
      this.pageNum = 1
      this.fetchReviewList()
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.fetchReviewList()
    },
    handleCurrentChange(val) {
      this.pageNum = val
      this.fetchReviewList()
    },
    passReview(id) {
      this.$confirm('确定审核通过该评价吗？', '审核确认', {
        type: 'success'
      }).then(() => {
        api.put(`/review/admin/pass/${id}`).then(res => {
          this.$message.success(res.message)
          this.fetchReviewList()
        }).catch(() => {
          this.$message.error('审核通过失败')
        })
      })
    },
    rejectReview(id) {
      this.$confirm('确定拒绝并删除该评价吗？删除后不可恢复！', '删除确认', {
        type: 'warning'
      }).then(() => {
        api.delete(`/review/admin/reject/${id}`).then(res => {
          this.$message.success(res.message)
          this.fetchReviewList()
        }).catch(() => {
          this.$message.error('拒绝失败')
        })
      })
    }
  }
}
</script>

<style scoped>
.admin-review-audit {
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