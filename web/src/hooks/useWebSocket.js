import { ref } from 'vue'

export function useWebSocket(path) {
  const isConnected = ref(false)
  const messages = ref([])
  let ws

  const connect = () => {
    const url = (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + path
    ws = new WebSocket(url)
    ws.onopen = () => { isConnected.value = true }
    ws.onmessage = (e) => { messages.value.push(e.data) }
    ws.onclose = () => { isConnected.value = false }
  }

  const sendMessage = (text) => { if (ws && isConnected.value) ws.send(text) }

  return { isConnected, messages, connect, sendMessage }
}

