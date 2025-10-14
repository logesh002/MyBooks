package com.example.mybooks2.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mybooks2.R
import com.example.mybooks2.model.Book
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.addBook2.ReadingStatus
import com.google.android.material.card.MaterialCardView
import java.io.File

class BookAdapter(private val onItemClicked: (Book) -> Unit,
                  private val onItemLongClicked: (Book) -> Unit) :
    ListAdapter<Book, RecyclerView.ViewHolder>(DiffCallback) {


    private var currentLayoutMode: LayoutMode = LayoutMode.GRID // Default to Grid

    private val VIEW_TYPE_GRID = 0
    private val VIEW_TYPE_LIST = 1
    var isSelectionModeActive: Boolean = false

    private var selectedIds: Set<Long> = emptySet()

    fun setSelectedIds(ids: Set<Long>) {
        selectedIds = ids
        notifyDataSetChanged()
    }
    fun setLayoutMode(mode: LayoutMode) {
        currentLayoutMode = mode
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_GRID) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.grid_item_book, parent, false)
            BookViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_book, parent, false)
            ListViewHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (currentLayoutMode == LayoutMode.GRID) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onBindViewHolder(holder:  RecyclerView.ViewHolder, position: Int) {
        val book = getItem(position)
        val isSelected = selectedIds.contains(book.id)


        val cardView = holder.itemView.findViewById<MaterialCardView>(R.id.card_item)
        cardView.isActivated = isSelected

        if (isSelected) {
            cardView.strokeWidth = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.selected_card_stroke_width) // Define this in dimens.xml
            cardView.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.your_selection_border_color)
        } else {
            cardView.strokeWidth = 0
        }

        // Handle clicks
        holder.itemView.setOnClickListener {
            if (isSelectionModeActive) {
                onItemLongClicked(book)
            } else {
                onItemClicked(book)
            }
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(book)
            true
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(book)
            true
        }

        if (holder is BookViewHolder) {
            holder.bind(book)
        } else if (holder is ListViewHolder) {
            holder.bind(book)
        }
    }
    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverImageView: ImageView = itemView.findViewById(R.id.image_view_cover)
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar)

        private val formatIcon: ImageView = itemView.findViewById(R.id.icon_bookmark)

        fun bind(book: Book) {
            titleTextView.text = book.title
            authorTextView.text = book.author
            ratingBar.rating = book.personalRating?.toFloat() ?: 0f

            if(book.status == ReadingStatus.FINISHED){
                ratingBar.visibility= View.VISIBLE
            }
            else{
                ratingBar.visibility= View.GONE
            }

            if (!book.coverImagePath.isNullOrEmpty()) {
                coverImageView.visibility = View.VISIBLE
                coverImageView.load(File(book.coverImagePath)) { crossfade(true) }
            } else {
                coverImageView.visibility = View.GONE
                coverImageView.setImageDrawable(null)
            }

            val iconRes = when (book.format) {
                BookFormat.PAPERBACK -> R.drawable.outline_menu_book_24
                BookFormat.EBOOK -> R.drawable.outline_fullscreen_portrait_24
                BookFormat.AUDIOBOOK -> R.drawable.outline_headphones_24
            }

            formatIcon.setImageResource(iconRes)
        }
    }

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverImageView: ImageView = itemView.findViewById(R.id.image_view_cover)
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)

        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)

        fun bind(book: Book) {
            if (!book.coverImagePath.isNullOrEmpty()) {
                titleTextView.visibility = View.GONE
                authorTextView.visibility = View.GONE
                coverImageView.visibility = View.VISIBLE

                val imageFile = File(book.coverImagePath)
                coverImageView.load(imageFile) {
                    crossfade(true)
                    placeholder(R.drawable.outline_downloading_24)
                    error(R.drawable.outline_book_24)
                }
            } else {
                titleTextView.visibility = View.VISIBLE
                authorTextView.visibility = View.VISIBLE
                coverImageView.visibility = View.INVISIBLE

                coverImageView.setImageDrawable(null)

                titleTextView.text = book.title
                authorTextView.text = book.author
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem == newItem
            }
        }
    }
}