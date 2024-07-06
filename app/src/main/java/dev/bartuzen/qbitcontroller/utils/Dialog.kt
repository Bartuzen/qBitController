package dev.bartuzen.qbitcontroller.utils

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.bartuzen.qbitcontroller.R

fun <VB : ViewBinding> showDialog(
    bindingInflater: (LayoutInflater) -> VB,
    context: Context,
    block: MaterialAlertDialogBuilder.(binding: VB) -> Unit,
): AlertDialog {
    val binding = bindingInflater(LayoutInflater.from(context))

    val dialog = MaterialAlertDialogBuilder(context)
        .setView(binding.root)
        .apply { block(binding) }
        .create()

    dialog.show()
    return dialog
}

fun showDialog(context: Context, block: MaterialAlertDialogBuilder.() -> Unit): AlertDialog {
    val dialog = MaterialAlertDialogBuilder(context)
        .apply { block() }
        .create()

    dialog.show()
    return dialog
}

fun <VB : ViewBinding> Fragment.showDialog(
    bindingInflater: (LayoutInflater) -> VB,
    block: MaterialAlertDialogBuilder.(binding: VB) -> Unit,
) = showDialog(bindingInflater, requireContext(), block)

fun Fragment.showDialog(block: MaterialAlertDialogBuilder.() -> Unit) = showDialog(requireContext(), block)

fun <VB : ViewBinding> Activity.showDialog(
    bindingInflater: (LayoutInflater) -> VB,
    block: MaterialAlertDialogBuilder.(binding: VB) -> Unit,
) = showDialog(bindingInflater, this, block)

fun Activity.showDialog(block: MaterialAlertDialogBuilder.() -> Unit) = showDialog(this, block)

fun MaterialAlertDialogBuilder.setPositiveButton(
    @StringRes textId: Int = R.string.dialog_ok,
    listener: ((dialog: DialogInterface, which: Int) -> Unit)? = null,
) = apply {
    setPositiveButton(textId, listener)
}

fun MaterialAlertDialogBuilder.setPositiveButton(
    text: String,
    listener: ((dialog: DialogInterface, which: Int) -> Unit)? = null,
) = apply {
    setPositiveButton(text, listener)
}

fun MaterialAlertDialogBuilder.setNegativeButton(
    @StringRes textId: Int = R.string.dialog_cancel,
    listener: ((dialog: DialogInterface, which: Int) -> Unit)? = null,
) = apply {
    setNegativeButton(textId, listener)
}

fun MaterialAlertDialogBuilder.setNegativeButton(
    text: String,
    listener: ((dialog: DialogInterface, which: Int) -> Unit)? = null,
) = apply {
    setNegativeButton(text, listener)
}
