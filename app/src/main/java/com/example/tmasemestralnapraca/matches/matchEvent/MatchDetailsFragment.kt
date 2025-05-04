package com.example.tmasemestralnapraca.matches.matchEvent

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
import com.example.tmasemestralnapraca.matches.MatchDetail
import com.example.tmasemestralnapraca.matches.MatchRepository
import com.example.tmasemestralnapraca.player.PlayerModel
import com.example.tmasemestralnapraca.player.PlayerRepository
import kotlinx.coroutines.launch


@Suppress("NAME_SHADOWING")
class MatchDetailsFragment : Fragment() {
    private var matchId: String? = null
    private var _binding: FragmentMatchDetailsBinding? = null
    private val binding get() = _binding!!
    private val matchRepository = MatchRepository()
    private val playerRepository = PlayerRepository()
    private lateinit var eventsAdapter: MatchEventAdapter
    private lateinit var matchDetail: MatchDetail
    private var allPlayers = listOf<PlayerModel>()
    private var matchEvent: MatchEvent? = null
    private var eventPlayer: EventWithPlayer? = null

    private val MATCH_START_MINUTE = 0
    private val MATCH_END_MINUTE = 90

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

        initRecyclerView()
        setupAddEventButton()

        binding.btnAddEvent.visibility = if (isAdmin) View.VISIBLE else View.GONE

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

    private fun setupAddEventButton() {
        binding.btnAddEvent.setOnClickListener {
            showAddEventDialog()
        }
    }

