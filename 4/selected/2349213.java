package net.sf.fmj.media;

import java.awt.*;
import javax.media.*;
import javax.media.control.*;
import net.sf.fmj.filtergraph.*;

/**
 * BasicFilterModule is a module which is not threaded and have one
 * InputConnector and one OutputConnector. It receives data from its input
 * connector, pass the data to the level 3 plugIn codec and put the result in
 * the OutputConnector. BasicFilterModule can be either Push or Pull driven. The
 * plugIn codec might be media decoder, media encoder, effect etc.
 * 
 */
public class BasicFilterModule extends BasicModule {

    protected Codec codec;

    protected InputConnector ic;

    protected OutputConnector oc;

    protected FrameProcessingControl frameControl = null;

    protected float curFramesBehind = 0f;

    protected float prevFramesBehind = 0f;

    protected java.awt.Frame controlFrame;

    protected final boolean VERBOSE_CONTROL = false;

    protected Buffer storedInputBuffer, storedOutputBuffer;

    protected boolean readPendingFlag = false, writePendingFlag = false;

    private boolean failed = false;

    private boolean markerSet = false;

    private Object lastHdr = null;

    public BasicFilterModule(Codec c) {
        ic = new BasicInputConnector();
        registerInputConnector("input", ic);
        oc = new BasicOutputConnector();
        registerOutputConnector("output", oc);
        setCodec(c);
        protocol = Connector.ProtocolPush;
        Object control = c.getControl(FrameProcessingControl.class.getName());
        if (control instanceof FrameProcessingControl) frameControl = (FrameProcessingControl) control;
    }

    @Override
    public void doClose() {
        if (codec != null) {
            codec.close();
        }
        if (controlFrame != null) {
            controlFrame.dispose();
            controlFrame = null;
        }
    }

    @Override
    public boolean doPrefetch() {
        return super.doPrefetch();
    }

