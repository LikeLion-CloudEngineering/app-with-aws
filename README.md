# 🦁 멋쟁이 사자처럼 : Cloud Engineering - app-with-aws

## 클라우드로 구축한 파일 업로드/다운로드 프로젝트
---

### 프로젝트
- [x] AWS 로그인 관련 application.properties 작성
- [x] Auditing Fields, Listener
- [x] EC2 인스턴스 내 리소스 물리적 저장소 경로 변경 (Windows(개발), Linux-Ubuntu(배포))

### RDS
- [x] public subnet(테스트용)에 구축
- [x] RDS 엔드포인트로 MySQLWorkbench 커넥션 생성
- [ ] private subnet에 RDS 구축하고 public subnet의 EC2 인스턴스를 통해 접근

### S3
- [x] Public 접근한 S3 버킷
- [x] 파일 업로드 및 다운로드
- [x] Repository 저장, 물리적인 저장 (S3 전송 후 리소스 삭제), S3 전송
- [ ] Cloud Front 연계하여 private한 S3 버킷 유지
      

### EC2 인스턴스
- [x] Ubuntu 22.04
- [x] EC2 인스턴스에 git, mvn, jdk 환경 세팅
- [x] Github repository pull, maven 패키징, nohup jar 파일 실행

