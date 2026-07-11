# 코드 컨벤션

## 0. 자바 코드 컨벤션

- 자바 코드 컨벤션은 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) 를 기본으로 한 우테코 스타일을 따른다.
- import on-demand(`*`) 는 금지한다. 항상 명시적 import 를 사용한다.
- **불변 DTO/값 객체는 `record`** 로 표현한다(생성자·getter·equals/hashCode 자동). Entity 는 `record` 로 만들 수 없으므로 일반 클래스로 둔다.
- `Optional` 을 필드/파라미터로 남용하지 않는다. 반환 타입에 한정해 사용한다.

## 1. 모듈 & 패키지 구조

**단일 Gradle 모듈**이다. 실행 산출물(`bootJar`)은 이 모듈 하나뿐이다.

패키지는 **도메인형 구조**로 나눈다. 최상위를 도메인(`member`, `post`, `comment`, `board`, `group` …)으로 쪼개고, 각 도메인 **내부에서 계층별 하위 패키지**(
`controller` / `service` / `domain` / `repository` / `exception`)로 다시 나눈다. 공통 관심사는 `common` 에 둔다.

```
cluverse
├── CluverseApiApplication.java
├── common
│   ├── api
│   │   ├── ApiControllerAdvice.java     # 전역 예외 핸들러(@RestControllerAdvice)
│   │   └── response                     # ApiResponse 등 공통 응답
│   ├── auth                             # 인증(@Login, LoginMember, ArgumentResolver, Interceptor, SessionManager)
│   ├── config                           # WebMvcConfig, QuerydslConfig, AwsS3Config …
│   ├── entity                           # BaseTimeEntity
│   ├── exception                        # 공통 예외(BadRequest/NotFound/Forbidden/Unauthorized)
│   └── properties                       # @ConfigurationProperties
└── <domain>                             # member, post, comment, board, group, recruitment, …
    ├── controller                       # @RestController (URL 에 /api/v1 버전 표기)
    ├── service
    │   ├── XxxService.java              # 쓰기/명령 유스케이스 흐름
    │   ├── XxxQueryService.java         # 조회 전용 유스케이스(§3.4)
    │   ├── request                      # 요청 DTO (record)
    │   ├── response                     # 응답 DTO (record)
    ├── implement                        # Implement Layer(Reader/Writer/…)
    ├── domain                           # @Entity + 해당 도메인 Enum
    ├── repository                       # XxxRepository (+ XxxQueryRepository)
    └── exception                        # XxxExceptionMessage (enum)
```

- 계층 구분(Presentation/Business/Implement/Data)은 **모듈 경계가 아니라 도메인 안의 하위 패키지 경계**로 표현한다.
- API 버전은 패키지가 아니라 **URL**(`/api/v1/...`)로 관리한다. 컨트롤러는 도메인의 `controller` 패키지에 둔다.
- DTO 가 존재하지 않는 경우 해당 패키지(`request`/`response`)를 만들지 않아도 된다.

### 1.1 네이밍 규칙

| 대상                                   | 규칙                                     | 예시                                                                       |
|--------------------------------------|----------------------------------------|--------------------------------------------------------------------------|
| 요청 DTO                               | `[동사][대상]Request` 또는 `[대상][기능]Request` | `AddMajorRequest`, `UpdateProfileRequest`, `MemberNicknameUpdateRequest` |
| 응답 DTO                               | `[도메인][기능]Response` 또는 `[도메인]Response` | `MemberProfileResponse`, `MemberMajorResponse`                           |
| **도메인(=Entity)**                     | **개념 명사 (Entity 접미사 없음)**              | `Member`, `MemberProfile`, `Follow`                                      |
| Repository                           | `[도메인]Repository`                      | `MemberRepository`, `FollowRepository`                                   |
| QueryRepository (동적·복잡 조회, QueryDSL) | `[도메인]QueryRepository`                 | `MemberQueryRepository`, `FeedQueryRepository`                           |
| Repository 확장 fragment (QueryDSL, 엔티티 반환·fetch join·벌크 UPDATE) | `[도메인]RepositoryCustom` + `[도메인]RepositoryImpl` | `MemberRepositoryCustom`/`Impl`, `PostRepositoryCustom`/`Impl`, `CommentRepositoryCustom`/`Impl` |
| Enum                                 | 개념 명사                                  | `MemberStatus`, `MajorType`                                              |
| Service (명령)                         | `[도메인]Service`                         | `MemberService`                                                          |
| Service (조회)                         | `[도메인]QueryService`                    | `MemberQueryService`, `FeedQueryService`                                 |
| Implement Layer                      | `[도메인][역할]`                            | `MemberReader`, `MemberWriter`                                           |
| 예외 메시지                               | `[도메인]ExceptionMessage` (enum)         | `MemberExceptionMessage`                                                 |

