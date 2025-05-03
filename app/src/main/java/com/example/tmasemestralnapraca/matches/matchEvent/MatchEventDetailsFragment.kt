package com.example.tmasemestralnapraca.matches.matchEvent

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tmasemestralnapraca.databinding.FragmentMatchEventDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MatchEventDetailsFragment : Fragment(), AddEditMatchEventFragment.AddEditEventListener {

    private var _binding: FragmentMatchEventDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MatchEventAdapter
    private val repository = MatchEventRepository()

    private var isAdmin: Boolean = false

    private var matchId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchEventDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        isAdmin = sharedPref.getBoolean("isAdminLoggedIn", false)

        matchId = arguments?.getString("matchId")

        initRecyclerView()
        observeMatchEvents()

        binding.btnAddEvent.setOnClickListener {
            showAddEditDialog()
        }

        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.buttonZostava.setOnClickListener {
            // Tu otvoríš zostavu hráčov - podľa potreby
        }
    }

    private fun initRecyclerView() {
        adapter = MatchEventAdapter(this, isAdmin)
        binding.eventsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.eventsRecyclerView.adapter = adapter
    }

    private fun observeMatchEvents() {
        matchId?.let { id ->
            lifecycleScope.launch {
                repository.getAllEventsToMatchByMatchId(id).collect { events ->
                    adapter.submitList(events)
                }
            }
        }
    }

    private fun showAddEditDialog(event: MatchEvent? = null) {
        val dialog = AddEditMatchEventFragment.newInstance(matchId!!, event?.id)
        dialog.setTargetFragment(this, 0)
        dialog.show(parentFragmentManager, AddEditMatchEventFragment.TAG)
    }

    override fun onSaveBtnClicked(isUpdate: Boolean, matchEvent: MatchEvent) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isUpdate) {
                repository.updateEvent(matchEvent)
            } else {
                repository.saveMatchEvent(matchEvent)
            }
        }
        Toast.makeText(
            requireContext(),
            if (isUpdate) "Udalosť bola upravená" else "Udalosť bola pridaná",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
