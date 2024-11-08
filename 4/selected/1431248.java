package bagaturchess.search.impl.uci_adaptor;

import bagaturchess.uci.api.BestMoveSender;
import bagaturchess.uci.impl.Channel;
import bagaturchess.uci.impl.commands.Go;

public class UCISearchMediatorImpl_OpponentPondering extends UCISearchMediatorImpl_Base {

    public UCISearchMediatorImpl_OpponentPondering(Channel _channel, Go _go, int _colourToMove, BestMoveSender _sender) {
        super(_channel, _go, _colourToMove, _sender);
        setStoper(new PonderingStopper());
    }

    @Override
    protected void send(String messageToGUI) {
        getChannel().sendLogToGUI(messageToGUI);
    }
}