### 1.2 DTO 검증

- 값에 대한 형식/기초 검증(0보다 큰지, 비어있지 않은지 등)은 **요청 DTO 에서** 처리한다. Spring Bean Validation 을 활용한다(§4.2).
    - ex. 필수값은 `@NotNull`, 비어있지 않음은 `@NotBlank`/`@NotEmpty`, 컨트롤러에서 `@Valid` 로 트리거.
- 그 외의 검증(재고, 중복, 상태 전이 등 비즈니스 규칙)은 **서비스/Implement Layer/도메인 모델 내부**에서 처리한다. (예: `MemberWriter` 에서 닉네임 중복을 검증한다.)

## 2. 레이어 아키텍처

> 이
>
절은 [지속 성장 가능한 소프트웨어를 만들어가는 방법](https://geminikims.medium.com/%EC%A7%80%EC%86%8D-%EC%84%B1%EC%9E%A5-%EA%B0%80%EB%8A%A5%ED%95%9C-%EC%86%8C%ED%94%84%ED%8A%B8%EC%9B%A8%EC%96%B4%EB%A5%BC-%EB%A7%8C%EB%93%A4%EC%96%B4%EA%B0%80%EB%8A%94-%EB%B0%A9%EB%B2%95-97844c5dab63)(
> geminikims)의 레이어드 아키텍처를 따른다.
> 핵심은 **각 레이어가 "무엇을 아는가"를 통제**하는 것이다. Service·Implement·Repository 는 이름이 아니라 **아는 범위(추상화 수준)**로 구분된다.

```
HTTP 요청
  │
  ▼
[Presentation]  Controller (<domain>.controller)          ── HTTP DTO ↔ ApiResponse 래핑, 그 외 로직 없음
  │            └─▶ Assembler (필요 시, §6)                 ── 여러 Service 조합이 필요할 때만
  ▼
[Business]      Service / QueryService (<domain>.service) ── 비즈니스 "흐름"만. Request → Response
  │                                                          (Repository 를 직접 모른다)
  ▼
[Implement]     Reader/Writer/… (<domain>.service.implement) ── 비즈니스 "구현". 재사용 단위
  │                                                          (유일하게 Repository 를 아는 계층)
  ▼
[Data Access]   Repository / QueryRepository (<domain>.repository) ── 순수 데이터 접근
```

### 2.1 각 레이어의 역할 (Service ≠ Implement ≠ Repository)

세 계층의 책임이 흐려지는 것이 가장 흔한 부패다. 아래 경계를 엄격히 지킨다.

| 계층                              | 한 문장 정의                                      | 하는 일                                                                                                    | 하지 않는 일 (금지)                                                                                       |
|---------------------------------|----------------------------------------------|---------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| **Service** (Business)          | "구현은 몰라도 **비즈니스 흐름은 읽히는**" 오케스트레이터           | 유스케이스 단위 흐름 조립, Implement 호출 순서 결정, 트랜잭션 밖 작업(외부 I/O) 배치, Request→Response 변환                           | ❌ Repository·EntityManager 직접 주입/호출<br>❌ JPQL·쿼리 메서드·fetch 전략 인지<br>❌ 조건 분기가 필요한 상세 조회/저장 로직 직접 구현 |
| **Implement** (Reader/Writer/…) | Service 가 쓰는 **재사용 가능한 도구(tool)**. 상세 구현을 소유 | 복합 조회+Aggregate 조립(Reader), 쓰기/상태변경/트랜잭션 경계(Writer), 검증(Validator) 등 **단일 책임 구현**. 유일하게 Repository 를 주입 | ❌ 유스케이스 전체 흐름 오케스트레이션<br>❌ 응답 DTO(`XxxResponse`)로의 변환<br>❌ 상위(Service) 존재 인지                       |
| **Repository** (Data Access)    | 저장소에 대한 **순수 인터페이스**                         | Spring Data 쿼리 메서드 / `@Query`(JPQL) / QueryDSL 로 **데이터 입출력만**                                           | ❌ 비즈니스 규칙·검증·분기 판단<br>❌ 여러 Aggregate 조립 판단(그건 Reader 몫)                                            |

> **판별 기준:** 메서드를 소리 내 읽었을 때 **비즈니스 흐름**이 들리면 Service, **구현 방법(어떻게)**이 들리면 Implement, **데이터 입출력**만 있으면 Repository. —
> Service 는 *무엇을*, Implement 는 *어떻게*, Repository 는 *어디서* 를 안다.

