# Suspend Function and CoroutineContext

해당 챕터에서는 검색한 기사를 실제로 표시하도록 리더App을 업데이트 함

그 다음 Suspend Function을 배우고 지금까지 사용한 비동기 함수와 비교함

4장에서 다루는 내용은 다음과 같음

- Suspend 함수의 개요
- Suspend 함수를 사용하는 방법
- Suspend 함수 대신 비동기 함수를 사용하는 경우
- CoroutineContext
- Dispatcher, exception handlers, non-cancellables와 같은 다양한 유형의 Context
- Coroutine 동작을 정의하기 위한 Context의 결합 및 분리

## RSS 리더 UI 개선

뉴스 기사 정보를 사용자가 스크롤 할 수 있는 목록으로 표시하도록 애플리케이션을 변경하고, 기사의 헤드라인 뿐 아니라 기사의 요약과 기사가 포함된 피드를 표시함

### 각 피드에 이름 부여

데이터 클래스를 구성하기 위해 model을 작성

URL, 이름으로 Feed를 식별할 수 있도록 하기 위해 Feed라는 Data class를 구성

```Kotlin
data class Feed(val name: String, val url: String)
```

이후 MainActivity의 feed 목록을 수정

### 피드의 기사에 대한 자세한 정보 가져오기

각 기사에 대한 feed, title, summary를 포함하는 자세한 정보를 표시

```Kotlin
data class Article(
    val feed: String,
    val title: String,
    val summary: String
)
```

### 스크롤이 가능한 기사 목록 추가

스크롤 가능한 목록을 표시하는 가장 좋은 방법은 RecyclerView를 사용하는 것

1. 의존성 추가

```
implementation "androidx.recyclerview:recyclerview:1.0.0"
```

2. 기사별 레이아웃 구현
3. 정보 매핑을 위한 어댑터 구현
   1. ViewHolder 추가
   2. 데이터 매핑
      1. RecyclerView.Adapter를 확장함
      2. onCreateViewHolder, onBindViewHolder, getItemCount를 구현
   3. 어댑터에 기사를 추가하기 위한 함수 구현
4. 액티비티에 어댑터 연결

새로운 UI를 테스트 해보면 HTML 태그가 나타나는 것을 확인할 수 있음

div 요소가 나오면 설명 부분(description)을 잘라내기 위해 asyncFetchArticles()를 수정해야 함

## Suspend Function

지금까지는 launch, async, runBlocking과 같은 코루틴 빌더를 사용해서 일시 중단 알고리즘의 대부분을 작성했음

코루틴 빌더를 호출할 때 전달하는 코드는 일시 중단 람다라고 함

함수에서의 일시 중단 코드를 작성하는 방법을 살펴봄

```Kotlin
suspend fun greetDelayed(delayMillis: Long) {
    delay(delayMillis)
    println("Hello, world!")
}
```

일시 중단 함수는 시그니처에 suspend 제어자만 추가하면 됨

코드를 코루틴 빌더 안에 감쌀 필요가 없기 때문에 코드를 명확하고 가독성 있게 만들어줌

하지만 코루틴 외부에서 이 함수를 호출하면 동작하지 않음

비 일시 중단 코드에서 함수를 호출하려면 다음과 같이 코루틴 빌더로 감싸야 함

```Kotlin
fun main(args: Array<String>) {
    runBlocking {
        greetDelayed(1000)
    }
}
```

### 동작 중인 함수를 일시 중단

Chapter02에서 동시성 코드를 구현할 때 코루틴 빌더 대신 비동기 함수를 사용하는 쪽이 더 편리한지에 대해 설명함

일시 중단 함수를 추가해 이 주제를 확장

간단한 레파지토리를 구현할 때 비동기 함수를 사용한 구현과 일시 중단 함수를 사용한 구현을 비교

#### 비동기 함수로 레파지토리 구현

Job 구현을 반환하는 함수가 있으면 어떤 시나리오에서는 편할 수 있지만, 코루틴이 실행되는 동안에 일시 중단을 위해 join()이나 await()을 사용하는 코드가 필요하다는 단점이 생김

