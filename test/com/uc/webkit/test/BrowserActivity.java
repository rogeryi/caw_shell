package com.uc.webkit.test;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.uc.webkit.WebChromeClient;
import com.uc.webkit.WebView;
import com.uc.webkit.WebViewClient;
import com.uc.webkit.utils.WebConfiguration;
import com.uc.webkit.utils.WebViewFPS;

public class BrowserActivity extends Activity {

    public static final String  LOG_TAG = "BrowserActivity";
    public static final boolean DEBUG = false;
    public static final boolean ENABLE_HW_ACCLERATION = true;
    public static final boolean ENABLE_ADAPT_SCREEN = false;
    public static final boolean ENABLE_AD_BLOCK = false;

    public static String[] TEST_URLS = new String[] {
            "Put your test urls in '/sdcard/test_urls.config'!!!",
            "http://www.baidu.com",
            "http://www.g.cn",
            "http://www.hao123.com",
            "http://info.3g.qq.com/",
            "http://3g.sina.com",
            "http://www.ifanr.com",
            "http://www.cnbeta.com",
            "http://www.163.com",
            "http://www.sina.com.cn",
            "http://www.craftymind.com/factory/guimark3/bitmap/GM3_JS_Bitmap.html",
            "http://www.craftymind.com/factory/guimark3/compute/GM3_JS_Compute.html",
            "http://www.craftymind.com/factory/guimark3/vector/GM3_JS_Vector.html",
            "http://www.cocos2d-x.org/html5-samples/samples/games/MoonWarriors/index.html",
            "http://ie.microsoft.com/testdrive/performance/fishietank/", 
    };

    private MyWebViewClient mWebViewClient;
    private MyWebChromeClient mWebViewChromeClient;

    private WebView mWebView;
    private final List<WebView> mWebViews = new ArrayList<WebView>();
    private ListView mUrlListView;
    private View mContentView;

