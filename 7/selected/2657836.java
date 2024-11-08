package de.robowars.ui;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.*;
import de.robowars.comm.transport.CardSelection;
import de.robowars.ui.config.ButtonEnumeration;
import de.robowars.ui.config.ShapeEnumeration;
import de.robowars.ui.config.StaticEnumeration;
import de.robowars.ui.event.CardEventListener;
import de.robowars.ui.event.EventController;
import de.robowars.ui.event.EventFromServerAdaptor;
import de.robowars.ui.resource.*;

/**
 * This class holds two cardSets, one for chosen programCards and one that represents
 * the cardSet submitted by the server
 * 
 * @author Stefan Henze
 */
public class CardSet extends EventFromServerAdaptor implements ActionListener {

    private static CardSet _instance = null;

    private org.apache.log4j.Category log;

    private Card[] codeSet, programSet;

    private int numCodeSet, playerId, round, timeout;

    private boolean isAnimating = false;

    private JLayeredPane mainPane, codeSetPane, programSetPane;

    private StaticLayer programBack, codeBack;

    private boolean powerDown = false;

    private StaticLayer lPowerDown, lPowerDownOn, lPowerDownOff;

    private ButtonLayer bPowerDown, bCardsSelected;

    private final int CARD_TOP = 15;

    private final int CARD_HEIGHT = 104;

    private final int CARD_WIDTH = 66;

    private final double CARD_SHIFT = 71.5;

    private final int PROGRAM_SHIFT = 0;

    private final int CODE_SHIFT = 369;

    private int programTop = 605, codeTop = 641;

    private CardSet(JPanel mainPanel) {
        log = org.apache.log4j.Category.getInstance(CardSet.class);
        log.debug("initializing");
        codeSet = new Card[9];
        programSet = new Card[5];
        EventController.getInstance().addIssueCardsListener(this);
        mainPane = ((GameDialog) mainPanel).getMainPane();
        if (mainPane == null) log.error("mainPane is NULL");
        try {
            createPanes();
            createBackgrounds();
            mainPane.add(programSetPane, new Integer(51));
            mainPane.add(codeSetPane, new Integer(52));
            createButtons();
        } catch (ResourceException e) {
            log.error(e, e);
        }
        log.debug("initializing done");
    }

    /**
	 * Method getInstance creates and returns the instance of the cardSet
	 * @param mainPanel - main window panel to draw the cardSet to
	 * @return CardSet
	 */
    public static CardSet getInstance(JPanel mainPanel) {
        if (_instance == null) {
            _instance = new CardSet(mainPanel);
        }
        return _instance;
    }

    /**
	 * Method getInstance returns the existing cardSet (otherwise returns null)
	 * @return CardSet
	 */
    public static CardSet getInstance() {
        return _instance;
    }

    private void createPanes() {
        programSetPane = new JLayeredPane();
        programSetPane.setBounds(0, programTop, 368, 163);
        programSetPane.setDoubleBuffered(true);
        codeSetPane = new JLayeredPane();
        codeSetPane.setBounds(369, codeTop, 656, 127);
        codeSetPane.setDoubleBuffered(true);
    }

    private void createBackgrounds() throws ResourceException {
        LayerFactory fact = LayerFactory.getInstance();
        programBack = fact.createStaticLayer(StaticEnumeration.CARDS_LEFT_NEW);
        programBack.setBounds(0, 0, 368, 163);
        codeBack = fact.createStaticLayer(StaticEnumeration.CARDS_RIGHT_NEW);
        codeBack.setBounds(0, 0, 656, 127);
    }

