## S02. 전반적인 동작 원리

### 사용자의 요청 -> 컨트롤러(시큐리티 의존성이 없는 경우)
- 클라이언트의 요청은 서버 컴퓨터의 WAS(톰캣)의 **필터들을 통과한 뒤** 스프링 컨테이너의 컨트롤러에 도달함

### 시큐리티 의존성 추가: 사용자의 요청을 감시하려면?
- 스프링 시큐리티가 사용자의 요청을 감시하고 통제하는 지점
    - WAS의 필터단에서 요청을 가로챈 후, 시큐리티의 역할을 수행함

- 엄밀한 정의
    - WAS의 필터에 하나의 필터를 만들어서 넣고 해당 필터에서 요청을 가로챔
    - 해당 요청은 **스프링 컨테이너 내부**에 구현되어 있는 스프링 시큐리티 감시 로직을 가짐
    - 시큐리티 로직을 마친 후 다시 WAS의 다음 필터로 복귀

### 스프링 시큐리시 감시 로직은 어떻게 구성되는가
- 여러 개의 필터들이 나열된 필터 체인 형태고 구성되어있음
    - 톰캣의 필터에서 요청을 가로채서
    - 스프링 컨테이너 내부에 시큐리티 로직 안에 여러 개의 필터를 거침
        - **Filter Chain**
        - 각각의 필터에서 CSRF, 로그아웃, 로그인, 인가 등 작업을 수행

### 주요 부분 용어
- DelegatingFilterProxy
    - 스프링 빈을 찾아 요청을 넘겨주는 서블릿 필터

- FilterChainProxy
    - 스프링 시큐리티 의존성을 추가하면 DelegatingFilterProxy에 의해 호출되는 SecurityFilterChain들을 들고 있는 Bean

- SecurityFilterChain
    - 스프링 시큐리티 필터들의 묶음으로 실제 시큐리티 로직이 처리되는 부분
    - FilterChainProxy가 SecurityFilterChain들을 들고 있음

---

## S03. DelegatingFilterProxy, FilterChainProxy

### 전체적인 그림
- DelegatingFilterProxy에서 요청을 가로챔
- FilterChainProxy에서 SecurityFilterChain들을 가지고 시큐리티 로직을 수행

### DelegatingFilterProxy
- 요청을 가로채서 FilterChainProxy라는 빈에 요청을 던져주는 매개체(Proxy) 역할만 함
- 서블릿 컨테이너(WAS)의 필터는 스프링에 등록된 빈을 알지 못함
- 서블릿 필터는 스프링 빈을 주입받아 사용할 수 없음
- 하지만 데이터베이스 접근, 각종 설정 등 다양한 스프링 빈을 필요로 함
- springSecurityFilterChain이라는 이름의 빈을 찾아서 요청 처리를 위임(FilterChainProxy)

### FilterChainProxy
- SpringSecurityFilterChain이라는 이름으로 등록되어있음
- 내부적으로 SecurityFilterChain들을 가지고 있음 
- 요청 URL을 분석하여, 자신이 가진 체인들 중 가장 먼저 매칭되는 단 하나의 SecurityFilterChain에게만 처리를 맡김

---

## SecurityFilterChain 등록

### 구조
- SecurityFilterChain 안에는 N개의 필터들이 있음
- FilterChainProxy는 이 SecurityFilterChain들을 여러 개 가지고 있고, 요청에 맡게 하나를 선택할 수 있음
- SecurityFilterChain 안에 N개의 필터들(로그인, 인가, 로그아웃, CSRF 등)이 시큐리티 로직을 수행함

### 커스텀 SecurityFilterChain 등록
- 스프링 시큐리티 의존성을 추가하면 기본적인 SecurityFilterchain 하나가 등록됨
    - 직접 수동 등록하면 사라짐
- 내가 원하는 SecurityFilterChain을 등록하기 위해서는 SecurityFilterChain을 리턴하는 @Bean 메소드를 등록하면 됨. (한 개 이상 등록 가능)
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain1(HttpSecurity http) throws Exception {
        // 필터 추가 등
        // 인가 작업 등
        return http.build();
    }

        @Bean
    public SecurityFilterChain filterChain2(HttpSecurity http) throws Exception {
        // 필터 추가 등
        // 인가 작업 등
        return http.build();
    }
}
```

### 멀티 SecurityFilterChain 중 하나 선택
- 기준
    - 등록 인덱스 순
    - 필터 체인에 대한 RequestMatcher 값이 일치하는지 확인(인가 설정 아님)

### 멀티 SecurityFilterChain 경로 설정: 필수
- N개를 처음 등록하면 모든 경로가 "/**"로 되어있음 
    -이때는 등록 인덱스 순으로 처리함

- .securityMatcher로 매핑을 해야함
- 등록 순 제어는 @Order(번호)로 할 수 있음
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 1. API 전용 필터 체인
     * - 대상: "/api/**" 경로
     * - 특징: 세션 미사용(STATELESS), CSRF 비활성화, 폼 로그인 비활성화
     */
    @Bean
    @Order(1) // 매우 중요: 이 체인을 먼저 검사하도록 우선순위 지정
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**") // 이 체인이 반응할 요청 URL 조건 설정
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public").permitAll()
                .anyRequest().authenticated()
            );
            // 실제로는 여기에 addFilterBefore() 등으로 JWT 필터를 추가합니다.
            
        return http.build();
    }

    /**
     * 2. 관리자 웹 페이지용 필터 체인
     * - 대상: "/admin/**" 경로
     * - 특징: 세션 사용, 폼 로그인 제공
     */
    @Bean
    @Order(2) // 우선순위 2번
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**") // 이 체인이 반응할 요청 URL 조건 설정
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(Customizer.withDefaults()); // 기본 폼 로그인 화면 제공
            
        return http.build();
    }
}
```

### 특정 요청은 피러를 거치지 않도록
- 시큐리티 작업이 딱히 필요하지 않은 경우, 굳이 할 필요없음
- 보통 정적 자원(이미지, CSS)의 경우 필터를 통과하지 않도록 아래 구문을 통해 설정할 수 있음
- 설정 시 하나의 SecurityFilterChain이 0번 인덱스로 설정되며 해당 필터 체인 내부에는 필터가 없는 상태로 생성됨
```java
@Bean
public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring().requestMatchers("/img/**");
}
```

---

## S05. SecurityFilterChain 구조

### 역할
- 보안, 인증/인가와 같은 주요 로직을 담당함
- 내부에 N개의 필터를 구성하여 각각의 필터가 하나의 로직(로그아웃, 로그인, 인가 등) 수행의 시작점이 됨
    - 실제 작업을 하는 것은 스프링 컨테이너 안에 있는 시큐리티 관련 Bean클래스들 모아둔 곳에서 진행함

### SecurityFilterChain에서 등록된 필터 확인
- @EnableWebSecurity debug 모드 설정
```java
@Configuration
@EnableWebSecurity(debug = true) // 반드시 개발 환경에서만!!
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain1(HttpSecurity http) throws Exception {
        // 필터 추가 등
        // 인가 작업 등
        return http.build();
    }
}
```
- 애플리케이션 실행 후, 아무 요청이나 보내면 터미널에 통과하는 securityFilterChain의 필터 목록이 출력됨
- 커스텀으로 SecurityFilterChain을 등록하면 활성화되어있는 필터가 별로 없음

