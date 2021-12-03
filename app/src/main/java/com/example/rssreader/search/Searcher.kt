package com.example.rssreader.search

import com.example.rssreader.model.Article
import com.example.rssreader.model.Feed
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class Searcher() {

    // 문서를 검색하기 위한 디스패처와 문서 팩토리를 추가함
    val dispatcher = newFixedThreadPoolContext(3, "IO-Search")
    val factory = DocumentBuilderFactory.newInstance()

    val feeds = listOf(
        Feed("npr", "https://www.npr.org/rss/rss.php?id=1001"),
        Feed("cnn", "http://rss.cnn.com/rss/cnn_topstories.rss"),
        Feed("fox", "http://feeds.foxnews.com/foxnews/latest?format=xml")
    )
    // 제목이나 설명을 포함해서 기사를 필터링하고 파라미터로 전달 받은 SendChannel을 통해 전송하는 search()함수를 추가할 수 있음
    // 초기 구현은 producer에서 사용했던 것과 매우 유사함

    private suspend fun search(
        feed: Feed,
        channel: SendChannel<Article>,
        query: String
        ) {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse(feed.url)
        val news = xml.getElementsByTagName("channel").item(0)

        (0 until news.childNodes.length)
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map { it as Element }
            .filter { "item" == it.tagName }
            .forEach {
                // 파싱 및 필터링, 모든 콘텐츠를 매핑하는 대신 필터링한 기사를 채널을 통해 전송함
                val title = it.getElementsByTagName("title")
                    .item(0)
                    .textContent
                var summary = it.getElementsByTagName("description")
                    .item(0)
                    .textContent

                if (title.contains(query) || summary.contains(query)) {
                    if (summary.contains("<div")) {
                        summary = summary.substring(0, summary.indexOf("<div"))
                    }
                    val article = Article(feed.name, title, summary)
                    channel.send(article)
                }
            }
    }

    /* 검색 함수 연결
     * 위의 비공개 search()함수와 쿼리를 받아 receiveChannel을 반환하는 공개 search()함수를 연결
     * 공개된 search 함수를 호출하는 호출자는 기사를 수신할 수 있음
     */

    fun search(query: String) : ReceiveChannel<Article> {
        val channel = Channel<Article>(15)

        feeds.forEach { feed ->
            GlobalScope.launch(dispatcher) {
                search(feed, channel, query)
            }
        }
        return channel
    }
}