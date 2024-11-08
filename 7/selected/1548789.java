package org.designerator.media.image.editor.filter;

import org.designerator.common.data.HistogramData;
import org.designerator.common.data.ProcessorData;
import org.designerator.common.data.SegmentImageData;
import org.designerator.common.interfaces.IImageEditor;
import org.designerator.common.interfaces.IRGBHistogram;
import org.designerator.media.image.filter.Processor;
import org.designerator.media.image.filter.util.FilterUtils;
import org.designerator.media.image.histogram.RGBHistogram;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;

public class GreyScale extends Processor {

    public static final String Name = "GrayScale";

    public static final String REDID = "Red";

    public static final String GREENID = "Green";

    public static final String BLUEID = "Blue";

    public static boolean runGrey(SegmentImageData sourceData, final byte[] dest, int[] greenGrey, int[] blueGrey, int[] redGrey, int[] bclut, IProgressMonitor monitor) {
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
        scanLineEnd = sourceData.start + sourceData.scanLineLength - step;
        final int bs = sourceData.blueShift;
        final int gs = sourceData.greenShift;
        final int rs = sourceData.redShift;
        int r, g, b, gray;
        final byte[] source = sourceData.data;
        final int length = sourceData.length - sourceData.pad;
        for (int i = sourceData.start; i < length; i += step) {
            b = bclut[source[i + bs] & 0xff];
            g = bclut[source[i + gs] & 0xff];
            r = bclut[source[i + rs] & 0xff];
            gray = ((greenGrey[g]) + (blueGrey[b]) + (redGrey[r]));
            gray = gray > 255 ? 255 : gray;
            dest[i] = dest[i + 1] = dest[i + 2] = (byte) gray;
            if (i == scanLineEnd) {
                scanLineEnd += sourceData.scanLineLength + sourceData.pad;
                i += sourceData.pad;
                if (monitor != null) {
                    monitor.worked(1);
                }
            }
        }
        return true;
    }

    private IImageEditor editor;

    private ImageData iData;

    private byte[] dest;

    private int[] redGrey;

    private int[] greenGrey;

    private int[] blueGrey;

    private int[] bclut;

    private int redRatio = 29;

    private int greenRatio = 60;

    private int blueRatio = 11;

    private float rgbmean = -1;

    private float brightness = 0f;

    private float contrast = 0f;

    public GreyScale() {
        super();
    }

    @Override
    public void dispose() {
        if (!isRunning()) {
            iData = null;
            dest = null;
            super.dispose();
        }
    }

    public ProcessorData[] getCurrentProcessorData() {
        ProcessorData[] params = new ProcessorData[5];
        ProcessorData redData = new ProcessorData();
        redData.name = REDID;
        redData.max = 100;
        redData.min = 0;
        redData.increment = 100;
        redData.index = 0;
        redData.delay = true;
        redData.selection = redRatio;
        redData.index = 0;
        params[0] = redData;
        ProcessorData greenData = new ProcessorData();
        greenData.name = GREENID;
        greenData.max = 100;
        greenData.min = 0;
        greenData.increment = 100;
        greenData.selection = greenRatio;
        greenData.index = 1;
        greenData.delay = true;
        params[1] = greenData;
        ProcessorData blueData = new ProcessorData();
        blueData.name = BLUEID;
        blueData.max = 100;
        blueData.min = 0;
        blueData.increment = 100;
        blueData.selection = blueRatio;
        blueData.index = 2;
        blueData.delay = true;
        params[2] = blueData;
        ProcessorData contrastData = new ProcessorData();
        contrastData.name = Contrast.CONTRAST;
        contrastData.max = 100;
        contrastData.min = -100;
        contrastData.increment = 100;
        contrastData.selection = getBCSelection(contrast);
        contrastData.index = 3;
        contrastData.delay = true;
        params[3] = contrastData;
        ProcessorData brightnessData = new ProcessorData();
        brightnessData.name = Contrast.BRIGHTNESS;
        brightnessData.max = 100;
        brightnessData.min = -100;
        brightnessData.increment = 100;
        brightnessData.selection = getBCSelection(brightness);
        brightnessData.index = 4;
        brightnessData.delay = true;
        params[4] = brightnessData;
        return params;
    }

