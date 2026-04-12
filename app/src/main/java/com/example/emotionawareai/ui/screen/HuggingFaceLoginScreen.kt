package com.example.emotionawareai.ui.screen

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.GradEnd
import com.example.emotionawareai.ui.theme.GradMid1
import com.example.emotionawareai.ui.theme.GradMid2
import com.example.emotionawareai.ui.theme.GradStart
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple
import com.example.emotionawareai.ui.theme.NeonRose

/**
 * Full-screen WebView that loads the HuggingFace token creation page so the
 * user can log in with their HuggingFace account and create a Read token
 * without ever leaving the app.
 *
 * Token detection strategy:
 *  1. When the page URL path ends with `/tokens` or `/tokens/new` and the page
 *     has finished loading, inject a small JS snippet that searches the DOM for
 *     any visible text node matching the `hf_` token pattern.
 *  2. If a match is found, the JS calls back into [HFTokenBridge.onTokenFound].
 *  3. The bridge forwards the token to [ChatViewModel.setHuggingFaceToken] and
 *     this screen pops back to Settings automatically.
 *
 * The user can also tap "Use this token" manually after navigating to any page
 * that shows their token value, which re-runs the JS extraction on demand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HuggingFaceLoginScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    var detectedToken by remember { mutableStateOf("") }
    var isPageLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current

    // Auto-navigate back once a token has been captured and saved.
    LaunchedEffect(detectedToken) {
        if (detectedToken.isNotBlank()) {
            viewModel.setHuggingFaceToken(detectedToken)
            kotlinx.coroutines.delay(1_500)
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "HuggingFace Login",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (currentUrl.isNotBlank()) {
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Manual "scan for token" button — useful if auto-detect misses it.
                    IconButton(
                        onClick = {
                            webViewRef?.let { wv ->
                                injectTokenScanScript(wv) { token ->
                                    detectedToken = token
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Key,
                            contentDescription = "Scan for token",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1117).copy(alpha = 0.97f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradStart, GradMid1, GradMid2, GradEnd)))
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Token detected banner ─────────────────────────────────
                if (detectedToken.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                NeonCyan.copy(alpha = 0.5f),
                                RoundedCornerShape(0.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Token detected!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = NeonCyan
                            )
                            Text(
                                "Saved & returning to settings…",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan.copy(alpha = 0.7f)
                            )
                        }
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                // ── Page loading indicator ────────────────────────────────
                if (isPageLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonPurple,
                        trackColor = NeonPurple.copy(alpha = 0.1f)
                    )
                }

                // ── Help banner ───────────────────────────────────────────
                if (!isPageLoading && detectedToken.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassCard)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Key,
                            contentDescription = null,
                            tint = NeonPurple.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Log in, then tap New Token → choose Read access → Create. " +
                                "Your token will be captured automatically.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            lineHeight = 16.sp
                        )
                    }
                }

                // ── WebView ───────────────────────────────────────────────
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).also { wv ->
                            webViewRef = wv
                            wv.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                @Suppress("DEPRECATION")
                                databaseEnabled = true
                                @Suppress("DEPRECATION")
                                allowFileAccess = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                userAgentString = wv.settings.userAgentString
                            }

                            // JS bridge — receives token strings from injected JS.
                            wv.addJavascriptInterface(
                                HFTokenBridge { token -> detectedToken = token },
                                "HFBridge"
                            )

                            wv.webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    isPageLoading = true
                                    currentUrl = url ?: ""
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isPageLoading = false
                                    currentUrl = url ?: ""

                                    // Auto-scan for token on pages that are likely to show one.
                                    val relevantPage = url?.contains("huggingface.co") == true &&
                                        (url.contains("/tokens") ||
                                            url.contains("/settings"))
                                    if (relevantPage && detectedToken.isBlank()) {
                                        view?.let { wv2 ->
                                            injectTokenScanScript(wv2) { token ->
                                                detectedToken = token
                                            }
                                        }
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean = false // allow all navigation inside the WebView
                            }

                            // Navigate straight to the token-creation page.
                            // If the user isn't logged in, HuggingFace redirects to login.
                            wv.loadUrl(HF_TOKENS_URL)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

/**
 * Injects a JS snippet that walks the page's text nodes looking for a
 * HuggingFace access-token string (`hf_` followed by 20+ alphanumeric chars).
 * When found, calls [HFTokenBridge.onTokenFound] via [bridge].
 */
private fun injectTokenScanScript(webView: WebView, onFound: (String) -> Unit) {
    // Register a one-shot interface so the JS can call back.
    webView.addJavascriptInterface(
        object {
            @JavascriptInterface
            fun found(token: String) {
                if (token.isNotBlank()) onFound(token)
            }
        },
        "_HFScan"
    )
    @Suppress("SpellCheckingInspection")
    webView.evaluateJavascript(
        """
        (function(){
          var all = document.querySelectorAll('*');
          for (var i = 0; i < all.length; i++) {
            var text = (all[i].innerText || all[i].textContent || '').trim();
            var match = text.match(/\bhf_[A-Za-z0-9]{20,}\b/);
            if (match) { _HFScan.found(match[0]); break; }
          }
        })();
        """.trimIndent(),
        null
    )
}

/** Kotlin-side bridge that receives the detected token from JS. */
private class HFTokenBridge(private val onToken: (String) -> Unit) {
    @JavascriptInterface
    fun onTokenFound(token: String) {
        if (token.matches(Regex("hf_[A-Za-z0-9]{20,}"))) {
            onToken(token)
        }
    }
}

private const val HF_TOKENS_URL =
    "https://huggingface.co/settings/tokens/new?tokenName=EmotionAwareAI&type=read"

