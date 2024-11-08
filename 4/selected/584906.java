package com.custom.view;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.custom.activity.IndexActivity;
import com.custom.utils.Constant;
import com.custom.utils.Constant.DirType;
import com.custom.utils.LoadResources;
import com.custom.utils.Logger;
import com.custom.utils.ScanFoldUtils;
import com.custom.utils.ToGetFile;
import com.custom.utils.ZipToFile;

public class InitView extends FrameLayout {

    private static final Logger logger = Logger.getLogger(IndexView.class);

    protected Context context = null;

    protected BackgroundLinearLayout scrollView = null;

    protected AbsoluteLayout mLayout = null;

    protected WebView mWebView = null;

    protected String foldPath = null;

    protected WindowManager.LayoutParams wmParams = null;

    protected WindowManager wm = null;

    protected int foldDepth = Constant.fistFoldDepth;

    protected ScanFoldUtils scanFoldUtils = null;

    protected int screenHeight = 0;

    protected int screenWidth = 0;

    protected Bitmap bm = null;

    protected ProgressDialog progress = null;

    public InitView(Context context, String foldPath, int foldDepth) {
        super(context);
        this.context = context;
        this.foldPath = foldPath;
        this.foldDepth = foldDepth;
    }

    /**
	 * 
	 * 调用隐藏的WebView方法 <br />
	 * 
	 * 说明：WebView完全退出swf的方法，停止声音的播放。
	 * 
	 * @param name
	 */
    private void callHiddenWebViewMethod(String name) {
        if (mWebView != null) {
            try {
                Log.e("callHiddenWebViewMethod", "callHiddenWebViewMethod");
                Method method = WebView.class.getMethod(name);
                method.invoke(mWebView);
            } catch (NoSuchMethodException e) {
                Log.i("No such method: " + name, e.toString());
            } catch (IllegalAccessException e) {
                Log.i("Illegal Access: " + name, e.toString());
            } catch (InvocationTargetException e) {
                Log.d("Invocation Target Exception: " + name, e.toString());
            }
        }
    }

    protected boolean isRestart = false;

    private MediaPlayer mMediaPlayer = null;

    public void onRestart() {
        isRestart = true;
    }

    public void onStart() {
        logger.error("onStart");
        if (!isRestart) {
            progress = ProgressDialog.show(context, "请稍候", "正在加载资源....");
            initBackground();
            new LoadResAsyncTask().execute(scanFoldUtils);
        }
    }

    protected boolean isFistStart = true;

    public void onResume() {
        logger.error("onResume");
        if (mWebView != null) {
            mWebView.resumeTimers();
            callHiddenWebViewMethod("onResume");
        }
        logger.error("createView(mLayout)" + (!isFistStart) + ":" + (scanFoldUtils.bgtype == Constant.BgType.swf) + ":" + (mLayout != null));
        if (!isFistStart && scanFoldUtils.bgtype == Constant.BgType.swf && wm != null && mLayout != null) {
            createView(mLayout);
        }
        isFistStart = false;
    }

