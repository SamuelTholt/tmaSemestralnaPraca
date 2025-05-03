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
import com.example.tmasemestralnapraca.matches.matchEvent.EventType
import com.example.tmasemestralnapraca.matches.matchEvent.EventWithPlayer
import com.example.tmasemestralnapraca.matches.matchEvent.MatchEvent
import com.example.tmasemestralnapraca.matches.matchLineup.LineupPlayer
import com.example.tmasemestralnapraca.matches.matchLineup.MatchLineupAdapter
import com.example.tmasemestralnapraca.matches.matchLineup.PlayerWithStats
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

    private val MATCH_START_MINUTE = 0
    private val MATCH_END_MINUTE = 90

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
        binding.btnAddToStartingLineup.visibility = if (isAdmin) View.VISIBLE else View.GONE

        binding.substitutesHeader.visibility = View.VISIBLE
        binding.substitutesRecyclerView.visibility = View.VISIBLE
        binding.btnAddToSubstitutes.visibility = if (isAdmin) View.VISIBLE else View.GONE

        binding.eventsHeader.visibility = View.VISIBLE
        binding.eventsRecyclerView.visibility = View.VISIBLE
        binding.btnAddEvent.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun showAddPlayerDialog(isStarting: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_player_to_match, null)
        val playerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerPlayer)
        val minutesInEditText = dialogView.findViewById<EditText>(R.id.etMinutesIn)
        val minutesOutEditText = dialogView.findViewById<EditText>(R.id.etMinutesOut)

        val playersNotInLineup = allPlayers.filter { player ->
            val isInStartingLineup = matchDetail.startingLineup.any { it.player.id == player.id }
            val isInSubstitutes = matchDetail.substitutes.any { it.player.id == player.id }
            !(isInStartingLineup || isInSubstitutes)
        }


        if (playersNotInLineup.isEmpty()) {
            Toast.makeText(requireContext(), "Všetci hráči sú už v zostave", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup player spinner
        val playerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            playersNotInLineup.map { "${it.firstName} ${it.lastName} (${it.numberOfShirt})" }
        )
        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        playerSpinner.adapter = playerAdapter

        // Set default values based on player type
        if (isStarting) {
            minutesInEditText.setText(MATCH_START_MINUTE.toString())
            minutesOutEditText.setText(MATCH_END_MINUTE.toString())
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isStarting) "Pridať hráča do základnej zostavy" else "Pridať náhradníka")
            .setView(dialogView)
            .setPositiveButton("Pridať") { _, _ ->
                val selectedPlayerIndex = playerSpinner.selectedItemPosition
                if (selectedPlayerIndex != -1) {
                    val selectedPlayer = playersNotInLineup[selectedPlayerIndex]

                    // Handle minutes logic
                    var minutesIn = minutesInEditText.text.toString().toIntOrNull()
                    var minutesOut = minutesOutEditText.text.toString().toIntOrNull()

                    // Handle default values for minutes
                    if (isStarting) {
                        // For starting players, default is 0-90
                        minutesIn = minutesIn ?: MATCH_START_MINUTE
                        minutesOut = minutesOut ?: MATCH_END_MINUTE
                    } else {
                        // For substitutes
                        minutesIn = minutesIn ?: MATCH_START_MINUTE
                        minutesOut = minutesOut ?: MATCH_END_MINUTE
                    }

                    // Validate minutes
                    if (minutesIn < MATCH_START_MINUTE || minutesIn > MATCH_END_MINUTE ||
                        minutesOut < MATCH_START_MINUTE || minutesOut > MATCH_END_MINUTE ||
                        minutesIn > minutesOut) {
                        Toast.makeText(requireContext(),
                            "Neplatné časy: hodnoty musia byť medzi $MATCH_START_MINUTE a $MATCH_END_MINUTE a čas vstupu musí byť pred časom výstupu",
                            Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    // Create a new LineupPlayer object
                    val lineupPlayer = LineupPlayer(
                        matchId = matchId ?: "",
                        playerId = selectedPlayer.id.toString(),
                        isStarting = isStarting,
                        minutesIn = minutesIn,
                        minutesOut = minutesOut
                    )

                    // Save to Firestore first
                    lifecycleScope.launch {
                        try {
                            matchRepository.addPlayerToLineup(lineupPlayer)

                            // Create PlayerWithStats object
                            val playerStats = PlayerWithStats(
                                player = selectedPlayer,
                                goals = 0,
                                assists = 0,
                                yellowCards = 0,
                                redCards = 0,
                                minutesIn = minutesIn,
                                minutesOut = minutesOut
                            )

                            // Update UI
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

                            // Automatically add substitution events if necessary
                            if (!isStarting && minutesIn != MATCH_START_MINUTE) {
                                // Create a SUBSTITUTION_IN event
                                addSubstitutionEvent(selectedPlayer.id.toString(), EventType.SUBSTITUTION_IN, minutesIn)
                            }

                            if (minutesOut != MATCH_END_MINUTE) {
                                // Create a SUBSTITUTION_OUT event
                                addSubstitutionEvent(selectedPlayer.id.toString(), EventType.SUBSTITUTION_OUT, minutesOut)
                            }

                            Toast.makeText(requireContext(), "Hráč pridaný", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Chyba pri pridávaní hráča: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Zrušiť", null)
            .show()
    }

    private fun addSubstitutionEvent(playerId: String, eventType: EventType, minute: Int) {
        // Create a new substitution event
        val newEvent = MatchEvent(
            matchId = matchId ?: "",
            playerId = playerId,
            eventType = eventType,
            minute = minute,
            playerAssistId = null
        )

        // Save the event to Firestore
        lifecycleScope.launch {
            try {
                matchRepository.addMatchEvent(newEvent)

                // Find the player for the event
                val player = allPlayers.find { it.id == playerId }

                if (player != null) {
                    // Create the event with player object
                    val eventWithPlayer = EventWithPlayer(
                        id = newEvent.id,
                        event = newEvent,
                        player = player,
                        assistPlayer = null
                    )

                    // Add to the events list
                    val updatedEvents = matchDetail.events.toMutableList()
                    updatedEvents.add(eventWithPlayer)
                    matchDetail.events = updatedEvents
                    eventsAdapter.submitList(updatedEvents)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Chyba pri pridávaní striedania: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_match_event, null)
        val playerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerPlayer)
        val eventTypeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerEventType)
        val minuteEditText = dialogView.findViewById<EditText>(R.id.etEventMinute)
        val assistPlayerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerAssistPlayer)

        val playersInLineup = mutableListOf<PlayerModel>()
        playersInLineup.addAll(matchDetail.startingLineup.map { it.player })
        playersInLineup.addAll(matchDetail.substitutes.map { it.player })

        if (playersInLineup.isEmpty()) {
            Toast.makeText(requireContext(), "Najskôr pridajte hráčov do zostavy", Toast.LENGTH_SHORT).show()
            return
        }

        val playerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            playersInLineup.map { "${it.firstName} ${it.lastName} (${it.numberOfShirt})" }
        )
        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        playerSpinner.adapter = playerAdapter
        assistPlayerSpinner.adapter = playerAdapter


        val goalEvents = matchDetail.events.count { it.event.eventType == EventType.GOAL }
        val availableEventTypes = EventType.entries.filter { eventType ->
            if (eventType == EventType.GOAL && goalEvents >= matchDetail.match.ourScore) {
                false
            } else if (eventType == EventType.SUBSTITUTION_IN || eventType == EventType.SUBSTITUTION_OUT) {
                false
            } else {
                true
            }
        }

        val eventTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableEventTypes.map { it.name }
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
                    // Validate minute value
                    if (minute < MATCH_START_MINUTE || minute > MATCH_END_MINUTE) {
                        Toast.makeText(requireContext(),
                            "Neplatný čas: hodnota musí byť medzi $MATCH_START_MINUTE a $MATCH_END_MINUTE",
                            Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    val selectedPlayer = playersInLineup[selectedPlayerIndex]
                    val eventType = availableEventTypes[selectedEventTypeIndex]

                    // Make sure player was on the field at given minute
                    val playerInStarting = matchDetail.startingLineup.find { it.player.id == selectedPlayer.id }
                    val playerInSubstitutes = matchDetail.substitutes.find { it.player.id == selectedPlayer.id }

                    val playerMinutesIn = playerInStarting?.minutesIn ?: playerInSubstitutes?.minutesIn ?: MATCH_START_MINUTE
                    val playerMinutesOut = playerInStarting?.minutesOut ?: playerInSubstitutes?.minutesOut ?: MATCH_END_MINUTE

                    if (minute < playerMinutesIn || minute > playerMinutesOut) {
                        Toast.makeText(requireContext(),
                            "Hráč nebol v danom čase na ihrisku (${playerMinutesIn} - ${playerMinutesOut})",
                            Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    // Create a new event without passing an ID
                    val newEvent = MatchEvent(
                        matchId = matchId ?: "",
                        playerId = selectedPlayer.id.toString(),
                        eventType = eventType,
                        minute = minute,
                        playerAssistId = if (eventType == EventType.GOAL && selectedAssistPlayerIndex != -1)
                            playersInLineup[selectedAssistPlayerIndex].id
                        else null
                    )

                    // Save the event to Firestore first, then add to UI
                    lifecycleScope.launch {
                        try {
                            matchRepository.addMatchEvent(newEvent)

                            // Now create the EventWithPlayer with the saved event
                            val eventWithPlayer = EventWithPlayer(
                                id = newEvent.id,
                                event = newEvent,
                                player = selectedPlayer,
                                assistPlayer = if (eventType == EventType.GOAL && selectedAssistPlayerIndex != -1)
                                    playersInLineup[selectedAssistPlayerIndex]
                                else null
                            )

                            val updatedEvents = matchDetail.events.toMutableList()
                            updatedEvents.add(eventWithPlayer)
                            matchDetail.events = updatedEvents
                            eventsAdapter.submitList(updatedEvents)

                            updatePlayerStats(selectedPlayer.id.toString(), eventType)

                            // If it's a goal and there's an assist player, update their stats too
                            if (eventType == EventType.GOAL && selectedAssistPlayerIndex != -1) {
                                val assistPlayerId = playersInLineup[selectedAssistPlayerIndex].id.toString()
                                updatePlayerAssistStats(assistPlayerId)
                            }

                            Toast.makeText(requireContext(), "Event pridaný", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Chyba pri pridávaní eventu: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
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

    @SuppressLint("NotifyDataSetChanged")
    private fun updatePlayerAssistStats(playerId: String) {
        val playerInStarting = matchDetail.startingLineup.find { it.player.id == playerId }
        val playerInSubstitutes = matchDetail.substitutes.find { it.player.id == playerId }

        playerInStarting?.let {
            it.assists++
            startingLineupAdapter.notifyDataSetChanged()
        }

        playerInSubstitutes?.let {
            it.assists++
            substitutesAdapter.notifyDataSetChanged()
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