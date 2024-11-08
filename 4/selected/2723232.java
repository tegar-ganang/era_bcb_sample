package socksviahttp.client.engine;

import java.net.*;
import java.io.*;
import java.util.zip.*;
import socksviahttp.core.consts.*;
import common.log.*;
import socksviahttp.core.net.*;
import socksviahttp.core.util.*;
import socksviahttp.client.net.*;

public abstract class ThreadCommunication extends Thread {

    protected ServerInfo server = null;

    protected Connection source = null;

    protected String id_conn = null;

    protected String destinationUri = null;

    protected Configuration configuration = null;

    protected boolean requestOnlyIfClientActivity = false;

    public ThreadCommunication() {
        super();
    }

    public abstract boolean init();

    public void run() {
        if (!init()) {
            configuration.printlnError("<CLIENT> Disconnecting application");
            source.disconnect();
            return;
        }
        boolean dialogInProgress = true;
        byte[] line;
        long initialTime = new java.util.Date().getTime();
        long lastUpdateTime = initialTime;
        long lastDataReceivedTime = initialTime;
        long lastDataSentTime = initialTime;
        ThreadFileWriter tfdClient = null;
        ThreadFileWriter tfdServer = null;
        if ((configuration.getSpyMode() == Configuration.SPY_MODE_CLIENT) || (configuration.getSpyMode() == Configuration.SPY_MODE_BOTH)) {
            tfdClient = new ThreadFileWriter(id_conn + "_svhc_fromapp.log");
            if (tfdClient.init()) tfdClient.start(); else tfdClient = null;
        }
        if ((configuration.getSpyMode() == Configuration.SPY_MODE_SERVER) || (configuration.getSpyMode() == Configuration.SPY_MODE_BOTH)) {
            tfdServer = new ThreadFileWriter(id_conn + "_svhc_toapp.log");
            if (tfdServer.init()) tfdServer.start(); else tfdServer = null;
        }
        while (dialogInProgress == true) {
            try {
                line = source.read();
                long now = new java.util.Date().getTime();
                if (now - initialTime > configuration.getDontTryToMinimizeTrafficBefore()) requestOnlyIfClientActivity = configuration.isRequestOnlyIfClientActivity();
                boolean forceRequest = (now > configuration.getForceRequestAfter() + lastUpdateTime);
                if (configuration.getForceRequestAfter() == 0) forceRequest = false;
                boolean requestBecauseDataReceived = (now < lastDataReceivedTime + configuration.getContinueRequestingAfterDataReceivedDuring());
                boolean requestBecauseDataSent = (now < lastDataSentTime + configuration.getContinueRequestingAfterDataSentDuring());
                if ((!requestOnlyIfClientActivity) || (forceRequest) || (requestBecauseDataReceived) || (requestBecauseDataSent) || (line == null) || (line.length > 0)) {
                    lastUpdateTime = new java.util.Date().getTime();
                    DataPacket dataPacket = new DataPacket();
                    dataPacket.id = id_conn;
                    if (line == null) {
                        configuration.printlnInfo("<CLIENT> Application closed the connection");
                        configuration.printlnInfo("<CLIENT> " + server.getServerName() + ", close the connection " + id_conn);
                        requestOnlyIfClientActivity = false;
                        dataPacket.type = Const.CONNECTION_DESTROY;
                        dataPacket.tab = Const.TAB_EMPTY;
                    } else {
                        dataPacket.type = Const.CONNECTION_REQUEST;
                        dataPacket.tab = line;
                        if ((configuration.getSpyMode() == Configuration.SPY_MODE_CLIENT) || (configuration.getSpyMode() == Configuration.SPY_MODE_BOTH)) {
                            if (tfdClient != null) {
                                if (line.length > 0) tfdClient.addLogMessage(new LogMessage(line));
                            }
                        }
                        if (line.length > 0) {
                            lastDataSentTime = new java.util.Date().getTime();
                        }
                    }
                    dataPacket.zipData = server.isZipData();
                    dataPacket.encryptData = server.isEncryptData();
                    dataPacket.encryptionKey = server.getEncryptionKey().getBytes();
                    boolean packetTransmitted = false;
                    int retry = 0;
                    DataPacket response = null;
                    while ((!packetTransmitted) && (retry < 1 + configuration.getMaxRetries())) {
                        try {
                            response = sendHttpMessage(configuration, server, dataPacket);
                            packetTransmitted = true;
                        } catch (Exception e) {
                            retry++;
                            configuration.printlnWarn("<CLIENT> Cannot reach " + server.getServerName() + " (try #" + retry + "). Exception : " + e);
                            Thread.sleep(configuration.getDelayBetweenTries());
                        }
                    }
                    if (retry == 1 + configuration.getMaxRetries()) {
                        configuration.printlnError("<CLIENT> The maximum number of retries has been done");
                        configuration.printlnError("<CLIENT> Disconnecting application");
                        source.disconnect();
                        dialogInProgress = false;
                        if (tfdClient != null) tfdClient.shutdown();
                        if (tfdServer != null) tfdServer.shutdown();
                        return;
                    }
                    if (response.errorCode != 0) {
                        configuration.printlnError("<CLIENT> CRC Error. Check your secret encryption key");
                        configuration.printlnError("<CLIENT> Disconnecting application");
                        source.disconnect();
                        dialogInProgress = false;
                        if (tfdClient != null) tfdClient.shutdown();
                        if (tfdServer != null) tfdServer.shutdown();
                        return;
                    }
                    switch(response.type) {
                        case Const.CONNECTION_WRONG_ENCRYPTION_KEY:
                            String serverMessage = new String(response.tab);
                            configuration.printlnError("<" + server.getServerName() + "> " + serverMessage);
                            configuration.printlnInfo("<CLIENT> Disconnecting application");
                            source.disconnect();
                            dialogInProgress = false;
                            break;
                        case Const.CONNECTION_RESPONSE:
                            if (tfdServer != null) {
                                if (response.tab.length > 0) tfdServer.addLogMessage(new LogMessage(response.tab));
                            }
                            if (response.tab.length > 0) {
                                lastDataReceivedTime = new java.util.Date().getTime();
                                source.write(response.tab);
                            }
                            break;
                        case Const.CONNECTION_NOT_FOUND:
                            configuration.printlnError("<" + server.getServerName() + "> Connection not found : " + id_conn);
                            break;
                        case Const.CONNECTION_DESTROY_OK:
                            configuration.printlnInfo("<" + server.getServerName() + "> As CLIENT asked, connection closed : " + id_conn);
                            break;
                        default:
                            configuration.printlnWarn("<CLIENT> " + server.getServerName() + " sent an unexpected response type : " + response.type);
                            break;
                    }
                    if (response.isConnClosed) {
                        configuration.printlnInfo("<" + server.getServerName() + "> Remote server closed the connection : " + response.id);
                        configuration.printlnInfo("<CLIENT> Disconnecting application");
                        source.disconnect();
                        dialogInProgress = false;
                    }
                    if (response.type == Const.CONNECTION_DESTROY_OK) {
                        configuration.printlnInfo("<CLIENT> Disconnecting application");
                        source.disconnect();
                        dialogInProgress = false;
                    }
                    if (response.type == Const.CONNECTION_NOT_FOUND) {
                        configuration.printlnError("<CLIENT> Disconnecting application");
                        source.disconnect();
                        dialogInProgress = false;
                    }
                }
                if (dialogInProgress) Thread.sleep(configuration.getDelay());
            } catch (Exception e) {
                configuration.printlnError("<CLIENT> Unexpected Exception : " + e);
            }
        }
        if (tfdClient != null) tfdClient.shutdown();
        if (tfdServer != null) tfdServer.shutdown();
    }

    public static DataPacket sendHttpMessage(Configuration config, ServerInfo server, DataPacket source) throws IOException {
        HttpMessage mess = new HttpMessage(server.getUrl());
        if (config.isProxyNeedsAuthentication()) {
            mess.setProxyAuthorization(config.getProxyUser(), config.getProxyPassword());
        }
        byte[] serialized = source.saveToByteArray();
        InputStream is = mess.sendByteArrayInPostMessage(source.saveToByteArray());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] tmpBuffer = new byte[65536];
        int n;
        while ((n = is.read(tmpBuffer)) >= 0) baos.write(tmpBuffer, 0, n);
        is.close();
        DataPacket ret = new DataPacket();
        ret.encryptionKey = server.getEncryptionKey().getBytes();
        ret.loadFromByteArray(baos.toByteArray());
        return (ret);
    }
}
