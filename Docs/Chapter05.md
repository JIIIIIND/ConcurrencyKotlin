# Iterator, Sequence, Producer

데이터 소스에서 정보를 검색하고 표시할 때는 온 디맨드 방식을 사용하는 것이 일반적

뉴스 리더의 경우 처음 실행 할 때 일부 기사를 가져오고 아래로 스크롤할 때 데이터를 더 가져오게 됨

정보의 출처에 따라 데이터는 거의 무한정이나 다름 없음

5장에서 다루는 내용은 다음과 같음
- 일시 중단 가능한 시퀀스(Suspendable sequence)
- 일시 중단 가능한 이터레이터(Suspendable iterator)
- 일시 중단 가능한 데이터 소스에서 데이터 산출
- 시퀀스와 이터레이터의 차이점
- 프로듀서를 사용한 비동기 데이터 검색
- 프로듀서의 실제 사례

## 일시 중단 가능한 시퀀스 및 이터레이터

이번 장에서는 실행 사이에 일시 중단이 일어나는 함수의 구현 같은, 조금은 다른 시나리오를 다룸

시퀀스 및 이터레이터에는 몇 가지 중요한 특성이 있음

- 호출 사이에서 일시 중단되지만, 실행 중에는 일시 중단될 수 없음
  - 일시 중단 연산이 없어도 반복할 수 있음
- 시퀀스와 이터레이터의 빌더는 CoroutineContext를 받지 않음
  - 코드를 호출한 컨텍스트와 동일한 컨텍스트에서 실행됨
- 정보산출(yielding) 후에만 일시 중지할 수 있음
  - yield() 또는 yieldAll() 함수를 호출해야 함

## 값 산출

값을 산출하면 값이 다시 요청될 때까지 시퀀스 또는 이터레이터가 일시 중단됨

```Kotlin
fun main(args: Array<String>) {
    val iterator = iterator {
        yield("First")
        yield("Second")
        yield("Third")
    }
    println(iterator.next())
    println(iterator.next())
    println(iterator.next())
}
```

iterator는 세 가지 요소를 포함하는 이터레이터를 빌드함

요소가 처음 요청되면 First 값이 산출되고 이후 실행이 중단됨

이터레이터는 값이 생성된 후 매번 일시 중단되어 총 세 번 일시 중단됨

## 이터레이터

요소들의 컬렉션을 순서대로 살펴보는 데 특히 유용함

코틀린 이터레이터의 특성은 다음과 같음

- 인덱스로 요소를 검색할 수 없으므로 요소는 순서대로만 액세스할 수 있음
- 더 많은 요소가 있는지 여부를 나타내는 hasNext()함수가 있음
- 요소는 한 방향으로만 검색할 수 있음
  - 이전 요소를 검색할 방법 없음
- 재설정할 수 없으므로 한 번만 반복할 수 있음

iterator()를 사용해 이터레이터 본문과 함께 람다를 전달함

따로 지정되지 않는 한 ```Iterator<T>```를 리턴함

T는 이터레이터가 생성하는 요소에 의해 결정

```Kotlin
val iterator = iterator {
    yield(1)
}
```

이때는 ```Iterator<Int>``` 타입

어떤 이유든 재정의하려면 타입을 정의할 수 있으며, 생성된 모든 값이 타입을 준수하는 한 잘 작동함

```Kotlin
val iterator = Iterator<Any> = iterator {
    yield(1)
    yield(10L)
    yield("Hello")
}
```

### 이터레이터와의 상호 작용

이터레이터를 사용하는 일반적인 방법과 예외를 피하는 방법, 값을 연산할 때 세부사항을 설명함

#### 모든 요소를 살펴보기

이터레이터의 모든 요소를 하나씩 가져오는 대신 한꺼번에 가져오는 경우도 있음

전체 이터레이터를 반복하기 위해 forEach()나 forEachRemaining() 함수를 사용할 수 있음

```Kotlin
iterator.forEach {
    println(it)
}
```

#### 다음 값 가져오기

이터레이터에서 요소를 읽으려면 next()를 사용할 수 있음

```Kotlin
fun main(args: Array<String>) {
    val iterator = Iterator<Any> = iterator {
        yield(1)
        yield(10L)
        yield("Hello")
    }
    println(iterator.next())
    println(iterator.next())
    println(iterator.next())
}
```

#### 요소가 더 있는지 검증하기

이터레이터가 하나 이상의 요소가 있으면 true를, 그렇지 않으면 false를 리턴함

```Kotlin
fun main(args: Array<String>) {
    val iterator = iterator {
        for (i in 0..4) {
            yield(i * 4)
        }
    }

    for (i in 0..5) {
        if (iterator.hasNext()) {
            println("element $i is ${iterator.next()}")
        } else {
            println("No more elements")
        }
    }
}
```

#### 요소를 검증하지 않고 next() 호출하기

next()로 이터레이터에서 요소를 가져올 때는 항상 먼저 hasNext()를 호출하는 것이 좋음

