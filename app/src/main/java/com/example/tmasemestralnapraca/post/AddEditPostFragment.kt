package com.example.tmasemestralnapraca.post

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tmasemestralnapraca.databinding.FragmentAddEditPostBinding
import com.example.tmasemestralnapraca.player.AddEditPlayerFragment
import com.example.tmasemestralnapraca.player.AddEditPlayerFragment.Companion
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class AddEditPostFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentAddEditPostBinding
    private var listener : AddEditPostListener? = null

    private var post: PostModel? = null

    private val postRepository = PostRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddEditPostBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val target = targetFragment
        if (target is AddEditPostListener) {
            listener = target
        } else {
            Log.e(TAG, "Target fragment does not implement AddEditPostListener")
            throw ClassCastException("Target fragment must implement AddEditPostListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getString("postId")
        if (postId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                post = postRepository.getPostById(postId)
                post?.let { setExistingDataOnUi(it) }
            }
        }
        post?.let { setExistingDataOnUi(it) }
        attachUiListener()
    }

    private fun attachUiListener() {
        binding.saveBtn.setOnClickListener {
            val postHeader = binding.postHeaderEditText.text.toString()
            val postText = binding.postTexteEditText.text.toString()

            if (postHeader.isNotEmpty() && postText.isNotEmpty()) {
                val newPost = PostModel(
                    id = post?.id,
                    postHeader = postHeader,
                    postText = postText,
                )

                Log.d("PostData", "Saving post: $newPost")
                coroutineScope.launch {
                    try {
                        listener?.onSaveBtnClicked(post != null, newPost)
                    } catch (e: Exception) {
                        Log.e(AddEditPlayerFragment.TAG, "Error saving post", e)
                    }
                }
            }
            dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setExistingDataOnUi(postModel: PostModel) {
        binding.postHeaderEditText.setText(postModel.postHeader)
        binding.postTexteEditText.setText(postModel.postText)

        binding.saveBtn.text = "Update"
    }

    companion object {
        const val TAG = "AddEditPostFragment"

        @JvmStatic
        fun newInstance(postId: String?) = AddEditPostFragment().apply {
            arguments = Bundle().apply {
                putString("postId", postId)
            }
        }
    }

    interface AddEditPostListener {
        fun onSaveBtnClicked(isUpdate: Boolean, postModel: PostModel)
    }

}