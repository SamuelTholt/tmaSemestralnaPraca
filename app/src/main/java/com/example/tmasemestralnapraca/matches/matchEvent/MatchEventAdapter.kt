package com.example.tmasemestralnapraca.matches.matchEvent

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tmasemestralnapraca.databinding.MatchEventItemBinding

class MatchEventAdapter : ListAdapter<EventWithPlayer, MatchEventAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<EventWithPlayer>() {
        override fun areItemsTheSame(oldItem: EventWithPlayer, newItem: EventWithPlayer): Boolean {
            return oldItem.event.id == newItem.event.id
        }

        override fun areContentsTheSame(oldItem: EventWithPlayer, newItem: EventWithPlayer): Boolean {
            return oldItem == newItem
        }
    }
) {

    class ViewHolder(private val binding: MatchEventItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(eventWithPlayer: EventWithPlayer) {

            binding.bootTextView.visibility = View.GONE
            binding.textViewAssistBy.visibility = View.GONE

            val event = eventWithPlayer.event

            val player = eventWithPlayer.player
            binding.fullNameTv.text = player.firstName + " " + player.lastName
            binding.eventTimeTv.text = "${event.minute}′"

            val iconText = when (event.eventType) {
                EventType.GOAL -> "⚽"
                EventType.YELLOW_CARD -> "\uD83D\uDFE8"
                EventType.RED_CARD -> "\uD83D\uDFE5"
            }
            binding.playerEventTv.text = iconText

            if (eventWithPlayer.assistPlayer != null) {
                binding.bootTextView.visibility = View.VISIBLE
                binding.textViewAssistBy.visibility = View.VISIBLE

                binding.textViewAssistBy.text = eventWithPlayer.assistPlayer.firstName + " " + eventWithPlayer.assistPlayer.lastName
            } else {
                binding.bootTextView.visibility = View.GONE
                binding.textViewAssistBy.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MatchEventItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}