### 스프링 시큐리티 제공 필터 간단 설명
- DisableEncodeUrlFilter: URL로 간주되지 않는 부분을 포함하지 않도록 설정

- WebAsyncManagerIntegrationFilter: 비동기로 처리되는 작업에 대해 알맞은 시큐리티 컨텍스트(세션)을 적용

- SecurityContextHolderFilter: 접근한 유저에 대해 시큐리티 컨텍스트 관리

- HeaderWriterFilter: 보안을 위한 응답 헤더 추가 (X-Frame-Options, X-XSS-Protection and X-Content-Type-Options)

- CorsFilter: CORS 설정 필터

- CsrfFilter: CSRF 방어 필터

- LogoutFilter: 로그아웃 요청 처리 시작점 GET : "/logout"

- UsernamePasswordAuthenticationFilter: username/password 기반 로그인 처리 시작점 POST : "/login"

- DefaultLoginPageGeneratingFilter: 기본 로그인 페이지 생성 GET : “/login”

- DefaultLogoutPageGeneratingFilter: 기본 로그아웃 페이지 생성 GET : “/logout”

- BasicAuthenticationFilter: http basic 기반 로그인 처리 시작점

- RequestCacheAwareFilter: 이전 요청 정보가 존재하면 처리 후 현재 요청 판단

- SecurityContextHolderAwareRequestFilter: ServletRequest에 서블릿 API 보안을 구현

- AnonymousAuthenticationFilter: 최초 접속으로 인증 정보가 없고, 인증을 하지 않았을 경우 세션에 익명 사용자 설정

- ExceptionTranslationFilter: 인증 및 접근 예외에 대한 처리

- AuthorizationFilter: 경로 및 권한별 인가 (구. filterSecurityIntercepter)

### SecurityFilterChain에 필터 활성/비활성화
- CSRF 필터 설정(기본적으로 활성화)
```java
http.cors((cors) -> cors.disable());
```

- CSRF 필터 설정(기본적으로 활성화)
```java
http.csrf((csrf) -> csrf.disable());
```

- Logout(기본적으로 활성화 / 비활성화 불가능)

- UsernamePasswordAuthenticationFilter 활성 비활성(기본적으로 비활성화)
```java
// 활성
http.formLogin(Customizer.witDefaults);

// 비활성
http.formLogin((login) -> login.disable());
```

> 기본적인 활성/비활성 + 비활성 불가능 한 것들로 구성되어있음이 핵심

### SecurityFilterChain에 커스텀 필터 등록
```java
// 기존 필터 이전
http.addFilterBefore(추가할 필터 객체, 기존필터.class);

// 기존 필터 위치
http.addFilterAt(추가할 필터 객체, 기존필터.class);

// 기존 필터 이후
http.addFilterAfter(추가할 필터 객체, 기존필터.class);
```
- addFilterAt은 같은 번호표를 발급하는 것(순서 보장 안됨)
- 거의 사용하지 않고, 기존 필터를 disable() 시켜서 완전히 없애버린 후, 그 빈자리에 내 필터를 넣을 경우 사용

---

## S06. SecurityContextHolder

### 상태 저장 필요
- SecurityFilterChain 내부에 존재하는 각각의 필터가 시큐리티 관련 작업을 기능 단위로 분업하여 진행함
- 작업 후 다른 필터가 그 내용을 알기 위한 저장소 개념이 필요함
- 예를 들어, 인가 필터가 작업을 완료하려면 ROLE에 대한 정보가 필요함
- 앞단의 필터에서 유저에게 ROLE값을 부여한 결과를 인가 필터까지 공유해야 확인할 수 있음

- SecurityContextHolder
    - SecurityContext: 유저 개수만큼 생성됨
        - **저장: Authentication 객체**: 한 개씩 생성

### Authentication
- Principal: 유저에 대한 정보
- Credentials: 증명(비밀번호, 토큰)
- Authorities: 권한(ROLE) 목록

- 권한 접근 예
```java
SecurityContextHolder.getContext().getAuthentication().getAuthorities()
```
> SecurityContextHolder의 메소드는 static으로 선언되기 때문에 어디서든 접근할 수 있음

- 특이사항
    - 다수의 사용자인 멀티 쓰레드 환경에서 SecurityContextHolder를 통해 SecurityContext를 부여하는 관리 전략은 위임하여 다른 클래스에게 맡김
    > 사용자별로 다른 저장소를 제공해야 인증 정보가 겹치는 일이 발생하지 않음

    - 즉, SecurityContextHolder는 SecurityContext들을 관리하는 메소드를 제공하지만 
    - 실제로 등록, 초기화, 읽기와 같은 작업은 SecurityContextHolderStrategy 인터페이스를 활용함

### SecurityContextHolderStrategy 구현 종료
```java
private static void initializeStrategy() {
    // 쓰레드 로컬 방식
    // ... 그 외 2개 더 존재
}
```

- 접근 쓰레드별 SecurityContext 배분
    - 톰캣 WAS는 멀티 쓰레드 방식으로 동작함
    - 유저가 접속하면 유저에게 하나의 쓰레드를 할당함
    - 각각의 유저는 동시에 시큐리티 로그인 로직을 사용할 수 있음
    > 이때 SecurityContextHolder의 필드에 선언된 SecurityContext를 호출하게 된다면 쓰레드 간 공유하는 메모리의 code 영역에 데이터가 있기 때문에 정보가 덮어지는 현상이 발생한다고 생각할 수 있는데, ThreeadLocal로 관리되기 때문에 쓰레드별 다른 구획을 나눠 제공함

### SecurityContext의 생명 주기
- Authentication 객체를 관리하는 SecurityContext는 사용자의 요청이 서버로 들어오면 생성되고, 처리가 끝난 후 응답되는 순간에 초기화됨
- 오해하면 안되는 사실
    - 세션 방식에서는 유지되는거 아닌가요?
    - 답: 필터가 요청을 가로챈뒤, 세션에서 인증정보(Authentication)을 찾음
    - 새로운 SecurityContextHolder를 생성하여 인증정보를 담음
    - 디스패처 서블릿을 지나 컨트롤러, 서비스 등을 돌아다님
        - @AuthenticationPrincipal등을 사용해서 정보를 꺼내서 사용할 수 있음
    - 이후 응답이 나가기 전 정보가 변경되면 갱신해서 다시 세션에 넣어둠
    - 이때 SecurityContext는 파기함

### 어디에서 사용하는가?
- 로그아웃 필터: 로그아웃 로직을 수행하면 SecurityContext의 Authentication 객체를 비움
- 로그인 필터: 인증을 완료한 뒤 유저 정보를 담은 Authentication 객체를 넣음

---

## S07. 필터 상속과 요청 전파
- 각각의 필터의 조상은 모두 동일함
- 필터의 기반이 되는 필터 클래스를 만들어두고 해당 클래스를 상속 받아 각 특성에 맞게 구현되어있음

