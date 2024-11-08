import com.rbnb.sapi.*;

public final class InALine extends Thread {

    /**
     * debug?
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/31/2003
     */
    public boolean debug = false;

    /**
     * the index of the source channel.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/30/2003
     */
    private int channel;

    /**
     * the list of <code>TestFilters</code> being run.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/30/2003
     */
    private TestFilter[] filters = null;

    /**
     * the name of the output channel of the last <code>TestFilter</code>,
     * including the source name.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/30/2003
     */
    private String outChan;

    /**
     * the number of filters to run.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/30/2003
     */
    private int nFilters;

    /**
     * the number of frames.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/30/2003
     */
    private int nFrames;

    /**
     * the address of the RBNB server.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/30/2003
     */
    private String rbnbAddress;

    /**
     * the <code>TestSource</code> providing the input channel.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/30/2003
     */
    private TestSource src;

    /**
     * the status.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 10/22/2003
     */
    private int status = 0;

    /**
     * the timeout duration.
     * <p>
     * a value of -1 is no timeout.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 11/13/2003
     */
    private long timeout = 90000;

    public InALine(String rbnbAddressI, TestSource srcI, int channelI, String nameI, int nFiltersI, int nFramesI, long timeoutI) {
        super(nameI);
        setRBNBAddress(rbnbAddressI);
        setSrc(srcI);
        setChannel(channelI);
        setNFilters(nFiltersI);
        setNFrames(nFramesI);
        setTimeout(timeoutI);
    }

    private final TestFilter createFilter(int idxI, String sourceNameI, String chanI) {
        TestFilter filterR = new TestFilter();
        filterR.setRBNBAddress(rbnbAddress);
        filterR.setSourceName(getName() + "_F" + idxI);
        filterR.setMaximumFrames(getNFrames());
        filterR.setTimeout(getTimeout());
        String[] strChns = new String[1];
        strChns[0] = sourceNameI + "/" + chanI;
        filterR.setStreamChannels(strChns);
        return (filterR);
    }

    public final int getChannel() {
        return (channel);
    }

    public final int getNFilters() {
        return (nFilters);
    }

    public final int getNFrames() {
        return (nFrames);
    }

    public final String getOutChan() {
        return (outChan);
    }

    public final String getRBNBAddress() {
        return (rbnbAddress);
    }

    public final TestSource getSrc() {
        return (src);
    }

    public final int getStatus() {
        return (status);
    }

    public final long getTimeout() {
        return (timeout);
    }

    public final void init() throws java.lang.Exception {
        if (getNFilters() == 0) {
            throw new IllegalArgumentException(getName() + " too few filters.");
        } else if (getNFrames() == 0) {
            throw new IllegalArgumentException(getName() + " too few frames.");
        }
        String chan = "c" + getChannel();
        filters = new TestFilter[getNFilters()];
        filters[0] = createFilter(0, src.getSourceName(), chan);
        chan = src.getSourceName() + "_" + chan;
        for (int idx = 1; idx < getNFilters(); ++idx) {
            filters[idx] = createFilter(idx, filters[idx - 1].getSourceName(), chan);
            chan = filters[idx - 1].getSourceName() + "_" + chan;
        }
        setOutChan(filters[getNFilters() - 1].getSourceName() + "/" + chan);
        if (debug) {
            System.err.println(getName() + " output " + getOutChan());
        }
    }

    public final void run() {
        try {
            if (debug) {
                System.err.println(getName() + " starting filters.");
            }
            for (int idx = 0; idx < filters.length; ++idx) {
                filters[idx].debug = debug;
                filters[idx].start();
            }
            if (debug) {
                System.err.println(getName() + " waiting for filters.");
            }
            for (int idx = 0; idx < filters.length; ++idx) {
                filters[idx].join();
            }
            if (debug) {
                System.err.println(getName() + " results:");
            }
            setStatus(0);
            for (int idx = 0; idx < filters.length; ++idx) {
                if (debug) {
                    System.err.println(filters[idx].getSourceName() + " " + filters[idx].getStatus());
                }
                setStatus(Math.max(getStatus(), filters[idx].getStatus()));
            }
        } catch (Exception e) {
            if (debug) {
                e.printStackTrace();
            }
            setStatus(1);
        }
    }

    public final void setChannel(int channelI) {
        channel = channelI;
    }

    public final void setNFilters(int nFiltersI) {
        nFilters = nFiltersI;
    }

    public final void setNFrames(int nFramesI) {
        nFrames = nFramesI;
    }

    public final void setOutChan(String outChanI) {
        outChan = outChanI;
    }

    public final void setRBNBAddress(String rbnbAddressI) {
        rbnbAddress = rbnbAddressI;
    }

    public final void setSrc(TestSource srcI) {
        src = srcI;
    }

    public final void setStatus(int statusI) {
        status = statusI;
    }

    public final void setTimeout(long timeoutI) {
        timeout = timeoutI;
    }

    public final void terminate() {
        for (int idx = 0; idx < filters.length; ++idx) {
            filters[idx].close(false, false);
        }
    }
}
