# devlog-11 image processor

세 API 버전이 공통으로 호출하는 외부 이미지 프로세서다. S3 staging key를 입력받아 본문용/썸네일 JPEG를 만들고 metadata를 반환한다. API 버전별 차이는 이 함수가 아니라 Spring에서 호출을 실행하고 합류하는 방식에만 있다.

필수 환경 변수는 `IMAGE_BUCKET`이다. Lambda execution role에는 해당 bucket의 `GetObject`(staging)와 `PutObject`(content/thumbnail) 권한이 필요하다.

컨테이너 이미지를 빌드해 Lambda에 배포한 뒤 애플리케이션에 `IMAGE_PROCESSOR_LAMBDA_NAME`을 지정한다.