### 필터의 상속
- 하나의 필터에 대한 모식도
    - 서블릿 Filter 인터페이스
    - GenericFilterBean 추상 클래스
    - 구현 1(예: 로그인)
    - 구현 2(필터로 등록. 예: 폼로그인)

- SecurityFilterChain에서 모식도
    - 간혹보면 GenericFilterBean기반으로 구현이 된 것도 있음(구현1로 바로 필터로 등록)
    - 간혹보면 OncePerRequestFilter기반으로 구현이 된 것도 있음(구현1로 바로 필터로 등록)(GenericFilterBean 상속)
    - 그 외 구현 1 - 구현 2(필터로 등록)로 된 것도 있음

- 상속의 이점
    - 중복되는 코드를 줄이고 각각의 구현부가 자신이 가지는 책임에 대해서만 작업을 수행
    - 가장 상단의 필터 클래스는 필터의 구조적인 역할만, 필터를 상속 받은 구현부는 구현부의 역할만 수행함
    - 로그인 필터의 경우, 로그인의 종류가 많기 때문에 기본적인 로그인 틀을 구현 1로 구체적인 로그인을 구현 2로 구현함

### 필터의 형식
- Filter 코드
```java
public interface Filter {
    default void init();
    void doFilter();
    default void destroy();
}
```
- 인터페이스
    - init(): 서블릿 컨테이너 실행시 필터를 생성하고 초기화할 때 사용하는 메소드
    - doFilter(): 요청에 대한 작업 수행 및 다음 필터를 호출하는 메소드
    - destroy(): 서블릿 컨테이너 종료 시 초기화하는 코드

### filterChain에서 다음 필터 호출
- LogoutFilter의 예
```java
private void doFilter() {
    chain.doFilter(request, response)
}
```

- GenericFilterBean과 OncePerRequestFilter의 로직 수행 메소드
    - GenericFilterBean: doFilter()
    - OncePerRequestFilter: doFilterInternal()

---

## S08. GenericeFilterBean과 OncePerRequestFilter

### 필터의 상속
- SecurityFilterChain에 담겨 있는 필터는 GenericFilterBean 기반으로 구현된 필터가 있고
- GenericeFilterBean을 상속한 OncePerRequestFilter 기반으로 구현된 필터가 있음

### 차이점
- 기준은 클라이언트의 한 번의 요청 대해서임
- GenericeFilterBean은 내부적으로 동일한 필터를 여러 번 통과하더라도 통과한 수만큼 내부 로직이 실행됨
- OncePerRequestFilter는 내부적으로 동일한 필터를 여러 번 통과하더라도 첫 한 번만 내부 로직이 실행됨

### 블로그의 잘못된 내용
- redirect에서 OncePerReuqestFilter가 한 번 동작된다고 말하는데, 정확히는 틀린 내용
- 302는 사용자가 재요청을 보내고 응답을 주는 것이기 때문에 사용자의 요청이 2번 보내지는 것과 동일함
> 즉, OncePerRequestFilter가 의미하는 동작을 이루기 위해서는 redirect시에는 해당이 안되고 forward 상태만 해당됨

### 각 상태에 대해서 어떻게 동작이 되는가
- forward 상태
    - 한 번의 클라이언트 요청에 대해서 동일한 필터를 2번 탔지만
    - OncePerRequestFilter는 한 번 동작함
    - 사용자에게 날아가는 것이 아님

- redirect 상태
    - 다른 경로로 재요청하라는 의미이기 때문에 OncePerRequestFilter는 두 번 동작함

```java
@Controller
public class MyController {

    // 1. 포워드 방식
    @GetMapping("/forward-test")
    public String forwardTest() {
        // 내부적으로 /target-url 로 토스합니다.
        // 브라우저 URL은 계속 /forward-test 로 남아있습니다.
        return "forward:/target-url"; 
    }

    // 2. 리다이렉트 방식
    @GetMapping("/redirect-test")
    public String redirectTest() {
        // 브라우저에게 /target-url 로 다시 접속하라고 명령합니다.
        // 브라우저 URL이 /target-url 로 바뀝니다.
        return "redirect:/target-url"; 
    }
}
```

--- 

## S09. DisableEncodeUrlFilter(ext. OncePerRequestFilter)

### 목적
- DefaultSecurityFilterChain에 기본적으로 등록되는 필터로 가장 첫 번째에 위치함
- URL 파라미터에 세션 id가 인코딩되어 로그로 유출되는 것을 방지하기 위함
    - request에 초점이 아닌, response에 초점을 둠
- 커스텀 SecurityFilterChain을 생성해도 등록되며 비활성은 아래와 같이 세션 관리 설정을 disable하면 됨
```java
http.sessionManageMent((manage) -> manage.disable()) // JWT 사용 시 주로 사용
```

### 실제 동작
```java
filterChain.doFilter(request, new DisableEncodeUrlResponseWrapper(response))
```
- 세션 아아디가 노출되는 것을 막을 수 있음(순수 url만 반환)
    - String encodedRedirectURL(String url);
    - String encodeURL(String url);

---

## S10. WebAsyncManagerIntegraionFilter

### 목적
- DefaultSecurityFilterChain에 기본적으로 등록되는 필터로 두 번째에 위치함
- 커스텀 SecurityFilterChain을 생성해도 두 번째에 위치함
- 비동기 작업을 수행할 때 서블릿 입출력 쓰레드와 작업 쓰레드와 동일한 SecurityContextHolder의 SecurityContext에 접근할 수 있는데, 비동기 방식의 경우 하나의 작업을 2개의 쓰레드로 수행하기 때문에 이 부분을 보완하기 위한 필터가 존재함
    - SecurityContextHolder는 하나의 쓰레드에서만 참조할 수 있게 쓰레드로컬로 만드는데
    - 이 필터가 두 개의 쓰레드에서도 동일한 SecurityContextHolder에 접근할 수 있게 도와줌

- 즉, SecurityContextHolder는 ThreadLocal 전략에 따라 동일한 쓰레드에서만 SecurityContextHolder에 접근할 수 있는데, 비동기 방식의 경우 하나의 작업을 2개의 쓰레드로 수행하기 때문에 이 부분을 보완하기 위한 필터

### Callable 사용시 쓰레드
```java
@GetMapping("/async")
@ResponseBody
public Callable<String> asyncPage() {

    // 서블릿 입출력 쓰레드
    System.out.println("start" + SecurityContextHolder.getContext().getAuthentication().getName());


    // 작업 쓸레드
    return () -> {
        Thread.sleep(4000);
        System.out.println("end" + SecurityContextHolder.getContext().getAuthentication().getName());

        return "async";
    };
}
```

### 서블릿단에서 비동기 처리인데 어떻게 필터단에서 판단
- 비동기는 컨테이너의 컨트롤러 그 안에서 일어나는데 어떻게 필터(이것도 컨테이너긴 함)가 그 작업을 미리 아는 것인가?
- 실제로 WebAsyncManageIntegrationFilter가 실제로 수행하는 작업과 Callable의 동작 방식에 관련이 있음

