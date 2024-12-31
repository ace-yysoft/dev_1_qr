<template>
  <div class="validate">
    <div class="page-header">
      <h1>QR 코드 검증</h1>
    </div>
      
    <div class="validate-form card">
      <div class="card-body">
        <div class="search-form">
          <input class="form-input" 
                  v-model="qrContent" 
                  placeholder="QR 코드 내용을 입력하세요"
                  @keyup.enter="validateQR"
                  autocomplete="off"
                  ref="qrInput">
          <button class="btn btn-primary" @click="validateQR">
            <i class="fas fa-search"></i>
            검증
          </button>
        </div>
      </div>
    </div>

    <div class="results-container" v-if="validationResult">
      <div class="card validation-result">
        <div class="card-header">
          <h2>검증 결과</h2>
          <div class="header-actions">
            <button class="btn btn-secondary" @click="copyQRContent">
              <i class="fas fa-copy"></i>
              복사
            </button>
            <span :class="['status-badge', validationResult.valid ? 'success' : 'error']">
              {{ validationResult.valid ? '정상' : '비정상' }}
            </span>
          </div>
        </div>
        <div class="card-body" v-if="validationResult.productCode">
          <p class="validation-message" :class="{ 'error': !validationResult.valid }">
            {{ validationResult.message }}
          </p>
          <div class="info-grid">
            <div class="info-item">
              <label>제품 코드</label>
              <span>{{ validationResult.productCode }}</span>
            </div>
            <div class="info-item">
              <label>제품 상태</label>
              <span>{{ getStatusText(validationResult.status) }}</span>
            </div>
            <div class="info-item">
              <label>스캔 횟수</label>
              <span>{{ validationResult.currentScanCount }} / {{ validationResult.maxScanCount }}</span>
            </div>
            <div class="info-item" v-if="validationResult.manufacturingLocation">
              <label>제조 위치</label>
              <span>{{ validationResult.manufacturingLocation }}</span>
            </div>
            <div class="info-item" v-if="validationResult.manufacturingTime">
              <label>제조 시간</label>
              <span>{{ formatDate(validationResult.manufacturingTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="validationResult && validationResult.productCode && scanHistory.length > 0" 
           class="card scan-history">
        <div class="card-header">
          <h2>스캔 이력</h2>
          <div class="header-actions">
            <button class="btn btn-secondary" @click="exportScanHistory">
              <i class="fas fa-download"></i>
              내보내기
            </button>
            <span class="scan-count">
              총 {{ scanHistory.length }}회
            </span>
          </div>
        </div>
        <div class="card-body">
          <div class="scan-list">
            <div v-for="scan in scanHistory" 
                 :key="scan.id" 
                 class="scan-item"
                 :class="getValidationStatusClass(scan)">
              <div class="scan-header">
                <span class="scan-time">{{ formatDate(scan.scanTime) }}</span>
                <span :class="['scan-status', getValidationStatusClass(scan)]">
                  <i class="status-icon"></i>
                  {{ getValidationStatus(scan) }}
                </span>
              </div>
              <div class="scan-details">
                <div class="validation-info">
                  <span :class="{ valid: scan.timeValid }">
                    <i class="icon">⏱</i> {{ scan.timeValid ? '시간 유효' : '시간 만료' }}
                  </span>
                  <span :class="{ valid: scan.hashValid }">
                    <i class="icon">🔒</i> {{ scan.hashValid ? '해시 유효' : '해시 위조' }}
                  </span>
                </div>
                <p class="scan-message">{{ scan.validationMessage }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.results-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-unit);
  margin-top: var(--spacing-unit);
}

.validation-result,
.scan-history {
  height: fit-content;
}

@media (max-width: 1200px) {
  .results-container {
    grid-template-columns: 1fr;
  }
}

.validate {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  font-size: 2rem;
  color: var(--primary-color);
}

.validate-form {
  margin-bottom: 30px;
}

.search-form {
  display: flex;
  gap: 12px;
}

.search-form .form-input {
  flex: 1;
  height: 48px;
  font-size: 1rem;
  padding: 0 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
}

.search-form .btn {
  height: 48px;
  padding: 0 24px;
  font-size: 1rem;
  min-width: 120px;
}

.validation-message {
  font-size: 1.1rem;
  margin-bottom: 20px;
  padding: 10px;
  border-radius: 6px;
  background: #E8F5E9;
  color: var(--success-color);
}

.validation-message.error {
  background: #FFEBEE;
  color: var(--error-color);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-unit);
  background: var(--background-color);
  padding: var(--spacing-unit);
  border-radius: 8px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  background: white;
  padding: var(--spacing-unit);
  border-radius: 6px;
  box-shadow: var(--card-shadow);
}

.info-item label {
  font-weight: 500;
  color: var(--secondary-color);
  font-size: 0.9rem;
}

.info-item span {
  font-size: 1.1rem;
  color: var(--primary-color);
}

.scan-count {
  font-size: 0.9rem;
  color: var(--secondary-color);
  background: var(--background-color);
  padding: 4px 12px;
  border-radius: 20px;
}

.scan-list {
  display: grid;
  gap: 15px;
  max-height: 500px;
  overflow-y: auto;
  padding-right: 10px;
}

.scan-item {
  background: white;
  padding: 15px;
  border-radius: 8px;
  box-shadow: var(--card-shadow);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scan-time {
  color: var(--secondary-color);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 5px;
}

.validation-info {
  display: flex;
  gap: 20px;
  margin: 10px 0;
  flex-wrap: wrap;
}

.validation-info span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.validation-info .icon {
  font-size: 1.2rem;
}

/* 스크롤바 스타일링 */
.scan-list::-webkit-scrollbar {
  width: 8px;
}

.scan-list::-webkit-scrollbar-track {
  background: var(--background-color);
  border-radius: 4px;
}

.scan-list::-webkit-scrollbar-thumb {
  background: #bdbdbd;
  border-radius: 4px;
}

.scan-list::-webkit-scrollbar-thumb:hover {
  background: #9e9e9e;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-unit);
}

.btn {
  height: 36px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 0.9rem;
}

.btn i {
  font-size: 1rem;
}

.scan-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;
}

.status-normal {
  background: #E8F5E9;
  color: var(--success-color);
}

.status-warning {
  background: #FFF3E0;
  color: var(--warning-color);
}

.status-error {
  background: #FFEBEE;
  color: var(--error-color);
}

.card-body {
  padding: var(--spacing-unit);
}

.card {
  width: 100%;
  max-width: 800px;
}
</style>

<script>
import axios from 'axios'
import webSocket from '@/socket'

export default {
  name: 'ValidateView',
  data() {
    return {
      qrContent: '',
      validationResult: null,
      scanHistory: [],
      isValidating: false,
      lastScanTime: null
    }
  },
  mounted() {
    this.$refs.qrInput?.focus();
  },
  async created() {
    const urlParams = new URLSearchParams(window.location.search)
    const content = urlParams.get('content')
    if (content) {
      this.qrContent = content
      await this.validateQR()
    }
    this.connectWebSocket()
  },
  beforeUnmount() {
    webSocket.disconnect()
  },
  methods: {
    async connectWebSocket() {
      try {
        await webSocket.connect()
        
        // 스캔 이력 구독
        webSocket.addSubscriber('scan-history', data => {
          if (this.validationResult?.productCode === data.productCode) {
            this.scanHistory.unshift(data)
          }
        })
      } catch (error) {
        console.error('WebSocket 연결 실패:', error)
      }
    },
    async validateQR() {
      try {
        if (this.isValidating) {
          console.log('이미 검증 중입니다.');
          return;
        }
        
        const now = Date.now();
        if (this.lastScanTime && now - this.lastScanTime < 1000) {
          console.log('너무 빠른 스캔입니다.');
          return;
        }
        
        this.isValidating = true;
        this.lastScanTime = now;
        
        if (!this.qrContent) {
          alert('QR 코드 내용을 입력해주세요');
          return;
        }
        
        if (!this.qrContent.includes('_')) {
          alert('올바른 QR 코드 형식이 아닙니다. (예: CODE_TIMESTAMP_HASH)');
          return;
        }
        
        console.log('검증 시도:', this.qrContent);
        
        const response = await axios.post(
          'http://localhost:8090/api/products/validate',
          null,
          {
            params: {
              content: this.qrContent
            }
          }
        );
        
        this.validationResult = response.data;
        
        if (this.validationResult.productCode) {
          await this.loadScanHistory(this.validationResult.productCode);
        }
      } catch (error) {
        console.error('검증 실패:', error);
        alert('검증 중 오류가 발생했습니다: ' + (error.response?.data?.message || '서버 오류가 발생했습니다.'));
      } finally {
        this.isValidating = false;
      }
    },
    
    async loadScanHistory(productCode) {
      try {
        const response = await axios.get(`http://localhost:8090/api/products/${productCode}/scan-history`);
        console.log('스캔 이력 데이터:', response.data);
        this.scanHistory = response.data;
      } catch (error) {
        console.error('스캔 이력 조회 실패:', error);
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
      return statusMap[status] || '알 수 없음';
    },

    getValidationStatus(scan) {
      console.log('스캔 상태 체크:', {
        message: scan.validationMessage,
        valid: scan.valid,
        status: scan.status
      });
      if (!scan?.validationMessage) return '무효';
      if (scan.validationMessage.includes('전환되었습니다') || 
          scan.validationMessage.includes('인증이 완료되었습니다') ||
          scan.validationMessage.includes('인증된 정품입니다')) {
        return '정상';
      }
      if (scan.validationMessage.includes('의심스러운 제품')) {
        return '의심';
      }
      if (scan.validationMessage.includes('스캔 횟수를 초과')) {
        return '초과';
      }
      return '무효';
    },

    getValidationStatusClass(scan) {
      if (!scan?.validationMessage) return 'status-error';
      if (scan.validationMessage.includes('전환되었습니다') || 
          scan.validationMessage.includes('인증이 완료되었습니다') ||
          scan.validationMessage.includes('인증된 정품입니다')) {
        return 'status-normal';
      }
      if (scan.validationMessage.includes('의심스러운 제품')) {
        return 'status-warning';
      }
      return 'status-error';
    },

    copyQRContent() {
      navigator.clipboard.writeText(this.qrContent)
        .then(() => alert('복사되었습니다.'))
        .catch(err => console.error('복사 실패:', err));
    },
    
    exportScanHistory() {
      const data = this.scanHistory.map(scan => ({
        시간: new Date(scan.scanTime).toLocaleString(),
        상태: this.getValidationStatus(scan),
        메시지: scan.validationMessage,
        시간유효: scan.timeValid ? 'O' : 'X',
        해시유효: scan.hashValid ? 'O' : 'X'
      }));
      
      const csv = this.convertToCSV(data);
      const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `scan_history_${this.validationResult.productCode}_${new Date().getTime()}.csv`;
      link.click();
    },
    
    convertToCSV(arr) {
      const array = [Object.keys(arr[0])].concat(arr);
      return array.map(row => {
        return Object.values(row)
          .map(value => `"${value}"`)
          .join(',');
      }).join('\n');
    }
  }
}
</script> 