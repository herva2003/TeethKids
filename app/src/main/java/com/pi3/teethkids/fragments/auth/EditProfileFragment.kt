package com.pi3.teethkids.fragments.auth

import android.app.Instrumentation.ActivityResult
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ExecutionError
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.R
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentEditProfileBinding
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EditProfileFragment : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var user: User

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load user
        loadUser()
        populateUser()

        // Add event listeners
        addEventListeners()

        binding.editImage.setOnClickListener{
            cameraProviderResult.launch(android.Manifest.permission.CAMERA)
        }

//        Log.d("latLong", "${user.latitude} ${user.longitude}")

    }

    private val cameraProviderResult = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if (it){
            view?.findNavController()?.navigate(R.id.action_editProfileFragment_to_cameraPreviewFragment)
        }else{
            Snackbar.make(binding.root, "Você não concedeu acesso à camêra.", Snackbar.LENGTH_INDEFINITE).show()
        }
    }

    private fun addEventListeners() {
        binding.buttonUpdate.setOnClickListener {
            updateProfile()
        }

        binding.arrowImage.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_editProfileFragment_to_profileFragment)
        }
    }

    private fun loadUser() {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }
    }

    private fun populateUser() {
        // Populate the fields
        with (binding) {
            inputPhoneNumber.setText(user.phoneNumber)
            inputAddress1.setText(user.address1)
            inputAddress2.setText(user.address2)
            inputAddress3.setText(user.address3)
            inputCurriculo.setText(user.curriculo)

            FirebaseUtils().firestore
                .collection("users")
                .document(user.userId!!)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    // Set the user object
                    if (documentSnapshot.exists()) {
                        user = documentSnapshot.toObject<User>()!!
                        user.userId = documentSnapshot.id

                        imgSelfie.background = ShapeDrawable(OvalShape())
                        imgSelfie.clipToOutline = true

                        val imageUrl = user.selfie
                        Picasso.get().load(imageUrl).into(imgSelfie, object : Callback {
                            override fun onSuccess() {
                                // Rotate the bitmap
                                val bitmap = (imgSelfie.drawable as BitmapDrawable).bitmap
                                val rotatedBitmap = rotateBitmap(bitmap, -90f)
                                imgSelfie.scaleType = ImageView.ScaleType.CENTER_CROP
                                imgSelfie.setImageBitmap(rotatedBitmap)
                            }

                            override fun onError(e: Exception) {
                            }
                        })
                    }
                }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun validateInputFields(): Boolean {
        // Validation Rules
        val phoneNumberValidation: Boolean = binding.inputPhoneNumber.text.toString().trim().isNotEmpty()
        val address1Validation: Boolean = binding.inputAddress1.text.toString().trim().isNotEmpty()
        val curriculoValidation: Boolean = binding.inputCurriculo.text.toString().trim().isNotEmpty()

        // Validation Message
        val blankMessage = "Esse campo não pode ser vazio"

        if (!phoneNumberValidation) {
            binding.inputPhoneNumber.error = blankMessage
        }

        if (!address1Validation) {
            binding.inputAddress1.error = blankMessage
        }

        if (!curriculoValidation) {
            binding.inputCurriculo.error = blankMessage
        }

        if (!phoneNumberValidation || !address1Validation || !curriculoValidation) {
            Toast.makeText(activity,"Tente novamente!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }


    private fun updateProfile() {
        if (validateInputFields()) {
            // Get input
            val address1: String = binding.inputAddress1.text.toString().trim()
            val address2: String = binding.inputAddress2.text.toString().trim()
            val address3: String = binding.inputAddress3.text.toString().trim()
            val phoneNumber: String = binding.inputPhoneNumber.text.toString().trim()
            val curriculo: String = binding.inputCurriculo.text.toString().trim()

            // Perform update
            val userHashMap = hashMapOf(
                "address1" to address1,
                "address2" to address2,
                "address3" to address3,
                "phoneNumber" to phoneNumber,
                "curriculo" to curriculo,
            )

            FirebaseUtils().firestore
                .collection("users")
                .document(user.userId!!)
                .update(userHashMap as Map<String, Any>)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(activity, "Atualização concluida!", Toast.LENGTH_SHORT).show()

                        // Go to profile home
                        view?.findNavController()?.navigate(R.id.action_editProfileFragment_to_profileFragment)
                    } else {
                        // Error handling
                        Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}