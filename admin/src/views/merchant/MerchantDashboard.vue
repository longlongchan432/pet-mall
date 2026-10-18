<template>
  <div style="padding: 20px;">
    <h3 style="margin-bottom: 20px;">商家数据看板</h3>
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">累计销售额</div>
          <div class="stat-value">¥{{ (stats.totalSales || 0).toFixed(2) }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">订单总数</div>
          <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">在售商品</div>
          <div class="stat-value">{{ stats.totalProducts || 0 }}</div>
        </div>
      </el-col>
    </el-row>
    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-top: 20px;">
      <el-col :span="14">
        <div class="chart-card">
          <div class="chart-title">近7天销售趋势</div>
          <div ref="trendChart" style="height: 320px;"></div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="chart-card">
          <div class="chart-title">热销商品 TOP10</div>
          <div ref="topChart" style="height: 320px;"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import api from '../../api'
import * as echarts from 'echarts'

export default {
  name: 'MerchantDashboard',
  data() {
    return {
      stats: {},
      trend: [],
      top: [],
      trendChart: null,
      topChart: null
    }
  },
  async mounted() {
    await this.loadStats()
    this.$nextTick(() => {
      this.renderTrend()
      this.renderTop()
    })
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.trendChart) { this.trendChart.dispose(); this.trendChart = null }
    if (this.topChart) { this.topChart.dispose(); this.topChart = null }
  },
  methods: {
    handleResize() {
      if (this.trendChart) this.trendChart.resize()
      if (this.topChart) this.topChart.resize()
    },
    async loadStats() {
      try {
        const [s, t, p] = await Promise.all([
          api.get('/stats/dashboard'),
          api.get('/stats/merchant/trend', { params: { days: 7 } }),
          api.get('/stats/merchant/top-products')
        ])
        this.stats = s.data || {}
        this.trend = t.data || []
        this.top = p.data || []
      } catch (e) {
        console.error('loadStats', e)
      }
    },
    renderTrend() {
      const el = this.$refs.trendChart
      if (!el) return
      // build full 7-day axis
      const days = []
      const now = new Date()
      for (let i = 6; i >= 0; i--) {
        const d = new Date(now)
        d.setDate(d.getDate() - i)
        days.push(d.toISOString().slice(0, 10))
      }
      const trendMap = {}
      this.trend.forEach(r => { trendMap[r.d] = r })
      const salesData = days.map(d => trendMap[d] ? Number(trendMap[d].amt) : 0)
      const cntData = days.map(d => trendMap[d] ? Number(trendMap[d].cnt) : 0)

      this.trendChart = echarts.init(el)
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['销售额', '订单量'] },
        xAxis: { type: 'category', data: days.map(d => d.slice(5)) },
        yAxis: [
          { type: 'value', name: '销售额(¥)', axisLabel: { formatter: '¥{value}' } },
          { type: 'value', name: '订单量', splitLine: { show: false } }
        ],
        series: [
          { name: '销售额', type: 'line', smooth: true, data: salesData, itemStyle: { color: '#409EFF' } },
          { name: '订单量', type: 'bar', yAxisIndex: 1, data: cntData, itemStyle: { color: '#67C23A' } }
        ]
      })
    },
    renderTop() {
      const el = this.$refs.topChart
      if (!el) return
      const items = this.top.slice(0, 10).reverse()
      this.topChart = echarts.init(el)
      this.topChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 100, right: 20, top: 10, bottom: 20 },
        xAxis: { type: 'value', name: '销量' },
        yAxis: {
          type: 'category',
          data: items.map(p => p.name.length > 8 ? p.name.slice(0, 8) + '…' : p.name)
        },
        series: [{
          type: 'bar',
          data: items.map(p => p.sales || 0),
          itemStyle: { color: '#E6A23C' },
          label: { show: true, position: 'right' }
        }]
      })
    }
  }
}
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.stat-label { color: #909399; font-size: 14px; margin-bottom: 8px; }
.stat-value { color: #303133; font-size: 28px; font-weight: bold; }
.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.chart-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; color: #303133; }
</style>