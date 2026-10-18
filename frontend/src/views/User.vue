<template>
  <div class="user">
    <el-container>
      <el-main>
        <div class="container">
          <h2>个人中心</h2>
          <div class="balance-card" v-if="userForm.balance !== undefined">
            <span class="label">账户余额</span>
            <span class="amount">¥{{ (userForm.balance || 0).toFixed(2) }}</span>
          </div>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="个人信息" name="info">
              <el-form :model="userForm" label-width="100px" style="max-width: 600px">
                <el-form-item label="用户名">
                  <el-input v-model="userForm.username" disabled></el-input>
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="userForm.email"></el-input>
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="userForm.phone"></el-input>
                </el-form-item>
                <el-form-item label="真实姓名">
                  <el-input v-model="userForm.realName"></el-input>
                </el-form-item>
                <el-form-item label="地址">
                  <el-input v-model="userForm.address" type="textarea"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="updateInfo">保存</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="账户充值" name="recharge">
              <div class="recharge-box">
                <p class="tip">当前余额：¥{{ (userForm.balance || 0).toFixed(2) }}</p>
                <el-form label-width="100px" style="max-width: 400px">
                  <el-form-item label="充值金额">
                    <el-radio-group v-model="rechargeForm.amount">
                      <el-radio-button :label="50">50元</el-radio-button>
                      <el-radio-button :label="100">100元</el-radio-button>
                      <el-radio-button :label="200">200元</el-radio-button>
                      <el-radio-button :label="500">500元</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="其他金额">
                    <el-input-number v-model="rechargeForm.customAmount" :min="1" :max="10000" placeholder="输入金额"></el-input-number>
                    <span class="unit">元</span>
                  </el-form-item>
                  <el-form-item label="支付方式">
                    <el-radio-group v-model="rechargeForm.paymentMethod">
                      <el-radio :label="1">支付宝</el-radio>
                      <el-radio :label="2">微信</el-radio>
                      <el-radio :label="3">银行卡</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="doRecharge">确认充值</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
            <el-tab-pane label="我的评价" name="reviews">
              <div v-if="myReviews.length === 0" class="empty-reviews">
                <el-empty description="暂无评价"></el-empty>
              </div>
              <div v-else class="review-list">
                <div v-for="r in myReviews" :key="r.id" class="review-item">
                  <div class="review-header">
                    <el-rate v-model="r.rating" disabled show-score text-color="#ff9900"></el-rate>
                    <span class="time">{{ formatTime(r.createTime) }}</span>
                    <!-- 新增：评价状态标签 -->
                    <el-tag
                        class="status-tag"
                        :type="getStatusTagType(r.status)"
                        size="small">
                      {{ getStatusText(r.status) }}
                    </el-tag>
                  </div>
                  <p class="content">{{ r.content || '暂无文字评价' }}</p>
                  <p class="product">商品ID：{{ r.productId }}</p>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="password">
              <el-form :model="passwordForm" label-width="100px" style="max-width: 600px">
                <el-form-item label="原密码">
                  <el-input type="password" v-model="passwordForm.oldPassword"></el-input>
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input type="password" v-model="passwordForm.newPassword"></el-input>
                </el-form-item>
                <el-form-item label="确认密码">
                  <el-input type="password" v-model="passwordForm.confirmPassword"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="updatePassword">修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="我的宠物" name="pets">
              <div style="max-width: 600px">
                <el-button type="primary" size="small" @click="openAddPet" style="margin-bottom: 16px">＋ 添加宠物</el-button>
                <div v-if="petProfiles.length === 0" class="empty-reviews">
                  <el-empty description="还没有添加宠物档案，添加后AI导购能给你更精准的推荐"></el-empty>
                </div>
                <div v-for="p in petProfiles" :key="p.id" class="pet-card">
                  <div class="pet-card-header">
                    <span class="pet-name">{{ getPetEmoji(p.petType) }} {{ p.breed || p.petType }}{{ p.ageMonths ? ' · ' + Math.floor(p.ageMonths / 12) + '岁' : '' }}</span>
                    <div>
                      <el-tag v-if="p.isPrimary === 1" type="primary" size="mini">主要</el-tag>
                      <el-button size="mini" type="text" @click="editPet(p)">编辑</el-button>
                      <el-button size="mini" type="text" style="color:#f56c6c" @click="removePet(p.id)">删除</el-button>
                    </div>
                  </div>
                  <div class="pet-card-body">
                    <span>类型：{{ p.petType }}</span>
                    <span v-if="p.ageMonths">年龄：{{ p.ageMonths }} 月</span>
                    <span v-if="p.weightKg">体重：{{ p.weightKg }} kg</span>
                    <span v-if="p.notes">备注：{{ p.notes }}</span>
                  </div>
                </div>
              </div>
              <!-- 宠物编辑弹窗 -->
              <el-dialog :title="petDialogTitle" :visible.sync="petDialogVisible" width="500px">
                <el-form :model="petForm" label-width="80px">
                  <el-form-item label="宠物类型">
                    <el-select v-model="petForm.petType" placeholder="请选择">
                      <el-option label="狗 (dog)" value="dog"></el-option>
                      <el-option label="猫 (cat)" value="cat"></el-option>
                      <el-option label="鸟 (bird)" value="bird"></el-option>
                      <el-option label="鱼 (fish)" value="fish"></el-option>
                      <el-option label="其他 (other)" value="other"></el-option>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="品种">
                    <el-input v-model="petForm.breed" placeholder="如：金毛、英短"></el-input>
                  </el-form-item>
                  <el-form-item label="年龄（月）">
                    <el-input-number v-model="petForm.ageMonths" :min="0" :max="500"></el-input-number>
                  </el-form-item>
                  <el-form-item label="体重（kg）">
                    <el-input-number v-model="petForm.weightKg" :min="0" :precision="2" :step="0.1"></el-input-number>
                  </el-form-item>
                  <el-form-item label="备注">
                    <el-input v-model="petForm.notes" type="textarea" placeholder="如：对鸡肉过敏"></el-input>
                  </el-form-item>
                  <el-form-item label="主要宠物">
                    <el-switch v-model="petForm.isPrimary" :active-value="1" :inactive-value="0"></el-switch>
                  </el-form-item>
                </el-form>
                <div slot="footer">
                  <el-button @click="petDialogVisible = false">取消</el-button>
                  <el-button type="primary" @click="savePet">保存</el-button>
                </div>
              </el-dialog>
            </el-tab-pane>
            <el-tab-pane label="优惠券" name="coupon">
              <div style="max-width: 700px">
                <h4 style="margin: 0 0 12px">领券中心</h4>
                <div v-if="availableCoupons.length === 0" class="empty-reviews">
                  <el-empty description="暂无可领取的优惠券"></el-empty>
                </div>
                <div v-for="c in availableCoupons" :key="c.id" style="display:flex;justify-content:space-between;align-items:center;border:1px solid #ebeef5;border-radius:6px;padding:10px 14px;margin-bottom:10px;">
                  <div>
                    <b>{{ couponLabel(c) }}</b>
                    <div style="color:#909399;font-size:12px;">{{ c.name }} · 剩余 {{ c.stock }} 张</div>
                  </div>
                  <el-button type="primary" size="small" @click="receiveCoupon(c)">领取</el-button>
                </div>
                <h4 style="margin: 20px 0 12px">我的优惠券</h4>
                <div v-if="myCoupons.length === 0" class="empty-reviews">
                  <el-empty description="还没有优惠券，去上面领一张吧"></el-empty>
                </div>
                <div v-for="c in myCoupons" :key="c.id" style="display:flex;justify-content:space-between;align-items:center;border:1px solid #ebeef5;border-radius:6px;padding:10px 14px;margin-bottom:10px;">
                  <div>
                    <b style="color:#606266">{{ couponLabel(c) }}</b>
                    <div style="color:#909399;font-size:12px;">{{ c.name }} · {{ formatTime(c.receiveTime) }} 领取</div>
                  </div>
                  <el-tag :type="c.status === 1 ? 'info' : 'success'" size="mini">{{ c.status === 1 ? '已使用' : '未使用' }}</el-tag>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="收货地址" name="address">
              <div style="max-width: 650px">
                <el-button type="primary" size="small" @click="openAddAddress" style="margin-bottom: 14px">＋ 新增收货地址</el-button>
                <div v-if="addressList.length === 0" class="empty-reviews">
                  <el-empty description="还没有收货地址，下单会更方便哦"></el-empty>
                </div>
                <div v-for="a in addressList" :key="a.id" style="display:flex;justify-content:space-between;align-items:center;border:1px solid #ebeef5;border-radius:6px;padding:12px 14px;margin-bottom:10px;">
                  <div>
                    <div><b>{{ a.receiverName }}</b> <span style="color:#909399;margin-left:8px">{{ a.receiverPhone }}</span> <el-tag v-if="a.isDefault===1" type="success" size="mini" style="margin-left:8px">默认</el-tag></div>
                    <div style="color:#606266;font-size:13px;margin-top:4px">{{ a.detail }}</div>
                  </div>
                  <div>
                    <el-button size="mini" type="text" v-if="a.isDefault!==1" @click="setDefaultAddress(a.id)">设为默认</el-button>
                    <el-button size="mini" type="text" @click="editAddress(a)">编辑</el-button>
                    <el-button size="mini" type="text" style="color:#f56c6c" @click="removeAddress(a.id)">删除</el-button>
                  </div>
                </div>
                <el-dialog :title="addressDialogTitle" :visible.sync="addressDialogVisible" width="480px">
                  <el-form :model="addressForm" label-width="80px">
                    <el-form-item label="收货人"><el-input v-model="addressForm.receiverName"></el-input></el-form-item>
                    <el-form-item label="手机号"><el-input v-model="addressForm.receiverPhone"></el-input></el-form-item>
                    <el-form-item label="详细地址"><el-input type="textarea" v-model="addressForm.detail"></el-input></el-form-item>
                    <el-form-item label="设为默认"><el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0"></el-switch></el-form-item>
                  </el-form>
                  <span slot="footer"><el-button @click="addressDialogVisible=false">取消</el-button><el-button type="primary" @click="saveAddress">保存</el-button></span>
                </el-dialog>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import api from '../api'
