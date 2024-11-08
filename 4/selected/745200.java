package au.edu.jcu.v4l4j;

import java.util.Vector;
import au.edu.jcu.v4l4j.FrameInterval.DiscreteInterval;
import au.edu.jcu.v4l4j.exceptions.CaptureChannelException;
import au.edu.jcu.v4l4j.exceptions.ImageFormatException;
import au.edu.jcu.v4l4j.exceptions.InitialisationException;
import au.edu.jcu.v4l4j.exceptions.InvalidValue;
import au.edu.jcu.v4l4j.exceptions.NoTunerException;
import au.edu.jcu.v4l4j.exceptions.StateException;
import au.edu.jcu.v4l4j.exceptions.UnsupportedMethod;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;
import au.edu.jcu.v4l4j.exceptions.VideoStandardException;

/**
 * This abstract class implements the core functionalities found in all frame 
 * grabbers, initialisation, starting the capture, capturing frames, stopping
 * the capture and releasing resources. It must be subclassed.<br>
 * Subclasses must implement {@link FrameGrabber#getImageFormat()} to return the
 * correct image format used for capture. {@link #init()} may also be overridden
 * if required.
 * @author gilles 
 */
abstract class AbstractGrabber implements FrameGrabber {

    protected static final int RAW_GRABBER = 0;

    protected static final int JPEG_GRABBER = 1;

    protected static final int RGB24_GRABBER = 2;

    protected static final int BGR24_GRABBER = 3;

    protected static final int YUV_GRABBER = 4;

    protected static final int YVU_GRABBER = 5;

    protected DeviceInfo dInfo;

    private int width;

    private int height;

    private int channel;

    private int standard;

    protected int nbV4LBuffers;

    protected Vector<BaseVideoFrame> videoFrames;

    private Vector<BaseVideoFrame> availableVideoFrames;

    protected State state;

    protected int format;

    private Tuner tuner;

    private int type;

    private long lastCapturedFrameSequence;

    private long lastCapturedFrameTimeuSec;

    private int lastCapturedFrameBufferIndex;

    private PushSource pushSource;

    private long pushSourceThreadId;

    protected long object;

    static {
        try {
            System.loadLibrary("v4l4j");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Cant load v4l4j JNI library");
            throw e;
        }
    }

    private native int doInit(long o, int w, int h, int ch, int std, int requestedFormat, int output) throws V4L4JException;

    private native void start(long o) throws V4L4JException;

    /**
	 * This method sets a new value for the JPEG quality
	 * @param o the struct v4l4_device
	 * @param i the new value
	 * @throws V4L4JException if the JPEG quality is disabled because of the
	 * type of this frame grabber (not {@link #JPEG_GRABBER}).
	 */
    protected native void setQuality(long o, int i);

    private native int getBufferSize(long o);

    private native int enqueueBuffer(long o, int index);

    private native int fillBuffer(long o, byte array[]) throws V4L4JException;

    private native void stop(long o);

    private native void doRelease(long o);

    private native void doSetFrameIntv(long o, int n, int d) throws InvalidValue;

    private native int doGetFrameIntv(long o, int what);

    private native void doSetVideoInputNStandard(long o, int input, int std);

    private native int doGetVideoInput(long o);

    private native int doGetVideoStandard(long o);

    /**
	 * This constructor builds a FrameGrabber object used to capture frames from
	 * a video source.
	 * @param di the DeviceInfo object associated with the video device who 
	 * created this grabber
	 * @param o a JNI pointer to the v4l4j_device structure
	 * @param w the requested frame width 
	 * @param h the requested frame height
	 * @param ch the input index, as returned by 
	 * <code>InputInfo.getIndex()</code>
	 * @param std the video standard, as returned by 
	 * <code>InputInfo.getSupportedStandards()</code> (see V4L4JConstants)
	 * @param t the {@link Tuner} associated with this frame grabber or
	 * <code>null</code>.
	 * @param imf the image format frames should be captured in
	 * @param ty the output image format, ie the type of this frame grabber:
	 * {@link #RAW_GRABBER}, {@link #JPEG_GRABBER}, {@link #RGB24_GRABBER}, 
	 * {@link #BGR24_GRABBER}, {@link #YUV_GRABBER}, {@link #YVU_GRABBER}
	 * @throw {@link ImageFormatException} if the image format is null and 
	 * type = {@link #RAW_GRABBER}
	 */
    protected AbstractGrabber(DeviceInfo di, long o, int w, int h, int ch, int std, Tuner t, ImageFormat imf, int ty) throws ImageFormatException {
        if (imf == null) throw new ImageFormatException("The image format can not be null");
        state = new State();
        dInfo = di;
        object = o;
        width = w;
        height = h;
        channel = ch;
        standard = std;
        format = imf.getIndex();
        tuner = t;
        type = ty;
        nbV4LBuffers = 0;
        videoFrames = new Vector<BaseVideoFrame>();
        availableVideoFrames = new Vector<BaseVideoFrame>();
        pushSource = null;
        pushSourceThreadId = 0;
    }

