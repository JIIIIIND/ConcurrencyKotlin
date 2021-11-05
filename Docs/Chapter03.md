# LifeCycle and Error Handling

Job과 Deferred에 대해 자세히 실펴보고 유사점과 차이점, 이들의 라이프사이클에 대해 알아봄

- Job과 사용 사례
- Job과 Deferred의 라이프사이클
- Deferred 사용 사례
- Job의 각 상태별 예상되는 사항
- Job의 현재 상태를 산출하는 방법
- 예외 처리 방법

## Job과 Deferred

비동기 함수는 다음과 같이 두 그룹으로 나눌 수 있음

- **결과가 없는 비동기 함수**
  - 로그에 기록하고 분석 데이터를 전송하는 것과 같은 백그라운드 작업
  - 완료 여부를 모니터링 할 수 있지만 결과를 갖지 않는 백그라운드 작업
- **결과를 반환하는 비동기 함수**
  - 비동기 함수가 웹 서비스에서 정보를 가져올 때 거의 대부분 해당 함수를 사용해 정보를 반환하고자 함

### Job

한 번 시작된 작업은 예외가 발생하지 않는 한 대기하지 않음

코루틴 빌더인 launch()를 사용해 Job을 생성하는 방법이 가장 일반적이다.

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val job = GlobalScope.launch {
        // Do Background task here
    }
}
```

다음과 같이 Job() 팩토리 함수를 사용할 수도 있음

```Kotlin
fun main(args: ARray<String>) = runBlocking {
    val job = Job()
}
```

#### 예외처리

기본적으로 Job 내부에서 발생하는 예외는 Job을 생성한 곳까지 전파됨

Job이 완료되기를 기다리지 않아도 발생함

```Kotlin
fun main(args: Array<String>) = runBlocking {
    GlobalScope.launch {
        TODO("Not Implemented!")
    }

    delay(500)
}
```

현재 스레드의 포착되지 않은 예외 처리기에 예외가 전파됨

JVM Application이라면 표준 오류 출력에 예외가 출력

#### LifeCycle

Job은 5가지 상태를 가짐

- New(생성)
  - 존재하지만 아직 실행되지 않는 Job
- Active(활성)
  - 실행 중인 Job. 일시 중단된 Job도 활성으로 간주함
- Completed(완료 됨)
  - Job이 더 이상 실행되지 않는 경우
- Canceling(취소 중)
  - 실행 중인 Job에서 cancel()이 호출되면 취소가 완료될 때까지 시간이 걸리기도 함
  - 활성과 취소 사이의 중간 상태
- Cancelled(취소 됨)
  - 취소로 인해 실행이 완료된 Job
  - 취소된 Job도 완료로 간주될 수 있음

**생성**

Job은 기본적으로 launch()나 Job()을 사용해 생성될 때 자동으로 시작됨

Job을 생성할 때 자동으로 시작되지 않게 하려면 CoroutinbeStart.LAZY를 사용해야 함

```Kotlin
fun main(args: Array<String>) = runBlocking {
    GlobalScope.launch(start = CoroutineStart.LAZY) {
        TODO("Not implemented yet!")
    }

    delay(500)
}
```

작업이 생성되었지만 시작되지 않았기에 예외가 발생하지 않음

**활성**

일반적으로 start()나 join()을 호출해서 실행함

start()의 경우 Job이 완료될 때까지 기다리지 않고 Job을 시작하는 반면, join()은 Job이 완료될 때까지 실행을 일시 중단한다는 차이가 있음

```Kotlin
fun main(args: Array<String>) {
    val job = GlobalScope.launch(start = CoroutineStart.LAZY) {
        delay(3000)
    }

    job.start()
}
```

job.start()가 호출될 때 실행을 일시 중단하지 않으므로 애플리케이션이 job이 완료되는 것을 기다리지 않고 실행을 끝냄

> start()는 실행을 일시 중단하지 않으므로 suspend나 코루틴에서 호출할 필요가 없음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val job = GlobalScope.launch(start = CoroutineStart.LAZY) {
        delay(3000)
    }

    job.join()
} 
```

join()을 사용하면 애플리케이션이 job을 완료할 때까지 대기함

> join()은 일시 중단이 가능하기 때문에 suspend나 코루틴에서 호출해야 함. 이를 위해 runBlocking이 사용되고 있음

**취소 중**

취소 요청을 받은 활성 Job은 취소 중이라고 하는 스테이징 상태로 들어갈 수 있음

cancel() 함수를 호출하여 취소를 요청

