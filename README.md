# 의사 결정 과정 공유

## 테이블-엔티티 설계

* 최초 생각: MongoDB
* 이후 든 생각: JSON 타입을 사용하는 MySQL / PostgreSQL의 JOIN
* DB 레벨 제어가 의도가 아닐 것 같음 : H2로 결정되므로 코드 레벨 제어 필요
* 테이블을 잘게 쪼갤수록 JOIN이 늘어남: PK 생성 시 uuid 타임스탬프 형태 + 테이블 파티셔닝 생각
* DB 레벨 제어가 의도가 아닐 것 같음 2:

------ 

* 객체지향적 관점에서 고려: `JPA Inheritance` 로 의사결정 
* Type 고민: Join과 Single 
* Null 컬럼을 통해 비워 두는 것이 깔끔하지 않아 보임
* -> Join으로 의사결정

ㅣ 어느 쪽에 부하를 줄지 트레이드오프하는 과정을 통해 코드레벨에서의 설계를 고민해 볼 수 있었음


## 동시성 문제

* 빠른 개발을 위해 Auto Increment 지정했으나 추후 `분산 처리를 위해 UUID` 등을 통한 KEY 제어 필요
* 설문조사 수정 시 `버전 테이블을 통한 낙관적 락`


## ERD / API 설계서

* [ERD](https://github.com/jeansuh42/be1-onboarding-survey/wiki/ERD)
* [API](https://github.com/jeansuh42/be1-onboarding-survey/wiki/%EC%83%81%EC%84%B8-API-%EC%84%A4%EA%B3%84)


-------

* **추가 기능**
* 설문조사 등록 시 설문 요소의 순서를 지정
* -> 해당 순서가 아닐 경우 등록 실패하는 방식으로 구현

-------

* **JPA 기능적 요소**
* `@Inheritance` 지정을 통한 관계 테이블 상속 매핑 전략
* `@Embedabble`을 사용한 객체 지향적 엔티티 작성
* 전체 리스트 조회 / 조인 시 `@Entitygraph`를 통한 N+1 부하 제어
* `@Transactional(readOnly = true)`를 통한 조회 성능 효율화 
* -> **@Version** 사용이 아닌 `버전 테이블 도입`으로 동시성 문제 고려