    /**
	 * This method initialises the capture, and apply the capture parameters.
	 * V4L may either adjust the height and width parameters to the closest 
	 * valid values or reject them altogether. If the values were adjusted, 
	 * they can be retrieved after calling {@link #init()} using 
	 * {@link #getWidth()} and {@link #getHeight()}.
	 * @throws VideoStandardException if the chosen video standard is not 
	 * supported
	 * @throws ImageFormatException for a raw frame grabber, this exception is 
	 * thrown if the chosen Image format is unsupported.
	 * @throws CaptureChannelException if the given channel number value is not 
	 * valid
	 * @throws ImageDimensionException if the given image dimensions are not 
	 * supported
	 * @throws InitialisationException if the video device file can not be 
	 * initialised 
	 * @throws StateException if the frame grabber is already initialised
	 * @throws V4L4JException if there is an error applying capture parameters
	 */
    void init() throws V4L4JException {
        state.init();
        nbV4LBuffers = doInit(object, width, height, channel, standard, format, type);
        int bufferSize = getBufferSize(object);
        createBuffers(bufferSize);
        state.commit();
    }

    /**
	 * This abstract method is called when {@link #init()} succeeds and is
	 * responsible for populating the {@link #videoFrames} member (vector of 
	 * {@link #nbV4LBuffers}  {@link BaseVideoFrame}s).
	 * @param bufferSize the size of each buffer
	 */
    protected abstract void createBuffers(int bufferSize);

    @Override
    public int getNumberOfVideoFrames() {
        return nbV4LBuffers;
    }

    @Override
    public int getNumberOfRecycledVideoFrames() {
        synchronized (availableVideoFrames) {
            return availableVideoFrames.size();
        }
    }

    @Override
    public void setFrameInterval(int num, int denom) throws InvalidValue {
        synchronized (state) {
            if (state.isStarted()) throw new StateException("Invalid method call");
            doSetFrameIntv(object, num, denom);
        }
    }

    @Override
    public DiscreteInterval getFrameInterval() {
        synchronized (state) {
            if (state.isStarted()) throw new StateException("Invalid method call: cannot get the" + " frame interval while capturing.");
            return new DiscreteInterval(doGetFrameIntv(object, 0), doGetFrameIntv(object, 1));
        }
    }

    @Override
    public void setVideoInputNStandard(int input, int standard) throws VideoStandardException, CaptureChannelException {
        state.checkReleased();
        doSetVideoInputNStandard(object, input, standard);
    }

    @Override
    public int getVideoInput() {
        state.checkReleased();
        return doGetVideoInput(object);
    }

    @Override
    public int getVideoStandard() {
        state.checkReleased();
        return doGetVideoStandard(object);
    }

    @Override
    public final Tuner getTuner() throws NoTunerException {
        if (tuner == null) throw new NoTunerException("This input does not have a tuner");
        state.checkReleased();
        return tuner;
    }

    @Override
    public final void setCaptureCallback(CaptureCallback callback) {
        if (callback == null) throw new NullPointerException("The callback object cannot be null");
        synchronized (state) {
            if (state.isStarted()) throw new StateException("This frame grabber is already started");
            pushSource = new PushSource(this, callback);
        }
    }

    @Override
    public final void startCapture() throws V4L4JException {
        state.start();
        if (pushSource != null) {
            pushSourceThreadId = pushSource.startCapture();
            state.waitForAtLeastOneUser();
        }
        try {
            start(object);
        } catch (V4L4JException e) {
            if (pushSource != null) pushSource.stopCapture();
            state.rollback();
            throw e;
        }
        state.commit();
        synchronized (availableVideoFrames) {
            availableVideoFrames.addAll(videoFrames);
            availableVideoFrames.notifyAll();
        }
    }

    /**
	 * This method is called as part of {@link #getVideoFrame()}. It retrieves a video
	 * frame marked as available (recycled). if no frame is available, this method
	 * will block. If interrupted while waiting, a {@link StateException} will
	 * be thrown
	 * @return an available video frame.
	 * @thrown {@link StateException} if interrupted while waiting.
	 */
    private BaseVideoFrame getAvailableVideoFrame() {
        BaseVideoFrame frame = null;
        synchronized (availableVideoFrames) {
            while (availableVideoFrames.size() == 0) try {
                availableVideoFrames.wait();
            } catch (InterruptedException e) {
                throw new StateException("Interrupted while waiting for a video frame", e);
            }
            frame = availableVideoFrames.remove(0);
        }
        return frame;
    }

    @Override
    public final VideoFrame getVideoFrame() throws V4L4JException {
        return getNextVideoFrame();
    }

    final VideoFrame getNextVideoFrame() throws V4L4JException {
        int frameSize;
        BaseVideoFrame nextFrame;
        state.get();
        if ((pushSource != null) && (Thread.currentThread().getId() != pushSourceThreadId)) {
            state.put();
            throw new UnsupportedMethod("This frame grabber is set to push mode");
        }
        try {
            nextFrame = getAvailableVideoFrame();
            frameSize = fillBuffer(object, nextFrame.getByteArray());
            nextFrame.prepareForDelivery(frameSize, lastCapturedFrameBufferIndex, lastCapturedFrameSequence, lastCapturedFrameTimeuSec);
        } finally {
            state.put();
        }
        return nextFrame;
    }