```Kotlin
fun main(args: ARray<String>) = runBlocking {
    val job = GlobalScope.launch {
        // Do some work here
        delay(5000)
    }

    delay(2000)
    job.cancel()
}
```

job 실행은 2초 후 취소됨

cancelAndJoin() 함수의 경우 실행을 취소할 뿐만 아니라 취소가 완료될 때까지 현재 코루틴을 일시 중단함

**취소 됨**

취소 또는 처리되지 않은 예외로 인해 실행이 종료된 Job은 취소됨으로 간주함

Job이 취소되면 getCancellationException() 함수를 통해 취소에 대한 정보를 얻을 수 있음

이 함수는 CancellationException을 반환하는데 취소 원인 등의 정보를 검색할 때 사용할 수 있음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val job = GlobalScope.launch {
        delay(5000)
    }

    delay(2000)
    
    // cancel
    job.cancel(cause = CancellationException("Tired of waiting"))

    val cancellation = job.getCancellationException()
    printf(cacellation.message)
}
```

취소된 Job과 예외로 인해 실패한 Job을 구별하기 위해 다음과 같이 CoroutineExceptinoHandler를 설정해 취소 작업을 처리하는 것이 좋음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val exceptionHandler = CoroutineExceptionHandler {
        _: CoroutineContext, throwable: Throwable ->
        println("Job cancelled due to ${throwable.message}")
    }

    GlobalScope.launch(exceptionHandler) {
        TODO("Not implemented yet!")
    }

    delay(2000)
}
```

다음과 같이 invokeOnCompletion() 을 사용할 수도 있음

```Kotlin
fun main(args: Array<String>) = runBlocking<Unit> {
    GlobalScope.launch {
        TODO("Not implemented yet!")
    }.invokeOnCompletion { cause ->
        cause?.let {
            println("Job cancelled due to ${it.message}")
        }
    }

    delay(2000)
}
```

**완료됨**

실행이 중지된 Job은 완료됨으로 간주

정상적으로 종료되거나 취소, 예외로 인해 종료되었는지 여부와 관계 없음

이러한 이유로 취소된 항목은 완료된 항목의 하위 항목으로 간주되기도 함

#### Job의 현재 상태 확인

Job에는 상태가 많아서 외부에서 현재 상태를 파악하는 방법을 알아야 함

Job은 이를 위해 다음과 같은 세 가지 속성을 가지고 있음

- isActive
  - Job이 활성 상태인지 여부
  - Job이 일시 중지인 경우도 true를 반환
- isCompleted
  - Job이 실행을 완료했는지 여부
- isCancelled
  - Job 취소 여부
  - 취소가 요청되면 즉시 true가 됨

속성들은 이전에 나열된 상태 목록과 쉽게 매핑될 수 있음

| 상태(State) | isActive | isCompleted | isCancelled |
| -- | -- | -- | -- |
| 생성됨(Created) | false | false | false |
| 활성(Active) | true | false | false |
| 취소 중(Cancelling) | false | false | true |
| 취소됨(Cancelled) | false | true | true |
| 완료됨(Completed) | false | true | false |


> 완료 중(Completeing)이라는 내부 상태가 있지만, 시그니처는 활성 상태와 유사하다는 점을 고려할 때 개별 상태로는 다루지 않음

### Deferred

결과를 갖는 비동기 작업을 수행하기 위해 Job을 확장함

다른 언어에서 Futures 또는 Promises라고 하는 것의 코틀린 구현체가 Deferred임

기본 컨셉은 연산이 객체를 반환할 것이며, 객체는 비동기 작업이 완료될 때까지 비어 있다는 것

Deferred와 그 상태의 라이프 사이클은 Job과 비슷함

실제 차이점은 반환 유형과 에러 핸들링

Deferred의 생성은 async를 사용할 수 있음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val headlinesTask = GlobalScope.async {
        getHeadlines()
    }

    headlinesTask.await()
}
```

또는 CompletableDeferred의 생성자를 사용할 수 있음

```Kotlin
val articleTask = CompletableDeferred<List<Article>>()
```

#### 예외 처리

순수한 Job과 달리 Deferred는 처리되지 않은 예외를 자동으로 전파하지 않음

Deferred의 결과를 대기할 것으로 예상하기 때문에 이런 방식을 사용함

실행이 성공했는지 확인하는 것은 사용자의 몫

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val deferred = GlobalScope.async {
        TODO("Not implemented yet!")
    }

    delay(2000)
}
```

위의 예제는 지연된 실패를 가지지만 예외를 전파하지는 않음

