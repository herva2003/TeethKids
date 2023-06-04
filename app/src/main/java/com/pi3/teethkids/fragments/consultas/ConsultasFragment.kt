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
    private var consulta : ArrayList<Consulta> = arrayListOf()
    private lateinit var user : User
    private lateinit var cons : Consulta

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConsultasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUser()
        addEventListeners()
        loadConsulta()
    }

    private fun loadUser() {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }
    }

    override fun onAddressSelected(address: String) {

        val emergencyHashMap = hashMapOf(
            "endereco" to address,
        )

        FirebaseUtils().firestore
            .collection("consulta")
            .document(cons.consultaId!!)
            .update(emergencyHashMap as Map<String, Any>)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Endereço enviado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, task.exception!!.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun addEventListeners() {
        with (binding) {

            val context = requireContext()
            // Initialise adapter
            appointmentsRecyclerView.adapter = ConsultaAdapter(
                consulta, user.userId!!, context,this@ConsultasFragment)

            swipeLayout.setOnRefreshListener {
                loadConsulta()
            }
        }
    }

    private fun loadConsulta() {
        // Clear consulta list
        consulta = arrayListOf()

        // Retrieve Consulta from Firebase
        FirebaseUtils().firestore
            .collection("consulta")
            .whereEqualTo("dentistId", user.userId)
            .get()
            .addOnCompleteListener { Task ->
                if (Task.isSuccessful) {
                    for (document in Task.result.documents) {
                        // Add consulta to ArrayList
                        val appointment : Consulta = document.toObject<Consulta>()!!
                        appointment.consultaId = document.id
                        consulta.add(appointment)
                    }
                    // Initialise views if consulta is found
                    if (consulta.size != 0) {
                        binding.txtEmpty.visibility = View.GONE
                    } else {
                        binding.txtEmpty.visibility = View.VISIBLE
                    }

                    // Sort consultas
                    consulta.sortByDescending {
                        it.createdAt
                    }

                    // Initialize 'cons' with the first consulta, if available
                    if (consulta.isNotEmpty()) {
                        cons = consulta[0]
                    }

                    val context = requireContext()
                    // Update recycler view
                    binding.appointmentsRecyclerView.adapter = ConsultaAdapter(
                        consulta, user.userId!!, context, this@ConsultasFragment )

                    // Remove refreshing
                    binding.swipeLayout.isRefreshing = false
                } else {
                    Toast.makeText(activity, Task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }
}