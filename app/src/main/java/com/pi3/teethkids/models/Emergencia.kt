package com.pi3.teethkids.models

import java.util.*

data class Emergencia(
    var emergenciaId: String? = null,
    var userId: String? = null,
    var issueName: String? = null,
    var issueTel: String? = null,
    var imageUrl1: String? = null,
    var aceitado: Boolean = false,
    var reviewedAt: Date? = null,
    var createdAt: Date? = null,
)