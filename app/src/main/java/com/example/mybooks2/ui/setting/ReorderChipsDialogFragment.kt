package com.example.mybooks2.ui.setting

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybooks2.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class ReorderChipsDialogFragment : DialogFragment() {



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reorder_chips, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.reorder_recycler_view)

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val defaultOrder = resources.getStringArray(R.array.status_values).toList()
        val savedOrderStr = prefs.getString("chip_order_preference", defaultOrder.joinToString(","))
        val currentOrderList = savedOrderStr!!.split(",").toMutableList()

        val adapter = ReorderAdapter(currentOrderList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(currentOrderList, fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("chip_order_preference", currentOrderList.joinToString(",")).apply()
            }
            .create()
    }

    class ReorderAdapter(private val items: List<String>) : RecyclerView.Adapter<ReorderAdapter.ViewHolder>() {
        val statusTextMap = mapOf(
            "IN_PROGRESS" to "In progress",
            "FINISHED" to "Finished",
            "FOR_LATER" to "To be read",
            "UNFINISHED" to "Dropped"
        )
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_reorder, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val statusValue = items[position]
           val statusText = statusTextMap[statusValue] ?: statusValue
            holder.textView.text = statusText
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.text_view_item)
        }
    }
}