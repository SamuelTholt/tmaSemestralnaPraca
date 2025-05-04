package com.example.tmasemestralnapraca

import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.tmasemestralnapraca.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppNotificationManager(this).createNotificationChannel()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nastavenie toolbaru ako ActionBar
        setSupportActionBar(binding.toolbar)

        // NavController pre navigáciu medzi fragmentami
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Prepojenie NavigationView s NavigationControllerom
        binding.navigationView.setupWithNavController(navController)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("isAdminLoggedIn", false)

        val navView = binding.navigationView
        navView.menu.clear()

        if (isAdmin) {
            navView.inflateMenu(R.menu.nav_admin_menu)
        } else {
            navView.inflateMenu(R.menu.nav_guest_menu)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_welcome -> navController.navigate(R.id.welcomeFragment)
                R.id.nav_players -> navController.navigate(R.id.playerFragment)
                R.id.nav_gallery -> navController.navigate(R.id.galleryFragment)
                R.id.nav_posts -> navController.navigate(R.id.postFragment)
                R.id.nav_teams -> navController.navigate(R.id.teamFragment)
                R.id.nav_matches -> navController.navigate(R.id.matchFragment)
                R.id.nav_logout -> {
                    prefs.edit().putBoolean("isAdminLoggedIn", false).apply()
                    reloadNavigationMenu()
                    navController.navigate(R.id.welcomeFragment)
                }
                R.id.nav_admin_login -> navController.navigate(R.id.adminLoginFragment)
                R.id.nav_exit -> finish()
            }
            binding.drawerLayout.closeDrawers()
            true
        }

        // Nastavenie DrawerToggle na otváranie/zatváranie menu
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAdminNow = prefs.getBoolean("isAdminLoggedIn", false)
            supportActionBar?.title = if (isAdminNow) "${destination.label} [Admin]" else "${destination.label}"
        }
    }

     fun reloadNavigationMenu() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("isAdminLoggedIn", false)

        val navView = binding.navigationView
        navView.menu.clear()

        if (isAdmin) {
            navView.inflateMenu(R.menu.nav_admin_menu)
        } else {
            navView.inflateMenu(R.menu.nav_guest_menu)
        }
    }

}