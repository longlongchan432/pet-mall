import Vue from 'vue'
import Vuex from 'vuex'
import createPersistedState from 'vuex-persistedstate'
import api from '../api'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    user: {
      token: null,
      info: null
    }
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.user.token = token
    },
    SET_USER_INFO(state, userInfo) {
      state.user.info = userInfo
    },
    CLEAR_USER(state) {
      state.user.token = null
      state.user.info = null
    }
  },
  actions: {
    async login({ commit }, { username, password }) {
      const res = await api.post('/user/login', { username, password })
      if (res.code === 200) {
        commit('SET_TOKEN', res.data.token)
        commit('SET_USER_INFO', res.data.user)
        return true
      }
      return false
    },
    async getUserInfo({ commit }) {
      const res = await api.get('/user/info')
      if (res.code === 200) {
        commit('SET_USER_INFO', res.data)
      }
    },
    logout({ commit }) {
      commit('CLEAR_USER')
    }
  },
  plugins: [createPersistedState()]
})



