package com.pi3.teethkids.fragments.auth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.R
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentProfileBinding
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat


class   ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var user: User
    private var nota: Float = 0.0f

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
            val onHashMap = hashMapOf(
                "status" to "ONLINE",
            )
            val offHashMap = hashMapOf(
                "status" to "OFFLINE",
            )

            // Get the initial status from the user object
            var flag = user.status == "ONLINE"

            updateButtonState(flag)

            btnOnOff.setOnClickListener {

                flag = !flag

                updateButtonState(flag)

                val statusHashMap = if (flag) onHashMap else offHashMap

                FirebaseUtils().firestore
                    .collection("users")
                    .document(user.userId!!)
                    .update(statusHashMap as Map<String, Any>)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            user.status = if (flag) "ONLINE" else "OFFLINE"
                            val statusText = if (flag) "ONLINE" else "OFFLINE"
                            Toast.makeText(activity, "Você está $statusText", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun updateButtonState(isOnline: Boolean) {
        with(binding) {
            if (isOnline) {
                btnOnOff.setBackgroundColor(Color.GREEN)
                btnOnOff.text = "ON"
            } else {
                btnOnOff.setBackgroundColor(Color.RED)
                btnOnOff.text = "OFF"
            }
        }
    }

    private fun loadUser() {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun populateUser() {
        binding.txtName.text = user.name

        FirebaseUtils().firestore
            .collection("users")
            .document(user.userId!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val notaString = documentSnapshot.getString("nota") ?: ""
                    nota = notaString.toFloatOrNull() ?: 0.0f
                    binding.txtNota.text = String.format("%.2f", nota)
                }
            }

        FirebaseUtils().firestore
            .collection("users")
            .document(user.userId!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                // Set the user object
                if (documentSnapshot.exists()) {
                    user = documentSnapshot.toObject<User>()!!
                    user.userId = documentSnapshot.id

                    binding.imgSelfie.background = ShapeDrawable(OvalShape())
                    binding.imgSelfie.clipToOutline = true

                    val imageUrl = user.selfie
                    Picasso.get().load(imageUrl).into(binding.imgSelfie, object : Callback {
                        override fun onSuccess() {
                            // Rotate the bitmap
                            val bitmap = (binding.imgSelfie.drawable as BitmapDrawable).bitmap
                            val rotatedBitmap = rotateBitmap(bitmap, -90f)
                            binding.imgSelfie.scaleType = ImageView.ScaleType.CENTER_CROP
                            binding.imgSelfie.setImageBitmap(rotatedBitmap)

                            // Check status and update button color and text
                            val status = user.status
                            if (status == "ONLINE") {
                                binding.btnOnOff.setBackgroundColor(Color.GREEN)
                                binding.btnOnOff.text = "ON"
                            } else {
                                binding.btnOnOff.setBackgroundColor(Color.RED)
                                binding.btnOnOff.text = "OFF"
                            }

                            // Add event listeners after populating the user
                            addEventListeners()
                        }

                        override fun onError(e: Exception) {
                        }
                    })
                }
            }
    }

}