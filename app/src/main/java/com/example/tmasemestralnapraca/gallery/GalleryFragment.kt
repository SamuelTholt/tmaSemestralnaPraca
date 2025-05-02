package com.example.tmasemestralnapraca.gallery

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.tmasemestralnapraca.databinding.FragmentGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID


class GalleryFragment : Fragment(), GalleryAdapter.PhotoClickListener {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: GalleryAdapter
    private val imageRepository = ImageRepository()

    private var currentSort: String = "Zostupne"

    private var isAdmin: Boolean = false

    private lateinit var multiplePhotoPickLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        isAdmin = sharedPref.getBoolean("isAdminLoggedIn", false)

        initCloudinaryIfNeeded()

        initRecyclerView()
        attachListeners()
        setupMultipleImagePicker()
        observeImages()

        binding.floatingActionButton.visibility = if (isAdmin) View.VISIBLE else View.GONE

        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSort = parent.getItemAtPosition(position).toString()
                observeImages()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
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

    private fun initRecyclerView() {
        adapter = GalleryAdapter(this, isAdmin)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun attachListeners() {
        binding.floatingActionButton.setOnClickListener {
            showMultiplePickerImages()
        }
    }

    private fun setupMultipleImagePicker() {
        multiplePhotoPickLauncher = registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia()
        ) { uris ->
            if (uris.isNotEmpty()) {

                lifecycleScope.launch(Dispatchers.IO) {
                    uris.forEach { uri ->
                        try {

                            val tempFile = createTempFileFromUri(uri)

                            if (tempFile != null) {

                                uploadToCloudinary(tempFile)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showToast("Chyba pri nahrávaní: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun showMultiplePickerImages() {
        multiplePhotoPickLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private suspend fun createTempFileFromUri(uri: android.net.Uri): File? = withContext(Dispatchers.IO) {
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
            val tempFile = File(tempDir, "img_${UUID.randomUUID()}$extension")

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

    private fun uploadToCloudinary(file: File) {
        // Vykonanie uploadu na Cloudinary
        MediaManager.get().upload(file.absolutePath)
            .unsigned("ml_default")
            .option("folder", "app_gallery")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Upload začal
                    Log.d("UPLOAD IMAGE", "Upload začal")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Pokrok uploadu
                    //Log.d("PROGRESS UPLOAD IMAGE", "Pokrok uploadu")
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // Upload úspešný
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val imageUrl = resultData["secure_url"] as String
                            val publicId = resultData["public_id"] as String

                            // Vytvorenie nového záznamu obrázka
                            val imageModel = ImageModel(
                                imagePath = imageUrl,
                                publicId = publicId,
                                imageDate = System.currentTimeMillis()
                            )

                            // Uloženie do Firebase
                            imageRepository.saveImage(imageModel)

                            // Odstránenie dočasného súboru
                            file.delete()

                            withContext(Dispatchers.Main) {
                                showToast("Obrázok úspešne nahraný")
                                Log.d("IMAGE UPLOAD", "Obrázok úspešne nahratý!")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                showToast("Chyba pri ukladaní do databázy: ${e.message}")
                            }
                        }
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Log.d("FAILED UPLOAD IMAGE", "Chyba pri nahrávaní: ${error.description}")
                    }
                    file.delete()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Upload naplánovaný na neskôr (napr. kvôli chýbajúcemu pripojeniu)
                }
            })
            .dispatch()
    }

    private fun observeImages() {
        lifecycleScope.launch {
            val imagesFlow = when(currentSort) {
                "Vzostupne" -> imageRepository.getImagesSortedByDateAsc()
                "Zostupne" -> imageRepository.getImagesSortedByDateDesc()
                else -> imageRepository.getAllImages()
            }

            imagesFlow.collect { images ->
                adapter.submitList(images)
            }
        }
    }

    override fun onDeletePhotoClick(imageModel: ImageModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (imageModel.publicId.isNotEmpty()) {
                    val cloudinary = MediaManager.get().cloudinary

                    try {
                        val result = cloudinary.uploader().destroy(imageModel.publicId, emptyMap<String, Any>())

                        if (result["result"] == "ok") {
                            // Odstránenie z Firebase
                            imageModel.id?.let { id ->
                                imageRepository.deleteImageById(id)
                            }

                            withContext(Dispatchers.Main) {
                                showToast("Obrázok bol úspešne odstránený!")
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
                                imageModel.id?.let { id ->
                                    imageRepository.deleteImageById(id)
                                    showToast("Obrázok bol odstránený z databázy, ale nie z Cloudinary")
                                }
                            } catch (innerE: Exception) {
                                showToast("Nepodarilo sa odstrániť obrázok: ${innerE.message}")
                            }
                        }
                    }
                } else {
                    // Ak nemáme publicId, odstránime len z databázy
                    imageModel.id?.let { id ->
                        imageRepository.deleteImageById(id)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}