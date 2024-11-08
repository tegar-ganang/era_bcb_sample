package ti.sutc.ttif;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Map;
import ti.ftt.FTTDeviceManager;
import ti.io.UDataInputStream;
import ti.io.UDataOutputStream;
import ti.mcore.Environment;
import ti.mcore.u.log.PlatoLogger;

/**
 * Note: the process of switching both input and output streams over is a bit
 * complex, due to the fact that certain steps need to be done from different
 * thread contexts to:
 * <ul>
 *   <li> keep everything well synchronized, ie. don't switch in/out streams
 *        mid-command
 *   <li> ensure that FTT_SWITCHING_CNF is the last message sent over primary
 *        link before switching to FTT link.  This is because, on the target
 *        side, the <code>ttif_ftt</code> module holds the write-lock from
 *        the time it receives FTT_CHANNEL_MASKDATA to the time it receives
 *        FTT_SWITCHING_CNF.  This sequence ensures that there are no more
 *        commands sent on the primary link after the PC has switched over
 *        to reading from the FTT link, and visa versa.
 * </ul>
 * The sequence of messages between the target and PC:
 * <pre>
 *                PC                            Target
 *                 |                             |
 *                 |----- FTT_LOOPBACK_REQ -----&gt;|
 *                 |                             | (target sends test
 *   (PC decodes   |                             |  pattern over FTT)
 *   test pattern) |                             |
 *                 |--- FTT_CHANNEL_MASKDATA ---&gt;|
 *                 |                             | (acquire write lock)
 *                 |&lt;---- FTT_SWITCHING_IND -----| (last message sent over
 *  (switch input) |                             |    primary link to PC)
 *                 |----- FTT_SWITCHING_CNF ----&gt;| (last message sent over
 *                 |                             |    primary link to target)
 * (switch output) |                             | (switch input & output)
 *                 |                             |
 *          
 * </pre>
 * On the PC side, to enforce the "last message" constraint, and to ensure
 * that the switch over is properly synchronized, a state machine between
 * two threads is implemented:
 * <pre>
 *             listener                other                      Target
 *                |                     |                           |
 *                |            (acquire dout lock)                  |
 *                |                     |                           |
 *                |                     |-- FTT_CHANNEL_MASKDATA --&gt;|
 *                |                     |                           |
 *                |&lt;------------ FTT_SWITCHING_IND -----------------|
 *                |                     |                           |
 *       (delay to ensure target's      |                           |
 *         RX buffer is empty)          |                           |
 *                |                     |                           |
 *       (switch input stream)          |                           |
 *                |                     |                           |
 *                |                     |-- FTT_SWITCHING_CNF -----&gt;|
 *                |                     |                           |
 *                |           (switch output stream)                |
 *                |                     |                           |
 *                |           (release dout lock)                   |
 *                |                     |                           |
 * </pre>
 * @author a0868903
 * @author a0873619
 */
public class FttModule extends Module {

    private static final PlatoLogger LOGGER = PlatoLogger.getLogger(FttModule.class);

    private boolean fttConnected = false;

    private FTTDeviceManager fttDeviceManager;

    private boolean receivedSwitchingInd;

    private int fttVersion;

    private boolean fttDetectedOnTarget = false;

    /**
	 * Class Constructor.
	 * 
	 * @param ttif the TTIF SUTConnection, where some central state is maintained
	 */
    public FttModule(short nodeId, TTIFSUTConnection ttif) {
        super(nodeId, 14, ttif);
    }

    /**
	 * Called after handshaking.  This should register any needed command 
	 * handlers, which will stay registered for the duration of this connection
	 * (ie. until {@link #doDisconnect} is called).
	 * 
	 * @param attr    the set of attributes sent from the target
	 */
    public void doConnect(Map<String, Object> attr) {
        fttConnected = false;
        receivedSwitchingInd = false;
        Number num = ((Number) (attr.get(new String("TTIF_FTT_CONNECTED"))));
        if ((num != null) && (num.intValue() == 1)) {
            fttDetectedOnTarget = true;
            if (fttDeviceManager == null) {
                fttDeviceManager = new FTTDeviceManager();
            }
            fttDeviceManager.openFTT();
            if (fttDeviceManager.isFTTInitialized() && (num.intValue() == 1)) {
                fttConnected = true;
            }
        }
        num = ((Number) attr.get("TTIF_FTT_VERSION"));
        if (num != null) fttVersion = num.intValue(); else fttVersion = 0;
        register(FTT_CHANNEL_OPEN);
        register(FTT_CHANNEL_ACK);
        register(FTT_CHANNEL_CLOSE);
        register(FTT_LOOPBACK_REQ);
        register(FTT_CHANNEL_MASKDATA);
        register(FTT_SWITCHING_IND);
        register(FTT_SWITCHING_CNF);
    }

