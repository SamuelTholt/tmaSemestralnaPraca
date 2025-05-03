package com.example.tmasemestralnapraca.matches

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tmasemestralnapraca.R
import com.example.tmasemestralnapraca.databinding.FragmentMatchDetailsBinding
import com.example.tmasemestralnapraca.player.PlayerModel
import com.example.tmasemestralnapraca.player.PlayerRepository
import kotlinx.coroutines.launch


class MatchDetailsFragment : Fragment() {
    private var matchId: String? = null
    private var _binding: FragmentMatchDetailsBinding? = null
    private val binding get() = _binding!!
    private val matchRepository = MatchRepository()
    private val playerRepository = PlayerRepository()
    private lateinit var startingLineupAdapter: MatchLineupAdapter
    private lateinit var substitutesAdapter: MatchLineupAdapter
    private lateinit var eventsAdapter: MatchEventAdapter
    private lateinit var matchDetail: MatchDetail
    private var allPlayers = listOf<PlayerModel>()
    private var matchEvent: MatchEvent? = null
    private var eventPlayer: EventWithPlayer? = null

    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            matchId = it.getString("match_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        isAdmin = sharedPref.getBoolean("isAdminLoggedIn", false)

        initRecyclerViews()
        setupAddButtons()

        binding.btnAddEvent.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.btnAddToStartingLineup.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.btnAddToSubstitutes.visibility = if (isAdmin) View.VISIBLE else View.GONE

        lifecycleScope.launch {
            allPlayers = playerRepository.getPlayers()

            matchId?.let { id ->
                matchDetail = matchRepository.getMatchDetailById(id)
                displayMatchDetails(matchDetail)
            }
        }

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAddButtons() {
        binding.btnAddToStartingLineup.setOnClickListener {
            showAddPlayerDialog(true)
        }

        binding.btnAddToSubstitutes.setOnClickListener {
            showAddPlayerDialog(false)
        }

        binding.btnAddEvent.setOnClickListener {
            showAddEventDialog()
        }
    }

    private fun initRecyclerViews() {
        startingLineupAdapter = MatchLineupAdapter()
        binding.startingLineupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = startingLineupAdapter
        }

        substitutesAdapter = MatchLineupAdapter()
        binding.substitutesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = substitutesAdapter
        }

        eventsAdapter = MatchEventAdapter()
        binding.eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventsAdapter
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayMatchDetails(matchDetail: MatchDetail) {
        val match = matchDetail.match

        binding.matchOpponentTv.text = match.opponentName
        binding.matchScoreTv.text = "${match.ourScore} : ${match.opponentScore}"
        binding.matchDateTv.text = match.date
        binding.matchLocationTv.text = if (match.playedHome) "Doma" else "Von"

        startingLineupAdapter.submitList(matchDetail.startingLineup)

        substitutesAdapter.submitList(matchDetail.substitutes)

        eventsAdapter.submitList(matchDetail.events)

        binding.startingLineupHeader.visibility = View.VISIBLE
        binding.startingLineupRecyclerView.visibility = View.VISIBLE
        binding.btnAddToStartingLineup.visibility = View.VISIBLE

        binding.substitutesHeader.visibility = View.VISIBLE
        binding.substitutesRecyclerView.visibility = View.VISIBLE
        binding.btnAddToSubstitutes.visibility = View.VISIBLE

        binding.eventsHeader.visibility = View.VISIBLE
        binding.eventsRecyclerView.visibility = View.VISIBLE
        binding.btnAddEvent.visibility = View.VISIBLE
    }

