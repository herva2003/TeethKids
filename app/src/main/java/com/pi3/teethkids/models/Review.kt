package com.pi3.teethkids.models

data class Review(
    var reviewId: String? = null,
    var dentistId: String? = null,
    var userName: String? = null,
    var reviewDentist: String? = null,
    var reviewNotaDentist: Int? = null,
    var reviewApp: String? = null,
    var reviewNotaApp: String? = null,
    var createdAt: Long? = null,
    var reportado: Boolean? = null,
)