    public int getBCSelection(float val) {
        return (int) (val * 100 + 0.5f);
    }

    public ImageData getOrignalImageData() {
        return iData;
    }

    public String getName() {
        return Name;
    }

    @Override
    public byte[] getResult() {
        return dest;
    }

    public void init(IImageEditor editor, boolean previewData, boolean deleteCache) {
        if (editor == null) {
            return;
        }
        this.editor = editor;
        iData = (editor.getImageData(previewData, deleteCache));
        try {
            dest = new byte[iData.data.length];
        } catch (java.lang.OutOfMemoryError e) {
            e.printStackTrace();
            return;
        }
        System.arraycopy(iData.data, 0, dest, 0, dest.length);
        setTimer(true);
        super.init(editor, previewData, deleteCache);
        initHistoGram();
    }

    private void initHistoGram() {
        final IRGBHistogram rgbhistogram = (IRGBHistogram) editor.getHistogram();
        if (rgbhistogram != null) {
            rgbmean = (((IRGBHistogram) editor.getHistogram()).getmean()) * 0.01f;
        }
        if (rgbmean == -1) {
            SegmentImageData sourceData = getSimpleIData(iData);
            HistogramData histoData = RGBHistogram.findHistogram(sourceData, null);
            rgbmean = RGBHistogram.countHistogram(histoData) / (iData.width * iData.height * 3);
        }
        if (rgbmean == -1) {
            SWT.error(SWT.ERROR_CANNOT_GET_COUNT);
            return;
        }
    }

    public void processDefault(IProgressMonitor monitor, boolean preview) {
        super.processDefault(monitor, preview);
        ProcessorData[] params = new ProcessorData[5];
        params[0] = new ProcessorData();
        params[0].name = REDID;
        params[0].selection = redRatio;
        params[1] = new ProcessorData();
        params[1].name = GREENID;
        params[1].selection = greenRatio;
        params[2] = new ProcessorData();
        params[2].name = BLUEID;
        params[2].selection = blueRatio;
        params[3] = new ProcessorData();
        params[3].name = Contrast.CONTRAST;
        params[3].selection = getBCSelection(contrast);
        params[4] = new ProcessorData();
        params[4].name = Contrast.BRIGHTNESS;
        params[4].selection = getBCSelection(brightness);
        if (!preview) {
            init(editor, false, false);
        }
        updatePixels(params);
        if (monitor != null) {
            monitor.beginTask(Name, getOrignalImageData().height);
        }
        process();
    }

    public void run(SegmentImageData segementData) {
        runGrey(segementData, dest, greenGrey, blueGrey, redGrey, bclut, getMonitor());
    }

    public boolean updatePixels(ProcessorData[] params) {
        if (isRunning() || params == null) {
            return false;
        }
        for (ProcessorData param : params) {
            if (param.name.equals(REDID)) {
                redRatio = param.selection;
            } else if (param.name.equals(GREENID)) {
                greenRatio = param.selection;
            } else if (param.name.equals(BLUEID)) {
                blueRatio = param.selection;
            } else if (param.name.equals(Contrast.CONTRAST)) {
                contrast = param.selection / 100.f;
            } else if (param.name.equals(Contrast.BRIGHTNESS)) {
                brightness = param.selection / 100.f;
            }
        }
        setUpLuts();
        setupBCLut(rgbmean);
        return true;
    }

    public void setUpLuts() {
        float factor = 100f / (redRatio + greenRatio + blueRatio);
        redGrey = FilterUtils.fillLUT(factor * redRatio / 100f);
        greenGrey = FilterUtils.fillLUT(factor * greenRatio / 100f);
        blueGrey = FilterUtils.fillLUT(factor * blueRatio / 100f);
    }

    public void setupBCLut(float mean) {
        if (mean < 0) {
            mean = 0;
        }
        if (brightness == 0 && contrast == 0) {
            bclut = FilterUtils.fillSimpleLUT(1.0f);
            return;
        }
        if (bclut == null) {
            bclut = new int[256];
        }
        for (int i = 0; i < 256; i++) {
            int x = (int) ((Contrast.getContrastBC((i / 255.0f), mean, brightness, contrast)) + 0.5f);
            bclut[i] = Math.min(Math.max(x, 0), 255);
        }
    }
}
