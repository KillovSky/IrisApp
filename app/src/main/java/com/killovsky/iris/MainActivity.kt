package com.killovsky.iris

import FavoritesDialog
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.util.regex.Pattern

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var webView: WebView
    private lateinit var ipAddressEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var loginSection: LinearLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var skipSslCheckBox: CheckBox
    private lateinit var toolbar: Toolbar
    private lateinit var bottomToolbar: Toolbar
    private lateinit var menuButton: Button
    private lateinit var urlEditText: EditText
    private lateinit var backButton: Button
    private lateinit var forwardButton: Button
    private lateinit var favoritesButton: Button
    private lateinit var homeButton: Button
    private lateinit var titleText: TextView
    private var firstURL = ""
    private val favoritesSet = mutableSetOf<String>()
    private val sharedPrefName = "MyAppPreferences"
    private val favoritesKey = "favorites"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialização de componentes UI
        toolbar = findViewById(R.id.toolbar)
        bottomToolbar = findViewById(R.id.bottom_toolbar)
        menuButton = findViewById(R.id.menuButton)
        webView = findViewById(R.id.webView)
        ipAddressEditText = findViewById(R.id.ipAddress)
        portEditText = findViewById(R.id.port)
        connectButton = findViewById(R.id.connectButton)
        progressBar = findViewById(R.id.progressBar)
        loginSection = findViewById(R.id.loginSection)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        skipSslCheckBox = findViewById(R.id.skipSsl)
        urlEditText = findViewById(R.id.urlEditText)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)
        favoritesButton = findViewById(R.id.favoritesButton)
        homeButton = findViewById(R.id.homeButton)
        titleText = findViewById(R.id.titleText)

        setSupportActionBar(toolbar)
        supportActionBar?.title = null
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.text_color))

        configureWebView()

        val toggle = ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        connectButton.setOnClickListener {
            val ipAddress = ipAddressEditText.text.toString().trim()
            val port = portEditText.text.toString().trim()

            if (validateInput(ipAddress, port)) {
                loadUrl("https://${sanitizeUrl(ipAddress)}:${sanitizeUrl(port)}")
                switchToolbarMode(true)
            } else {
                Toast.makeText(this, "Porta ou IP inválidos", Toast.LENGTH_SHORT).show()
            }

            saveConfiguration()
        }

        loadSavedConfiguration()

        urlEditText.setOnEditorActionListener { _, _, _ ->
            val url = urlEditText.text.toString().trim()
            loadUrl(url)
            true
        }

        backButton.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        forwardButton.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        homeButton.setOnClickListener {
            webView.loadUrl(firstURL)
        }

        favoritesButton.setOnClickListener {
            toggleFavorite()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateFavoriteButtonState() {
        val currentUrl = webView.url ?: return
        favoritesButton.text = if (favoritesSet.contains(currentUrl)) "★" else "☆"
    }

    private fun toggleFavorite() {
        val currentUrl = webView.url ?: return
        if (favoritesSet.contains(currentUrl)) {
            favoritesSet.remove(currentUrl)
            Toast.makeText(this, "Removido dos favoritos", Toast.LENGTH_SHORT).show()
        } else {
            favoritesSet.add(currentUrl)
            Toast.makeText(this, "Adicionado aos favoritos", Toast.LENGTH_SHORT).show()
        }
        saveFavorites()
        updateFavoriteButtonState()
    }

    private fun loadFavorites() {
        val sharedPref = getSharedPreferences(sharedPrefName, MODE_PRIVATE)
        val favorites = sharedPref.getStringSet(favoritesKey, mutableSetOf()) ?: mutableSetOf()

        if (favorites.isEmpty()) {
            Toast.makeText(this, "Nenhum favorito salvo ainda", Toast.LENGTH_SHORT).show()
            return
        }

        val favoritesDialog = FavoritesDialog(this, favorites.toList()) { selectedUrl ->
            loadUrl(selectedUrl)
        }
        favoritesDialog.show()
    }

    private fun saveFavorites() {
        val sharedPref = getSharedPreferences(sharedPrefName, MODE_PRIVATE)
        with(sharedPref.edit()) {
            putStringSet(favoritesKey, favoritesSet)
            apply()
        }
    }

    private fun validateInput(ipAddress: String, port: String): Boolean {
        val portNum = port.toIntOrNull()
        val ipAddressPattern = Pattern.compile("^(([0-9]{1,3})\\.){3}([0-9]{1,3})\$")
        return ipAddressPattern.matcher(ipAddress).matches() && portNum != null && portNum in 1..65535
    }

    private fun sanitizeUrl(input: String): String {
        return input.replace("[^\\w.-]".toRegex(), "")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                if (skipSslCheckBox.isChecked) {
                    handler?.proceed()
                } else {
                    handler?.cancel()
                    super.onReceivedSslError(view, handler, error)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                loginSection.visibility = View.GONE
                webView.visibility = View.VISIBLE
                urlEditText.setText(url)
                updateFavoriteButtonState()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                progressBar.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
        }

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = false
        webSettings.databaseEnabled = false
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webSettings.setSupportMultipleWindows(false)
    }

    private fun loadUrl(url: String) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            webView.loadUrl(url)
            firstURL = url
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        } else {
            Toast.makeText(this, "URL inválida", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveConfiguration() {
        val ipAddress = ipAddressEditText.text.toString().trim()
        val port = portEditText.text.toString().trim()

        if (validateInput(ipAddress, port)) {
            val sharedPref = getSharedPreferences("IrisConfig", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("ipAddress", ipAddress)
                putString("port", port)
                putBoolean("skipSSL", skipSslCheckBox.isChecked)
                apply()
            }
        } else {
            Toast.makeText(this, "IP ou Porta inválidos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedConfiguration() {
        val sharedPref = getSharedPreferences("IrisConfig", MODE_PRIVATE)
        val ipAddress = sharedPref.getString("ipAddress", "")
        val port = sharedPref.getString("port", "")
        ipAddressEditText.setText(ipAddress)
        portEditText.setText(port)
        skipSslCheckBox.isChecked = sharedPref.getBoolean("skipSSL", true)
    }

    private fun switchToolbarMode(connected: Boolean) {
        if (connected) {
            titleText.visibility = View.GONE
            backButton.visibility = View.VISIBLE
            homeButton.visibility = View.VISIBLE
            forwardButton.visibility = View.VISIBLE
            favoritesButton.visibility = View.VISIBLE
        } else {
            titleText.visibility = View.VISIBLE
            backButton.visibility = View.GONE
            homeButton.visibility = View.GONE
            forwardButton.visibility = View.GONE
            favoritesButton.visibility = View.GONE
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.login -> {
                loginSection.visibility = View.VISIBLE
                webView.visibility = View.GONE
                switchToolbarMode(false)
            }

            R.id.home -> {
                loadUrl("https://${ipAddressEditText.text}:${portEditText.text}/homepage")
                switchToolbarMode(true)
            }

            R.id.google -> {
                loadUrl("https://www.google.com")
                switchToolbarMode(true)
            }

            R.id.facebook -> {
                loadUrl("https://www.facebook.com")
                switchToolbarMode(true)
            }

            R.id.youtube -> {
                loadUrl("https://www.youtube.com")
                switchToolbarMode(true)
            }

            R.id.instagram -> {
                loadUrl("https://www.instagram.com")
                switchToolbarMode(true)
            }

            R.id.twitter -> {
                loadUrl("https://www.twitter.com")
                switchToolbarMode(true)
            }

            R.id.duckduckgo -> {
                loadUrl("https://www.duckduckgo.com")
                switchToolbarMode(true)
            }

            R.id.wikipedia -> {
                loadUrl("https://www.wikipedia.org")
                switchToolbarMode(true)
            }

            R.id.favorites -> {
                loadFavorites()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
