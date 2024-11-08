package bagaturchess.search.impl.uci_adaptor;

import bagaturchess.search.impl.uci_adaptor.timemanagement.ITimeController;
import bagaturchess.search.impl.uci_adaptor.timemanagement.TimeControllerFactory;
import bagaturchess.search.impl.uci_adaptor.timemanagement.controllers.TimeController_IncrementPerMove;
import bagaturchess.search.impl.utils.DEBUGSearch;
import bagaturchess.search.impl.utils.SearchMediatorProxy;
import bagaturchess.uci.api.BestMoveSender;
import bagaturchess.uci.impl.Channel;
import bagaturchess.uci.impl.commands.Go;

public class UCISearchAdaptorImpl_PonderingUCIStandard extends UCISearchAdaptorImpl_Base {

    public UCISearchAdaptorImpl_PonderingUCIStandard(Object[] args) {
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
            currentMediator = new SearchMediatorProxy(new UCISearchMediatorImpl_StandardPondering(channel, go, colourToMove, sender));
            currentGoCommand = go;
            goSearch(true);
        } else {
            if (currentGoCommand != null) {
                if (currentGoCommand.isPonder()) {
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
            if (currentGoCommand == null) {
                throw new IllegalStateException("currentGoCommand == null");
            }
            if (!currentGoCommand.isPonder()) {
                throw new IllegalStateException("!currentGoCommand.isPonder()");
            }
        }
        UCISearchMediatorImpl_StandardPondering ponderMediator = (UCISearchMediatorImpl_StandardPondering) ((SearchMediatorProxy) currentMediator).getParent();
        Go go = ponderMediator.getGoCommand();
        go.setPonder(false);
        ITimeController timeController = TimeControllerFactory.createTimeController(searchAdaptorCfg.getTimeConfig(), boardForSetup.getColourToMove(), go);
        UCISearchMediatorImpl_NormalSearch switchedMediator = new UCISearchMediatorImpl_NormalSearch(ponderMediator.getChannel(), go, timeController, ponderMediator.getColourToMove(), ponderMediator.getBestMoveSender());
        switchedMediator.setLastInfo(ponderMediator.getLastInfo());
        ((SearchMediatorProxy) currentMediator).setParent(switchedMediator);
    }
}
