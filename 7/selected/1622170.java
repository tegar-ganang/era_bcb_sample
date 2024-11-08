package org.designerator.media.image.filter.color;

import org.designerator.common.data.ProcessorData;
import org.designerator.common.data.SegmentImageData;
import org.designerator.common.interfaces.IImageEditor;
import org.designerator.media.image.filter.Processor;
import org.designerator.media.image.filter.util.FilterUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

public class GreyScale extends Processor {

    static final float rf = 0.299f;

    static final float gf = 0.587f;

    static final float bf = 0.114f;

    static final float rf1 = 0.299f;

    static final float gf1 = 0.587f;

    static final float bf1 = 0.114f;

    static final float lumR = 0.3086f;

    static final float lumG = 0.6094f;

    static final float lumB = 0.0820f;

    ;

    public static final int[] redGrey = FilterUtils.fillLUT(rf);

    public static final int[] greenGrey = FilterUtils.fillLUT(gf);

    public static final int[] blueGrey = FilterUtils.fillLUT(bf);

    static final int[] rgbLut;

    public static final String Name = "Greyscale";

    public static final int Auto = 1;

    public static boolean runGrey(SegmentImageData sourceData, final byte[] dest) {
        if (sourceData == null || sourceData.data == null) {
            SWT.error(SWT.ERROR_CANNOT_BE_ZERO);
            return false;
        }
        if (!(sourceData.bpp == 3 || sourceData.bpp == 4)) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            return false;
        }
        int scanLineEnd = -1;
        final int step = sourceData.bpp;
        if (sourceData.pad != 0) {
            scanLineEnd = sourceData.start + sourceData.scanLineLength - step;
        }
        final int gs = sourceData.greenShift;
        final int bs = sourceData.blueShift;
        final int rs = sourceData.redShift;
        int r, g, b, gray;
        final byte[] source = sourceData.data;
        final int length = sourceData.length - sourceData.pad;
        for (int i = sourceData.start; i < length; i += step) {
            b = source[i + rs] & 0xff;
            g = source[i + gs] & 0xff;
            r = source[i + bs] & 0xff;
            gray = ((greenGrey[(b & 0xff)]) + (blueGrey[(g & 0xff)]) + (redGrey[(r & 0xff)]));
            gray = gray > 255 ? 255 : gray;
            dest[i] = dest[i + 1] = dest[i + 2] = (byte) gray;
            if (i == scanLineEnd) {
                scanLineEnd += sourceData.scanLineLength + sourceData.pad;
                i += sourceData.pad;
            }
        }
        return true;
    }

    public static boolean runTo8BitGrey(SegmentImageData sourceData, final byte[] dest) {
        if (sourceData == null || sourceData.data == null) {
            SWT.error(SWT.ERROR_CANNOT_BE_ZERO);
            return false;
        }
        if ((sourceData.height * sourceData.width != dest.length)) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            return false;
        }
        if (!(sourceData.bpp == 3 || sourceData.bpp == 4)) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            return false;
        }
        int scanLineEnd = -1;
        final int step = sourceData.bpp;
        if (sourceData.pad != 0) {
            scanLineEnd = sourceData.start + sourceData.scanLineLength - step;
        }
        final int gs = sourceData.greenShift;
        final int bs = sourceData.blueShift;
        final int rs = sourceData.redShift;
        int r, g, b, gray;
        final byte[] source = sourceData.data;
        final int length = sourceData.length - sourceData.pad;
        int j = 0;
        for (int i = sourceData.start; i < length; i += step) {
            b = source[i + rs] & 0xff;
            g = source[i + gs] & 0xff;
            r = source[i + bs] & 0xff;
            gray = ((rgbLut[(b & 0xff)]) + (rgbLut[(g & 0xff)]) + (rgbLut[(r & 0xff)]));
            gray = gray > 255 ? 255 : gray;
            dest[j++] = (byte) gray;
            if (i == scanLineEnd) {
                scanLineEnd += sourceData.scanLineLength + sourceData.pad;
                i += sourceData.pad;
            }
        }
        return true;
    }

    private ImageData iData;

    private IImageEditor editor;

    private byte[] dest;

    static {
        rgbLut = FilterUtils.fillLUT(1.0f);
    }

    public GreyScale() {
    }

    @Override
    public void dispose() {
        src = null;
        dest = null;
        iData = null;
    }

    public ProcessorData[] getCurrentProcessorData() {
        ProcessorData[] params = new ProcessorData[1];
        ProcessorData pd = new ProcessorData();
        pd.name = Name;
        pd.selection = 1;
        pd.index = 0;
        params[0] = pd;
        return params;
    }

    public ImageData getOrignalImageData() {
        return iData;
    }

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public void init(IImageEditor editor, boolean previewData, boolean deleteCache) {
        if (editor == null) {
            return;
        }
        editor.setProcessor(this);
        this.editor = editor;
        iData = editor.getImageData(true, false);
        dest = new byte[iData.data.length];
        setTimer(true);
        init();
    }

    public byte[] processInThread() {
        runGrey(getSimpleIData(iData), dest);
        return dest;
    }

    public void run() {
        Display display = editor.getDisplay();
        runGrey(getLoopIData(), dest);
        if (hasrun()) {
            display.syncExec(new Runnable() {

                public void run() {
                    iData.data = dest;
                    editor.processFinnished(iData, true);
                    editor.updateMessage(Name + ": " + getRunTime(), true);
                }
            });
        }
    }

    public void sendMessage(int amount) {
        String message = Name + ": " + amount + "% in " + getRunTime() + "ms";
        editor.updateMessage(message, true);
    }

    public boolean updatePixels(ProcessorData[] params) {
        if (isRunning() || params == null) {
            return false;
        }
        return true;
    }
}
