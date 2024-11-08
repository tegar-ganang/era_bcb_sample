package edu.cmu.ece.agora.android;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.concurrent.Executors;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.clanwts.nbdf.boot.SimpleBootContext;
import com.clanwts.nbdf.kernel.BootContext;
import com.clanwts.nbdf.kernel.Configuration;
import com.clanwts.nbdf.kernel.Kernel;
import com.clanwts.nbdf.kernel.util.JavaPropertiesConfiguration;
import edu.cmu.ece.agora.core.AbstractAsyncWriter;
import edu.cmu.ece.agora.core.AsyncReader;
import edu.cmu.ece.agora.core.AsyncWriter;
import edu.cmu.ece.agora.core.DirectAsyncQueue;
import edu.cmu.ece.agora.futures.Future;

public class AgoraActivity extends Activity {

    private static AgoraActivity instance = null;

    public static AgoraActivity getInstance() {
        return instance;
    }

    private TextView outputView;

    private EditText inputView;

    private DirectAsyncQueue<String> inputQueue;

    private AsyncReader reader;

    private AsyncWriter writer;

    public AsyncReader getReader() {
        return reader;
    }

    public AsyncWriter getWriter() {
        return writer;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.console);
        instance = this;
        outputView = (TextView) this.findViewById(R.id.TextView01);
        inputView = (EditText) this.findViewById(R.id.EditText01);
        outputView.setText("");
        inputView.setText("");
        inputQueue = new DirectAsyncQueue<String>(Executors.newSingleThreadExecutor());
        reader = new AsyncReader() {

            @Override
            public Future<String> scan() {
                return inputQueue.poll();
            }

            @Override
            public Future<String> scanLine() {
                return inputQueue.poll();
            }
        };
        writer = new AbstractAsyncWriter() {

            @Override
            public void print(final String str) {
                AgoraActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        outputView.append(str);
                        scrollToBottom(outputView);
                    }
                });
            }
        };
        inputView.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView arg0, int action, KeyEvent arg2) {
                if (action == EditorInfo.IME_NULL && (inputView.getText().length() > 0)) {
                    AgoraActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            String line = inputView.getText().toString();
                            Log.d("input line enter", "{" + line + "}");
                            inputQueue.offer(line);
                            outputView.append(line);
                            outputView.append("\n");
                            scrollToBottom(outputView);
                            inputView.setText("");
                        }
                    });
                    return true;
                }
                return false;
            }
        });
        final FrameLayout fl = new FrameLayout(this);
        final EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER);
        fl.addView(input, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        input.setText("");
        new AlertDialog.Builder(this).setView(fl).setTitle("Please enter something...").setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                d.dismiss();
                bootKernel(input.getText().toString());
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                d.dismiss();
                finish();
            }
        }).create().show();
    }

    private void bootKernel(String conf) {
        try {
            AssetManager am = getResources().getAssets();
            InputStream is = am.open(conf + ".conf");
            Properties props = new Properties();
            props.load(is);
            is.close();
            Log.d("bootKernel", "Listing sdcard assets...");
            String[] sdcardfiles = am.list("sdcard");
            for (String file : sdcardfiles) {
                Log.d("bootKernel", "Copying sdcard asset " + file + ".");
                AssetFileDescriptor afd = am.openFd("sdcard/" + file);
                FileInputStream fis = afd.createInputStream();
                FileChannel fic = fis.getChannel();
                FileOutputStream fos = new FileOutputStream("/sdcard/" + file);
                FileChannel foc = fos.getChannel();
                fic.transferTo(0, fic.size(), foc);
                fic.close();
                foc.close();
            }
            Configuration gconf = new JavaPropertiesConfiguration(props);
            Configuration bconf = gconf.subset("boot");
            String kclass_name = bconf.getString("kernel");
            Log.d("bootKernel", "Attempting to load kernel from class '" + kclass_name + "'...");
            Class<? extends Kernel> kclass = Class.forName(kclass_name).asSubclass(Kernel.class);
            Kernel kernel = kclass.newInstance();
            Log.d("bootKernel", "Kernel loaded, proceeding with boot...");
            BootContext bctx = new SimpleBootContext(gconf, AndroidBridgeService.class, AndroidBridgeServiceImpl.class);
            kernel.boot(bctx).get();
            Log.d("bootKernel", "Kernel boot complete.");
        } catch (Exception e) {
            Log.e("bootKernel", "Unable to boot kernel due to exception.", e);
            finish();
        }
    }

    private static void scrollToBottom(TextView in_oTextView) {
        int l_nLineCount = in_oTextView.getLineCount();
        int l_nViewHeight = in_oTextView.getHeight();
        int l_nPixelsPerLine = in_oTextView.getLineHeight();
        int l_nDifference = (l_nLineCount * l_nPixelsPerLine) - l_nViewHeight;
        if (l_nDifference < 1) {
            return;
        }
        in_oTextView.scrollTo(0, l_nDifference);
    }
}
