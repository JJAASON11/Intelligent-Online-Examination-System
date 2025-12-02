import { mount } from '@vue/test-utils'
import Home from './Home.vue'

test('renders welcome text', () => {
  const wrapper = mount(Home)
  expect(wrapper.text()).toContain('欢迎使用智能在线考试系统')
})

