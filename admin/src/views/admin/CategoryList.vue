<template>
  <div class="category-list">
    <div class="container">
      <div class="header">
        <h2>分类管理</h2>
        <el-button type="primary" @click="showAddDialog">新增分类</el-button>
      </div>
      <el-table :data="categories" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="name" label="分类名称"></el-table-column>
        <el-table-column prop="description" label="描述"></el-table-column>
        <el-table-column prop="parentId" label="父分类ID">
          <template slot-scope="scope">
            {{ scope.row.parentId || '无' }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="100"></el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button size="mini" @click="editCategory(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="deleteCategory(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 新增/编辑分类对话框 -->
      <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
        <el-form :model="categoryForm" :rules="categoryRules" ref="categoryForm" label-width="100px">
          <el-form-item label="分类名称" prop="name">
            <el-input v-model="categoryForm.name" placeholder="请输入分类名称"></el-input>
          </el-form-item>
          <el-form-item label="分类描述" prop="description">
            <el-input type="textarea" v-model="categoryForm.description" placeholder="请输入分类描述"></el-input>
          </el-form-item>
          <el-form-item label="父分类" prop="parentId">
            <el-select v-model="categoryForm.parentId" placeholder="请选择父分类（可选）" clearable>
              <el-option label="无" :value="null"></el-option>
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
                v-if="!isEdit || category.id !== categoryForm.id"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="categoryForm.sort" :min="0" placeholder="排序值，数字越小越靠前"></el-input-number>
          </el-form-item>
        </el-form>
        <div slot="footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCategory" :loading="submitLoading">确定</el-button>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import api from '../../api'

export default {
  name: 'CategoryList',
  data() {
    return {
      categories: [],
      dialogVisible: false,
      submitLoading: false,
      isEdit: false,
      categoryForm: {
        name: '',
        description: '',
        parentId: null,
        sort: 0
      },
      categoryRules: {
        name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑分类' : '新增分类'
    }
  },
  mounted() {
    this.loadCategories()
  },
  methods: {
    async loadCategories() {
      try {
        const res = await api.get('/category/admin/list')
        if (res.code === 200) {
          this.categories = res.data
        }
      } catch (e) {
        this.$message.error('加载分类失败')
      }
    },
    showAddDialog() {
      this.isEdit = false
      this.categoryForm = {
        name: '',
        description: '',
        parentId: null,
        sort: 0
      }
      this.dialogVisible = true
    },
    editCategory(category) {
      this.isEdit = true
      this.categoryForm = {
        id: category.id,
        name: category.name,
        description: category.description || '',
        parentId: category.parentId || null,
        sort: category.sort || 0
      }
      this.dialogVisible = true
    },
    async submitCategory() {
      this.$refs.categoryForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
            if (this.isEdit) {
              const res = await api.put('/category/admin/update', this.categoryForm)
              if (res.code === 200) {
                this.$message.success('更新成功')
                this.dialogVisible = false
                this.loadCategories()
              }
            } else {
              const res = await api.post('/category/admin/add', this.categoryForm)
              if (res.code === 200) {
                this.$message.success('添加成功')
                this.dialogVisible = false
                this.loadCategories()
              }
            }
          } catch (e) {
            this.$message.error('操作失败')
          } finally {
            this.submitLoading = false
          }
        }
      })
    },
    async deleteCategory(category) {
      this.$confirm('确定要删除该分类吗？', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await api.delete(`/category/admin/${category.id}`)
          if (res.code === 200) {
            this.$message.success('删除成功')
            this.loadCategories()
          }
        } catch (e) {
          this.$message.error('删除失败')
        }
      })
    }
  }
}
</script>

<style scoped>
.category-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>



