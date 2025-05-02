package com.example.tmasemestralnapraca.matches

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tmasemestralnapraca.databinding.MatchItemBinding
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("DEPRECATION")
class MatchAdapter(private val listener: MatchClickListener,
                   private val isAdmin: Boolean) :
    ListAdapter<MatchModel, MatchAdapter.MatchViewHolder>(MatchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = MatchItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val currentMatch = getItem(position)
        holder.bind(currentMatch)
    }

    inner class MatchViewHolder(private val binding: MatchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val match = getItem(position)
                    if (match.played) {
                        listener.onMatchClick(match)
                    }
                }
            }

            binding.deleteBtn.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteMatchClick(getItem(position))
                }
            }

            binding.editBtn.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEditMatchClick(getItem(position))
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(match: MatchModel) {
            try {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = dateFormat.parse(match.date)

                if (date != null) {
                    val dayMonthFormat = SimpleDateFormat("dd.MM.", Locale.getDefault())
                    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    binding.matchDateDayMonthTv.text = dayMonthFormat.format(date)
                    binding.matchDateYearTv.text = yearFormat.format(date)
                    binding.matchDateTimeTv.text = timeFormat.format(date)
                }
            } catch (e: Exception) {
                binding.matchDateDayMonthTv.text = match.date
                binding.matchDateYearTv.text = ""
                binding.matchDateTimeTv.text = ""
            }


            val ourTeamName = "Náš tím"

            if (match.playedHome) {
                binding.homeTeamViewTv.text = ourTeamName
                binding.awayTeamTv.text = match.opponentName
                binding.homeAway.text = "Doma"

                Glide.with(binding.root.context)
                    .load(match.opponentLogo)
                    .centerCrop()
                    .into(binding.imgIconAwayTeam)

                if (match.played) {
                    binding.homeTeamScoreTv.text = match.ourScore.toString()

                    binding.awayTeamScoreTv.text = match.opponentScore.toString()
                } else {
                    binding.homeTeamScoreTv.text = "-"
                    binding.awayTeamScoreTv.text = "-"
                }
            } else {
                binding.homeTeamViewTv.text = match.opponentName
                binding.awayTeamTv.text = ourTeamName
                binding.homeAway.text = "Vonku"

                Glide.with(binding.root.context)
                    .load(match.opponentLogo)
                    .centerCrop()
                    .into(binding.imgIconHomeTeam)

                if (match.played) {
                    binding.homeTeamScoreTv.text = match.opponentScore.toString()
                    binding.awayTeamScoreTv.text = match.ourScore.toString()
                } else {
                    binding.homeTeamScoreTv.text = "-"
                    binding.awayTeamScoreTv.text = "-"
                }
            }

            if (match.played) {
                binding.winLoseDrawTv.visibility = View.VISIBLE

                if (match.playedHome) {
                    when {
                        match.ourScore > match.opponentScore -> {
                            binding.winLoseDrawTv.text = "W"
                            binding.winLoseDrawTv.setBackgroundResource(com.example.tmasemestralnapraca.R.color.green008000)
                        }
                        match.ourScore < match.opponentScore -> {
                            binding.winLoseDrawTv.text = "L"
                            binding.winLoseDrawTv.setBackgroundResource(com.example.tmasemestralnapraca.R.color.redFF0000)
                        }
                        else -> {
                            binding.winLoseDrawTv.text = "D"
                            binding.winLoseDrawTv.setBackgroundResource(com.example.tmasemestralnapraca.R.color.grey808080)
                        }
                    }
                } else {
                    when {
                        match.ourScore > match.opponentScore -> {
                            binding.winLoseDrawTv.text = "W"
                            binding.winLoseDrawTv.setBackgroundResource(com.example.tmasemestralnapraca.R.color.green008000)
                        }
                        match.ourScore < match.opponentScore -> {
                            binding.winLoseDrawTv.text = "L"
                            binding.winLoseDrawTv.setBackgroundResource(com.example.tmasemestralnapraca.R.color.redFF0000)
                        }
                        else -> {
                            binding.winLoseDrawTv.text = "D"
                            binding.winLoseDrawTv.setBackgroundResource(com.example.tmasemestralnapraca.R.color.grey808080)
                        }
                    }
                }
            } else {
                binding.winLoseDrawTv.visibility = View.INVISIBLE
            }

            if (isAdmin) {
                binding.editBtn.visibility = View.VISIBLE
                binding.deleteBtn.visibility = View.VISIBLE
                binding.infoBtn.visibility = View.VISIBLE
            } else {
                binding.editBtn.visibility = View.GONE
                binding.deleteBtn.visibility = View.GONE
                binding.infoBtn.visibility = View.VISIBLE
            }
        }
    }

    interface MatchClickListener {
        fun onMatchClick(match: MatchModel)
        fun onEditMatchClick(match: MatchModel)
        fun onDeleteMatchClick(match: MatchModel)
    }

    class MatchDiffCallback : DiffUtil.ItemCallback<MatchModel>() {
        override fun areItemsTheSame(oldItem: MatchModel, newItem: MatchModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MatchModel, newItem: MatchModel): Boolean {
            return oldItem == newItem
        }
    }
}