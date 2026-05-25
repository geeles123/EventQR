package com.thedavelopers.eventqr.features.auth.forgotpassword

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.ForgotPassword
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.SignIn
import com.thedavelopers.eventqr.core.util.Validators

interface ForgotPasswordContract {
    interface View {
        fun showLoading(isLoading: Boolean)
        fun showEmailError(message: String?)
        fun showMessage(message: String)
        fun navigateBackToSignIn()
    }
}

class ForgotPasswordPresenter(
    private var view: ForgotPasswordContract.View?,
) {
    fun attach(view: ForgotPasswordContract.View) {
        this.view = view
    }

    fun detach() {
        view = null
    }

    fun submitRequest(email: String) {
        val emailValue = email.trim()
        if (!Validators.isValidEmail(emailValue)) {
            view?.showEmailError("Enter a valid email address")
            return
        }

        view?.showEmailError(null)
        view?.showLoading(true)
        view?.showLoading(false)
        view?.showMessage("Endpoint not available yet")
    }

    fun backToSignIn() {
        view?.navigateBackToSignIn()
    }
}

open class ForgotPasswordActivity : AppCompatActivity(), ForgotPasswordContract.View {
    private lateinit var presenter: ForgotPasswordPresenter
    private lateinit var emailInput: EditText
    private lateinit var sendButton: Button
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        presenter = ForgotPasswordPresenter(this)
        presenter.attach(this)

        emailInput = findViewById(R.id.editEmail)
        sendButton = findViewById(R.id.btnSendResetLink)
        backButton = findViewById(R.id.btnBackToSignIn)

        sendButton.setOnClickListener {
            presenter.submitRequest(emailInput.text.toString())
        }

        backButton.setOnClickListener {
            presenter.backToSignIn()
        }
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun showLoading(isLoading: Boolean) {
        sendButton.isEnabled = !isLoading
        sendButton.text = if (isLoading) "Sending..." else "Send Reset Link"
    }

    override fun showEmailError(message: String?) {
        emailInput.error = message
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateBackToSignIn() {
        startActivity(Intent(this, SignIn::class.java))
        finish()
    }
}
