<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>营收统计</span>
            </div>
          </template>
          <el-form inline>
            <el-form-item label="开始">
              <el-date-picker v-model="revenueQuery.startDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="结束">
              <el-date-picker v-model="revenueQuery.endDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="粒度">
              <el-select v-model="revenueQuery.granularity" style="width: 100px">
                <el-option label="按日" value="day" />
                <el-option label="按月" value="month" />
                <el-option label="按年" value="year" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadRevenue">查询</el-button>
            </el-form-item>
          </el-form>

          <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
            <el-descriptions-item label="总营收">
              <b style="color: #f56c6c">¥{{ Number(revenue.totalAmount || 0).toFixed(2) }}</b>
            </el-descriptions-item>
            <el-descriptions-item label="订单笔数">{{ revenue.orderCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="现金">¥{{ Number(revenue.byPayType?.cash || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="微信">¥{{ Number(revenue.byPayType?.wechat || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="支付宝">¥{{ Number(revenue.byPayType?.alipay || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="月卡抵扣">¥{{ Number(revenue.byPayType?.card || 0).toFixed(2) }}</el-descriptions-item>
          </el-descriptions>

          <div ref="revenueChart" style="height: 320px"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>车流统计</span>
            </div>
          </template>
          <el-form inline>
            <el-form-item label="开始">
              <el-date-picker v-model="trafficQuery.startDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="结束">
              <el-date-picker v-model="trafficQuery.endDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadTraffic">查询</el-button>
            </el-form-item>
          </el-form>
          <div ref="trafficChart" style="height: 320px"></div>
        </el-card>

        <el-card shadow="never" style="margin-top: 16px">
          <template #header>
            <div class="card-header">
              <span>车位利用率</span>
            </div>
          </template>
          <el-progress
            type="dashboard"
            :percentage="Math.round((Number(occupancy.occupancyRate || 0)) * 100)"
            :color="occupancyRateColor"
          >
            <template #default="{ percentage }">
              <div style="text-align: center">
                <div style="font-size: 24px; font-weight: bold">{{ percentage }}%</div>
                <div style="font-size: 12px; color: #909399">占用率</div>
              </div>
            </template>
          </el-progress>
          <div style="text-align: center; margin-top: 8px">
            总车位 {{ occupancy.total || 0 }}，占用 {{ occupancy.occupied || 0 }}，空闲 {{ occupancy.free || 0 }}
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getRevenueReport, getTrafficReport, getOccupancyReport } from '@/api'

const revenue = ref({})
const occupancy = ref({})
const revenueChart = ref()
const trafficChart = ref()

const revenueQuery = reactive({ startDate: '', endDate: '', granularity: 'day' })
const trafficQuery = reactive({ startDate: '', endDate: '' })

const occupancyRateColor = computed(() => {
  const rate = Number(occupancy.value.occupancyRate || 0)
  if (rate > 0.8) return '#f56c6c'
  if (rate > 0.5) return '#e6a23c'
  return '#67c23a'
})

let echarts = null
let revenueInstance = null
let trafficInstance = null

const loadRevenue = async () => {
  revenue.value = await getRevenueReport(revenueQuery)
  renderRevenue()
}

const loadTraffic = async () => {
  const data = await getTrafficReport(trafficQuery)
  renderTraffic(data)
}

const loadOccupancy = async () => {
  occupancy.value = await getOccupancyReport()
}

const renderRevenue = () => {
  if (!revenueChart.value || !echarts) return
  const items = revenue.value.items || []
  revenueInstance = revenueInstance || echarts.init(revenueChart.value)
  revenueInstance.setOption({
    title: { text: '营收趋势', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: items.map((i) => i.date) },
    yAxis: { type: 'value' },
    series: [{
      name: '营收(元)',
      type: 'line',
      smooth: true,
      areaStyle: {},
      data: items.map((i) => i.amount)
    }]
  })
}

const renderTraffic = (data) => {
  if (!trafficChart.value || !echarts) return
  const dates = [...new Set([...Object.keys(data.inMap || {}), ...Object.keys(data.outMap || {})])].sort()
  trafficInstance = trafficInstance || echarts.init(trafficChart.value)
  trafficInstance.setOption({
    title: { text: '出入场车次', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['入场', '出场'], bottom: 0 },
    grid: { left: 50, right: 20, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '入场', type: 'bar', barWidth: 10, data: dates.map((d) => data.inMap[d] || 0) },
      { name: '出场', type: 'bar', barWidth: 10, data: dates.map((d) => data.outMap[d] || 0) }
    ]
  })
}

const handleResize = () => {
  revenueInstance?.resize()
  trafficInstance?.resize()
}

onMounted(async () => {
  // 动态引入 echarts 节省体积
  const mod = await import('echarts')
  echarts = mod.default || mod
  await Promise.all([loadRevenue(), loadTraffic(), loadOccupancy()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  revenueInstance?.dispose()
  trafficInstance?.dispose()
})
</script>

<style scoped>
.card-header {
  font-weight: 600;
}
</style>
