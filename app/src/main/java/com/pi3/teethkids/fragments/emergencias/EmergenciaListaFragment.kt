package com.pi3.teethkids.fragments.emergencias

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.pi3.teethkids.R
import com.pi3.teethkids.adapters.EmergenciaAdapter
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentEmergenciaListaBinding
import com.pi3.teethkids.models.Emergencia
import com.pi3.teethkids.models.User
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.utils.FirebaseUtils
import kotlin.math.*

class EmergenciaListaFragment : Fragment() {

    private lateinit var binding: FragmentEmergenciaListaBinding
    private var emergencias: ArrayList<Emergencia> = arrayListOf()
    private lateinit var user: User
    private var showAll: Boolean = true
    private lateinit var userLocationGeoPoint: GeoPoint

    interface UserLocationCallback {
        fun onUserLocationLoaded(geoPoint: GeoPoint)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEmergenciaListaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUser()
        loadUserLocation(object : UserLocationCallback {
            override fun onUserLocationLoaded(geoPoint: GeoPoint) {
                userLocationGeoPoint = geoPoint
                addEventListeners()
                loadEmergencies(showAll)
            }
        })
    }

    private fun loadUser() {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }
    }

    private fun addEventListeners() {
        with(binding) {
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

    private fun calcularDistanciaEntrePontos(pontoA: GeoPoint, pontoB: GeoPoint): Double {
        val raioTerra = 6371 // Raio médio da Terra em quilômetros

        val latA = Math.toRadians(pontoA.latitude)
        val lonA = Math.toRadians(pontoA.longitude)
        val latB = Math.toRadians(pontoB.latitude)
        val lonB = Math.toRadians(pontoB.longitude)

        val dlon = lonB - lonA
        val dlat = latB - latA

        val a = sin(dlat / 2).pow(2) + (cos(latA) * cos(latB) * sin(dlon / 2).pow(2))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return raioTerra * c
    }

    private fun loadUserLocation(callback: UserLocationCallback) {
        FirebaseUtils().firestore
            .collection("users")
            .document(user.userId!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val dentistLocation = documentSnapshot.get("dentistLocation") as? Map<*, *>

                if (dentistLocation != null) {
                    val latitude = dentistLocation["latitude"] as? Double
                    val longitude = dentistLocation["longitude"] as? Double

                    if (latitude != null && longitude != null) {
                        Log.d("EmergenciaListaFragment", "User Latitude: $latitude, User Longitude: $longitude")
                        val userLocationGeoPoint = GeoPoint(latitude, longitude)
                        callback.onUserLocationLoaded(userLocationGeoPoint)
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
                            val emergencia: Emergencia = document.toObject<Emergencia>()!!

                            // Skip emergency if user has been declined
                            val recusadoPor = emergencia.recusadoPor
                            if (recusadoPor != null && recusadoPor.contains(user.userId!!)) {
                                continue
                            }

                            emergencia.emergenciaId = document.id

                            // Get location GeoPoint from the document
                            val location = document.getGeoPoint("location")

                            if (location != null) {
                                val distancia = calcularDistanciaEntrePontos(userLocationGeoPoint, location)
                                if (distancia <= 20) {
                                    emergencia.distancia = distancia
                                    emergencias.add(emergencia)
                                }
                            }
                        }

                        // Initialise views if emergency is found
                        if (emergencias.size != 0) {
                            binding.txtEmpty.visibility = View.GONE
                        } else {
                            binding.txtEmpty.visibility = View.VISIBLE
                        }

                        // Sort emergencies
                        emergencias.sortedByDescending {
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