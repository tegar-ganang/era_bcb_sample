package com.pbxworkbench.pbx.mock;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asteriskjava.fastagi.AgiException;
import com.pbxworkbench.commons.CampaignRuntimeException;
import com.pbxworkbench.pbx.CallParameters;
import com.pbxworkbench.pbx.IChannelAddress;
import com.pbxworkbench.pbx.IChannelApplet;
import com.pbxworkbench.pbx.IPbxCall;
import com.pbxworkbench.pbx.IPbxCallObserver;
import com.pbxworkbench.pbx.IPbxService;

public class MockPbxService implements IPbxService {

    private static final Log LOG = LogFactory.getLog(MockPbxService.class);

    private BlockingQueue<MyCall> callQueue = new LinkedBlockingQueue<MyCall>();

    private long callDuration = 2000;

    private long callOriginationDuration = 1500;

    public MockPbxService(long callDuration) {
        this.callDuration = callDuration;
        Thread thread = new Thread(new MyWorker());
        thread.start();
    }

    public IPbxCall createCall(CallParameters callParameters) {
        return new MyCall(callParameters);
    }

    private class MyWorker implements Runnable {

        public void run() {
            while (true) {
                try {
                    MyCall call = callQueue.take();
                    LOG.debug("take a new call");
                    process(call);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    protected void process(MyCall call) {
        call.start();
        autoAnswerCall(call);
    }

    private void autoAnswerCall(final MyCall call) {
        TimerTask autoAnswerCall = new TimerTask() {

            @Override
            public void run() {
                call.answer();
                try {
                    Thread.sleep(callDuration);
                } catch (InterruptedException e) {
                }
                call.hangup();
            }
        };
        Timer timer = new Timer();
        timer.schedule(autoAnswerCall, 0);
    }

    protected class MyCall implements IPbxCall {

        private IPbxCallObserver observer;

        private CallParameters callParameters;

        public MyCall(CallParameters callParameters) {
            this.callParameters = callParameters;
        }

        public void start() {
            observer.onCallStart();
            try {
                Thread.sleep(callOriginationDuration);
            } catch (InterruptedException e) {
            }
        }

        public void answer() {
            observer.onAnswer();
            try {
                getChannelApplet().run(new MockRequest(), new MockChannel(getAddress()));
            } catch (AgiException e) {
                LOG.debug("exception while running mock channel applet", e);
            }
        }

        private IChannelApplet getChannelApplet() {
            return callParameters.getChannelApplet();
        }

        private IChannelAddress getAddress() {
            return callParameters.getChannelAddress();
        }

        public void hangup() {
            observer.onHangup();
        }

        public void addObserver(IPbxCallObserver observer) {
            this.observer = observer;
        }

        public void connect() {
            try {
                callQueue.put(this);
            } catch (InterruptedException e) {
                LOG.debug(e);
                throw new CampaignRuntimeException("could not connect call", e);
            }
        }
    }
}
