# springimpl

`springimpl`은 Spring MVC와 DI 컨테이너의 핵심 흐름을 축소해서 직접 구현한 학습용 프로젝트다.  
애노테이션 기반 빈 등록, 간단한 의존성 주입, 요청 매핑, 컨트롤러 메서드 호출, JSON 파싱까지 하나의 흐름으로 연결되어 있다.

이 프로젝트는 실제 Servlet 컨테이너 위에서 동작하는 웹 애플리케이션이 아니다.  
`Scanner`로 콘솔 입력을 받아 `GET`, `POST`, `PUT`, `DELETE` 같은 요청을 흉내 내고, 그 요청을 내부 디스패처가 처리하는 구조다.  
즉, Spring Boot + DispatcherServlet 구조를 학습하기 위한 축소 구현에 가깝다.

## 구현 목표

- 애노테이션으로 컴포넌트와 컨트롤러를 구분한다.
- 애플리케이션 시작 시 클래스패스를 스캔해 빈을 등록한다.
- `@MyAutowired`를 이용해 필드 주입을 수행한다.
- 요청 메서드와 URI를 기준으로 컨트롤러 메서드를 찾는다.
- Path Variable과 JSON Body를 파라미터로 변환해 메서드를 호출한다.
- 호출 결과를 `UserResponse`로 감싸 출력한다.

## 실행 방법

### Gradle로 실행

```powershell
$env:GRADLE_USER_HOME="$PWD\.gradle-home"
.\gradlew.bat run
```

### 직접 컴파일 후 실행

```powershell
New-Item -ItemType Directory -Force -Path .\out | Out-Null
$files = Get-ChildItem .\src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -parameters -d .\out $files
java -cp .\out edu.pnu.myspring.test.TestApplication
```

`-parameters` 옵션이 필요한 이유는 메서드 파라미터 이름과 타입 정보를 유지해 요청 파라미터 바인딩에 활용하기 위해서다.

## 요청 예시

### GET 요청

입력:

```text
GET
/hello
```

결과:

```text
Response: UserResponse{data=Hello, MySpring World!, message='Success', success=true}
```

### POST 요청

입력:

```text
POST
/echo
{"id":1,"body":"abc"}
END
```

결과:

```text
Response: UserResponse{data=You sent: 1 - abc, message='Success', success=true}
```

종료하려면 다음과 같이 입력하면 된다.

```text
EXIT
```

## 전체 동작 흐름

1. `TestApplication.main()`이 시작점이 된다.
2. `MySpringApplicationRunner.run()`이 실행 클래스를 검사하고 기준 패키지를 결정한다.
3. `MyApplicationContext`가 클래스패스를 스캔해 빈을 등록하고 의존성 주입을 수행한다.
4. `ControllerRegistry`, `MyHandlerMapping`, `MyHandlerAdapter`가 컨트롤러 메서드 호출 준비를 마친다.
5. `RequestDispatcher`가 콘솔에서 입력받은 요청을 해석한다.
6. URI와 HTTP Method에 맞는 메서드를 찾는다.
7. Path Variable과 JSON Body를 메서드 인자로 변환한다.
8. 컨트롤러 메서드를 reflection으로 호출한다.
9. 반환값을 `UserResponse`로 감싸 콘솔에 출력한다.

## 패키지 구조

```text
src/main/java/edu/pnu/myspring
├─ annotations
├─ boot
├─ core
├─ dispatcher
├─ test
└─ utils
```

## 주요 코드 설명

### 1. 애플리케이션 시작

#### `edu.pnu.myspring.test.TestApplication`

프로젝트의 실행 진입점이다.

- `@MySpringApplication`이 붙어 있어 프레임워크가 시작 가능한 애플리케이션 클래스라고 인식한다.
- `main()`에서 `MySpringApplicationRunner.run(TestApplication.class, args)`를 호출한다.

#### `edu.pnu.myspring.boot.MySpringApplicationRunner`

실행 부트스트랩 역할을 한다.

- 전달된 클래스에 `@MySpringApplication`이 있는지 검사한다.
- 시작 클래스의 패키지명을 기준 패키지로 사용한다.
- `MyApplicationContext`를 생성해 빈 스캔과 등록을 시작한다.
- `RequestDispatcher`를 생성한 뒤 입력 루프를 시작한다.

Spring Boot의 `SpringApplication.run()`을 아주 단순한 형태로 흉내 낸 클래스라고 보면 된다.

### 2. DI 컨테이너

#### `edu.pnu.myspring.core.MyApplicationContext`

이 프로젝트에서 가장 핵심적인 클래스다.  
간이 IoC 컨테이너 역할을 수행한다.

주요 책임:

- 지정된 패키지 아래의 클래스를 스캔한다.
- 빈 후보인지 판단한다.
- 인스턴스를 생성하고 `beanRegistry`에 저장한다.
- 컨트롤러 빈은 별도로 `controllerClasses`에 보관한다.
- `@MyAutowired`가 붙은 필드를 찾아 의존성 주입을 수행한다.
- `getBean()`으로 타입 기반 조회를 제공한다.