    @Override
    public boolean doRealize() {
        if (codec != null) {
            try {
                codec.open();
                if (VERBOSE_CONTROL) {
                    controlFrame = new java.awt.Frame(codec.getName() + "  Control");
                    controlFrame.setLayout(new com.sun.media.controls.VFlowLayout(1));
                    controlFrame.add(new Label(codec.getName() + "  Control", Label.CENTER));
                    controlFrame.add(new Label(" "));
                    Control[] c = (Control[]) codec.getControls();
                    for (int i = 0; i < c.length; i++) {
                        controlFrame.add(c[i].getControlComponent());
                    }
                    controlFrame.pack();
                    controlFrame.show();
                }
            } catch (ResourceUnavailableException rue) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the plugIn codec of this filter, null if not yet set.
     */
    public Codec getCodec() {
        return codec;
    }

    @Override
    public Object getControl(String s) {
        return codec.getControl(s);
    }

    @Override
    public Object[] getControls() {
        return codec.getControls();
    }

    @Override
    public boolean isThreaded() {
        if ((getProtocol() == Connector.ProtocolSafe)) return true;
        return false;
    }

    @Override
    public void process() {
        Buffer inputBuffer, outputBuffer;
        do {
            if (readPendingFlag) inputBuffer = storedInputBuffer; else {
                Format incomingFormat;
                inputBuffer = ic.getValidBuffer();
                incomingFormat = inputBuffer.getFormat();
                if (incomingFormat == null) {
                    incomingFormat = ic.getFormat();
                    inputBuffer.setFormat(incomingFormat);
                }
                if (incomingFormat != ic.getFormat() && incomingFormat != null && !incomingFormat.equals(ic.getFormat()) && !inputBuffer.isDiscard()) {
                    if (writePendingFlag) {
                        storedOutputBuffer.setDiscard(true);
                        oc.writeReport();
                        writePendingFlag = false;
                    }
                    if (!reinitCodec(inputBuffer.getFormat())) {
                        inputBuffer.setDiscard(true);
                        ic.readReport();
                        failed = true;
                        if (moduleListener != null) moduleListener.formatChangedFailure(this, ic.getFormat(), inputBuffer.getFormat());
                        return;
                    }
                    Format oldFormat = ic.getFormat();
                    ic.setFormat(inputBuffer.getFormat());
                    if (moduleListener != null) moduleListener.formatChanged(this, oldFormat, inputBuffer.getFormat());
                }
                if ((inputBuffer.getFlags() & Buffer.FLAG_SYSTEM_MARKER) != 0) {
                    markerSet = true;
                }
            }
            if (writePendingFlag) outputBuffer = storedOutputBuffer; else {
                outputBuffer = oc.getEmptyBuffer();
                if (outputBuffer != null) {
                    outputBuffer.setLength(0);
                    outputBuffer.setOffset(0);
                    lastHdr = outputBuffer.getHeader();
                }
            }
            outputBuffer.setTimeStamp(inputBuffer.getTimeStamp());
            outputBuffer.setDuration(inputBuffer.getDuration());
            outputBuffer.setSequenceNumber(inputBuffer.getSequenceNumber());
            outputBuffer.setFlags(inputBuffer.getFlags());
            outputBuffer.setHeader(inputBuffer.getHeader());
            if (resetted) {
                if ((inputBuffer.getFlags() & Buffer.FLAG_FLUSH) != 0) {
                    codec.reset();
                    resetted = false;
                }
                readPendingFlag = writePendingFlag = false;
                ic.readReport();
                oc.writeReport();
                return;
            }
            if (failed || inputBuffer.isDiscard()) {
                if (markerSet) {
                    outputBuffer.setFlags(outputBuffer.getFlags() & ~Buffer.FLAG_SYSTEM_MARKER);
                    markerSet = false;
                }
                curFramesBehind = 0;
                ic.readReport();
                if (!writePendingFlag) oc.writeReport();
                return;
            }
            if (frameControl != null && curFramesBehind != prevFramesBehind && (inputBuffer.getFlags() & Buffer.FLAG_NO_DROP) == 0) {
                frameControl.setFramesBehind(curFramesBehind);
                prevFramesBehind = curFramesBehind;
            }
            int rc = 0;
            try {
                rc = codec.process(inputBuffer, outputBuffer);
            } catch (Throwable e) {
                Log.dumpStack(e);
                if (moduleListener != null) moduleListener.internalErrorOccurred(this);
            }
            if (PlaybackEngine.TRACE_ON && !verifyBuffer(outputBuffer)) {
                System.err.println("verify buffer failed: " + codec);
                Thread.dumpStack();
                if (moduleListener != null) moduleListener.internalErrorOccurred(this);
            }
            if ((rc & PlugIn.PLUGIN_TERMINATED) != 0) {
                failed = true;
                if (moduleListener != null) moduleListener.pluginTerminated(this);
                readPendingFlag = writePendingFlag = false;
                ic.readReport();
                oc.writeReport();
                return;
            }
            if (curFramesBehind > 0f && outputBuffer.isDiscard()) {
                curFramesBehind -= 1.0f;
                if (curFramesBehind < 0) curFramesBehind = 0f;
                rc = rc & ~PlugIn.OUTPUT_BUFFER_NOT_FILLED;
            }
            if ((rc & PlugIn.BUFFER_PROCESSED_FAILED) != 0) {
                outputBuffer.setDiscard(true);
                if (markerSet) {
                    outputBuffer.setFlags(outputBuffer.getFlags() & ~Buffer.FLAG_SYSTEM_MARKER);
                    markerSet = false;
                }
                ic.readReport();
                oc.writeReport();
                readPendingFlag = writePendingFlag = false;
                return;
            }
            if (outputBuffer.isEOM() && ((rc & PlugIn.INPUT_BUFFER_NOT_CONSUMED) != 0 || (rc & PlugIn.OUTPUT_BUFFER_NOT_FILLED) != 0)) {
                outputBuffer.setEOM(false);
            }
            if ((rc & PlugIn.OUTPUT_BUFFER_NOT_FILLED) != 0) {
                writePendingFlag = true;
                storedOutputBuffer = outputBuffer;
            } else {
                if (markerSet) {
                    outputBuffer.setFlags(outputBuffer.getFlags() | Buffer.FLAG_SYSTEM_MARKER);
                    markerSet = false;
                }
                oc.writeReport();
                writePendingFlag = false;
            }
            if (((rc & PlugIn.INPUT_BUFFER_NOT_CONSUMED) != 0 || (inputBuffer.isEOM() && !outputBuffer.isEOM()))) {
                readPendingFlag = true;
                storedInputBuffer = inputBuffer;
            } else {
                inputBuffer.setHeader(lastHdr);
                ic.readReport();
                readPendingFlag = false;
            }
        } while (readPendingFlag);
    }

    /**
     * A new input format has been detected, we'll check if the existing codec
     * can handle it. Otherwise, we'll try to re-create a new codec to handle
     * it.
     */
    protected boolean reinitCodec(Format input) {
        if (codec != null) {
            if (codec.setInputFormat(input) != null) {
                return true;
            }
            codec.close();
            codec = null;
        }
        Codec c;
        if ((c = SimpleGraphBuilder.findCodec(input, null, null, null)) == null) return false;
        setCodec(c);
        return true;
    }

    public boolean setCodec(Codec codec) {
        this.codec = codec;
        return true;
    }

    /**
     * sets the plugIn codec of this filter.
     * 
     * @param codec
     *            the plugIn codec (should we specify Codec class or String)
     * @return true if successful
     */
    public boolean setCodec(String codec) {
        return true;
    }

    @Override
    public void setFormat(Connector c, Format f) {
        if (c == ic) {
            if (codec != null) codec.setInputFormat(f);
        } else if (c == oc) {
            if (codec != null) codec.setOutputFormat(f);
        }
    }

    protected void setFramesBehind(float framesBehind) {
        curFramesBehind = framesBehind;
    }
}
