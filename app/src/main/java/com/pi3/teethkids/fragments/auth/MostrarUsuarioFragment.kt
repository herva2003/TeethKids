package com.pi3.teethkids.fragments.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentMostrarUsuarioBinding
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils
import java.text.SimpleDateFormat
import com.squareup.picasso.Picasso
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.navigation.findNavController
import com.pi3.teethkids.R
import com.squareup.picasso.Callback

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

        binding.arrowImage.setOnClickListener {
            view.findNavController().navigate(R.id.action_mostrarUsuarioFragment_to_profileFragment)
        }
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

                    binding.imgSelfie.background = ShapeDrawable(OvalShape())
                    binding.imgSelfie.clipToOutline = true

                    val imageUrl = profileUser.selfie
                    Picasso.get().load(imageUrl).into(binding.imgSelfie, object : Callback {
                        override fun onSuccess() {
                            // Rotate the bitmap
                            val bitmap = (binding.imgSelfie.drawable as BitmapDrawable).bitmap
                            val rotatedBitmap = rotateBitmap(bitmap, -90f)
                            binding.imgSelfie.scaleType = ImageView.ScaleType.CENTER_CROP
                            binding.imgSelfie.setImageBitmap(rotatedBitmap)

                            // Populate profile
                            populateProfile()
                        }

                        override fun onError(e: Exception) {
                            // Handle error
                        }
                    })
                }
            }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
            txtUserCurriculo.text = profileUser.curriculo

            // Set join date
            if (profileUser.createdAt != null) {
                val formattedDate: String = "Se juntou em: " + SimpleDateFormat("MMMM yyyy").format(profileUser.createdAt!!)
                txtUserJoinedAt.text = formattedDate
            }
        }
    }
}