    private int mOrientation;
    private boolean mFirstWebView = true;
    private boolean mFullScreen;

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }

        if (mUrlListView != null) {
            System.exit(0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load the library first for native debugging
        try {
            System.loadLibrary("webviewuc");
            System.loadLibrary("webviewuc_plat_support");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Load libwebviewuc.so failed!");
        }

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_PROGRESS);
        mOrientation = getResources().getConfiguration().orientation;

        // Remove background and set window as Opaque to avoid additional glClear in OpenGLRenderer
        getWindow().setFormat(PixelFormat.OPAQUE);
        getWindow().setBackgroundDrawable(null);

        // Enable method trace
        // TraceHelper.ENABLE = true;

        // Setup hardware acceleration
        if (ENABLE_HW_ACCLERATION) {
            final int FLAG_HARDWARE_ACCELERATED = 0x01000000;
            getWindow().setFlags(FLAG_HARDWARE_ACCELERATED, FLAG_HARDWARE_ACCELERATED);
        }

        String url = getUrlFromIntent(this.getIntent());
        if (url == null || url.length() == 0) {
            // Try to read test urls from sdcard/test_urls.config
            final WebConfiguration testUrls = new WebConfiguration(WebConfiguration.TEST_URLS_CONFIG);
            if (testUrls.isValid()) {
                TEST_URLS = testUrls.getLines();
            }

            mUrlListView = new ListView(this);
            mUrlListView.setAdapter(new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, TEST_URLS));
            mUrlListView.setTextFilterEnabled(true);
            mUrlListView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    final String url = TEST_URLS[position];
                    if (mFirstWebView) {
                        openFirstWebView(url);
                    } else {
                        openNewWebView(url);
                    }
                }
            });
            setContentView(mUrlListView);
        } else {
            // Start by TestActivity
            Toast.makeText(this, "intent - " + url, Toast.LENGTH_SHORT).show();
            openFirstWebView(url);
        }
    }

    private static String getUrlFromIntent(Intent intent) {
        return intent != null ? intent.getDataString() : null;
    }

    private void openFirstWebView(String url) {
        try {
            mWebViewClient = new MyWebViewClient();
            mWebViewChromeClient = new MyWebChromeClient();
            openNewWebView(url);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void openNewWebView(String url) {
        openNewWebView(url, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
	private void openNewWebView(String url, boolean background) {
        WebView webview = new WebView(this);
        mWebViews.add(webview);

        webview.setWebViewClient(mWebViewClient);
        webview.setWebChromeClient(mWebViewChromeClient);
        webview.getSettings().setJavaScriptEnabled(true);

        // Context menu listener
        webview.setOnCreateContextMenuListener(this);

        if (url != null && url.length() > 0) {
            Toast.makeText(this, "Load - " + url, Toast.LENGTH_SHORT).show();
            webview.loadUrl(url);
        }

        if (!background) {
            mWebView = webview;
            setContentView(mWebView);
            mWebView.requestFocus();
        }
        mFirstWebView = false;
    }

    private void closeCurrentWebView() {
        closeWebView(mWebView);
    }

    private void closeWebView(WebView webView) {
        if (mWebView == webView) {
            WebView next = getNextOpenWebView(webView);
            setContentView(next != null ? next : mUrlListView);
            mWebView = next;
        }

        if (webView != null) {
            webView.destroy();
            mWebViews.remove(webView);
        }
    }

    private void switchToWebView(int i) {
        if (i >= 0 && i < mWebViews.size()) {
            final WebView webView = mWebViews.get(i);
            setContentView(webView);
            mWebView = webView;
        }
    }

    private void createEmptyWebView() {
        if (mWebViews.size() == 1 && mContentView != mUrlListView) {
            setContentView(mUrlListView);
            mWebView = null;
            return;
        }

        int i = 0;
        String[] windowTitles = new String[mWebViews.size()];
        for (WebView webView : mWebViews) {
            final String title = webView.getTitle();
            windowTitles[i++] = (title != null && title.length() > 0) ? title
                    : "No Title";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Window");
        builder.setItems(windowTitles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg, int which) {
                switchToWebView(which);
            }
        });
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setContentView(mUrlListView);
                        mWebView = null;
                    }
                });
        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dlgWindows = builder.create();
        dlgWindows.show();
    }

    private WebView getNextOpenWebView(WebView webView) {
        if (webView == null || mWebViews.size() == 1)
            return null;

        int idx = mWebViews.indexOf(webView);
        int nextIdx = -1;
        if (idx == 0)
            nextIdx = 1;
        else
            nextIdx = idx - 1;
        return mWebViews.get(nextIdx);
    }

    
    @SuppressWarnings("deprecation")
    private void setTitleAndLogo(View view, Bitmap logo) {
        if (view instanceof WebView) {
            final WebView webview = (WebView) view;

            final String title = webview.getTitle();
            if (title != null && title.length() > 0)
                BrowserActivity.this.setTitle(title);
            else
                BrowserActivity.this.setTitle(webview.getUrl());
            
            if (logo == null)
                logo = webview.getFavicon();
            
            if (logo != null)
                getActionBar().setLogo(new BitmapDrawable(logo));
            else
                getActionBar().setLogo(R.drawable.ic_launcher);
        } else {
            setTitle(R.string.app_name);
            getActionBar().setLogo(R.drawable.ic_launcher);
        }
    }

    
    @Override
    public void setContentView(View view) {
        if (mWebView != null)
            mWebView.onPause();

        setTitleAndLogo(view, null);
        mContentView = view;
        super.setContentView(view);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
                return true;
            }

            if (mContentView == mWebView && mUrlListView != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Close Window");
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                closeCurrentWebView();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog dlgWindows = builder.create();
                dlgWindows.show();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mFullScreen) {
            this.getMenuInflater().inflate(R.menu.menu_fullscreen, menu);
        } else {
            this.getMenuInflater().inflate(R.menu.menu, menu);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(Menu.NONE, R.id.menu_id_fullscreen, 0, R.string.mi_fullscreen);
        menu.add(Menu.NONE, R.id.menu_id_back, 0, R.string.mi_back);    
        menu.add(Menu.NONE, R.id.menu_id_forward, 0, R.string.mi_forward);
        menu.add(Menu.NONE, R.id.menu_id_reload, 0, R.string.mi_reload);
        menu.add(Menu.NONE, R.id.menu_id_new, 0, R.string.mi_new);
        menu.add(Menu.NONE, R.id.menu_id_close, 0, R.string.mi_close);
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus)
            fullscreen(mFullScreen);
        super.onWindowFocusChanged(hasFocus);
    }

    private void fullscreen(boolean enable) {
        final Window win = getWindow();
        final WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        final ActionBar ab = this.getActionBar();
        if (enable) {
            if (ab != null)
                ab.hide();

            winParams.flags |= bits;
        } else {
            if (ab != null)
                ab.show();

            winParams.flags &= ~bits;
        }

        win.setAttributes(winParams);
        if (enable) {
            final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 0x00001000;
            mContentView
                    .setSystemUiVisibility(Build.VERSION.SDK_INT >= 19 ? View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            : View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        mFullScreen = enable;
        invalidateOptionsMenu();
    }

    @Override
    public boolean onMenuItemSelected(int featureID, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_id_back:
            if (mWebView != null && mWebView.canGoBack())
                mWebView.goBack();
            return true;
        case R.id.menu_id_forward:
            if (mWebView != null && mWebView.canGoForward())
                mWebView.goForward();
            return true;
        case R.id.menu_id_reload:
            if (mWebView != null) {
                mWebView.reload();
                mWebView.requestFocus();
            }
            return true;
        case R.id.menu_id_new:
            if (mUrlListView != null && mWebViews.size() > 0) {
                createEmptyWebView();
            }
            return true;
        case R.id.menu_id_close:
            if (mWebView != null) {
                if (mUrlListView != null) {
                    closeCurrentWebView();
                } else {
                    mWebView.destroy();
                    finish();
                }
            }
            return true;
        case R.id.menu_id_fullscreen:
            fullscreen(!mFullScreen);
            return true;
        case R.id.menu_id_fps:
            WebViewFPS.ENABLE = !WebViewFPS.ENABLE;
            WebViewFPS.ENABLE_GL = WebViewFPS.ENABLE;
            return true;
            
            /*
             * case R.id.menu_id_fontsize: if (mWebView != null)
             * adjustFontSize(); return true; case R.id.menu_id_fps:
             * WebViewFPS.ENABLE = !WebViewFPS.ENABLE; WebViewFPS.ENABLE_GL =
             * WebViewFPS.ENABLE;
             * UCMobileWebKit.setEnableGLFps(WebViewFPS.ENABLE_GL,
             * WebViewFPS.ENABLE_GL_EX, WebViewFPS.ENABLE_GL_CANVAS,
             * WebViewFPS.ENABLE_GL_AUTO_REDRAW); return true; case
             * R.id.menu_id_fps_ex: WebViewFPS.ENABLE_GL_EX =
             * !WebViewFPS.ENABLE_GL_EX;
             * UCMobileWebKit.setEnableGLFps(WebViewFPS.ENABLE_GL,
             * WebViewFPS.ENABLE_GL_EX, WebViewFPS.ENABLE_GL_CANVAS,
             * WebViewFPS.ENABLE_GL_AUTO_REDRAW); return true; case
             * R.id.menu_id_fps_canvas: WebViewFPS.ENABLE_GL_CANVAS =
             * !WebViewFPS.ENABLE_GL_CANVAS;
             * UCMobileWebKit.setEnableGLFps(WebViewFPS.ENABLE_GL,
             * WebViewFPS.ENABLE_GL_EX, WebViewFPS.ENABLE_GL_CANVAS,
             * WebViewFPS.ENABLE_GL_AUTO_REDRAW); return true; case
             * R.id.menu_id_dump_tile_textures_base:
             * UCMobileWebKit.dumpTileTextures(true); return true; case
             * R.id.menu_id_dump_tile_textures_layer:
             * UCMobileWebKit.dumpTileTextures(false); return true; case
             * R.id.menu_id_set_draw_base: UCMobileWebKit.ENABLE_DRAW_BASE =
             * !UCMobileWebKit.ENABLE_DRAW_BASE;
             * UCMobileWebKit.setDrawLayer(UCMobileWebKit.ENABLE_DRAW_BASE,
             * UCMobileWebKit.ENABLE_DRAW_LAYER); return true; case
             * R.id.menu_id_set_draw_layer: UCMobileWebKit.ENABLE_DRAW_LAYER =
             * !UCMobileWebKit.ENABLE_DRAW_LAYER;
             * UCMobileWebKit.setDrawLayer(UCMobileWebKit.ENABLE_DRAW_BASE,
             * UCMobileWebKit.ENABLE_DRAW_LAYER); return true; case
             * R.id.menu_id_debug_missing_region:
             * UCMobileWebKit.ENABLE_DEBUG_MISSING_REGION =
             * !UCMobileWebKit.ENABLE_DEBUG_MISSING_REGION;
             * UCMobileWebKit.setDebugMissingRegion
             * (UCMobileWebKit.ENABLE_DEBUG_MISSING_REGION); return true; case
             * R.id.menu_id_debug_pure_color:
             * UCMobileWebKit.ENABLE_DEBUG_PURE_COLOR =
             * !UCMobileWebKit.ENABLE_DEBUG_PURE_COLOR;
             * UCMobileWebKit.setDebugPureColor
             * (UCMobileWebKit.ENABLE_DEBUG_PURE_COLOR); return true; case
             * R.id.menu_id_debug_graphic_buffer:
             * UCMobileWebKit.ENABLE_DEBUG_GRAPHIC_BUFFER =
             * !UCMobileWebKit.ENABLE_DEBUG_GRAPHIC_BUFFER;
             * UCMobileWebKit.setDebugGraphicBuffer
             * (UCMobileWebKit.ENABLE_DEBUG_GRAPHIC_BUFFER); return true; case
             * R.id.menu_id_autodraw: WebViewFPS.ENABLE_GL_AUTO_REDRAW =
             * !WebViewFPS.ENABLE_GL_AUTO_REDRAW;
             * UCMobileWebKit.setEnableGLFps(WebViewFPS.ENABLE_GL,
             * WebViewFPS.ENABLE_GL_EX, WebViewFPS.ENABLE_GL_CANVAS,
             * WebViewFPS.ENABLE_GL_AUTO_REDRAW); Toast.makeText(this,
             * "GL AUTO_REDRAW = " + WebViewFPS.ENABLE_GL_AUTO_REDRAW,
             * Toast.LENGTH_LONG).show(); return true; case R.id.menu_id_trace:
             * TraceHelper.ENABLE = !TraceHelper.ENABLE; Toast.makeText(this,
             * "TRACE = " + TraceHelper.ENABLE, Toast.LENGTH_LONG).show();
             * return true; case R.id.menu_id_displaytree: if (mWebView != null)
             * mWebView.dumpDisplayTree(); return true; case
             * R.id.menu_id_rendertree: if (mWebView != null)
             * mWebView.dumpRenderTree(true); return true;
             */
        }
        return super.onMenuItemSelected(featureID, item);
    }

    public class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            BrowserActivity.this.setTitleAndLogo(view, favicon);
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            BrowserActivity.this.setTitleAndLogo(view, null);
        }
    }
    
    public class MyWebChromeClient extends WebChromeClient {
        
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            setProgress(newProgress * 100);
        }
    }
}
