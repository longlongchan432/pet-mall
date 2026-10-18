<template>
  <div class="product-form">
    <div class="container">
      <h2>{{ isEdit ? '编辑商品' : '添加商品' }}</h2>
      <el-form :model="form" :rules="rules" ref="productForm" label-width="100px" style="max-width: 800px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input type="textarea" v-model="form.description" :rows="4"></el-input>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2"></el-input-number>
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0"></el-input-number>
        </el-form-item>
        <el-form-item label="商品图片" prop="image">
          <el-upload
            class="upload-demo"
            action="/api/file/upload"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :show-file-list="false"
            :before-upload="beforeUpload"
          >
            <el-button size="small" type="primary">点击上传</el-button>
            <div slot="tip" class="el-upload__tip">只能上传 jpg/png/gif/webp/bmp 图片，单张不超过 5MB</div>
          </el-upload>
          <div v-if="form.image" class="image-preview">
            <img :src="getImageUrl(form.image)" alt="商品图片" style="max-width: 200px; max-height: 200px; margin-top: 10px;">
            <el-button size="mini" type="danger" @click="form.image = ''" style="margin-left: 10px;">删除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit" :loading="loading">保存</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import api from '../../api'

export default {
  name: 'ProductForm',
  data() {
    return {
      form: {
        name: '',
        description: '',
        price: 0,
        stock: 0,
        image: '',
        categoryId: null
      },
      categories: [],
      rules: {
        name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
        price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
        stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
      },
      loading: false,
      uploadHeaders: {}
    }
  },
  mounted() {
    // 设置上传请求头
    const token = this.$store.state.user.token
    if (token) {
      this.uploadHeaders = {
        'Authorization': 'Bearer ' + token
      }
    }
    this.loadCategories()
    if (this.isEdit) {
      this.loadProduct()
    }
  },
  computed: {
    isEdit() {
      return !!this.$route.params.id
    }
  },
  methods: {
    getImageUrl(imagePath) {
      if (!imagePath) return ''
      if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
        return imagePath
      }
      return imagePath // 相对路径：由 dev 代理 /upload 转发到文件服务器 nginx
    },
    beforeUpload(file) {
      // 与后端白名单/上限保持一致（后端：png/jpg/jpeg/gif/webp/bmp，5MB），别再按 2MB 拒掉后端能收的图
      const okType = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp'].includes(file.type)
      const okSize = file.size / 1024 / 1024 < 5
      if (!okType) {
        this.$message.error('上传图片只能是 JPG/PNG/GIF/WEBP/BMP 格式!')
        return false
      }
      if (!okSize) {
        this.$message.error('上传图片大小不能超过 5MB!')
        return false
      }
      return true
    },
    handleUploadSuccess(response) {
      if (response.code === 200) {
        this.form.image = response.data.url
        this.$message.success('图片上传成功')
      } else {
        this.$message.error(response.message || '图片上传失败')
      }
    },
    handleUploadError() {
      this.$message.error('图片上传失败')
    },
    async loadCategories() {
      try {
        const res = await api.get('/category/public/list')
        if (res.code === 200) {
          this.categories = res.data
        }
      } catch (e) {
        console.error(e)
      }
    },
    async loadProduct() {
      try {
        const res = await api.get(`/product/public/${this.$route.params.id}`)
        if (res.code === 200) {
          this.form = {
            id: res.data.id,
            name: res.data.name,
            description: res.data.description,
            price: res.data.price,
            stock: res.data.stock,
            image: res.data.image,
            categoryId: res.data.categoryId
          }
        }
      } catch (e) {
        this.$message.error('加载商品失败')
      }
    },
    async submit() {
      this.$refs.productForm.validate(async (valid) => {
        if (valid) {
          this.loading = true
          try {
            if (this.isEdit) {
              const res = await api.put('/product/merchant/update', this.form)
              if (res.code === 200) {
                this.$message.success('更新成功')
                this.$router.push('/merchant/products')
              }
            } else {
              const res = await api.post('/product/merchant/add', this.form)
              if (res.code === 200) {
                this.$message.success('添加成功')
                this.$router.push('/merchant/products')
              }
            }
          } catch (e) {
            this.$message.error('操作失败')
          } finally {
            this.loading = false
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.product-form {
  padding: 20px;
}
</style>

