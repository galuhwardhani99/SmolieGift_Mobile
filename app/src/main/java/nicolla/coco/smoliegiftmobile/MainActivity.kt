package nicolla.coco.smoliegiftmobile

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        // Pengaturan website
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // PENTING: Tambahkan ini agar fitur zoom, cache, dan rendering UI lebih baik
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(false) // Biasanya aplikasi web-view mematikan fitur zoom agar terlihat seperti aplikasi native
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false

        // WebViewClient agar link dibuka di dalam aplikasi, bukan dilempar ke Chrome
        webView.webViewClient = WebViewClient()

        // WebChromeClient penting jika web Anda memiliki fitur alert() javascript atau upload file/foto
        webView.webChromeClient = WebChromeClient()

        // Masukkan URL utama website Laravel Anda
        // Pastikan IP Address ini (192.168.1.102) adalah IP Laptop Anda yang aktif saat ini
        webView.loadUrl("http://192.168.1.104:8000")

        // ---------------------------------------------------------
        // PENANGANAN TOMBOL BACK MODERN
        // ---------------------------------------------------------
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }
}