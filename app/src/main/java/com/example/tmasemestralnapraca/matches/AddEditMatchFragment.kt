package com.example.tmasemestralnapraca.matches

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.bumptech.glide.Glide
import com.example.tmasemestralnapraca.databinding.FragmentAddEditMatchBinding
import com.example.tmasemestralnapraca.teams.TeamModel
import com.example.tmasemestralnapraca.teams.TeamRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Suppress("DEPRECATION")
class AddEditMatchFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAddEditMatchBinding
    private var listener: AddEditMatchListener? = null
    private var matchModel: MatchModel? = null
    private val matchRepository = MatchRepository()
    private val teamRepository = TeamRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var teams: List<TeamModel> = emptyList()
    private var selectedTeamId: String = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val target = targetFragment
        if (target is AddEditMatchListener) {
            listener = target
        } else {
            Log.e(TAG, "Parent fragment or activity does not implement AddEditMatchListener")
            throw ClassCastException("Parent fragment or activity must implement AddEditMatchListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    interface AddEditMatchListener {
        fun onSaveBtnClicked(isUpdate: Boolean, match: MatchModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddEditMatchBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val matchId = arguments?.getString("matchId")

        loadTeams()

        if (matchId != null) {
            coroutineScope.launch {
                matchModel = matchRepository.getMatchWithTeamDetails(matchId)
                matchModel?.let {

                    if (teams.isEmpty()) {
                        withContext(Dispatchers.IO) {
                            teams = teamRepository.getTeams()
                        }
                        setupTeamsSpinner()
                    }
                    setExistingDataOnUi(it)
                }
            }
        }

        attachUiListener()

    }

    @SuppressLint("SetTextI18n")
    private fun setExistingDataOnUi(match: MatchModel) {
        binding.OurScoreEditText.setText(match.ourScore.toString())
        binding.opponentScoreEditText.setText(match.opponentScore.toString())

        val date = if (match.date.contains(":")) {
            match.date
        } else {
            try {
                val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val parsedDate = inputFormat.parse(match.date)
                parsedDate?.let { outputFormat.format(it) } ?: match.date
            } catch (e: Exception) {
                Log.e(TAG, "Chyba pri formatovaní dátumu", e)
                match.date
            }
        }

        binding.matchDateEditText.setText(date)
        binding.isPlayedCheckBox.isChecked = match.played
        binding.isHomeCheckBox.isChecked = match.playedHome
        binding.saveBtn.text = "Update"

        if (match.opponentTeamId.isNotEmpty() && teams.isNotEmpty()) {
            val teamIndex = teams.indexOfFirst { it.id == match.opponentTeamId }
            if (teamIndex != -1) {
                binding.opponentTeamSpinner.setSelection(teamIndex + 1)

            } else {
                binding.opponentTeamNameTv.text = match.opponentName

                if (match.opponentLogo.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(match.opponentLogo)
                        .into(binding.imgIconTeam)
                } else {
                    binding.imgIconTeam.setImageResource(com.example.tmasemestralnapraca.R.drawable.no_logo)
                }
            }
        }

        if (match.opponentTeamId.isNotEmpty()) {
            val team = teams.find { it.id == match.opponentTeamId }
            team?.let {
                binding.opponentTeamNameTv.text = it.teamName

                if (!it.teamImageLogoPath.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(it.teamImageLogoPath)
                        .into(binding.imgIconTeam)
                } else {
                    binding.imgIconTeam.setImageResource(com.example.tmasemestralnapraca.R.drawable.no_logo)
                }
            }
        }
    }

    private fun attachUiListener() {
        binding.saveBtn.setOnClickListener {
            if (selectedTeamId.isEmpty()) {
                return@setOnClickListener
            }

            val ourScore = binding.OurScoreEditText.text.toString().toIntOrNull() ?: 0
            val opponentScore = binding.opponentScoreEditText.text.toString().toIntOrNull() ?: 0
            val date = binding.matchDateEditText.text.toString()
            val isPlayed = binding.isPlayedCheckBox.isChecked
            val isPlayedHome = binding.isHomeCheckBox.isChecked

            val selectedTeam = teams.find { it.id == selectedTeamId }
            if (selectedTeam == null) {
                return@setOnClickListener
            }

            if (date.isNotEmpty()) {
                val newMatch = MatchModel(
                    id = matchModel?.id,
                    opponentTeamId = selectedTeamId,
                    opponentName = selectedTeam.teamName,
                    opponentLogo = selectedTeam.teamImageLogoPath ?: "",
                    ourScore = ourScore,
                    opponentScore = opponentScore,
                    date = date,
                    played = isPlayed,
                    playedHome = isPlayedHome
                )
                Log.d("MatchData", "Ukladanie zápasu: $newMatch")

                coroutineScope.launch {
                    try {
                        listener?.onSaveBtnClicked(matchModel != null, newMatch)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error pri ukladaní zápasu", e)
                    }
                }
            }
            dismiss()
        }

        binding.matchDateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()

            if (binding.matchDateEditText.text.toString().isNotEmpty()) {
                try {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val date = dateFormat.parse(binding.matchDateEditText.text.toString())
                    if (date != null) {
                        calendar.time = date
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date", e)
                }
            }

            //DatePickerDialog
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    //TimePickerDialog
                    val timePickerDialog = TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)

                            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                            binding.matchDateEditText.setText(dateFormat.format(calendar.time))
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    )
                    timePickerDialog.show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }


    private fun loadTeams() {
        coroutineScope.launch {
            try {
                teams = withContext(Dispatchers.IO) {
                    teamRepository.getTeams()
                }
                setupTeamsSpinner()

                matchModel?.let { existingMatch ->
                    if (existingMatch.opponentTeamId.isNotEmpty()) {
                        val teamPosition = teams.indexOfFirst { it.id == existingMatch.opponentTeamId }
                        if (teamPosition != -1) {
                            binding.opponentTeamSpinner.post {
                                binding.opponentTeamSpinner.setSelection(teamPosition + 1)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading teams", e)
            }
        }
    }

    private fun setupTeamsSpinner() {
        val teamNames = teams.map { it.teamName }.toMutableList()
        teamNames.add(0, "Vyber súpera/openenta")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            teamNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.opponentTeamSpinner.adapter = adapter

        binding.opponentTeamSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedTeam = teams[position - 1]
                    selectedTeamId = selectedTeam.id ?: ""

                    binding.opponentTeamNameTv.text = selectedTeam.teamName

                    if (!selectedTeam.teamImageLogoPath.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(selectedTeam.teamImageLogoPath)
                            .into(binding.imgIconTeam)
                    } else {
                        binding.imgIconTeam.setImageResource(com.example.tmasemestralnapraca.R.drawable.no_logo)
                    }
                } else {
                    selectedTeamId = ""
                    binding.opponentTeamNameTv.text = ""
                    binding.imgIconTeam.setImageResource(com.example.tmasemestralnapraca.R.drawable.no_logo)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedTeamId = ""
                binding.opponentTeamNameTv.text = ""
                binding.imgIconTeam.setImageResource(com.example.tmasemestralnapraca.R.drawable.no_logo)
            }
        }

        matchModel?.let { existingMatch ->
            if (existingMatch.opponentTeamId.isNotEmpty()) {
                val teamPosition = teams.indexOfFirst { it.id == existingMatch.opponentTeamId }
                if (teamPosition != -1) {
                    binding.opponentTeamSpinner.setSelection(teamPosition + 1)
                }
            }
        }
    }

    companion object {
        const val TAG = "AddEditMatchFragment"

        @JvmStatic
        fun newInstance(matchId: String?): AddEditMatchFragment {
            return AddEditMatchFragment().apply {
                arguments = Bundle().apply {
                    putString("matchId", matchId)
                }
            }
        }
    }
}