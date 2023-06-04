package com.pi3.teethkids.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pi3.teethkids.models.Review
import com.pi3.teethkids.databinding.ListReviewItemBinding
import java.text.SimpleDateFormat

class ReviewAdapter(
    private val context: Context,
    private val reviews: List<Review>,
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
            }
        }
    }
}