다음과 같은 데이터 클래스를 작성

```Kotlin
data class Profile(
    val id: Long,
    val name: String,
    val age: Int
)
```

이름이나 ID를 기준으로 프로필을 검색하는 클라이언트 인터페이스를 설계함

```Kotlin
interfafce ProfileServiceRepository {
    fun fetchByName(name: String) : Profile
    fun fetchById(id: Long) : Profile

    // 비동기 구현은 다음과 같음
    fun asyncFetchByName(name: String) : Deferred<Profile>
    fun asyncFetchById(id: Long) : Deferred<Profile>
}
```

모의 구현은 다음과 같음

```Kotlin
class ProfileServiceClient : ProfileServiceRepository {
    override fun asyncFetchByName(name: String) = GlobalScope.async {
        Profile(1, name, 28)
    }
    override fun asyncFetchById(id: Long) = GlobalScope.async {
        Profile(id, "Susan", 28)
    }
}
```

다른 일시 중단 연산에서 이 구현을 호출할 수 있음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val client : ProfileServiceRepository = ProfileServiceClient()
    val profile = client.asyncFetchById(12).await()
    println(profile)
}
```

구현에서 관찰 할 수 있는 몇 가지 사항은 다음과 같음

- 함수 이름이 이해하기 편리하게 되어 있음
  - 필요할 때 클라이언트가 진행하는 것을 완료할 때까지 대기해야 함
  - 함수가 비동기(async)라는 것을 명시하는 것이 중요
- 이러한 성질로 인해, 호출자는 항상 요청이 완료될 때까지 일시정지해야 하므로 보통 함수 호출 직후 await호출이 있게 됨
- 구현은 Deferred와 엮이게 될 것
  - 다른 유형의 퓨처(future)로 ProfileServiceRepository 인터페이스를 깔끔하게 구현하기 위한 방법은 없음


#### suspend function으로 업그레이드

데이터 클래스는 동일하게 사용

긴 함수 이름 대신 좀더 깔끔하게 이름을 지을 수 있음

더 중요한 것은 비동기 함수로 구현하도록 강제하는 인터페이스 대신 일시 중단과 Profile을 반환하는 작업에만 신경 쓰면 된다는 점

다음과 같이 Deferred를 제거할 수 있음

```Kotlin
interface ProfileServiceRepository {
    suspend fun fetchByName(name: String) : Profile
    suspend fun fetchById(id: Long) : Profile
}
```

구현 또한 쉽게 바뀔 수 있음. 지금은 모의 상황이므로 실제로 일시 중지할 필요는 없음

```Kotlin
class ProfileServiceClient : ProfileServiceRepository {
    override suspend fun fetchByName(name: String) : Profile {
        return Profile(1, name, 28)
    }
    override suspend fun fetchById(id: Long) : Profile {
        return Profile(id, "Susan", 28)
    }
}
```

호출자의 코드가 좀 더 깔끔해짐

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val repository: ProfileServiceRepository = ProfileServiceClient()
    val profile = repository.fetchById(12)
    println(profile)
}
```

해당 방식은 비동기 구현에 비해 몇 가지 분명한 이점이 있음

- **유연함**
  - 인터페이스의 상세 구현 내용은 노출되지 않기에 Future를 지원하는 모든 라이브러리를 구현에서 사용할 수 있음
  - 현재 스레드를 차단하지 않고 예상된 Profile을 반환하는 구현이라면 어떤 Future 유형도 동작할 것임
- **간단함**
  - 비동기 함수는 항상 await을 호출해야 하는 번거로움이 생김
  - 명시적으로 async가 포함된 함수의 이름을 지정해야 함
  - 일시 중단 함수를 사용하면 레파지토리를 사용할 때마다 이름을 변경하지 않아도 되고 await을 호출할 필요가 없어짐

#### 일시 중단 함수와 비동기 함수

비동기 함수 대신 일시 중단 함수를 사용하기 위한 가이드라인은 다음과 같음

