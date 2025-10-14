package com.example.mybooks2

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.WindowInsetsController
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mybooks2.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController
    private lateinit var fab: FloatingActionButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)



//        val navHostFragment = supportFragmentManager
//            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        navController = navHostFragment.navController
//
//        fab = findViewById(R.id.fab)
//        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
//
//        // Link the BottomNavigationView with the NavController
//        NavigationUI.setupWithNavController(bottomNavView, navController)
//
//        // Set up the listener to control FAB visibility
//        setupFabVisibility()

        val window = window
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = decorView.windowInsetsController
            if (controller != null) {
                if (!isDarkTheme()) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        }
    }
    private fun setupFabVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            println(destination)
            when (destination.id) {
                // If the destination is the Home Fragment, show the FAB
                R.id.navigation_home -> fab.show()

                // For any other destination, hide the FAB
                else -> {
                    println(destination.id)
                    fab.hide()
                }
            }
        }
    }
    fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

//            getMenuInflater().inflate(R.menu.action_bar_menu_1, menu);
        return super.onCreateOptionsMenu(menu)

    }

}