    private fun initRecyclerView() {
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

        Glide.with(binding.root.context)
            .load(match.opponentLogo)
            .centerCrop()
            .into(binding.imgLogo)


        eventsAdapter.submitList(matchDetail.events)
        binding.eventsHeader.visibility = View.VISIBLE
        binding.eventsRecyclerView.visibility = View.VISIBLE
        binding.btnAddEvent.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_match_event, null)
        val playerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerPlayer)
        val eventTypeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerEventType)
        val minuteEditText = dialogView.findViewById<EditText>(R.id.etEventMinute)
        val assistPlayerSpinner = dialogView.findViewById<Spinner>(R.id.spinnerAssistPlayer)

        val availablePlayers = allPlayers.filter { player ->
            val hasRedCard = matchDetail.events.any {
                it.event.playerId == player.id.toString() && it.event.eventType == EventType.RED_CARD
            }

            val yellowCardCount = countPlayerYellowCards(player.id.toString(), matchDetail.events)

            !hasRedCard && yellowCardCount < 2
        }

        val playerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availablePlayers.map { "${it.firstName} ${it.lastName} (${it.numberOfShirt})" }
        )
        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        playerSpinner.adapter = playerAdapter

        val assistPlayerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availablePlayers.map { "${it.firstName} ${it.lastName} (${it.numberOfShirt})" }
        )
        assistPlayerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        assistPlayerSpinner.adapter = assistPlayerAdapter

        playerSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateAvailableEventTypes(position, eventTypeSpinner)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        })

        updateAvailableEventTypes(0, eventTypeSpinner)

        AlertDialog.Builder(requireContext())
            .setTitle("Pridať event")
            .setView(dialogView)
            .setPositiveButton("Pridať") { _, _ ->
                val selectedPlayerIndex = playerSpinner.selectedItemPosition
                val selectedEventTypeIndex = eventTypeSpinner.selectedItemPosition
                val minute = minuteEditText.text.toString().toIntOrNull() ?: 0
                val selectedAssistPlayerIndex = assistPlayerSpinner.selectedItemPosition

                if (selectedPlayerIndex != -1 && selectedEventTypeIndex != -1) {
                    if (minute < MATCH_START_MINUTE || minute > MATCH_END_MINUTE) {
                        Toast.makeText(requireContext(),
                            "Neplatný čas: hodnota musí byť medzi $MATCH_START_MINUTE a $MATCH_END_MINUTE",
                            Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    val availablePlayers = allPlayers.filter { player ->
                        val hasRedCard = matchDetail.events.any {
                            it.event.playerId == player.id.toString() && it.event.eventType == EventType.RED_CARD
                        }
                        val yellowCardCount = countPlayerYellowCards(player.id.toString(), matchDetail.events)
                        !hasRedCard && yellowCardCount < 2
                    }

                    if (selectedPlayerIndex >= availablePlayers.size) {
                        Toast.makeText(requireContext(), "Neplatný výber hráča", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val selectedPlayer = availablePlayers[selectedPlayerIndex]

                    val availableEventTypes = getAvailableEventTypesForPlayer(selectedPlayer.id.toString())

                    if (availableEventTypes.isEmpty()) {
                        Toast.makeText(requireContext(), "Žiadne dostupné typy eventov pre tohto hráča", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val eventType = availableEventTypes[selectedEventTypeIndex]

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

                    lifecycleScope.launch {
                        try {
                            matchRepository.addMatchEvent(newEvent)

                            val eventWithPlayer = EventWithPlayer(
                                id = newEvent.id,
                                event = newEvent,
                                player = selectedPlayer,
                                assistPlayer = if (eventType == EventType.GOAL && selectedAssistPlayerIndex != -1)
                                    allPlayers[selectedAssistPlayerIndex]
                                else null
                            )

                            val updatedEvents = matchDetail.events.toMutableList()
                            updatedEvents.add(eventWithPlayer)

                            if (eventType == EventType.YELLOW_CARD) {
                                val yellowCardCount = countPlayerYellowCards(selectedPlayer.id.toString(), updatedEvents)
                                if (yellowCardCount >= 2) {
                                    val redCardEvent = MatchEvent(
                                        id = null,
                                        matchId = matchId ?: "",
                                        playerId = selectedPlayer.id.toString(),
                                        eventType = EventType.RED_CARD,
                                        minute = minute,
                                        playerAssistId = null
                                    )

                                    matchRepository.addMatchEvent(redCardEvent)

                                    val redCardEventWithPlayer = EventWithPlayer(
                                        id = redCardEvent.id,
                                        event = redCardEvent,
                                        player = selectedPlayer,
                                        assistPlayer = null
                                    )

                                    updatedEvents.add(redCardEventWithPlayer)
                                    Toast.makeText(requireContext(),
                                        "Hráč dostal druhú žltú kartu, automaticky pridaná červená karta",
                                        Toast.LENGTH_LONG).show()
                                }
                            }

                            val sortedEvents = updatedEvents.sortedBy { it.event.minute }
                            matchDetail.events = sortedEvents
                            eventsAdapter.submitList(sortedEvents)

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

    private fun updateAvailableEventTypes(playerPosition: Int, eventTypeSpinner: Spinner) {
        val availablePlayers = allPlayers.filter { player ->
            val hasRedCard = matchDetail.events.any {
                it.event.playerId == player.id.toString() && it.event.eventType == EventType.RED_CARD
            }
            val yellowCardCount = countPlayerYellowCards(player.id.toString(), matchDetail.events)
            !hasRedCard && yellowCardCount < 2
        }

        if (playerPosition < 0 || playerPosition >= availablePlayers.size) return

        val selectedPlayer = availablePlayers[playerPosition]
        val availableEventTypes = getAvailableEventTypesForPlayer(selectedPlayer.id.toString())

        val eventTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableEventTypes.map { it.name }
        )
        eventTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        eventTypeSpinner.adapter = eventTypeAdapter
    }

    private fun getAvailableEventTypesForPlayer(playerId: String): List<EventType> {
        val goalEvents = matchDetail.events.count { it.event.eventType == EventType.GOAL }
        val yellowCardCount = countPlayerYellowCards(playerId, matchDetail.events)

        return EventType.entries.filter { eventType ->
            when (eventType) {
                EventType.GOAL -> goalEvents < matchDetail.match.ourScore

                EventType.YELLOW_CARD -> yellowCardCount < 2

                else -> true
            }
        }
    }

    private fun countPlayerYellowCards(playerId: String, events: List<EventWithPlayer>): Int {
        return events.count {
            it.event.playerId == playerId && it.event.eventType == EventType.YELLOW_CARD
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