### 2.2 참조 규칙 (4대 원칙)

1. **순방향 참조만 허용**: 레이어는 위 → 아래로만 참조한다. Controller→Service→Implement→Repository. 역방향 호출은 없다.
2. **역류 금지 (하위는 상위를 모른다)**: `MemberReader` 는 `MemberService` 의 존재를 알아선 안 된다. Implement/Repository 는 상위 타입을 import 하지
   않는다.
3. **레이어 건너뛰기 금지**: **Service 는 Repository 를 직접 참조하지 않는다.** 데이터 접근은 반드시 Implement 를 경유한다.
4. **동일 레이어 격리 — 단, Implement 는 예외**: Service 는 다른 Service 를, Repository 는 다른 Repository 를 가로로 참조하지 않는다. **Implement 만**
   재사용/조합을 위해 다른 도메인의 Implement 를 참조할 수 있다(예: `MemberWriter` 가 `MajorRepository`/`InterestRepository` 를 조합). 여러 Service
   조합이 필요하면 Assembler(§6)를 쓴다.

> 단일 모듈이라 컴파일러가 이 방향을 강제하지 못한다. 대신 **패키지·주입 규율로 스스로 지킨다.**
> 빠른 자가 점검: *Service 에 `Repository` 필드가 있으면 규칙 3 위반, Implement 에 `Service` import 가 있으면 규칙 2 위반이다.*

### 2.3 레이어 간 반환 타입

- **Web ↔ Controller**: 컨트롤러는 `service/request`·`service/response` DTO 를 그대로 쓴다(별도 controller DTO 는 필요할 때만 만든다).
- **Controller ↔ Service**: **Service 메서드는 요청 객체(`XxxRequest`)를 받고 응답 객체(`XxxResponse`)를 반환한다.**
    - 예: `MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request)`.
    - 도메인(=Entity)을 Controller 로 그대로 노출하지 않는다. 응답 DTO 로의 변환 책임은 **Service** 가 진다.
- **Service ↔ Implement**: Service 는 **Implement 만** 호출한다(Repository 직접 접근 금지, §2.2 규칙 3). Implement 는 **도메인(=Entity)**
  을 반환하고, Service 가 그것을 조합해 **응답 DTO 로 변환한 뒤** 반환한다.
- **Implement ↔ Repository**: Repository 는 Entity/Projection 을 입출력하고, Implement(주로 Reader)가 이를 받아 Aggregate 로 조립한다.
  Repository 는 판단하지 않는다.

> **주의:** 도메인과 Entity 가 통합되어 Entity 가 Service 계층까지 올라온다. 이 때문에
> **Entity 를 응답 경계(Controller) 밖으로 새어나가게 하지 않는 규율이 더 중요해진다.**
> Service 는 반드시 `XxxResponse` 로 변환해 반환하며, `@Entity` 타입을 반환 타입으로 노출하지 않는다.
> 지연로딩(LAZY) 프록시가 뷰까지 흘러가는 것을 막기 위해 `open-in-view: false` 를 유지한다.

### 2.4 Service / QueryService 분리 (CQRS-lite)

- **쓰기/명령**은 `XxxService`, **조회**는 `XxxQueryService` 로 클래스를 분리한다(예: `MemberService` + `MemberQueryService`).
- 읽기/쓰기 흐름이 한 클래스에 섞이지 않게 하여 트랜잭션 경계와 의존성을 단순화한다.
- 조회 Service 는 Reader(또는 QueryRepository 를 주입한 Reader)를 통해 읽기 전용 모델을 조립한다.

## 3. DTO 변환 & 검증

### 3.1 변환

- **요청 DTO → 도메인/명령 값**: 변환이 필요하면 요청 DTO(`record`) 안에 `toXxx()` 메서드를 둔다. 단순 전달이면 record 필드를 그대로 넘긴다.
- **도메인(=Entity) → 응답 DTO**: 응답 DTO(`record`)에 정적 팩토리 `of(...)` 를 둔다. **이 변환은 Service 안에서 호출**한다.
  ```java
  public record MemberProfileResponse(Long memberId, String nickname /* ... */) {
      public static MemberProfileResponse of(Member member) { ... }
  }
  ```
  ```java
  @Service
  @RequiredArgsConstructor
  public class MemberService {
      private final MemberReader memberReader;
      private final MemberWriter memberWriter;

      public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
          Member member = memberReader.readById(memberId);   // 어떻게 읽는지는 모름
          memberWriter.updateProfile(member, request);        // 어떻게 저장하는지는 모름
          return MemberProfileResponse.of(member);            // ★ 응답 변환은 Service 책임
      }
  }
  ```

