# Channel - Memory Sharing for communication

채널은 스레드가 서로 상태를 공유하는 대신 메시지를 주고받는 통신을 하도록 함으로써 동시성 코드를 작성하는 데 도움을 주는 도구

채널에 관련하여 다음 주제들을 다룰 것
- 실 사례를 통한 채널의 이해
- 채널의 유형
- 채널과의 상호작용
- RSS 리더를 위한 실제 채널 구현

## 채널의 이해

채널은 동시성 코드 간에 서로 안전한 통신을 할 수 있도록 해주는 도구

채널은 동시성 코드가 메시지를 보내 통신할 수 있도록 해줌

실행 중인 스레드에 상관없이 서로 다른 코루틴 간에 메시지를 안전하게 보내고 받기 위한 파이프라인으로 생각할 수 있음

### 스트리밍 데이터 사례

특정 키워드에 대해 10개의 콘텐츠 인덱서를 조회한 뒤 검색 결과를 보여주는 작업이 있음

첫 번째 접근 방법은 각 콘텐츠 인덱서를 위한 코루틴을 시작해서 ```Deferred<List<ResultDto>>```를 반환하고 모든 코루틴이 완료되면 결과를 병합해서 UI에 보내는 것

각 인덱서가 응답을 반환하는 시간이 다르고 일부는 상당히 오래 걸릴 수 있다는 문제점이 있음

그렇기에 모든 코루틴이 완료될 때까지 기다려야 하는데, 결과 표시를 지연시켜서 사용자가 즉시 결과와 상호작용하는 것을 방해함

#### 해결 방법

1. search()에서 ```Channel<ResultDto>```를 반환
2. search()에서 결과가 도착하는 대로 각 코루틴으로부터 끊김 없이 결과를 수신

이를 위해 각 코루틴은 응답을 가져오고 처리할 때 단일 채널을 통해서 결과를 전송함

search()는 호출자가 편리하게 결과를 처리할 수 있도록 간단히 채널만 반환함

호출자는 콘텐츠 인덱서로부터 얻은 결과를 즉시 수신하게 되고 UI는 그 결과를 점차적으로 표시함으로써, 사용자에게 부드러우면서 빠른 경험을 할 수 있도록 해줌

### 분산 작업 사례

유즈넷 그룹의 콘텐츠를 색인화하는 애플리케이션

유즈넷 그룹은 오늘날 포럼의 효시로, 각 그룹은 각각의 주제를 가지고 있으며, 사람들은 새로운 스레드(아티클)을 작성할 수 있고 누구든지 그 아티클에 답변을 할 수 있음

유즈넷에 접속할 때는 인터넷 브라우저를 사용하지 못하며, 동시 접속 수가 제한되어 있음

요약해서 애플리케이션의 목적은 모든 커넥션을 사용해 가능한 많은 아티클을 가져오고 파싱한 후 카테고리를 분류해서 처리된 정보를 데이터베이스에 저장하는 것

접근 방법은 가능한 모든 커넥션에 하나의 코루틴을 사용해 각 아티클을 가져와서 파싱하고 콘텐츠를 분류한 다음 데이터베이스에 넣음

각 코루틴은 하나를 끝내면 다음 아티클로 넘어감

위의 방식을 사용하는 경우 가용 가능한 커넥션을 최대로 사용하지 못함

아티클을 인덱스하는 데 걸리는 시간의 30%만이 가져오는 데 쓰이고, 나머지는 처리하는데 사용됨

그렇지만 인덱스 생성의 모든 단계를 수행하기 위해 하나의 코루틴을 사용하기 때문에 각 아티클 당 70%는 커넥션을 사용하지 않음

#### 해결 방법

각 아티클을 가져오는데 사용되는 코루틴과 처리를 위한 코루틴을 가지는 것

하드웨어가 충분히 있다는 가정하에 끊김 없이 데이터를 가져오고 가능한 한 많이 처리하도록 하드웨어의 사용을 극대화할 수 있음

## 채널 유형과 배압

Channel의 send()는 일시 중단 함수

실제로 데이터를 수신하는 누군가가 있을 때까지 전송하는 코드를 일시 중지하고 싶을 수도 있기 때문

흔히 배압이라고 하며 리시버가 실제로 처리할 수 있는 것보다 더 많은 요소들로 채널이 넘치지 않도록 도움

코루틴은 채널 안의 요소가 버퍼 크기에 도달하면 일시 중단되고, 채널에서 요소가 제거되는 즉시 송신자는 재개됨

### 언버퍼드 채널

버퍼가 없는 채널

다음과 같은 형식으로 생성할 수 있음

```kotlin
val channel = RendezvousChannel<Int>()

val redezvousChannel = Channel<Int>()

val rendezvousChannel = Channel<Int>(0)
```

