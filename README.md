
# QR Code Generator and Validator

QR 코드 생성 및 검증 시스템

## 주요 기능
- QR 코드 생성 및 관리
- QR 코드 검증
- 실시간 스캔 이력
- 제품 상태 추적
- 위변조 방지 시스템

## 기술 스택
- Backend: Spring Boot, MongoDB, WebSocket
- Frontend: Vue.js 3, Axios, SockJS

## 설치 방법
1. 백엔드 실행
   ```
   cd backend
   mvn spring-boot:run
   ```

2. 프론트엔드 실행
   ```
   cd frontend
   npm install
   npm run serve
   ```

=========================

[ 상세 테스트 시나리오 ]



1. 제품 생성 테스트



1-1. 신규 제품 등록 (HomeView)

    - 제품명: "테스트 제품"

    - 제품코드: "TEST001"

    - 제조 위치: "서울시 강남구"

    - 제조 시간: 현재 시간

    - 예상 판매 위치: "서울시 서초구"

    - 예상 판매 시간: 현재 시간 + 7일

    - 상태: CREATED

    - 최대 스캔 횟수: 3



1-2. 생성 결과 확인

    - 제품 목록에 새 제품 표시 확인

    - QR 코드 이미지 생성 확인

    - QR 코드 텍스트 형식 확인 (코드_타임스탬프_해시)



2. 상태 전환 테스트



2-1. CREATED -> IN_TRANSIT

    - QR 코드 스캔 (ValidateView)

    - 메시지 확인: "제품이 유통 단계로 전환되었습니다."

    - 제품 상태 변경 확인

    - 스캔 이력 확인



2-2. IN_TRANSIT -> READY_FOR_SALE

    - 동일 QR 코드 재스캔

    - 메시지 확인: "제품이 판매 준비 상태로 전환되었습니다."

    - 제품 상태 변경 확인

    - 스캔 이력 추가 확인



2-3. READY_FOR_SALE -> ACTIVATED

    - 동일 QR 코드 재스캔

    - 메시지 확인: "정품 인증이 완료되었습니다."

    - 제품 상태 변경 확인

    - 첫 스캔 시간 기록 확인

    - 스캔 횟수 1로 설정 확인



3. 스캔 횟수 테스트



3-1. ACTIVATED 상태 첫 스캔

    - 동일 QR 코드 재스캔

    - 메시지 확인: "인증된 정품입니다. (스캔 횟수: 2/3)"

    - 스캔 횟수 증가 확인



3-2. ACTIVATED 상태 두 번째 스캔

    - 동일 QR 코드 재스캔

    - 메시지 확인: "인증된 정품입니다. (스캔 횟수: 3/3)"

    - 스캔 횟수 증가 확인



3-3. 최대 스캔 횟수 초과 테스트

    - 동일 QR 코드 재스캔

    - 메시지 확인: "허용된 스캔 횟수를 초과했습니다."

    - 제품 상태 SUSPICIOUS 변경 확인



4. 검증 실패 테스트



4-1. 잘못된 형식의 QR 코드

    - 입력: "TEST001"

    - 메시지 확인: "잘못된 QR 코드 형식입니다."



4-2. 위조된 해시

    - 입력: "TEST001_20241231235959_12345678"

    - 메시지 확인: "위조된 QR코드입니다."

    - 제품 상태 SUSPICIOUS 변경 확인



4-3. 만료된 타임스탬프

    - 어제 날짜로 QR 코드 생성

    - 메시지 확인: "만료된 QR코드입니다."



5. 스캔 이력 확인



5-1. 이력 데이터 확인 (MongoDB)

    db.scan_history.find({productCode: "TEST001"}).sort({scanTime: -1})

    - 각 스캔별 기록 확인:

        - scanTime

        - valid

        - validationMessage

        - productStatus

        - scanCount

        - hashValid

        - timeValid



5-2. UI 이력 표시 확인

    - HomeView의 스캔 이력 보기 클릭

    - ValidateView의 스캔 이력 섹션

    - 시간순 정렬 확인

    - 상태별 색상 표시 확인



6. 엣지 케이스 테스트



6-1. 네트워크 오류

    - 서버 중지 후 스캔 시도

    - 에러 메시지 확인



6-2. 존재하지 않는 제품

    - 임의의 제품코드로 QR 코드 생성

    - 메시지 확인: "제품을 찾을 수 없습니다."



6-3. 중복 스캔 방지

    - 동일 QR 코드 빠르게 연속 스캔

    - 정상적인 처리 확인

