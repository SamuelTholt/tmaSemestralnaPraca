package com.example.tmasemestralnapraca.player

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController

import com.example.tmasemestralnapraca.databinding.FragmentPlayerInfoBinding
import kotlinx.coroutines.launch

class PlayerInfoFragment : Fragment() {
    private var playerId: String? = null
    private var _binding: FragmentPlayerInfoBinding? = null
    private val binding get() = _binding!!
    private val playerRepository = PlayerRepository()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playerId = it.getString("player_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Načítať údaje hráča z databázy
        playerId?.let { id ->
            lifecycleScope.launch {
                val player = playerRepository.getPlayerById(id)
                player?.let {
                    binding.firstNameTv.text = it.firstName
                    binding.lastNameTv.text = it.lastName
                    binding.numberOfShirtTv.text = it.numberOfShirt.toString()
                    binding.positionTv.text = it.position
                    binding.goalsTv.text = it.goals.toString()
                    binding.assistsTv.text = it.assists.toString()
                    binding.yellowCardsTv.text = it.yellowCards.toString()
                    binding.redCardsTv.text = it.redCards.toString()
                }
            }
        }

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    companion object {
        fun newInstance(playerId: Int): PlayerInfoFragment {
            val fragment = PlayerInfoFragment()
            val args = Bundle()
            args.putInt("player_id", playerId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}