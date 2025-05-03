package com.example.tmasemestralnapraca.matches.matchLineup

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tmasemestralnapraca.databinding.MatchPlayerItemBinding

class MatchLineupAdapter : ListAdapter<PlayerWithStats, MatchLineupAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<PlayerWithStats>() {
        override fun areItemsTheSame(oldItem: PlayerWithStats, newItem: PlayerWithStats): Boolean {
            return oldItem.player.id == newItem.player.id
        }

        override fun areContentsTheSame(oldItem: PlayerWithStats, newItem: PlayerWithStats): Boolean {
            return oldItem == newItem
        }
    }
) {

    class ViewHolder(private val binding: MatchPlayerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(playerStats: PlayerWithStats) {
            val player = playerStats.player

            binding.numberOfShirtTextView.text = player.numberOfShirt.toString()
            binding.fullNameTv.text = "${player.firstName} ${player.lastName}"

            val statsText = buildString {
                if (playerStats.goals > 0) append("⚽ ${playerStats.goals} ")
                if (playerStats.assists > 0) append("👟 ${playerStats.assists} ")
                if (playerStats.yellowCards > 0) append("🟨 ${playerStats.yellowCards} ")
                if (playerStats.redCards > 0) append("🟥 ${playerStats.redCards}")
            }

            if (statsText.isNotEmpty()) {
                binding.playerEventTv.text = statsText
                binding.playerEventTv.visibility = View.VISIBLE
            } else {
                binding.playerEventTv.visibility = View.GONE
            }

            if (playerStats.minutesIn != null) {
                val timeText = if (playerStats.minutesOut != null) {
                    "${playerStats.minutesIn}′ - ${playerStats.minutesOut}′"
                } else {
                    "${playerStats.minutesIn}′ →"
                }
                binding.minutesInOutTv.text = timeText
                binding.minutesInOutTv.visibility = View.VISIBLE
            } else {
                binding.minutesInOutTv.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MatchPlayerItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}