package com.pi3.teethkids.constants

import android.app.Activity
import android.content.Context
import com.pi3.teethkids.models.User
import java.text.SimpleDateFormat
import java.util.*

class UserConstants {
    companion object {
        private fun saveData(activity: Activity, variable: String, data: String) {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
            with (sharedPref.edit()) {
                putString(variable, data)
                apply()
            }
        }

        private fun getData(activity: Activity, variable: String, defaultValue: String): String? {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
            return sharedPref.getString(variable, defaultValue)
        }

        fun saveUser(activity: Activity, user: User) {
            // Save data
            user.userId?.let { saveData(activity, "userId", it) }
            user.email?.let { saveData(activity, "email", it) }
            user.name?.let { saveData(activity, "name", it) }
            user.phoneNumber?.let { saveData(activity, "phoneNumber", it) }
            user.address1?.let { saveData(activity, "address1", it) }
            user.address2?.let { saveData(activity, "address2", it) }
            user.address3?.let { saveData(activity, "address3", it) }
            user.curriculo?.let { saveData(activity, "curriculo", it) }

            // Save dentistLocation
            user.dentistLocation?.let { dentistLocation ->
                val dentistLocationMap = dentistLocation.mapValues { it.value.toString() }
                saveData(activity, "dentistLocation", dentistLocationMap.toString())
            }
            // Format date
            user.createdAt?.let {
                val createdAtString: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.createdAt!!)
                saveData(activity, "createdAt", createdAtString)
            }
        }

        fun getUser(activity: Activity): User {
            // Get data
            val userId: String? = getData(activity, "userId", "")
            val email: String? = getData(activity, "email", "")
            val name: String? = getData(activity, "name", "")
            val phoneNumber: String? = getData(activity, "phoneNumber", "")
            val address1: String? = getData(activity, "address1", "")
            val address2: String? = getData(activity, "address2", "")
            val address3: String? = getData(activity, "address3", "")
            val curriculo: String? = getData(activity, "curriculo", "")
            val selfie: String? = getData(activity, "selfie", "")
            val fcmToken: String? = getData(activity, "fcmToken", "")
            val status: String? = getData(activity, "status", "")
            val nota: String? = getData(activity, "nota", "")
            val count: String? = getData(activity, "count", "")

            // Get dentistLocation
            val dentistLocationString: String? = getData(activity, "dentistLocation", "")
            val dentistLocation: Map<String, Double>? = if (!dentistLocationString.isNullOrEmpty()) {
                // Parse string to map
                val dentistLocationMap = dentistLocationString
                    .substring(1, dentistLocationString.length - 1) // Remove enclosing brackets []
                    .split(", ")
                    .map {
                        val keyValue = it.split("=")
                        keyValue[0] to keyValue[1].toDouble()
                    }
                    .toMap()
                dentistLocationMap
            } else {
                null
            }

            // Parse string to date
            val createdAtString: String? = getData(activity, "createdAt", "")
            val createdAt: Date? = if (!createdAtString.isNullOrEmpty()) {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                formatter.parse(createdAtString)
            } else {
                null
            }

            // Return user object
            return User(
                userId,
                email,
                name,
                phoneNumber,
                address1,
                address2,
                address3,
                curriculo,
                createdAt,
                selfie,
                fcmToken,
                status,
                nota,
                count,
                dentistLocation
            )
        }

        fun clearUserFromSharedPreference(activity: Activity) {
            // Clear from shared preference
            saveData(activity, "userId", "")
            saveData(activity, "email", "")
            saveData(activity, "name", "")
            saveData(activity, "phoneNumber", "")
            saveData(activity, "address1", "")
            saveData(activity, "address2", "")
            saveData(activity, "address3", "")
            saveData(activity, "curriculo", "")
            saveData(activity, "createdAt", "")
            saveData(activity, "selfie", "")
            saveData(activity, "fcmToken", "")
            saveData(activity, "status", "")
            saveData(activity, "nota", "")
            saveData(activity, "dentistLocation", "")
        }
    }
}