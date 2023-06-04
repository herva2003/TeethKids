package com.pi3.teethkids.fragments.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.pi3.teethkids.R
import com.pi3.teethkids.databinding.FragmentRegisterBinding
import com.pi3.teethkids.utils.FirebaseUtils

class RegisterFragment : Fragment() {
    private lateinit var binding: FragmentRegisterBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addEventListeners()
    }

    private fun addEventListeners() {
        with (binding) {
            buttonLogin.setOnClickListener {
                view?.findNavController()?.navigate(R.id.action_registerFragment_to_loginFragment)
            }

            buttonRegister.setOnClickListener {
                registerUser()
            }
        }
    }

    private fun validateInputFields(): Boolean {
        // Validation Rules
        val emailValidation: Boolean = binding.inputEmail.text.toString().trim().isNotEmpty()
        val passwordValidation: Boolean = binding.inputPassword.text.toString().isNotEmpty()
        val confirmPasswordValidation: Boolean =
            binding.inputConfirmPassword.text.toString().isNotEmpty()
        val passwordSameValidation: Boolean =
            binding.inputPassword.text.toString() == binding.inputConfirmPassword.text.toString()

        // Validation Message
        val blankMessage = "Esse campo não pode ser vazio"
        val passwordSameMessage = "As senhas devem ser iguais"

        if (!emailValidation) {
            binding.inputEmail.error = blankMessage
        }

        if (!passwordValidation) {
            binding.inputPassword.error = blankMessage
        }

        if (!confirmPasswordValidation) {
            binding.inputConfirmPassword.error = blankMessage
        }

        if (!passwordSameValidation) {
            binding.inputPassword.error = passwordSameMessage
            binding.inputConfirmPassword.error = passwordSameMessage
        }

        if (!emailValidation || !passwordValidation || !confirmPasswordValidation || !passwordSameValidation) {
            Toast.makeText(activity, "Tente novamente!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registerUser() {
        if (validateInputFields()) {
            // Get input
            val email: String = binding.inputEmail.text.toString().trim()
            val password: String = binding.inputPassword.text.toString()

            // Perform registration
            FirebaseUtils().auth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Navigate to CompleteRegistration
                        view?.findNavController()?.navigate(R.id.action_registerFragment_to_completeRegistrationFragment)
                    } else {
                        // Error handling
                        Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}