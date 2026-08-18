import { defineStore } from 'pinia'
import { login, getMe } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('parkcar_token') || '',
    userInfo: JSON.parse(localStorage.getItem('parkcar_user') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => (state.userInfo?.roles || []).includes('ADMIN')
  },
  actions: {
    async login(username, password) {
      const data = await login({ username, password })
      this.token = data.token
      this.userInfo = data.user
      localStorage.setItem('parkcar_token', data.token)
      localStorage.setItem('parkcar_user', JSON.stringify(data.user))
      return data
    },
    async fetchMe() {
      try {
        const info = await getMe()
        this.userInfo = info
        localStorage.setItem('parkcar_user', JSON.stringify(info))
      } catch (e) {
        /* 忽略，跳转登录由拦截器处理 */
      }
    },
    clear() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('parkcar_token')
      localStorage.removeItem('parkcar_user')
    }
  }
})
