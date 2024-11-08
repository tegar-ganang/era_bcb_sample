package net.sf.dz.daemon.tcp;

import net.sf.dz.daemon.onewire.DeviceContainer;
import net.sf.dz.daemon.onewire.SwitchContainer;
import net.sf.jukebox.jmx.JmxDescriptor;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * The TCP controller. Accepts the control messages, interprets and tries to
 * execute.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2007
 * @version $Id: Controller.java,v 1.25 2007-03-26 09:26:08 vtt Exp $
 */
public class Controller extends Connector {

    /**
   * Set of known switch addresses.
   */
    private DeviceSet switchSet = new DeviceSet();

    /**
   * Map of known device channel numbers by address. It is assumed that the
   * device with the same 1-Wire address will never change its channel count,
   * therefore, the probe for channel count will be attempted only once.
   */
    private Map<String, Integer> channelMap = new TreeMap<String, Integer>();

    /**
   * Clean flag.
   */
    private boolean clean = true;

    /**
   * {@inheritDoc}
   */
    @Override
    protected String getAnnounce() {
        StringBuffer sb = new StringBuffer();
        sb.append("DZ DAC Switches").append(super.getAnnounce()).append("/");
        synchronized (switchSet) {
            for (Iterator<DeviceContainer> i = switchSet.iterator(); i.hasNext(); ) {
                sb.append(" ").append(getDeviceSignature(i.next()));
            }
        }
        return sb.toString();
    }

    /**
   * Get the device signature for the given container.
   *
   *  @param dc Device container to get the signature for.
   *  @return Device signature.
   */
    protected String getDeviceSignature(DeviceContainer dc) {
        String signature = "";
        int channelCount = getChannelCount(dc.getAddress());
        for (int idx = 0; idx < channelCount; idx++) {
            signature += dc.getSignature() + ":" + idx;
            if (idx < (channelCount - 1)) {
                signature += " ";
            }
        }
        return signature;
    }

