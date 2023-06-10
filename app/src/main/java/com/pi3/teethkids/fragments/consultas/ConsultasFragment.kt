package com.pi3.teethkids.fragments.consultas

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.adapters.ConsultaAdapter
import com.pi3.teethkids.adapters.ConsultaAdapterListener
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentConsultasBinding
import com.pi3.teethkids.models.Consulta
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils

class ConsultasFragment : Fragment(), ConsultaAdapterListener {

    private lateinit var binding: FragmentConsultasBinding
    private var consultas: ArrayList<Consulta> = arrayListOf()
    private lateinit var user: User

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConsultasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUser()
        addEventListeners()
        loadConsultas()
    }

    private fun loadUser() {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }
    }

    override fun onAddressSelected(consulta: Consulta, address: String) {
        val emergencyHashMap = hashMapOf(
            "endereco" to address,
        )

        FirebaseUtils().firestore
            .collection("consulta")
            .document(consulta.consultaId!!)
            .update(emergencyHashMap as Map<String, Any>)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Endereço enviado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addEventListeners() {
        with(binding) {
            val context = requireContext()
            // Initialise adapter
            consultaRecyclerView.adapter = ConsultaAdapter(
                consultas, user.userId!!, context, this@ConsultasFragment
            )

            swipeLayout.setOnRefreshListener {
                loadConsultas()
            }
        }
    }

    private fun loadConsultas() {
        // Clear consultas list
        consultas.clear()

        // Retrieve Consultas from Firebase
        FirebaseUtils().firestore
            .collection("consulta")
            .whereEqualTo("dentistId", user.userId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result.documents) {
                        // Add consulta to ArrayList
                        val consulta: Consulta = document.toObject<Consulta>()!!
                        consulta.consultaId = document.id
                        consultas.add(consulta)
                    }
                    // Initialise views if consultas are found
                    if (consultas.isNotEmpty()) {
                        binding.txtEmpty.visibility = View.GONE
                    } else {
                        binding.txtEmpty.visibility = View.VISIBLE
                    }

                    // Sort consultas
                    consultas.sortByDescending {
                        it.createdAt
                    }

                    val context = requireContext()
                    // Update recycler view
                    binding.consultaRecyclerView.adapter = ConsultaAdapter(
                        consultas, user.userId!!, context, this@ConsultasFragment
                    )

                    // Remove refreshing
                    binding.swipeLayout.isRefreshing = false
                } else {
                    Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }
}