# NEMONEMO GitHub Pages

이 폴더는 NEMONEMO 앱의 GitHub Pages 웹사이트를 위한 파일들을 포함합니다.

## 파일 구조

- `index.html` - 메인 웹페이지
- `styles.css` - 스타일시트
- `script.js` - JavaScript 기능
- `404.html` - 404 에러 페이지
- `robots.txt` - 검색 엔진 크롤링 설정
- `sitemap.xml` - 사이트맵

## 배포

이 웹사이트는 GitHub Actions를 통해 자동으로 배포됩니다. `main` 브랜치에 푸시하면 자동으로 GitHub Pages에 배포됩니다.

## 로컬 테스트

로컬에서 웹사이트를 테스트하려면:

1. Python 3가 설치되어 있는지 확인
2. docs 폴더로 이동
3. 다음 명령어 실행:
   ```bash
   python -m http.server 8000
   ```
4. 브라우저에서 `http://localhost:8000` 접속

## 커스터마이징

- `styles.css`에서 색상과 스타일 수정
- `script.js`에서 인터랙션 기능 수정
- `index.html`에서 콘텐츠 수정

## 참고사항

- GitHub Pages URL: `https://yourusername.github.io/Nemonemo/`
- 실제 사용 시 `yourusername`을 실제 GitHub 사용자명으로 변경해야 합니다. 