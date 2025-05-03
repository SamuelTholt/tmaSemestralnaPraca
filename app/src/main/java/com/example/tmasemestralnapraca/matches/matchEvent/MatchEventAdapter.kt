package com.example.tmasemestralnapraca.matches.matchEvent

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tmasemestralnapraca.databinding.MatchEventItemBinding

@Suppress("DEPRECATION")
class MatchEventAdapter(private val listener: MatchEventDetailsFragment,
                        private val isAdmin: Boolean) :
    ListAdapter<EventWithPlayer, MatchEventAdapter.MatchEventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchEventViewHolder {
        val binding = MatchEventItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MatchEventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchEventViewHolder, position: Int) {
        val currentPlayer = getItem(position)
        holder.bind(currentPlayer)
    }

    inner class MatchEventViewHolder(private val binding: MatchEventItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.deleteBtn.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeletePlayerClick(getItem(position))
                }
            }

            binding.editBtn.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEditPlayerClick(getItem(position))
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(eventWithPlayer: EventWithPlayer) {

            binding.bootTextView.visibility = View.GONE
            binding.textViewAssistBy.visibility = View.GONE

            val event = eventWithPlayer.event


            binding.eventTimeTv.text = "${event.minute}′"

            val iconText = when (event.eventType) {
                EventType.GOAL -> "⚽"
                EventType.YELLOW_CARD -> "\uD83D\uDFE5"
                EventType.RED_CARD -> "\uD83D\uDFE8"
                EventType.SUBSTITUTION_IN -> "↗️"
                EventType.SUBSTITUTION_OUT -> "↘️"
            }
            binding.playerEventTv.text = iconText

            if (eventWithPlayer.assistPlayer != null) {
                binding.bootTextView.visibility = View.VISIBLE
                binding.textViewAssistBy.visibility = View.VISIBLE

                binding.textViewAssistBy.text =
                    eventWithPlayer.assistPlayer.firstName + " " + eventWithPlayer.assistPlayer.lastName
            } else {
                binding.bootTextView.visibility = View.GONE
                binding.textViewAssistBy.visibility = View.GONE
            }
        }
    }


    interface MatchEventClickListener {
        fun onEditPlayerClick(eventWithPlayer: EventWithPlayer)
        fun onDeletePlayerClick(eventWithPlayer: EventWithPlayer)
    }

    class EventDiffCallback : DiffUtil.ItemCallback<EventWithPlayer>() {
        override fun areItemsTheSame(oldItem: EventWithPlayer, newItem: EventWithPlayer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EventWithPlayer, newItem: EventWithPlayer): Boolean {
            return oldItem == newItem
        }
    }

}