package com.example.rssreader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rssreader.adapter.ArticleAdapter
import com.example.rssreader.databinding.ActivitySearchBinding
import com.example.rssreader.search.Searcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private val binding by lazy { ActivitySearchBinding.inflate(layoutInflater) }
    private val searcher = Searcher()

    private lateinit var articles: RecyclerView
    private lateinit var viewAdapter: ArticleAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        viewManager = LinearLayoutManager(this)
        viewAdapter = ArticleAdapter()
        articles = binding.articles.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        binding.searchButton.setOnClickListener {
            viewAdapter.clear()
            GlobalScope.launch {
                search()
            }
        }
    }

    private suspend fun search() {
        val query = binding.searchText.text.toString()
        val channel = searcher.search(query)

        while (!channel.isClosedForReceive) {
            val article = channel.receive()

            GlobalScope.launch(Dispatchers.Main) {
                viewAdapter.add(article)
            }
        }
    }
}