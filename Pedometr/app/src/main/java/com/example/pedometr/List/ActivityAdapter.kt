package com.example.pedometr.List

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pedometr.R
import com.example.pedometr.databinding.ItemActivityBinding
import com.example.pedometr.mariaDb.UserActivity

class ActivityAdapter : ListAdapter<HighlightedActivity, ActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ActivityViewHolder(private val binding: ItemActivityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(highlightedActivity: HighlightedActivity) {
            val activity = highlightedActivity.activity
            val matchedFields = highlightedActivity.matchedFields

            binding.userGroupText.text = applyHighlight(activity.user_group, matchedFields["user_group"])
            binding.activityDateText.text = applyHighlight(activity.activity_date, matchedFields["activity_date"])
            binding.stepsText.text = applyHighlight(activity.steps.toString(), matchedFields["steps"])
            binding.distanceText.text = applyHighlight("${activity.distance_km} km", matchedFields["distance_km"])
            binding.activeTimeText.text = applyHighlight("${activity.active_time_minutes} min", matchedFields["active_time_minutes"])
        }

        private fun applyHighlight(text: String, matchedQuery: String?): SpannableString {
            val spannable = SpannableString(text)
            if (!matchedQuery.isNullOrBlank()) {
                val textLower = text.lowercase()
                var start = textLower.indexOf(matchedQuery)
                while (start != -1) {
                    val end = start + matchedQuery.length
                    spannable.setSpan(
                        BackgroundColorSpan(Color.YELLOW),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    start = textLower.indexOf(matchedQuery, start + 1)
                }
            }
            return spannable
        }
    }

    class ActivityDiffCallback : DiffUtil.ItemCallback<HighlightedActivity>() {
        override fun areItemsTheSame(oldItem: HighlightedActivity, newItem: HighlightedActivity): Boolean {
            return oldItem.activity.activity_date == newItem.activity.activity_date &&
                    oldItem.activity.user_group == newItem.activity.user_group
        }

        override fun areContentsTheSame(oldItem: HighlightedActivity, newItem: HighlightedActivity): Boolean {
            return oldItem == newItem
        }
    }
}