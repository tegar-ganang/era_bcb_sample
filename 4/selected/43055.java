package charismata.broadcast;

public class ChannelWorker extends Thread {

    private boolean runWorker = true;

    private boolean busy = true;

    public void run() {
        while (isRunning()) {
            setBusy(true);
            ChannelQueue queue = ChannelQueue.getInstance();
            BroadcastInfo bi = null;
            while ((bi = queue.receive()) != null) {
                Channel channel = bi.getChannel();
                channel.broadcast(bi);
            }
            setBusy(false);
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                while (Thread.interrupted()) {
                }
            }
        }
    }

    public synchronized boolean isRunning() {
        return runWorker;
    }

    public synchronized boolean isBusy() {
        return busy;
    }

    public synchronized void setBusy(boolean busy) {
        this.busy = busy;
    }
}
