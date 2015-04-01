# CacheMem
CacheMem 은 Java8, vert.x 그리고 memcached 와 MySQL을 이용한 Object 캐시 서버 입니다.

## CacheMem 소개
웹어플리케이션 또는 앱을 운영하다 보면 가장 많이 부하가 걸리고 서비스 속도에 지장을 주는 부분이 
아마 Database 에서 데이타를 가지고 오는 부분 혹은 복잡한 비즈니스 로직일 것 입니다.
CacheMem 은 Database 의 result set 이나 혹은 특정 비즈니스 로직 (메소드)를 실행후 나온 결과를 
memcached 에 캐시 하여 빠르게 결과를 리턴해 줄수있으며 상황에 따라서 유연하게 서버를 늘려나갈수 
있는 캐시 서버 입니다.

# CacheMem 설치
  1. Java 8 설치하기
  
    ###JDK를 다운로드한다.
  
      - http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
  
    ###압축을 풀어준다.
   
      - tar xvfz jdk-8u40-linux-x64.gz
      
    ###적당한 폴더로 이동시켜 준뒤 심볼릭 링크를 걸어준다.
   
      - mv jdk1.8.0_40 /usr/local/
      - cd /usr/local/
      - ln -s jdk1.8.0_40 java
      
    ###Java 의 PATH 를 설정해 준다. (.bash_profile 혹은 /etc/profile 에 설정)
   
      - JAVA_HOME=/usr/local/java
      - PATH=$PATH:$JAVA_HOME/bin
      - export JAVA_HOME
      - export PATH


## CacheMem 진행 상황 
* 2015.03.31 서버 개발 완료 
* 클라이언트 개발 진행중
* Spring 환경에서 쉽게 적용 할수 있는 custom annotation 개발 진행중