### WebAsyncManageIntegrationFilter가 실제로 수행하는 작업
- 현재 쓰레드의 SecurityContext를 다룰 수 있는 
- SecurityContextCallableProcessingInterceptor를 WebAsyncManager에 등록만 진행함
- 이후 서블릿단에서 WebAsyncManager를 통해 새로운 쓰레드에 SecurityContext를 전달함
    - 아직 비동기를 사용할지는 모름
    - 비동기를 사용하게 된다면 Interceptor를 가지고 기존 스레드의 SecurityContext를 새 스레드로 복사해줌

### Callable 동작 방시과 DispatcherServlet
- 사용자의 요청은 필터단을 모두 거친 후 스프링 컨테이너에서 컨트롤러에 접근하게 됨
- 이때 컨트롤러 바로 전에 DispatcherServlet이라는 서블릿이 존재하는데 사용자의 요청과 알맞는 컨트롤러를 찾는 역할을 수행함

- Callable 수행 과정
    - DispatcherServler에서 알맞은 Controller를 찾아서 요청 전달
    - Controller에서 요청 수행 후 Callbale 부분을 DispatcherServlet으로 리턴
    - DispatcherServlet은 Callable 객체를 WebAsyncManager에게 전달
    - WebAsyncManager가 비동기 부분을 새로운 쓰레드에서 수행 후 응답

- WebAsycnManager는 WebAsyncManagerIntegrationFilter에 의해 기존 쓰레드가 참조하던 SecurityContext를 전달 받았기 때문에 Callable을 수행할 새로운 쓰레드에게 기존 SecurityContext를 전달할 수 있음

### 주의사항
- @Async는 이게 아님
- 추가적인 작업을 진행해야함
- **Callable 방식에서만 사용 가능**

---

## S11. SecurityContextHolderFilter

### 목적
- DefaultSecurityFilterChain에 기본적으로 등록되는 필터로 세 번째에 위치함
- 이전 요청을 통해 이미 인증한 사용자가 정보를 현재 요청의 SecurityContextHolder의 SecurityContext에 할당하는 역할을 수행하고, 현재 요청이 끝나면 응답이 될 때, SecurityContext를 초기화
- 커스템 SecurityFilterChain을 생성해도 등록되며, 비활성화 할 수 있음(굳이?)
    - SecurityContext 초기화가 주 목적

### SecurityContextHolderFilter 클래스
- 기본 로직
    - 이전 요청의 사용자가 로그인을 했고, STATELESS 상태가 아니라면
    - 서버는 세션(메모리)또는 레디스와 같은 저장 매체에 유저의 정보가 있음

    - 해당 저장 매체로부터 유저 정보를 가져올 때, **SecurityContextRepository**를 통해서 가져옴(loadDeferredContext(request))
        - Redis / Session 등 가져오는 방식은 각각 다름(인터페이스, 추상화)

    - 불러온 유저 정보를 SecurityContextHolder에 초기화를 시켜줌(넣어준다는 의미)
    - 응답이 이루어지면 SecurityContextHolder를 다시 초기화 시켜줌(비워준다는 의미)

### SecurityContextRepository 인터페이스와 그 구현들
- 서버 세션이나 레디스와 같은 저장 매체로부터 유저 정보를 불러오는 SecurityContextRepository가 인터페이스로 정의된 이유는 세션, 레디스 매체별로 구현 방식이 다름

- HttpSessionSecurityContextRepository
- NullSecurityContextRepository: 아무 작업을 하지 않을때(JWT를 사용해서 STATELESS 명시)
- RequestAttributeSecurityContextRepository: HTTP request 저장 기반 구현cp
- 기타: 직접 구현해서 사용

### 기본 구현체와 커스텀 등록 방법
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http
            .securityContext((context) -> context
                    .securityContextRepository(new RequestAttributeSecurityContextRepository()));

    return http.build();
}
```

### SecurityContextPersistenceFilter와 SecurityContextHolderFilter의 차이
- SecurityContextHolderFilter가 SecurityContextPersistenceFilter의 후속으로 시큐리티 5.8 버전 부터 내부 구현이 변경되면서 기존 클래스는 deprecated됨
- 변경 부분
    - 구현들이 많이 변경되었지만 기능은 거의 동일하다. 다만 주의 깊게 볼 부분은 try문에서 finally에 존재하는 doFilter 응답 부분에 대해서 **변경점 저장 로직**임
    - 이전 클래스는 SecurityContext가 변경되면 변경된 부분을 세션이나 레디스와 같은 저장소에 SecurityContextRepository로 저장했지만 현재 클래스는 변경점을 저장하지 않음

> 로그인 이후, 뭔가 바뀌었다면 따로 분리해서 저장을 해주어야함!!!

```java
@PostMapping("/login")
public String myLogin(...) {
    // 1. 아이디 비번 확인 후 인증 객체(auth) 생성
    // 2. SecurityContext에 넣기 (스레드에 명찰 달기)
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    // [과거 버전]: 여기까지 쓰면 끝! 나갈 때 필터가 알아서 세션에 저장해 줌.

    // [최신 버전 (6.x)]: 이 코드를 무조건 추가해야 세션에 저장됨! 
    securityContextRepository.saveContext(context, request, response);
    
    return "success";
}
```

### 추가 사항
- 수동 로그인 일 경우
```java
@PostMapping("/login")
public String myLogin(...) {
    // 1. 아이디 비번 확인 후 인증 객체(auth) 생성
    // 2. SecurityContext에 넣기 (스레드에 명찰 달기)
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    // [과거 버전]: 여기까지 쓰면 끝! 나갈 때 필터가 알아서 세션에 저장해 줌.

    // [최신 버전 (6.x)]: 이 코드를 무조건 추가해야 세션에 저장됨! 
    securityContextRepository.saveContext(context, request, response);
    
    return "success";
}
```

- 로그인 이후 권한 변경
```java
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    // 세션에 저장하기 위한 레포지토리 (6.x 버전 필수)
    private final SecurityContextRepository securityContextRepository = 
            new HttpSessionSecurityContextRepository();

    public void updateUserRoleToAdmin(HttpServletRequest request, HttpServletResponse response) {
        
        // 1. DB에서 실제 사용자의 권한을 ADMIN으로 업데이트하는 로직 (생략)
        // userRepository.updateRole(userId, Role.ADMIN);

        // ---------------------------------------------------------
        // 2. 현재 스레드(SecurityContext)에 있는 기존 인증 정보 꺼내기
        // ---------------------------------------------------------
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication oldAuth = context.getAuthentication();

        // 3. 새로운 권한 리스트 만들기
        List<GrantedAuthority> newAuthorities = new ArrayList<>(oldAuth.getAuthorities());
        newAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN")); // 새 권한 추가

        // 4. 새로운 인증 객체 생성 (기존 사용자 정보 + 기존 비밀번호 + 새로운 권한)
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                oldAuth.getPrincipal(), 
                oldAuth.getCredentials(), 
                newAuthorities
        );

        // 5. SecurityContext에 새로운 인증 객체 갈아 끼우기
        context.setAuthentication(newAuth);

        // ---------------------------------------------------------
        // 6. 변경된 SecurityContext를 세션(사물함)에 덮어쓰기 저장! (가장 중요)
        // ---------------------------------------------------------
        securityContextRepository.saveContext(context, request, response);
    }
}
```

---

## S12. HeaderWriterFilter


### 목적
- DefaultSecurityFilterChain에 기본적으로 등록되는 필터로 네 번째에 위치함
- HTTP 응답 헤더에 사용자 보로를 위한 시큐리티 관련 헤더를 추가하는 필터
- 커스텀 SecurityFilterChain을 생성해도 등록되며 비활성화도 가능
```java
http.headers((headers) -> headers.disable());
```

### 헤더 목록
- X-Content-Type-Options: 컨텐츠 스니핑을 막기 위해 nosniff value를 할당해 서버에서 응답하는 Content-Type과 다른 타입일 경우 읽지 못하도록 설정

- X-XSS-Protection: XSS 공격 감지시 로딩 금지 (0은 비활성화)

- Cache-Control: 이전에 받았던 데이터와 현재 보낼 데이터가 같다면 로딩에 대한 결정 여부

- Pragma: HTTP/1.0 방식에서 사용하던 Cache-Control

- Expires: 서버에서 보낼 데이터를 브라우저에서 캐싱할 시간

- X-Frame-Options: 브라우저가 응답 데이터를 iframe, frame, embed, object 태그에서 로딩해도 되는 여부

### 커스텀
- header에 우리가 원하는 값도 넣어줄 수 있음
```java
http.headers((headers) -> headers
        .frameOptions(frameOptions -> frameOptions.sameOrigin())
        .cacheControl(cache -> cache.disable())
        .contentTypeOptions(contentTypeOptions -> contentTypeOptions.disable())
);
```
​
---

# S13. CORS Filter

### 들어가며
- API 서버를 구축하여 프론트와 백엔드의 오리진이 다르면 발생하는 CORS 문제를 해결해야함
- 토이나 실무 모두 문제를 해결을 위해 등록되는 SecurityFilterChain의 CorsConfigurationSource 값을 설정하는 것이 더 중요하기 때문에 CorsFilter가 상대적으로 덜 중요하지만 중요함

### 목적
- DefaultSecurityFilterChain에 기본적으로 등록되는 필터로 다섯 번째에 위치함
- CorsConfigurationSoucre에 설정한 값에 따라 필터단에서 응답 헤더를 설정하는 필터
- 커스텀 SecurityFilterChain을 설정해도 활성화됨 / 비활성화 가능

### CorsConfigurationSource 설정 방법
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		http.cors(corsCustomizer -> corsCustomizer.configurationSource(new              CorsConfigurationSource() {

                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                        CorsConfiguration configuration = new CorsConfiguration();

                        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
                        configuration.setAllowedMethods(Collections.singletonList("*"));
                        configuration.setAllowCredentials(true);
                        configuration.setAllowedHeaders(Collections.singletonList("*"));
                        configuration.setMaxAge(3600L);

                        configuration.setExposedHeaders(Collections.singletonList("Set-Cookie"));
                        configuration.setExposedHeaders(Collections.singletonList("Authorization"));

                        return configuration;
                    }
                }));

    return http.build();
}
```