### 3.2 검증

- 사용자 요청 값의 **형식/기초 검증은 요청 DTO 에서 Bean Validation** 으로 수행하고, 컨트롤러에서 `@Valid` 로 트리거한다.
  ```java
  public record AddMajorRequest(
          @NotNull(message = "학과 ID를 입력해주세요.") Long majorId,
          @NotNull(message = "전공 유형을 선택해주세요.") MajorType majorType
  ) {}
  ```
    - 검증 실패는 `MethodArgumentNotValidException`/`BindException` 으로 던져지고 `ApiControllerAdvice`(§9) 가 400 응답으로 변환한다.
- **핵심 비즈니스 규칙 검증**(중복, 소유권, 상태 전이 등)은 Service / Implement Layer / 도메인(=Entity) 내부에서 수행하고, 도메인별 `XxxExceptionMessage`(
  §8) 와 공통 예외를 사용한다.
  ```java
  private void validateNicknameNotDuplicated(String nickname) {
      if (memberRepository.existsByNickname(nickname)) {
          throw new BadRequestException(MemberExceptionMessage.NICKNAME_ALREADY_EXISTS.getMessage());
      }
  }
  ```

## 4. 도메인 모델 (= JPA Entity)

도메인 모델과 JPA Entity 를 **하나의 클래스로 사용한다.**

