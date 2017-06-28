package com.example.gy.easiget;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

public class WebViewActivity extends AppCompatActivity {
    TextView murl;
    private WebView webview;
    private static final String TAG = "TestWebviewDemo";
    ProgressDialog pro;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        String url = getIntent().getStringExtra(GetpageActivity.URLSTRING);

//		ActionBar actionBar = getActionBar();
//		actionBar.setTitle(url);
//		actionBar.setDisplayHomeAsUpEnabled(true);
//		actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#55212121")));
//		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);

        //murl = (TextView)findViewById(R.id.url_to_connect);


        //murl.setText(url);

        webview = (WebView) findViewById(R.id.webvw);
        //设置WebView属性，能够执行Javascript脚本
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        // webSettings.setBuiltInZoomControls(true);//support zoom
        webSettings.setUseWideViewPort(true);// 这个很关键
        webSettings.setLoadWithOverviewMode(true);

        webview.loadUrl(url);
        Log.e("URL",url);

        pro = new ProgressDialog(this,R.style.dialog);
        pro.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pro.setCancelable(true);
        pro.setCanceledOnTouchOutside(false);
        pro.setMessage("Loading");
        webview.setWebViewClient(new HelloWebViewClient (){

            @Override
            // 开始加载网页时要做的工作
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if(pro != null)
                    pro.show();
                super.onPageStarted(view, url, favicon);
            }

            @Override
            //加载完成时要做的工作
            public void onPageFinished(WebView view, String url) {
                if(pro != null)
                    pro.dismiss();
                super.onPageFinished(view, url);
            }

            @Override
            // 加载错误时要做的工作
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d(TAG, "error="+ description);
                Toast.makeText(WebViewActivity.this, errorCode+ "/" + description, Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    //设置回退
    //覆盖Activity类的onKeyDown(int keyCoder,KeyEvent event)方法
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
            webview.goBack(); //goBack()表示返回WebView的上一页面
//            webview.stopLoading(); //停止加载
//            pro.dismiss();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    //Web视图
    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            view.loadUrl(url);
            return true;
        }
    }
}


