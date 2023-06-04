package com.pi3.teethkids.fragments.emergencias

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.R
import com.pi3.teethkids.adapters.EmergenciaAdapter
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentEmergenciaListaBinding
import com.pi3.teethkids.models.Emergencia
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils

class EmergenciaListaFragment : Fragment() {

    private lateinit var binding: FragmentEmergenciaListaBinding
    private var emergencias: ArrayList<Emergencia> = arrayListOf()
    private lateinit var user: User
    private var showAll: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEmergenciaListaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUser()
        addEventListeners()
        loadEmergencies(showAll)
    }

    private fun loadUser() {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }
    }

    private fun addEventListeners() {
        with (binding) {
            emergencyRecyclerView.adapter = EmergenciaAdapter(view?.context!!, emergencias)

            swipeLayout.setOnRefreshListener {
                loadEmergencies(showAll)
            }

            btnToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btn_all -> showAll = true
                        R.id.btn_reviewed-> showAll = false
                    }
                    loadEmergencies(showAll)
                }
            }
        }
    }

    private fun loadEmergencies(showAll: Boolean = true) {
        // Clear emergency list
        emergencias = arrayListOf()

        if (showAll) {
            FirebaseUtils().firestore
                .collection("emergencias")
                .whereEqualTo("aceitado", false)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (document in task.result.documents) {
                            // Add emergency to ArrayList
                            val emergencia : Emergencia = document.toObject<Emergencia>()!!
                            emergencia.emergenciaId = document.id
                            emergencias.add(emergencia)
                        }
                        // Initialise views if emergency is found
                        if (emergencias.size != 0) {
                            binding.txtEmpty.visibility = View.GONE
                        } else {
                            binding.txtEmpty.visibility = View.VISIBLE
                        }

                        // Update recycler view
                        binding.emergencyRecyclerView.adapter = EmergenciaAdapter(view?.context!!, emergencias)

                        // Remove refreshing
                        binding.swipeLayout.isRefreshing = false
                    } else {
                        Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            FirebaseUtils().firestore
                .collection("emergencias")
                .whereEqualTo("aceitadoPor", user.userId!!)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (document in task.result.documents) {
                            // Add emergency to ArrayList
                            val emergencia : Emergencia = document.toObject<Emergencia>()!!
                            emergencia.emergenciaId = document.id
                            emergencias.add(emergencia)
                        }

                        // Initialise views if emergency is found
                        if (emergencias.size != 0) {
                            binding.txtEmpty.visibility = View.GONE
                        } else {
                            binding.txtEmpty.visibility = View.VISIBLE
                        }

                        // Sort emergencies
                        emergencias.sortByDescending {
                            it.createdAt
                        }

                        // Update recycler view
                        binding.emergencyRecyclerView.adapter = EmergenciaAdapter(view?.context!!, emergencias)

                        // Remove refreshing
                        binding.swipeLayout.isRefreshing = false
                    } else {
                        Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}