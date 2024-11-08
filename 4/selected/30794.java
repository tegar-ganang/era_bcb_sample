package net.sf.andpdf.pdfviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.pdfviewer.gui.FullScrollView;
import net.sf.andpdf.refs.HardReference;
import net.sf.andpdf.refs.WeakReference;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.decrypt.PDFAuthenticationFailureException;
import com.sun.pdfview.decrypt.PDFPassword;
import com.sun.pdfview.font.PDFFont;

/**
 * U:\Android\android-sdk-windows-1.5_r1\tools\adb push u:\Android\simple_T.pdf /data/test.pdf
 * @author ferenc.hechler
 */
public class PdfViewerActivity extends Activity {

    private static final int STARTPAGE = 1;

    private static final float STARTZOOM = 1.0f;

    private static final String TAG = "PDFVIEWER";

    private static final int MEN_NEXT_PAGE = 1;

    private static final int MEN_PREV_PAGE = 2;

    private static final int MEN_GOTO_PAGE = 3;

    private static final int MEN_ZOOM_IN = 4;

    private static final int MEN_ZOOM_OUT = 5;

    private static final int MEN_BACK = 6;

    private static final int MEN_CLEANUP = 7;

    private static final int DIALOG_PAGENUM = 1;

    private GraphView mOldGraphView;

    private GraphView mGraphView;

    private String pdffilename;

    private PDFFile mPdfFile;

    private int mPage;

    private float mZoom;

    private File mTmpFile;

    private PDFPage mPdfPage;

    private Thread backgroundThread;

    private Handler uiHandler;

    @Override
    public Object onRetainNonConfigurationInstance() {
        Log.e(TAG, "onRetainNonConfigurationInstance");
        return this;
    }

    /**
	 * restore member variables from previously saved instance
	 * @see onRetainNonConfigurationInstance
	 * @return true if instance to restore from was found
	 */
    private boolean restoreInstance() {
        mOldGraphView = null;
        Log.e(TAG, "restoreInstance");
        if (getLastNonConfigurationInstance() == null) return false;
        PdfViewerActivity inst = (PdfViewerActivity) getLastNonConfigurationInstance();
        if (inst != this) {
            Log.e(TAG, "restoring Instance");
            mOldGraphView = inst.mGraphView;
            mPage = inst.mPage;
            mPdfFile = inst.mPdfFile;
            mPdfPage = inst.mPdfPage;
            mTmpFile = inst.mTmpFile;
            mZoom = inst.mZoom;
            pdffilename = inst.pdffilename;
            backgroundThread = inst.backgroundThread;
        }
        return true;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHandler = new Handler();
        restoreInstance();
        if (mOldGraphView != null) {
            mGraphView = new GraphView(this);
            mGraphView.fileMillis = mOldGraphView.fileMillis;
            mGraphView.mBi = mOldGraphView.mBi;
            mGraphView.mLine1 = mOldGraphView.mLine1;
            mGraphView.mLine2 = mOldGraphView.mLine2;
            mGraphView.mLine3 = mOldGraphView.mLine3;
            mGraphView.mText = mOldGraphView.mText;
            mGraphView.pageParseMillis = mOldGraphView.pageParseMillis;
            mGraphView.pageRenderMillis = mOldGraphView.pageRenderMillis;
            mOldGraphView = null;
            mGraphView.mImageView.setImageBitmap(mGraphView.mBi);
            mGraphView.updateTexts();
            setContentView(mGraphView);
        } else {
            mGraphView = new GraphView(this);
            Intent intent = getIntent();
            Log.i(TAG, "" + intent);
            boolean showImages = getIntent().getBooleanExtra(PdfFileSelectActivity.EXTRA_SHOWIMAGES, PdfFileSelectActivity.DEFAULTSHOWIMAGES);
            PDFImage.sShowImages = showImages;
            boolean antiAlias = getIntent().getBooleanExtra(PdfFileSelectActivity.EXTRA_ANTIALIAS, PdfFileSelectActivity.DEFAULTANTIALIAS);
            PDFPaint.s_doAntiAlias = antiAlias;
            boolean useFontSubstitution = getIntent().getBooleanExtra(PdfFileSelectActivity.EXTRA_USEFONTSUBSTITUTION, PdfFileSelectActivity.DEFAULTUSEFONTSUBSTITUTION);
            PDFFont.sUseFontSubstitution = useFontSubstitution;
            boolean keepCaches = getIntent().getBooleanExtra(PdfFileSelectActivity.EXTRA_KEEPCACHES, PdfFileSelectActivity.DEFAULTKEEPCACHES);
            HardReference.sKeepCaches = keepCaches;
            if (intent != null) {
                if ("android.intent.action.VIEW".equals(intent.getAction())) {
                    pdffilename = storeUriContentToFile(intent.getData());
                } else {
                    pdffilename = getIntent().getStringExtra(PdfFileSelectActivity.EXTRA_PDFFILENAME);
                }
            }
            if (pdffilename == null) pdffilename = "no file selected";
            mPage = STARTPAGE;
            mZoom = STARTZOOM;
            setContent(null);
        }
    }

