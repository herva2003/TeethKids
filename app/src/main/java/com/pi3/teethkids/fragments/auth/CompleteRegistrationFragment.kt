package com.pi3.teethkids.fragments.auth

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.pi3.teethkids.R
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentCompleteRegistrationBinding
import com.pi3.teethkids.datastore.UserPreferencesRepository
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils

class CompleteRegistrationFragment : Fragment() {
    private lateinit var binding: FragmentCompleteRegistrationBinding
    private var currentUser: FirebaseUser? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCompleteRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getUser()
        createEmergencia()
    }

    private fun getUser() {
        currentUser = FirebaseUtils().auth.currentUser
    }

    private fun getFcmToken(): String? {
        return runCatching {
            FirebaseMessaging.getInstance().token.result
        }.getOrNull()
    }

    private fun createEmergencia(){
        binding.buttonRegister.setOnClickListener {
            if (validateInputFields()) {
                // Get input
                val email: String = binding.inputEmail.text.toString().trim()
                val name: String = binding.inputName.text.toString().trim()
                val phoneNumber: String = binding.inputPhoneNumber.text.toString().trim()
                val address1: String = binding.inputAddress1.text.toString().trim()
                val address2: String = binding.inputAddress2.text.toString().trim()
                val address3: String = binding.inputAddress3.text.toString().trim()

                // Perform registration
                val userHashMap = hashMapOf(
                    "email" to email,
                    "name" to name,
                    "phoneNumber" to phoneNumber,
                    "address1" to address1,
                    "address2" to address2,
                    "address3" to address3,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "fcmToken" to getFcmToken()
                )

                FirebaseUtils().firestore
                    .collection("users")
                    .document(currentUser!!.uid)
                    .set(userHashMap)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            FirebaseUtils().firestore
                                .collection("users")
                                .document(currentUser!!.uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        // Grab the user object
                                        val user = document.toObject<User>()!!
                                        user.userId = document.id

                                        // Save user to shared preferences
                                        activity?.let { UserConstants.saveUser(it, user) }

                                        // Go to home
                                        view?.findNavController()
                                            ?.navigate(R.id.action_completeRegistrationFragment_to_homeFragment)
                                    }
                                }
                        } else {
                            // Error handling
                            Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
    private fun addEventListeners() {

    }

    private fun validateInputFields(): Boolean {
        // Validation Rules
        val nameValidation: Boolean = binding.inputName.text.toString().trim().isNotEmpty()
        val address1Validation: Boolean =
            binding.inputAddress1.text.toString().trim().isNotEmpty()
        val address2Validation: Boolean =
            binding.inputAddress2.text.toString().trim().isNotEmpty()
        val address3Validation: Boolean =
            binding.inputAddress3.text.toString().trim().isNotEmpty()
        val phoneNumberValidation: Boolean =
            binding.inputPhoneNumber.text.toString().trim().isNotEmpty()

        // Validation Message
        val blankMessage = "Esse campo não pode ser vazio"

        if (!nameValidation  ) {
            binding.inputName.error = blankMessage
        }

        if (!address1Validation) {
            binding.inputAddress1.error = blankMessage
        }

        if (!address2Validation) {
            binding.inputAddress2.error = blankMessage
        }

        if (!address3Validation) {
            binding.inputAddress3.error = blankMessage
        }

        if (!phoneNumberValidation) {
            binding.inputPhoneNumber.error = blankMessage
        }

        if (!nameValidation || !address1Validation || !address2Validation
            || !address3Validation || !phoneNumberValidation) {
            Toast.makeText(activity, "Tente novamente!", Toast.LENGTH_SHORT).show()
            return false
        }else{
            Toast.makeText(activity, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
        }
        return true
    }
}