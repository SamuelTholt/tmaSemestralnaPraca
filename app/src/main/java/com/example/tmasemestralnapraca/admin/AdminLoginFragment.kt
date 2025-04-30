package com.example.tmasemestralnapraca.admin

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.tmasemestralnapraca.MainActivity
import com.example.tmasemestralnapraca.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminLoginFragment : Fragment() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    private val adminRepository = AdminRepository()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_login, container, false)

        emailEditText = view.findViewById(R.id.editTextEmail)
        passwordEditText = view.findViewById(R.id.editTextPassword)
        loginButton = view.findViewById(R.id.buttonLogin)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginAdmin(email, password)
            } else {
                Toast.makeText(requireContext(), "Fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loginAdmin(email: String, password: String) {
        lifecycleScope.launch {
            val admin = withContext(Dispatchers.IO) {
                adminRepository.getAdminByEmail(email)
            }

            if (admin != null) {
                val result = BCrypt.verifyer().verify(password.toCharArray(), admin.password)
                if (result.verified) {
                    val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean("isAdminLoggedIn", true).apply()

                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.reloadNavigationMenu()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Admin not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        insertDefaultAdminIfNeeded()
    }

    private fun insertDefaultAdminIfNeeded() {
        lifecycleScope.launch {
            val existingAdmin = withContext(Dispatchers.IO) {
                adminRepository.getAdminByEmail("admin@admin.com")
            }

            if (existingAdmin == null) {
                val hashedPassword = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray())
                val defaultAdmin = AdminModel(
                    email = "admin@admin.com",
                    password = hashedPassword
                )
                withContext(Dispatchers.IO) {
                    adminRepository.insertAdmin(defaultAdmin)
                }
                Log.d("AdminLogin", "Default admin inserted.")
            } else {
                Log.d("AdminLogin", "Admin already exists.")
            }
        }
    }
}