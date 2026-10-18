<template>
  <div class="user-list">
    <div class="container">
      <h2>用户管理</h2>
      <el-table :data="users" style="width: 100%">
        <el-table-column prop="username" label="用户名"></el-table-column>
        <el-table-column prop="email" label="邮箱"></el-table-column>
        <el-table-column prop="phone" label="手机号"></el-table-column>
        <el-table-column prop="role" label="角色">
          <template slot-scope="scope">
            {{ scope.row.role === 0 ? '普通用户' : scope.row.role === 1 ? '商家' : '管理员' }}
          </template>
        </el-table-column>
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
    </div>
  </div>
</template>

<script>
import api from '../../api'

export default {
  name: 'UserList',
  data() {
    return {
      users: []
    }
  },
  mounted() {
    this.loadUsers()
  },
  methods: {
    async loadUsers() {
      try {
        const res = await api.get('/user/list')
        if (res.code === 200) {
          this.users = res.data
        }
      } catch (e) {
        this.$message.error('加载用户失败')
      }
    },
    async toggleStatus(user) {
      try {
        const res = await api.put('/user/status', {
          id: user.id,
          status: user.status === 1 ? 0 : 1
        })
        if (res.code === 200) {
          this.$message.success('操作成功')
          this.loadUsers()
        }
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async resetPassword(user) {
      this.$prompt('请输入新密码', '重置密码', {
        inputPattern: /.{6,}/,
        inputErrorMessage: '密码至少6位'
      }).then(async ({ value }) => {
        try {
          const res = await api.put('/user/reset-password-admin', {
            id: user.id,
            newPassword: value
          })
          if (res.code === 200) {
            this.$message.success('密码重置成功')
          }
        } catch (e) {
          this.$message.error('密码重置失败')
        }
      })
    }
  }
}
</script>

<style scoped>
.user-list {
  padding: 20px;
}
</style>



