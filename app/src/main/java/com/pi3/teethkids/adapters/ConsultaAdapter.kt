package com.pi3.teethkids.adapters

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.pi3.teethkids.R
import com.pi3.teethkids.databinding.ListConsultaItemBinding
import com.pi3.teethkids.models.Consulta
import java.text.SimpleDateFormat
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import com.pi3.teethkids.utils.FirebaseUtils


interface ConsultaAdapterListener {
    fun onAddressSelected(consulta: Consulta, address: String)
}

class ConsultaAdapter(
    private val consulta: List<Consulta>,
    private val userId: String,
    private val context: Context,
    private val listener: ConsultaAdapterListener
) : RecyclerView.Adapter<ConsultaAdapter.ConsultaViewHolder>() {

    inner class ConsultaViewHolder(val binding: ListConsultaItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.txtLocal.setOnClickListener {
                showCustomDialog(adapterPosition)
            }

            binding.txtDescription.setOnClickListener {
                val clickedConsulta = consulta[adapterPosition]
                openDialer(clickedConsulta.userPhoneNumber!!)
            }

            binding.btnLocal.setOnClickListener {
                val clickedConsulta = consulta[adapterPosition]
                val latitude = clickedConsulta.latitude
                val longitude = clickedConsulta.longitude

                val address = "$latitude,$longitude"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?q=$address"))
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = consulta.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsultaViewHolder {
        val binding = ListConsultaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ConsultaViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ConsultaViewHolder, position: Int) {
        val consulta = consulta[position]

        with (holder) {
            with (binding) {
                // Set date
                if (consulta.createdAt != null) {
                    val time: String = SimpleDateFormat("hh:mm aa").format(consulta.createdAt!!)
                    val date: String = SimpleDateFormat("dd MMMM").format(consulta.createdAt!!)

                    txtTime.text = time
                    txtDate.text = date
                }

                // Description format
                if (consulta.dentistId == userId) {
                    // Set description
                    val description = "Você marcou uma consulta. Ligue para ${consulta.userPhoneNumber} e confirme os detalhes."
                    txtDescription.text = formatTextToBold(description, consulta.userPhoneNumber!!)
                }

                // Set visibility of btnLocal based on latitude and longitude
                val latitude = consulta.latitude
                val longitude = consulta.longitude

                if (latitude != null && longitude != null) {
                    btnLocal.visibility = View.VISIBLE
                } else {
                    btnLocal.visibility = View.GONE
                }
            }
        }
    }

    private fun formatTextToBold(text: String, targetText: String): CharSequence {
        val spannableStringBuilder = SpannableStringBuilder(text)
        val startIndex = text.indexOf(targetText)
        val endIndex = startIndex + targetText.length
        spannableStringBuilder.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableStringBuilder
    }

    private fun openDialer(phoneNumber: String) {
        val permission = Manifest.permission.CALL_PHONE
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            context.startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(permission), REQUEST_CALL_PHONE_PERMISSION)
        }
    }

    private fun showCustomDialog(position: Int) {

        FirebaseUtils().firestore
            .collection("users")
            .document(userId)
            .get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val address1 = document.getString("address1")
                val address2 = document.getString("address2")
                val address3 = document.getString("address3")

                val addresses = listOfNotNull(address1, address2, address3)
                val addressItems = addresses.toTypedArray()
                var addSelected: String? = null

                val alertDialogBuilder = AlertDialog.Builder(context)
                alertDialogBuilder.setTitle("Escolha o endereço a enviar")
                alertDialogBuilder.setSingleChoiceItems(addressItems, -1) { _, which ->
                    addSelected = addressItems[which]
                }
                alertDialogBuilder.setPositiveButton("OK") { _, _ ->
                    if (addSelected != null) {
                        val selectedConsulta = consulta[position]
                        listener.onAddressSelected(selectedConsulta, addSelected!!)
                    } else {
                        Toast.makeText(context, "Selecione um endereço", Toast.LENGTH_SHORT).show()
                    }
                }

                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()

                val button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                button.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_projeto))
                button.setTextColor(ContextCompat.getColor(context, R.color.white))

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.gravity = Gravity.CENTER_VERTICAL
                button.layoutParams = layoutParams
            }
        }
    }

    companion object {
        private const val REQUEST_CALL_PHONE_PERMISSION = 1
    }
}
