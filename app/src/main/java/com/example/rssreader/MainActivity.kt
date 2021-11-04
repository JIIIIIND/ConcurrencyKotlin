package com.example.rssreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.rssreader.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

/*
 * join(): async 블록 안에서 발생하는 예외는 결과에 첨부되기에 결과를 확인해야 예외를 찾을 수 있음
 * isCancelled/getCancellationException() 메서드를 사용
 * await() 사용 시에는 비정상적으로 중단됨
 * 이 경우를 예외를 감싸지 않고 전파하는, 감싸지 않은 디퍼드라고 함(unwrapping deferred)
 * 결과를 반환하지 않는 코루틴은 launch를 사용, launch는 연산이 실패한 경우에만 통보 받기를 원하는 시나리오를 위한 설계
 * 예외가 스택에 출력되지만 중단되지 않음
 */
class MainActivity : AppCompatActivity() {
    /* 스레드를 하나만 가지는 CoroutinbeDispatecher 생성
     * 이 Dispatcher를 사용하는 코루틴은 모두 특정 스레드에서 실행이 됨
    */
    @ExperimentalCoroutinesApi
    private val dispatcher = newSingleThreadContext(name = "ServiceCall")
    private val factory = DocumentBuilderFactory.newInstance()
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // 1번 옵션 사용 시 아래와 같이 Dispatcher를 지정해야 함
        GlobalScope.launch (dispatcher) {
            loadNews()
        }
        // 2번 옵션 사용 시 아래와 같이 바로 호출할 수 있음. 하지만 백그라운드 스레드에서 강제로 실행되기에 함수의 유연성이 떨어짐
        // 비동기로 실행된다는 것을 명시적으로 나타내기 위해 관례적으로 async라는 명칭을 앞에 붙임
        asyncLoadNews()
    }

    private fun fetchRssHeadlines(): List<String> {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse("https://www.npr.org/rss/rss.php?id=1001")
        val news = xml.getElementsByTagName("channel").item(0)
        // XML의 모든 요소들을 검사하면서 피드에 있는 각 기사의 제목을 제외한 모든 것을 필터링함
        return (0 until news.childNodes.length)
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map { it as Element }
            .filter { "item" == it.tagName }
            .map {
                it.getElementsByTagName("title").item(0).textContent
            }
    }

    // 1. 비동기 호출자로 감싼 동기 함수
    private fun loadNews() {
        val headlines = fetchRssHeadlines()
        val newsCount = binding.newsCount
        // ui 업데이트는 UI스레드에서 발생해야 하기에 여기에 작성되면 App이 중단됨
        GlobalScope.launch(Dispatchers.Main) {
            newsCount.text = "Found ${headlines.size} News"
        }
    }

    // 2. 미리 정의된 디스패처를 갖는 비동기 함수
    private fun asyncLoadNews() = GlobalScope.launch (dispatcher) {
        val headlines = fetchRssHeadlines()
        val newsCount = binding.newsCount
        launch(Dispatchers.Main) {
            newsCount.text = "Found ${headlines.size} News"
        }
    }

    override fun onResume() {
        super.onResume()
        // 이러면 메인 스레드가 정지되기 때문에 화면이 업데이트 되지 않음 -> UI 스레드는 뷰를 만들고 업데이트 하는곳에 사용하고 나머지는 백그라운드 스레드를 사용
//        Thread.sleep(5000)
    }
}