- 일반적으로 구현에 Job이 엮이는 것을 피하기 위해서는 일시 중단 함수를 사용하는 것이 좋음
- 인터페이스를 정의할 때는 항상 일시 중단 함수를 사용
  - 비동기 함수를 사용하면 Job을 반환하기 위한 구현을 해야 함
- 마찬가지로 추상 함수를 정의할 때는 항상 일시 중단 함수를 사용
  - 가시성이 높은 함수일수록 일시 중단 함수를 사용해야 함
  - 비동기 함수는 private 및 internal 함수로 제한되어야 함

## 코루틴 컨텍스트

코루틴은 항상 컨텍스트 안에서 실행됨

컨텍스트는 코루틴이 어떻게 실행되고 동작해야 하는지를 정의할 수 있게 해주는 요소들의 그룹

### Dispatcher

코루틴이 실행될 스레드를 결정

시작될 곳과 중단 후 재개될 곳을 모두 포함함

#### CommonPool

CommonPool은 CPU 바운드 작업을 위해서 프레임워크에 자동으로 생성되는 스레드풀

최대 크기는 시스템의 코어 수에서 1을 뺀 값

현재는 기본 디스패처로 사용되지만 용도를 명시하고 싶다면, 다른 디스패처처럼 사용할 수 있음

```Kotlin
GlobalScope.launch(CommonPool) {
    // TODO: Implement CPU-bound algorithm here
}
```

#### 기본 디스패처

현재는 CommmonPool과 같음

기본 디스패처 사용을 위해서 디스패처 전달 없이 빌더를 사용할 수 있음

```Kotlin
GlobalScope.launch {
    // TODO: 일시 중단 람다 구현
}
```

또는 명시적으로 지정할 수 있음

```Kotlin
GlobalScope.launch(Dispatchers.Default) {
    // TODO: 일시 중단 람다 구현
}
```

#### Unconfined

첫 번째 중단 지점에 도달할 때까지 현재 스레드에 있는 코루틴을 실행함

코루틴은 일시 중지된 후에 일시 중단 연산에서 사용된 기존 스레드에서 다시 시작함

```Kotlin
fun main(args: Array<String>) = runBlocking {
    GlobalScope.launch(Dispatchers.Unconfined) {
        println("Starting in ${Thread.currentThread().name}")
        delay(500)
        println("Resuming in ${Thread.currentThread().name}")
    }.join()
}
```

처음에는 main()에서 실행 중이었지만 그 다음 일시 중단 연산이 실행된 Default Executor 스레드로 이동함

#### 단일 스레드 컨텍스트

항상 코루틴이 특정 스레드 안에서 실행된다는 것을 보장함

이 유형의 디스패처를 생성하려면 newSingleThreadContext()를 사용해야 함

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val dispatcher = newSingleThreadContext("myThread")

    GlobalScope.launch(dispatcher) {
        println("Starting in ${Thread.currentThread().name}")
        delay(500)
        println("Resuming in ${Thread.currentThread().name}")
    }.join()
}
```

일시 중지 후에도 항상 같은 스레드에서 실행 됨

#### 스레드 풀

스레드 풀을 갖고 있으며 해당 풀에서 가용한 스레드에서 코루틴을 시작하고 재개함

런타임이 가용한 스레드를 정하고 부하 분산을 위한 방법도 정하기 때문에 따로 할 작업은 없음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val dispatcher = newFixedThreadPoolContext(4, "myPool")

    GlobalScope.launch(dispatcher) {
        println("Starting in ${Thread.currentThread().name}")
        delay(500)
        println("Resuming in ${Thread.currentThread().name}")
    }.join()
}
```

위 코드는 보통 다음과 같이 출력 됨

```
Starting in myPool-1
Resuming in myPool-2
```

### 예외처리

코루틴 컨텍스트의 또 다른 중요한 용도는 예측이 어려운 예외에 대한 동작을 정의하는 것