    static synchronized void Log(String s) {
        System.out.println(Thread.currentThread().getName() + ": " + s);
        System.out.flush();
    }

    /**
	 * This method is called by a video frame, when it is being recycled.
	 * @param frame the frame being recycled.
	 */
    final void recycleVideoBuffer(BaseVideoFrame frame) {
        enqueueBuffer(object, frame.getBufferInex());
        synchronized (availableVideoFrames) {
            availableVideoFrames.add(frame);
            availableVideoFrames.notify();
        }
    }

    @Override
    public final void stopCapture() {
        state.stop();
        if (pushSource != null) pushSource.stopCapture();
        state.waitTillNoMoreUsers();
        for (VideoFrame frame : videoFrames) frame.recycle();
        stop(object);
        synchronized (availableVideoFrames) {
            availableVideoFrames.removeAllElements();
        }
        state.commit();
    }

    /**
	 * This method releases resources used by the FrameCapture object.
	 * @throws StateException if if this 
	 * <code>FrameGrabber</code> has been already released, and therefore must 
	 * not be used anymore.
	 */
    final void release() {
        try {
            stopCapture();
        } catch (StateException se) {
        }
        state.release();
        doRelease(object);
        state.commit();
    }

    @Override
    public final int getHeight() {
        state.checkReleased();
        return height;
    }

    @Override
    public final int getWidth() {
        state.checkReleased();
        return width;
    }

    @Override
    public final int getChannel() {
        state.checkReleased();
        return channel;
    }

    @Override
    public final int getStandard() {
        state.checkReleased();
        return standard;
    }

    /**
	 * be careful when using this method to not introduce race conditions
	 * @return if this grabber is started
	 */
    final boolean isStarted() {
        return state.isStarted();
    }

    protected static class State {

        private int state;

        private int temp;

        private int users;

        private static int UNINIT = 0;

        private static int INIT = 1;

        private static int STARTED = 2;

        private static int STOPPED = 3;

        private static int RELEASED = 4;

        /**
		 * Start the state machine with state == temp == UNINIT
		 */
        public State() {
            state = UNINIT;
            temp = UNINIT;
            users = 0;
        }

        /**
		 * From state = UNINIT and not about to move to INIT, move to INIT state
		 * otherwise throw StateException
		 */
        public synchronized void init() {
            if (state == UNINIT && temp != INIT) {
                temp = INIT;
            } else throw new StateException("This FrameGrabber can not be " + "initialised again");
        }

        /**
		 * From state = INIT or (state = STOPPED and not about to start),
		 * move to state = STARTED
		 */
        public synchronized void start() {
            if (state == INIT || state == STOPPED && temp != STARTED) {
                temp = STARTED;
            } else throw new StateException("This FrameGrabber is not initialised" + " or stopped and can not be started");
        }

        /**
		 * Must be called with state object lock held.
		 * @throws StateException if released
		 * @return
		 */
        public boolean isStarted() {
            checkReleased();
            return state == STARTED && temp != STOPPED;
        }

        /**
		 * Must be called with state object lock held
		 * @return
		 */
        public void checkReleased() {
            if (state == RELEASED || temp == RELEASED) throw new StateException("This FrameGrabber has been released");
        }

        /**
		 * Increase number of users by one.
		 */
        public synchronized void get() {
            if (users == 0) notify();
            users++;
        }

        /**
		 * Block until there is at least one user
		 */
        public synchronized void waitForAtLeastOneUser() {
            while (users == 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while waiting for " + "push thread to start");
                    e.printStackTrace();
                    throw new StateException("Interrupted while waiting for" + "push thread to start");
                }
            }
        }

        /**
		 * Decrease the number of users by one
		 */
        public synchronized void put() {
            if (--users == 0) notify();
            if (temp == STOPPED) throw new StateException("This framegrabber was stopped");
        }

        public synchronized void stop() {
            if (state == STARTED && temp != STOPPED) {
                temp = STOPPED;
            } else throw new StateException("This FrameGrabber is not started and " + "can not be stopped");
        }

        /**
		 * This method unblocks when there are no more users.
		 * It is the caller's responsibility to ensure before the call that the
		 * current state does not allow any more users to join, but only to exit 
		 */
        public synchronized void waitTillNoMoreUsers() {
            while (users != 0) try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        public synchronized void release() {
            if (state == INIT || state == STOPPED && temp != RELEASED) {
                temp = RELEASED;
            } else throw new StateException("This FrameGrabber is neither " + "initialised nor stopped and can not be released");
        }

        public synchronized void commit() {
            state = temp;
        }

        public synchronized void rollback() {
            temp = state;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dInfo == null) ? 0 : dInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractGrabber)) {
            return false;
        }
        AbstractGrabber other = (AbstractGrabber) obj;
        if (dInfo == null) {
            if (other.dInfo != null) {
                return false;
            }
        } else if (!dInfo.equals(other.dInfo)) {
            return false;
        }
        return true;
    }
}
