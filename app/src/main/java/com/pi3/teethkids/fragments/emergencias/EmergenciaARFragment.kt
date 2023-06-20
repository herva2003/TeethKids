package com.pi3.teethkids.fragments.emergencias

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.messaging.FirebaseMessaging
import com.pi3.teethkids.R
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentEmergenciaArBinding
import com.pi3.teethkids.models.Emergencia
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils
//import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat

class EmergenciaARFragment : Fragment() {
    private lateinit var binding: FragmentEmergenciaArBinding
    private lateinit var emergencia: Emergencia
    private lateinit var user: User

    // Get argument (emergency id)
    private val args: EmergenciaARFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEmergenciaArBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUser()
        loadEmergencia()
        addEventListeners()
    }

    private fun loadUser() {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }

        binding.txtUserName
    }

    private fun loadEmergencia() {
        val emergenciaId : String = args.emergenciaId

        // Load information if emergency id is present
        if (emergenciaId.isNotEmpty()) {
            FirebaseUtils().firestore
                .collection("emergencias")
                .document(emergenciaId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // Grab the emergency object
                        emergencia = documentSnapshot.toObject<Emergencia>()!!
                        emergencia.emergenciaId = documentSnapshot.id

                        // Populate emergency details
                        populateEmergency()

                        // Load patient
                        loadPatient()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(activity, exception.message.toString(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun populateEmergency() {
        with (binding) {

            val emergenciaId : String = args.emergenciaId

            // Show image
            val imageSlider = imageSlider
            val imageList = ArrayList<SlideModel>()

            FirebaseUtils().firestore
                .collection("emergencias")
                .document(emergenciaId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val imageUrl1 = document.getString("imageUrl1")
                        val imageUrl2 = document.getString("imageUrl2")
                        val imageUrl3 = document.getString("imageUrl3")
                        txtIssueName.text = document.getString("clientName")
                        txtIssueTel.text = document.getString("clientPhone")

                        // Verificar se o campo 'imageUrl' existe e não está vazio
                        if (!imageUrl1.isNullOrEmpty() && !imageUrl2.isNullOrEmpty() && !imageUrl3.isNullOrEmpty() ) {
                            imageList.add(SlideModel(imageUrl1))
                            imageList.add(SlideModel(imageUrl2))
                            imageList.add(SlideModel(imageUrl3))

                            imageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP)
                        }
                    }
                }


            // Load date created
            if (emergencia.createdAt != null) {
                val formattedDate: String = SimpleDateFormat("dd MMMM yyyy, hh.mm aa").format(emergencia.createdAt!!)
                txtCreatedAt.text = formattedDate
            }
        }
    }

    private fun loadPatient() {
        if (emergencia.userId != null) {
            FirebaseUtils().firestore
                .collection("users")
                .document(emergencia.userId!!)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // Grab the user object
                        user = documentSnapshot.toObject<User>()!!
                        user.userId = documentSnapshot.id

                        // Populate user details
                        populatePatient()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(activity, exception.message.toString(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun populatePatient() {
        with (binding) {
            txtUserName.text = user.name
            txtIssueName.text = user.name
            txtIssueTel.text = user.phoneNumber
        }
    }

    private fun addEventListeners() {
        with (binding) {
            btnSubmit.setOnClickListener {
                aceitarOuRecusarEmergencia()
            }

            arrowImage.setOnClickListener {
                view?.findNavController()?.navigate(R.id.action_emergenciaARFragment_to_emergenciaListaFragment)
            }
        }
    }

    private fun aceitarOuRecusarEmergencia() {
        val aceitarEmergencia = binding.cbxAceitarEmergencia.isChecked
        val recusarEmergencia = binding.cbxRecusarEmergencia.isChecked

        if (aceitarEmergencia && recusarEmergencia) {
            Toast.makeText(activity, "Marque apenas um botão", Toast.LENGTH_SHORT).show()
        } else if (!aceitarEmergencia && !recusarEmergencia) {
            Toast.makeText(activity, "Marque um botão", Toast.LENGTH_SHORT).show()
        } else {

            // Update emergency
            if (aceitarEmergencia) {
                val emergencyHashMap = hashMapOf(
                    "dentistId" to FieldValue.arrayUnion(user.userId),
                )

                FirebaseUtils().firestore
                    .collection("emergencias")
                    .document(emergencia.emergenciaId!!)
                    .update(emergencyHashMap as Map<String, Any>)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Go to emergency review index
                            Toast.makeText(activity, "Emergencia aceitada!", Toast.LENGTH_SHORT).show()
                            view?.findNavController()?.navigate(R.id.action_emergenciaARFragment_to_emergenciaListaFragment)
                        } else {
                            // Error handling
                            Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {

                // Update emergency
                val emergencyHashMap = hashMapOf(
                    "recusadoPor" to FieldValue.arrayUnion(user.userId),
                )

                FirebaseUtils().firestore
                    .collection("emergencias")
                    .document(emergencia.emergenciaId!!)
                    .update(emergencyHashMap as Map<String, Any>)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Go to emergency review index
                            Toast.makeText(activity, "Emergencia recusada!", Toast.LENGTH_SHORT).show()
                            view?.findNavController()?.navigate(R.id.action_emergenciaARFragment_to_emergenciaListaFragment)
                        } else {
                            // Error handling
                            Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            view?.findNavController()?.navigate(R.id.action_emergenciaARFragment_to_emergenciaListaFragment)
        }
    }
}