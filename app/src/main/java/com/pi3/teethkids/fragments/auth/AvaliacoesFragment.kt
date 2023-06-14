package com.pi3.teethkids.fragments.auth

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import com.pi3.teethkids.R
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

        binding.reviewRecyclerView.adapter = ReviewAdapter(view.context!!, reviews) { review, problematicReason ->
            markReviewAsProblematic(review, problematicReason)
        }

        loadUser().addOnSuccessListener {
            loadReviews()
        }

        binding.swipeLayout.setOnRefreshListener {
            loadReviews()
        }

        binding.arrowImage.setOnClickListener {
            view.findNavController().navigate(R.id.action_avaliacoesFragment_to_profileFragment)
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
                    calcularMediaNotas(user.userId!!)
                }
            }
    }

    private fun loadReviews() {

        binding.reviewRecyclerView.adapter = ReviewAdapter(view?.context!!, reviews) { review, problematicReason ->
            markReviewAsProblematic(review, problematicReason)
        }

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
                    binding.reviewRecyclerView.adapter = ReviewAdapter(view?.context!!, reviews) { review, problematicReason ->
                        markReviewAsProblematic(review, problematicReason)
                    }

                    // Remove refreshing
                    binding.swipeLayout.isRefreshing = false
                } else {
                    Toast.makeText(activity, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun markReviewAsProblematic(review: Review, problematicReason: String) {
        val disputeData = hashMapOf(
            "dentistId" to review.dentistId,
            "avaliacaoId" to review.reviewId,
            "comentarioAtendimento" to review.reviewDentist,
            "motivo" to problematicReason,
        )

        FirebaseUtils().firestore
            .collection("disputas")
            .add(disputeData)
            .addOnSuccessListener {
                Toast.makeText(activity, "Comentário marcado como problemático.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(activity, "Erro ao marcar o comentário como problemático: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        val reportadoData = hashMapOf(
            "reportado" to true
        )

        FirebaseUtils().firestore
            .collection("avaliacoes")
            .document(review.reviewId!!)
            .update(reportadoData as Map<String, Any>)
    }

    private fun calcularMediaNotas(userId: String) {
        FirebaseUtils().firestore
            .collection("avaliacoes")
            .whereEqualTo("dentistId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var somaNotas = 0
                var quantidadeNotas = 0

                for (document: DocumentSnapshot in querySnapshot.documents) {
                    val notaDentista = document.getDouble("notaAtendimento")
                    if (notaDentista != null) {
                        somaNotas += notaDentista.toInt()
                        quantidadeNotas++
                    }
                }

                val mediaNotas = if (quantidadeNotas > 0) somaNotas.toDouble() / quantidadeNotas else 0.0
                val mediaNotasString = mediaNotas.toString()

                FirebaseUtils().firestore
                    .collection("users")
                    .document(userId)
                    .update("nota", mediaNotasString)
                    .addOnSuccessListener {
                        // Atualizar a quantidade de avaliações no documento userId
                        updateQuantidadeAvaliacoes(userId, quantidadeNotas)
                    }
            }
    }

    private fun updateQuantidadeAvaliacoes(userId: String, quantidadeAvaliacoes: Int) {
        FirebaseUtils().firestore
            .collection("users")
            .document(userId)
            .update("quantidadeAvaliacoes", quantidadeAvaliacoes)
    }
}
