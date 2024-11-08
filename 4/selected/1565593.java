package gov.sns.apps.diagnostics.corede.corede;

import java.util.*;
import gov.sns.ca.*;

public class PVTrigger extends AbstractTrigger {

    private int period = -1;

    private Timer localTimer;

    private Channel trigChannel;

    private TimerTask localTimerTask;

    private IEventSinkValue ivalm;

    private Boolean isRunning = false;

    public PVTrigger(String n) {
        super(n);
        trigChannel = Utilities.getChannelFactory().getChannel(n);
        trigChannel.connect();
        ivalm = new IEventSinkValue() {

            public void eventValue(ChannelRecord p1, Channel p2) {
                synchronized (isRunning) {
                    if (isRunning) {
                        Dbg.println("Firing event: " + eventID);
                        processJob();
                    }
                }
            }
        };
        try {
            trigChannel.addMonitorValue(ivalm, 1);
        } catch (Exception e) {
            Dbg.warnln(e.getMessage());
        }
    }

    @Override
    protected void startMe(boolean r) {
        if (r) {
            synchronized (isRunning) {
                isRunning = true;
            }
        } else {
            synchronized (isRunning) {
                isRunning = false;
            }
        }
    }
}
