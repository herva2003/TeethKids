package com.pi3.teethkids.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pi3.teethkids.R
import com.pi3.teethkids.models.Review
import com.pi3.teethkids.databinding.ListReviewItemBinding
import com.pi3.teethkids.utils.FirebaseUtils
import java.text.SimpleDateFormat

class ReviewAdapter(
    private val context: Context,
    private val reviews: List<Review>,
    private val onReviewMarkedAsProblematic: (Review, String) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(val binding: ListReviewItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Size of data
    override fun getItemCount(): Int = reviews.size

    // Create view holder which host a single list item view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ListReviewItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        with(holder) {
            with(binding) {
                // Populate data
                txtName.text = review.userName
                txtDescription.text = review.reviewDentist
                txtNota.text = review.reviewNotaDentist.toString()

                // Format date
                if (review.createdAt != null) {
                    val date: String = SimpleDateFormat("dd MM yyyy").format(review.createdAt!!)
                    txtDate.text = date
                }

                FirebaseUtils().firestore
                    .collection("avaliacoes")
                    .document(review.reviewId!!)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val reportado = documentSnapshot.getBoolean("reportado")

                        if (reportado == true) {
                            btnMarkAsProblematic.isEnabled = false
                            btnMarkAsProblematic.isClickable = false
                            btnMarkAsProblematic.backgroundTintList = ContextCompat.getColorStateList(context, R.color.grey_500)
                        } else {
                            btnMarkAsProblematic.isEnabled = true
                            btnMarkAsProblematic.isClickable = true
                            btnMarkAsProblematic.setOnClickListener {
                                showConfirmationDialog(review)
                            }
                        }
                    }
            }
        }
    }

    private fun showConfirmationDialog(review: Review) {
        val editTextProblematicReason = EditText(context).apply {
            hint = "Motivo"
            setTextColor(ContextCompat.getColor(context, R.color.black))
            setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_padding)
            setPadding(padding, padding, padding, padding)
        }

        val alertDialog = AlertDialog.Builder(context).apply {
            setTitle("Por favor, detalhe o motivo.")
            setView(editTextProblematicReason)
            setPositiveButton("Solicitar revisão") { _, _ ->
                val problematicReason = editTextProblematicReason.text.toString()
                onReviewMarkedAsProblematic(review, problematicReason)
            }
            setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
        }.create()

        alertDialog.show()
    }
}

