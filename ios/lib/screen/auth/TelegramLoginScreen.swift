//
//  TelegramLoginScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

internal import Shared
import SwiftUI
import WebKit

struct TelegramLoginScreen: View {
    let onGetResult: (String) -> Void
    let onCancel: () -> Void

    @State private var isLoading = true

    private var urlToLoad: String {
        "https://oauth.telegram.org/auth?bot_id=\(appConfig.telegramBotId)&origin=\(appConfig.publicBaseUrl)&lang=uz"
    }

    var body: some View {
        ZStack(alignment: .top) {
            WebView(
                urlString: urlToLoad,
                isLoading: $isLoading,
                onGetResult: onGetResult,
                onCancel: onCancel
            )
            .frame(minHeight: 600)
            .background(Color.white)

            if isLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .accentColor))
                    .scaleEffect(1.2)
                    .padding(.top, 64)
            }
        }
    }
}

// MARK: - WebView Wrapper
struct WebView: UIViewRepresentable {
    let urlString: String
    @Binding var isLoading: Bool
    let onGetResult: (String) -> Void
    let onCancel: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIView(context: Context) -> WKWebView {
        // Configure WKWebView with message handler
        let configuration = WKWebViewConfiguration()
        let contentController = WKUserContentController()

        // Add message handler for cancel action
        contentController.add(context.coordinator, name: "cancelHandler")

        // Inject JavaScript to intercept ONLY cancel button click
        let script = WKUserScript(
            source: """
                (function() {
                    // Store the original loginCancel function if it exists
                    const originalLoginCancel = window.loginCancel;

                    // Override the loginCancel function
                    window.loginCancel = function(event) {
                        event.preventDefault();
                        event.stopPropagation();

                        // Notify Swift about cancel
                        window.webkit.messageHandlers.cancelHandler.postMessage('cancel');

                        // Call original function if it existed
                        if (originalLoginCancel && typeof originalLoginCancel === 'function') {
                            originalLoginCancel.call(this, event);
                        }

                        return false;
                    };

                    // More specific: Only intercept buttons with onclick="loginCancel"
                    document.addEventListener('click', function(e) {
                        const button = e.target.closest('button');

                        // Check if it's specifically the cancel button
                        if (button &&
                            button.classList.contains('button-item-flat') &&
                            button.getAttribute('onclick') &&
                            button.getAttribute('onclick').includes('loginCancel')) {

                            e.preventDefault();
                            e.stopPropagation();
                            window.webkit.messageHandlers.cancelHandler.postMessage('cancel');
                        }
                    }, true);
                })();
                """,
            injectionTime: .atDocumentEnd,
            forMainFrameOnly: true
        )

        contentController.addUserScript(script)
        configuration.userContentController = contentController

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.backgroundColor = .white
        webView.isOpaque = true
        webView.navigationDelegate = context.coordinator

        if let url = URL(string: urlString) {
            let request = URLRequest(url: url)
            webView.load(request)
        }

        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        if let currentURL = webView.url?.absoluteString,
            currentURL != urlString,
            let url = URL(string: urlString)
        {
            let request = URLRequest(url: url)
            webView.load(request)
        }
    }

    // MARK: - Coordinator
    class Coordinator: NSObject, WKNavigationDelegate, WKScriptMessageHandler {
        let parent: WebView

        init(_ parent: WebView) {
            self.parent = parent
        }

        // Handle messages from JavaScript
        func userContentController(
            _ userContentController: WKUserContentController,
            didReceive message: WKScriptMessage
        ) {
            if message.name == "cancelHandler", message.body as? String == "cancel" {
                DispatchQueue.main.async {
                    self.parent.onCancel()
                }
            }
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            parent.isLoading = true
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            parent.isLoading = false
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            if let url = navigationAction.request.url {
                // Extract fragment (hash) from URL
                if let fragment = url.fragment, fragment.hasPrefix("tgAuthResult=") {
                    let jsonPart = fragment.replacingOccurrences(of: "tgAuthResult=", with: "")
                    parent.onGetResult(jsonPart)
                }
            }

            decisionHandler(.allow)
        }
    }

    private func clearCookies() {
        let dataStore = WKWebsiteDataStore.default()
        dataStore.fetchDataRecords(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes()) { records in
            dataStore.removeData(
                ofTypes: WKWebsiteDataStore.allWebsiteDataTypes(),
                for: records,
                completionHandler: {}
            )
        }
    }
}

// MARK: - Preview
#Preview {
    TelegramLoginScreen(
        onGetResult: { _ in },
        onCancel: {}
    )
}