    /**
   * Get the number of channels supported by the container at the given address.
   * @param address Address to retrieve channel count for.
   * @return Channel count. If the device can't be resolved, default is 2.
   */
    protected int getChannelCount(String address) {
        Integer iCount = channelMap.get(address);
        if (iCount == null) {
            logger.warn(address + ": how come we don't have a channel map for it? Assuming 2 until able to resolve");
            iCount = new Integer(2);
        }
        return iCount.intValue();
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public String getServiceSignature() {
        return "DZ DAC Switches";
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public int getDefaultListenPort() {
        return 5002;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public int getDefaultBroadcastPort() {
        return 5003;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected ConnectionHandler createHandler(Socket socket, BufferedReader br, PrintWriter pw) {
        clean = false;
        return new ControlHandler(socket, br, pw);
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected synchronized void cleanup() throws Throwable {
        if (clean) {
            logger.debug("Already clean");
            return;
        }
        for (Iterator<DeviceContainer> i = switchSet.iterator(); i.hasNext(); ) {
            SwitchContainer sc = (SwitchContainer) i.next();
            logger.info("Cleanup: shutting down " + sc);
            sc.reset();
        }
        logger.info("Cleaned up");
        clean = true;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected void shutdown2() throws Throwable {
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected boolean isUnique() {
        return true;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected synchronized void broadcastArrival(DeviceContainer dc) {
        if (dc instanceof SwitchContainer) {
            switchSet.add(dc);
            updateChannelMap((SwitchContainer) dc);
            announce(getAnnounce());
            for (Iterator<ConnectionHandler> i = iterator(); i.hasNext(); ) {
                ConnectionHandler ch = i.next();
                ch.iHave();
                for (StringTokenizer st = new StringTokenizer(getDeviceSignature(dc), " "); st.hasMoreTokens(); ) {
                    ch.send("+ " + st.nextToken());
                }
            }
        }
    }

    /**
   * Update the channel map with the switch container.
   *
   * @param sc Container to add to the map.
   */
    private synchronized void updateChannelMap(SwitchContainer sc) {
        String address = sc.getAddress();
        if (channelMap.get(address) != null) {
            return;
        }
        channelMap.put(address, new Integer(sc.getChannelCount()));
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected synchronized void broadcastDeparture(DeviceContainer dc) {
        if (!switchSet.contains(dc)) {
            return;
        }
        logger.debug("Departed: " + dc);
        switchSet.remove(dc);
        announce(getAnnounce());
        for (Iterator<ConnectionHandler> i = iterator(); i.hasNext(); ) {
            ConnectionHandler ch = i.next();
            ch.iHave();
            for (StringTokenizer st = new StringTokenizer(getDeviceSignature(dc), " "); st.hasMoreTokens(); ) {
                ch.send("- " + st.nextToken());
            }
        }
    }

    /**
   * Connection handler for the control connection.
   */
    protected class ControlHandler extends ConnectionHandler {

        /**
     * Create an instance.
     *
     * @param socket Socket to operate on.
     * @param br Reader to use.
     * @param pw Writer to use.
     */
        public ControlHandler(Socket socket, BufferedReader br, PrintWriter pw) {
            super(socket, br, pw);
        }

        /**
     * {@inheritDoc}
     */
        @Override
        protected void sayHello() {
            iHave();
        }

        /**
     * {@inheritDoc}
     */
        @Override
        public synchronized void iHave() {
            String message = "";
            int deviceCount = 0;
            for (Iterator<DeviceContainer> i = switchSet.iterator(); i.hasNext(); ) {
                DeviceContainer dc = i.next();
                message += getDeviceSignature(dc);
                deviceCount += getChannelCount(dc.getAddress());
                if (i.hasNext()) {
                    message += " ";
                }
            }
            send("IHAVE " + deviceCount + ": " + message);
        }

        /**
     * {@inheritDoc}
     */
        @Override
        protected CommandParser createParser() {
            return new ControlParser();
        }

        /**
     * Command parser for the control connection.
     */
        protected class ControlParser extends CommandParser {

            /**
       * {@inheritDoc}
       */
            @Override
            protected void parse2(String command) throws Throwable {
                logger.info("Command received: '" + command + "'");
                StringTokenizer st = new StringTokenizer(command, " ");
                String address = st.nextToken();
                if (address.indexOf(":") != -1) {
                    parseNew(address, st);
                } else {
                    parseOld(address, st);
                }
            }

            /**
       * Parse the old syntax command.
       *
       * @param address Address to assume.
       * @param st Tokenizer to parse from.
       * @throws Throwable if anything goes wrong.
       */
            private void parseOld(String address, StringTokenizer st) throws Throwable {
                String verb = st.nextToken();
                String channel = st.nextToken();
                boolean value = false;
                if ("write".equalsIgnoreCase(verb)) {
                    String sValue = st.nextToken();
                    if ("1".equals(sValue)) {
                        value = true;
                    } else if ("0".equals(sValue)) {
                        value = false;
                    } else {
                        throw new IllegalArgumentException("Illegal value: '" + sValue + "'");
                    }
                }
                execute(address, Integer.parseInt(channel), verb, value);
            }

            /**
       * Parse the new syntax command.
       *
       * @param extendedAddress Extended address to assume.
       * @param st Tokenizer to parse from.
       * @throws Throwable if anything goes wrong.
       */
            private void parseNew(String extendedAddress, StringTokenizer st) throws Throwable {
                int offset = extendedAddress.indexOf(":");
                String address = extendedAddress.substring(0, offset);
                String channel = extendedAddress.substring(offset + 1);
                String verb = st.nextToken();
                boolean value = false;
                logger.debug("Extended: " + extendedAddress);
                logger.debug("Address:  " + address);
                logger.debug("Channel:  " + channel);
                logger.debug("Verb:     " + verb);
                if ("write".equalsIgnoreCase(verb)) {
                    String sValue = st.nextToken();
                    if ("1".equals(sValue)) {
                        value = true;
                    } else if ("0".equals(sValue)) {
                        value = false;
                    } else {
                        throw new IllegalArgumentException("Illegal value: '" + sValue + "'");
                    }
                    logger.debug("Value:   " + value);
                }
                execute(address, Integer.parseInt(channel), verb, value);
            }

            /**
       * Execute the command.
       *
       * @param address Address to execute the command on.
       * @param channel Channel to use.
       * @param verb Verb to execute.
       * @param value Value to set.
       * @throws Throwable if anything goes wrong.
       */
            private void execute(String address, int channel, String verb, boolean value) throws Throwable {
                SwitchContainer sc = (SwitchContainer) switchSet.resolve(address);
                if (sc == null) {
                    send("E " + address + ":" + channel + " Device Not Present");
                    return;
                }
                if ("read".equalsIgnoreCase(verb)) {
                    send(sc.read(channel) ? "1" : "0");
                } else if ("write".equalsIgnoreCase(verb)) {
                    sc.write(channel, value);
                    send("OK");
                }
            }
        }
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        JmxDescriptor d = super.getJmxDescriptor();
        return new JmxDescriptor("DZ", d.name, d.instance, "DAC TCP controller");
    }
}
