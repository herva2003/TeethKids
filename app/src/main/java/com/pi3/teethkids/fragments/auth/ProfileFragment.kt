package com.pi3.teethkids.fragments.auth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.firestore.FieldValue
import com.pi3.teethkids.R
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentProfileBinding
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils
import java.text.SimpleDateFormat


class   ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var user: User

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load user
        loadUser()
        populateUser()

        // Add event listeners
        addEventListeners()

        val notificationEnabled = context?.let { NotificationManagerCompat.from(it).areNotificationsEnabled() }
        if (notificationEnabled == true) {
            binding.not.visibility = View.GONE
        } else {
            binding.not.visibility = View.VISIBLE
        }

        binding.not.setOnClickListener {
            requestNotificationPermission(requireContext())
        }

        binding.btnAvaliacao.setOnClickListener {
            view.findNavController().navigate(R.id.action_profileFragment_to_avaliacoesFragment)
        }
    }

    private fun addEventListeners() {
        with(binding) {
            // Edit profile button
            btnEditProfile.setOnClickListener {
                view?.findNavController()
                    ?.navigate(R.id.action_profileFragment_to_editProfileFragment)
            }

            // Logout button
            btnLogout.setOnClickListener {
                FirebaseUtils().auth.signOut().also {
                    // Clear user to shared preferences
                    activity?.let { UserConstants.clearUserFromSharedPreference(it) }

                    // Navigate to login page
                    view?.findNavController()?.navigate(R.id.action_loginFragment)
                }
            }

            // Add event listener to point to own profile
            btnViewProfile.setOnClickListener { view ->
                val action =
                    ProfileFragmentDirections.actionProfileFragmentToMostrarUsuarioFragment(user.userId!!)
                view.findNavController().navigate(action)
            }
            statusUser()
        }
    }

    private fun requestNotificationPermission(context: Context) {
        val notificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

        if (notificationEnabled) {
            // As notificações estão ativadas, então o botão da notificação não deve ser visível
            binding.not.visibility = View.GONE
        } else {
            // As notificações estão desativadas, então o botão da notificação deve ser visível
            binding.not.visibility = View.VISIBLE

            val channel = NotificationChannel(
                "channelId",
                "channelName",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "channelDescription"
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val intent = Intent().apply {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
            }
            context.startActivity(intent)
        }
    }


    private fun statusUser() {
        with(binding) {
            var flag = true

            val onHashMap = hashMapOf(
                "status" to "ONLINE",
            )
            val offHashMap = hashMapOf(
                "status" to "OFFLINE",
            )

            btnOnOff.setOnClickListener{

                flag = if (flag) {
                    btnOnOff.setBackgroundColor(Color.GREEN)
                    btnOnOff.text = "ON"
                    false
                } else {
                    btnOnOff.setBackgroundColor(Color.RED)
                    btnOnOff.text = "OFF"
                    true
                }

                if (btnOnOff.text == "ON") {
                    FirebaseUtils().firestore
                        .collection("users")
                        .document(user.userId!!)
                        .update(onHashMap as Map<String, Any>)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Go to inspection review index
                                Toast.makeText(activity, "Voce está ONLINE", Toast.LENGTH_SHORT).show()
                            } else {
                                // Error handling
                                Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    FirebaseUtils().firestore
                        .collection("users")
                        .document(user.userId!!)
                        .update(offHashMap as Map<String, Any>)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Go to inspection review index
                                Toast.makeText(activity, "Voce está OFFLINE", Toast.LENGTH_SHORT).show()
                            } else {
                                // Error handling
                                Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }

    private fun loadUser() {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }
    }

    private fun populateUser() {
        binding.txtName.text = user.name
        }
}