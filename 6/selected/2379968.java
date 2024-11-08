package ru.bluesteel.j2me.lj;

import java.util.Hashtable;
import java.io.IOException;
import ru.bluesteel.utils.UTFDecoder;

/**
 * User: Administrator Date: 21.10.2004 Time: 22:11:00
 */
public class LJNetworkRunner extends Thread {

    public static final int LOGIN = 1, POST = 2, LOAD_LAST_ENTRY = 3, EDIT = 4;

    LJConnector connector;

    private SoliloquyMidlet midlet;

    private boolean goOn;

    private int command;

    public boolean isRunning() {
        return running;
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

    private boolean running;

    public void reInit() {
        connector.setHost(midlet.getSettings().useSpecific ? midlet.getSettings().specificHostname : (midlet.getSettings().useIp ? midlet.getAppProperty("Default-IP") : midlet.getAppProperty("Default-Hostname")));
    }

    public LJNetworkRunner(SoliloquyMidlet midlet) {
        this.midlet = midlet;
        command = 0;
        running = false;
        goOn = true;
        String host = (midlet.getSettings().useSpecific ? midlet.getSettings().specificHostname : (midlet.getSettings().useIp ? midlet.getAppProperty("Default-IP") : midlet.getAppProperty("Default-Hostname")));
        connector = new LJConnectorImplME(host);
    }

    public synchronized void run() {
        while (goOn) {
            if (goOn && command != 0) {
                execute();
            }
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        setRunning(false);
    }

    public synchronized void doCommand(int command) {
        this.command = command;
        if (!isRunning()) {
            setRunning(true);
            this.start();
        } else notify();
    }

    public synchronized void repeatCommand() {
        if (!isRunning()) {
            setRunning(true);
            this.start();
        } else notify();
    }

    private void execute() {
        switch(command) {
            case LOGIN:
                doLogin();
                break;
            case POST:
                doCommand(LJConnector.MODE_POST, "Posting event...");
                break;
            case LOAD_LAST_ENTRY:
                doCommand(LJConnector.MODE_LOAD_ENTRY, "Loading last entry...");
                break;
            case EDIT:
                doCommand(LJConnector.MODE_EDIT_EVENT, "Posting edited event...");
                break;
            default:
                midlet.displayError("Error", "Unknown command");
                break;
        }
    }

    private void doLogin() {
        if ((!midlet.getSettings().loginNow) && (midlet.getSettings().autoLogin)) {
            midlet.displayAttributesForm();
            return;
        }
        midlet.setGaugeForm("Logging in...");
        connector.connect(midlet.params);
        Hashtable response;
        try {
            response = connector.login();
        } catch (IOException e) {
            midlet.displayError("Logging error", "Error during sending data via GPRS");
            return;
        }
        if (response.get("success").equals("OK")) {
            if (response.get("access_count") != null) {
                int journalsCount = Integer.parseInt((String) response.get("access_count")) + 1;
                midlet.journals = new String[journalsCount];
                midlet.journals[0] = UTFDecoder.decode((String) response.get("name"));
                for (int i = 1; i < journalsCount; i++) {
                    midlet.journals[i] = (String) response.get("access_" + String.valueOf(i));
                }
            } else {
                midlet.journals = new String[1];
                midlet.journals[0] = UTFDecoder.decode((String) response.get("name"));
            }
            if (response.get("pickw_count") != null) {
                int picsCount = Integer.parseInt((String) response.get("pickw_count"));
                if (picsCount > 0) {
                    midlet.userpics = new String[picsCount];
                    for (int i = 0; i < picsCount; i++) {
                        midlet.userpics[i] = UTFDecoder.decode((String) response.get("pickw_" + String.valueOf(i + 1)));
                    }
                }
            } else {
                midlet.userpics = null;
            }
        } else {
            midlet.displayError("Logging error", (String) response.get("errmsg"));
            return;
        }
        midlet.displayAttributesForm();
    }

    private void doCommand(int mode, String gaugeMessage) {
        midlet.setGaugeForm(gaugeMessage);
        String action = "";
        try {
            Hashtable response = null;
            switch(mode) {
                case LJConnector.MODE_POST:
                    action = "Posting";
                    response = connector.post(midlet.post);
                    break;
                case LJConnector.MODE_LOAD_ENTRY:
                    action = "Loading entry";
                    response = connector.loadLastEntry(midlet.post);
                    break;
                case LJConnector.MODE_EDIT_EVENT:
                    action = "Posting edited event";
                    response = connector.edit(midlet.post);
                    break;
            }
            String status = (String) response.get("success");
            if (status == null || !status.equals("OK")) {
                String errMsg = (String) response.get("errmsg");
                midlet.displayError("Error", (errMsg == null) ? "Unknown Error" : errMsg);
            } else {
                midlet.displaySuccess(action + " success");
            }
        } catch (IOException e) {
            midlet.displayError(action + " failure", "Error during sending data");
        }
    }
}