- 도메인 = `@Entity` 클래스. 개념 명사로 명명하며(`Member`), 시간 필드는 `BaseTimeEntity`(§7.2) 를 상속해 얻는다.
- 도메인 로직은 이 클래스 내부(메서드)에 둔다(Tell, Don't Ask). 예: `member.updateNickname(...)`, `member.delete()`.
- **생성은 정적 팩토리 메서드**로 한다. JPA 요구사항인 기본 생성자는 `protected` 로 숨긴다. Lombok(`@Getter` 등)을 활용한다.
- **setter 는 만들지 않는다.** 가변 상태는 `private` 필드 + 의도를 드러내는 메서드로만 변경한다.
- 필드가 많은 생성/조립은 **빌더 또는 명명된 정적 팩토리**로 필드 혼동을 막는다.
- **명령/값 객체(테이블에 매핑되지 않는 개념)는 `record`** 로 둔다. Entity 로 승격하지 않는다.
- 같은 Aggregate 는 연관관계 매핑, 다른 Aggregate 는 **ID 참조**(§7.6).

## 5. Implement Layer

Service 가 데이터 접근 기술이나 읽기/쓰기 분기 로직까지 알게 되는 것을 막기 위해, Service 하위에 **역할별 Implement Layer** 를 둔다. 모두 `@Component` 이며
`<domain>.service.implement` 패키지에 둔다. Implement 는 **재사용 가능한 도구**이며, 상세 구현을 이 계층이 **독점 소유**한다(§2.1).

### 5.1 세 계층의 협업 — 하나의 유스케이스

같은 흐름을 Service/Implement/Repository 로 어떻게 나누는지 보인다. **각자 아는 범위가 다르다.**

```java
// ── [Business] Service : "무엇을" 하는지(흐름)만 안다. Repository 를 모른다. ──────────────
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberReader memberReader;   // Implement 만 주입
    private final MemberWriter memberWriter;

    public void follow(Long followerId, Long followingId) {
        memberWriter.follow(followerId, followingId);   // 어떻게 검증·저장하는지는 모름
    }
}

// ── [Implement] Writer : "어떻게" 쓰는지 안다. 유일하게 Repository 를 안다. 트랜잭션 경계. ──
@Component
@RequiredArgsConstructor
@Transactional
public class MemberWriter {
    private final FollowRepository followRepository;

    public void follow(Long followerId, Long followingId) {
        validateCannotTargetSelf(followerId, followingId);          // 규칙 검증
        validateNotAlreadyFollowing(followerId, followingId);
        followRepository.save(Follow.of(followerId, followingId));  // 데이터 접근은 Repository 위임
    }
    // validateXxx(...) 는 private 로 모듈화
}

// ── [Data Access] Repository : "어디서" 가져오는지만 안다. 판단하지 않는다. ────────────────
public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
```

> **안티패턴(하지 말 것):** `MemberService` 가 `FollowRepository` 를 직접 주입받아 검증 + `save()` 를 한 메서드에 몰아넣는 것. 이 순간 Service 는 "흐름"이
> 아니라 "구현"을 알게 되고(§2.2 규칙 3 위반), 재사용도 불가능해진다.

### 5.2 역할 접미사 사전

cluverse 는 **`Reader`(조회·조립)** 와 **`Writer`(생성/수정/저장)** 를 기본으로 쓴다. 더 세분화가 필요하면 아래 어휘를 재사용한다.

| 접미사         | 책임                        | 예시                              |
|-------------|---------------------------|---------------------------------|
| `Reader`    | 복합 조회 + 도메인(Aggregate) 조립 | `MemberReader`, `FeedReader`    |
| `Writer`    | 쓰기(생성/수정/저장/삭제)           | `MemberWriter`, `PostWriter`    |
| `Manager`   | 상태/설정 관리 흐름               | `NotificationPreferenceManager` |
| `Validator` | 검증                        | (필요 시)                          |
| `Processor` | 여러 쓰기를 원자적으로 묶는 처리        | (필요 시, §5.4)                    |
| `Generator` | 생성기                       | (필요 시)                          |

> 핵심은 **읽기/쓰기/검증의 분리**다. 새 역할이 필요하면 위 어휘를 우선 재사용한다.

### 5.3 Repository 의존

- Implement Layer 는 **`<domain>.repository` 의 Repository 인터페이스를 생성자 주입**받는다(`@RequiredArgsConstructor`).
- **Reader 는 Entity(=도메인)를 그대로 반환한다.** 별도의 "순수 도메인 Repository 인터페이스"는 만들지 않는다.

### 5.4 트랜잭션 (최소 범위 · Implement 경계)

> **원칙: 트랜잭션은 "원자적으로 묶여야 할 DB 쓰기 묶음"에만 최소 범위로 건다.**
> 뚱뚱한 Service 트랜잭션은 외부 I/O(알림·PG·HTTP)까지 트랜잭션 안에 가둬 DB 커넥션을 불필요하게 점유하고, 외부 장애가 전체 리소스 고갈로 번지게 한다.

- **경계는 Implement 에.** 쓰기 트랜잭션은 쓰기 책임 Implement(주로 `Writer`)에 `@Transactional` 을 둔다(클래스 또는 필요한 메서드 레벨).
- **Service 에는 원칙적으로 트랜잭션을 걸지 않는다.** Service 는 흐름만 조립하고, 트랜잭션 밖 작업(외부 I/O)을 배치한다.
- **외부 I/O 는 트랜잭션 밖에 둔다.** 알림/SMS/PG 호출 등은 커밋 이후 실행한다. 보장이 필요하면 `@TransactionalEventListener(phase = AFTER_COMMIT)` 또는
  outbox 로 뺀다.
- **조회 전용**에는 쓰기 트랜잭션을 걸지 않는다. 지연로딩 조립이 필요하면 Reader 메서드에 `@Transactional(readOnly = true)` 를 건다. JPA 는
  `open-in-view: false` 로 동작한다.
- **더티체킹 주의:** 상태 변경은 엔티티가 영속(managed) 상태일 때만 반영된다. 따라서 **load 와 mutate 를 같은 트랜잭션 메서드 안에** 둔다. 트랜잭션 밖에서 `entity.xxx()` 로
  바꾸면 detached 라 조용히 무시된다.
- **자기호출 주의:** `@Transactional` 은 Spring 프록시를 통과하는 **외부 호출**에만 적용된다. 같은 클래스 내부 메서드 직접 호출로는 트랜잭션이 시작되지 않는다.
- 여러 컴포넌트의 쓰기를 하나로 묶어야 하면 전용 `Processor` 메서드로 그 묶음을 캡슐화하고 거기에 트랜잭션을 건다(남발하지 않는다).

### 5.5 도메인 간 참조 규칙

- 다른 도메인의 **Service 참조는 금지**, 다른 도메인의 **Implement/Repository 참조는 허용**한다(재사용성).
    - 예: `MemberWriter` 가 `MajorRepository`·`InterestRepository` 를 주입받아 존재 검증에 사용.
- 여러 Service 조합이 필요하면 Service 끼리 호출하지 말고 Assembler(§6)를 사용한다.

## 6. Assembler (Facade)

- Controller 가 **여러 Service 를 조합**해야 할 때만, 도메인에 `XxxAssembler`(`@Component`)를 둔다. (단순 위임이면 도입하지 않는다.)
- Assembler 도 **Service 와 마찬가지로 요청/응답 객체 기준**으로 조합한다. 각 Service 가 반환한 `XxxResponse` 를 조합하거나 상위 응답 DTO 로 합성한다.

## 7. Repository & Entity

### 7.1 Repository

- `public interface [도메인]Repository extends JpaRepository<[도메인], Long>` 형태. (제네릭 타입이 `XxxEntity` 가 아니라 도메인 클래스 `Xxx` 임에
  유의.)
- Aggregate Root 당 Repository 를 두는 걸 원칙으로 한다. 하위 엔티티 Repository 는 지양하되, 불가피하면 허용한다.
- **쿼리 우선순위(단순 → 복잡):**
    1. Spring Data 쿼리 메서드(`findByFollowerIdAndFollowingId`) — 엔티티/기본 조회.
    2. 조회 결과가 DTO 면 **인터페이스 Projection** 또는 `@Query`(JPQL) 생성자 Projection.
    3. 엔티티 반환이면서 복잡하면 `@Query`(JPQL) + fetch join. 조건이 정적이면 이 방식으로 충분하다.
    4. **동적 쿼리(런타임 조건 조합)·복잡 조인/집계/서브쿼리는 QueryDSL 로 분리한다.** 반환 형태에 따라 두 곳 중 하나에 둔다(§7.5):
        - 읽기 전용 **DTO(projection/summary)** 를 반환하면 → 독립 `[도메인]QueryRepository`.
        - **엔티티 fetch join·벌크 UPDATE·count** 처럼 메인 Repository 에 붙어야 하면 → `[도메인]RepositoryCustom` + `[도메인]RepositoryImpl` fragment.
- N+1 대응: `hibernate.default_batch_fetch_size` 전역 설정 + 필요 시 fetch join.

### 7.2 Entity (= 도메인)

- 모든 Entity 는 생성/수정 시각을 위해 `BaseTimeEntity` 를 상속한다. 시각은 Spring Data JPA Auditing 으로 자동 채운다.
  ```java
  @Getter
  @MappedSuperclass
  @EntityListeners(AuditingEntityListener.class)
  public abstract class BaseTimeEntity {
      @CreatedDate
      @Column(updatable = false, nullable = false)
      private LocalDateTime createdAt;

      @LastModifiedDate
      @Column(nullable = false)
      private LocalDateTime updatedAt;
  }
  ```
- `id` 는 각 Entity 에 `@Id @GeneratedValue(strategy = IDENTITY)` 로 둔다.
- 논리 삭제가 필요한 도메인은 상태 필드(예: `MemberStatus`)와 `delete()` 같은 의도 드러내는 메서드로 처리한다.
- 가변 상태는 `private` 필드 + 의도 드러내는 메서드로 변경한다. setter 금지.
- 제약조건(unique/index)은 `@Table(indexes = ...)` 또는 DB 스키마에서 관리한다.

### 7.3 Projection (조회 모델)

- 엔티티가 아닌 형태로 조회해야 할 때는 **DTO Projection** 으로 결합도를 낮춘다.
    - **인터페이스 Projection**: 단순/정적 조회의 기본(Spring Data).
    - **record Projection/Summary**: 생성자 기반. `@Query` 또는 QueryDSL `Projections` 로 채운다.
- 어느 수단으로 뽑든 결과 타입은 위 모델로 통일한다 — 위층(Reader→Service)은 수단을 몰라도 된다.

### 7.4 보안 컬럼

- 민감 컬럼은 `AttributeConverter` + 보안 프로퍼티로 암호화하여 저장한다. 관련 인프라 설정은 `common.config` 에 둔다.

### 7.5 동적·복잡 조회 (QueryDSL)

**QueryDSL 은 "필요할 때만" 꺼내는 도구다.** 동적 쿼리나 복잡한 조인·집계·서브쿼리가 필요할 때만 **QueryDSL 전용 클래스**로 분리한다. 단순/정적 조회는 Spring Data(
§7.1·§7.3)로 충분하다.

- **위치**: `<domain>.repository` 패키지, Repository 와 나란히 둔다.
- **네이밍**: **`[도메인]QueryRepository`** (예: `MemberQueryRepository`, `FeedQueryRepository`). `@Repository`,
  `JPAQueryFactory` 를 생성자 주입.
- **역할 분리**: `[도메인]Repository`(Spring Data) = CRUD·단순/정적 조회, `[도메인]QueryRepository`(QueryDSL) = 동적·복잡 조회.
- **반환 타입**: 읽기 전용 모델(record Summary/Projection)만 반환한다. 엔티티(=도메인)를 이 경로로 노출하지 않는다.
- **주입 규칙(§2.2)**: `QueryRepository` 는 Data Access 이므로 **Implement(주로 `Reader`)만 주입**한다. **Service 는 직접 주입하지 않는다.**
  Reader 가 `Repository` 와 `QueryRepository` 를 조합한다.
  ```
  Controller → QueryService → MemberReader(Implement) → MemberQueryRepository / MemberRepository → Summary
  ```
- **인프라 설정**: `JPAQueryFactory` 빈 정의(`QuerydslConfig`)는 `common.config` 에 둔다. Q 클래스는 `annotationProcessor` 로
  `build/generated` 에 생성한다.
- **주의**: `QueryRepository` 는 데이터 조회만 한다. 조합·가공·비즈니스 판단은 `Reader` 몫이다.

### 7.5.1 Repository 확장 fragment (QueryDSL — 엔티티·벌크)

QueryDSL 이 **읽기 전용 DTO 가 아니라 엔티티(fetch join)를 반환하거나, 벌크 UPDATE·count 처럼 메인 Repository 에
붙어야 하는** 경우가 있다. 이때는 독립 `QueryRepository`(§7.5) 대신 **Spring Data fragment** 로 메인 Repository 를 확장한다.
`QueryRepository` 는 "projection 을 반환하는 별도 조회 컴포넌트"이고, fragment 는 "메인 Repository 의 일부로 노출되는
QueryDSL 확장"이라는 점이 다르다.

- **구성**: `[도메인]RepositoryCustom`(인터페이스) + `[도메인]RepositoryImpl`(`JPAQueryFactory` 주입 구현).
  메인 Repository 가 `extends JpaRepository<[도메인], Long>, [도메인]RepositoryCustom` 로 fragment 를 합성한다.
  Spring Data 규약상 구현 클래스 이름은 반드시 **`[Repository 이름]Impl`** 이어야 한다.
- **위치**: `<domain>.repository` 패키지, 메인 Repository 와 나란히 둔다.
- **쓰임(현행 예)**:
    - 엔티티 fetch join: `PostRepositoryCustom.findWithImagesById` / `findAllWithImagesByIdIn`, `MemberRepositoryCustom.findBySocialAccount`.
    - 벌크 UPDATE(1차 캐시 flush/clear 포함): `CommentRepositoryCustom.increase/decreaseLikeCount·ReplyCount`.
    - 엔티티/집계 조회: `MemberRepositoryCustom.findByEmail`, `findMajorsByMemberId`, `countActivePostsByMemberId`.
- **주입 규칙**: fragment 는 메인 Repository 를 통해 노출되므로 §2.2 의 Repository 주입 규칙을 그대로 따른다 —
  Implement(`Reader`/`Writer`)가 `[도메인]Repository` 를 주입하면 fragment 메서드도 함께 사용한다.
- **선택 기준**: DTO(projection/summary) 반환이면 `QueryRepository`, 엔티티/벌크면 fragment. 정적·단순 fetch join 은
  `@Query`(JPQL, §7.1-3)로도 충분하므로 QueryDSL 이 꼭 필요할 때만 fragment 를 만든다.

### 7.6 연관관계 매핑

- 같은 애그리거트에 있는 엔티티는 연관관계 매핑, 다른 애그리거트에 있는 엔티티는 연관관계 매핑 X(id 로 참조).
- 연관관계 매핑이 필요한 경우, 양방향보다 **단방향 매핑**을 선호한다.
    - Aggregate Root 에 종속적인 관계를 제외하면 양방향 매핑은 지양한다.
- fetch 전략은 기본적으로 **LAZY**(예: `@ManyToOne(fetch = FetchType.LAZY)`). N+1 은 fetch join / batch fetch size 로 해결한다.
- NOT NULL·유니크 등 구체적 제약조건은 DB 스키마에서 관리한다. (`ddl-auto: none` 운영)

## 8. Enum & 예외 처리

### 8.1 Enum

- 도메인 Enum 은 해당 도메인의 `domain` 패키지에 둔다(예: `MemberStatus`, `MajorType`, `VerificationStatus` 는 `member.domain`).
- 여러 도메인이 공유해야 하는 Enum 만 `common` 으로 승격한다.
- 제한된 문자열 값은 항상 Enum 으로 표현하고, `@Enumerated(EnumType.STRING)` 로 저장한다.

### 8.2 예외 처리 (도메인별 메시지 + 공통 예외 타입)

- **공통 예외 타입**은 `common.exception` 에 둔다. HTTP 의미별로 나뉜 얇은 `RuntimeException` 이다.
    - `BadRequestException`(400), `UnauthorizedException`(401), `ForbiddenException`(403), `NotFoundException`(404).
- **예외 메시지는 도메인별로 관리**한다. 각 도메인의 `exception` 패키지에 `[도메인]ExceptionMessage` **enum** 을 두고 메시지를 모은다.
  ```java
  @Getter
  @RequiredArgsConstructor
  public enum MemberExceptionMessage {
      MEMBER_NOT_FOUND("존재하지 않는 회원입니다."),
      NICKNAME_ALREADY_EXISTS("이미 사용 중인 닉네임입니다.");
      private final String message;
  }
  ```
- 던질 때는 **공통 예외 + 도메인 메시지** 를 조합한다.
  ```java
  throw new NotFoundException(MemberExceptionMessage.MEMBER_NOT_FOUND.getMessage());
  ```
- **전역 핸들링**: `common.api.ApiControllerAdvice`(`@RestControllerAdvice`) 가 위 예외들과 Bean Validation 예외(
  `MethodArgumentNotValidException`/`BindException`)를 잡아 `ApiResponse`(§9) 로 변환하고, 그 외 `Exception` 은 500 으로 처리한다. 클라이언트
  예외는 `WARN`, 미처리 예외는 `ERROR` 로 로깅한다.

## 9. API 응답 포맷

- 모든 응답은 `ApiResponse<T>` 로 감싼다. `{code, status, message, data}` 형태이며, 팩토리 메서드로 생성한다.
  ```java
  ApiResponse.ok(data);        // 200 + data
  ApiResponse.ok();            // 200, data 없음
  ApiResponse.created(data);   // 201 + data
  ApiResponse.of(status, data);
  // 실패 응답(badRequest/unauthorized/forbidden/notFound/error)은 ApiControllerAdvice 가 생성
  ```
- **컨트롤러는 Service 가 반환한 `XxxResponse` 를 그대로 `ApiResponse.ok(...)` 로 감싼다.** 응답 DTO 변환(`of`)은 Service 에서 이미 끝났으므로(§2.3,
  §3.1) 컨트롤러에서 재변환하지 않는다.
  ```java
  @RestController
  @RequestMapping("/api/v1/members")
  @RequiredArgsConstructor
  public class MemberController {
      private final MemberQueryService memberQueryService;
      private final MemberService memberService;

      @PutMapping("/me/profile")
      public ApiResponse<MemberProfileResponse> updateProfile(
              @Login LoginMember loginMember,
              @RequestBody @Valid UpdateProfileRequest request) {
          return ApiResponse.ok(memberService.updateProfile(loginMember.memberId(), request));
      }
  }
  ```
- 인증이 필요한 요청은 `@Login LoginMember`(ArgumentResolver) 로 로그인 회원을 주입받는다.
- 페이지 응답은 페이지 응답 DTO 를 사용한다.

## 10. 테스트 코드

- **Controller 테스트**는 Spring REST Docs 를 활용하여 API 문서와 함께 작성한다(`XxxControllerDocsTest`).
- **Service 테스트**는 Mockito 를 활용하여 단위 테스트를 작성한다.
    - Repository/Implement 를 mock 으로 주입하여 테스트한다.
    - mockistic 하게 작성하여 실제 동작보단 행동 검증에 초점을 맞춘다.
- **Implement 테스트**(예: `MemberReaderTest`, `PostReactionWriterTest`)도 필요 시 작성한다.
- **Repository 테스트**는 `@DataJpaTest` 로 작성한다. QueryRepository 는 테스트용 설정과 함께 검증한다.
- **given-when-then 패턴**을 활용한다(가능하면). 주석으로 `// given`, `// when`, `// then` 구분을 명확히 한다.

## 11. 기타 컨벤션

- setter 메서드는 지양한다. 상태 변경은 의도를 드러내는 메서드로 한다(예: `changeStatus()`, `updateNickname()`).
- Lombok 으로 getter, constructor 등을 자동 생성한다. 파라미터가 많은 생성자는 `@Builder` 를 활용한다. 로그는 `@Slf4j`.
- 불필요한 주석은 지양한다(Lombok 자동 생성 메서드에 대한 주석 등).
- 메서드 이름은 동사로 시작한다(`createXxx()`, `getXxxById()`). 정적 팩토리는 `of`/`from` 을 쓸 수 있다 — 기술적 객체(예: `ApiResponse`)는 `of`/`from`,
  도메인 생성은 동사형이 어울린다.
- 변수 이름은 명확하게 작성한다(`memberId`, `followingId` 등).
- 상수는 대문자 + `final` 로 작성한다(예: `private static final int MIN_PRIMARY_MAJOR = 1;`).
- 비즈니스 제약(예: 1개 이상)에서 매직 넘버를 직접 쓰지 않고 **상수로 관리**한다.
- 메서드 모듈화를 신경쓴다. `if (xxx) throw ...` 같은 검증은 `validateXxx()` 메서드로 분리한다.
- 도메인 로직은 최대한 도메인 모델 내부에 둔다(Tell, Don't Ask).
- JpaRepository 쿼리 메서드 이름이 너무 길어지면 QueryDSL(`XxxQueryRepository`)로 옮긴다.
