package ch.iserver.ace.net.impl.protocol;

import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.MessageStatus;
import org.beepcore.beep.core.OutputDataStream;

/**
 *
 */
public class MessageMSGStub implements MessageMSG {

    private OutputDataStream output = null;

    public MessageStatus sendANS(OutputDataStream arg0) throws BEEPException {
        throw new UnsupportedOperationException();
    }

    public MessageStatus sendERR(BEEPError arg0) throws BEEPException {
        throw new UnsupportedOperationException();
    }

    public MessageStatus sendERR(int arg0, String arg1) throws BEEPException {
        throw new UnsupportedOperationException();
    }

    public MessageStatus sendERR(int arg0, String arg1, String arg2) throws BEEPException {
        throw new UnsupportedOperationException();
    }

    public MessageStatus sendNUL() throws BEEPException {
        throw new UnsupportedOperationException();
    }

    public MessageStatus sendRPY(OutputDataStream output) throws BEEPException {
        this.output = output;
        return null;
    }

    public OutputDataStream getRPY() {
        return output;
    }

    public InputDataStream getDataStream() {
        throw new UnsupportedOperationException();
    }

    public Channel getChannel() {
        throw new UnsupportedOperationException();
    }

    public int getMsgno() {
        throw new UnsupportedOperationException();
    }

    public int getAnsno() {
        throw new UnsupportedOperationException();
    }

    public int getMessageType() {
        throw new UnsupportedOperationException();
    }
}
