package bagaturchess.search.impl.uci_adaptor;

import bagaturchess.bitboard.impl.movelist.BaseMoveList;
import bagaturchess.search.impl.info.SearchInfoImpl;
import bagaturchess.search.impl.uci_adaptor.timemanagement.ITimeController;
import bagaturchess.search.impl.uci_adaptor.timemanagement.TimeControllerFactory;
import bagaturchess.search.impl.utils.DEBUGSearch;
import bagaturchess.uci.api.BestMoveSender;
import bagaturchess.uci.impl.Channel;
import bagaturchess.uci.impl.commands.Go;

public class UCISearchAdaptorImpl_PonderingOpponentMove extends UCISearchAdaptorImpl_Base {

    private int revertedMoveForPondering;

    public UCISearchAdaptorImpl_PonderingOpponentMove(Object[] args) {
        super(args);
    }

    @Override
    public synchronized void goSearch(Channel channel, BestMoveSender sender, Go go) {
        if (currentMediator != null) throw new IllegalStateException("mediator is not null");
        int colourToMove = boardForSetup.getColourToMove();
        if (isPonderSearch(go)) {
            if (DEBUGSearch.DEBUG_MODE) {
                if (currentGoCommand != null && currentGoCommand.isPonder()) {
                    throw new IllegalStateException("currentGoCommand != null && currentGoCommand.isPonder()");
                }
            }
            revertedMoveForPondering = boardForSetup.getLastMove();
            boardForSetup.makeMoveBackward(revertedMoveForPondering);
            currentMediator = new UCISearchMediatorImpl_OpponentPondering(channel, go, boardForSetup.getColourToMove(), sender);
            currentGoCommand = go;
            goSearch(true);
        } else {
            if (currentGoCommand != null) {
                if (currentGoCommand.isPonder()) {
                    BaseMoveList list = new BaseMoveList();
                    boardForSetup.genAllMoves(list);
                    int dummyMoveToPrevenIllegalMoveFromGUI = list.next();
                    SearchInfoImpl info = (SearchInfoImpl) currentMediator.getLastInfo();
                    info.setBestMove(dummyMoveToPrevenIllegalMoveFromGUI);
                    info.setPV(new int[] { dummyMoveToPrevenIllegalMoveFromGUI });
                    sender.sendBestMove();
                } else {
                    if (DEBUGSearch.DEBUG_MODE) throw new IllegalStateException("currentGoCommand.isPonder");
                }
            }
            ITimeController timeController = TimeControllerFactory.createTimeController(searchAdaptorCfg.getTimeConfig(), boardForSetup.getColourToMove(), go);
            currentMediator = new UCISearchMediatorImpl_NormalSearch(channel, go, timeController, colourToMove, sender);
            currentGoCommand = go;
            goSearch(false);
        }
    }

    @Override
    public synchronized void ponderHit() {
        if (DEBUGSearch.DEBUG_MODE) {
            if (!searchAdaptorCfg.isPonderingEnabled()) {
                throw new IllegalStateException("searchAdaptorCfg.isPonderingEnabled() = " + searchAdaptorCfg.isPonderingEnabled());
            }
            if (currentGoCommand == null) {
                throw new IllegalStateException("currentGoCommand == null");
            }
            if (!currentGoCommand.isPonder()) {
                throw new IllegalStateException("!currentGoCommand.isPonder()");
            }
        }
        UCISearchMediatorImpl_OpponentPondering ponderMediator = (UCISearchMediatorImpl_OpponentPondering) currentMediator;
        if (currentGoCommand != null) {
            if (currentGoCommand.isPonder()) {
                stopSearch();
            } else {
                if (DEBUGSearch.DEBUG_MODE) throw new IllegalStateException("currentGoCommand.isPonder");
            }
        } else {
            if (DEBUGSearch.DEBUG_MODE) throw new IllegalStateException("currentGoCommand != null");
        }
        boardForSetup.makeMoveForward(revertedMoveForPondering);
        Go go = ponderMediator.getGoCommand();
        go.setPonder(false);
        ITimeController timeController = TimeControllerFactory.createTimeController(searchAdaptorCfg.getTimeConfig(), boardForSetup.getColourToMove(), go);
        currentMediator = new UCISearchMediatorImpl_NormalSearch(ponderMediator.getChannel(), go, timeController, boardForSetup.getColourToMove(), ponderMediator.getBestMoveSender());
        currentGoCommand = go;
        goSearch(false);
    }
}