    private fun showAddPlayerDialog(isStarting: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_player_to_match, null)
        val playerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerPlayer)
        val minutesInEditText = dialogView.findViewById<EditText>(R.id.etMinutesIn)
        val minutesOutEditText = dialogView.findViewById<EditText>(R.id.etMinutesOut)

        // Vytvoríme zoznam hráčov pre spinner
        val playerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            allPlayers.map { "${it.firstName} ${it.lastName} (${it.numberOfShirt})" }
        )
        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        playerSpinner.adapter = playerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle(if (isStarting) "Pridať hráča do základnej zostavy" else "Pridať náhradníka")
            .setView(dialogView)
            .setPositiveButton("Pridať") { _, _ ->
                val selectedPlayerIndex = playerSpinner.selectedItemPosition
                if (selectedPlayerIndex != -1) {
                    val selectedPlayer = allPlayers[selectedPlayerIndex]
                    val minutesIn = minutesInEditText.text.toString().toIntOrNull()
                    val minutesOut = minutesOutEditText.text.toString().toIntOrNull()

                    val playerStats = PlayerWithStats(
                        player = selectedPlayer,
                        goals = 0,
                        assists = 0,
                        yellowCards = 0,
                        redCards = 0,
                        minutesIn = minutesIn,
                        minutesOut = minutesOut
                    )

                    if (isStarting) {
                        val updatedList = matchDetail.startingLineup.toMutableList()
                        updatedList.add(playerStats)
                        matchDetail.startingLineup = updatedList
                        startingLineupAdapter.submitList(updatedList)
                    } else {
                        val updatedList = matchDetail.substitutes.toMutableList()
                        updatedList.add(playerStats)
                        matchDetail.substitutes = updatedList
                        substitutesAdapter.submitList(updatedList)
                    }


                    saveMatchDetails()
                }
            }
            .setNegativeButton("Zrušiť", null)
            .show()
    }

    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_match_event, null)
        val playerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerPlayer)
        val eventTypeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerEventType)
        val minuteEditText = dialogView.findViewById<EditText>(R.id.etEventMinute)
        val assistPlayerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerAssistPlayer)


        val playerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            allPlayers.map { "${it.firstName} ${it.lastName} (${it.numberOfShirt})" }
        )
        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        playerSpinner.adapter = playerAdapter
        assistPlayerSpinner.adapter = playerAdapter


        val eventTypes = EventType.entries.map { it.name }
        val eventTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            eventTypes
        )
        eventTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        eventTypeSpinner.adapter = eventTypeAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Pridať event")
            .setView(dialogView)
            .setPositiveButton("Pridať") { _, _ ->
                val selectedPlayerIndex = playerSpinner.selectedItemPosition
                val selectedEventTypeIndex = eventTypeSpinner.selectedItemPosition
                val minute = minuteEditText.text.toString().toIntOrNull() ?: 0
                val selectedAssistPlayerIndex = assistPlayerSpinner.selectedItemPosition

                if (selectedPlayerIndex != -1 && selectedEventTypeIndex != -1) {
                    val selectedPlayer = allPlayers[selectedPlayerIndex]
                    val eventType = EventType.entries[selectedEventTypeIndex]

                    val newEvent = MatchEvent(
                        id = matchEvent?.id,
                        matchId = matchId ?: "",
                        playerId = selectedPlayer.id.toString(),
                        eventType = eventType,
                        minute = minute,
                        playerAssistId = if (eventType == EventType.GOAL && selectedAssistPlayerIndex != -1)
                            allPlayers[selectedAssistPlayerIndex].id
                        else null
                    )


                    val eventWithPlayer = EventWithPlayer(
                        id = eventPlayer?.id,
                        event = newEvent,
                        player = selectedPlayer,
                        assistPlayer = if (eventType == EventType.GOAL && selectedAssistPlayerIndex != -1)
                            allPlayers[selectedAssistPlayerIndex]
                        else null
                    )

                    val updatedEvents = matchDetail.events.toMutableList()
                    updatedEvents.add(eventWithPlayer)
                    matchDetail.events = updatedEvents
                    eventsAdapter.submitList(updatedEvents)

                    updatePlayerStats(selectedPlayer.id.toString(), eventType)

                    saveMatchDetails()
                }
            }
            .setNegativeButton("Zrušiť", null)
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updatePlayerStats(playerId: String, eventType: EventType) {
        val playerInStarting = matchDetail.startingLineup.find { it.player.id == playerId }
        val playerInSubstitutes = matchDetail.substitutes.find { it.player.id == playerId }

        when (eventType) {
            EventType.GOAL -> {
                playerInStarting?.let {
                    it.goals++
                    startingLineupAdapter.notifyDataSetChanged()
                }
                playerInSubstitutes?.let {
                    it.goals++
                    substitutesAdapter.notifyDataSetChanged()
                }
            }
            EventType.YELLOW_CARD -> {
                playerInStarting?.let {
                    it.yellowCards++
                    startingLineupAdapter.notifyDataSetChanged()
                }
                playerInSubstitutes?.let {
                    it.yellowCards++
                    substitutesAdapter.notifyDataSetChanged()
                }
            }
            EventType.RED_CARD -> {
                playerInStarting?.let {
                    it.redCards++
                    startingLineupAdapter.notifyDataSetChanged()
                }
                playerInSubstitutes?.let {
                    it.redCards++
                    substitutesAdapter.notifyDataSetChanged()
                }
            }
            else -> {}
        }
    }

    private fun saveMatchDetails() {
        lifecycleScope.launch {
            try {
                matchRepository.updateMatchDetail(matchDetail)
                Toast.makeText(requireContext(), "Zmeny boli úspešne uložené", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Chyba pri ukladaní: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        fun newInstance(matchId: String): MatchDetailsFragment {
            val fragment = MatchDetailsFragment()
            val args = Bundle()
            args.putString("match_id", matchId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}