이 채널은 요소가 검색될 때까지 송신자의 실행을 중지함

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val time = measureTimeMillis {
        val channel = Channel<Int>()
        val sender = GlobalScope.launch {
            repeat(10) {
                channel.send(it)
                println("Sent $it")
            }
        }
        channel.receive()
        channel.receive()
    }
    println("Took ${time}ms")
}
```

sender 코루틴은 채널을 통해 최대 10개의 숫자까지 보낼 수 있지만 채널로부터 수신하는 요소가 두 개 뿐이어서 두 요소만 전송됨

결과는 다음과 같음

```Kotlin
Sent 0
Sent 1
Took 25ms
```

### 버퍼드 채널

이 유형의 채널은 채널 내 요소의 수가 버퍼의 크기와 같을 때마다 송신자의 실행을 중지함

#### LinkedListChannel

중단 없이 무한의 요소를 전송할 수 있는 채널

이 채널 유형은 어떤 송신자도 중단하지 않음

```Kotlin
val channel = LinkedListChannel<Int>()
```

Channel.UNLIMITED 파라미터와 함께 Channel() 함수를 사용할 수도 있음

```Kotlin
val channel = Channel<Int>(Channel.UNLIMITED)
// Channel.UNLIMITED는 Int.MAX_VALUE와 같음
```

Channel() 채널은 송신자를 일시 중지하지 않음

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val time = measureTimeMillis {
        val channel = Channel<Int>(Channel.UNLIMITED)
        val sender = GlobalScope.launch {
            repeat(5) {
                println("Sending $it")
                channel.send(it)
            }
        }
        delay(500)
    }
    println("Took ${time}ms")
}
```

채널이 5개의 요소를 처리할 수 있는 리시버가 없더라도 sender가 5개의 요소를 내보낼 수 있음

결과는 다음과 같음

```Kotlin
Sending 0
Sending 1
Sending 2
Sending 3
Sending 4
Took 523ms
```

이 유형의 채널은 메모리를 너무 많이 소모할 수 있기 때문에 사용할 때 주의해야 함

이 채널보다는 요구사항과 대상 디바이스에 기반하는 버퍼 크기를 갖는 버퍼드 채널을 사용하는 것을 권장

#### ArrayChannel

이 채널 유형은 버퍼 크기를 0부터 최대 int.MAX_VALUE - 1까지 가지며, 요소의 양이 버퍼 크기에 이르면 송신자를 일시 중단함

int.MAX_VALUE보다 적은 값을 Channel에 전달하는 방식으로 생성

```Kotlin
val channel = Channel<Int>(50)

// 생성자 직접 호출 가능
val arrayChannel = ArrayChannel<Int>(50)
```

버퍼가 가득 차면 송신자를 일시 중지하고, 다음과 같이 하나 이상의 항목이 검색되면 다시 재개함

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val time = measureTimeMillis {
        val channel = Channel<Int>(4)
        val sender = GlobalScope.launch {
            repeat(10) {
                channel.send(it)
                println("Sent $it")
            }
        }
        delay(500)
        println("Taking two")
        channel.take(2).receivec()
        delay(500)
    }
    println("Took ${time}ms")
}
```

sender는 최대 10개의 요소를 내보낼 수 있지만 채널 용량이 4이기 때문에 다섯 번째 요소를 보내기 전에 중단됨

두 개의 요소가 수신되면 sender는 버퍼가 다시 찰 때까지 재개함

```
Sent 0
Sent 1
Sent 2
Sent 3
Taking two
Sent 4
Sent 5
Took 1038ms
```

#### ConflatedChannel

세 번째 유형의 버퍼드 채널은 내보낸 요소가 유실돼도 괜찮다는 생각이 깔려 있음

이 유형의 채널에는 하나의 요소의 버퍼만 가지고 있으며, 새로운 요소가 보내질 때마다 이전 요소는 유실됨

또한 송신자가 절대로 일시 중지되지 않는다는 것을 의미함

다음과 같이 인스턴스를 생성할 수 있음

```Kotlin
val channel = ConflatedChannel<Int>()

val channel = Channel<Int>(Channel.CONFLATED)
```

채널은 송신자를 일시 중지하지 않지만 가져오지 않은 요소를 덮어씀

```Kotlin
fun main(args: Array<String>) = runBlocking {
    val time = measureTimeMillis {
        val channel = Channel<Int>(Channel.CONFLATED)
        launch {
            repeat(5) {
                channel.send(it)
                println("Sent $it")
            }
        }
        delay(500)
        val element = channel.receive()
        println("Received $element")
    }
    println("Took ${time}ms")
}
```

채널을 통해 전송된 마지막 값을 포함하는 요소를 가짐(숫자 4)

```
Sent 0
Sent 1
Sent 2
Sent 3
Sent 4
Received 4
Took 521ms
```