    /**
	 * Called after/during disconnect, to give the module a chance to reset
	 * any internal state.
	 */
    public void doDisconnect() {
        if (fttConnected) fttDeviceManager.closeFTT();
        fttConnected = false;
        fttDetectedOnTarget = false;
    }

    private Message FTT_CHANNEL_OPEN = new Message(0, "FTT_CHANNEL_OPEN") {

        @Override
        public void processMessage(UDataInputStream din, int compSts, int size, long time) throws IOException {
            UDataOutputStream dout = dout();
            int retVal = (fttConnected) ? 1 : -1;
            int channelId = din.readUShort();
            synchronized (dout) {
                writeHeader(dout, FTT_CHANNEL_ACK, 4);
                dout.writeUShort(channelId);
                dout.writeUShort(retVal);
            }
        }
    };

    private Message FTT_CHANNEL_ACK = new Message(1, "FTT_CHANNEL_ACK") {
    };

    private Message FTT_CHANNEL_CLOSE = new Message(2, "FTT_CHANNEL_CLOSE") {

        @Override
        public void processMessage(UDataInputStream din, int compSts, int size, long time) throws IOException {
            int channelId = din.readUShort();
            LOGGER.dbg("FTT_CHANNEL_CLOSE: channelId=" + channelId);
        }
    };

    private Message FTT_LOOPBACK_REQ = new Message(3, "FTT_LOOPBACK_REQ") {
    };

    private Message FTT_CHANNEL_MASKDATA = new Message(4, "FTT_CHANNEL_MASKDATA") {
    };

    private Message FTT_SWITCHING_IND = new Message(5, "FTT_SWITCHING_IND") {

        @Override
        public void processMessage(UDataInputStream din, int compSts, int size, long time) throws IOException {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOGGER.logError(e);
            }
            setInputStream(new BufferedInputStream(fttDeviceManager.getInputStream()));
            synchronized (FttModule.this) {
                receivedSwitchingInd = true;
                FttModule.this.notify();
            }
        }
    };

    private Message FTT_SWITCHING_CNF = new Message(6, "FTT_SWITCHING_CNF") {
    };

    public boolean isFTTConnected() {
        return fttConnected;
    }

    public boolean isFTTInitialized() {
        if (fttDeviceManager != null) return fttDeviceManager.isFTTInitialized();
        return false;
    }

    /**
	 * @return <code>true</code> if FTT board plugged in and detected by the target
	 */
    public boolean isFTTDetectedOnTarget() {
        return fttDetectedOnTarget;
    }

    /**
	 * Function that validates all FTT channels and sends the result to target 
	 * 
	 * @return
	 *   If FTT is opened/initialized
	 *     then returns InputStream
	 *   	 else returns null
	 */
    public boolean doFTTSelfTest() {
        try {
            if (fttConnected) {
                sendLoopbackReq();
                byte[] data = fttDeviceManager.getChannelStatus();
                if (data == null) return false;
                sendMaskData(data);
                return true;
            }
        } catch (IOException e) {
            Environment.getEnvironment().unhandledException(e);
        }
        return false;
    }

    private void sendMaskData(byte[] maskData) throws IOException {
        try {
            UDataOutputStream dout = dout();
            synchronized (dout) {
                writeHeader(dout, FTT_CHANNEL_MASKDATA, maskData.length);
                dout.write(maskData);
                dout.flush();
                synchronized (FttModule.this) {
                    while (!receivedSwitchingInd) {
                        FttModule.this.wait();
                    }
                }
                writeHeader(dout, FTT_SWITCHING_CNF, 0);
                dout.flush();
                if (fttVersion >= 2) {
                    System.err.println("*** switch output stream");
                    setOutputStream(new BufferedOutputStream(fttDeviceManager.getOutputStream()));
                }
                setFastConnected(nodeId, true);
            }
        } catch (InterruptedException e) {
            LOGGER.logError(e);
        }
    }

    private boolean sendLoopbackReq() throws IOException {
        if (fttConnected) {
            UDataOutputStream dout = dout();
            synchronized (dout) {
                writeHeader(dout, FTT_LOOPBACK_REQ, 0);
                dout.flush();
                return true;
            }
        } else {
            LOGGER.dbg("FTT not connected. No Loopback Req possible.");
            return false;
        }
    }
}
