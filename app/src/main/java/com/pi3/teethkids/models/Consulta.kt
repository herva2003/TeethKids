package com.pi3.teethkids.models

import java.util.*

data class Consulta(
    var consultaId: String? = null,
    var dentistId: String? = null,
    var userPhoneNumber: String? = null,
    var createdAt: Date? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
)

