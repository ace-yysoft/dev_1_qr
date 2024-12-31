<template>
  <div class="home">
    <div class="dashboard-header">
      <h1>제품 관리</h1>
      <div class="header-actions">
        <button class="btn btn-secondary" @click="refreshProducts">
          <i class="fas fa-sync-alt"></i>
          새로고침
        </button>
      </div>
    </div>

    <!-- 제품 목록 -->
    <div class="products-grid">
      <div v-for="product in products" 
           :key="product.id" 
           class="card product-card"
           :data-product-code="product.code"
           :class="product.status.toLowerCase()">
        <div class="card-body">
          <div class="product-info">
            <div class="product-header">
              <h3>{{ product.name }}</h3>
              <span class="status-badge" :class="getStatusClass(product.status)">
                {{ getStatusText(product.status) }}
              </span>
            </div>
            <div class="product-details">
              <p><i class="fas fa-barcode"></i> {{ product.code }}</p>
              <p>
                <i class="fas fa-sync-alt"></i> 
                스캔 횟수: {{ product.currentScanCount }}/{{ product.maxScanCount }}
              </p>
              <div class="product-actions">
                <button class="btn btn-secondary btn-sm" @click="toggleDetails(product)">
                  <i class="fas fa-info-circle"></i> 상세정보
                </button>
                <button class="btn btn-secondary btn-sm" @click="toggleScanHistory(product)">
                  <i class="fas fa-history"></i> 스캔이력
                </button>
              </div>
            </div>
          </div>
          
          <div class="qr-section">
            <div class="qr-container">
              <img :src="'data:image/png;base64,' + product.qrCode" 
                   :alt="product.code"
                   @click="showQRDetail(product)">
              <button class="btn btn-secondary btn-sm" @click="downloadQR(product)">
                <i class="fas fa-download"></i> QR 다운로드
              </button>
            </div>
          </div>
        </div>
        
        <!-- 상세 정보 패널 -->
        <div v-if="selectedProduct === product" class="details-panel">
          <div class="details-content">
            <h4>제조 정보</h4>
            <p><strong>위치:</strong> {{ product.manufacturingLocation }}</p>
            <p><strong>시간:</strong> {{ formatDate(product.manufacturingTime) }}</p>
            
            <h4>유통 정보</h4>
            <p><strong>예상 위치:</strong> {{ product.expectedDeliveryLocation }}</p>
            <p><strong>예상 시간:</strong> {{ formatDate(product.expectedDeliveryTime) }}</p>
          </div>
        </div>
        
        <!-- 스캔 이력 패널 -->
        <div v-if="scanHistories[product.code]" class="scan-history-panel">
          <div class="scan-list">
            <div v-for="scan in scanHistories[product.code]" 
                 :key="scan.id" 
                 class="scan-item">
              <div class="scan-time">
                <i class="far fa-clock"></i>
                {{ formatDate(scan.scanTime) }}
              </div>
              <div class="scan-status" :class="scan.valid ? 'valid' : 'invalid'">
                {{ scan.validationMessage }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- QR 코드 상세 모달 -->
    <div class="modal" v-if="selectedQR">
      <div class="modal-content card">
        <div class="card-header">
          <h2>QR 코드 상세</h2>
          <button class="btn-close" @click="selectedQR = null">×</button>
        </div>
        <div class="card-body qr-detail">
          <img :src="'data:image/png;base64,' + selectedQR.qrCode" 
               :alt="selectedQR.code">
          <div class="qr-info">
            <p><strong>제품명:</strong> {{ selectedQR.name }}</p>
            <p><strong>제품코드:</strong> {{ selectedQR.code }}</p>
            <p><strong>QR 내용:</strong> {{ selectedQR.qrCodeText }}</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="notifications.length > 0" class="notifications">
      <div v-for="(notification, index) in notifications" 
           :key="index"
           class="notification"
           :class="notification.type">
        <p class="notification-message">{{ notification.message }}</p>
        <span class="notification-time">
          {{ formatDate(notification.timestamp) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import webSocket from '@/socket'

export default {
  name: 'HomeView',
  data() {
    return {
      products: [],
      newProduct: {
        name: '',
        code: '',
        manufacturingLocation: '',
        manufacturingTime: '',
        expectedDeliveryLocation: '',
        expectedDeliveryTime: '',
        status: 'CREATED',
        maxScanCount: 1
      },
      scanHistories: {},
      showNewProductForm: false,
      selectedProduct: null,
      selectedQR: null,
      notifications: []
    }
  },
  async created() {
    await this.loadProducts()
    this.connectWebSocket()
  },
  beforeUnmount() {
    webSocket.disconnect()
  },
  methods: {
    async connectWebSocket() {
      try {
        await webSocket.connect()
        
        // 제품 상태 변경 구독
        webSocket.addSubscriber('product-status', data => {
          const product = this.products.find(p => p.code === data.productCode)
          if (product) {
            product.status = data.status
          }
        })
        
        // 의심스러운 스캔 알림 구독
        webSocket.addSubscriber('suspicious-scan', data => {
          this.notifications.unshift({
            type: 'warning',
            message: `의심스러운 스캔 발생: ${data.productCode} - ${data.reason}`,
            timestamp: new Date(data.timestamp)
          })
        })
      } catch (error) {
        console.error('WebSocket 연결 실패:', error)
      }
    },
    async loadProducts() {
      try {
        const response = await axios.get('http://localhost:8090/api/products')
        this.products = response.data
      } catch (error) {
        console.error('제품 목록 로딩 실패:', error)
      }
    },
    async createProduct() {
      try {
        if (!this.validateProductForm()) {
          return;
        }

        const response = await axios.post('http://localhost:8090/api/products', this.newProduct);
        console.log('생성된 제품:', response.data);
        this.showNewProductForm = false;

        this.products.unshift(response.data);
        
        this.$nextTick(() => {
          const newProductElement = document.querySelector(`[data-product-code="${response.data.code}"]`);
          if (newProductElement) {
            newProductElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
            newProductElement.classList.add('highlight');
            setTimeout(() => {
              newProductElement.classList.remove('highlight');
            }, 2000);
          }
        });

        this.newProduct = {
          name: '',
          code: '',
          manufacturingLocation: '',
          manufacturingTime: '',
          expectedDeliveryLocation: '',
          expectedDeliveryTime: '',
          status: 'CREATED',
          maxScanCount: 1
        };
      } catch (error) {
        console.error('제품 등록 실패:', error.response?.data || error.message);
        alert('제품 등록 실패: ' + (error.response?.data?.message || '서버 오류가 발생했습니다.'));
      }
    },
    validateProductForm() {
      if (!this.newProduct.name.trim()) {
        alert('제품명을 입력해주세요.');
        return false;
      }
      if (!this.newProduct.code.trim()) {
        alert('제품코드를 입력해주세요.');
        return false;
      }
      if (!this.newProduct.manufacturingLocation.trim()) {
        alert('제조 위치를 입력해주세요.');
        return false;
      }
      if (!this.newProduct.manufacturingTime) {
        alert('제조 시간을 입력해주세요.');
        return false;
      }
      return true;
    },
    async loadScanHistory(productCode) {
      try {
        console.log('스캔 이력 요청:', productCode);
        const response = await axios.get(`http://localhost:8090/api/products/${productCode}/scan-history`);
        
        // 응답 데이터 상세 로깅
        console.log('스캔 이력 응답 상세:', JSON.stringify(response.data.map(item => ({
            id: item.id,
            productCode: item.productCode,
            scanTime: new Date(item.scanTime).toLocaleString(),
            valid: item.valid,
            timeValid: item.timeValid,
            hashValid: item.hashValid,
            message: item.validationMessage
        })), null, 2));
        
        if (Array.isArray(response.data)) {
            this.scanHistories[productCode] = response.data;
            this.scanHistories = { ...this.scanHistories };
        } else {
            console.error('잘못된 응답 형식:', response.data);
        }
      } catch (error) {
        console.error('스캔 이력 로딩 실패:', error.response?.data || error.message);
        alert('스캔 이력을 불러오는데 실패했습니다.');
      }
    },
    formatDate(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleString()
    },
    getStatusText(status) {
      const statusMap = {
        'CREATED': '생성됨',
        'IN_TRANSIT': '유통중',
        'READY_FOR_SALE': '판매준비',
        'ACTIVATED': '정품인증완료',
        'SUSPICIOUS': '의심스러운 제품'
      };
      return statusMap[status] || status;
    },
    getStatusClass(status) {
      const statusMap = {
        'CREATED': 'created',
        'IN_TRANSIT': 'in_transit',
        'READY_FOR_SALE': 'ready_for_sale',
        'ACTIVATED': 'activated',
        'SUSPICIOUS': 'suspicious'
      };
      return statusMap[status] || 'error';
    },
    toggleDetails(product) {
      this.selectedProduct = this.selectedProduct === product ? null : product;
    },
    async toggleScanHistory(product) {
      if (this.scanHistories[product.code]) {
        this.scanHistories = Object.fromEntries(
          Object.entries(this.scanHistories).filter(([key]) => key !== product.code)
        );
      } else {
        await this.loadScanHistory(product.code);
      }
    },
    async refreshProducts() {
      await this.loadProducts()
    },
    downloadQR(product) {
      const link = document.createElement('a');
      link.href = `data:image/png;base64,${product.qrCode}`;
      link.download = `${product.code}_qr.png`;
      link.click();
    },
    showQRDetail(product) {
      this.selectedQR = product;
    }
  }
}
</script>

<style scoped>
@keyframes highlight {
  0% { transform: scale(1); }
  50% { transform: scale(1.02); }
  100% { transform: scale(1); }
}

.product-card.highlight {
  animation: highlight 1s ease;
  box-shadow: 0 0 0 3px var(--primary-color);
}

.product-card {
  transition: all 0.3s ease;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(26, 35, 126, 0.15);
}

.card-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: white;
}

.card-body {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 20px;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-section p {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--primary-color);
}

.qr-section img {
  width: 120px;
  height: 120px;
  border: 2px solid var(--primary-color);
  padding: 5px;
  border-radius: 8px;
}

.card-footer {
  padding: 15px 20px;
  background: var(--background-color);
  display: flex;
  gap: 10px;
}

.details-panel, .scan-history-panel {
  padding: var(--spacing-unit);
  border-top: 1px solid #e0e0e0;
  background: var(--background-color);
}

.details-content {
  display: grid;
  gap: 15px;
}

.details-content h4 {
  color: var(--primary-color);
  margin: 0 0 10px 0;
}

.details-content p {
  margin: 0;
  color: var(--secondary-color);
}

.scan-list {
  display: grid;
  gap: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.scan-item {
  background: white;
  padding: 12px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--card-shadow);
}

/* 모달 스타일 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(26, 35, 126, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* 폼 스타일 */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: var(--spacing-unit);
}

.form-group label {
  font-weight: 500;
  color: var(--primary-color);
}

/* 상태 뱃지 색상 */
.status-badge.created { background: #E3F2FD; color: #1976D2; }
.status-badge.in_transit { background: #E8F5E9; color: #388E3C; }
.status-badge.ready_for_sale { background: #FFF3E0; color: #F57C00; }
.status-badge.activated { background: #E8F5E9; color: #388E3C; }
.status-badge.suspicious { background: #FFEBEE; color: #D32F2F; }

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-unit);
}

.btn-sm {
  height: 32px;
  padding: 0 12px;
  font-size: 0.875rem;
}

.qr-section img {
  cursor: pointer;
  transition: transform 0.3s ease;
}

.qr-section img:hover {
  transform: scale(1.05);
}

.qr-detail {
  display: flex;
  gap: var(--spacing-unit);
  align-items: flex-start;
}

.qr-detail img {
  width: 200px;
  height: 200px;
  border: 2px solid var(--primary-color);
  padding: 10px;
  border-radius: 8px;
}

.qr-info {
  flex: 1;
}

.qr-info p {
  margin: 0 0 10px 0;
}

.notifications {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  max-width: 400px;
}

.notification {
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: white;
  box-shadow: var(--card-shadow);
  animation: slideIn 0.3s ease;
}

.notification.warning {
  border-left: 4px solid var(--warning-color);
}

.notification-message {
  margin: 0 0 5px 0;
  color: var(--primary-color);
}

.notification-time {
  font-size: 0.8rem;
  color: var(--secondary-color);
}

@keyframes slideIn {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(600px, 1fr));
  gap: var(--spacing-unit);
  padding: var(--spacing-unit);
}

.product-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.card-body {
  display: flex;
  padding: var(--spacing-unit);
  gap: var(--spacing-unit);
}

.product-info {
  flex: 1;
}

.product-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.product-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: var(--primary-color);
}

.product-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.qr-section {
  width: 200px;
  border-left: 1px solid #e0e0e0;
  padding-left: var(--spacing-unit);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.qr-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  background: white;
  padding: 10px;
  border-radius: 8px;
  box-shadow: var(--card-shadow);
}

.qr-section img {
  width: 150px;
  height: 150px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.qr-section img:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.modal-footer {
  padding: var(--spacing-unit);
  border-top: 1px solid #e0e0e0;
}

.button-container {
  display: flex;
  justify-content: center;
  gap: var(--spacing-unit);
}

.modal-footer .btn {
  min-width: 120px;
}
</style> 