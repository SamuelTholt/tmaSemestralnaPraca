package com.example.tmasemestralnapraca.teams

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tmasemestralnapraca.databinding.TeamItemBinding

@Suppress("DEPRECATION")
class TeamAdapter(private val listener: TeamDetailsClickListener,
                  private val isAdmin: Boolean)
    : ListAdapter<TeamModel, TeamAdapter.TeamDetailsViewHolder>(DiffUtilCallback()) {

    inner class TeamDetailsViewHolder(private val binding: TeamItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.editBtn.setOnClickListener {
                listener.onEditTeamClick(getItem(adapterPosition))
            }

            binding.deleteBtn.setOnClickListener {
                listener.onDeleteTeamClick(getItem(adapterPosition))
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(team: TeamModel) {
            binding.position.text = team.position.toString()
            binding.teamViewTv.text = team.teamName

            Glide.with(binding.root.context)
                .load(team.teamImageLogoPath)
                .centerCrop()
                .into(binding.imgIconTeam)


            binding.teamPlayedMatchesTv.text = team.playedMatches.toString()
            binding.teamPoints.text = team.points.toString()

            binding.teamScoreTv.text = team.goalsScored.toString() + ":" + team.goalsConceded.toString()

            if(isAdmin) {
                binding.editBtn.visibility = ViewGroup.VISIBLE
                binding.deleteBtn.visibility = ViewGroup.VISIBLE
            } else {
                binding.editBtn.visibility = ViewGroup.GONE
                binding.deleteBtn.visibility = ViewGroup.GONE
            }

        }
    }

    class DiffUtilCallback : DiffUtil.ItemCallback<TeamModel>() {
        override fun areItemsTheSame(oldItem: TeamModel, newItem: TeamModel) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TeamModel, newItem: TeamModel) = oldItem == newItem
    }

    interface TeamDetailsClickListener {
        fun onEditTeamClick(team: TeamModel)
        fun onDeleteTeamClick(team: TeamModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamDetailsViewHolder {
        return TeamDetailsViewHolder(TeamItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: TeamDetailsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}