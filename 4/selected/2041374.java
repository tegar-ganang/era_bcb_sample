package bagaturchess.search.impl.uci_adaptor;

import java.io.IOException;
import bagaturchess.bitboard.impl.movegen.MoveInt;
import bagaturchess.search.api.internal.ISearchInfo;
import bagaturchess.search.api.internal.ISearchMediator;
import bagaturchess.search.api.internal.ISearchStopper;
import bagaturchess.uci.api.BestMoveSender;
import bagaturchess.uci.impl.Channel;
import bagaturchess.uci.impl.commands.Go;

public abstract class UCISearchMediatorImpl_Base implements ISearchMediator {

    private Channel channel;

    private Go goCommand;

    private int colourToMove;

    private ISearchStopper stopper;

    private BestMoveSender sender;

    private ISearchInfo lastinfo;

    private long startTime;

    public UCISearchMediatorImpl_Base(Channel _channel, Go _go, int _colourToMove, BestMoveSender _sender) {
        channel = _channel;
        goCommand = _go;
        colourToMove = _colourToMove;
        sender = _sender;
        startTime = System.currentTimeMillis();
    }

    protected long getStartTime() {
        return startTime;
    }

    protected Channel getChannel() {
        return channel;
    }

    public void startIteration(int iteration) {
    }

    public int getColourToMove() {
        return colourToMove;
    }

    public Go getGoCommand() {
        return goCommand;
    }

    public ISearchInfo getLastInfo() {
        return lastinfo;
    }

    void setLastInfo(ISearchInfo info) {
        lastinfo = info;
    }

    public void changedMinor(ISearchInfo info) {
        sendMinorMessage(info);
    }

    public void changedMajor(ISearchInfo info) {
        lastinfo = info;
        sendMajorMessage(info);
    }

    public ISearchStopper getStopper() {
        return stopper;
    }

    void setStoper(ISearchStopper _stopper) {
        stopper = _stopper;
    }

    public void dump(String msg) {
        channel.dump(msg);
    }

    public void dump(Throwable t) {
        channel.dump(t);
    }

    /**
	 * PRIVATE METHODS 
	 * 
	 */
    protected void sendMajorMessage(ISearchInfo info) {
        long time = (System.currentTimeMillis() - getStartTime());
        long nodes = info.getSearchedNodes();
        long timeInSecs = (time / 1000);
        if (timeInSecs == 0) {
            timeInSecs = 1;
        }
        String message = "";
        message += "info ";
        message += "depth " + info.getDepth();
        message += " seldepth " + info.getSelDepth();
        message += " time " + time;
        message += " nodes " + nodes;
        long nps = nodes / timeInSecs;
        if (nps > 1000) {
            message += " nps " + nps;
        }
        long eval = (int) info.getEval();
        if (info.isMateScore()) {
            message += " score mate " + info.getMateScore();
        } else {
            message += " score cp " + eval;
        }
        message += " hashfull " + 10 * info.getHashFull();
        String pv = "";
        if (info.getPV() != null) {
            for (int j = 0; j < info.getPV().length; j++) {
                pv += MoveInt.moveToStringUCI(info.getPV()[j]);
                if (j != info.getPV().length - 1) {
                    pv += " ";
                }
            }
        }
        message += " pv " + pv;
        send(message);
    }

    private void sendMinorMessage(ISearchInfo info) {
        long time = (System.currentTimeMillis() - info.getStartTime());
        long timeInSecs = (time / 1000);
        if (timeInSecs == 0) {
            timeInSecs = 1;
        }
        long nodes = info.getSearchedNodes();
        String message = "";
        message += "info ";
        message += " nodes " + info.getSearchedNodes();
        long nps = nodes / timeInSecs;
        if (nps > 1000) {
            message += " nps " + nps;
        }
        if (info.getCurrentMove() != 0) {
            message += " currmove " + MoveInt.moveToStringUCI(info.getCurrentMove());
            message += " currmovenumber " + info.getCurrentMoveNumber();
        }
        message += " hashfull " + 10 * info.getHashFull();
        send(message);
    }

    protected void send(String messageToGUI) {
        try {
            channel.sendCommandToGUI(messageToGUI);
        } catch (IOException e) {
            Channel.dump(e);
        }
    }

    public BestMoveSender getBestMoveSender() {
        return sender;
    }

    public void addSearchedNodes(long searchedNodes) {
        throw new IllegalStateException();
    }
}
