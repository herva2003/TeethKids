package com.pi3.teethkids.fragments.auth

import android.content.Context
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
import android.location.Address
import android.location.Geocoder
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng

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

    private fun addressToGeoPoint(context: Context, addressString: String): LatLng? {
        val geocoder = Geocoder(context)
        val addressList: List<Address>?
        val address: Address?
        var location: LatLng? = null

        try {
            addressList = geocoder.getFromLocationName(addressString, 1)
            if (!addressList.isNullOrEmpty()) {
                address = addressList[0]
                location = LatLng(address.latitude, address.longitude)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return location
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

            // Convert address to GeoPoint
            val address1 = profileUser.address1
            val address2 = profileUser.address2
            val address3 = profileUser.address3

            val geoPoint1 = addressToGeoPoint(requireContext(), address1!!)
            val geoPoint2 = addressToGeoPoint(requireContext(), address2!!)
            val geoPoint3 = addressToGeoPoint(requireContext(), address3!!)

            // Set click listeners to save the GeoPoint to the user document
            txtUserAddress1.setOnClickListener {
                saveGeoPointToUser(geoPoint1)
                animateClick(txtUserAddress1)
            }
            txtUserAddress2.setOnClickListener {
                saveGeoPointToUser(geoPoint2)
                animateClick(txtUserAddress2)
            }
            txtUserAddress3.setOnClickListener {
                saveGeoPointToUser(geoPoint3)
                animateClick(txtUserAddress3)
            }

            // Set join date
            if (profileUser.createdAt != null) {
                val formattedDate: String = "Se juntou em: " + SimpleDateFormat("MMMM yyyy").format(profileUser.createdAt!!)
                txtUserJoinedAt.text = formattedDate
            }
        }
    }

    private fun animateClick(view: View) {
        val scaleAnimation = ScaleAnimation(1f, 0.9f, 1f, 0.9f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        scaleAnimation.duration = 100
        scaleAnimation.repeatCount = 1
        scaleAnimation.repeatMode = Animation.REVERSE
        view.startAnimation(scaleAnimation)
    }

    private fun saveGeoPointToUser(geoPoint: LatLng?) {
        if (geoPoint != null) {
            val firestore = FirebaseUtils().firestore

            firestore.collection("users")
                .document(user.userId!!)
                .update("dentistLocation", geoPoint)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Endereço definido como ativo!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Erro ao ativar endereço!", Toast.LENGTH_SHORT).show()
                }
        }
    }
}