이러한 유형의 컨텍스트는 다음과 같이 CoroutineExceptionHandler를 구현해 만들 수 있음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val handler = CoroutineExceptionHandler({ context, throwable ->
        println("Error captured in $context")
        println("Message: ${throwable.message}")
    })

    GlobalScope.launch(handler) {
        TODO("Not implemented yet!")
    }

    delay(500)
}
```

예측이 어려운 예외에 대한 정보를 출력하는 CoroutineExceptionHandler를 생성함

이후 예외를 던지고, 애플리케이션에 메시지를 출력하기 위해 약간의 시간을 주는 코루틴을 시작함

그러면 애플리케이션이 예외를 정상적으로 처리하게 됨

#### Non-cancellable

코루틴의 실행이 취소되면 코루틴 내부에 CancellationException 유형의 예외가 발생하고 코루틴이 종료됨

코루틴 내부에서 예외가 발생하기 때문에 try-finally 블록을 사용해 리소스를 닫는 클리닝 작업을 수행하거나 로깅을 수행할 수 있음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val duration = measureTimeMillis {
        val job = launch {
            try {
                while (isActive) {
                    delay(500)
                    println("still running")
                }
            } finally {
                println("cancelled, will end now")
            }
        }
        delay(1200)
        job.cancelAndJoin()
    }
    println("Took $duration ms")
}
```

해당 코드는 코루틴이 취소될 때까지 0.5초마다 "still running"을 출력함

코루틴을 취소할 때는 finally 블록이 실행됨

예상대로 1.2초 지연된 후 작업이 취소되었고, finally 블록에서 메시지를 출력함

실제로 코루틴이 종료되기 전에 5초 동안 멈추도록 finally 블록을 수정해보면 그런식으로 동작하지 않음을 보게 됨(5초 지연X)

```Kotlin
} finally{
    println("cancelled, will delay finalization now")
    delay(5000)
    println("delay completed, bye bye")
}
```

finally 블록에서 실제 지연은 일어나지 않았음

코루틴은 일시 중단된 후 바로 종료되었기 때문이고, 취소 중인 코루틴은 일시 중단될 수 없도록 설계되었기 때문임

코루틴이 취소되는 동안 일시 중지가 필요한 경우는 NonCancellable 컨텍스트를 사용해야 함

finally 블록을 다음과 같이 수정하면 일시 중단이 되는것을 확인할 수 있음

```Kotlin
} finally {
    withContext(NonCancellable) {
        println("cancelled, will delay finalization now")
        delay(5000)
        println("delay completed, bye bye")
    }
}
```

## 컨텍스트에 대한 추가 정보

컨텍스트는 코루틴이 어떻게 동작할지에 대한 다른 세부사항들을 많이 정의할 수 있음

컨텍스트는 또한 결합된 동작을 정의해 작동하기도 함

### 컨텍스트 결합

컨텍스트의 일부분이 될 수 있는 여러 종류의 요소가 있음

다양한 요구사항을 만족하는 컨텍스트를 생성하기 위해 이러한 요소들을 결합시킬 수 있음

#### 컨텍스트 조합

특정 스레드에서 실행하는 코루틴을 실행하고 동시에 해당 스레드를 위한 예외처리를 설정한다고 가정했을 때, 더하기 연산자를 사용해 둘을 결합할 수 있음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val dispatcher = newSingleThreadContext("myDispatcher")
    val handler = CoroutineExceptionHandler({ _, throwable ->
        println("Error captured")
        println("Message: ${throwable.message}")
    })

    GlobalScope.launch(dispatcher + handler) {
        println("Running in ${Thread.currentThread().name}")
        TODO("Not implemented!")
    }.join()
}
```

단일 스레드 디스패처와 예외 처리를 결합하고 있으며, 코루틴은 그에 따라 동작하게 됨

조합된 컨텍스트를 유지할 변수를 만들면, 한 번 이상 더하기 연산자를 사용하지 않아도 됨

```Kotlin
val context = dispatcher handler

