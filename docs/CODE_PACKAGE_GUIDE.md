# 코드 컨벤션

## 0. 자바 코드 컨벤션

- 자바 코드 컨벤션은 구글 자바 스타일 가이드를 기본으로 변형된 우테코 스타일을 따른다. (https://google.github.io/styleguide/javaguide.html)

## 1. Layer간 DTO 변환

- Web <-> ApiController | 여기서 DTO 하나 필요하다.. (controller/request,response)
- ApiController <-> Service | 여기서 DTO 하나 필요해. (service/request,response에 위치)

그런데 두 개가 같은데 따로 만들기엔 귀찮으니까 일단 서비스 계층의 DTO로 사용가능하다면 하나만 만든다.

## 2. DTO 네이밍

- Request : [도메인][기능][Request]
- Response : [도메인][기능][Response]
    - 범용적으로 사용되는 조회 쿼리 시  [도메인][Response]로 네이밍 가능하다.

## 3. DTO 패키지 구조

- controller/request
- controller/response
- service/request
- service/response
  물론 DTO가 존재하지 않는 경우 패키지 자체를 생성하지 않아도 된다.

## 4. 연관관계 매핑

- 같은 애그리거트에 있는 엔티티는 연관관계 매핑
- 다른 애그리거트에 있는 엔티티는 연관관계 매핑 X (id로 참조)
- 연관관계 매핑이 필요한 경우, 양방향 매핑보다는 단방향 매핑을 선호한다.
    - Aggregate Root에 종속적인 관계를 제외하면 양방향 매핑은 지양한다. (예시: Order - OrderItem)
- 엔티티 매핑 시 fetch 전략은 기본적으로 LAZY로 설정한다. (예시: @ManyToOne(fetch = FetchType.LAZY))
    - N+1 문제를 해결하기 위해서는 fetch join을 활용하여 조회하도록 한다. (예시: OrderRepository에서 Order과 OrderItem을 함께 조회할 때 fetch join을 활용한다.)
- NOT NULL 제약 조건, 유니크 제약 조건 등 구체적인 제약조건은 DB 스키마에서만 관리하도록 한다.
    - ddl-auto: none으로 운영한다.

## 5. 도메인 형 패키지 구조 간 격벽

- 현재 도메인에 따라 패키지가 구성되어 있다. 하지만, Aggregate에 따른 동작을 지향해야 한다.
- JPA 엔티티 매핑도 이에 따라 구성되어 있다.
- 하지만 쿼리 등을 해올 때 성능 상의 이슈로 다른 도메인까지 한 번에 조회해야 하는 경우가 있다.
- 이 경우, 도메인형 패키지 구조는 유지하되, 도메인 간 격벽을 허물 수 있다.
    - 단, 이 경우 DTO Projection을 활용하여 데이터를 조회해오도록 한다.
    - 비즈니스 로직이 필요한 경우 다른 격벽의 Service를 호출하여 처리하도록 한다.
    - 이렇게 하면 결합도를 낮출 수 있다.
- Repository 클래스는 Aggregate 당 1개만 생성한다. Root 에서 하위 엔티티까지 조회할 수 있도록 한다. (예시: OrderRepository에서 Order과 OrderItem을 함께 조회)
    - Aggregate는 생각보다 작다. 보통 라이프사이클이 같은 객체들의 집합들끼리 묶이기 때문이다.
    - 정확히는 트랜잭션 일관성 경계에 속한 객체들의 집합이 애그리거트이다. (예시: Order와 OrderItem은 라이프사이클이 같기 때문에 같은 애그리거트에 속한다.)

## 6. Facade

- Controller에서 여러 Service를 호출해야 하는 경우, Facade를 도입하여 하나의 Service에서 여러 Service를 호출하도록 한다.
- Facade 패턴을 활용하면 Controller의 복잡도를 낮출 수 있다.

## 7. 도메인 서비스

- 도메인 서비스는 도메인 로직이 여러 애그리거트에 걸쳐 있는 경우에 사용한다.
- 도메인 서비스는 애그리거트에 종속적이지 않으며, 도메인 로직을 캡슐화한다.
- 도메인 서비스는 stateless하게 구현한다.
- 예시: 결제 로직이 Order와 Payment 애그리거트에 걸쳐 있는 경우, PaymentService에서 결제 로직을 처리하도록 한다.
- 도메인 서비스는 애그리거트의 상태를 변경하지 않도록 한다. (예시: OrderService에서 Order의 상태를 변경하는 로직은 Order 애그리거트 내부에서 처리하도록 한다.)
- 도메인 서비스는 애그리거트의 상태를 변경하는 경우, 해당 애그리거트의 메서드를 호출하여 상태를 변경하도록 한다. (예시: OrderService에서 Order의 상태를 변경하는 경우, Order 애그리거트의
  changeStatus() 메서드를 호출하여 상태를 변경하도록 한다.)

## 8. 예외 처리

- 예외 객체는 common.exception 패키지에 위치한다.
- 예외 메시지는 각 도메인 별로 관리한다. (예시: order.exception 패키지에 OrderException 클래스 생성)
- 예외 메시지는 ENUM으로 관리한다.

## 9. 테스트 코드

- Controller 테스트는 RestDocs를 활용하여 API 문서와 함께 작성한다.
- Service 테스트는 mockito를 활용하여 단위 테스트를 작성한다.
    - Service 테스트에서는 Repository를 mock 객체로 주입하여 테스트한다.
    - mockistic 하게 작성하여 실제 동작보단 행동 검증에 초점을 맞춘다.
- Repository 테스트는 H2 DB를 사용하여, @DataJpaTest 어노테이션을 활용하여 작성한다.
- given-when-then 패턴을 활용하여 테스트 코드를 작성한다. (가능하면)
    - 주석으로 given-when-then 구분을 명확히 한다. (예시: // given, // when, // then)

## 10. 기타

- setter 메서드는 지양한다. (예시: Order 객체의 상태를 변경할 때, setStatus() 메서드 대신 changeStatus() 메서드를 활용한다.)
- Lombok을 활용하여 getter, constructor 등을 자동으로 생성한다.
- 도메인 객체 생성자의 파라미터가 많을 경우 Lombok의 @Builder 어노테이션을 활용하여 빌더 패턴을 사용한다.
- Lombok의 @Slf4j 어노테이션을 활용하여 로그를 남긴다. (필요한 경우)
- 불필요한 주석은 지양한다. (예시: // getter, setter 등 Lombok으로 자동 생성되는 메서드에 대한 주석은 지양한다.)
- 메서드 이름은 동사로 시작하도록 한다. (예시: createOrder(), getOrderById() 등)
    - 정적 팩토리 메서드의 경우 of, from을 써도 돼. 기술적인 (ex.ApiResponse)의 경우 of, from이 더 잘 어울리고, 도메인을 담는 경우 동사형을 쓰면 좋겠지
- 변수 이름은 명확하게 작성한다. (예시: orderId, userId 등)
- 상수는 대문자로 작성한다.
- 상수는 final 키워드를 활용하여 변경 불가능하도록 한다. (예시: private static final String ORDER_STATUS_PENDING = "PENDING";)
- 메서드 모듈화를 신경쓴다. 예를 들면, if (xxx) throw new IllegalArgumentException("xxx"); 이런 경우, validateXxx() 메서드를 만들어서 해당 메서드에서
  검증하도록 한다. (예시: validateOrderStatus() 메서드에서 주문 상태를 검증하도록 한다.)
- 도메인 모델 패턴에 따라, 도메인 로직을 최대한 도메인 모델 내부에 위치하도록 한다. (예시: Order 객체의 상태 변경 로직은 Order 객체 내부에 위치하도록 한다.)
    - Tell, Don't Ask 원칙을 지키도록 한다. (예시: Order 객체의 상태를 변경할 때, Order 객체에 changeStatus() 메서드를 호출하여 상태를 변경하도록 한다.)
- Repository에서 조회해오는 로직이 재활용가능하고 복잡한 경우, Service와 Repository 사이에 ImplementLayer(XXXReader, XXXWriter 등)를 만들어서 해당 로직을
  구현하도록 한다. (예시: OrderReader 인터페이스를 만들어서 OrderRepository에서 조회해오는 로직을 구현하도록 한다.)
