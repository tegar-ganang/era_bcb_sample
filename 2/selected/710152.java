package com.gmxteam.funkydomino.activities.examples.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import org.anddev.andengine.engine.handler.timer.ITimerCallback;
import org.anddev.andengine.engine.handler.timer.TimerHandler;
import org.anddev.andengine.entity.util.FPSCounter;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.Callback;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.StreamUtils;
import org.anddev.andengine.util.SystemUtils;
import org.anddev.andengine.util.SystemUtils.SystemUtilsException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.widget.Toast;
import com.gmxteam.funkydomino.activities.examples.R;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 10:38:36 - 27.06.2010
 */
public abstract class BaseBenchmark extends BaseGameActivity {

    private static final long RANDOM_SEED = 1234567890;

    private static final int DIALOG_SHOW_RESULT = 1;

    private static final String SUBMIT_URL = "http://www.andengine.org/sys/benchmark/submit.php";

    /**
         * 
         */
    protected static final int ANIMATIONBENCHMARK_ID = 0;

    /**
         * 
         */
    protected static final int PARTICLESYSTEMBENCHMARK_ID = ANIMATIONBENCHMARK_ID + 1;

    /**
         * 
         */
    protected static final int PHYSICSBENCHMARK_ID = PARTICLESYSTEMBENCHMARK_ID + 1;

    /**
         * 
         */
    protected static final int ENTITYMODIFIERBENCHMARK_ID = PHYSICSBENCHMARK_ID + 1;

    /**
         * 
         */
    protected static final int SPRITEBENCHMARK_ID = ENTITYMODIFIERBENCHMARK_ID + 1;

    /**
         * 
         */
    protected static final int TICKERTEXTBENCHMARK_ID = SPRITEBENCHMARK_ID + 1;

    private float mFPS;

    /**
         * 
         */
    protected final Random mRandom = new Random(RANDOM_SEED);