검색할 요소가 있는지 확인하지 않으면 실행 중에 ```NoSuchElementException``` 예외가 발생

#### hasNext()의 내부 작업에 대한 참고사항

hasNext()가 작동하려면 런타임은 코루틴 실행을 재개함

새로운 값이 나오면 함수는 true를 반환하고, 더 이상 값이 없어 이터레이터의 실행이 끝나면 함수는 false를 반환함

hasNext()로 값이 산출되면 값이 유지되다가 다음 next() 호출에 값이 반환됨

```Kotlin
fun main(args: Array<String>) {
    val iterator = iterator {
        println("yielding 1")
        yield(1)
        println("yielding 2")
        yield(2)
    }

    iterator.next()
    if (iterator.hasNext()) {
        println("iterator has next")
        iterator.next()
    }
}

/* return value
 * yielding 1
 * yielding 2
 * iterator has next
*/
```

## 시퀀스

시퀀스는 이터레이터와는 상당히 다름

시퀀스의 몇 가지 특성을 살펴보면 다음과 같음

- 인덱스로 값을 가져올 수 있음
- 상태가 저장되지 않으며, 상호 작용한 후 자동으로 재설정 됨
- 한번의 호출로 값 그룹을 가져올 수 있음

sequence() 빌더를 사용하여 생성함

빌더는 일시 중단 람다를 가져와 ```Sequence<T>```를 반환

### 시퀀스와 상호 작용

```Kotlin
val sequence = sequence {
    yield(1)
    yield(1)
    yield(2)
    yield(3)
    yield(5)
    yield(8)
    yield(13)
    yield(21)
}
```

#### 시퀀스의 모든 요소 읽기

시퀀스의 모든 요소를 살펴보기 위해 forEach() 및 forEachIndexed()를 사용할 수 있음

둘 다 유사하게 동작하지만 forEachIndexed()는 값과 함께 값의 인덱스를 제공하는 확장 함수

#### 특정 요소 얻기

인덱스로 값을 가져오는 경우 다음 기능 중 하나를 사용할 수 있음

| 함수 | 설명 | 예시 |
| -- | -- | -- |
| elementAt | 인덱스를 가져와 해당 위치의 요소를 반환 | ```sequence.elementAt(4)``` |
| elementAtOrElse | 인덱스에 요소가 없으면 람다로 실행 | ```sequence.elementAtOrElse(10, { it * 2 })``` |
| elementAtOrNull | 인덱스에 요소가 없으면 null 반환 | ```sequence.elementAtOrNull(10)``` |

#### 요소 그룹 얻기

한 번에 값들의 그룹을 가져올 수 있음

```Kotlin
val firstFive = sequence.take(5)
println(firstFive.joinToString())

/* result
 * 1, 1, 2, 3, 5
*/
```

쉼표로 구분된 처음 5개의 값이 출력됨

> take()는 중간 연산이므로 나중에 종단 연산이 호출되는 시점에 계산돼 ```Sequence<T>```를 반환함

### 시퀀스는 상태가 없다

일시 중단 시퀀스는 상태가 없고 사용된 후에 재설정 됨

```Kotlin
val sequence = sequence {
    for (i in 0..9) {
        println("Yielding $i")
        yield(i)
    }
}

fun main(args: Array<String>) {
    println("Requesting index 1")
    sequence.elementAt(1)
    /* result
    Yielding 0
    Yielding 1
    */

    println("Requesting index 2")
    sequence.elementAt(2)
    /* result
    Yielding 0
    Yielding 1
    Yielding 2
    */

    println("Taking 3")
    sequence.take(3).joinToString()
    /* result
    Yielding 0
    Yielding 1
    Yielding 2
    */
}
```

### 피보나치 수열

#### 시퀀스 사용

```Kotlin
val fibonacci = sequence {
    yield(1)
    var current = 1
    var next = 1
    while (true) {
        yield(next)
        val tmpNext = current + next
        current = next
        next = tmpNext
    }
}
```

코드 설명

- 시퀀스는 1을 산출함
- 첫 번째 숫자가 나오면 시퀀스 중단
- 두 번째 숫자가 요청되면 시퀀스는 current와 next의 두 변수를 생성
- 이후 무한루프에 들어감
  - 데이터 소스가 시퀀스에서 요청된 수만큼 많은 수의 데이터를 배출할 수 있게 해줌
- 다음 번 시퀀스에서 숫자가 요청되면 가장 먼저 next 값이 산출돼 시퀀스가 일시 중지됨
- 그 시점부터 값이 요청될 때마다 현재 값과 다음 값이 모두 새 값을 포함하도록 다시 계산되고 다음 값이 산출됨

#### 이터레이터 사용

```Kotlin
val fibonacci = iterator {
    yield(1)
    var current = 1
    var next = 1
    while (true) {
        yield(next)
        val tmpNext = current + next
        current = next
        next = tmpNext
    }
}
```

시퀀스를 사용한 코드와 동일함

## 프로듀서

시퀀스와 이터레이터에는 실행 중에 일시 중단할 수 없다는 제한이 있음

