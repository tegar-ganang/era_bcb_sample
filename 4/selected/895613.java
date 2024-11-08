package au.edu.jcu.v4l4j.examples;

import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.ImageFormatList;
import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

/**
 * Objects of this class compute the maximum achievable frame rate for a video
 * device, and print it.
 * @author gilles
 *
 */
public class GetFrameRate implements CaptureCallback {

    public static final int captureLength = 10;

    private static String dev;

    private static int inFmt, outFmt, width, height, channel, std, intv;

    private FrameGrabber fg;

    private VideoDevice vd;

    private ImageFormatList imfList;

    private int numFrames;

    private long startTime, currentTime;

    private boolean seenFirstFrame;

    public static void main(String[] args) throws Exception {
        dev = (System.getProperty("test.device") != null) ? System.getProperty("test.device") : "/dev/video0";
        width = (System.getProperty("test.width") != null) ? Integer.parseInt(System.getProperty("test.width")) : 640;
        height = (System.getProperty("test.height") != null) ? Integer.parseInt(System.getProperty("test.height")) : 480;
        std = (System.getProperty("test.standard") != null) ? Integer.parseInt(System.getProperty("test.standard")) : V4L4JConstants.STANDARD_WEBCAM;
        channel = (System.getProperty("test.channel") != null) ? Integer.parseInt(System.getProperty("test.channel")) : 0;
        inFmt = (System.getProperty("test.inFormat") != null) ? Integer.parseInt(System.getProperty("test.inFormat")) : -1;
        outFmt = (System.getProperty("test.outFormat") != null) ? Integer.parseInt(System.getProperty("test.outFormat")) : 0;
        intv = (System.getProperty("test.fps") != null) ? Integer.parseInt(System.getProperty("test.fps")) : -1;
        System.out.println("This program will open " + dev + ", capture frames for " + captureLength + " seconds and print the FPS");
        new GetFrameRate().startTest();
    }

    /**
	 * This method builds a new object to test the maximum FPS for a video 
	 * device
	 * @throws V4L4JException if there is an error initialising the video device
	 */
    public GetFrameRate() throws V4L4JException {
        try {
            vd = new VideoDevice(dev);
            imfList = vd.getDeviceInfo().getFormatList();
            if (outFmt == 0) getRawFg(); else if (outFmt == 1 && vd.supportJPEGConversion()) getJPEGfg(); else if (outFmt == 2 && vd.supportRGBConversion()) getRGBfg(); else if (outFmt == 3 && vd.supportBGRConversion()) getBGRfg(); else if (outFmt == 4 && vd.supportYUVConversion()) getYUVfg(); else if (outFmt == 5 && vd.supportYVUConversion()) getYVUfg(); else {
                System.out.println("Unknown / unsupported output format: " + outFmt);
                throw new V4L4JException("unknown / unsupported output format");
            }
            System.out.println("Input image format: " + fg.getImageFormat().getName());
        } catch (V4L4JException e) {
            e.printStackTrace();
            System.out.println("Failed to instanciate the FrameGrabber (" + dev + ")");
            vd.release();
            throw e;
        }
        width = fg.getWidth();
        height = fg.getHeight();
        std = fg.getStandard();
        channel = fg.getChannel();
    }

    private void getJPEGfg() throws V4L4JException {
        if (inFmt == -1 || imfList.getJPEGEncodableFormat(inFmt) == null) {
            System.out.println("Invalid format / no capture format " + "specified, let v4l4j find a suitable one");
            fg = vd.getJPEGFrameGrabber(width, height, channel, std, 80);
        } else {
            System.out.println("Trying input format " + imfList.getJPEGEncodableFormat(inFmt).getName());
            fg = vd.getJPEGFrameGrabber(width, height, channel, std, 80, imfList.getJPEGEncodableFormat(inFmt));
        }
        System.out.println("Output image format: JPEG");
    }

    private void getRGBfg() throws V4L4JException {
        if (inFmt == -1 || imfList.getRGBEncodableFormat(inFmt) == null) {
            System.out.println("Invalid format / no capture format " + "specified, let v4l4j find a suitable one");
            fg = vd.getRGBFrameGrabber(width, height, channel, std);
        } else {
            System.out.println("Trying input format " + imfList.getRGBEncodableFormat(inFmt).getName());
            fg = vd.getRGBFrameGrabber(width, height, channel, std, imfList.getRGBEncodableFormat(inFmt));
        }
        System.out.println("Output image format: RGB");
    }

