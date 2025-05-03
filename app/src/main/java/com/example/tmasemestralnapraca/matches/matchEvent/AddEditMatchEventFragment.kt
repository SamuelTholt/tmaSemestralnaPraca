package com.example.tmasemestralnapraca.matches.matchEvent

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tmasemestralnapraca.databinding.FragmentAddEditMatchEventBinding
import com.example.tmasemestralnapraca.player.PlayerModel
import com.example.tmasemestralnapraca.player.PlayerRepository
import com.example.tmasemestralnapraca.post.PostModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class AddEditMatchEventFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddEditMatchEventBinding? = null
    private val binding get() = _binding!!

    private var listener: AddEditEventListener? = null

    private var matchId: String = ""
    private var eventId: String? = null

    private var selectedEventType: EventType = EventType.GOAL
    private var selectedPlayerId: String? = null
    private var selectedAssistPlayerId: String? = null
    private var players: List<PlayerModel> = listOf()

    private val playerRepository = PlayerRepository()
    private val matchEventRepository = MatchEventRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditMatchEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val target = targetFragment
        if (target is AddEditEventListener) {
            listener = target
        } else {
            throw ClassCastException("Target fragment must implement AddEditEventListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            matchId = it.getString("matchId", "")
            eventId = it.getString("eventId")
        }

        setupEventTypeSpinner()
        loadPlayers()
        setupAssistVisibility()
        setupSaveButton()

        eventId?.let { loadExistingEvent(it) }
    }

    private fun setupEventTypeSpinner() {
        val eventTypes = EventType.entries.map { it.name.replace("_", " ") }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, eventTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEventType.adapter = adapter

        binding.spinnerEventType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedEventType = EventType.entries.toTypedArray()[position]
                setupAssistVisibility()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupAssistVisibility() {
        binding.assistsInputLayout.visibility = if (selectedEventType == EventType.GOAL) View.VISIBLE else View.GONE
    }

    private fun loadPlayers() {
        viewLifecycleOwner.lifecycleScope.launch {
            playerRepository.getAllPlayers().collect { playerList ->
                players = playerList
                setupPlayerSpinners(playerList)
            }
        }
    }

    private fun setupPlayerSpinners(playerList: List<PlayerModel>) {
        val playerNames = playerList.map { "${it.firstName} ${it.lastName}" }

        val playerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, playerNames)
        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlayer.adapter = playerAdapter

        val assistAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("None") + playerNames)
        assistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAssistPlayer.adapter = assistAdapter

        binding.spinnerPlayer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) {
                selectedPlayerId = players[position].id
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.spinnerAssistPlayer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) {
                selectedAssistPlayerId = if (position == 0) null else players[position - 1].id
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadExistingEvent(eventId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val event = matchEventRepository.getEventById(eventId)
            event?.let {
                val typeIndex = EventType.entries.indexOf(it.eventType)
                binding.spinnerEventType.setSelection(typeIndex)
                binding.etEventMinute.setText(it.minute.toString())

                players.indexOfFirst { player -> player.id == it.playerId }
                    .takeIf { it != -1 }?.let { pos -> binding.spinnerPlayer.setSelection(pos) }

                it.playerAssistId?.let { assistId ->
                    players.indexOfFirst { p -> p.id == assistId }
                        .takeIf { it != -1 }?.let { pos -> binding.spinnerAssistPlayer.setSelection(pos + 1) }
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveBtn.setOnClickListener {
            val minute = binding.etEventMinute.text.toString().toIntOrNull()
            if (minute == null || selectedPlayerId == null) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val matchEvent = MatchEvent(
                id = eventId,
                matchId = matchId,
                playerId = selectedPlayerId!!,
                eventType = selectedEventType,
                minute = minute,
                playerAssistId = if (selectedEventType == EventType.GOAL) selectedAssistPlayerId else null
            )

            listener?.onSaveBtnClicked(eventId != null, matchEvent)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(matchId: String, eventId: String? = null) =
            AddEditMatchEventFragment().apply {
                arguments = Bundle().apply {
                    putString("matchId", matchId)
                    eventId?.let { putString("eventId", it) }
                }
            }

        const val TAG = "AddEditMatchEventFragment"
    }

    interface AddEditEventListener {
        fun onSaveBtnClicked(isUpdate: Boolean, matchEvent: MatchEvent)
    }
}