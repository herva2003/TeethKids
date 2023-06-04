package com.pi3.teethkids.fragments.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.R
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentMostrarUsuarioBinding
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils
import java.text.SimpleDateFormat

class MostrarUsuarioFragment : Fragment() {
    private lateinit var binding: FragmentMostrarUsuarioBinding
    private lateinit var user: User
    private lateinit var profileUser: User

    // Get argument (user id)
    private val args: MostrarUsuarioFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMostrarUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load user
        loadUser()

        // Load profile
        loadProfile()

        // load reviews
        loadReviews()
    }

    private fun loadReviews() {

    }
    private fun loadUser() {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }
    }

    private fun loadProfile() {
        val profileUserId: String = args.usuariosId

        FirebaseUtils().firestore
            .collection("users")
            .document(profileUserId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                // Set the user object
                if (documentSnapshot.exists()) {
                    profileUser = documentSnapshot.toObject<User>()!!
                    profileUser.userId = documentSnapshot.id

                    // Populate profile
                    populateProfile()
                }
            }
    }

    private fun populateProfile() {
        // Populate the fields
        with (binding) {

            // Load info name/phone
            txtName.text = profileUser.name
            txtUserEmail.text = profileUser.email
            txtUserTelefone.text = profileUser.phoneNumber
            txtUserAddress1.text = profileUser.address1
            txtUserAddress2.text = profileUser.address2
            txtUserAddress3.text = profileUser.address3

            // Set join date
            if (profileUser.createdAt != null) {
                val formattedDate: String = "Se juntou em: " + SimpleDateFormat("MMMM yyyy").format(profileUser.createdAt!!)
                txtUserJoinedAt.text = formattedDate
            }
        }
    }
}