---

## S14. CsrfFilter

### CSRF 공격
- 사용자의 의지와 무관하게 해커가 강제로 사용자의 브라우저를 통해 서버측으로 특정한 요청을 보내도록 공격하는 방법
- Session 방식일 때 공격이 발생할 수 있음
    - 브라우저에 들고 있는 쿠키 값을 가지고 서버측으로 요청을 보내기 때문

### 목적
- DefaultSecurityFilterChain에 기본적으로 등록되는 필터로 여섯 번째에 위치함
- CSRF 공격 방어를 위해 HTTP 메소드 중 GET, HEAD, TRACE, OPTIONS 메소드를 제외한 요청에 대해서 검증을 진행
- 토큰 방식, 요청시 토큰을 서버 저장소에 저장후 클라이언트에게도 전송되며, 그 후 해당하는 요청에 대해서 서버에 저장된 토큰과 비교 검증을 진행함

### 주요 로직
- doFilterInternal
```java
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		
	// 토큰을 토큰 저장소로 부터 불러옴
	DeferredCsrfToken deferredCsrfToken = this.tokenRepository.loadDeferredToken(request, response);
	// 다음 요청을 위해 request에 추가
	request.setAttribute(DeferredCsrfToken.class.getName(), deferredCsrfToken);
	this.requestHandler.handle(request, response, deferredCsrfToken::get);
	
	// HTTP 메소드 확인 후 CSRF 검증이 필요 없는 메소드면 다음 필터로 넘김
	if (!this.requireCsrfProtectionMatcher.matches(request)) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace("Did not protect against CSRF since request did not match "
					+ this.requireCsrfProtectionMatcher);
		}
		filterChain.doFilter(request, response);
		return;
	}
	
	// 서버 저장 토큰
	CsrfToken csrfToken = deferredCsrfToken.get();
	// 클라이언트에서 온 토큰
	String actualToken = this.requestHandler.resolveCsrfTokenValue(request, csrfToken);
	
	// 클라이언트로 부터 온 토큰과 서버 저장소의 토큰을 비교 검증
	if (!equalsConstantTime(csrfToken.getToken(), actualToken)) {
		boolean missingToken = deferredCsrfToken.isGenerated();
		this.logger
			.debug(LogMessage.of(() -> "Invalid CSRF token found for " + UrlUtils.buildFullRequestUrl(request)));
		AccessDeniedException exception = (!missingToken) ? new InvalidCsrfTokenException(csrfToken, actualToken)
				: new MissingCsrfTokenException(actualToken);
		this.accessDeniedHandler.handle(request, response, exception);
		return;
	}
	
	// 다음 필터로 넘김
	filterChain.doFilter(request, response);
}
```
- DeferredCsrfToken deferredCsrfToken = this.tokenRepository.loadDeferredToken(request, response); 토큰을 하나 불러옴

### CsrfTokenRepository
- CSRF 토큰의 생성 및 관리는 CsrfTokenRepository라는 인터페이스를 정의하고 그것을 구현한 클래스에게 위임
    - HttpSessionCsrfTokenRepository: 서버 세션에 토큰을 저장 관리함
    - CookieCsrfTokenRepository: 쿠키에 토큰을 저장 관리함
    - 직접 구현 가능

- 토큰 저장소 직접 설정
```java
http.csrf((csrf) -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()));
```

### CSRF 토큰 클라이너트측으로 발급
- 기본 동작은 SSR 세션 방식으로 설정되어있음
- STATELESS REST API에서는 사용할 일이 거의 없음
- Controller 단에서 VIEW 단 응답시 HTML form 영역에 서버에 저장되어있는 _csrf 토큰 값을 넣어주면 됨

