package com.pi3.teethkids.models

import android.media.Image
import java.util.*

data class User(
    var userId: String? = null,
    var email: String? = null,
    var name: String? = null,
    var phoneNumber: String? = null,
    var address1: String? = null,
    var address2: String? = null,
    var address3: String? = null,
    var curriculo: String? = null,
    var createdAt: Date? = null,
    var selfie: String? = null,
    var fcmToken: String? = null,
    var status: String? = null,
    var nota: String? = null,
    var count: String? = null,
    var dentistLocation: Map<String, Double>? = null
)

