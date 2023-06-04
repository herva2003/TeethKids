package com.pi3.teethkids.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.pi3.teethkids.databinding.ListEmergenciesItemBinding
import com.pi3.teethkids.fragments.emergencias.EmergenciaListaFragmentDirections
import com.pi3.teethkids.models.Emergencia
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat

class EmergenciaAdapter(

    private val context: Context,
    private val emergencias: List<Emergencia>,
) : RecyclerView.Adapter<EmergenciaAdapter.EmergenciaViewHolder>() {

    inner class EmergenciaViewHolder(val binding: ListEmergenciesItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Size of data
    override fun getItemCount(): Int = emergencias.size

    // Create view holder which host a single list item view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergenciaViewHolder {
        val binding = ListEmergenciesItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return EmergenciaViewHolder(binding)
    }

    // Bind the data to the view holder programmatically
    override fun onBindViewHolder(holder: EmergenciaViewHolder, position: Int) {

        val emergencia = emergencias[position]
        // Data binding
        with(holder) {
            with (binding) {
                // Image
                Picasso.get().load(emergencia.imageUrl1).into(imgIssue)
                // Format date
                if (emergencia.createdAt != null) {
                    val formattedDate: String = SimpleDateFormat("dd MMMM yyyy, hh.mm aa").format(emergencia.createdAt!!)
                    txtDate.text = formattedDate
                }

                if (emergencia.aceitado) {
                    // Colour the bar with green
                    imgReviewed.setBackgroundColor(Color.GREEN)
                }

                // Route to review if emergency is not reviewed
                if (!emergencia.aceitado) {
                    inspectionCardView.setOnClickListener { View ->
                        val action = EmergenciaListaFragmentDirections.actionEmergenciaListaFragmentToEmergenciaARFragment(emergencia.emergenciaId!!)
                        View.findNavController().navigate(action)
                    }
                }else{
                    inspectionCardView.setOnClickListener { View ->
                        val action = EmergenciaListaFragmentDirections.actionEmergenciaListaFragmentToMostrarEmergenciaFragment(emergencia.emergenciaId!!)
                        View.findNavController().navigate(action)
                    }
                }
            }
        }
    }
}