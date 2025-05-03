package com.example.tmasemestralnapraca.player

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tmasemestralnapraca.databinding.PlayerItemBinding

@Suppress("DEPRECATION")
class PlayerAdapter(private val listener: PlayerClickListener,
                    private val isAdmin: Boolean) :
    ListAdapter<PlayerModel, PlayerAdapter.PlayerViewHolder>(PlayerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = PlayerItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val currentPlayer = getItem(position)
        holder.bind(currentPlayer)
    }

    inner class PlayerViewHolder(private val binding: PlayerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.infoBtn.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onInfoPlayerClick(getItem(position))
                }
            }

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
        fun bind(player: PlayerModel) {
            binding.firstNameTv.text = player.firstName
            binding.lastNameTv.text = player.lastName
            binding.numberOfShirtTv.text = player.numberOfShirt.toString()
            binding.positionTv.text = player.position

            if(isAdmin) {
                binding.editBtn.visibility = ViewGroup.VISIBLE
                binding.deleteBtn.visibility = ViewGroup.VISIBLE
            } else {
                binding.editBtn.visibility = ViewGroup.GONE
                binding.deleteBtn.visibility = ViewGroup.GONE
            }

        }
    }

    interface PlayerClickListener {
        fun onEditPlayerClick(player: PlayerModel)
        fun onDeletePlayerClick(player: PlayerModel)
        fun onInfoPlayerClick(player: PlayerModel)
    }

    class PlayerDiffCallback : DiffUtil.ItemCallback<PlayerModel>() {
        override fun areItemsTheSame(oldItem: PlayerModel, newItem: PlayerModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PlayerModel, newItem: PlayerModel): Boolean {
            return oldItem == newItem
        }
    }
}