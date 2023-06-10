package com.pi3.teethkids.fragments.auth

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.messaging.FirebaseMessaging
import com.pi3.teethkids.R
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentCompleteRegistrationBinding
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CompleteRegistrationFragment : Fragment() {
    private lateinit var binding: FragmentCompleteRegistrationBinding
    private var currentUser: FirebaseUser? = null

    private var isAddress2Visible = false
    private var isAddress3Visible = false
    private lateinit var address2TextView: AppCompatTextView
    private lateinit var address2EditText: AppCompatEditText
    private lateinit var address3TextView: AppCompatTextView
    private lateinit var address3EditText: AppCompatEditText
    private lateinit var addAddressButton: Button

    private lateinit var currentPhotoPath: String
    private lateinit var photoUri: Uri

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCompleteRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialize as variáveis dos elementos de layout
        address2TextView = binding.txtAddress2
        address2EditText = binding.inputAddress2
        address3TextView = binding.txtAddress3
        address3EditText = binding.inputAddress3
        addAddressButton = binding.buttonAddAddress

        // Configurar clique do botão "+" para mostrar/ocultar campos de endereço
        addAddressButton.setOnClickListener {
            if (!isAddress2Visible) {
                address2TextView.visibility = View.VISIBLE
                address2EditText.visibility = View.VISIBLE
                isAddress2Visible = true
                addAddressButton.text = "+"
            } else if (!isAddress3Visible) {
                address3TextView.visibility = View.VISIBLE
                address3EditText.visibility = View.VISIBLE
                isAddress3Visible = true
                addAddressButton.visibility = View.GONE
            }
        }

        getUser()
        createACC()

        binding.editImage.setOnClickListener{
            openCamera()
        }

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Criando conta...")
        progressDialog.setCancelable(false)


        binding.inputEmail.setText(currentUser!!.email)

    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireContext().packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.pi3.teethkids.fileprovider",
                    it
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                cameraProviderResult.launch(cameraIntent)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private val cameraProviderResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap: Bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            uploadPhotoToFirebase(bitmap)
        } else {
            Snackbar.make(binding.root, "Falha ao capturar imagem", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun uploadPhotoToFirebase(bitmap: Bitmap) {
        val storageRef = FirebaseUtils().storage.reference
        val imageRef = storageRef.child("images/${currentUser!!.uid}.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnSuccessListener {
        }.addOnFailureListener { exception ->
            Toast.makeText(activity, "Falha ao fazer upload da imagem: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUser() {
        currentUser = FirebaseUtils().auth.currentUser
    }

    private fun getFcmToken(): String? {
        return runCatching {
            FirebaseMessaging.getInstance().token.result
        }.getOrNull()
    }

    private fun createACC(){
        binding.buttonRegister.setOnClickListener {
            if (validateInputFields()) {
                progressDialog.show()

                // Get input
                val email: String = binding.inputEmail.text.toString().trim()
                val name: String = binding.inputName.text.toString().trim()
                val phoneNumber: String = binding.inputPhoneNumber.text.toString().trim()
                val address1: String = binding.inputAddress1.text.toString().trim()
                val address2: String = binding.inputAddress2.text.toString().trim()
                val address3: String = binding.inputAddress3.text.toString().trim()
                val curriculo: String = binding.inputCurriculo.text.toString().trim()

                // Upload photo to Firebase
                val bitmap: Bitmap = BitmapFactory.decodeFile(currentPhotoPath)

                val storageRef = FirebaseUtils().storage.reference
                val imageRef = storageRef.child("user-selfies/${currentUser!!.uid}.jpg")

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()
                val uploadTask = imageRef.putBytes(data)
                uploadTask.addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val photoUrl = uri.toString()

                        // Perform registration
                        val userHashMap = hashMapOf(
                            "email" to email,
                            "name" to name,
                            "phoneNumber" to phoneNumber,
                            "address1" to address1,
                            "address2" to address2,
                            "address3" to address3,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "fcmToken" to getFcmToken(),
                            "curriculo" to curriculo,
                            "selfie" to photoUrl,
                            "status" to "OFFLINE"
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
                                            Toast.makeText(activity, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                                            progressDialog.dismiss()

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
                                    Toast.makeText(activity,task.exception!!.message.toString(),Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
            }
        }
    }

    private fun validateInputFields(): Boolean {
        // Validation Rules
        val nameValidation: Boolean = binding.inputName.text.toString().trim().isNotEmpty()
        val address1Validation: Boolean = binding.inputAddress1.text.toString().trim().isNotEmpty()
        val phoneNumberValidation: Boolean = binding.inputPhoneNumber.text.toString().trim().isNotEmpty()
        val curriculoValidation: Boolean = binding.inputCurriculo.text.toString().trim().isNotEmpty()

        // Validation Message
        val blankMessage = "Esse campo não pode ser vazio"

        if (!nameValidation  ) {
            binding.inputName.error = blankMessage
        }

        if (!address1Validation) {
            binding.inputAddress1.error = blankMessage
        }

        if (!phoneNumberValidation) {
            binding.inputPhoneNumber.error = blankMessage
        }

        if (!curriculoValidation) {
            binding.inputCurriculo.error = blankMessage
        }

        if (!nameValidation || !address1Validation || !phoneNumberValidation || !curriculoValidation) {
            Toast.makeText(activity, "Tente novamente!", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if photo was taken
        if (!::currentPhotoPath.isInitialized) {
            Toast.makeText(activity, "Por favor, tire uma foto antes de prosseguir.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}