package com.example.tmasemestralnapraca.gallery

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tmasemestralnapraca.databinding.ImageItemBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class GalleryAdapter(private val listener: PhotoClickListener,
                     private val isAdmin: Boolean)
    : ListAdapter<ImageModel, GalleryAdapter.PhotoViewHolder>(DiffUtilCallback()) {

    inner class PhotoViewHolder(private val binding: ImageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.deleteBtn.setOnClickListener {
                listener.onDeletePhotoClick(getItem(adapterPosition))
            }
        }

        fun bind(imageModel: ImageModel) {

            Glide.with(binding.root.context)
                .load(imageModel.imagePath)
                .centerCrop()
                .into(binding.photoImageView)

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formatedDate = dateFormat.format(Date(imageModel.imageDate))
            binding.photoDateTv.text = formatedDate


            if(isAdmin) {
                binding.deleteBtn.visibility = ViewGroup.VISIBLE
            } else {
                binding.deleteBtn.visibility = ViewGroup.GONE
            }

        }
    }

    class DiffUtilCallback : DiffUtil.ItemCallback<ImageModel>() {
        override fun areItemsTheSame(oldItem: ImageModel, newItem: ImageModel) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ImageModel, newItem: ImageModel) = oldItem == newItem
    }

    interface PhotoClickListener {
        fun onDeletePhotoClick(imageModel: ImageModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(ImageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}