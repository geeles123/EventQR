package com.thedavelopers.eventqr.features.common

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.util.Constants

class ComingSoonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coming_soon)

        val titleView = findViewById<TextView>(R.id.txtComingSoonTitle)
        val messageView = findViewById<TextView>(R.id.txtComingSoonMessage)
        val closeButton = findViewById<Button>(R.id.btnComingSoonClose)

        titleView.text = intent.getStringExtra(Constants.EXTRA_TITLE).orEmpty().ifBlank { "Coming Soon" }
        messageView.text = intent.getStringExtra(Constants.EXTRA_MESSAGE).orEmpty().ifBlank {
            "This screen is a foundation and will be connected in the next feature slice."
        }

        closeButton.setOnClickListener {
            finish()
        }
    }
}
