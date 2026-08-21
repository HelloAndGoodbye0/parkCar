<template>
  <el-row :gutter="16">
    <el-col :span="10">
      <el-card shadow="never">
        <template #header>
          <span class="card-title">车辆出场结算</span>
        </template>

        <el-form label-width="90px">
          <el-form-item label="车牌号">
            <el-input
              v-model="plateNo"
              placeholder="输入车牌查询在场车辆"
              size="large"
              @keyup.enter="onPreview"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="previewLoading" @click="onPreview">查询 / 试算</el-button>
          </el-form-item>
        </el-form>

        <el-descriptions v-if="preview" :column="1" border size="small">
          <el-descriptions-item label="车牌">{{ preview.plateNo }}</el-descriptions-item>
          <el-descriptions-item label="车位">{{ preview.spaceNo }}</el-descriptions-item>
          <el-descriptions-item label="入场时间">{{ preview.inTime }}</el-descriptions-item>
          <el-descriptions-item label="停车时长">
            {{ formatDuration(preview.durationMinutes) }}
          </el-descriptions-item>
          <el-descriptions-item label="车辆类型">
            <el-tag v-if="preview.memberFree" type="success" size="small">月卡免费</el-tag>
            <el-tag v-else-if="preview.isMember === 1" type="warning" size="small">月卡已过期</el-tag>
            <el-tag v-else type="info" size="small">临时车</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="计费规则">{{ preview.rule?.name }}</el-descriptions-item>
          <el-descriptions-item label="应收金额">
            <span style="color: #f56c6c; font-size: 20px; font-weight: bold">¥{{ preview.payableAmount }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="preview && preview.feeItems && preview.feeItems.length">
          <el-divider content-position="left">收费明细</el-divider>
          <el-table :data="preview.feeItems" size="small" border>
            <el-table-column label="项目" width="76" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="feeTypeTag(row.type)">{{ feeTypeText(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时段" min-width="150">
              <template #default="{ row }">{{ row.period || '-' }}</template>
            </el-table-column>
            <el-table-column prop="desc" label="说明" min-width="150" />
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">
                <span :style="{ color: row.amount > 0 ? '#f56c6c' : '#909399' }">¥{{ row.amount }}</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-card>
    </el-col>

    <el-col :span="14">
      <el-card shadow="never" v-if="preview">
        <template #header>
          <span class="card-title">收款确认</span>
        </template>
        <el-form label-width="90px" style="max-width: 420px">
          <el-form-item label="应收金额">
            <span style="color: #f56c6c; font-size: 18px; font-weight: bold">¥{{ preview.amount }}</span>
          </el-form-item>
          <el-form-item label="减免金额">
            <el-input-number
              v-model="discount"
              :min="0"
              :max="preview.amount"
              :precision="2"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="实收金额">
            <span style="color: #409eff; font-size: 18px; font-weight: bold">
              ¥{{ Math.max(0, preview.amount - discount).toFixed(2) }}
            </span>
          </el-form-item>
          <el-form-item label="支付方式" v-if="!preview.memberFree">
            <el-radio-group v-model="payType">
              <el-radio-button :value="1">现金</el-radio-button>
              <el-radio-button :value="2">微信</el-radio-button>
              <el-radio-button :value="3">支付宝</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="settleLoading" @click="onSettle">
              {{ preview.memberFree ? '月卡免费放行' : '确认收款并放行' }}
            </el-button>
            <el-button @click="reset">重新查询</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          v-if="result"
          type="success"
          :closable="false"
          style="margin-top: 12px"
        >
          <template #title>
            车牌 {{ result.plateNo }} 已放行，实收 <b>¥{{ result.paidAmount }}</b>，
            车位 {{ result.spaceNo }} 已释放，订单号 {{ result.orderNo }}
          </template>
        </el-alert>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
defineOptions({ name: 'ParkingOut' })
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { outPreview, outSettle } from '@/api'

const route = useRoute()
const plateNo = ref('')
const preview = ref(null)
const discount = ref(0)
const payType = ref(1)
const result = ref(null)
const previewLoading = ref(false)
const settleLoading = ref(false)

const formatDuration = (minutes) => {
  if (minutes == null) return '-'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

const feeTypeText = (t) => ({ DAY: '白天', NIGHT: '夜间', ONCE: '按次', FREE: '免费' }[t] || t || '-')
const feeTypeTag = (t) => ({ DAY: '', NIGHT: 'warning', ONCE: 'primary', FREE: 'success' }[t] || 'info')

const onPreview = async () => {
  if (!plateNo.value.trim()) {
    ElMessage.warning('请输入车牌号')
    return
  }
  previewLoading.value = true
  try {
    preview.value = await outPreview({ plateNo: plateNo.value.trim() })
    discount.value = 0
    result.value = null
  } finally {
    previewLoading.value = false
  }
}

const onSettle = async () => {
  if (preview.value.memberFree) {
    discount.value = 0
  }
  settleLoading.value = true
  try {
    result.value = await outSettle({
      recordId: preview.value.recordId,
      payType: preview.value.memberFree ? null : payType.value,
      discount: discount.value
    })
    ElMessage.success('出场结算成功')
    preview.value = null
  } finally {
    settleLoading.value = false
  }
}

const reset = () => {
  preview.value = null
  result.value = null
  discount.value = 0
  plateNo.value = ''
}

onMounted(() => {
  if (route.query.plateNo) {
    plateNo.value = route.query.plateNo
    onPreview()
  }
})
</script>

<style scoped>
.card-title {
  font-weight: 600;
}
</style>
