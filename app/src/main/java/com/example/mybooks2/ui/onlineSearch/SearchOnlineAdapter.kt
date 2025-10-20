package com.example.mybooks2.ui.onlineSearch

import android.util.Log
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
import com.example.mybooks2.model.VolumeItem

class SearchOnlineAdapter(private val onItemClicked: (UnifiedSearchResult) -> Unit) :
    ListAdapter<UnifiedSearchResult, SearchOnlineAdapter.UnifiedResultViewHolder>(DiffCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnifiedResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_online_search_result, parent, false)
        return UnifiedResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: UnifiedResultViewHolder, position: Int) {
        val result = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClicked(result)
        }
        holder.bind(result)
    }

    class BookDocViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)
        private val coverImageView: ImageView = itemView.findViewById(R.id.cover_image_view)

        fun bind(bookDoc: VolumeItem) {
            titleTextView.text = bookDoc.volumeInfo?.title ?: "No Title"
            authorTextView.text = bookDoc.volumeInfo?.authors?.joinToString(", ") ?: "Unknown Author"

            val coverUrl = bookDoc.volumeInfo?.imageLinks?.smallThumbnail
            val secureCoverUrl = coverUrl?.replace("http://", "https://")
            coverImageView.load(secureCoverUrl) {
                placeholder(R.drawable.outline_book_24)
                error(R.drawable.outline_book_24)
            }
        }
    }

    class UnifiedResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverImageView: ImageView = itemView.findViewById(R.id.cover_image_view)
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)

        fun bind(result: UnifiedSearchResult) {
            titleTextView.text = result.title
            authorTextView.text = result.authors

            coverImageView.load(result.coverUrl) {
                placeholder(R.drawable.outline_book_24)
                error(R.drawable.outline_book_24)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<UnifiedSearchResult>() {
            override fun areItemsTheSame(
                oldItem: UnifiedSearchResult,
                newItem: UnifiedSearchResult
            ): Boolean {
                return (oldItem.isbn != null && oldItem.isbn == newItem.isbn) || (oldItem.title == newItem.title && oldItem.authors == newItem.authors)
            }

            override fun areContentsTheSame(
                oldItem: UnifiedSearchResult,
                newItem: UnifiedSearchResult
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}