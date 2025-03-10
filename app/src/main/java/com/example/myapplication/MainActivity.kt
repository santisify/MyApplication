package com.example.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val executor = ThreadPoolExecutor(
        2, // 核心线程数
        4, // 最大线程数
        30L, TimeUnit.SECONDS, // 空闲线程存活时间
        LinkedBlockingQueue(20) // 任务队列容量
    )

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // 初始化 SwipeRefreshLayout 和 WebView
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        webView = findViewById(R.id.webview)

        // 配置 SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload() // 刷新网页
        }

        // 配置 WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null) // 启用硬件加速

        // 添加 JavaScript 接口
        webView.addJavascriptInterface(WebAppInterface(), "android")

        // 设置 WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                view?.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefreshLayout.isRefreshing = false
            }
        }

        showWelcomeDialog()
        if (!isNetworkAvailable()) {
            loadNoNetworkPage()
        } else {
            webView.loadUrl("https://blog.lazy-boy-acmer.cn")
        }
    }

    private fun showWelcomeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.custom_welcome_dialog, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        // 设置弹窗背景为透明
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 绑定按钮点击事件
        dialogView.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            dialog.dismiss() // 关闭弹窗
        }

        // 显示弹窗
        dialog.show()
    }

    // 检查网络是否可用
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    // 加载无网络页面
    private fun loadNoNetworkPage() {
        val htmlPath = "file:///android_res/raw/no_network.html"
        webView.loadUrl(htmlPath)
    }

    // JavaScript 接口
    inner class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun openSettings() {
            // 跳转到网络设置页面
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }

        @android.webkit.JavascriptInterface
        fun refreshPage() {
            // 刷新页面
            runOnUiThread {
                if (isNetworkAvailable()) {
                    webView.loadUrl("https://blog.lazy-boy-acmer.cn")
                } else {
                    loadNoNetworkPage()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            Log.d("WebView", "Can go back")
            webView.goBack()
        } else {
            Log.d("WebView", "Not going back")
            showExitConfirmationDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.custom_exit_dialog, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            finishAffinity()
        }
        dialog.show()
    }

    fun enqueueTriggerTask(task: Runnable): Boolean {
        return try {
            executor.execute(task)
            true
        } catch (e: RejectedExecutionException) {
            Log.e("TaskManager", "Failed to enqueue task", e)
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
