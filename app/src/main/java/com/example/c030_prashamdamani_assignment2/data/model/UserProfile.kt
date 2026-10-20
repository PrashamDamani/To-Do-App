package com.example.c030_prashamdamani_assignment2.data.model

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val rollNumber: String = "",
    val createdAt: Long = 0L
) {
    val firstName: String get() = fullName.trim().substringBefore(" ").ifBlank { "My" }

    /** "Prasham's" - used in headings such as "Prasham's Tasks". */
    val possessive: String get() = if (fullName.isBlank()) "My" else "${firstName}'s"

    /** Assignment identity such as "C030 - Prasham Damani". Roll number is only shown if set. */
    val identity: String get() = if (rollNumber.isNotBlank()) "$rollNumber - $fullName" else fullName

    /** Calendar heading, e.g. "C030 - Prasham Damani Schedule". */
    val scheduleHeading: String get() = if (rollNumber.isNotBlank()) "$rollNumber - $fullName Schedule" else "$fullName's Schedule"
}
