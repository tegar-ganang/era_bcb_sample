package org.red5.server.net.rtmpe;

import javax.crypto.Cipher;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPE IO filter
 * 
 * @author Peter Thomas (ptrthomas@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPEIoFilter extends IoFilterAdapter {

    private static final Logger log = LoggerFactory.getLogger(RTMPEIoFilter.class);

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object obj) throws Exception {
        RTMP rtmp = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
        if (session.containsAttribute(RTMPConnection.RTMP_HANDSHAKE)) {
            log.trace("Handshake exists on the session");
            RTMPHandshake handshake = (RTMPHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
            int handshakeType = handshake.getHandshakeType();
            if (handshakeType == 0) {
                log.trace("Handshake type is not currently set");
                byte handshakeByte = RTMPConnection.RTMP_NON_ENCRYPTED;
                if (obj instanceof IoBuffer) {
                    IoBuffer message = (IoBuffer) obj;
                    message.mark();
                    handshakeByte = message.get();
                    message.reset();
                }
                handshake.setHandshakeType(handshakeByte);
                rtmp.setEncrypted(handshakeByte == RTMPConnection.RTMP_ENCRYPTED ? true : false);
            } else if (handshakeType == 3) {
                if (rtmp.getState() == RTMP.STATE_CONNECTED) {
                    log.debug("In connected state");
                    session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                    log.debug("Using non-encrypted communications");
                }
            } else if (handshakeType == 6) {
                RTMPMinaConnection conn = (RTMPMinaConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
                long readBytesCount = conn.getReadBytes();
                long writeBytesCount = conn.getWrittenBytes();
                log.trace("Bytes read: {} written: {}", readBytesCount, writeBytesCount);
                if (writeBytesCount >= (Constants.HANDSHAKE_SIZE * 2)) {
                    log.debug("Assumed to be in a connected state");
                    session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                    log.debug("Using encrypted communications");
                    if (session.containsAttribute(RTMPConnection.RTMPE_CIPHER_IN)) {
                        log.debug("Ciphers already exist on the session");
                    } else {
                        log.debug("Adding ciphers to the session");
                        session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherIn());
                        session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherOut());
                    }
                }
            }
        }
        Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN);
        if (cipher != null) {
            IoBuffer message = (IoBuffer) obj;
            if (rtmp.getState() == RTMP.STATE_HANDSHAKE) {
                byte[] handshakeReply = new byte[Constants.HANDSHAKE_SIZE];
                message.get(handshakeReply);
                rtmp.setState(RTMP.STATE_CONNECTED);
            }
            log.debug("Decrypting buffer: {}", message);
            byte[] encrypted = new byte[message.remaining()];
            message.get(encrypted);
            message.clear();
            message.free();
            byte[] plain = cipher.update(encrypted);
            IoBuffer messageDecrypted = IoBuffer.wrap(plain);
            log.debug("Decrypted buffer: {}", messageDecrypted);
            nextFilter.messageReceived(session, messageDecrypted);
        } else {
            log.trace("Not decrypting message received: {}", obj);
            nextFilter.messageReceived(session, obj);
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception {
        Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
        if (cipher != null) {
            IoBuffer message = (IoBuffer) request.getMessage();
            if (!message.hasRemaining()) {
                log.debug("Buffer was empty");
            } else {
                log.debug("Encrypting buffer: {}", message);
                byte[] plain = new byte[message.remaining()];
                message.get(plain);
                message.clear();
                message.free();
                byte[] encrypted = cipher.update(plain);
                IoBuffer messageEncrypted = IoBuffer.wrap(encrypted);
                log.debug("Encrypted buffer: {}", messageEncrypted);
                nextFilter.filterWrite(session, new EncryptedWriteRequest(request, messageEncrypted));
            }
        } else {
            log.trace("Not encrypting write request");
            nextFilter.filterWrite(session, request);
        }
    }

    private static class EncryptedWriteRequest extends WriteRequestWrapper {

        private final IoBuffer encryptedMessage;

        private EncryptedWriteRequest(WriteRequest writeRequest, IoBuffer encryptedMessage) {
            super(writeRequest);
            this.encryptedMessage = encryptedMessage;
        }

        @Override
        public Object getMessage() {
            return encryptedMessage;
        }
    }
}
