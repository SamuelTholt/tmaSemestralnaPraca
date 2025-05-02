package com.example.tmasemestralnapraca.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tmasemestralnapraca.databinding.PostItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class PostAdapter(private val listener: PostDetailsClickListener,
                  private val isAdmin: Boolean)
    : ListAdapter<PostModel, PostAdapter.PostDetailsViewHolder>(DiffUtilCallback()) {

    inner class PostDetailsViewHolder(private val binding: PostItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.editBtn.setOnClickListener {
                listener.onEditPostClick(getItem(adapterPosition))
            }

            binding.deleteBtn.setOnClickListener {
                listener.onDeletePostClick(getItem(adapterPosition))
            }
        }

        fun bind(postModel: PostModel) {
            binding.postHeaderTv.text = postModel.postHeader
            binding.postTextTv.text = postModel.postText

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formatedDate = dateFormat.format(Date(postModel.postDate))
            binding.postDateTv.text = formatedDate

            if(isAdmin) {
                binding.editBtn.visibility = ViewGroup.VISIBLE
                binding.deleteBtn.visibility = ViewGroup.VISIBLE
            } else {
                binding.editBtn.visibility = ViewGroup.GONE
                binding.deleteBtn.visibility = ViewGroup.GONE
            }

        }
    }

    class DiffUtilCallback : DiffUtil.ItemCallback<PostModel>() {
        override fun areItemsTheSame(oldItem: PostModel, newItem: PostModel) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PostModel, newItem: PostModel) = oldItem == newItem
    }

    interface PostDetailsClickListener {
        fun onEditPostClick(postModel: PostModel)
        fun onDeletePostClick(postModel: PostModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostDetailsViewHolder {
        return PostDetailsViewHolder(PostItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: PostDetailsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}