launch(context) { ... }
```

#### 컨텍스트 분리

결합된 컨텍스트에서 컨텍스트 요소를 제거할 수도 있음

이렇게 하려면 제거할 요소의 키에 대한 참조가 필요함

앞의 예제를 수정해 결합된 컨텍스트를 분리함

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val dispatcher = newSingleThreadContext("myDispatcher")
    val handler = CoroutineExceptionHandler({ _, throwable ->
        println("Error captured")
        println("Message: ${throwable.message}")
    })

    // 두 컨텍스트를 결합
    val context = dispatcher + handler

    // 컨텍스트에서 하나의 요소 제거
    val tmpCtx = context.minusKey(dispatcher.key)

    GlobalScope.launch(tmpCtx) {
        println("Running in ${Thread.currentThread().name}")
        TODO("Not implemented!")
    }.join()
}
```

launch(handler) { ... } 를 사용할 때와 같음

여기서 스레드는 dispatcher의 스레드가 아닌 기본 디스패처에 해당함

### withContext를 사용하는 임시 컨텍스트 스위치

이미 일시 중단 함수 상태에 있을 때 withContext()를 사용해 코드 블록에 대한 컨텍스트를 변경할 수 있음

withContext()는 코드 블록 실행을 위해 주어진 컨텍스트를 사용할 일시 중단 함수

다른 스레드에서 작업을 실행해야 할 필요가 있다면 계속 진행하기 전에 해당 작업이 끝날 때까지 항상 기다리게 됨

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val dispatcher = newSingleThreadContext("myThread")
    val name = GlobalScope.async(dispatcher) {
        // 중요한 작업 수행
        "Susan Calvin"
    }.await()
    println("User: $name")
}
```

여기서는 컨텍스트 디스패처를 이용한 작업을 위해 async()를 사용하는데, async()는 Deferred<String>을 반환하기 때문에 name이 준비될 때까지 일시 중단할 수 있도록 바로 await()을 호출해야 함

withContext()를 사용할 수 있음

withContext() 함수는 Job이나 Deferred를 반환하지 않음

전달한 람다의 마지막 구문에 해당하는 값을 반환

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val dispatcher = newSingleThreadContext("myThread")
    val name = withContext(dispatcher) {
        // 중요한 작업 수행 및 이름 반환
        "Susan Calvin"
    }
    println("User: $name")
}
```

해당 코드는 순차적으로 동작함

main은 join()이나 await()을 호출할 필요 없이 이름을 가져올 때까지 일시중단됨

## 요약

4장에서는 앞으로 나올 고급 주제에 필요한 새로운 주제를 많이 다뤘음

- RecyclerView를 구현하고, RSS 피드에서 뉴스를 보여주는 Android 프로그래밍
- 어댑터를 통해 뷰에 데이터 집합을 매핑하는 방법과 ViewHolder가 뷰의 일부로 사용되는 방법에 대해 이야기 함
- 안드로이드의 RecyclerView가 많은 뷰를 생성하지 않는다는 것을 배움
  - 사용자가 스크롤 할 때 재활용됨
- 일시 중단 함수(suspend function), withContext() 일시 중단 코드 정의를 위한 유연한 방법을 제공한다는 것을 배움
- 비동기 함수(Job 구현을 반환하는 함수)는 특정 구현을 강요하는 위험을 피하기 위해 withContext() 공개 API의 일부가 돼서는 안된다고 언급함
- 코루틴 컨텍스트에 대한 흥미로운 주제와 작동하는 방법에 대해 다룸
- 디스패처를 시작으로 예외 처리와 취소 불가능한 고유한 컨텍스트로 옮겨가는 다양한 유형의 코루틴 컨텍스트를 나열
- 코루틴에서 기대하는 동작을 얻기 위해 많은 컨텍스트를 하나로 결합하기 위한 방법을 배움
- 요소 중 하나의 키를 제거함으로써 결합된 컨텍스트를 분리하는 세부적인 부분을 익힘
- withContext를 통해 프로세스에 Job을 포함시키지 않고도 다른 컨텍스트로 전환할 수 있게 해줌