### CSRF Referer
- STATELESS한 API 서버를 구축하게 된다면 JSESSION에 대한 서버 세션이 상태를 가지지 않기 때문에 CSRF 공격 위험 자체가 없음
- 하지만 JWT를 쿠키에 저장할 경우 CSRF 공격의 위험이 있을 수 있기 때문에 활성화하는 것이 좋음
- 다만 CSRF 토큰을 발급할 VIEW 페이지와 같은 로직이 없기 때문에 토큰 방식이 아닌 Referer 방식을 사용
- 이 방식은 HTTP Referer 헤더를 통해 요청의 출발점, 이전 URL등을 검증함

---

## S15. LogoutFilter

### 목적
- DefaultSecurityFilterChain에 기본적으로 등록되는 필터로 일곱 번째에 위치함
- 사용자가 로그인이 된 후 사용자 식별 정보가 서버에 남아있음
- 사용자가 로그아웃을 한다면, 로그아웃 핸들러를 돌며 로그아웃을 수행하는 필터
- 기본적으로 세션 방식에 대한 로그아웃 설정이 되어있기 때문에 JWT 방식이나 추가할 로직이 많을 경우 커스텀해야함
- 커스텀시에도 등록됨
    - 커스텀 로그아웃을 구현할 시 비활성화도 가능

### LogoutFilter
- 사용자의 로그아웃 요청이면
    - 수행
        - 로그아웃 핸들러를 동작
        - 쿠키 삭제
        - SecurityContext를 삭제 등등 수행
        - 다음 필터로 넘어가지 않고 return!!
    - 아니면 다른 필터 수행

### handler
- CompositeLogoutHandler를 호출하도록 설정이 되어있음

### 커스텀 로그아웃 핸들러
```java
CookieClearingLogoutHandler cookies = new CookieClearingLogoutHandle("our-custom-cookie");
http.logout((logout) -> logout.addLogoutHandler(cookies));
```

## 로그아웃 성공 핸들러
- 로그아웃이 성공한 뒤 URL 리디렉션과 같은 값들을 수행하는 것
- 이거를 가지고 커스텀을 하면 됨
```java
public interface LogoutSuccessHandler {

	void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException;

}
```

---

## S16. UsernamePasswordAuthenticationFilter(매우 중요)

### 목적
- DefaultSecurityFilterChain에 기본적으로 등록되는 여덟 번째에 위치함
- post /login 경로에서 form 기반으로 인증을 진행할 수 있도록 multipart/form-date 형태의 username/password 데이터를 받아 인증 클래스에게 값을 넘겨주는 역할을 수행
- **커스텀 SecurityFilterChain을 생성하면 자동 등록이 안되기 때문에 아래 구문을 통해서 필터를 활성화 시켜줘야함**
```java
http.formLogin(Customizer.withDefaults());
// 여기에 내가 작성한 걸 넣을 수 있음
```

### UsernamePasswordAuthenticationFilter 클래스
- doFilter가 없음
    - 부모 클래스인 AbstractAuthenticationProcessingFilter 클래스에 존재함

- 왜?
    - UsernamePasswordAuthenticationFilter는 Form 로그인 방식에 대한 필터
    - Form 데이터를 받은 후 인증 과정은 어떻게 될까?

- 과정
    - 사용자에게 데이터를 받아 인증 -> 인증 결과 -> 성공/실패 핸들

- 사용자가 보낸 데이터 방식이 다르다고 해서 위 과정이 변할까?
    - username/password를 Form, JSON 방식으로 보낸다고 해서 위 과정이 변하지 않음
    - 그래서 추상클래스로 정의되어있음

    - Form은 이미 UsernamePasswordAuthenticationFilter
    - JSON은 직접 커스텀
    - OAuth2은 OAuth2LoginAuthenticationFilter

### AbstractAuthenticationProcessingFilter
- doFilter
```java
private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		
	// 1. 로그인 경로 요청인지 확인
	if (!requiresAuthentication(request, response)) {
		chain.doFilter(request, response);
		return;
	}
	
	// 2. 로그인 과정 시도
	try {
		// **사용자로 부터 데이터를 받아 상황에 맞는 인증을 진행 (이 부분을 구현)**
		Authentication authenticationResult = attemptAuthentication(request, response);
		
		if (authenticationResult == null) {
			return;
		}
		// 인증 결과가 존재하면 세션 전략에 따라 SecurityContext에 저장
		this.sessionStrategy.onAuthentication(authenticationResult, request, response);
		
		// 아래 값이 설정되어 있으면 다음 필터로 넘김
		if (this.continueChainBeforeSuccessfulAuthentication) {
			chain.doFilter(request, response);
		}
		
		// 로그인 성공 핸들러
		successfulAuthentication(request, response, chain, authenticationResult);
	}
	// 3. 로그인 실패 핸들러
	catch (InternalAuthenticationServiceException failed) {
		this.logger.error("An internal error occurred while trying to authenticate the user.", failed);
		unsuccessfulAuthentication(request, response, failed);
	}
	catch (AuthenticationException ex) {
		unsuccessfulAuthentication(request, response, ex);
	}
}
```

### attemptAuthentication 추상 메서드 
```java
public abstract Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
		throws AuthenticationException, IOException, ServletException;
```
- Form/JSON/OAuth2

### AbstractAuthenticationProcessingFilter 추상 클래스의 구현체
- UsernamePasswordAuthenticationFilter
- OAuth2LoginAuthenticationFilter
- Saml2WebSsoAuthenticationFilter
- CasAuthenticationFilter

### UsernamePasswordAuthentication에서 attemptAuthentication()
```java
@Override
public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
		throws AuthenticationException {
		
	// 1. 로그인 경로 요청인지 확인
	if (this.postOnly && !request.getMethod().equals("POST")) {
		throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
	}
	
	// 요청으로부터 multipart/form-data로 전송되는 username, password 획득
	String username = obtainUsername(request);
	username = (username != null) ? username.trim() : "";
	String password = obtainPassword(request);
	password = (password != null) ? password : "";
	
	// 인증을 위해 위 데이터를 인증 토큰에 넣음
	UsernamePasswordAuthenticationToken authRequest = UsernamePasswordAuthenticationToken.unauthenticated(username,
			password);
	// Allow subclasses to set the "details" property
	setDetails(request, authRequest);
	
	// username/password 기반 인증을 진행하는 AuthenticationManager에게 인증을 요청 후 응답
	return this.getAuthenticationManager().authenticate(authRequest);
}
```

### 로그인이 수행되는 과정
- SecurityFilterChain에서 LoginFilter가 동작
- AuthenticationManager로 인증 요청 전달
    - 인증을 시작하는 인터페이스
    - 인터페이스 구현체(ProviderManager)가 인증 시작

- AuthenticationProvider에서 앞에서 보낸 데이터와 서버에 저장된 데이터 비교
    - 인증을 수행하는 인터페이스
    - DaoAuthenticationProvider: username/password 기반 인증 구현체
    - UserDetailsService에서 UserDetails를 가져와서 인증을 수행

---

## S17. DefaultLoginPageGeneratingFilter

