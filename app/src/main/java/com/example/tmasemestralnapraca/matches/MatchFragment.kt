package com.example.tmasemestralnapraca.matches

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
import com.example.tmasemestralnapraca.databinding.FragmentMatchBinding
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class MatchFragment : Fragment(), AddEditMatchFragment.AddEditMatchListener,
    MatchAdapter.MatchClickListener {

    private var _binding: FragmentMatchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MatchAdapter

    private var currentSort: String = "Zostupne"
    private var currentFilter: String = "All"
    private var isAdmin: Boolean = false

    private val matchRepository = MatchRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        isAdmin = sharedPref.getBoolean("isAdminLoggedIn", false)

        initRecyclerView()
        setupButtonListeners()
        setupSortSpinner()
        observeMatches()

        binding.floatingActionButton.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun initRecyclerView() {
        adapter = MatchAdapter(this, isAdmin)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtonListeners() {
        binding.buttonAllMatches.setOnClickListener {
            currentFilter = "All"
            updateFilterButtonState()
            observeMatches()
        }

        binding.buttonPlayedMatches.setOnClickListener {
            currentFilter = "Played"
            updateFilterButtonState()
            observeMatches()
        }

        binding.buttonUnplayedMatches.setOnClickListener {
            currentFilter = "Upcoming"
            updateFilterButtonState()
            observeMatches()
        }

        binding.floatingActionButton.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun updateFilterButtonState() {
        // Reset all buttons to default state
        binding.buttonAllMatches.alpha = 0.7f
        binding.buttonPlayedMatches.alpha = 0.7f
        binding.buttonUnplayedMatches.alpha = 0.7f

        // Highlight the selected button
        when (currentFilter) {
            "All" -> binding.buttonAllMatches.alpha = 1.0f
            "Played" -> binding.buttonPlayedMatches.alpha = 1.0f
            "Upcoming" -> binding.buttonUnplayedMatches.alpha = 1.0f
        }
    }

    private fun setupSortSpinner() {
        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSort = parent.getItemAtPosition(position).toString()
                observeMatches()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showBottomSheet(match: MatchModel? = null) {
        val bottomSheet = AddEditMatchFragment.newInstance(match?.id)
        bottomSheet.setTargetFragment(this, 0)
        bottomSheet.show(parentFragmentManager, AddEditMatchFragment.TAG)
    }

    private fun observeMatches() {
        lifecycleScope.launch {
            val playedFilter = when (currentFilter) {
                "All" -> null  // Žiadny filter
                "Played" -> true
                "Upcoming" -> false
                else -> null
            }

            val sortDirection = when (currentSort) {
                "Vzostupne" -> Query.Direction.ASCENDING
                "Zostupne" -> Query.Direction.DESCENDING
                else -> null
            }

            val matches = matchRepository.getFilteredAndSortedMatches(playedFilter, sortDirection)

            matches.collect { matchList ->
                adapter.submitList(matchList)
            }
        }
    }

    override fun onSaveBtnClicked(isUpdate: Boolean, match: MatchModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isUpdate)
                matchRepository.updateMatch(match.id!!, match)
            else
                matchRepository.createMatch(match)
        }
        if (isUpdate)
            Toast.makeText(requireContext(), "Zápas bol úspešne upravený!", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(requireContext(), "Zápas bol úspešne pridaný!", Toast.LENGTH_SHORT).show()
    }

    override fun onMatchClick(match: MatchModel) {
    }

    override fun onEditMatchClick(match: MatchModel) {
        showBottomSheet(match)
    }

    override fun onDeleteMatchClick(match: MatchModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            matchRepository.deleteMatch(match.id.toString())
        }
        Toast.makeText(requireContext(), "Match was successfully deleted!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}