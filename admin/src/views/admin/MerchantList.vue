<template>
  <div class="merchant-list">
    <div class="container">
      <div class="header">
        <h2>商家管理</h2>
        <el-button type="primary" @click="showAddDialog">新增商家</el-button>
      </div>
      <el-table :data="merchants" style="width: 100%">
        <el-table-column prop="username" label="用户名"></el-table-column>
        <el-table-column prop="email" label="邮箱"></el-table-column>
        <el-table-column prop="phone" label="手机号"></el-table-column>
        <el-table-column prop="status" label="状态">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button size="mini" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="mini" type="warning" @click="resetPassword(scope.row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 新增商家对话框 -->
      <el-dialog title="新增商家" :visible.sync="addDialogVisible" width="500px">
        <el-form :model="merchantForm" :rules="merchantRules" ref="merchantForm" label-width="100px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="merchantForm.username" placeholder="请输入用户名"></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input type="password" v-model="merchantForm.password" placeholder="请输入密码"></el-input>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="merchantForm.email" placeholder="请输入邮箱"></el-input>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="merchantForm.phone" placeholder="请输入手机号"></el-input>
          </el-form-item>
        </el-form>
        <div slot="footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addMerchant" :loading="addLoading">确定</el-button>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import api from '../../api'

export default {
  name: 'MerchantList',
  data() {
    return {
      merchants: [],
      addDialogVisible: false,
      addLoading: false,
      merchantForm: {
        username: '',
        password: '',
        email: '',
        phone: ''
      },
      merchantRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
        email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
        phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.loadMerchants()
  },
  methods: {
    async loadMerchants() {
      try {
        const res = await api.get('/user/list', { params: { role: 1 } })
        if (res.code === 200) {
          this.merchants = res.data
        }
      } catch (e) {
        this.$message.error('加载商家失败')
      }
    },
    async toggleStatus(merchant) {
      try {
        const res = await api.put('/user/status', {
          id: merchant.id,
          status: merchant.status === 1 ? 0 : 1
        })
        if (res.code === 200) {
          this.$message.success('操作成功')
          this.loadMerchants()
        }
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async resetPassword(merchant) {
      this.$prompt('请输入新密码', '重置密码', {
        inputPattern: /.{6,}/,
        inputErrorMessage: '密码至少6位'
      }).then(async ({ value }) => {
        try {
          const res = await api.put('/user/reset-password-admin', {
            id: merchant.id,
            newPassword: value
          })
          if (res.code === 200) {
            this.$message.success('密码重置成功')
          }
        } catch (e) {
          this.$message.error('密码重置失败')
        }
      })
    },
    showAddDialog() {
      this.addDialogVisible = true
      this.merchantForm = {
        username: '',
        password: '',
        email: '',
        phone: ''
      }
    },
    async addMerchant() {
      this.$refs.merchantForm.validate(async (valid) => {
        if (valid) {
          this.addLoading = true
          try {
            const res = await api.post('/user/admin/create-merchant', {
              ...this.merchantForm,
              role: 1 // 商家角色
            })
            if (res.code === 200) {
              this.$message.success('商家创建成功')
              this.addDialogVisible = false
              this.loadMerchants()
            }
          } catch (e) {
            this.$message.error('商家创建失败')
          } finally {
            this.addLoading = false
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.merchant-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>

