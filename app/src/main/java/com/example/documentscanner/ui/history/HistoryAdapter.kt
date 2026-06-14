package com.example.documentscanner.ui.history

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.document.scanner.R
import com.example.documentscanner.data.model.ScannedDocument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (ScannedDocument) -> Unit,
    private val onMoreClick: (ScannedDocument, View) -> Unit
) : ListAdapter<ScannedDocument, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_scan, parent, false)
        return ViewHolder(view, onItemClick, onMoreClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (ScannedDocument) -> Unit,
        private val onMoreClick: (ScannedDocument, View) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val tvDocumentName: TextView = itemView.findViewById(R.id.tvDocumentName)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        private val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)

        fun bind(document: ScannedDocument) {
            tvDocumentName.text = document.displayName
            tvDateTime.text = formatDateTime(document.createdAt)
            tvFileSize.text = formatFileSize(document.fileSizeBytes)

            ivThumbnail.setImageURI(Uri.parse(document.firstImageUri()))

            itemView.setOnClickListener { onItemClick(document) }
            btnMore.setOnClickListener { onMoreClick(document, it) }
        }

        private fun formatDateTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        private fun formatFileSize(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScannedDocument>() {
        override fun areItemsTheSame(
            oldItem: ScannedDocument,
            newItem: ScannedDocument
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ScannedDocument,
            newItem: ScannedDocument
        ) = oldItem == newItem
    }
}
