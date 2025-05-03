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
import com.bumptech.glide.Glide
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

    private var isAdmin: Boolean = false

    private val MATCH_START_MINUTE = 0
    private val MATCH_END_MINUTE = 90
    private val MAX_STARTING_PLAYERS = 11

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
            if (matchDetail.startingLineup.size >= MAX_STARTING_PLAYERS) {
                Toast.makeText(requireContext(), "Maximálny počet hráčov v základnej zostave je $MAX_STARTING_PLAYERS", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddPlayerDialog(true)
        }

        binding.btnAddToSubstitutes.setOnClickListener {
            if (matchDetail.startingLineup.size < MAX_STARTING_PLAYERS) {
                Toast.makeText(requireContext(), "Najprv musíte pridať kompletných $MAX_STARTING_PLAYERS hráčov do základnej zostavy", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
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
        if (match.opponentLogo.isNotEmpty()) {
            Glide.with(requireContext())
                .load(match.opponentLogo)
                .into(binding.imgLogo)
        } else {
            binding.imgLogo.setImageResource(com.example.tmasemestralnapraca.R.drawable.no_logo)
        }

        binding.matchScoreTv.text = "${match.ourScore} : ${match.opponentScore}"
        binding.matchDateTv.text = match.date
        binding.matchLocationTv.text = if (match.playedHome) "Doma" else "Von"

        startingLineupAdapter.submitList(matchDetail.startingLineup)

        substitutesAdapter.submitList(matchDetail.substitutes)

        eventsAdapter.submitList(matchDetail.events)

        binding.startingLineupHeader.visibility = View.VISIBLE
        binding.startingLineupRecyclerView.visibility = View.VISIBLE
        binding.btnAddToStartingLineup.visibility = if (isAdmin && matchDetail.startingLineup.size < MAX_STARTING_PLAYERS) View.VISIBLE else View.GONE

        binding.substitutesHeader.visibility = View.VISIBLE
        binding.substitutesRecyclerView.visibility = View.VISIBLE
        binding.btnAddToSubstitutes.visibility = if (isAdmin && matchDetail.startingLineup.size >= MAX_STARTING_PLAYERS) View.VISIBLE else View.GONE

        binding.eventsHeader.visibility = View.VISIBLE
        binding.eventsRecyclerView.visibility = View.VISIBLE
        binding.btnAddEvent.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    private fun showAddPlayerDialog(isStarting: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_player_to_match, null)
        val playerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerPlayer)
        val minutesInEditText = dialogView.findViewById<EditText>(R.id.etMinutesIn)
        val minutesOutEditText = dialogView.findViewById<EditText>(R.id.etMinutesOut)

        // Adding a spinner for selecting which player to substitute
        val substituteForSpinner = if (!isStarting) dialogView.findViewById<Spinner>(R.id.spinnerSubstituteFor) else null
        val substituteForLayout = if (!isStarting) dialogView.findViewById<View>(R.id.substituteForLayout) else null

        // Make substitute-specific UI visible only when adding substitutes
        substituteForLayout?.visibility = if (!isStarting) View.VISIBLE else View.GONE

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
        } else {
            // For substitutes, set up the "substitute for" spinner
            val startingPlayersAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                matchDetail.startingLineup.map { "${it.player.firstName} ${it.player.lastName} (${it.player.numberOfShirt})" }
            )
            startingPlayersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            substituteForSpinner?.adapter = startingPlayersAdapter

            // Default substitution time (for example, 60th minute)
            minutesInEditText.setText("60")
            minutesOutEditText.setText(MATCH_END_MINUTE.toString())
        }

        val builder = AlertDialog.Builder(requireContext())
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
                        minutesIn = minutesIn ?: 60 // Default substitution time
                        minutesOut = minutesOut ?: MATCH_END_MINUTE

                        // Handle the player substitution
                        val substituteForIndex = substituteForSpinner?.selectedItemPosition
                        if (substituteForIndex != null && substituteForIndex != -1) {
                            val substituteForPlayer = matchDetail.startingLineup[substituteForIndex]

                            // Update the starter's minutesOut to match the substitute's minutesIn
                            lifecycleScope.launch {
                                try {
                                    // Create the substitution events
                                    val substitutionIn = MatchEvent(
                                        matchId = matchId ?: "",
                                        playerId = selectedPlayer.id.toString(),
                                        eventType = EventType.SUBSTITUTION_IN,
                                        minute = minutesIn ?: 60,
                                        playerAssistId = substituteForPlayer.player.id.toString() // Store the player being substituted
                                    )

                                    val substitutionOut = MatchEvent(
                                        matchId = matchId ?: "",
                                        playerId = substituteForPlayer.player.id.toString(),
                                        eventType = EventType.SUBSTITUTION_OUT,
                                        minute = minutesIn ?: 60,
                                        playerAssistId = selectedPlayer.id.toString() // Store the substitute player
                                    )

                                    // Save events to Firestore
                                    matchRepository.addMatchEvent(substitutionIn)
                                    matchRepository.addMatchEvent(substitutionOut)

                                    // Update starter's minutes out
                                    val updatedStarterLineup = LineupPlayer(
                                        matchId = matchId ?: "",
                                        playerId = substituteForPlayer.player.id.toString(),
                                        isStarting = true,
                                        minutesIn = substituteForPlayer.minutesIn,
                                        minutesOut = minutesIn ?: 60
                                    )

                                    matchRepository.updatePlayerInLineup(updatedStarterLineup)

                                    // Update UI for the starter
                                    val updatedStarter = substituteForPlayer.copy(minutesOut = minutesIn ?: 60)
                                    val updatedStartingLineup = matchDetail.startingLineup.toMutableList()
                                    val starterIndex = updatedStartingLineup.indexOfFirst { it.player.id == substituteForPlayer.player.id }
                                    if (starterIndex != -1) {
                                        updatedStartingLineup[starterIndex] = updatedStarter
                                        matchDetail.startingLineup = updatedStartingLineup
                                        startingLineupAdapter.submitList(updatedStartingLineup)
                                    }

                                    // Add the events to the UI
                                    addSubstitutionEventsToUI(substitutionIn, substitutionOut, selectedPlayer, substituteForPlayer.player)

                                } catch (e: Exception) {
                                    Toast.makeText(requireContext(), "Chyba pri aktualizácii striedania: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
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

                                // Update the button visibility if we've reached the max
                                binding.btnAddToStartingLineup.visibility = if (updatedList.size >= MAX_STARTING_PLAYERS) View.GONE else View.VISIBLE
                                binding.btnAddToSubstitutes.visibility = if (updatedList.size >= MAX_STARTING_PLAYERS) View.VISIBLE else View.GONE
                            } else {
                                val updatedList = matchDetail.substitutes.toMutableList()
                                updatedList.add(playerStats)
                                matchDetail.substitutes = updatedList
                                substitutesAdapter.submitList(updatedList)
                            }

                            Toast.makeText(requireContext(), "Hráč pridaný", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Chyba pri pridávaní hráča: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Zrušiť", null)

        builder.show()
    }

    private fun addSubstitutionEventsToUI(substitutionIn: MatchEvent, substitutionOut: MatchEvent, substitutePlayer: PlayerModel, starterPlayer: PlayerModel) {
        val inEvent = EventWithPlayer(
            id = substitutionIn.id,
            event = substitutionIn,
            player = substitutePlayer,
            assistPlayer = starterPlayer
        )

        val outEvent = EventWithPlayer(
            id = substitutionOut.id,
            event = substitutionOut,
            player = starterPlayer,
            assistPlayer = substitutePlayer
        )

        val updatedEvents = matchDetail.events.toMutableList()
        updatedEvents.add(inEvent)
        updatedEvents.add(outEvent)

        // Sort events by minute
        val sortedEvents = updatedEvents.sortedBy { it.event.minute }
        matchDetail.events = sortedEvents
        eventsAdapter.submitList(sortedEvents)
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

    @SuppressLint("MissingInflatedId")
    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_match_event, null)
        val playerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerPlayer)
        val eventTypeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerEventType)
        val minuteEditText = dialogView.findViewById<EditText>(R.id.etEventMinute)
        val assistPlayerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerAssistPlayer)
        val assistPlayerLayout = dialogView.findViewById<View>(R.id.assistPlayerLayout)

        // Initially hide assist player layout, will show only for goals
        assistPlayerLayout.visibility = View.GONE

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

        // Add listener to the event type spinner to show/hide assist field
        eventTypeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedEventType = availableEventTypes[position]
                // Only show assist player selection for goal events
                assistPlayerLayout.visibility = if (selectedEventType == EventType.GOAL) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                assistPlayerLayout.visibility = View.GONE
            }
        }

        // Add listener to player spinner to update assist player spinner
        playerSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateAssistPlayerSpinner(playerSpinner.selectedItemPosition, assistPlayerSpinner, playersInLineup)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        }

        // Initial setup of assist player spinner
        updateAssistPlayerSpinner(0, assistPlayerSpinner, playersInLineup)

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

                    // Get the adjusted index for assist player (since we removed the scorer from the list)
                    val assistPlayerActualIndex = if (eventType == EventType.GOAL &&
                        selectedAssistPlayerIndex != -1 &&
                        assistPlayerLayout.visibility == View.VISIBLE) {
                        // Get the actual player from the filtered assist player list
                        val assistPlayerAdapter = assistPlayerSpinner.adapter as ArrayAdapter<*>
                        val assistPlayerText = assistPlayerAdapter.getItem(selectedAssistPlayerIndex).toString()
                        val filteredPlayersInLineup = playersInLineup.filterIndexed { index, _ -> index != selectedPlayerIndex }
                        val assistPlayer = filteredPlayersInLineup.firstOrNull { player ->
                            "${player.firstName} ${player.lastName} (${player.numberOfShirt})" == assistPlayerText
                        }
                        playersInLineup.indexOf(assistPlayer)
                    } else {
                        -1
                    }

                    val newEvent = MatchEvent(
                        matchId = matchId ?: "",
                        playerId = selectedPlayer.id.toString(),
                        eventType = eventType,
                        minute = minute,
                        playerAssistId = if (eventType == EventType.GOAL && assistPlayerActualIndex != -1)
                            playersInLineup[assistPlayerActualIndex].id.toString()
                        else null
                    )

                    lifecycleScope.launch {
                        try {
                            matchRepository.addMatchEvent(newEvent)

                            val eventWithPlayer = EventWithPlayer(
                                id = newEvent.id,
                                event = newEvent,
                                player = selectedPlayer,
                                assistPlayer = if (eventType == EventType.GOAL && assistPlayerActualIndex != -1)
                                    playersInLineup[assistPlayerActualIndex]
                                else null
                            )

                            val updatedEvents = matchDetail.events.toMutableList()
                            updatedEvents.add(eventWithPlayer)
                            // Sort events by minute
                            val sortedEvents = updatedEvents.sortedBy { it.event.minute }
                            matchDetail.events = sortedEvents
                            eventsAdapter.submitList(sortedEvents)

                            updatePlayerStats(selectedPlayer.id.toString(), eventType)

                            if (eventType == EventType.GOAL && assistPlayerActualIndex != -1) {
                                val assistPlayerId = playersInLineup[assistPlayerActualIndex].id.toString()
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

    private fun updateAssistPlayerSpinner(selectedPlayerIndex: Int, assistPlayerSpinner: Spinner, allPlayers: List<PlayerModel>) {
        // Create a filtered list excluding the selected player
        val filteredPlayers = allPlayers.filterIndexed { index, _ -> index != selectedPlayerIndex }

        // Create adapter for assist player spinner with filtered list
        val assistPlayerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filteredPlayers.map { "${it.firstName} ${it.lastName} (${it.numberOfShirt})" }
        )
        assistPlayerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        assistPlayerSpinner.adapter = assistPlayerAdapter
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