package com.pi3.teethkids.fragments.auth

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import  android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentCameraPreviewBinding
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewFragment : Fragment() {

    private lateinit var binding: FragmentCameraPreviewBinding

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imgCaptureExecutor: ExecutorService
    private lateinit var user: User

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCameraPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            user = UserConstants.getUser(it)
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.takePhoto.setOnClickListener {
            takePhoto()
            blinkPreview()
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({

            imageCapture = ImageCapture.Builder().build()

            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraPreview", "Failed to open camera.")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun blinkPreview(){
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    private fun takePhoto() {
        imageCapture?.let { imageCapture ->

            val filename = "FOTO_JPEG_${System.currentTimeMillis()}"
            val file = File(requireActivity().externalMediaDirs[0], filename)

            val outPutFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            imageCapture.takePicture(
                outPutFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val imageUri = file.toUri()

                        uploadImageToFirebaseStorage(imageUri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(requireContext(),"Erro ao salvar foto.",Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val storage = FirebaseStorage.getInstance()
        val storageReference: StorageReference = storage.reference

        val imageName = "FOTO_JPEG_${System.currentTimeMillis()}"
        val imageRef = storageReference.child("user-selfies/$imageName")
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                saveImageUrlToFirestore(imageUrl)
            }
        }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        val firestore = FirebaseFirestore.getInstance()

        val imageDocument = firestore.collection("users").document(user.userId!!)

        val data = hashMapOf(
            "selfie" to imageUrl
        )

        imageDocument.update(data as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Imagem atualizada com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao atualizar a imagem.", Toast.LENGTH_SHORT).show()
            }
    }
}