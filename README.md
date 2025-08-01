# NEMONEMO 📸

포토부스 사진을 디지털화하여 소중한 추억을 보관하는 Android 애플리케이션입니다.

## 📱 앱 소개

NEMONEMO는 포토부스에서 나온 인쇄 사진을 스마트폰으로 촬영하여 디지털로 저장하고, 함께 찍은 사람들의 이름을 태그로 관리할 수 있는 앱입니다.

### 주요 기능

- 📷 **포토부스 사진 촬영**: ML Kit Document Scanner를 사용한 고품질 사진 촬영
- 🖼️ **갤러리에서 이미지 불러오기**: 기존 사진을 앱으로 가져오기
- 🏷️ **태그 관리**: 함께 찍은 사람들의 이름을 태그로 저장
- 📚 **추억 보관**: 촬영한 사진들을 날짜별로 정리하여 보관
- 🔍 **태그 필터링**: 특정 사람과 찍은 사진들을 쉽게 찾기
- 💾 **갤러리 저장**: 촬영한 사진을 기기 갤러리에 저장
- 🔍 **전체화면 보기**: 저장된 사진을 전체화면으로 자세히 보기

## 🛠️ 기술 스택

- **언어**: Kotlin
- **최소 SDK**: API 24 (Android 7.0)
- **타겟 SDK**: API 36 (Android 14)
- **UI 프레임워크**: Material Design 3
- **데이터베이스**: Room Database
- **이미지 처리**: Glide
- **문서 스캔**: ML Kit Document Scanner
- **아키텍처**: MVVM 패턴

## 📋 요구사항

- Android 7.0 (API 24) 이상
- 카메라 권한
- 저장소 권한 (Android 9 이하)

## 🚀 설치 및 실행

### 1. 프로젝트 클론

```bash
git clone [repository-url]
cd nemonemo
```

### 2. Android Studio에서 열기

1. Android Studio 실행
2. "Open an existing Android Studio project" 선택
3. 프로젝트 폴더 선택

### 3. 빌드 및 실행

1. Gradle 동기화 완료 대기
2. 디바이스 또는 에뮬레이터 연결
3. `Run` 버튼 클릭 또는 `Shift + F10`

### 4. 명령줄에서 빌드

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드
./gradlew assembleRelease

# 앱 설치
./gradlew installDebug
```

## 📁 프로젝트 구조

```
app/
├── src/main/
│   ├── java/com/nemonemo/
│   │   ├── data/                    # 데이터베이스 관련
│   │   │   ├── AppDatabase.kt       # Room 데이터베이스
│   │   │   ├── ScanHistory.kt       # 스캔 히스토리 엔티티
│   │   │   └── ScanHistoryDao.kt    # 데이터 액세스 객체
│   │   ├── MainActivity.kt          # 메인 액티비티
│   │   ├── HistoryActivity.kt       # 히스토리 액티비티
│   │   ├── FullscreenImageActivity.kt # 전체화면 이미지 액티비티
│   │   └── TagInputDialog.kt        # 태그 입력 다이얼로그
│   ├── res/
│   │   ├── layout/                  # 레이아웃 파일들
│   │   ├── values/                  # 문자열, 색상, 테마
│   │   ├── drawable/                # 아이콘 및 이미지
│   │   └── xml/                     # 설정 파일들
│   └── AndroidManifest.xml          # 앱 매니페스트
└── build.gradle.kts                 # 앱 레벨 빌드 설정
```

## 🎯 사용법

### 1. 포토부스 사진 촬영
1. 앱 실행
2. "포토부스 사진 촬영" 버튼 탭
3. 카메라로 포토부스 사진 촬영
4. 촬영 완료 후 결과 확인

### 2. 갤러리에서 이미지 불러오기
1. "갤러리에서 이미지 불러오기" 버튼 탭
2. 갤러리에서 포토부스 사진 선택
3. 선택한 이미지 확인

### 3. 태그 추가 및 저장
1. 촬영/선택한 이미지 확인 후 "추억에 저장" 버튼 탭
2. 함께 찍은 사람들의 이름 입력 (예: 민수, 영희, 철수)
3. 저장 완료

### 4. 추억 보기
1. 메인 화면에서 "추억 보기" 버튼 탭
2. 저장된 포토부스 사진들 확인
3. 태그별 필터링 가능
4. 사진 탭하여 전체화면으로 보기

## 🔧 개발 환경 설정

### 필수 도구
- Android Studio Hedgehog | 2023.1.1 이상
- JDK 11
- Android SDK API 36

### 의존성 관리
프로젝트는 Version Catalogs를 사용하여 의존성을 관리합니다:
- `gradle/libs.versions.toml`에서 버전 관리
- 주요 라이브러리:
  - AndroidX Core KTX
  - Material Design 3
  - Room Database
  - Glide (이미지 로딩)
  - ML Kit Document Scanner

## 📝 주요 클래스 설명

### MainActivity
- 앱의 메인 화면
- 카메라 촬영 및 갤러리 선택 기능
- ML Kit Document Scanner 통합
- 이미지 저장 및 태그 관리

### HistoryActivity
- 저장된 포토부스 사진 목록 표시
- 태그별 필터링 기능
- 날짜별 정렬

### AppDatabase
- Room 데이터베이스 설정
- ScanHistory 엔티티 관리
- 데이터베이스 마이그레이션

## 🐛 문제 해결

### 일반적인 문제들

1. **빌드 오류**
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. **권한 문제**
   - 앱 설정에서 카메라 및 저장소 권한 확인
   - Android 10 이상에서는 저장소 권한이 자동으로 관리됨

3. **ML Kit Document Scanner 오류**
   - Google Play Services 최신 버전 확인
   - 인터넷 연결 확인

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 📞 연락처

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요.

---

**NEMONEMO** - 포토부스 추억을 디지털로 보관하세요 📸✨ 