    private void createButtons() throws ResourceException {
        LayerFactory fact = LayerFactory.getInstance();
        bCardsSelected = fact.createButtonLayer(ButtonEnumeration.SUBMIT_CARDS);
        bCardsSelected.setBounds(5, 15, 101, 27);
        bCardsSelected.addActionListener(DialogControl.getInstance());
        bCardsSelected.setActionCommand("game:sendcards");
        bCardsSelected.addMouseListener(bCardsSelected);
        bPowerDown = fact.createButtonLayer(ButtonEnumeration.POWER_DOWN);
        bPowerDown.setBounds(106, 15, 91, 27);
        bPowerDown.addActionListener(DialogControl.getInstance());
        bPowerDown.setActionCommand("game:powerdown");
        bPowerDown.addMouseListener(bPowerDown);
        lPowerDownOn = fact.createStaticLayer(StaticEnumeration.POWER_ON);
        lPowerDownOff = fact.createStaticLayer(StaticEnumeration.POWER_OFF);
        lPowerDownOn.setBounds(202, 5, 47, 47);
        lPowerDownOff.setBounds(202, 5, 47, 47);
        lPowerDown = lPowerDownOn;
    }

    private void addButtons() {
        programSetPane.add(bCardsSelected, new Integer(10));
        programSetPane.add(bPowerDown, new Integer(10));
        programSetPane.add(lPowerDown, new Integer(20));
    }

    /**
	 * Called by the EventController
	 * 
	 * @see de.robowars.ui.event.EventFromServerListener#issueCards(CardSet)
	 */
    public void issueCards(de.robowars.comm.transport.CardSet cards) {
        numCodeSet = cards.getCardsDealt();
        setAnimating(false);
        playerId = cards.getPlayerId();
        round = cards.getRound();
        timeout = cards.getTimeout();
        List cardList = new Vector();
        Iterator cardIterator = null;
        TreeSet treeSet = new TreeSet(new CardComparator());
        treeSet.addAll(cards.getCard());
        cardList.addAll(treeSet);
        int i;
        cardIterator = cardList.iterator();
        for (i = 0; cardIterator.hasNext(); i++) {
            de.robowars.comm.transport.Card cardTemp = (de.robowars.comm.transport.Card) cardIterator.next();
            codeSet[i] = new Card(i, cardTemp.getPriority(), cardTemp.getCardType());
        }
        while (i < 9) {
            codeSet[i] = null;
            i++;
        }
        for (i = 0; i < 5; i++) {
            programSet[i] = null;
        }
        setAnimating(false);
        if (powerDown && numCodeSet > 0) togglePowerDown();
        drawCards();
        Point robotPos = PlayerInfo.getInstance(playerId).getRobotPosition();
    }

    public void drawCards() {
        int i;
        log.debug("drawing Cards");
        programSetPane.removeAll();
        codeSetPane.removeAll();
        programSetPane.add(programBack, new Integer(0));
        codeSetPane.add(codeBack, new Integer(0));
        addButtons();
        if (!isAnimating) {
            setPositions(codeSet);
            for (i = 0; i < 9; i++) {
                if (codeSet[i] != null) {
                    codeSet[i].addCard(codeSetPane, "codeSetPane", i);
                }
            }
        }
        setPositions(programSet);
        for (i = 0; i < 5; i++) {
            if (programSet[i] != null) {
                programSet[i].addCard(programSetPane, "programSetPane", i);
            }
        }
    }

    private void setPositions(Card[] set) {
        int shift = 12, extratop = 0;
        if (set == programSet) {
            shift = 5;
            extratop = 36;
        }
        for (int i = 0; i < set.length; i++) {
            if (set[i] != null) {
                log.debug("position " + CARD_TOP + ", " + (shift + (i * (CARD_SHIFT))));
                set[i].setPosition((int) java.lang.Math.round((shift + (i * (CARD_SHIFT)))), CARD_TOP + extratop);
            }
        }
    }

