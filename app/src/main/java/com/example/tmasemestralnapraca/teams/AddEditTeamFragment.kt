package com.example.tmasemestralnapraca.teams

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.tmasemestralnapraca.databinding.FragmentAddEditTeamBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@Suppress("DEPRECATION", "SameParameterValue")
class AddEditTeamFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentAddEditTeamBinding
    private var listener: AddEditTeamListener? = null
    private var team: TeamModel? = null
    private var selectedImageUri: Uri? = null
    private var hasSelectedNewImage = false

    private val teamRepository = TeamRepository()

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val target = targetFragment
        if (target is AddEditTeamListener) {
            listener = target
        } else {
            Log.e(TAG, "Target fragment does not implement AddEditTeamListener")
            throw ClassCastException("Target fragment must implement AddEditTeamListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddEditTeamBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCloudinaryIfNeeded()


        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                hasSelectedNewImage = true
                binding.imgLogo.setImageURI(it)
            }
        }

        val teamId = arguments?.getString("teamId")

        if (teamId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                team = teamRepository.getTeamById(teamId)
                team?.let { setExistingDataOnUi(it) }
            }
        }
        team?.let { setExistingDataOnUi(it) }
        attachUiListener()
    }

    private fun initCloudinaryIfNeeded() {
        try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            val config = hashMapOf<String, String>()
            config["cloud_name"] = "doxc5aa4h"
            config["api_key"] = "556148936248496"
            config["api_secret"] = "OPp2LUz-gTCmutOizH7zmox4gz8"
            config["secure"] = "true"
            MediaManager.init(requireContext(), config)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setExistingDataOnUi(team: TeamModel) {
        binding.teamNameEditText.setText(team.teamName)
        binding.teamPlayedMatchesEditText.setText(team.playedMatches.toString())
        binding.teamScoredEditText.setText(team.goalsScored.toString())
        binding.teamGoalsConceredEditText.setText(team.goalsConceded.toString())
        binding.teamPointsEditText.setText(team.points.toString())

        //Zobrazenie pôvodného obrázka len ak užívateľ ešte nevybral nový
        if (!hasSelectedNewImage && !team.teamImageLogoPath.isNullOrEmpty()) {
            try {
                // Načítanie obrázku z URL (Cloudinary)
                Glide.with(this).load(team.teamImageLogoPath).into(binding.imgLogo)


            } catch (e: Exception) {
                Log.e(TAG, "Chyba pri načítaní obrázka: ${e.message}")
            }
        }

        binding.saveBtn.text = "Update"
    }

    private fun attachUiListener() {
        binding.addPhotoBtn.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.saveBtn.setOnClickListener {
            val teamName = binding.teamNameEditText.text.toString()
            val playedMatches = binding.teamPlayedMatchesEditText.text.toString()
            val goalScored = binding.teamScoredEditText.text.toString()
            val goalConceded = binding.teamGoalsConceredEditText.text.toString()
            val points = binding.teamPointsEditText.text.toString()

            if (teamName.isNotEmpty() && playedMatches.isNotEmpty()
                && goalScored.isNotEmpty() && goalConceded.isNotEmpty() && points.isNotEmpty()
            ) {
                val position = 1

                if (hasSelectedNewImage && selectedImageUri != null) {
                    // Ak bol vybratý nový obrázok - spracovanie cez Cloudinary
                    handleImageUpload(teamName, playedMatches, goalScored, goalConceded, points, position)
                } else {
                    // Ak nebol vybratý nový obrázok - použitie existujúceho alebo žiadneho
                    saveTeam(
                        teamName, playedMatches, goalScored, goalConceded, points, position,
                        team?.teamImageLogoPath ?: "", team?.publicId ?: ""
                    )
                }
            } else {
                Toast.makeText(requireContext(), "Vyplňte všetky polia", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun handleImageUpload(
        teamName: String, playedMatches: String, goalScored: String,
        goalConceded: String, points: String, position: Int
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                selectedImageUri?.let { uri ->
                    val tempFile = createTempFileFromUri(uri)

                    tempFile?.let {
                        uploadToCloudinary(
                            tempFile, teamName, playedMatches, goalScored,
                            goalConceded, points, position
                        )
                    } ?: run {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "Chyba pri vytváraní dočasného súboru",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Chyba pri spracovaní obrázka: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun createTempFileFromUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(requireContext().cacheDir, "temp_uploads")
            if (!tempDir.exists()) tempDir.mkdirs()

            val mimeType = requireContext().contentResolver.getType(uri)
            val extension = when (mimeType) {
                "image/png" -> ".png"
                "image/jpeg" -> ".jpg"
                "image/webp" -> ".webp"
                else -> ".jpg"
            }
            val tempFile = File(tempDir, "team_${System.currentTimeMillis()}$extension")

            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun uploadToCloudinary(
        file: File, teamName: String, playedMatches: String, goalScored: String,
        goalConceded: String, points: String, position: Int
    ) {
        // Odstránenie starého obrázka z Cloudinary, ak existuje
        if (team?.publicId?.isNotEmpty() == true) {
            deleteImageFromCloudinary(team?.publicId!!)
        }

        // Upload nového obrázka na Cloudinary
        MediaManager.get().upload(file.absolutePath)
            .unsigned("ml_default")
            .option("folder", "app_teams")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Upload začal
                    Log.d(TAG, "Upload začal")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Pokrok uploadu
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // Upload úspešný
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            val imageUrl = resultData["secure_url"] as String
                            val publicId = resultData["public_id"] as String

                            // Uloženie tímu s novým obrázkom
                            saveTeam(
                                teamName, playedMatches, goalScored, goalConceded,
                                points, position, imageUrl, publicId
                            )

                            // Odstránenie dočasného súboru
                            file.delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                requireContext(),
                                "Chyba pri ukladaní dát: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Chyba pri nahrávaní: ${error.description}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    file.delete()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Upload naplánovaný na neskôr
                }
            })
            .dispatch()
    }

    private fun deleteImageFromCloudinary(publicId: String) {
        try {
            val cloudinary = MediaManager.get().cloudinary
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = cloudinary.uploader().destroy(publicId, emptyMap<String, Any>())
                    Log.d(TAG, "Odstránenie z Cloudinary: ${result["result"]}")
                } catch (e: Exception) {
                    Log.e(TAG, "Chyba pri odstraňovaní z Cloudinary: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chyba pri získavaní Cloudinary inštancie: ${e.message}")
        }
    }

    private fun saveTeam(
        teamName: String, playedMatches: String, goalScored: String,
        goalConceded: String, points: String, position: Int,
        imagePath: String, publicId: String
    ) {
        val newTeam = TeamModel(
            id = team?.id,
            position = position,
            teamName = teamName,
            teamImageLogoPath = imagePath,
            publicId = publicId,
            playedMatches = playedMatches.toInt(),
            goalsScored = goalScored.toInt(),
            goalsConceded = goalConceded.toInt(),
            points = points.toInt()
        )

        Log.d(TAG, "Ukladám tím: $newTeam")
        listener?.onSaveBtnClicked(team != null, newTeam)
        dismiss()
    }

    interface AddEditTeamListener {
        fun onSaveBtnClicked(isUpdate: Boolean, team: TeamModel)
    }



    companion object {
        const val TAG = "AddEditTeamFragment"

        @JvmStatic
        fun newInstance(teamId: String?) = AddEditTeamFragment().apply {
            arguments = Bundle().apply {
                putString("teamId", teamId)
            }
        }
    }
}