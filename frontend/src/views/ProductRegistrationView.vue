<template>
  <div class="product-registration">
    <div class="page-header">
      <h1>신규 제품 등록</h1>
    </div>
    
    <div class="card registration-form">
      <div class="card-body">
        <div class="grid grid-2">
          <div class="form-group">
            <label>기본 정보</label>
            <input class="form-input" v-model="product.name" placeholder="제품명">
            <input class="form-input" v-model="product.code" placeholder="제품코드">
          </div>
          
          <div class="form-group">
            <label>제조 정보</label>
            <input class="form-input" v-model="product.manufacturingLocation" placeholder="제조 위치">
            <input class="form-input" type="datetime-local" v-model="product.manufacturingTime">
          </div>
          
          <div class="form-group">
            <label>유통 정보</label>
            <input class="form-input" v-model="product.expectedDeliveryLocation" placeholder="예상 판매/배송 위치">
            <input class="form-input" type="datetime-local" v-model="product.expectedDeliveryTime">
          </div>
          
          <div class="form-group">
            <label>설정</label>
            <select v-model="product.status" class="form-input">
              <option value="CREATED">생성됨</option>
              <option value="IN_TRANSIT">유통중</option>
              <option value="READY_FOR_SALE">판매준비</option>
            </select>
            <input class="form-input" type="number" v-model="product.maxScanCount" placeholder="최대 스캔 횟수" min="1">
          </div>
        </div>
      </div>
      <div class="card-footer">
        <div class="button-container">
          <button class="btn btn-secondary" @click="$router.push('/')">
            <span>취소</span>
          </button>
          <button class="btn btn-primary" @click="registerProduct">
            <span>등록</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'ProductRegistrationView',
  data() {
    return {
      product: {
        name: '',
        code: '',
        manufacturingLocation: '',
        manufacturingTime: '',
        expectedDeliveryLocation: '',
        expectedDeliveryTime: '',
        status: 'CREATED',
        maxScanCount: 1
      }
    }
  },
  methods: {
    async registerProduct() {
      try {
        if (!this.validateForm()) {
          return;
        }

        const productData = {
          ...this.product,
          manufacturingTime: new Date(this.product.manufacturingTime).toISOString(),
          expectedDeliveryTime: new Date(this.product.expectedDeliveryTime).toISOString()
        };

        const response = await axios.post('http://localhost:8090/api/products', productData);
        console.log('생성된 제품:', response.data);
        
        alert('제품이 성공적으로 등록되었습니다.');
        
        // 성공 시 제품 목록 페이지로 이동
        this.$router.push('/');
      } catch (error) {
        const errorMessage = error.response?.data?.message || error.message || '서버 오류가 발생했습니다.';
        console.error('제품 등록 실패:', errorMessage);
        alert('제품 등록 실패: ' + errorMessage);
      }
    },
    validateForm() {
      if (!this.product.name.trim()) {
        alert('제품명을 입력해주세요.');
        return false;
      }
      if (!this.product.code.trim()) {
        alert('제품코드를 입력해주세요.');
        return false;
      }
      if (!this.product.manufacturingLocation.trim()) {
        alert('제조 위치를 입력해주세요.');
        return false;
      }
      if (!this.product.manufacturingTime) {
        alert('제조 시간을 입력해주세요.');
        return false;
      }
      return true;
    }
  }
}
</script>

<style scoped>
.product-registration {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-unit);
}

.page-header {
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  font-size: 2rem;
  color: var(--primary-color);
}

.registration-form {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 40px;
}

.card-footer {
  padding: 30px;
  border-top: 1px solid #e0e0e0;
  background: var(--background-color);
}

.button-container {
  display: flex;
  justify-content: center;
  gap: var(--spacing-unit);
}

.button-container .btn {
  min-width: 120px;
  padding: 12px 24px;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
}

.button-container .btn span {
  display: inline-block;
  text-align: center;
  width: 100%;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 10px;
  font-weight: 500;
  color: var(--primary-color);
}

.form-input {
  margin-bottom: 10px;
}
</style> 