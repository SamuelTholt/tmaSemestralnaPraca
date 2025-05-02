package com.example.tmasemestralnapraca.post

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tmasemestralnapraca.databinding.FragmentPostBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class PostFragment : Fragment(), AddEditPostFragment.AddEditPostListener,
    PostAdapter.PostDetailsClickListener {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostAdapter

    private var currentSort: String = "Zostupne"

    private var isAdmin: Boolean = false

    private val postRepository = PostRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        isAdmin = sharedPref.getBoolean("isAdminLoggedIn", false)

        initRecyclerView()
        attachListeners()
        observePosts()

        binding.floatingActionButton.visibility = if (isAdmin) View.VISIBLE else View.GONE


        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSort = parent.getItemAtPosition(position).toString()
                observePosts()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun initRecyclerView() {
        adapter = PostAdapter(this, isAdmin)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun attachListeners() {
        binding.floatingActionButton.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun showBottomSheet(postModel: PostModel? = null) {
        val bottomSheet = AddEditPostFragment.newInstance(postModel?.id)
        bottomSheet.setTargetFragment(this, 0)
        bottomSheet.show(parentFragmentManager, AddEditPostFragment.TAG)
    }

    private fun observePosts() {
        lifecycleScope.launch {
            when(currentSort) {
                "Vzostupne" -> postRepository.getPostSortedByDateAsc()
                "Zostupne" -> postRepository.getPostSortedByDateDesc()
                else -> postRepository.getAllPosts()
            }.collect { posts ->
                adapter.submitList(posts)
            }
        }
    }

    override fun onSaveBtnClicked(isUpdate: Boolean, postModel: PostModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isUpdate)
                postRepository.updatePost(postModel)
            else
                postRepository.savePost(postModel)
        }
        if (isUpdate)
            Toast.makeText(requireContext(), "Príspevok bol úspešne upravený!", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(requireContext(), "Príspevok bol úspešne pridaný!", Toast.LENGTH_SHORT).show()
    }

    override fun onEditPostClick(postModel: PostModel) {
        showBottomSheet(postModel)
    }

    override fun onDeletePostClick(postModel: PostModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            postRepository.deletePostById(postModel.id.toString())
        }
        Toast.makeText(requireContext(), "Príspevok bol úspešne odstránený!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}