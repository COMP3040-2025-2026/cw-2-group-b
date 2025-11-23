package com.nottingham.mynottingham.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MenuItem(
    val id: String,
    val category: String,
    val name: String,
    val description: String,
    val price: Double
) : Parcelable