여기서는 Deferred의 실행을 모니터링하지 않는 시나리오를 재현할 수 있도록 delay를 사용하지만 Deferred는 모니터링하도록 되어 있으므로 이렇게 해서는 안됨

```Kotlin
fun main(args: Array<String>) = runBlocking<Unit> {
    val deferred = GlobalScope.async {
        TODO("Not implemented yet!")
    }

    deferred.await()
}
```

앞의 코드와는 다르게 이 코드는 예외를 전파하고 애플리케이션을 중단시킴

Deferred의 실행이 코드 흐름의 필수적인 부분임을 나타내는 것이기 때문에 await()을 호출하는 이런 방식으로 설계되었음

이 방법을 통해 명령형(imperative)으로 보이는 비동기 코드를 보다 쉽게 작성할 수 있고, try-catch 블록을 사용해 예외를 처리할 수 있음

```Kotlin
fun main(args: Array<String>) = runBlocking<Unit> {
    val deferred = GlobalScope.async {
        TODO("Not implemented yet!")
    }

    try {
        deferred.await()
    } catch (throwable: Throwable) {
        println("Deferred cancelled due to ${throwable.message}")
    }
}
```

CoroutineExceptionHandler를 Job에 사용한 것과 같은 방식으로 사용할 수 있음

> 이 장의 나머지 부분에서는 Job과 Deferred를 모두 Job으로 표기하는데, Job이 베이스 인터페이스이기 때문이며, 별도로 명시하지 않는 한 Job에 대해 언급한 내용은 Deferred에도 적용됨

## 상태는 한 방향으로만 이동

일단 Job이 특정 상태에 도달하면 이전 상태로 되돌아가지 않음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val time = mesureTimeMillis {
        val job = GlobalScope.launch {
            delay(2000)
        }
        job.join()

        job.start()
        job.join()
    }
    println("Took $time ms")
}
```

코드는 2초 동안 실행을 일시중단하는 Job을 만들고 완료되기까지 join으로 대기함

이후 job을 다시 실행하기 위해 start를 호출하고 join으로 대기하지만 job은 한번만 실행됨

### 최종 상태의 주의 사항

일부 Job의 상태는 최종 상태(final state)로 간주됨

최종 상태는 Job을 옮길 수 없는 상태를 의미함

Job이 이전 상태로 돌아가지 않을 것이라는 점을 고려하면 해당 상태는 취소됨과 완료됨 상태

## RSS - 여러 피드에서 동시에 읽기

안드로이드 RSS 리더의 기능을 개선

1. fetchRssHeadlines 함수 개선
   1. 고정된 feed를 인자로 받아서 처리할 수 있도록 수정
   2. Dispatcher를 받아 비동기로 동작하도록 수정
2. dispatcher를 업데이트
   1. newSingleThreadContext(name = "ServiceCall")에서 newFixedThreadPoolContent(2, "IO")로 변경
   2. asyncFetchHeadlines()는 서버에서 정보를 가져오고 파싱도 하기 때문에 풀의 크기를 늘림
   3. XML을 파싱하는 오버헤드는 단일 스레드를 사용하는 경우 성능에 영향을 줌
   4. 때로는 다른 스레드의 파싱이 완료될 때까지 한 피드로부터 정보를 가져오는 것이 지연될 수 있음
3. 데이터를 동시에 가져오기
   1. 목록에서 각 피드당 하나의 Deferred를 생성
   2. asyncLoadNews 함수를 수정해 대기하는 모든 Deferred를 추적할 수 있는 목록을 구현
4. 응답 병합
   1. asyncLoadNews는 각 요청이 끝날 때까지 대기함
   2. 각각이 헤드라인의 목록을 반환하기 때문에 이들을 하나의 리스트에 담을 수 있음 -> Deferred의 내용을 flat map을 이용해 담을 수 있음
5. 예외 처리
   1. 코루틴이 완료될때 까지 await을 사용해 대기하므로 코루틴 내부의 예외는 현재 스레드로 전파됨
   2. 인터넷이 연결되지 않았거나 feed URL이 유효하지 않은 경우 예외가 발생
   3. await 대신 join을 사용해 Deferred를 대기
   4. 요청을 읽을 때에도 예외가 전파되기 때문에 getCompleted()를 호출하기 이전에 isCancelled를 통해 필터링
   5. 네트워크 연결이 없는 장치나 잘못된 URL이 있어도 중단 없이 실행 됨
6. 예외 처리된 Feed에 대해서도 표시할 수 있도록 UI 수정