package org.jmule.core.protocol.donkey;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import org.jmule.util.Convert;

/** FIXME: a class deserves a proper 
 * @version $Revision: 1.2 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/09/09 15:28:34 $
 */
public class DonkeyPacketReceiver implements DonkeyPacketConstants {

    static Logger log = Logger.getLogger(DonkeyPacketReceiver.class.getName());

    public DonkeyPacketReceiver(DonkeyConnection myParent) {
        this(myParent, null);
    }

    public DonkeyPacketReceiver(DonkeyConnection myParent, DonkeyClientExtension[] extend) {
        missingBytes = 6;
        packetStarted = false;
        this.dc = myParent;
        this.extend = extend;
        extenionEnabled = extend != null;
    }

    public synchronized void newPacket() {
        packetLength = 0;
        commandId = 0;
        missingBytes = 5;
        packetStarted = true;
        headerDone = false;
    }

    public synchronized int append(int limit) throws IOException {
        int result = 0;
        while (!dc.doClose() && result < limit) {
            intermedianBuffer.position(0);
            intermedianBuffer.limit(Math.min(limit - result, intermedianBufferSize));
            int bytes = dc.getChannel().read(intermedianBuffer);
            if (bytes > 0) {
                result += bytes;
                append(intermedianBuffer, bytes);
            } else if (bytes == -1) {
                dc.addReceivedBytesNum(result);
                log.fine("detect end of stream on channel.read(). close connection " + dc.getConnectionNumber());
                dc.close();
                break;
            } else {
                break;
            }
        }
        return result;
    }

    public synchronized void append(ByteBuffer byteBuffer, int n) throws IOException {
        byteBuffer.rewind();
        byteBuffer.limit(n);
        if (extension_n > 0 && extend[extension_n].usesNonStandardPacket()) {
            log.fine(" usesNonStandardPacket extension " + extension_n + " incomming buffer: " + byteBuffer);
            extension_n = extend[extension_n].appendMessage(byteBuffer) ? -1 : extension_n;
            n = byteBuffer.remaining();
            if (n == 0) {
                return;
            }
        }
        if (inBuffer != null) {
            ByteBuffer oldBuffer = inBuffer;
            inBuffer = DonkeyPacket.allocatePlainByteBuffer(oldBuffer.remaining() + n);
            inBuffer.put(oldBuffer);
            DonkeyPacket.disposePlainByteBuffer(oldBuffer);
        } else inBuffer = DonkeyPacket.allocatePlainByteBuffer(n);
        inBuffer.put(byteBuffer);
        inBuffer.rewind();
        while (!dc.doClose() && inBuffer.remaining() >= missingBytes) {
            if (!packetStarted) {
                byte b = inBuffer.get();
                int bAsInt = Convert.byteToInt(b);
                if (bAsInt != OP_EDONKEYHEADER && extenionEnabled) {
                    log.finer("search extension for protocolopcode: 0x" + Convert.byteToHex(b));
                    for (int i = 0; i < extend.length; i++) {
                        if (bAsInt == extend[i].protocolopcode()) {
                            extension_n = i;
                            log.finer("use extension " + extension_n + " for protocolopcode: 0x" + Convert.byteToHex(b));
                            break;
                        }
                    }
                    if (extension_n > -1 && extend[extension_n].usesNonStandardPacket()) {
                        log.fine(" usesNonStandardPacket extension " + extension_n + " incomming buffer: " + byteBuffer);
                        extension_n = extend[extension_n].appendMessage(inBuffer) ? -1 : extension_n;
                        continue;
                    }
                } else {
                    switch(bAsInt) {
                        case OP_EDONKEYHEADER:
                            break;
                        case 0xC5:
                            skippacket = true;
                            log.warning(((DonkeyConnectionSkeleton) dc).getConnectionNumber() + " send emule packet");
                            break;
                        default:
                            skippacket = true;
                            log.warning(((DonkeyConnectionSkeleton) dc).getConnectionNumber() + " send unsupported packettype protocolopcode = " + Convert.byteToHex(b));
                            throw new IOException(((DonkeyConnectionSkeleton) dc).getConnectionNumber() + " send unsupported packettype protocolopcode = " + Convert.byteToHex(b));
                    }
                }
                newPacket();
            } else {
                if (!headerDone) {
                    packetLength = inBuffer.getInt();
                    if (packetLength > 0 && packetLength < BLOCKSIZE + 26) {
                        packet = new DonkeyPacket(packetLength + 5);
                    } else {
                        String errormsg = ((DonkeyConnectionSkeleton) dc).getConnectionNumber() + " send packet with unsupported packet size " + packetLength;
                        log.warning(errormsg);
                        throw new IOException(errormsg);
                    }
                    packetBody = packet.getBuffer();
                    packet.setCommandId(inBuffer.get());
                    packetBody.position(6);
                    missingBytes = packetLength - 1;
                    headerDone = true;
                    if (missingBytes == 0) {
                        packetStarted = false;
                        headerDone = false;
                        packetBody.rewind();
                        if (extension_n < 0) {
                            dc.addInPacket(packet);
                        } else {
                            log.finer("extension_n: " + extension_n + " packet fininshed");
                            packet.getBuffer().put(0, (byte) extend[extension_n].protocolopcode());
                            extend[extension_n].addStandardPacket(packet);
                            extension_n = -1;
                        }
                        missingBytes = 6;
                        packetLength = 0;
                    }
                } else {
                    int templimit = inBuffer.limit();
                    if (missingBytes > 0) {
                        inBuffer.limit(inBuffer.position() + missingBytes);
                        packetBody.put(inBuffer);
                    }
                    packetBody.rewind();
                    inBuffer.limit(templimit);
                    packetStarted = false;
                    if (!skippacket) {
                        if (extension_n < 0) {
                            dc.addInPacket(packet);
                        } else {
                            log.finer("extension_n: " + extension_n + " packet fininshed");
                            packet.getBuffer().put(0, (byte) extend[extension_n].protocolopcode());
                            extend[extension_n].addStandardPacket(packet);
                            extension_n = -1;
                        }
                    } else {
                        skippacket = false;
                        log.info("skipped packet " + Convert.byteBufferToHexString(inBuffer, 0, 64));
                    }
                    missingBytes = 6;
                    packetLength = 0;
                }
            }
        }
    }

    protected synchronized void cleanup() {
        DonkeyPacket.disposePlainByteBuffer(intermedianBuffer);
        intermedianBuffer = null;
        if (inBuffer != null) {
            DonkeyPacket.disposePlainByteBuffer(inBuffer);
            inBuffer = null;
        }
    }

    private static final int intermedianBufferSize = 10240;

    private ByteBuffer intermedianBuffer = DonkeyPacket.allocatePlainByteBuffer(intermedianBufferSize);

    private boolean skippacket = false;

    private int packetLength;

    private byte commandId;

    private int missingBytes;

    private boolean packetStarted;

    private boolean headerDone;

    private DonkeyPacket packet;

    private ByteBuffer inBuffer;

    private ByteBuffer packetBody;

    private DonkeyConnection dc;

    private DonkeyClientExtension[] extend;

    private boolean extenionEnabled = false;

    private int extension_n = -1;

    public DonkeyConnection getDonkeyConnection() {
        return dc;
    }
}