구현 방식:

- 클래스패스 자원을 읽어 `file`과 `jar` 두 경우를 모두 처리한다.
- `.class` 파일을 reflection으로 로드한다.
- `@MyComponent`, `@MyRestController`, `@MyService`, `@MyRepository` 여부를 기준으로 빈 등록을 시도한다.
- 등록 후 `processAutowiring()`에서 필드 주입을 수행한다.

이 클래스는 Spring의 `ApplicationContext`와 `BeanFactory` 일부 기능을 학습용으로 단순화한 구현이다.

### 3. 컨트롤러 관리

#### `edu.pnu.myspring.dispatcher.ControllerRegistry`

컨트롤러 인스턴스를 꺼내는 중간 계층이다.

- `MyApplicationContext`에서 컨트롤러 빈을 가져온다.
- 한 번 조회한 컨트롤러는 `controllerCache`에 저장한다.
- 전체 컨트롤러 클래스 목록을 제공한다.

이 클래스 덕분에 `MyHandlerMapping`은 컨텍스트 내부 구조를 직접 알지 않고도 컨트롤러를 사용할 수 있다.

### 4. 요청 매핑

#### `edu.pnu.myspring.dispatcher.MyHandlerMapping`

요청 URI와 HTTP Method를 실제 자바 메서드에 연결하는 클래스다.

주요 책임:

- 컨트롤러 클래스의 메서드를 순회한다.
- `@MyRequestMapping`, `@MyPostMapping`이 붙은 메서드를 찾아 등록한다.
- `GET /hello` 같은 키를 만들어 메서드와 연결한다.
- URI 패턴을 정규식으로 바꿔 동적 경로를 매칭한다.
- `{id}` 같은 placeholder에서 path variable 값을 추출한다.
- 추출한 파라미터를 메서드 인자 타입에 맞게 변환한다.

예를 들어 `/users/{id}` 같은 URI가 있다면, 실제 요청 `/users/10`을 받아 `id=10`을 추출하는 역할을 담당한다.

현재 구현상 메서드 파라미터 바인딩은 `@MyPathVariable` 중심이며, JSON Body는 우선 `Map`으로 파싱한 뒤 같은 파라미터 맵에 합쳐 사용한다.

### 5. 핸들러 호출

#### `edu.pnu.myspring.dispatcher.MyHandlerAdapter`

찾아낸 핸들러 메서드를 실제로 호출하는 클래스다.

- 전달된 핸들러가 `Method`인지 검사한다.
- `@MyRequestMapping` 또는 `@MyPostMapping` 메서드인지 확인한다.
- `ControllerRegistry`를 통해 연결된 컨트롤러 인스턴스를 찾는다.
- reflection으로 메서드를 실행한다.
- 반환값을 `UserResponse.success()`로 감싸 반환한다.

Spring MVC의 `HandlerAdapter` 개념을 매우 단순하게 옮긴 구현이다.

### 6. 요청 디스패처

#### `edu.pnu.myspring.dispatcher.RequestDispatcher`

이 프로젝트의 요청 처리 중심부다.

주요 책임:

- `InputProvider`로부터 사용자 입력을 받는다.
- HTTP Method와 URI에 맞는 핸들러를 찾는다.
- `POST`, `PUT` 요청이면 JSON Body를 파싱한다.
- Path Variable과 Body 데이터를 하나의 파라미터 맵으로 합친다.
- `MyHandlerAdapter`에 메서드 호출을 위임한다.
- 결과 또는 에러를 콘솔에 출력한다.

실제 웹 서버의 `DispatcherServlet`과 같은 진입점 역할을 하지만, 이 프로젝트에서는 Servlet 대신 콘솔 입력 루프를 사용한다.

### 7. 요청/응답 모델

#### `edu.pnu.myspring.dispatcher.UserRequest`

사용자 입력을 내부 요청 객체로 표현한다.

- `method`
- `uri`
- `jsonBody`

세 가지 정보를 가지고 있으며, 콘솔에서 받은 입력을 프레임워크 내부가 다루기 쉬운 형태로 바꾼 DTO다.

#### `edu.pnu.myspring.dispatcher.UserResponse`

컨트롤러 실행 결과를 감싸는 응답 래퍼다.

- `data`
- `message`
- `success`

`success()`와 `error()` 팩토리 메서드를 제공해 응답 형식을 일정하게 유지한다.

### 8. 입력 처리

#### `edu.pnu.myspring.utils.InputProvider`

콘솔 기반 요청 입력기다.

- 지원 가능한 HTTP Method를 출력한다.
- 메서드와 URI를 입력받는다.
- `POST`, `PUT`인 경우 JSON 문자열을 여러 줄로 입력받는다.
- `END` 입력 시 Body 입력을 종료한다.
- 이를 `UserRequest` 객체로 변환한다.