import { mapState } from 'vuex'

export default {
  name: 'User',
  data() {
    return {
      activeTab: 'info',
      userForm: {},
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      rechargeForm: {
        amount: 100,
        customAmount: null,
        paymentMethod: 1
      },
      myReviews: [],
      // 优惠券
      availableCoupons: [],
      myCoupons: [],
      // 收货地址
      addressList: [],
      addressDialogVisible: false,
      addressDialogTitle: '新增地址',
      addressForm: { id: null, receiverName: '', receiverPhone: '', detail: '', isDefault: 0 },
      // 宠物档案
      petProfiles: [],
      petDialogVisible: false,
      petDialogTitle: '添加宠物',
      petForm: {
        id: null,
        petType: '',
        breed: '',
        ageMonths: null,
        weightKg: null,
        notes: '',
        isPrimary: 0
      }
    }
  },
  computed: {
    ...mapState({
      userInfo: state => state.user.info
    })
  },
  mounted() {
    this.loadUserInfo()
  },
  watch: {
    activeTab(val) {
      if (val === 'reviews') this.loadMyReviews()
      if (val === 'pets') this.loadPetProfiles()
      if (val === 'coupon') {
        this.loadAvailableCoupons()
        this.loadMyCoupons()
      }
      if (val === 'address') this.loadAddressList()
    }
  },
  methods: {
    formatTime(t) {
      if (!t) return ''
      const d = new Date(t)
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0') + ' ' + String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
    },
    // 映射评价状态
    getStatusText(status) {
      // 根据后端实际返回的类型调整判断
      if (status === 1 || status === '1') {
        return '审核通过';
      } else if (status === 0 || status === '0' || status === 'pending') {
        return '待审核';
      } else if (status === 2 || status === '2' || status === 'rejected') {
        return '已拒绝';
      } else {
        return '未知状态';
      }
    },
    // 新增：获取评价状态标签类型（Element UI 标签样式）
    getStatusTagType(status) {
      switch (status) {
        case 0:
          return 'warning'; // 黄色
        case 1:
          return 'success'; // 绿色
        case 2:
          return 'danger';  // 红色
        default:
          return 'info';    // 蓝色
      }
    },
    couponLabel(c) {
      return c.type === 1
        ? `满${parseFloat(c.threshold)}减${parseFloat(c.value)}`
        : `${(parseFloat(c.value) * 10).toFixed(1)}折`
    },
    async loadAvailableCoupons() {
      try {
        const res = await api.get('/coupon/list')
        this.availableCoupons = res.code === 200 ? (res.data || []) : []
      } catch (e) {
        this.availableCoupons = []
      }
    },
    async loadMyCoupons() {
      try {
        const res = await api.get('/coupon/my')
        this.myCoupons = res.code === 200 ? (res.data || []) : []
      } catch (e) {
        this.myCoupons = []
      }
    },
    async receiveCoupon(c) {
      try {
        const res = await api.post(`/coupon/receive/${c.id}`)
        this.$message.success(res.message || '领取成功')
        this.loadAvailableCoupons()
        this.loadMyCoupons()
      } catch (e) {
        this.$message.error(e.message || '领取失败')
      }
    },
    async loadAddressList() {
      try {
        const res = await api.get('/address/list')
        if (res.code === 200) this.addressList = res.data || []
      } catch (e) {
        this.addressList = []
      }
    },
    openAddAddress() {
      this.addressDialogTitle = '新增地址'
      this.addressForm = { id: null, receiverName: '', receiverPhone: '', detail: '', isDefault: 0 }
      this.addressDialogVisible = true
    },
    editAddress(a) {
      this.addressDialogTitle = '编辑地址'
      this.addressForm = { id: a.id, receiverName: a.receiverName, receiverPhone: a.receiverPhone, detail: a.detail, isDefault: a.isDefault }
      this.addressDialogVisible = true
    },
    async saveAddress() {
      if (!this.addressForm.receiverName || !this.addressForm.receiverPhone || !this.addressForm.detail) {
        this.$message.warning('请填写完整收货信息')
        return
      }
      try {
        if (this.addressForm.id) {
          await api.put('/address/update/' + this.addressForm.id, this.addressForm)
        } else {
          await api.post('/address/add', this.addressForm)
        }
        this.$message.success('保存成功')
        this.addressDialogVisible = false
        this.loadAddressList()
      } catch (e) {
        this.$message.error(e.message || '保存失败')
      }
    },
    async removeAddress(id) {
      try {
        await api.delete('/address/delete/' + id)
        this.$message.success('已删除')
        this.loadAddressList()
      } catch (e) {
        this.$message.error(e.message || '删除失败')
      }
    },
    async setDefaultAddress(id) {
      try {
        await api.post('/address/default/' + id)
        this.loadAddressList()
      } catch (e) {
        this.$message.error(e.message || '设置失败')
      }
    },
    async loadMyReviews() {
      try {
        const res = await api.get('/review/my-list')
        if (res.code === 200) this.myReviews = res.data || []
      } catch (e) {
        this.myReviews = []
      }
    },
    async doRecharge() {
      const amount = this.rechargeForm.customAmount > 0 ? this.rechargeForm.customAmount : this.rechargeForm.amount
      if (!amount || amount < 1) {
        this.$message.warning('请选择或输入充值金额')
        return
      }
      try {
        const res = await api.post('/user/recharge', {
          amount: amount,
          paymentMethod: this.rechargeForm.paymentMethod
        })
        if (res.code === 200) {
          this.$message.success(res.message || '充值申请提交成功，等待管理员审核')
          //this.loadUserInfo()
          //this.$store.dispatch('getUserInfo')
        }
      } catch (e) {
        this.$message.error('充值申请提交失败')
      }
    },
    async loadUserInfo() {
      try {
        const res = await api.get('/user/info')
        if (res.code === 200) {
          this.userForm = { ...res.data }
        }
      } catch (e) {
        this.$message.error('加载用户信息失败')
      }
    },
    async updateInfo() {
      try {
        const res = await api.put('/user/info', this.userForm)
        if (res.code === 200) {
          this.$message.success('更新成功')
          this.$store.dispatch('getUserInfo')
        }
      } catch (e) {
        this.$message.error('更新失败')
      }
    },
    async updatePassword() {
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.$message.warning('两次密码不一致')
        return
      }
      try {
        const res = await api.put('/user/password', {
          oldPassword: this.passwordForm.oldPassword,
          newPassword: this.passwordForm.newPassword
        })
        if (res.code === 200) {
          this.$message.success('密码修改成功')
          this.passwordForm = {
            oldPassword: '',
            newPassword: '',
            confirmPassword: ''
          }
        }
      } catch (e) {
        this.$message.error('密码修改失败')
      }
    },
    getPetEmoji(type) {
      return { dog: '🐶', cat: '🐱', bird: '🐦', fish: '🐠', other: '🐾' }[type] || '🐾'
    },
    async loadPetProfiles() {
      try {
        const res = await api.get('/pet-profile/list')
        if (res.code === 200) this.petProfiles = res.data || []
      } catch (e) {
        this.petProfiles = []
      }
    },
    openAddPet() {
      this.petDialogTitle = '添加宠物'
      this.petForm = { id: null, petType: '', breed: '', ageMonths: null, weightKg: null, notes: '', isPrimary: 0 }
      this.petDialogVisible = true
    },
    editPet(p) {
      this.petDialogTitle = '编辑宠物'
      this.petForm = { ...p }
      this.petDialogVisible = true
    },
    async savePet() {
      if (!this.petForm.petType) {
        this.$message.warning('请选择宠物类型')
        return
      }
      try {
        if (this.petForm.id) {
          await api.put('/pet-profile/' + this.petForm.id, this.petForm)
          this.$message.success('修改成功')
        } else {
          await api.post('/pet-profile', this.petForm)
          this.$message.success('添加成功')
        }
        this.petDialogVisible = false
        this.loadPetProfiles()
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async removePet(id) {
      try {
        await this.$confirm('确定要删除这个宠物档案吗？', '提示', { type: 'warning' })
        await api.delete('/pet-profile/' + id)
        this.$message.success('已删除')
        this.loadPetProfiles()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败')
      }
    }
  }
}
</script>

<style scoped>
.user {
  min-height: calc(100vh - 60px);
  background: #f5f5f5;
  padding: 20px 0;
}

.user h2 {
  margin-bottom: 20px;
}

.balance-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 20px 24px;
  border-radius: 12px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.balance-card .label {
  font-size: 14px;
  opacity: 0.9;
}

.balance-card .amount {
  font-size: 28px;
  font-weight: bold;
}

.recharge-box .tip {
  margin-bottom: 20px;
  font-size: 15px;
  color: #606266;
}

.recharge-box .unit {
  margin-left: 8px;
  color: #909399;
}

.empty-reviews {
  padding: 40px 0;
}

.review-list {
  max-width: 700px;
}

.review-item {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fafafa;
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.review-header .time {
  font-size: 12px;
  color: #909399;
}

.review-item .content {
  color: #606266;
  margin: 8px 0;
  line-height: 1.6;
}

.review-item .product {
  font-size: 12px;
  color: #909399;
}
/* 评价状态标签样式，和布局对齐 */
.status-tag {
  margin-left: 10px;
  vertical-align: middle;
}
.review-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px; /* 元素之间的间距，更美观 */
}
.time {
  color: #999;
  font-size: 12px;
}

.pet-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: #fafafa;
}

.pet-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.pet-card-header .pet-name {
  font-weight: bold;
  font-size: 15px;
}

.pet-card-body {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #606266;
}

.pet-card-body span {
  background: #fff;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>



