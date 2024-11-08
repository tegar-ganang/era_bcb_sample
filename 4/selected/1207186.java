package org.rip.ftp.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.rip.ftp.FtpConstants;
import org.rip.ftp.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetrieveCommand extends ClientCommand implements ResponseListener {

    private static Logger LOG = LoggerFactory.getLogger(RetrieveCommand.class);

    private String remoteName;

    private String localName;

    public ClientResponse currResponse = null;

    public String syntax() {
        return FtpConstants.RETR + SP + "<file> [local file]";
    }

    @Override
    public void parseCommand(String line) throws ParseException {
        if (StringUtils.isBlank(line)) {
            throw new ParseException("Invalid retrieve command:" + line);
        }
        String[] split = StringUtils.split(line);
        if (split.length < 2) {
            throw new ParseException("Invalid retrieve command:" + line);
        }
        remoteName = split[1];
        if (split.length > 2) {
            localName = split[2];
        }
    }

    @Override
    public void execute() {
        if (session.getState() != FtpClientState.READY && session.getDCState() != FtpClientState.DC_OPENED) {
            LOG.info("client not in correct state");
            return;
        }
        String cmd = FtpConstants.RETR + SP + remoteName + CRLF;
        session.addListener(this);
        session.sendCommand(cmd);
    }

    boolean complete = false;

    public void processResponse(ClientResponse expected) {
        switch(expected.getResponse()) {
            case Restart:
                break;
            case TransferStarting:
            case AboutToOpenDataConnection:
                session.setDCState(FtpClientState.DC_TRANSFER);
                LOG.trace("setting to transferring");
                break;
            case ClosingDataConnection:
            case Completed:
                InputStream raw = ((ClientDataConnection) session.getData()).getDataAsStream();
                session.closeDataConnection();
                session.removeListener(this);
                processData(raw);
                complete = true;
                break;
            case CannotOpenDataConnection:
            case DataConnectionClosedAborted:
            case LocalError:
            case FileUnavailable:
            case SyntaxErrorCommand:
            case SyntaxErrorParam:
            case CommandNotImplemented:
            case NotAvailable:
            case NotLoggedIn:
            case FileUnavailableError:
            case BadSequence:
                LOG.info(expected.getText());
                session.closeDataConnection();
                session.removeListener(this);
                break;
            case ReadComplete:
            case FileSent:
                LOG.info("RETR: {} {}", expected.getCode(), expected.getText());
                if (!complete) {
                    session.readReply();
                }
                break;
            default:
                LOG.info("RETR unexpected response: {} {}", expected.getCode(), expected.getText());
        }
    }

    private void processData(InputStream raw) {
        String fileName = remoteName;
        if (localName != null) {
            fileName = localName;
        }
        try {
            FileOutputStream fos = new FileOutputStream(new File(fileName), true);
            IOUtils.copy(raw, fos);
            LOG.info("ok");
        } catch (IOException e) {
            LOG.error("error writing file", e);
        }
    }
}