    private void setContent(String password) {
        try {
            parsePDF(pdffilename, password);
            setContentView(mGraphView);
            startRenderThread(mPage, mZoom);
        } catch (PDFAuthenticationFailureException e) {
            setContentView(R.layout.pdf_file_password);
            final EditText etPW = (EditText) findViewById(R.id.etPassword);
            Button btOK = (Button) findViewById(R.id.btOK);
            Button btExit = (Button) findViewById(R.id.btExit);
            btOK.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    String pw = etPW.getText().toString();
                    setContent(pw);
                }
            });
            btExit.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    private synchronized void startRenderThread(final int page, final float zoom) {
        if (backgroundThread != null) return;
        mGraphView.showText("reading page " + page + ", zoom:" + zoom);
        backgroundThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (mPdfFile != null) {
                        showPage(page, zoom);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                backgroundThread = null;
            }
        });
        updateImageStatus();
        backgroundThread.start();
    }

    private void updateImageStatus() {
        if (backgroundThread == null) {
            mGraphView.updateUi();
            return;
        }
        mGraphView.updateUi();
        mGraphView.postDelayed(new Runnable() {

            @Override
            public void run() {
                updateImageStatus();
            }
        }, 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MEN_PREV_PAGE, Menu.NONE, "Previous Page");
        menu.add(Menu.NONE, MEN_NEXT_PAGE, Menu.NONE, "Next Page");
        menu.add(Menu.NONE, MEN_GOTO_PAGE, Menu.NONE, "Goto Page");
        menu.add(Menu.NONE, MEN_ZOOM_OUT, Menu.NONE, "Zoom Out");
        menu.add(Menu.NONE, MEN_ZOOM_IN, Menu.NONE, "Zoom In");
        menu.add(Menu.NONE, MEN_BACK, Menu.NONE, "Back");
        if (HardReference.sKeepCaches) menu.add(Menu.NONE, MEN_CLEANUP, Menu.NONE, "Clear Caches");
        return true;
    }

    /**
     * Called when a menu item is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()) {
            case MEN_NEXT_PAGE:
                {
                    nextPage();
                    break;
                }
            case MEN_PREV_PAGE:
                {
                    prevPage();
                    break;
                }
            case MEN_GOTO_PAGE:
                {
                    gotoPage();
                    break;
                }
            case MEN_ZOOM_IN:
                {
                    zoomIn();
                    break;
                }
            case MEN_ZOOM_OUT:
                {
                    zoomOut();
                    break;
                }
            case MEN_BACK:
                {
                    finish();
                    break;
                }
            case MEN_CLEANUP:
                {
                    HardReference.cleanup();
                    break;
                }
        }
        return true;
    }

    private void zoomIn() {
        if (mPdfFile != null) {
            if (mZoom < 4) {
                mZoom *= 1.5;
                if (mZoom > 4) mZoom = 4;
                startRenderThread(mPage, mZoom);
            }
        }
    }

    private void zoomOut() {
        if (mPdfFile != null) {
            if (mZoom > 0.25) {
                mZoom /= 1.5;
                if (mZoom < 0.25) mZoom = 0.25f;
                startRenderThread(mPage, mZoom);
            }
        }
    }

    private void nextPage() {
        if (mPdfFile != null) {
            if (mPage < mPdfFile.getNumPages()) {
                mPage += 1;
                startRenderThread(mPage, mZoom);
            }
        }
    }

    private void prevPage() {
        if (mPdfFile != null) {
            if (mPage > 1) {
                mPage -= 1;
                startRenderThread(mPage, mZoom);
            }
        }
    }

    private void gotoPage() {
        if (mPdfFile != null) {
            showDialog(DIALOG_PAGENUM);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_PAGENUM:
                LayoutInflater factory = LayoutInflater.from(this);
                final View pagenumView = factory.inflate(R.layout.dialog_pagenumber, null);
                final EditText edPagenum = (EditText) pagenumView.findViewById(R.id.pagenum_edit);
                edPagenum.setText(Integer.toString(mPage));
                return new AlertDialog.Builder(this).setIcon(R.drawable.icon).setTitle("Jump to page").setView(pagenumView).setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        String strPagenum = edPagenum.getText().toString();
                        int pageNum = mPage;
                        try {
                            pageNum = Integer.parseInt(strPagenum);
                        } catch (NumberFormatException ignore) {
                        }
                        if ((pageNum != mPage) && (pageNum >= 1) && (pageNum <= mPdfFile.getNumPages())) {
                            mPage = pageNum;
                            startRenderThread(mPage, mZoom);
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
        }
        return null;
    }

    private class GraphView extends FullScrollView {

        private String mText;

        private long fileMillis;

        private long pageParseMillis;

        private long pageRenderMillis;

        private Bitmap mBi;

        private String mLine1;

        private String mLine2;

        private String mLine3;

        private ImageView mImageView;

        private TextView mLine1View;

        private TextView mLine2View;

        private TextView mLine3View;

        private Button mBtPage;

        private Button mBtPage2;

        public GraphView(Context context) {
            super(context);
            LinearLayout.LayoutParams lpWrap1 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
            LinearLayout.LayoutParams lpWrap10 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 10);
            LinearLayout vl = new LinearLayout(context);
            vl.setLayoutParams(lpWrap10);
            vl.setOrientation(LinearLayout.VERTICAL);
            mLine1 = "PDF Viewer initializing";
            mLine1View = new TextView(context);
            mLine1View.setLayoutParams(lpWrap1);
            mLine1View.setText(mLine1);
            mLine1View.setTextColor(Color.BLACK);
            vl.addView(mLine1View);
            mLine2 = "unknown number of pages";
            mLine2View = new TextView(context);
            mLine2View.setLayoutParams(lpWrap1);
            mLine2View.setText(mLine2);
            mLine2View.setTextColor(Color.BLACK);
            vl.addView(mLine2View);
            mLine3 = "unknown timestamps";
            mLine3View = new TextView(context);
            mLine3View.setLayoutParams(lpWrap1);
            mLine3View.setText(mLine3);
            mLine3View.setTextColor(Color.BLACK);
            vl.addView(mLine3View);
            addNavButtons(vl);
            mBtPage2 = mBtPage;
            mImageView = new ImageView(context);
            setPageBitmap(null);
            updateImage();
            mImageView.setLayoutParams(lpWrap1);
            mImageView.setPadding(5, 5, 5, 5);
            vl.addView(mImageView);
            addNavButtons(vl);
            setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 100));
            setBackgroundColor(Color.LTGRAY);
            setHorizontalScrollBarEnabled(true);
            setHorizontalFadingEdgeEnabled(true);
            setVerticalScrollBarEnabled(true);
            setVerticalFadingEdgeEnabled(true);
            addView(vl);
        }

        private void addNavButtons(ViewGroup vg) {
            addSpace(vg, 6, 6);
            LinearLayout.LayoutParams lpChild1 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
            LinearLayout.LayoutParams lpWrap10 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 10);
            Context context = vg.getContext();
            LinearLayout hl = new LinearLayout(context);
            hl.setLayoutParams(lpWrap10);
            hl.setOrientation(LinearLayout.HORIZONTAL);
            Button bZoomOut = new Button(context);
            bZoomOut.setLayoutParams(lpChild1);
            bZoomOut.setText("-");
            bZoomOut.setWidth(40);
            bZoomOut.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    zoomOut();
                }
            });
            hl.addView(bZoomOut);
            Button bZoomIn = new Button(context);
            bZoomIn.setLayoutParams(lpChild1);
            bZoomIn.setText("+");
            bZoomIn.setWidth(40);
            bZoomIn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    zoomIn();
                }
            });
            hl.addView(bZoomIn);
            addSpace(hl, 6, 6);
            Button bPrev = new Button(context);
            bPrev.setLayoutParams(lpChild1);
            bPrev.setText("<");
            bPrev.setWidth(40);
            bPrev.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    prevPage();
                }
            });
            hl.addView(bPrev);
            mBtPage = new Button(context);
            mBtPage.setLayoutParams(lpChild1);
            String maxPage = ((mPdfFile == null) ? "?" : Integer.toString(mPdfFile.getNumPages()));
            mBtPage.setText(mPage + "/" + maxPage);
            mBtPage.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    gotoPage();
                }
            });
            hl.addView(mBtPage);
            Button bNext = new Button(context);
            bNext.setLayoutParams(lpChild1);
            bNext.setText(">");
            bNext.setWidth(40);
            bNext.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    nextPage();
                }
            });
            hl.addView(bNext);
            addSpace(hl, 20, 20);
            Button bExit = new Button(context);
            bExit.setLayoutParams(lpChild1);
            bExit.setText("Back");
            bExit.setWidth(60);
            bExit.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            hl.addView(bExit);
            vg.addView(hl);
            addSpace(vg, 6, 6);
        }

        private void addSpace(ViewGroup vg, int width, int height) {
            TextView tvSpacer = new TextView(vg.getContext());
            tvSpacer.setLayoutParams(new LinearLayout.LayoutParams(width, height, 1));
            tvSpacer.setText("");
            vg.addView(tvSpacer);
        }

        private void showText(String text) {
            Log.i(TAG, "ST='" + text + "'");
            mText = text;
            updateUi();
        }

        private void updateUi() {
            uiHandler.post(new Runnable() {

                @Override
                public void run() {
                    updateTexts();
                }
            });
        }

        private void updateImage() {
            uiHandler.post(new Runnable() {

                @Override
                public void run() {
                    mImageView.setImageBitmap(mBi);
                }
            });
        }

        private void setPageBitmap(Bitmap bi) {
            if (bi != null) mBi = bi; else {
                mBi = Bitmap.createBitmap(100, 100, Config.RGB_565);
                Canvas can = new Canvas(mBi);
                can.drawColor(Color.RED);
                Paint paint = new Paint();
                paint.setColor(Color.BLUE);
                can.drawCircle(50, 50, 50, paint);
                paint.setStrokeWidth(0);
                paint.setColor(Color.BLACK);
                can.drawText("Bitmap", 10, 50, paint);
            }
        }

        protected void updateTexts() {
            mLine1 = "PdfViewer: " + mText;
            float fileTime = fileMillis * 0.001f;
            float pageRenderTime = pageRenderMillis * 0.001f;
            float pageParseTime = pageParseMillis * 0.001f;
            mLine2 = "render page=" + format(pageRenderTime, 2) + ", parse page=" + format(pageParseTime, 2) + ", parse file=" + format(fileTime, 2);
            int maxCmds = PDFPage.getParsedCommands();
            int curCmd = PDFPage.getLastRenderedCommand() + 1;
            mLine3 = "PDF-Commands: " + curCmd + "/" + maxCmds;
            mLine1View.setText(mLine1);
            mLine2View.setText(mLine2);
            mLine3View.setText(mLine3);
            if (mPdfPage != null) {
                if (mBtPage != null) mBtPage.setText(mPdfPage.getPageNumber() + "/" + mPdfFile.getNumPages());
                if (mBtPage2 != null) mBtPage2.setText(mPdfPage.getPageNumber() + "/" + mPdfFile.getNumPages());
            }
        }

        private String format(double value, int num) {
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setGroupingUsed(false);
            nf.setMaximumFractionDigits(num);
            String result = nf.format(value);
            return result;
        }
    }

    private void showPage(int page, float zoom) throws Exception {
        long startTime = System.currentTimeMillis();
        long middleTime = startTime;
        try {
            mGraphView.setPageBitmap(null);
            mGraphView.updateImage();
            mPdfPage = mPdfFile.getPage(page, true);
            int num = mPdfPage.getPageNumber();
            int maxNum = mPdfFile.getNumPages();
            float wi = mPdfPage.getWidth();
            float hei = mPdfPage.getHeight();
            String pageInfo = new File(pdffilename).getName() + " - " + num + "/" + maxNum + ": " + wi + "x" + hei;
            mGraphView.showText(pageInfo);
            Log.i(TAG, pageInfo);
            RectF clip = null;
            middleTime = System.currentTimeMillis();
            Bitmap bi = mPdfPage.getImage((int) (wi * zoom), (int) (hei * zoom), clip, true, true);
            mGraphView.setPageBitmap(bi);
            mGraphView.updateImage();
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
            mGraphView.showText("Exception: " + e.getMessage());
        }
        long stopTime = System.currentTimeMillis();
        mGraphView.pageParseMillis = middleTime - startTime;
        mGraphView.pageRenderMillis = stopTime - middleTime;
    }

    private void parsePDF(String filename, String password) throws PDFAuthenticationFailureException {
        long startTime = System.currentTimeMillis();
        try {
            File f = new File(filename);
            long len = f.length();
            if (len == 0) {
                mGraphView.showText("file '" + filename + "' not found");
            } else {
                mGraphView.showText("file '" + filename + "' has " + len + " bytes");
                openFile(f, password);
            }
        } catch (PDFAuthenticationFailureException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            mGraphView.showText("Exception: " + e.getMessage());
        }
        long stopTime = System.currentTimeMillis();
        mGraphView.fileMillis = stopTime - startTime;
    }

    /**
     * <p>Open a specific pdf file.  Creates a DocumentInfo from the file,
     * and opens that.</p>
     *
     * <p><b>Note:</b> Mapping the file locks the file until the PDFFile
     * is closed.</p>
     *
     * @param file the file to open
     * @throws IOException
     */
    public void openFile(File file, String password) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer bb = ByteBuffer.NEW(channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()));
        if (password == null) mPdfFile = new PDFFile(bb); else mPdfFile = new PDFFile(bb, new PDFPassword(password));
        mGraphView.showText("Anzahl Seiten:" + mPdfFile.getNumPages());
    }

    private byte[] readBytes(File srcFile) throws IOException {
        long fileLength = srcFile.length();
        int len = (int) fileLength;
        byte[] result = new byte[len];
        FileInputStream fis = new FileInputStream(srcFile);
        int pos = 0;
        int cnt = fis.read(result, pos, len - pos);
        while (cnt > 0) {
            pos += cnt;
            cnt = fis.read(result, pos, len - pos);
        }
        return result;
    }

    private String storeUriContentToFile(Uri uri) {
        String result = null;
        try {
            if (mTmpFile == null) {
                File root = Environment.getExternalStorageDirectory();
                if (root == null) throw new Exception("external storage dir not found");
                mTmpFile = new File(root, "AndroidPdfViewer/AndroidPdfViewer_temp.pdf");
                mTmpFile.getParentFile().mkdirs();
                mTmpFile.delete();
            } else {
                mTmpFile.delete();
            }
            InputStream is = getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(mTmpFile);
            byte[] buf = new byte[1024];
            int cnt = is.read(buf);
            while (cnt > 0) {
                os.write(buf, 0, cnt);
                cnt = is.read(buf);
            }
            os.close();
            is.close();
            result = mTmpFile.getCanonicalPath();
            mTmpFile.deleteOnExit();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTmpFile != null) {
            mTmpFile.delete();
            mTmpFile = null;
        }
    }
}
