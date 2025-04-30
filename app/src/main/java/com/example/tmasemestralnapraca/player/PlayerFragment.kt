package com.example.tmasemestralnapraca.player

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tmasemestralnapraca.databinding.FragmentPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class PlayerFragment : Fragment(), AddEditPlayerFragment.AddEditPlayerListener,
    PlayerAdapter.PlayerClickListener {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlayerAdapter

    private var currentSort: String = "Číslo"
    private var currentQuery: String = ""
    private var searchJob: Job? = null

    private var isAdmin: Boolean = false

    private val playerRepository = PlayerRepository()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        isAdmin = sharedPref.getBoolean("isAdminLoggedIn", false)

        initRecyclerView()
        attachListeners()
        observePlayers()

        //binding.floatingActionButton.visibility = if (isAdmin) View.VISIBLE else View.GONE

        setupSearchView()

        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSort = parent.getItemAtPosition(position).toString()
                observePlayers()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    currentQuery = newText.orEmpty()
                    observePlayers()
                }
                return true
            }
        })
    }

    private fun initRecyclerView() {
        adapter = PlayerAdapter(this, isAdmin)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun attachListeners() {
        binding.floatingActionButton.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun showBottomSheet(player: PlayerModel? = null) {
        val bottomSheet = AddEditPlayerFragment.newInstance(player?.id)
        bottomSheet.setTargetFragment(this, 0)
        bottomSheet.show(parentFragmentManager, AddEditPlayerFragment.TAG)
    }

    private fun observePlayers() {
        lifecycleScope.launch {
            when {
                currentQuery.isNotEmpty() -> {
                    playerRepository.searchPlayers(currentQuery).collect { players ->
                        adapter.submitList(players)
                    }
                }
                else -> {
                    when (currentSort) {
                        "Číslo" -> playerRepository.getPlayersSortedByNumber()
                        "Meno" -> playerRepository.getPlayersSortedByFirstName()
                        "Priezvisko" -> playerRepository.getPlayersSortedByLastName()
                        "Pozícia" -> playerRepository.getPlayersSortedByPosition()
                        else -> playerRepository.getAllPlayers()
                    }.collect { players ->
                        adapter.submitList(players)
                    }
                }
            }
        }
    }
    override fun onSaveBtnClicked(isUpdate: Boolean, player: PlayerModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isUpdate)
                playerRepository.updatePlayer(player)
            else
                playerRepository.savePlayer(player)
        }
        if (isUpdate)
            Toast.makeText(requireContext(), "Hráč bol úspešne upravený!", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(requireContext(), "Hráč bol úspešne pridaný!", Toast.LENGTH_SHORT).show()
    }

    override fun onEditPlayerClick(player: PlayerModel) {
        showBottomSheet(player)
    }

    override fun onDeletePlayerClick(player: PlayerModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            playerRepository.deletePlayerById(player.id.toString())
        }
        Toast.makeText(requireContext(), "Hráč bol úspešne odstránený!", Toast.LENGTH_SHORT).show()
    }

    override fun onInfoPlayerClick(player: PlayerModel) {
        val action = PlayerFragmentDirections.actionPlayerFragmentToPlayerInfoFragment(player.id.toString())
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}