    /**
         * 
         * @param pFPS
         */
    protected void showResult(final float pFPS) {
        this.mFPS = pFPS;
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                BaseBenchmark.this.showDialog(DIALOG_SHOW_RESULT);
            }
        });
    }

    /**
         * 
         * @return
         */
    protected abstract int getBenchmarkID();

    /**
         * 
         * @return
         */
    protected abstract float getBenchmarkDuration();

    /**
         * 
         * @return
         */
    protected abstract float getBenchmarkStartOffset();

    /**
         * 
         */
    @Override
    public void onLoadComplete() {
        this.mEngine.registerUpdateHandler(new TimerHandler(this.getBenchmarkStartOffset(), new ITimerCallback() {

            @Override
            public void onTimePassed(final TimerHandler pTimerHandler) {
                BaseBenchmark.this.mEngine.unregisterUpdateHandler(pTimerHandler);
                System.gc();
                BaseBenchmark.this.setUpBenchmarkHandling();
            }
        }));
    }

    /**
         * 
         */
    protected void setUpBenchmarkHandling() {
        final FPSCounter fpsCounter = new FPSCounter();
        this.mEngine.registerUpdateHandler(fpsCounter);
        this.mEngine.registerUpdateHandler(new TimerHandler(this.getBenchmarkDuration(), new ITimerCallback() {

            @Override
            public void onTimePassed(final TimerHandler pTimerHandler) {
                BaseBenchmark.this.showResult(fpsCounter.getFPS());
            }
        }));
    }

    /**
         * 
         * @param pID
         * @return
         */
    @Override
    protected Dialog onCreateDialog(final int pID) {
        switch(pID) {
            case DIALOG_SHOW_RESULT:
                return new AlertDialog.Builder(this).setTitle(this.getClass().getSimpleName()).setMessage(String.format("Result: %.2f FPS", this.mFPS)).setPositiveButton("Submit (Please!)", new OnClickListener() {

                    @Override
                    public void onClick(final DialogInterface pDialog, final int pWhich) {
                        BaseBenchmark.this.submitResults();
                    }
                }).setNegativeButton(android.R.string.cancel, new OnClickListener() {

                    @Override
                    public void onClick(final DialogInterface pDialog, final int pWhich) {
                        BaseBenchmark.this.finish();
                    }
                }).create();
            default:
                return super.onCreateDialog(pID);
        }
    }

    private void submitResults() {
        this.doAsync(R.string.dialog_benchmark_submit_title, R.string.dialog_benchmark_submit_message, new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                final HttpClient httpClient = new DefaultHttpClient();
                final HttpPost httpPost = new HttpPost(SUBMIT_URL);
                final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(18);
                nameValuePairs.add(new BasicNameValuePair("benchmark_id", String.valueOf(BaseBenchmark.this.getBenchmarkID())));
                nameValuePairs.add(new BasicNameValuePair("benchmark_versionname", BaseBenchmark.getVersionName(BaseBenchmark.this)));
                nameValuePairs.add(new BasicNameValuePair("benchmark_versioncode", String.valueOf(BaseBenchmark.getVersionCode(BaseBenchmark.this))));
                nameValuePairs.add(new BasicNameValuePair("benchmark_fps", String.valueOf(BaseBenchmark.this.mFPS).replace(",", ".")));
                nameValuePairs.add(new BasicNameValuePair("device_model", Build.MODEL));
                nameValuePairs.add(new BasicNameValuePair("device_android_version", Build.VERSION.RELEASE));
                nameValuePairs.add(new BasicNameValuePair("device_sdk_version", String.valueOf(Build.VERSION.SDK_INT)));
                nameValuePairs.add(new BasicNameValuePair("device_manufacturer", Build.MANUFACTURER));
                nameValuePairs.add(new BasicNameValuePair("device_brand", Build.BRAND));
                nameValuePairs.add(new BasicNameValuePair("device_build_id", Build.ID));
                nameValuePairs.add(new BasicNameValuePair("device_build", Build.DISPLAY));
                nameValuePairs.add(new BasicNameValuePair("device_device", Build.DEVICE));
                nameValuePairs.add(new BasicNameValuePair("device_product", Build.PRODUCT));
                nameValuePairs.add(new BasicNameValuePair("device_cpuabi", Build.CPU_ABI));
                nameValuePairs.add(new BasicNameValuePair("device_board", Build.BOARD));
                nameValuePairs.add(new BasicNameValuePair("device_fingerprint", Build.FINGERPRINT));
                nameValuePairs.add(new BasicNameValuePair("benchmark_extension_vbo", GLHelper.EXTENSIONS_VERTEXBUFFEROBJECTS ? "1" : "0"));
                nameValuePairs.add(new BasicNameValuePair("benchmark_extension_drawtexture", GLHelper.EXTENSIONS_DRAWTEXTURE ? "1" : "0"));
                final TelephonyManager telephonyManager = (TelephonyManager) BaseBenchmark.this.getSystemService(Context.TELEPHONY_SERVICE);
                nameValuePairs.add(new BasicNameValuePair("device_imei", telephonyManager.getDeviceId()));
                final DisplayMetrics displayMetrics = new DisplayMetrics();
                BaseBenchmark.this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                nameValuePairs.add(new BasicNameValuePair("device_displaymetrics_widthpixels", String.valueOf(displayMetrics.widthPixels)));
                nameValuePairs.add(new BasicNameValuePair("device_displaymetrics_heightpixels", String.valueOf(displayMetrics.heightPixels)));
                nameValuePairs.add(new BasicNameValuePair("device_displaymetrics_xdpi", String.valueOf(displayMetrics.xdpi)));
                nameValuePairs.add(new BasicNameValuePair("device_displaymetrics_ydpi", String.valueOf(displayMetrics.ydpi)));
                try {
                    final float bogoMips = SystemUtils.getCPUBogoMips();
                    nameValuePairs.add(new BasicNameValuePair("device_cpuinfo_bogomips", String.valueOf(bogoMips)));
                } catch (IllegalStateException e) {
                    Debug.e(e);
                }
                try {
                    final float memoryTotal = SystemUtils.getMemoryTotal();
                    final float memoryFree = SystemUtils.getMemoryFree();
                    nameValuePairs.add(new BasicNameValuePair("device_memoryinfo_total", String.valueOf(memoryTotal)));
                    nameValuePairs.add(new BasicNameValuePair("device_memoryinfo_free", String.valueOf(memoryFree)));
                } catch (IllegalStateException e) {
                    Debug.e(e);
                }
                try {
                    final int cpuFrequencyCurrent = SystemUtils.getCPUFrequencyCurrent();
                    final int cpuFrequencyMax = SystemUtils.getCPUFrequencyMax();
                    nameValuePairs.add(new BasicNameValuePair("device_cpuinfo_frequency_current", String.valueOf(cpuFrequencyCurrent)));
                    nameValuePairs.add(new BasicNameValuePair("device_cpuinfo_frequency_max", String.valueOf(cpuFrequencyMax)));
                } catch (SystemUtilsException e) {
                    Debug.e(e);
                }
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                final HttpResponse response = httpClient.execute(httpPost);
                final int statusCode = response.getStatusLine().getStatusCode();
                Debug.d(StreamUtils.readFully(response.getEntity().getContent()));
                if (statusCode == HttpStatus.SC_OK) {
                    return true;
                } else {
                    throw new RuntimeException();
                }
            }
        }, new Callback<Boolean>() {

            @Override
            public void onCallback(final Boolean pCallbackValue) {
                BaseBenchmark.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(BaseBenchmark.this, "Success", Toast.LENGTH_LONG).show();
                        BaseBenchmark.this.finish();
                    }
                });
            }
        }, new Callback<Exception>() {

            @Override
            public void onCallback(final Exception pException) {
                Debug.e(pException);
                Toast.makeText(BaseBenchmark.this, "Exception occurred: " + pException.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
                BaseBenchmark.this.finish();
            }
        });
    }

    /**
         * 
         * @param ctx
         * @return
         */
    public static String getVersionName(final Context ctx) {
        try {
            final PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return pi.versionName;
        } catch (final PackageManager.NameNotFoundException e) {
            Debug.e("Package name not found", e);
            return "?";
        }
    }

    /**
         * 
         * @param ctx
         * @return
         */
    public static int getVersionCode(final Context ctx) {
        try {
            final PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return pi.versionCode;
        } catch (final PackageManager.NameNotFoundException e) {
            Debug.e("Package name not found", e);
            return -1;
        }
    }
}
