package com.pi3.teethkids.fragments.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.adapters.ReviewAdapter
import com.pi3.teethkids.constants.UserConstants
import com.pi3.teethkids.databinding.FragmentAvaliacoesBinding
import com.pi3.teethkids.models.Review
import com.pi3.teethkids.models.User
import com.pi3.teethkids.utils.FirebaseUtils

class AvaliacoesFragment : Fragment() {

    private var reviews : ArrayList<Review> = arrayListOf()
    private lateinit var user: User
    private lateinit var binding: FragmentAvaliacoesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        binding = FragmentAvaliacoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reviewRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.reviewRecyclerView.adapter = ReviewAdapter(view.context!!, reviews)

        loadUser().addOnSuccessListener {
            loadReviews()
        }

        binding.swipeLayout.setOnRefreshListener {
            loadReviews()
        }
    }

    private fun loadUser(): Task<DocumentSnapshot> {
        // Get user from shared preference
        activity?.let {
            user = UserConstants.getUser(it)
        }
        return FirebaseUtils().firestore
            .collection("users")
            .document(user.userId!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                // Set the user object
                if (documentSnapshot.exists()) {
                    user = documentSnapshot.toObject<User>()!!
                    user.userId = documentSnapshot.id
                }
            }
    }

    private fun loadReviews() {
        // Clear review list
        reviews = arrayListOf()

        // Retrieve reviews from Firebase
        FirebaseUtils().firestore
            .collection("avaliacoes")
            .whereEqualTo("dentistId", user.userId!!)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result.documents) {
                        // Create a new Review object and assign values from the document
                        val review = Review(
                            reviewId = document.id,
                            dentistId = document.getString("dentistId") ?: "",
                            reviewNotaDentist = document.getLong("notaAtendimento")?.toInt() ?: 0,
                            reviewDentist = document.getString("comentarioAtendimento") ?: "",
                            createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            userName = document.getString("userName") ?: ""
                        )

                        // Add the review to the ArrayList
                        reviews.add(review)
                    }

                    // Initialise views if review is found
                    if (reviews.size != 0) {
                        binding.txtEmpty.visibility = View.GONE
                    } else {
                        binding.txtEmpty.visibility = View.VISIBLE
                    }

                    // Sort reviews
                    reviews.sortByDescending {
                        it.createdAt
                    }

                    // Update recycler view
                    binding.reviewRecyclerView.adapter = ReviewAdapter(view?.context!!, reviews)

                    // Remove refreshing
                    binding.swipeLayout.isRefreshing = false
                } else {
                    Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }
}