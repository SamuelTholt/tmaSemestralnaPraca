package com.example.tmasemestralnapraca.teams

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudinary.android.MediaManager
import com.example.tmasemestralnapraca.R
import com.example.tmasemestralnapraca.databinding.FragmentTeamBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class TeamFragment : Fragment(), AddEditTeamFragment.AddEditTeamListener,
    TeamAdapter.TeamDetailsClickListener {

    private var _binding: FragmentTeamBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TeamAdapter

    private val teamRepository = TeamRepository()

    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        isAdmin = sharedPref.getBoolean("isAdminLoggedIn", false)

        initRecyclerView()
        attachListeners()
        observeTeams()

        binding.floatingActionButton.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun initRecyclerView() {
        adapter = TeamAdapter(this, isAdmin)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun attachListeners() {
        binding.floatingActionButton.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun showBottomSheet(team: TeamModel? = null) {
        val bottomSheet = AddEditTeamFragment.newInstance(team?.id)
        bottomSheet.setTargetFragment(this, 0)
        bottomSheet.show(parentFragmentManager, AddEditTeamFragment.TAG)
    }

    private fun observeTeams() {
        lifecycleScope.launch {
            teamRepository.getTeamsSortedByRanking().collect { teams ->
                adapter.submitList(teams)
            }
        }
    }

    override fun onSaveBtnClicked(isUpdate: Boolean, team: TeamModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isUpdate)
                teamRepository.updateTeam(team)
            else
                teamRepository.saveTeam(team)

            recalculatePositions()
        }
        if (isUpdate)
            Toast.makeText(requireContext(), "Tím bol úspešne upravený!", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(requireContext(), "Tím bol úspešne pridaný!", Toast.LENGTH_SHORT).show()

    }

    override fun onEditTeamClick(team: TeamModel) {
        showBottomSheet(team)
    }
    override fun onDeleteTeamClick(team: TeamModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (team.publicId.isNotEmpty()) {
                    val cloudinary = MediaManager.get().cloudinary

                    try {
                        val result = cloudinary.uploader().destroy(team.publicId, emptyMap<String, Any>())

                        if (result["result"] == "ok") {
                            // Odstránenie z Firebase
                            team.id?.let { id ->
                                teamRepository.deleteTeamById(id)
                            }

                            withContext(Dispatchers.Main) {
                                showToast("Tím s logom bol úspešne odstránený!")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast("Chyba pri odstraňovaní z Cloudinary")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            showToast("Chyba pri odstraňovaní z Cloudinary: ${e.message}")

                            // Skúsime odstrániť aspoň z Firebase ak zlyhalo odstránenie z Cloudinary
                            try {
                                team.id?.let { id ->
                                    teamRepository.deleteTeamById(id)
                                    showToast("Tím a logo bol odstránené z databázy, ale nie z Cloudinary")
                                }
                            } catch (innerE: Exception) {
                                showToast("Nepodarilo sa odstrániť tím s logom: ${innerE.message}")
                            }
                        }
                    }
                } else {
                    // Ak nemáme publicId, odstránime len z databázy
                    team.id?.let { id ->
                        teamRepository.deleteTeamById(id)
                        withContext(Dispatchers.Main) {
                            showToast("Obrázok bol odstránený z databázy")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Chyba pri odstraňovaní: ${e.message}")
                }
            }
        }

    }

    private fun showToast(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
    private suspend fun recalculatePositions() {
        teamRepository.let { updateTeams ->
            val teams = updateTeams.getTeamsOrderedForRanking()
            val updatedTeams = teams.mapIndexed { index, team ->
                team.copy(position = index + 1)
            }
            updateTeams.updateTeams(updatedTeams)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}