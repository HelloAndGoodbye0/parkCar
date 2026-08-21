<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon><Van /></el-icon>
        <span>停车场管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="'/' + item.path"
        >
          <el-icon><component :is="item.meta.icon" /></el-icon>
          <span>{{ item.meta.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <el-dropdown @command="onCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
              <el-tag size="small" style="margin-left: 6px">
                {{ userStore.isAdmin ? '管理员' : '收费员' }}
              </el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <div class="tabs-bar">
        <div class="tabs-scroll">
          <div
            v-for="tab in tabsStore.visitedViews"
            :key="tab.path"
            class="tab-item"
            :class="{ active: route.path === tab.path }"
            @click="go(tab.path)"
          >
            <span>{{ tab.title }}</span>
            <el-icon v-if="!tab.affix" class="tab-close" @click.stop="close(tab)">
              <Close />
            </el-icon>
          </div>
        </div>
        <el-dropdown trigger="click" @command="onTabsCommand">
          <span class="tabs-action">
            <el-icon><MoreFilled /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="closeOthers">关闭其他</el-dropdown-item>
              <el-dropdown-item command="closeAll">关闭全部</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <keep-alive :include="tabsStore.cachedViews">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tabsStore = useTabsStore()

const allMenus = router.options.routes
  .find((r) => r.path === '/')
  ?.children.filter((r) => r.meta?.title) || []

const menuItems = computed(() =>
  allMenus.filter((r) => !r.meta?.adminOnly || userStore.isAdmin)
)

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => route.meta?.title || '工作台')

// 路由变化时登记 tab；工作台固定保留（不可关闭）
watch(
  () => route.path,
  () => {
    if (route.meta?.title) {
      tabsStore.addView({
        path: route.path,
        title: route.meta.title,
        name: route.name || '',
        affix: route.path === '/dashboard'
      })
    }
  },
  { immediate: true }
)

const go = (path) => {
  if (path !== route.path) router.push(path)
}

const close = (tab) => {
  tabsStore.delView(tab.path)
  if (tab.path === route.path) {
    const views = tabsStore.visitedViews
    const last = views[views.length - 1]
    router.push(last ? last.path : '/dashboard')
  }
}

const onTabsCommand = (cmd) => {
  if (cmd === 'closeOthers') {
    tabsStore.closeOthers(route.path)
  } else if (cmd === 'closeAll') {
    tabsStore.closeAll()
    router.push('/dashboard')
  }
}

const onCommand = (cmd) => {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        userStore.clear()
        router.push('/login')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background-color: #1f2d3d;
}

.aside .el-menu {
  border-right: none;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  background-color: #172433;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 4px;
  color: #333;
}

.tabs-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  height: 40px;
  padding: 0 8px;
  flex-shrink: 0;
}

.tabs-scroll {
  flex: 1;
  display: flex;
  align-items: center;
  overflow-x: auto;
  scrollbar-width: none;
}

.tabs-scroll::-webkit-scrollbar {
  display: none;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 12px;
  height: 28px;
  margin: 0 6px 0 0;
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.tab-item:hover {
  color: #409eff;
}

.tab-item.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.tab-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 1px;
}

.tab-close:hover {
  background: rgba(0, 0, 0, 0.15);
}

.tabs-action {
  display: flex;
  align-items: center;
  padding: 0 6px;
  cursor: pointer;
  color: #606266;
}

.tabs-action:hover {
  color: #409eff;
}

.main {
  background: #f0f2f5;
}
</style>