이 프로젝트가 Servlet API나 Netty 같은 네트워크 서버 대신 콘솔 인터페이스를 택했기 때문에, `InputProvider`가 간이 HTTP 클라이언트 역할을 대신한다.

### 9. JSON 파서

#### `edu.pnu.myspring.utils.MyJsonParser`

외부 JSON 라이브러리를 사용하지 않고 직접 만든 간단한 파서다.

주요 기능:

- JSON 문자열을 `HashMap<String, Object>`로 변환한다.
- 객체, 배열, 문자열, 숫자, 불리언, `null`을 구분한다.
- 중첩 객체와 배열을 재귀적으로 처리한다.
- JSON 문자열을 다시 문자열 형태로 직렬화할 수 있다.
- 사람이 읽기 쉬운 pretty 출력 기능도 포함하고 있다.

현재 `RequestDispatcher`는 `POST` 또는 `PUT` Body를 처리할 때 이 파서를 사용한다.

## 애노테이션 설명

### 실행 및 컴포넌트 계열

#### `@MySpringApplication`

애플리케이션 시작 클래스임을 표시한다.

#### `@MyComponent`

기본 컴포넌트 마커다. 빈 등록 대상 여부를 판단할 때 사용된다.

#### `@MyService`

서비스 계층 마커다. 의도는 Spring의 `@Service`와 같다.

#### `@MyRepository`

리포지토리 계층 마커다. 의도는 Spring의 `@Repository`와 같다.

#### `@MyRestController`

컨트롤러 클래스를 표시한다. 요청 매핑 대상이 된다.

#### `@MyAutowired`

필드 주입을 지시한다. `MyApplicationContext`가 등록된 빈 중 타입이 맞는 객체를 찾아 주입한다.

### 요청 매핑 계열

#### `@MyRequestMapping`

메서드와 URI를 직접 지정하는 일반 매핑 애노테이션이다.

예:

```java
@MyRequestMapping(value = "/hello", method = "GET")
```

#### `@MyPostMapping`

POST 전용 매핑처럼 사용하도록 만든 애노테이션이다.  
현재 구현에서는 `@MyRequestMapping`과 유사하게 처리된다.

#### `@MyPathVariable`

URI 또는 파라미터 맵에서 특정 이름의 값을 꺼내 메서드 인자로 바인딩한다.

### 보조 애노테이션

#### `@MyRequestBody`

현재 코드베이스에는 선언되어 있지만, 실제 바인딩 로직은 아직 구현되어 있지 않다.

#### `@MyResponseBody`

현재 코드베이스에는 선언되어 있지만, 반환 처리에서 특별한 동작을 하지는 않는다.

#### `@Mapping`

현재 코드베이스에는 선언만 되어 있고 핵심 흐름에서 사용되지 않는다.

## 예제 컨트롤러

#### `edu.pnu.myspring.test.TestController`

프레임워크 동작 검증용 컨트롤러다.

- `GET /hello`
  문자열 `"Hello, MySpring World!"`를 반환한다.
- `POST /echo`
  요청에서 `id`, `body` 값을 받아 `"You sent: {id} - {body}"` 형식으로 반환한다.

이 컨트롤러는 요청 매핑, path variable 방식의 파라미터 바인딩, 응답 래핑까지 전체 흐름을 확인하는 데 사용된다.

## 현재 구현의 특징과 한계

### 특징

- reflection 기반 빈 스캔과 핸들러 호출을 직접 구현했다.
- 작은 코드량으로 IoC, DI, Dispatcher 개념을 한 번에 볼 수 있다.
- 외부 웹 프레임워크 없이도 MVC 흐름의 핵심 아이디어를 학습할 수 있다.

### 한계

- 실제 HTTP 서버가 아니라 콘솔 입력 기반이다.
- `@MyRequestBody`, `@MyResponseBody`, `@Mapping`은 메타데이터는 정리되어 있지만 핵심 디스패치 흐름에 완전히 연결되어 있지 않다.
- 생성자 주입, 스코프, 예외 처리 전략, 인터셉터, 필터 같은 기능은 없다.
- URI 패턴 처리와 타입 변환 로직이 제한적이다.
- 현재 샘플 컨트롤러는 `@MyRequestMapping` 중심으로 동작하며, 애노테이션 계층과 바인딩 전략은 더 확장할 여지가 있다.

## 요약

이 프로젝트는 Spring의 핵심 개념인

- 애플리케이션 부트스트랩
- 컴포넌트 스캔
- 빈 등록
- 의존성 주입
- 요청 매핑
- 핸들러 호출
- 응답 래핑

을 직접 구현해 보는 데 목적이 있는 미니 프레임워크다.  
실제 Servlet 기반 웹 애플리케이션을 만드는 프로젝트라기보다, Spring 내부 동작을 손으로 재구성해 보는 학습 프로젝트로 이해하면 가장 정확하다.
