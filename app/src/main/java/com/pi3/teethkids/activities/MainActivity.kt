package com.pi3.teethkids.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.R
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.ActivityMainBinding
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var currentUser: FirebaseUser? = null
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get user
        loadUser()

        // Inflate layout with binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialise views & navigation
        initialiseView()
        setupBottomNavigationBar()
    }

    private fun initialiseView() {
        navController = findNavController(R.id.nav_controller)
        binding.bottomNavigation.visibility = View.VISIBLE
    }

    private fun authenticationCheck() {
        if (currentUser != null)  {
            // Login user
            // Add user to shared preference
            UserConstants.saveUser(this, user)

            // Navigate to home page
            navController.navigate(R.id.action_homeFragment)
        } else {
            // Logout user
            // Clear user from shared preference
            UserConstants.clearUserFromSharedPreference(this)

            // Navigate to login page
            navController.navigate(R.id.action_loginFragment)
        }
    }

    private fun loadUser(authCheck: Boolean = true) {
        currentUser = FirebaseUtils().auth.currentUser

        if (currentUser != null) {
            FirebaseUtils().firestore
                .collection("users")
                .document(currentUser!!.uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    // Set the user object
                    if (documentSnapshot.exists()) {
                        user = documentSnapshot.toObject<User>()!!
                        user.userId = documentSnapshot.id

                        // Perform auth check
                        if (authCheck) authenticationCheck()

                        // Refresh navigation bar routing
                        setupBottomNavigationBarRouting()
                    }
                }
        }
    }

    private fun setupBottomNavigationBar() {
        // Hide bottom navigation bar
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Hide bottom navigation bar
                R.id.loginFragment -> binding.bottomNavigation.visibility = View.GONE
                R.id.registerFragment -> binding.bottomNavigation.visibility = View.GONE
                R.id.completeRegistrationFragment -> binding.bottomNavigation.visibility = View.GONE
                R.id.MostrarEmergenciasFragment -> binding.bottomNavigation.visibility = View.GONE
                R.id.emergenciaARFragment -> binding.bottomNavigation.visibility = View.GONE
                R.id.cameraPreviewFragment -> binding.bottomNavigation.visibility = View.GONE
                R.id.editProfileFragment -> binding.bottomNavigation.visibility = View.GONE
                R.id.mostrarUsuarioFragment -> binding.bottomNavigation.visibility = View.GONE
                R.id.avaliacoesFragment -> binding.bottomNavigation.visibility = View.GONE
                
                else -> binding.bottomNavigation.visibility = View.VISIBLE
            }

            // Refresh user
            when (destination.id) {
                R.id.loginFragment -> loadUser(false)
                R.id.homeFragment -> loadUser(false)
            }
        }
    }

    private fun setupBottomNavigationBarRouting() {
        // Setup nav bar routing
        binding.bottomNavigation.setOnItemSelectedListener {
            // Conditional routing
            when (it.itemId) {
                R.id.home -> navController.navigate(R.id.action_homeFragment)
                R.id.Emergencias -> navController.navigate(R.id.action_EmergenciaListaFragment)
                R.id.Consultas -> navController.navigate(R.id.action_ConsultasFragment)
                R.id.Perfil -> navController.navigate(R.id.action_profileFragment)
            }
            true
        }
    }
}