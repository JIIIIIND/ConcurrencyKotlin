package com.example.rssreader.producer

import com.example.rssreader.model.Article
import com.example.rssreader.model.Feed
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

object ArticleProducer {
    private val feeds = listOf(
        Feed("npr", "https://www.npr.org/rss/rss.php?id=1001"),
        Feed("cnn", "http://rss.cnn.com/rss/cnn_topstories.rss"),
        Feed("fox", "http://feeds.foxnews.com/foxnews/latest?format=xml")
    )

    private val dispatcher = newFixedThreadPoolContext(2, "IO")
    private val factory = DocumentBuilderFactory.newInstance()

    /*
     * Chapter 3에서 사용
     * feed와 dispatcher를 인자로 받아 비동기로 처리
     * Chapter04에서 title만 추출하는 부분을 data class 형식으로 변경함
     */
    private fun fetchArticles(feed: Feed) : List<Article> {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse(feed.url)
        val news = xml.getElementsByTagName("channel").item(0)

        return (0 until news.childNodes.length)
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map { it as Element }
            .filter { "item" == it.tagName }
            .map {
                val title = it.getElementsByTagName("title")
                    .item(0)
                    .textContent
                var summary = it.getElementsByTagName("description")
                    .item(0)
                    .textContent
                // Summary가 비어있는 것을 피하기 위해 div로 시작하지 않는 경우에만 잘라냄
                if (!summary.startsWith("<div")
                    && summary.contains("<div")) {
                    summary = summary.substring(0, summary.indexOf("<div"))
                }
                Article(feed.name, title, summary)
            }
    }

    val producer = GlobalScope.produce(dispatcher) {
        feeds.forEach {
            send(fetchArticles(it))
        }
    }

}