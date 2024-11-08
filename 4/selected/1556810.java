package org.mhpbox.infra;

import java.awt.Color;

public class ChannelSelector {

    private ChannelCleanerThread thread;

    private int state;

    private StringBuffer chBuffer;

    private ChannelLabel lblChannel;

    private Integer channel;

    private boolean end;

    public ChannelSelector(ChannelLabel lbl) {
        this.lblChannel = lbl;
        this.chBuffer = new StringBuffer();
        this.channel = new Integer(0);
        this.thread = new ChannelCleanerThread();
        this.thread.start();
    }

    public ChannelLabel getChannelLabel() {
        return this.lblChannel;
    }

    public void setVisible(boolean bool) {
        this.lblChannel.setVisible(bool);
    }

    public void appendDigit(int dig) {
        synchronized (chBuffer) {
            chBuffer.append(dig);
            if (chBuffer.length() == 1) {
                this.state = 1;
                setText(chBuffer + "-");
            }
            if (chBuffer.length() == 2) {
                this.state = 2;
                String strCh = chBuffer.toString();
                this.chBuffer.setLength(0);
                setText(strCh);
                this.channel = new Integer(strCh);
                AbstractSTB.getInstance().selectService(channel);
            }
        }
    }

    private void setText(String str) {
        synchronized (chBuffer) {
            lblChannel.setText(str);
            lblChannel.setColor(Color.GREEN);
            lblChannel.setVisible(true);
            thread.countDown();
        }
    }

    private String getChannelFormated() {
        String str = (channel.intValue() < 10) ? "0" : "";
        return str + channel;
    }

    private void timeout() {
        synchronized (chBuffer) {
            chBuffer.setLength(0);
            switch(state) {
                case 1:
                    lblChannel.setText(getChannelFormated());
                    lblChannel.setColor(Color.RED);
                    lblChannel.setVisible(true);
                    thread.countDown();
                    state = 3;
                    break;
                case 2:
                    lblChannel.setVisible(false);
                    state = 0;
                    break;
                case 3:
                    lblChannel.setVisible(false);
                    state = 0;
                    break;
            }
        }
    }

    private class ChannelCleanerThread extends Thread {

        private boolean recount;

        private boolean end;

        public void countDown() {
            synchronized (chBuffer) {
                recount = true;
                chBuffer.notifyAll();
            }
        }

        public void end() {
            synchronized (chBuffer) {
                end = true;
                chBuffer.notifyAll();
            }
        }

        public void run() {
            synchronized (chBuffer) {
                while (end == false) {
                    try {
                        chBuffer.wait();
                        if (end) {
                            return;
                        }
                    } catch (InterruptedException e) {
                    }
                    do {
                        recount = false;
                        try {
                            chBuffer.wait(TVControls.TIMEOUT);
                        } catch (InterruptedException e) {
                        }
                        if (recount == false) {
                            timeout();
                        }
                    } while (recount);
                }
            }
        }
    }
}
