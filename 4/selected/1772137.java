package ibcontroller;

import javax.swing.JFrame;
import javax.swing.JMenuItem;

class StopTask implements Runnable {

    private static SwitchLock _Running = new SwitchLock();

    private final boolean mGateway;

    private final CommandChannel mChannel;

    public StopTask(boolean gateway, final CommandChannel channel) {
        this.mGateway = gateway;
        mChannel = channel;
    }

    public void run() {
        if (!_Running.set()) {
            writeNack("STOP already in progress");
            return;
        }
        try {
            writeInfo("Closing IBController");
            stop((mGateway) ? "Close" : "Exit");
        } catch (Exception ex) {
            writeNack(ex.getMessage());
        } finally {
            _Running.clear();
        }
    }

    private void stop(String stopCommand) {
        JFrame jf = TwsListener.getMainWindow();
        if (jf == null) {
            Utils.logToConsole("main window not yet found");
            writeNack("main window not yet found");
            return;
        }
        JMenuItem jmi = Utils.findMenuItem(jf, new String[] { "File", stopCommand });
        if (jmi == null) {
            System.err.println("IBController: Could not find File > " + stopCommand + " menu.");
            writeNack("File > " + stopCommand + " menu not found");
            return;
        }
        writeAck("");
        jmi.doClick();
    }

    private void writeAck(String message) {
        if (!(mChannel == null)) mChannel.writeAck(message);
    }

    private void writeInfo(String message) {
        if (!(mChannel == null)) mChannel.writeInfo(message);
    }

    private void writeNack(String message) {
        if (!(mChannel == null)) mChannel.writeNack(message);
    }
}
