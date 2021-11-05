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