    /**
	 * Method setAnimating sets animation mode (right cardSet not visible)
	 * @param isAnimating
	 */
    public void setAnimating(boolean isAnimating) {
        if (this.isAnimating != isAnimating) {
            if (isAnimating) {
                bCardsSelected.removeActionListener(DialogControl.getInstance());
                bCardsSelected.setEnabled(false);
                bPowerDown.removeActionListener(DialogControl.getInstance());
                bPowerDown.setEnabled(false);
                codeSetPane.setBounds(369, codeTop + 120, 656, 127);
                for (int i = 0; i < programSet.length; i++) {
                    programSet[i].active(false);
                }
            } else {
                codeSetPane.setBounds(369, codeTop, 656, 127);
                for (int i = 0; i < programSet.length; i++) {
                    programSet[i].active(true);
                }
                bCardsSelected.addActionListener(DialogControl.getInstance());
                bCardsSelected.setEnabled(true);
                bPowerDown.addActionListener(DialogControl.getInstance());
                bPowerDown.setEnabled(true);
            }
        }
        this.isAnimating = isAnimating;
        drawCards();
    }

    /**
	 * Method getAnimating.
	 * @return boolean
	 */
    public boolean getAnimating() {
        return isAnimating;
    }

    /**
	 * Method addCardToProgram.
	 * @param card: Index of a card in the codeSet[]
	 * @return boolean
	 */
    private boolean addCardToProgram(int card) {
        if (codeSet[card].isBlocked() || codeSet[card].isSelected()) return false;
        int i = 0;
        while (i < 5 && programSet[i] != null) {
            i++;
        }
        if (i == 5) {
            log.debug(" -- no free slot found");
            return false;
        }
        programSet[i] = new Card(i, codeSet[card].getPriotity(), codeSet[card].getType(), 0, 0, false, false);
        setPositions(programSet);
        programSet[i].addCard(programSetPane, "programSetPane", i);
        codeSet[card].select(true);
        log.debug(" -- added to " + i);
        return true;
    }

    /**
	 * Method removeCardFromProgram.
	 * @param card to remove
	 * @return success
	 */
    private boolean removeCardFromProgram(int card) {
        log.debug("starting removeCardFromProgram.");
        if (programSet[card].isBlocked()) return false;
        log.debug("searching for card from program set in card set.");
        int i = 0;
        while (i < 9 && codeSet[i].getPriotity() != programSet[card].getPriotity()) {
            i++;
        }
        if (i == 9) {
            log.debug(" -- card not found");
            return false;
        } else log.debug(" -- found card at [" + i + "]");
        log.debug("removing card from programm card set.");
        programSet[card].removeCard(programSetPane);
        programSet[card] = null;
        codeSet[i].select(false);
        i = card;
        while (i < 4) {
            programSet[i] = programSet[i + 1];
            programSet[i + 1] = null;
            i++;
        }
        drawCards();
        return true;
    }

    /**
	 * Method togglePowerDown switches the own robot on and off
	 */
    public void togglePowerDown() {
        if (powerDown) {
            powerDown = false;
            programSetPane.remove(lPowerDown);
            lPowerDown = lPowerDownOff;
            programSetPane.add(lPowerDown, new Integer(20));
        } else {
            powerDown = true;
            programSetPane.remove(lPowerDown);
            lPowerDown = lPowerDownOn;
            programSetPane.add(lPowerDown, new Integer(20));
        }
    }

    /**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        log.debug("Card Command: " + command);
        int nr = Integer.parseInt(Character.toString(command.charAt(command.length() - 1)));
        if (command.matches("codeSetPane:[0-8]")) {
            if (!addCardToProgram(nr)) log.error("Card switching failed");
        } else if (command.matches("programSetPane:[0-4]")) {
            if (!removeCardFromProgram(nr)) log.error("Cannot remove card");
        }
    }

    /**
	 * Method cardsSelected.
	 * Checks if selected PorgramSet is valid
	 * @return boolean
	 */
    public boolean cardsSelected() {
        if (programSet[4] != null) return true; else return false;
    }

    /**
	 * Method getCardSelection.
	 * @return CardSelection
	 */
    public CardSelection getCardSelection() {
        setAnimating(true);
        CardSelection ret = new CardSelection();
        ret.setPlayerId(playerId);
        ret.setRound(round);
        ret.setSwitchOff(powerDown);
        List cardList = ret.getCard();
        for (int i = 0; i < 5; i++) {
            cardList.add(programSet[i].getTransportCard());
        }
        return ret;
    }
}