### 목적
- 기본적인 아홉 번째에 위치
- GET: /loing 경로에 기본 로그인 페이지를 응답하는 역할을 수행
- 여러 로그인 설정에 의존되며 가장 많이 사용하는 formLogin에서는 커스텀 SecurityFilterChain 등록 시 아래와 같은 설정을 통해 사용할 수 있으며, 커스텀 로그인 페이지를 사용할 경우 제외됨
```java
// 기본 사용
http
        .formLogin(Customizer.withDefaults());
        
// 커스텀 하더라도 아래와 같이 loginPage() 메소드를 다루지 않으면 기본 로그인 페이지 활성
http
        .formLogin((login) -> login.loginPage("/커스텀경로"));
```

### 주요 로직
- doFilter
```java
private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		
	// Error 파라미터가 있는 경우
	boolean loginError = isErrorPage(request);
	
	// 로그아웃 성공시
	boolean logoutSuccess = isLogoutSuccess(request);
	
	// 조건에 맞으면 로그인 페이지 발생 로직 실행
	if (isLoginUrlRequest(request) || loginError || logoutSuccess) {
	
		// 로그인 페이지 생성
		String loginPageHtml = generateLoginPageHtml(request, loginError, logoutSuccess);
		// 헤더 값 설정 및 바디 추가
		response.setContentType("text/html;charset=UTF-8");
		response.setContentLength(loginPageHtml.getBytes(StandardCharsets.UTF_8).length);
		response.getWriter().write(loginPageHtml);
		return;
	}
	
	// 조건이 아닌 경우 다음 필터 실행
	chain.doFilter(request, response);
}
```

- 로그인 설정에 따른 활성화
    - form 로그인
    - oauth2 로그인
    - saml 로그인

### form 로그인 페이지
- 기본 설정은 GET: /login 경로

- 왜 필터단에서 페이지를 응답하는가?
    - 시큐리티 의존성의 한계 때문
    - 컨트롤러에 디폴트 페이지가 존재한다면, 커스텀할 컨트롤러가 디폴트 컨트롤를 유의하면서 구현하기 쉽지 않기 때문에 필터단에서 구현됨

---

## S18. DefaultLogoutPageGeneratingFilter

### 목적
- 열 번째에 위치함
- GET: /logout 경로에 대해 기본 로그아웃 페이지를 응답하는 역할을 수행함
- 여러 로그인 설정에 의존되며 가장 많이 사용하는 formLogin에서는 커스텀 SecurityFilterChain 등록시 아래와 같은 설정을 통해 사용할 수 있음
```java
// 기본 사용
http
        .formLogin(Customizer.withDefaults());
```
> 주의: CSRF 설정이 Enable 되어있어야 GET/ logout을 볼 수 있음

### 기타 : 성능을 올리기 위한 StringBuilder의 사용 모습
```java
private void renderLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
	StringBuilder sb = new StringBuilder();
	sb.append("<!DOCTYPE html>\n");
	sb.append("<html lang=\"en\">\n");
	sb.append("  <head>\n");
	sb.append("    <meta charset=\"utf-8\">\n");
	sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n");
	sb.append("    <meta name=\"description\" content=\"\">\n");
	sb.append("    <meta name=\"author\" content=\"\">\n");
	sb.append("    <title>Confirm Log Out?</title>\n");
	sb.append("    <link href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/css/bootstrap.min.css\" "
			+ "rel=\"stylesheet\" integrity=\"sha384-/Y6pD6FV/Vv2HJnA6t+vslU6fwYXjCFtcEpHbNJ0lyAFsXTsjBbfaDjzALeQsN6M\" "
			+ "crossorigin=\"anonymous\">\n");
	sb.append("    <link href=\"https://getbootstrap.com/docs/4.0/examples/signin/signin.css\" "
			+ "rel=\"stylesheet\" integrity=\"sha384-oOE/3m0LUMPub4kaC09mrdEhIc+e3exm4xOGxAmuFXhBNF4hcg/6MiAXAf5p0P56\" crossorigin=\"anonymous\"/>\n");
	sb.append("  </head>\n");
	sb.append("  <body>\n");
	sb.append("     <div class=\"container\">\n");
	sb.append("      <form class=\"form-signin\" method=\"post\" action=\"" + request.getContextPath()
			+ "/logout\">\n");
	sb.append("        <h2 class=\"form-signin-heading\">Are you sure you want to log out?</h2>\n");
	sb.append(renderHiddenInputs(request)
			+ "        <button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">Log Out</button>\n");
	sb.append("      </form>\n");
	sb.append("    </div>\n");
	sb.append("  </body>\n");
	sb.append("</html>");
	response.setContentType("text/html;charset=UTF-8");
	response.getWriter().write(sb.toString());
}
```
- StringBuilder를 사용하여 성능을 올림

---

## S19. BasicAuthenticationFilter

### 목적
- 열한 번째에 위치
- Basic 기반의 로그인 인증을 수행하기 위해 등록됨
- 커스텀 시 자동으로 등록되지 않음 / 활성화가 따로 필요함
```java
http
        .httpBasic(Customizer.withDefaults());
```

### Basic 인증이란?
- From과의 비교
- Form 인증
    - Form 태그에 username/password를 보내고 서버에서 세션 또는 JWT를 생성하여 사용자를 기억함

- Basic 인증
    - 브라우저에서 제공하는 입력기에 username/password를 입력하면 브라우저가 **매 요청시 BASE64 인코딩하여** Authorization 헤더에 넣어서 전송함
    - 사용자를 기억하지 않아도 됨
    - 단 매 요청마다 Authorization 헤더가 필수로 요구됨

- 하지만 스프링 시큐리티의 Basic인증 로직은 매번 재인증을 하지는 않음
- 세션을 만들어서 저장해서 유저를 기억함(Authorization 헤더는 매번 필요함)

### BasicAuthenticationFilter 클래스
```java
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws IOException, ServletException {
	
	try {
	
		// HTTP Authorization 헤더에서 값을 꺼냄 (없다면 다음 필터로 넘김)
		Authentication authRequest = this.authenticationConverter.convert(request);
		if (authRequest == null) {
			this.logger.trace("Did not process authentication request since failed to find "
					+ "username and password in Basic Authorization header");
			chain.doFilter(request, response);
			return;
		}
		
		// username 값을 가져옴
		String username = authRequest.getName();
		this.logger.trace(LogMessage.format("Found username '%s' in Basic Authorization header", username));
		
		// Security Context에 해당 username이 이미 존재하는지 확인 (여기서 인증을 진행)
		if (authenticationIsRequired(username)) {
			// 인증 진행
			Authentication authResult = this.authenticationManager.authenticate(authRequest);
			// 인증 결과를 Security Context에 저장
			SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
			context.setAuthentication(authResult);
			this.securityContextHolderStrategy.setContext(context);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(LogMessage.format("Set SecurityContextHolder to %s", authResult));
			}
			// Remember Me 서비스에 등록
			this.rememberMeServices.loginSuccess(request, response, authResult);
			// Security Context Repository에 저장
			this.securityContextRepository.saveContext(context, request, response);
			// 로그인 성공 핸들러
			onSuccessfulAuthentication(request, response, authResult);
		}
	}
	catch (AuthenticationException ex) {
		this.securityContextHolderStrategy.clearContext();
		this.logger.debug("Failed to process authentication request", ex);
		this.rememberMeServices.loginFail(request, response);
		onUnsuccessfulAuthentication(request, response, ex);
		if (this.ignoreFailure) {
			chain.doFilter(request, response);
		}
		else {
			this.authenticationEntryPoint.commence(request, response, ex);
		}
		return;
	}

	chain.doFilter(request, response);
}
```

