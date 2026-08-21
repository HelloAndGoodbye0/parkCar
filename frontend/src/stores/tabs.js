import { defineStore } from 'pinia'

/**
 * 多标签页（tabs）状态管理
 * visitedViews: 已打开的页面列表 { path, title, name, affix }
 * cachedViews: 需要 keep-alive 缓存的组件名列表（配合路由 name / 组件 defineOptions name）
 */
export const useTabsStore = defineStore('tabs', {
  state: () => ({
    visitedViews: []
  }),
  getters: {
    cachedViews: (state) => state.visitedViews.map((v) => v.name).filter(Boolean)
  },
  actions: {
    addView(view) {
      if (!view || !view.path) return
      if (this.visitedViews.some((v) => v.path === view.path)) return
      this.visitedViews.push({
        path: view.path,
        title: view.title || '',
        name: view.name || '',
        affix: !!view.affix
      })
    },
    delView(path) {
      const idx = this.visitedViews.findIndex((v) => v.path === path)
      if (idx > -1) this.visitedViews.splice(idx, 1)
    },
    closeOthers(path) {
      this.visitedViews = this.visitedViews.filter((v) => v.path === path || v.affix)
    },
    closeAll() {
      this.visitedViews = this.visitedViews.filter((v) => v.affix)
    }
  }
})