    public void onPause() {
        logger.error("onPause");
        if (mWebView != null) {
            mWebView.pauseTimers();
            callHiddenWebViewMethod("onPause");
        }
        if (scanFoldUtils.bgtype == Constant.BgType.swf && wm != null && mLayout != null) {
            wm.removeView(mLayout);
        }
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) mMediaPlayer.stop();
    }

    public void onStop() {
        logger.error("onStop");
        logger.equals("onDestroy");
        if (bm != null && !bm.isRecycled()) {
            logger.error("onDestroy:" + bm.hashCode());
            bm.recycle();
        }
        bm = null;
        scanFoldUtils = null;
    }

    public void onDestroy() {
    }

    private class LoadResAsyncTask extends AsyncTask<ScanFoldUtils, Integer, ScanFoldUtils> {

        /**
    	 * 获取文件名称
    	 * @param files
    	 * @return
    	 */
        private String[] getFileNames(File[] files) {
            String[] lists = null;
            if (files != null && files.length > 0) {
                lists = new String[files.length];
                for (int i = 0; i < lists.length; i++) {
                    lists[i] = files[i].getName();
                }
            }
            return lists;
        }

        public void modifyInitedFile(HashMap<String, String> btnInfo, String filePath) {
            try {
                RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
                raf.setLength(0);
                raf.close();
                FileOutputStream fos = new FileOutputStream(new File(filePath));
                Iterator it = btnInfo.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    key = key + "\n";
                    fos.write(key.getBytes());
                }
                fos.getChannel().force(true);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 以下COPY SD卡内容
         */
        private void copyFile() {
            if (Constant.getExtSdPath() == null || "".equals(Constant.getExtSdPath())) {
                return;
            }
            HashMap<String, String> btnInfo = new HashMap<String, String>();
            try {
                byte[] buf = LoadResources.loadFile(context, Constant.root_fold + File.separator + Constant.copy_file_info_file, DirType.sd, false);
                if (buf != null) {
                    BufferedReader fin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf)));
                    String line = fin.readLine();
                    while (line != null) {
                        btnInfo.put(line, line);
                        line = fin.readLine();
                    }
                }
            } catch (Exception e) {
            }
            File sdfile = LoadResources.getFileByType(Constant.copy_file_fold, DirType.extSd);
            String[] lists = getFileNames(sdfile.listFiles());
            ZipToFile zipToFile = new ZipToFile();
            if (lists != null) {
                for (int i = 0; i < lists.length; i++) {
                    if (!btnInfo.containsKey(lists[i])) {
                        try {
                            zipToFile.upZipFile(Constant.getExtSdPath() + File.separator + Constant.copy_file_fold + File.separator + lists[i], true, "..");
                            btnInfo.put(lists[i], lists[i]);
                        } catch (Exception e) {
                        }
                    }
                }
                String filePath = Constant.getSdPath() + File.separator + Constant.root_fold + File.separator + Constant.copy_file_info_file;
                modifyInitedFile(btnInfo, filePath);
            }
        }

        /**
         * 以下为加压SD卡内容
         */
        private void initFile() {
            if (Constant.getExtSdPath() == null || "".equals(Constant.getExtSdPath())) {
                return;
            }
            FilenameFilter fl = new FilenameFilter() {

                @Override
                public boolean accept(File arg0, String arg1) {
                    if (arg1.indexOf(".") < 0) return false;
                    return "ZIP".equals(arg1.substring(arg1.indexOf(".") + 1).toUpperCase());
                }
            };
            HashMap<String, String> btnInfo = new HashMap<String, String>();
            try {
                byte[] buf = LoadResources.loadFile(context, Constant.root_fold + File.separator + Constant.inited_file_info_file, DirType.sd, false);
                if (buf != null) {
                    BufferedReader fin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf)));
                    String line = fin.readLine();
                    while (line != null) {
                        btnInfo.put(line, line);
                        line = fin.readLine();
                    }
                }
            } catch (Exception e) {
            }
            File sdfile = LoadResources.getFileByType(Constant.inited_file_fold, DirType.extSd);
            String[] lists = getFileNames(sdfile.listFiles(fl));
            ToGetFile toGetFile = new ToGetFile();
            if (lists != null) {
                for (int i = 0; i < lists.length; i++) {
                    if (!btnInfo.containsKey(lists[i])) {
                        toGetFile.downFileFromzip(Constant.getExtSdPath() + File.separator + Constant.inited_file_fold + File.separator + lists[i]);
                        btnInfo.put(lists[i], lists[i]);
                    }
                }
                String filePath = Constant.getSdPath() + File.separator + Constant.root_fold + File.separator + Constant.inited_file_info_file;
                modifyInitedFile(btnInfo, filePath);
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected ScanFoldUtils doInBackground(ScanFoldUtils... scanFoldUtils) {
            initFile();
            copyFile();
            return scanFoldUtils[0];
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onPostExecute(ScanFoldUtils scanFoldUtils) {
            if (progress != null) progress.dismiss();
            Intent i = new Intent();
            i.setClass(context.getApplicationContext(), IndexActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            ((Activity) context).finish();
        }

        @Override
        protected void onCancelled() {
        }
    }

    /**
	 * 构建界面
	 */
    public void initView() {
    }

    protected void initBackground() {
        try {
            if (scanFoldUtils == null) {
                scanFoldUtils = new ScanFoldUtils(context, foldPath, foldDepth);
            }
            logger.error("logger.error(scanFoldUtils.bgPic);" + scanFoldUtils.bgPic);
            if (scanFoldUtils.bgPic == null) return;
            if (scanFoldUtils.bgtype == Constant.BgType.pic) {
                scrollView = new BackgroundLinearLayout(this.context);
                scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
                this.addView(scrollView);
                if (bm == null) {
                    bm = LoadResources.loadBitmap(context, scanFoldUtils.bgPic, scanFoldUtils.bgDirtype);
                }
                int[] viewXY = calBackGroudView(bm);
                mLayout = new AbsoluteLayout(context);
                LinearLayout.LayoutParams mLayoutParams = new LinearLayout.LayoutParams(viewXY[0], viewXY[1]);
                mLayout.setLayoutParams(mLayoutParams);
                mLayout.setBackgroundDrawable(new BitmapDrawable(bm));
                scrollView.setFocusable(true);
                scrollView.setFocusableInTouchMode(true);
                scrollView.addView(mLayout);
            } else if (scanFoldUtils.bgtype == Constant.BgType.swf) {
                if (mWebView == null) {
                    mWebView = new WebView(context);
                    mWebView.setHorizontalScrollBarEnabled(false);
                    mWebView.setVerticalScrollBarEnabled(false);
                    mWebView.getSettings().setJavaScriptEnabled(true);
                    mWebView.getSettings().setPluginsEnabled(true);
                    try {
                        LoadResources.saveToTempFile(context, scanFoldUtils.bgPic, scanFoldUtils.bgDirtype, Constant.backGroundSwfName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mWebView.setWebViewClient(new WebViewClient() {

                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            view.loadUrl(url);
                            Log.e("shouldOverrideUrlLoading", "shouldOverrideUrlLoading");
                            return true;
                        }

                        @Override
                        public void onPageFinished(final WebView webView, String url) {
                            try {
                                mWebView.loadUrl("javascript:showgame('" + context.getFilesDir() + File.separator + Constant.backGroundSwfName + "')");
                            } catch (Exception e) {
                            }
                        }
                    });
                    mWebView.loadUrl(Constant.swfView);
                    this.addView(mWebView);
                }
                mLayout = new AbsoluteLayout(context);
                createView(mLayout);
            }
            logger.error("background end");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void createView(View view) {
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wmParams = new WindowManager.LayoutParams();
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wm.addView(view, wmParams);
    }

    protected int[] calBackGroudView(Bitmap bm) {
        int[] viewXY = new int[2];
        viewXY[0] = screenWidth > screenHeight ? screenWidth : screenHeight;
        viewXY[1] = screenWidth > screenHeight ? screenHeight : screenWidth;
        return viewXY;
    }
}
