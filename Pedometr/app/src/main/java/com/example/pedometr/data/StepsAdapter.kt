package com.example.pedometr.data

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pedometr.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepsAdapter : RecyclerView.Adapter<StepsAdapter.StepViewHolder>() {
    private var stepsList: List<StepEntry> = emptyList()

    class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stepsTextView: TextView = itemView.findViewById(R.id.stepsTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val stepEntry = stepsList[position]
        holder.stepsTextView.text = stepEntry.steps.toString()

        // Логирование для отладки
        Log.d("StepsAdapter", "Parsing date: ${stepEntry.date}")

        // Проверка формата даты перед парсингом
        val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
        if (!stepEntry.date.matches(dateRegex)) {
            Log.e("StepsAdapter", "Invalid date format: ${stepEntry.date}")
            holder.dateTextView.text = stepEntry.date // Отображаем как есть, если формат неверный
            return
        }

        // Преобразование даты из "yyyy-MM-dd" в "день месяц год"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("d MMMM yyyy г.", Locale("ru"))
        try {
            val date: Date? = inputFormat.parse(stepEntry.date)
            holder.dateTextView.text = date?.let { outputFormat.format(it) } ?: stepEntry.date
        } catch (e: Exception) {
            Log.e("StepsAdapter", "Failed to parse date: ${stepEntry.date}, error: ${e.message}")
            holder.dateTextView.text = stepEntry.date // Отображаем как есть в случае ошибки
        }
    }

    override fun getItemCount(): Int = stepsList.size

    fun submitList(newList: List<StepEntry>) {
        stepsList = newList.sortedByDescending { it.date }
        notifyDataSetChanged()
    }
}