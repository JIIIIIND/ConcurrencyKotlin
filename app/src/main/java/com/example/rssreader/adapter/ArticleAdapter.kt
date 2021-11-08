package com.example.rssreader.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rssreader.databinding.ArticleBinding
import com.example.rssreader.model.Article
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface ArticleLoader {
    suspend fun loadMore()
}

class ArticleAdapter(
    private val loader: ArticleLoader
    ) : RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {
    private val articles: MutableList<Article> = mutableListOf()
    private var loading = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ArticleBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding.root, binding.feed, binding.title, binding.summary)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]

        if (!loading && position >= articles.size - 2) {
            loading = true
            GlobalScope.launch {
                loader.loadMore()
                loading = false
            }
        }
        holder.feed.text = article.feed
        holder.title.text = article.title
        holder.summary.text = article.summary
    }

    override fun getItemCount(): Int {
        return articles.size
    }

    fun add(articles: List<Article>) {
        this.articles.addAll(articles)
        notifyDataSetChanged()
    }
    class ViewHolder(
        val layout: LinearLayout,
        val feed: TextView,
        val title: TextView,
        val summary: TextView
    ) : RecyclerView.ViewHolder(layout)
}