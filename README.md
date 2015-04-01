# CacheMem
CacheMem 은 Java8, vert.x 그리고 memcached 와 MySQL을 이용한 Object 캐시 서버 입니다.

## CacheMem 소개
웹어플리케이션 또는 앱을 운영하다 보면 가장 많이 부하가 걸리고 서비스 속도에 지장을 주는 부분이 
아마 Database 에서 데이타를 가지고 오는 부분 혹은 복잡한 비즈니스 로직일 것 입니다.
CacheMem 은 Database 의 result set 이나 혹은 특정 비즈니스 로직 (메소드)를 실행후 나온 결과를 
memcached 에 캐시 하여 빠르게 결과를 리턴해 줄수있으며 상황에 따라서 유연하게 서버를 늘려나갈수 
있는 캐시 서버 입니다.

## CacheMem 진행 상황 
* 2015.03.31 서버 개발 완료 
* 클라이언트 개발 진행중
* Spring 환경에서 쉽게 적용 할수 있는 custom annotation 개발 진행중
