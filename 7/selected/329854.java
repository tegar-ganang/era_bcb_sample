package it.jdeck.core.model.source;

import it.jdeck.core.model.Card;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import au.com.bytecode.opencsv.CSVReader;

@SuppressWarnings("serial")
public class CardsStack implements Serializable {

    private Card[] cards;

    private int cardsUsed;

    private int cardsPresent;

    protected CardsStack() {
        this.cards = new Card[100];
        this.cardsUsed = 0;
        this.cardsPresent = 0;
    }

    protected CardsStack(FileReader in) throws NumberFormatException, IOException {
        this.cards = new Card[100];
        this.cardsUsed = 0;
        this.cardsPresent = 0;
        String[] nextLine;
        CSVReader reader = new CSVReader(in);
        int num = 0;
        while ((nextLine = reader.readNext()) != null) {
            int i = Integer.valueOf(nextLine[0]);
            for (int j = 0; j < i; j++) {
                this.addCard(new Card(nextLine, num));
                num++;
            }
        }
    }

    protected void shuffle() {
        for (int i = cardsPresent - 1; i > 0; i--) {
            int rand = (int) (Math.random() * (i + 1));
            Card temp = cards[i];
            cards[i] = cards[rand];
            cards[rand] = temp;
        }
    }

    protected void addCard(Card card) {
        cards[this.cardsPresent] = card;
        cardsPresent++;
    }

    protected Card getCard() {
        if (cardsPresent > 0) {
            cardsPresent--;
            cardsUsed++;
            Card get = cards[cardsPresent];
            cards[cardsPresent] = null;
            return get;
        } else {
            return null;
        }
    }

    protected Card getCardByName(String name) {
        Card current = null;
        if (cardsPresent > 0) {
            for (int i = 0; i < cardsPresent; i++) {
                if (cards[i].getName().equals(name)) {
                    current = cards[i];
                    cards[i] = null;
                    break;
                }
            }
            ricompatta();
            cardsPresent--;
            cardsUsed++;
            return current;
        }
        return current;
    }

    protected Card getCardById(String id) {
        Card current = null;
        if (cardsPresent > 0) {
            for (int i = 0; i < cardsPresent; i++) {
                if (cards[i].getId().equals(id)) {
                    current = cards[i];
                    cards[i] = null;
                    break;
                }
            }
            ricompatta();
            cardsPresent--;
            cardsUsed++;
            return current;
        }
        return current;
    }

    protected Card selectCardById(String id) {
        Card current = null;
        if (cardsPresent > 0) {
            for (int i = 0; i < cardsPresent; i++) {
                if (cards[i].getId().equals(id)) {
                    current = cards[i];
                    break;
                }
            }
            return current;
        }
        return current;
    }

    private void ricompatta() {
        for (int i = 0; i < cardsPresent; i++) {
            if (cards[i] == null && cards[i + 1] != null) {
                cards[i] = cards[i + 1];
                cards[i + 1] = null;
            }
        }
    }

    protected List<Card> getCardsList() {
        List<Card> tempList = new ArrayList<Card>();
        for (int i = 0; i < cardsPresent; i++) {
            tempList.add(cards[i]);
        }
        return tempList;
    }
}
