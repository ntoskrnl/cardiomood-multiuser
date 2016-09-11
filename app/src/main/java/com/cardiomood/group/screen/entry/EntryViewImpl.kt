package com.cardiomood.group.screen.entry

import android.app.ProgressDialog
import android.support.design.widget.TextInputLayout
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import com.cardiomood.group.R
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxbinding.widget.textChanges
import rx.Observable
import rx.functions.Action1

class EntryViewImpl(private val view: View) : EntryView {

    val inputField = view.findViewById(R.id.group_code_field) as TextInputLayout
    val inputText = view.findViewById(R.id.group_code) as EditText
    val button = view.findViewById(R.id.submit_button) as ImageButton

    val progressDialog = ProgressDialog(view.context).apply {
        setMessage("Loading...")
        isIndeterminate = true
        setCancelable(false)
    }

    override val submitClicks: Observable<Unit> = button.clicks()

    override val groupCodeInputStream: Observable<String> = inputText.textChanges().map { it.toString() }

    override val showNetworkError = Action1<Unit> {
        inputField.error = "Network request failed. Check Internet connectivity."
    }

    override val showNotFoundError = Action1<Unit> {
        inputField.error = "Group with this code was not found"
    }


    override val clearError = Action1<Unit> {
        inputField.error = null
    }

    override val enableButton = Action1<Boolean> {
        button.isEnabled = it
        button.isClickable = it
    }

    override fun showProgress() {
        progressDialog.show()
    }

    override fun hideProgress() {
        progressDialog.dismiss()
    }
}