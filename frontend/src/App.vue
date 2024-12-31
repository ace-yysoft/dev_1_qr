<template>
  <div id="app">
    <nav class="sidebar">
      <div class="logo">
        <h1>QR 관리 시스템</h1>
      </div>
      <div class="nav-links">
        <router-link to="/register" class="nav-link">
          <i class="fas fa-plus"></i>
          <span>신규 제품 등록</span>
        </router-link>
        <router-link to="/" class="nav-link">
          <i class="fas fa-box"></i>
          <span>제품 관리</span>
        </router-link>
        <router-link to="/validate" class="nav-link">
          <i class="fas fa-qrcode"></i>
          <span>QR 코드 검증</span>
        </router-link>
      </div>
    </nav>
    <main class="main-content">
      <router-view/>
    </main>
  </div>
</template>

<style>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css');

/* 전역 변수 설정 */
:root {
  --primary-color: #1a237e;
  --primary-light: #283593;
  --secondary-color: #424242;
  --success-color: #388E3C;
  --warning-color: #F57C00;
  --error-color: #D32F2F;
  --background-color: #f5f5f5;
  --card-shadow: 0 2px 8px rgba(26, 35, 126, 0.1);
  --border-radius: 12px;
  --spacing-unit: 20px;
}

#app {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--secondary-color);
  display: flex;
  min-height: 100vh;
}

.app-container {
  display: grid;
  grid-template-columns: 250px 1fr;
  min-height: 100vh;
}

.sidebar {
  width: 240px;
  background: var(--primary-color);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 30px;
}

/* 공통 컴포넌트 스타일 */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background: var(--primary-light);
}

.btn-secondary {
  background: #e0e0e0;
  color: var(--secondary-color);
}

.btn-secondary:hover {
  background: #bdbdbd;
}

.card {
  background: white;
  border-radius: var(--border-radius);
  box-shadow: var(--card-shadow);
  overflow: hidden;
}

.card-header {
  padding: var(--spacing-unit);
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-body {
  padding: var(--spacing-unit);
}

.status-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 600;
}

.status-badge.success {
  background: #E8F5E9;
  color: var(--success-color);
}

.status-badge.warning {
  background: #FFF3E0;
  color: var(--warning-color);
}

.status-badge.error {
  background: #FFEBEE;
  color: var(--error-color);
}

/* 입력 필드 공통 스타일 */
.form-input {
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.form-input:focus {
  border-color: var(--primary-color);
  outline: none;
}

/* 그리드 레이아웃 */
.grid {
  display: grid;
  gap: var(--spacing-unit);
}

.grid-2 {
  grid-template-columns: repeat(2, 1fr);
}

.grid-3 {
  grid-template-columns: repeat(3, 1fr);
}

@media (max-width: 768px) {
  .grid-2, .grid-3 {
    grid-template-columns: 1fr;
  }
}

.logo {
  padding: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h1 {
  color: white;
  margin: 0;
  font-size: 1.2rem;
  text-align: center;
}

.nav-links {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.nav-link:hover,
.nav-link.router-link-active {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-link i {
  width: 20px;
  text-align: center;
}

.main-content {
  flex: 1;
  padding: 30px;
  background: var(--background-color);
  overflow-y: auto;
}
</style>
