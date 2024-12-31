import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'

class WebSocketService {
    constructor() {
        this.stompClient = null
        this.connected = false
        this.subscribers = new Map()
    }

    connect() {
        return new Promise((resolve, reject) => {
            if (this.connected) {
                resolve();
                return;
            }

            const sockJS = new SockJS(process.env.VUE_APP_WS_URL);
            this.stompClient = Stomp.over(sockJS);

            this.stompClient.connect({}, 
                () => {
                    this.connected = true
                    console.log('WebSocket 연결됨')
                    this.subscribeToNotifications()
                    resolve()
                },
                error => {
                    console.error('WebSocket 연결 실패:', error)
                    reject(error)
                }
            )
        })
    }

    subscribeToNotifications() {
        if (!this.connected) return

        // 제품 상태 변경 구독
        this.subscribe('/topic/product-status', message => {
            const data = JSON.parse(message.body)
            this.notifySubscribers('product-status', data)
        })

        // 스캔 이력 구독
        this.subscribe('/topic/scan-history', message => {
            const data = JSON.parse(message.body)
            this.notifySubscribers('scan-history', data)
        })

        // 의심스러운 스캔 알림 구독
        this.subscribe('/topic/suspicious-scan', message => {
            const data = JSON.parse(message.body)
            this.notifySubscribers('suspicious-scan', data)
        })
    }

    subscribe(topic, callback) {
        if (!this.connected) return null
        return this.stompClient.subscribe(topic, callback)
    }

    addSubscriber(type, callback) {
        if (!this.subscribers.has(type)) {
            this.subscribers.set(type, new Set())
        }
        this.subscribers.get(type).add(callback)
    }

    removeSubscriber(type, callback) {
        if (this.subscribers.has(type)) {
            this.subscribers.get(type).delete(callback)
        }
    }

    notifySubscribers(type, data) {
        if (this.subscribers.has(type)) {
            this.subscribers.get(type).forEach(callback => callback(data))
        }
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect()
            this.connected = false
        }
    }
}

export default new WebSocketService() 