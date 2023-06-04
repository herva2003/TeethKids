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
import com.pi3.teethkids.R
import com.pi3.teethkids.databinding.FragmentMostrarEmergenciasBinding
import com.pi3.teethkids.models.Emergencia
import com.pi3.teethkids.utils.FirebaseUtils
import java.text.SimpleDateFormat

class MostrarEmergenciasFragment : Fragment() {

    private lateinit var binding: FragmentMostrarEmergenciasBinding
    private lateinit var emergencia: Emergencia

    // Get argument (emergency id)
    private val args: MostrarEmergenciasFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMostrarEmergenciasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadEmergencia()

        binding.btnAvaliacao.setOnClickListener {
            pedirAvaliacao()
        }
    }

    private fun pedirAvaliacao(){

        val avaliacaoHashMap = hashMapOf(
            "avaliado" to false,
        )

        FirebaseUtils().firestore
            .collection("emergencias")
            .document(emergencia.emergenciaId!!)
            .update(avaliacaoHashMap as Map<String, Any>)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(activity, "Pedido enviado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun loadEmergencia() {
        val emergenciaId : String = args.emergenciasId
        // Load emergency if emergency id is present
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

                        // Populate emergency fields
                        populateEmergencia()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(activity, exception.message.toString(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun populateEmergencia() {
        with (binding) {

            val emergenciaId : String = args.emergenciasId
            // Show image
            val imageSlider = imageSlider
            val imageList = ArrayList<SlideModel>()

            // Recuperar a referência para a coleção 'emergencias'
            val emergenciasCollection = FirebaseFirestore.getInstance().collection("emergencias")

            // Recuperar o documento relevante usando o ID (ou outro critério)
            val emergenciaDoc = emergenciasCollection.document(emergenciaId)

            // Recuperar os dados do documento
            emergenciaDoc.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val imageUrl1 = document.getString("imageUrl1")
                        val imageUrl2 = document.getString("imageUrl2")
                        val imageUrl3 = document.getString("imageUrl3")
                        txtName.text = document.getString("clientName")
                        txtTel.text = document.getString("clientPhone")

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
                val formattedDate: String = SimpleDateFormat("MMMM dd, yyyy hh.mm aa").format(emergencia.createdAt!!)
                txtCreatedAt.text = formattedDate
            }
        }
    }
}