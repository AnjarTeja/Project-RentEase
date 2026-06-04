package com.example.rentease

import android.app.Activity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton

object DialogUtils {

    /**
     * Show the standard confirmation dialog (used for Logout, Exit, etc.)
     */
    fun showConfirmationDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String = "Ya, Lanjutkan",
        negativeButtonText: String = "Batal",
        onPositiveClick: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_confirmation, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.dialog_title).text = title
        dialogView.findViewById<TextView>(R.id.dialog_message).text = message
        
        val btnYes = dialogView.findViewById<MaterialButton>(R.id.btn_yes)
        val btnNo = dialogView.findViewById<MaterialButton>(R.id.btn_no)
        
        btnYes.text = positiveButtonText
        btnNo.text = negativeButtonText

        btnYes.setOnClickListener {
            onPositiveClick()
            dialog.dismiss()
        }
        btnNo.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    /**
     * Show the danger confirmation dialog (used for Delete, Reset, etc.)
     * Features the new side-by-side button layout and dark aesthetic.
     */
    fun showDangerDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String = "Ya, Hapus",
        negativeButtonText: String = "Batal",
        iconRes: Int = R.drawable.ic_delete,
        iconBgColorRes: Int = R.drawable.bg_icon_circle_orange,
        onPositiveClick: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_danger, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.dialog_title).text = title
        dialogView.findViewById<TextView>(R.id.dialog_message).text = message
        
        val ivIcon = dialogView.findViewById<ImageView>(R.id.dialog_icon)
        val vIconBgs = listOf(
            dialogView.findViewById<android.view.View>(R.id.dialog_icon_bg),
            dialogView.findViewById<android.view.View>(R.id.dialog_icon_inner_bg)
        )

        ivIcon.setImageResource(iconRes)
        vIconBgs.forEach { it?.setBackgroundResource(iconBgColorRes) }
        
        val btnYes = dialogView.findViewById<MaterialButton>(R.id.btn_yes)
        val btnNo = dialogView.findViewById<MaterialButton>(R.id.btn_no)
        
        btnYes.text = positiveButtonText
        btnNo.text = negativeButtonText

        btnYes.setOnClickListener {
            onPositiveClick()
            dialog.dismiss()
        }
        btnNo.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    /**
     * Show a beautiful, uncancelable loading dialog
     */
    fun showLoadingDialog(activity: Activity, message: String = "Harap tunggu sebentar"): AlertDialog {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_loading_text)
        if (tvMessage != null) {
            tvMessage.text = message
        }

        dialog.show()
        return dialog
    }
}
