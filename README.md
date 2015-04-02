# CacheMem
CacheMem 은 Java8, vert.x 그리고 memcached 와 MySQL을 이용한 Object 캐시 서버 입니다.

## CacheMem 소개
웹어플리케이션 또는 앱을 운영하다 보면 가장 많이 부하가 걸리고 서비스 속도에 지장을 주는 부분이 
아마 Database 에서 데이타를 가지고 오는 부분 혹은 복잡한 비즈니스 로직일 것 입니다.
CacheMem 은 Database 의 result set 이나 혹은 특정 비즈니스 로직 (메소드)를 실행후 나온 결과를 
memcached 에 캐시 하여 빠르게 결과를 리턴해 줄수있으며 상황에 따라서 유연하게 서버를 늘려나갈수 
있는 캐시 서버 입니다.

## CacheMem 설치
  1. Java 8 설치하기
  
    ######JDK를 다운로드한다.
  
      - http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
  
    ######압축을 풀어준다.
   
      - tar xvfz jdk-8u40-linux-x64.gz
      
    ######적당한 폴더로 이동시켜 준뒤 심볼릭 링크를 걸어준다.
   
      - mv jdk1.8.0_40 /usr/local/
      - cd /usr/local/
      - ln -s jdk1.8.0_40 java
      
    ######Java 의 PATH 를 설정해 준다. (.bash_profile 혹은 /etc/profile 에 설정)
   
      ```
      JAVA_HOME=/usr/local/java
      PATH=$PATH:$JAVA_HOME/bin
      export JAVA_HOME
      export PATH
      ```
      - source .bash_profile 혹은 /etc/profile

  2. Gradle 설치하기
  
    ######Gradle을 다운로드한다.

      - https://gradle.org/downloads

    ######압축을 풀어준다.
    
      - unzip gradle-2.2.1-bin.zip
      
    ######적당한 폴더로 이동시켜 준뒤 심볼릭 링크를 걸어준다.
    
      - mv gradle-2.2.1 /usr/local
      - cd /usr/local
      - ln -s gradle-2.2.1 gradle
      
    ######Gradle 의 PATH 를 설정해 준다. (.bash_profile 혹은 /etc/profile 에 설정)
      
      ```
      GRADLE_HOME=/usr/local/gradle
      PATH=$PATH:$GRADLE_HOME/bin
      export GRADLE_HOME
      export PATH
      ```
      - source .bash_profile 혹은 /etc/profile
      
  3. Maven 설치하기
  
    ######Maven을 다운로드한다.
  
      - http://apache.tt.co.kr/maven/maven-3/3.3.1/binaries/apache-maven-3.3.1-bin.tar.gz
    
    ######압축을 풀어준다.

      - tar xvfz apache-maven-3.3.1-bin.tar.gz
    
    ######적당한 폴더로 이동시켜 준뒤 심볼릭 링크를 걸어준다.
    
      - mv apache-maven-3.3.1 /usr/local
      - cd /usr/local
      - ln -s apache-maven-3.3.1 maven
      
    ######Maven 의 PATH 를 설정해 준다. (.bash_profile 혹은 /etc/profile 에 설정)
      
      ```
      MAVEN_HOME=/usr/local/maven
      PATH=$PATH:$MAVEN_HOME/bin
      export MAVEN_HOME
      export PATH
      ```
      - source .bash_profile 혹은 /etc/profile
      
  4. Vert.x 설치하기
    
    ######Vert.x 다운로드한다.
  
      - http://vertx.io/vertx-downloads/downloads/vert.x-1.3.1.final.tar.gz
    
    ######압축을 풀어준다.

      - tar xvfz vert.x-1.3.1.final.tar.gz
      
    ######적당한 폴더로 이동시켜 준뒤 심볼릭 링크를 걸어준다.
    
      - mv vert.x-1.3.1.final /usr/local
      - cd /usr/local
      - ln -s vert.x-1.3.1.final vertx
      
    ######Vert.x 의 PATH 를 설정해 준다. (.bash_profile 혹은 /etc/profile 에 설정)
      
      ```
      VERTX_HOME=/usr/local/vertx
      PATH=$PATH:$VERTX_HOME/bin
      export VERTX_HOME
      export PATH
      ```
      - source .bash_profile 혹은 /etc/profile
      
  5. Memcached 설치하기 
    
    ######libevent 설치.

      - https://sourceforge.net/projects/levent/files/libevent/libevent-2.0/libevent-2.0.22-stable.tar.gz
      - tar xvfz libevent-2.0.22-stable.tar.gz
      - cd libevent-2.0.22-stable
      - ./configure
      - make
      - make install
      
    ######libevent ld path 설정 
    
      - vi /etc/ld.so.conf
      - /usr/local/lib path 추가 
      
      ```
      include ld.so.conf.d/*.conf
      /usr/local/lib
      ```
      
      - ldconfig -v 실행하여 적용 및 확인
    
    ######Memcached 설치 
      
      - http://www.memcached.org/files/memcached-1.4.22.tar.gz
      - tar xvfz memcached-1.4.22.tar.gz
      - cd memcached-1.4.22
      - ./configure --prefix=/usr/local/memcached-1.4.22 --with-libevent=/usr/local/lib/
      - make 
      - make install
      - cd /usr/local
      - ln -s memcached-1.4.22 memcached
      
    ######Memcached 실행 
    
      - /usr/local/memcached/bin/memcached -p 11211 -d -u root -m 32 -c 10240 -b 10240 -P /dev/shm/memcached.pid -t 1
      
    ######Memcached 실행 옵션 설명 
    
      - -p : tcp 포트번호
      - -d : daemon mode  
      - -u : 유저 실행 권한 
      - -m : 사용할 최대 메모리 (mb 단위)
      - -c : 최대 접속 수
      - -b : 최대 백로그 (큐)
      - -P : pid 파일 위치 
      - -t : 사용할 thread 수 
      



## CacheMem 진행 상황 
* 2015.03.31 서버 개발 완료 
* 클라이언트 개발 진행중
* Spring 환경에서 쉽게 적용 할수 있는 custom annotation 개발 진행중