이상적으로는 다른 작업이 끝나기를 기다리는 동안 일시 중단할 수 있어야 하기에 이것은 대부분 큰 제약사항임

프로듀서를 사용하여 한계를 극복할 수 있음

프로듀서에 대한 몇 가지 중요한 세부 사항
- 프로듀서은 값이 생성된 후 일시 중단
  - 새로운 값이 요청될 때 다시 재개
  - 시퀀스 및 이터레이터와 유사함
- 프로듀서는 특정 CoroutineContext로 생성할 수 있음
- 전달되는 일시 중단 람다의 본문은 언제든지 일시 중단될 수 있음
- 어느 시점에서든 일시 중단할 수 있으므로 프로듀서의 값은 일시 중단 연산에서만 수신할 수 있음
- 채널을 사용해 작동하므로 데이터를 스트림처럼 생각할 수 있음
  - 요소를 수신하면 스트렘에서 요소가 제거됨

### 프로듀서 생성

코루틴 빌더 produce()를 호출해야 함

```ReceiveChannel<E>```를 리턴

프로듀서는 채널 위에 구축되므로 프로듀서의 요소를 산출하기 위해 send(E) 함수를 사용함

```Kotlin
val producer = GlobalScope.produce {
    send(1)
}
```

launch() 또는 async()와 같은 방식으로 CoroutineContext를 지정할 수 있음

```Kotlin
val context = newSingleThreadContext("myThread")
val producer = GlobalScope.produce(context) {
    send(1)
}
```

이터레이터 및 시퀀스와 마찬가지로 타입을 지정할 수 있으며 배출되는 요소가 이른 준수하는 한 작동함

```Kotlin
val producer : ReceiveChannel<Any> = GlobalScope.produce(context) {
    send(5)
    send("a")
}
```

> 채널에 대해서는 다음 장에서 설명함

### 프로듀서와 상호작용

시퀀스와 이터레이터를 사용해 수행되는 방식을 혼합한 것임

지금은 ReceiveChannel의 일부 기능에 대해서만 설명함

#### 프로듀서의 모든 요소 읽기

프로듀서의 모든 요소를 살펴보기 위해 consumerEach() 함수를 사용할 수 있음

```Kotlin
val context = newSingleThreadContext("myThread")
val producer = GlobalScope.produce(context) {
    for (i in 0..9) {
        send(i)
    }
}
```

프로듀서는 최대 10개의 숫자를 생성함

이들을 모두 가져오려면 간단히 프로듀서에서 consumerEach()를 호출하면 됨

```Kotlin
fun main(args: Array<String>) {
    producer.consumeEach {[4][KD5]
        println(it)
    }
}
```

#### 단일 요소 받기

다음과 같이 receive() 함수를 사용할 수 있음

```Kotlin
val producer = GlobalScope.produce {
    send(5)
    send("a")
}

fun main(args: Array<String>) {
    println(producer.receive())
    println(producer.receive())
}
```

코드는 먼저 숫자 5를 출력한 다음 문자 a를 출력함

#### 요소 그룹 가져오기

take()의 매개변수로 요소의 개수를 제공해서 값을 읽을 수 있음

다음과 같이 프로듀서로부터 처음 세 요소를 사용할 수 있음

```Kotlin
producer.take(3).comsumeEach {
    println(it)
}
```

```ReceiveChannel<E>```의 take()는 ```ReceiveChannel<E>```를 반환하며, take()는 중간 연산이므로 종단 연산이 발생할 때 세 요소의 실제 값이 계산됨

여기서 종단 연산은 comsumerEach()

#### 사용 가능한 요소보다 더 많은 요소 사용하기

이터레이터와 시퀀스의 경우 가능한 요소보다 더 많은 요소를 검색하려고 하면 ```NoSuchElementException``` 예외가 발생함

반면에 프로듀서의 경우는 얼마나 많은 요소를 가져오려 했는지 상관 없이 더 이상 요소가 없으면 중지됨

```Kotlin
producer.take(12).comsumeEach {
    println(it)
}
// 해당 코드는 실패하지 않음
```

다른 요소에 별개의 receive()를 추가하도록 코드를 수정하면 애플리케이션이 중단됨

```Kotlin
producer.take(12).consumeEach {
    println(it)
}

val element = producer.receive()
```

프로듀서가 실행을 완료하면 채널이 닫히기 때문에 중단이 발생함

발생하는 예외는 ```ClosedReceiveChannelException```

### 프로듀서를 사용한 피보나치 수열

이터레이터 및 시퀀스와 매우 비슷함

yield를 send로 바꾸기만 하면 됨

```Kotlin
val context = newSingleThreadContext("myThread")

val fibonacci = GlobalScope.produce(context) {
    send(1L)
    var current = 1L
    var next = 1L
    while (true) {
        send(next)
        val tmpNext = current + next
        current = next
        next = tmpNext
    }
}

fun main(args: Array<String>) = runBlocking {
    fibonacci.take(10).comsumeEach {
        println(it)
    }
}
```