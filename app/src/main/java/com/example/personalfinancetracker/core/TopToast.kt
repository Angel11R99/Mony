package com.example.personalfinancetracker.core

import android.content.Context
import android.view.Gravity
import android.widget.Toast

fun Context.showTopToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).apply {
        setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
    }.show()
}
