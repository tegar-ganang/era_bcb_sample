package com.primianotucci.jsmartcardexplorer;

import java.util.List;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

/**
 *
 * @author Primiano Tucci - http://www.primianotucci.com/
 */
public class SmartCard {

    String currentCardATR;

    String currentCard;

    CardTerminal reader;

    Card card;

    CardChannel chan;

    public String connect(String iProto) throws CardException {
        card = reader.connect(iProto);
        ATR atr = card.getATR();
        currentCardATR = StringUtil.byteArrToString(atr.getBytes(), " ");
        currentCard = CardList.lookupByAtr(atr.getBytes());
        chan = card.getBasicChannel();
        return currentCard;
    }

    public boolean isConnected() {
        return (card != null);
    }

    public void disconnect() throws CardException {
        if (card == null) return;
        card.disconnect(true);
        card = null;
    }

    public CardChannel getChannel() {
        return chan;
    }

    public String setReader(Object iReaderRef) throws CardException {
        reader = objectToReader(iReaderRef);
        if (reader == null) return "Null Object [" + iReaderRef + "]";
        if (!reader.isCardPresent()) return "No card present";
        return reader.getName();
    }

    CardTerminal objectToReader(Object iRef) {
        return TerminalFactory.getDefault().terminals().getTerminal(iRef.toString().substring(4));
    }

    public String[] listReaders() throws CardException {
        List<CardTerminal> tlist = TerminalFactory.getDefault().terminals().list();
        String[] outList = new String[tlist.size()];
        for (int i = 0; i < tlist.size(); i++) outList[i] = (tlist.get(i).isCardPresent() ? "[x] " : "[ ] ") + tlist.get(i).getName();
        return outList;
    }

    public String getCurrentCardATR() {
        return currentCardATR;
    }
}
