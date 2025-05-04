package com.example.tmasemestralnapraca.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tmasemestralnapraca.R
import com.example.tmasemestralnapraca.databinding.FragmentAddEditPlayerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class AddEditPlayerFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAddEditPlayerBinding
    private var listener: AddEditPlayerListener? = null
    private var player: PlayerModel? = null
    private val playerRepository = PlayerRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val target = targetFragment
        if (target is AddEditPlayerListener) {
            listener = target
        } else {
            Log.e(TAG, "Parent fragment or activity does not implement AddEditPlayerListener")
            throw ClassCastException("Parent fragment or activity must implement AddEditPlayerListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddEditPlayerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playerId = arguments?.getString("playerId")

        if (playerId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                player = playerRepository.getPlayerById(playerId)
                player?.let { setExistingDataOnUi(it) }
            }
        }
        if (player == null) {
            //Goly
            binding.playerGoalsInputLayout.visibility = View.GONE
            binding.playerGoalsEditText.visibility = View.GONE

            //Asistencie
            binding.playerAssistsInputLayout.visibility = View.GONE
            binding.playerAssistsEditText.visibility = View.GONE

            //Zlte
            binding.playerYellowCardsInputLayout.visibility = View.GONE
            binding.playerYellowCardsEditText.visibility = View.GONE

            //Cervene
            binding.playerRedCardsInputLayout.visibility = View.GONE
            binding.playerRedCardsEditText.visibility = View.GONE

            //Minuty
            binding.playerMinutesInputLayout.visibility = View.GONE
            binding.playerMinutesEditText.visibility = View.GONE
        } else {
            player?.let { setExistingDataOnUi(it) }
        }
        attachUiListener()
    }

    @SuppressLint("SetTextI18n")
    private fun setExistingDataOnUi(player: PlayerModel) {
        binding.playerFirstNameEditText.setText(player.firstName)
        binding.playerLastNameEditText.setText(player.lastName)
        binding.playerNumberEditText.setText(player.numberOfShirt.toString())

        val positionSpinner = binding.positionSpinner
        val positions = resources.getStringArray(R.array.player_positions)
        val positionIndex = positions.indexOf(player.position)
        if (positionIndex >= 0) {
            positionSpinner.setSelection(positionIndex)
        }
        binding.playerGoalsEditText.setText(player.goals.toString())
        binding.playerAssistsEditText.setText(player.assists.toString())
        binding.playerYellowCardsEditText.setText(player.yellowCards.toString())
        binding.playerRedCardsEditText.setText(player.redCards.toString())
        binding.saveBtn.text = "Update"
    }

    private fun attachUiListener() {
        binding.saveBtn.setOnClickListener {
            val firstName = binding.playerFirstNameEditText.text.toString()
            val lastName = binding.playerLastNameEditText.text.toString()
            val numberOfShirt = binding.playerNumberEditText.text.toString()
            val positionSpinner = binding.positionSpinner
            val selectedPosition = positionSpinner.selectedItem.toString()

            val goals = if (player == null) 0 else binding.playerGoalsEditText.text.toString().toInt()
            val assists = if (player == null) 0 else binding.playerAssistsEditText.text.toString().toInt()
            val yellowCards = if (player == null) 0 else binding.playerYellowCardsEditText.text.toString().toInt()
            val redCards = if (player == null) 0 else binding.playerRedCardsEditText.text.toString().toInt()

            if (firstName.isNotEmpty() && lastName.isNotEmpty() && numberOfShirt.isNotEmpty() &&
                selectedPosition.isNotEmpty()) {
                val newPlayer = PlayerModel(
                    id = player?.id,
                    firstName = firstName,
                    lastName = lastName,
                    numberOfShirt = numberOfShirt.toInt(),
                    position = selectedPosition,
                    goals = goals,
                    assists = assists,
                    yellowCards = yellowCards,
                    redCards = redCards,
                )
                Log.d("PlayerData", "Saving player: $newPlayer")


                coroutineScope.launch {
                    try {
                        listener?.onSaveBtnClicked(player != null, newPlayer)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving player", e)
                    }
                }
            }
            dismiss()
        }
    }

    interface AddEditPlayerListener {
        fun onSaveBtnClicked(isUpdate: Boolean, player: PlayerModel)
    }

    companion object {
        const val TAG = "AddEditPlayerFragment"

        @JvmStatic
        fun newInstance(playerId: String?): AddEditPlayerFragment {
            return AddEditPlayerFragment().apply {
                arguments = Bundle().apply {
                    putString("playerId", playerId)
                }
            }
        }
    }
}