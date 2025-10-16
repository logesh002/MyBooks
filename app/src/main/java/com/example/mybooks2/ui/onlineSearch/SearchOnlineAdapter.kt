package com.example.mybooks2.ui.onlineSearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mybooks2.R
import com.example.mybooks2.model.BookDoc

class SearchOnlineAdapter(private val onItemClicked: (BookDoc) -> Unit) :
    ListAdapter<BookDoc, SearchOnlineAdapter.BookDocViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookDocViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_online_search_result, parent, false)
        return BookDocViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookDocViewHolder, position: Int) {
        val bookDoc = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClicked(bookDoc)
        }
        holder.bind(bookDoc)
    }

    class BookDocViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)
        private val coverImageView: ImageView = itemView.findViewById(R.id.cover_image_view)

        fun bind(bookDoc: BookDoc) {
            titleTextView.text = bookDoc.title ?: "No Title"
            authorTextView.text = bookDoc.authorName?.joinToString(", ") ?: "Unknown Author"

            val coverUrl = bookDoc.getCoverUrl("M")
            coverImageView.load(coverUrl) {
                placeholder(R.drawable.outline_book_24)
                error(R.drawable.outline_book_24)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<BookDoc>() {
            override fun areItemsTheSame(oldItem: BookDoc, newItem: BookDoc): Boolean {
                return oldItem.title == newItem.title && oldItem.authorName == newItem.authorName
            }

            override fun areContentsTheSame(oldItem: BookDoc, newItem: BookDoc): Boolean {
                return oldItem == newItem
            }
        }
    }
}