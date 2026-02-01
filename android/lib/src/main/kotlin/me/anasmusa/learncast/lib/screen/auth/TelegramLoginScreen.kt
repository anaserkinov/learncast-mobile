package me.anasmusa.learncast.lib.screen.auth

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import me.anasmusa.learncast.core.appConfig

@Composable
fun TelegramLoginScreen(
    onGetResult: (result: String) -> Unit,
    onCancel: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    val urlToLoad = "https://oauth.telegram.org/auth?bot_id=${appConfig.telegramBotId}&origin=${appConfig.publicBaseUrl}&lang=uz"
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 600.dp)
                .background(Color.White),
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.WHITE)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
//                    val cookieManager = CookieManager.getInstance()
//                    cookieManager.removeAllCookies(null)
//                    cookieManager.flush()
                    addJavascriptInterface(
                        CancelJsInterface(onCancel),
                        "AndroidCancelHandler"
                    )

                    webViewClient =
                        object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?,
                            ) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                injectCancelInterceptor(this@apply)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val url = request?.url.toString()

                                val uri = url.toUri()
                                val hash = uri.fragment // gives "tgAuthResult=eyJpZCI6..."
                                val jsonPart = hash?.removePrefix("tgAuthResult=")

                                if (jsonPart != null) onGetResult(jsonPart)
                                return false
                            }
                        }

                    loadUrl(urlToLoad)
                }
            },
            update = {
                it.loadUrl(urlToLoad)
            },
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .padding(top = 64.dp)
                        .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
        }
    }
}

class CancelJsInterface(
    private val onCancel: () -> Unit
) {

    @JavascriptInterface
    fun onCancel() {
        Handler(Looper.getMainLooper()).post {
            this.onCancel.invoke()
        }
    }
}


private fun injectCancelInterceptor(webView: WebView) {
    val js = """
        (function() {
            const originalLoginCancel = window.loginCancel;

            window.loginCancel = function(event) {
                if (event) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                AndroidCancelHandler.onCancel();

                if (originalLoginCancel && typeof originalLoginCancel === 'function') {
                    originalLoginCancel.call(this, event);
                }

                return false;
            };

            document.addEventListener('click', function(e) {
                const button = e.target.closest('button');

                if (
                    button &&
                    button.classList.contains('button-item-flat') &&
                    button.getAttribute('onclick') &&
                    button.getAttribute('onclick').includes('loginCancel')
                ) {
                    e.preventDefault();
                    e.stopPropagation();
                    AndroidCancelHandler.onCancel();
                }
            }, true);
        })();
    """.trimIndent()

    webView.evaluateJavascript(js, null)
}

