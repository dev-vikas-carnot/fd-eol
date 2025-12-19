package com.carnot.fd.eol.features.faq.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.carnot.fd.eol.R
import com.carnot.fd.eol.databinding.ActivityFaqBinding
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_FAQ_VIEWED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_VIEW
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_FAQ
import com.carnot.fd.eol.firebase.FirebaseAnalyticsEvents
import com.carnot.fd.eol.utils.getAssetPdfUri


class FAQActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaqBinding
    private lateinit var faqAdapter: FAQAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_faq)
        // Log Share Action Started Event
        val bundle = Bundle().apply {
            putString("event_type", EVENT_TYPE_VIEW)
        }
        FirebaseAnalyticsEvents.logEvent(EVENT_FAQ_VIEWED, SCREEN_FAQ,bundle)

        initView()
        observeChanges()

        // Enable back button in the Action Bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.help)
    }

    private fun initView() {
        //faqAdapter = FAQAdapter()
        getAssetPdfUri("eol_self_doc_page.pdf")?.let { binding.pdfView.initWithUri(it) }
    }

    private fun goToBackActivity() {
        onBackPressed()
    }

    private fun observeChanges() {
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed() // Handle the back button
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    companion object {
    }

}