---

## S20. RequestCacheAwareFilter

### 목적
- 열두 번째에 위치
- 이전 HTTP 요청에서 처리할 작업이 있고, 현재 요청에서 그 작업을 수행하기 위해 등록됨
- 이전에 처리하지 못한 작업을 다시 수행하기 위해서 사용
- 기본적으로 등록됨 / 비활성화 가능
```java
http
        .requestCache((cache) -> cache.disable());
```

### 동작 예시
1. 로그인하지 않은 사용자가 권한이 필요한 /my에 접근
2. 권한 없음, 예외 발생, 핸들러에서 /my 경로를 기억 후 핸들 작업 수행
3. 스프링 시큐리티가 /login 창을 띄움
4. username/password를 입력 후 인증을 진행
5. 로그인 이후 저장되어있은 /my 경로를 불러내서 실행

### 핸들 시점
- 2번에 핸들
- 예외를 처리하는 필터인 ExceptionTranslationFilter에서 ExceptionTranslationFilter 이후에 발생하는 예외들을 모두 받음
- AccessDeniedException과 같은 몇 예외 발생 시 내부 메소드 sendStartAuthentication() 메소드가 실행 여기서 requestCache를 저장함
```java
protected void sendStartAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
		AuthenticationException reason) throws ServletException, IOException {

	SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
	this.securityContextHolderStrategy.setContext(context);
	this.requestCache.saveRequest(request, response);
	this.authenticationEntryPoint.commence(request, response, reason);
}
```

### RequestCacheAwareFilter 클래스
- 저장된 requestCache를 확인 후 현재 요청에 적용시킴
```java
public class RequestCacheAwareFilter extends GenericFilterBean {

	private RequestCache requestCache;

	public RequestCacheAwareFilter() {
		this(new HttpSessionRequestCache());
	}
    // - 여기에 세션 아이디 기반으로 이전 요청 정보를 저장

	public RequestCacheAwareFilter(RequestCache requestCache) {
		Assert.notNull(requestCache, "requestCache cannot be null");
		this.requestCache = requestCache;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
			
		// request 캐시에서 이전 요청에 캐싱해둔 데이터가 있는지 조회
		HttpServletRequest wrappedSavedRequest = this.requestCache.getMatchingRequest((HttpServletRequest) request,
				(HttpServletResponse) response);
				
		// 있다면 다음 필터로 넘길때 현재 요청에 덮어 쓰기
		chain.doFilter((wrappedSavedRequest != null) ? wrappedSavedRequest : request, response);
	}
}
```

---

## S21. SecurityContextHolderAwareRequestFilter

### 목적
- 열세 번째 위치
- 사용자의 요청은 ServletRequest에 담겨서 날아옴
- ServlertRequest 요청에 스프링 시큐리티 API를 다룰 수 있는 메소드를 추가하기 위함
- 기본적으로 등록됨

### SecurityContextHolderAwareRequestFilter 클래스
- 추가되는 스프링 시큐리티 API 메소드
    - authenticate(): 사용자가 인증 여부를 확인하는 메소드
    - login(): 사용자가 AuthenticationManager를 활용하여 인증을 진행하는 메소드
    - logout(): 사용자가 로그아웃 핸들러를 호출할 수 있는 메소드
    - AsyncContext.start(): Callable를 사용하여 비동기 처리를 진행할 때 SecurityContext를 복사하도록 설정하는 메소드

---

## S22. AnonymousAuthenticationFilter

### 목적
- 열네 번째에 위치
- 여러 필터를 거치면서 현재 지점까지 SecurityContext 값이 null인 경우 Anonymout 값을 넣어주기 위해 사용됨
- 커스텀에서도 기본적으로 등록됨
- ROLE_ANONYMOUS로 등록됨


---

## S23. ExceptionTranslationFilter

### 목적
- 열다섯 번째 위치에 등록
- 이 필터 이후에 발생하는 인증, 인가 예외를 핸들링하기 위해서 사용됨
- 기본으로 등록됨

### 중요한 점
- 이 필터 이전에 발생하는 예외의 경우 처리하지 못함
- UsernamePasswordAuthenticationFilter는 얘보다 앞에 존재하기 때문에 처리할 수 없음

---

## S24. AuthorizationFilter
- 참고
    - RequestCacheAwareFilter에서 발생한 오류가 아니라
    - AuthorizationFilter에서 인가 작업 중 권한 없음이 발생하는 경우 발생하는 것

### 목적
- 마지막에 위치
- SecurityFilterChain의 authorizeHttpRequests()를 통해 인가 작업을 진행한 값에 따라 최종적으로 인가를 수행함
- 커스텀 SecurityFilterChain에도 기본적으로 등록되며 인가를 설정할 수 있음
```java
http
        .authorizeHttpRequests((auth) -> auth
                .requestMatchers("/").permitAll()
                .anyRequest().permitAll());
```

### AuthorizationFilter 클래스
```java
public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
		throws ServletException, IOException {

	HttpServletRequest request = (HttpServletRequest) servletRequest;
	HttpServletResponse response = (HttpServletResponse) servletResponse;

  // 특정 설정이 enable이고 이 필터가 이번 요청에서 이미 사용 되었다면 건너 뜀
	if (this.observeOncePerRequest && isApplied(request)) {
		chain.doFilter(request, response);
		return;
	}
	
	// 비동기 요청과 같은 특정 상황에서 인가 작업을 건너 뛸지 결정
	if (skipDispatch(request)) {
		chain.doFilter(request, response);
		return;
	}
	
	// 현재 필터가 이미 사용되었다면 값을 추가
	String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
	request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
	
	// 인가 작업 수행
	try {
		// 인가 매니저를 통해 인가 확인
		AuthorizationDecision decision = this.authorizationManager.check(this::getAuthentication, request);
		this.eventPublisher.publishAuthorizationEvent(this::getAuthentication, request, decision);
		
		if (decision != null && !decision.isGranted()) {
			// 인가 권한이 안맞다면 예외 발생
			throw new AccessDeniedException("Access Denied");
		}
		chain.doFilter(request, response);
	}
	finally {
		// 최종적으로 모든 작업 처리 후 사용 기록 삭제
		request.removeAttribute(alreadyFilteredAttributeName);
	}
}
```

---

## S25. SessionManagementConfigurer

### 목적
- 세션에 관련된 설정 및 초기화를 진행하는 Config 클래스
- 설정 목록
    - 시큐리티 필터
        - SessionManagementFilter
        - ConcurrentSessionFilter

    - 저장 및 공유 객체
        - RequestCache
        - SecurityContextRepository
        - InvalidSessionStrategy
        - AuthenticationTrustResolver