    private void getBGRfg() throws V4L4JException {
        if (inFmt == -1 || imfList.getBGREncodableFormat(inFmt) == null) {
            System.out.println("Invalid format / no capture format " + "specified, let v4l4j find a suitable one");
            fg = vd.getBGRFrameGrabber(width, height, channel, std);
        } else {
            System.out.println("Trying input format " + imfList.getBGREncodableFormat(inFmt).getName());
            fg = vd.getBGRFrameGrabber(width, height, channel, std, imfList.getBGREncodableFormat(inFmt));
        }
        System.out.println("Output image format: BGR");
    }

    private void getYUVfg() throws V4L4JException {
        if (inFmt == -1 || imfList.getYUVEncodableFormat(inFmt) == null) {
            System.out.println("Invalid format / no capture format " + "specified, let v4l4j find a suitable one");
            fg = vd.getYUVFrameGrabber(width, height, channel, std);
        } else {
            System.out.println("Trying input format " + imfList.getYUVEncodableFormat(inFmt).getName());
            fg = vd.getYUVFrameGrabber(width, height, channel, std, imfList.getYUVEncodableFormat(inFmt));
        }
        System.out.println("Output image format: YUV");
    }

    private void getYVUfg() throws V4L4JException {
        if (inFmt == -1 || imfList.getYVUEncodableFormat(inFmt) == null) {
            System.out.println("Invalid format / no capture format " + "specified, let v4l4j find a suitable one");
            fg = vd.getYVUFrameGrabber(width, height, channel, std);
        } else {
            System.out.println("Trying input format " + imfList.getYVUEncodableFormat(inFmt).getName());
            fg = vd.getYVUFrameGrabber(width, height, channel, std, imfList.getYVUEncodableFormat(inFmt));
        }
        System.out.println("Output image format: YVU");
    }

    private void getRawFg() throws V4L4JException {
        if (inFmt == -1 || imfList.getNativeFormat(inFmt) == null) {
            System.out.println("Invalid format / no capture format " + "specified, v4l4j will pick the first one");
            fg = vd.getRawFrameGrabber(width, height, channel, std);
        } else {
            System.out.println("Trying input format " + imfList.getNativeFormat(inFmt).getName());
            fg = vd.getRawFrameGrabber(width, height, channel, std, imfList.getNativeFormat(inFmt));
        }
        System.out.println("Output image format: RAW (same as input)");
    }

    private void startCapture() throws V4L4JException {
        if (intv != -1) {
            try {
                System.out.println("setting frame rate to " + intv);
                fg.setFrameInterval(1, intv);
            } catch (Exception e) {
                System.out.println("Couldnt set the frame interval");
            }
        }
        fg.setCaptureCallback(this);
        try {
            fg.startCapture();
        } catch (V4L4JException e) {
            e.printStackTrace();
            System.out.println("Failed to start capture");
            vd.releaseFrameGrabber();
            vd.release();
            throw e;
        }
    }

    /**
	 * This method starts the frame rate test. It will run for 10 seconds, and
	 * then print the achieved FPS
	 * @throws V4L4JException if there is an error capturing frames
	 * @throws InterruptedException 
	 */
    public void startTest() throws V4L4JException, InterruptedException {
        startTime = 0;
        currentTime = 0;
        numFrames = 0;
        seenFirstFrame = false;
        System.out.println("Starting test capture at " + width + "x" + height + " for " + captureLength + " seconds");
        startCapture();
        synchronized (this) {
            wait();
        }
        fg.stopCapture();
        System.out.println("End time: " + currentTime);
        System.out.println(" =====  TEST RESULTS  =====");
        System.out.println("\tFrames captured :" + numFrames);
        System.out.println("\tFPS: " + ((float) numFrames / (currentTime / 1000 - startTime / 1000)));
        System.out.println(" =====  END  RESULTS  =====");
        vd.releaseFrameGrabber();
        vd.release();
    }

    @Override
    public void nextFrame(VideoFrame frame) {
        if (!seenFirstFrame) {
            startTime = System.currentTimeMillis();
        } else {
            numFrames++;
            currentTime = System.currentTimeMillis();
            if (currentTime >= startTime + (captureLength * 1000)) {
                synchronized (this) {
                    notify();
                }
            }
        }
        seenFirstFrame = true;
        frame.recycle();
    }

    @Override
    public void exceptionReceived(V4L4JException e) {
        e.printStackTrace();
        System.out.println("Failed to perform test capture");
        synchronized (this) {
            notify();
        }
    }
}
