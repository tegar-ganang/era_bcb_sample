package com.mtm.txholdem;

public class Rules {

    private int A = 14;

    private int K = 13;

    private int Q = 12;

    private int J = 11;

    private int ROYALFLUSH = 0;

    private int STRAIGHTFLUSH = 1;

    private int FOUROFAKIND = 2;

    private int FULLHOUSE = 3;

    private int FLUSH = 4;

    private int STRAIGHT = 5;

    private int THREEOFAKIND = 6;

    private int TWOPAIR = 7;

    private int ONEPAIR = 8;

    private int HIGHCARD = 9;

    private int heart = 0;

    private int diamond = 0;

    private int club = 0;

    private int spade = 0;

    private int myHighCard = 0;

    private int opHighCard = 0;

    private Card[] myCombined, opCombined;

    Rules() {
    }

    public String betterHand(Card[] myCards, Card[] opCards, Card[] boardCards) {
        int myHand = -1;
        int opHand = -1;
        Card tempCard;
        String result = "";
        myCombined = new Card[myCards.length + boardCards.length];
        opCombined = new Card[opCards.length + boardCards.length];
        for (int i = 0; i < myCards.length; i++) {
            myCombined[i] = myCards[i];
            opCombined[i] = opCards[i];
        }
        for (int i = 0; i < boardCards.length; i++) {
            myCombined[i + myCards.length] = boardCards[i];
            opCombined[i + opCards.length] = boardCards[i];
        }
        for (int i = 0; i < myCombined.length; i++) {
            for (int j = 0; j < myCombined.length - 1 - i; j++) {
                if (cardValueToInt(myCombined[j + 1].getValue()) > cardValueToInt(myCombined[j].getValue())) {
                    tempCard = myCombined[j];
                    myCombined[j] = myCombined[j + 1];
                    myCombined[j + 1] = tempCard;
                }
                if (cardValueToInt(opCombined[j + 1].getValue()) > cardValueToInt(opCombined[j].getValue())) {
                    tempCard = opCombined[j];
                    opCombined[j] = opCombined[j + 1];
                    opCombined[j + 1] = tempCard;
                }
            }
        }
        myHand = getHand(myCombined);
        opHand = getHand(opCombined);
        System.out.println("MyHand= " + myHand + " OpHand= " + opHand);
        if (myHand < opHand) {
            result = "You Win!";
        } else if (myHand > opHand) {
            result = "You Lose!";
        } else if (myHand == opHand) {
            if (myHand == HIGHCARD && (getHighCard(myCombined) > getHighCard(opCombined))) result = "You Win!"; else if (myHand == HIGHCARD && (getHighCard(myCombined) < getHighCard(opCombined))) result = "You Lose!"; else if (myHand == HIGHCARD && (getHighCard(myCombined) == getHighCard(opCombined))) {
                if (myHand == HIGHCARD && (get2ndHighCard(myCombined) > get2ndHighCard(opCombined))) result = "You Win!"; else if (myHand == HIGHCARD && (get2ndHighCard(myCombined) < get2ndHighCard(opCombined))) result = "You Lose!"; else if (myHand == HIGHCARD && (get2ndHighCard(myCombined) == get2ndHighCard(opCombined))) {
                    if (myHand == HIGHCARD && (get3rdHighCard(myCombined) > get3rdHighCard(opCombined))) result = "You Win!"; else if (myHand == HIGHCARD && (get3rdHighCard(myCombined) < get3rdHighCard(opCombined))) result = "You Lose!"; else if (myHand == HIGHCARD && (get3rdHighCard(myCombined) == get3rdHighCard(opCombined))) {
                        if (myHand == HIGHCARD && (get4thHighCard(myCombined) > get4thHighCard(opCombined))) result = "You Win!"; else if (myHand == HIGHCARD && (get4thHighCard(myCombined) < get4thHighCard(opCombined))) result = "You Lose!"; else if (myHand == HIGHCARD && (get4thHighCard(myCombined) == get4thHighCard(opCombined))) {
                            if (myHand == HIGHCARD && (get5thHighCard(myCombined) > get5thHighCard(opCombined))) result = "You Win!"; else if (myHand == HIGHCARD && (get5thHighCard(myCombined) < get5thHighCard(opCombined))) result = "You Lose!"; else if (myHand == HIGHCARD && (get5thHighCard(myCombined) == get5thHighCard(opCombined))) result = "Push!";
                        }
                    }
                }
            } else if (myHand == ONEPAIR && (getTopPair(myCombined) > getTopPair(opCombined))) result = "You Win!"; else if (myHand == ONEPAIR && (getTopPair(myCombined) < getTopPair(opCombined))) result = "You Lose!"; else if (myHand == ONEPAIR && (getTopPair(myCombined) == getTopPair(opCombined))) {
                if (getHighCard(myCombined) > getHighCard(opCombined)) result = "You Win!"; else if (getHighCard(myCombined) < getHighCard(opCombined)) result = "You Lose!"; else if (getHighCard(myCombined) == getHighCard(opCombined)) {
                    if (get2ndHighCard(myCombined) > get2ndHighCard(opCombined)) result = "You Win!"; else if (get2ndHighCard(myCombined) < get2ndHighCard(opCombined)) result = "You Lose!"; else {
                        if (get3rdHighCard(myCombined) > get3rdHighCard(opCombined)) result = "You Win!"; else if (get3rdHighCard(myCombined) < get3rdHighCard(opCombined)) result = "You Lose!"; else if (get3rdHighCard(myCombined) == get3rdHighCard(opCombined)) result = "Push!";
                    }
                }
            } else if (myHand == TWOPAIR && (getTopPair(myCombined) > getTopPair(opCombined))) result = "You Win!"; else if (myHand == TWOPAIR && (getTopPair(myCombined) < getTopPair(opCombined))) result = "You Lose!"; else if (myHand == TWOPAIR && (getBottomPair(myCombined) > getBottomPair(opCombined))) result = "You Lose!"; else if (myHand == TWOPAIR && (getBottomPair(myCombined) < getBottomPair(opCombined))) result = "You Lose!"; else if (myHand == TWOPAIR && (getHighCard(myCombined) > getHighCard(opCombined))) result = "You Win!"; else if (myHand == TWOPAIR && (getHighCard(myCombined) < getHighCard(opCombined))) result = "You Lose!"; else if (myHand == TWOPAIR && (getHighCard(myCombined) == getHighCard(opCombined))) result = "Push!"; else if (myHand == THREEOFAKIND && (getTopThree(myCombined) > getTopThree(opCombined))) result = "You Win!"; else if (myHand == THREEOFAKIND && (getTopThree(myCombined) < getTopThree(opCombined))) result = "You Lose!"; else if (myHand == FULLHOUSE && (getTopThree(myCombined) > getTopThree(opCombined))) result = "You Win!"; else if (myHand == FULLHOUSE && (getTopThree(myCombined) < getTopThree(opCombined))) result = "You Lose!"; else if (myHand == STRAIGHT && (getHighStraight(myCombined) > getHighStraight(opCombined))) result = "You Win!"; else if (myHand == STRAIGHT && (getHighStraight(myCombined) < getHighStraight(opCombined))) result = "You Lose!"; else if (myHand == STRAIGHT && (getHighStraight(myCombined) == getHighStraight(opCombined))) result = "Push!";
        }
        return result;
    }

    private int getHand(Card[] seven) {
        if (royalFlush(seven)) return 0; else if (straightFlush(seven)) return 1; else if (fourOfAKind(seven)) return 2; else if (fullHouse(seven)) return 3; else if (flush(seven)) return 4; else if (straight(seven)) return 5; else if (threeOfAKind(seven)) return 6; else if (twoPair(seven)) return 7; else if (onePair(seven)) return 8;
        return 9;
    }

    private boolean royalFlush(Card[] seven) {
        resetValues();
        int royalFlush = 0;
        String suit = getFlushSuit(seven);
        if (!suit.equals("")) {
            for (int i = 0; i < seven.length; i++) {
                if (seven[i].getCard().equals("A" + suit)) royalFlush++; else if (seven[i].getCard().equals("K" + suit)) royalFlush++; else if (seven[i].getCard().equals("Q" + suit)) royalFlush++; else if (seven[i].getCard().equals("J" + suit)) royalFlush++; else if (seven[i].getCard().equals("10" + suit)) royalFlush++;
            }
            if (royalFlush == 5) return true;
        }
        return false;
    }

    private boolean straightFlush(Card[] seven) {
        resetValues();
        String suit = getFlushSuit(seven);
        Card[] straight = new Card[5];
        int isStraight = 0;
        int j = -1;
        if (!suit.equals("")) {
            for (int i = 0; i < seven.length; i++) {
                if (seven[i].getSuit().equals(suit) && j < 4) straight[++j] = seven[i];
            }
            for (int i = 0; i < straight.length - 1; i++) {
                if (cardValueToInt(straight[i].getValue()) == 14 && cardValueToInt(straight[straight.length - 1].getValue()) == 2) {
                    isStraight++;
                }
                if (cardValueToInt(straight[i].getValue()) == cardValueToInt(straight[i + 1].getValue()) + 1) {
                    isStraight++;
                }
            }
            if (isStraight == 4) return true;
        }
        return false;
    }

    public boolean fourOfAKind(Card[] seven) {
        int four = 0;
        for (int i = 0; i < seven.length - 1; i++) {
            if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue())) {
                four++;
                if (four == 3) return true;
            } else four = 0;
        }
        return false;
    }

    public boolean fullHouse(Card[] seven) {
        int three = 0;
        int two = 0;
        int twoInARow = 0;
        for (int i = 0; i < seven.length - 1; i++) {
            if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue())) {
                twoInARow++;
                if (twoInARow == 2 || three == 2) two++; else {
                    three++;
                }
            } else twoInARow = 0;
        }
        if (two == 1 && three == 2) {
            return true;
        }
        return false;
    }

    public boolean flush(Card[] seven) {
        resetValues();
        if (!getFlushSuit(seven).equals("")) {
            return true;
        }
        return false;
    }

    public boolean straight(Card[] seven) {
        int isStraight = 0;
        for (int i = 0; i < seven.length - 1; i++) {
            if (cardValueToInt(seven[i].getValue()) != cardValueToInt(seven[i + 1].getValue())) {
                if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue()) + 1) {
                    isStraight++;
                    if (isStraight > 3) return true;
                } else isStraight = 0;
            }
        }
        if (isStraight > 3) {
            return true;
        }
        return false;
    }

    public boolean threeOfAKind(Card[] seven) {
        int three = 0;
        for (int i = 0; i < seven.length - 1; i++) {
            System.out.println(seven[i].getValue() + " " + seven[i + 1].getValue());
            if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue())) {
                three++;
                if (three == 2) return true;
            } else three = 0;
            System.out.println(three);
        }
        return false;
    }

    public boolean twoPair(Card[] seven) {
        int onePair = 0;
        int twoPair = 0;
        for (int i = 0; i < seven.length - 1; i++) {
            if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue())) {
                if (onePair == 1) twoPair++; else onePair++;
                System.out.println("OnePair: " + onePair);
                System.out.println("TwoPair: " + twoPair);
            }
        }
        if (onePair == 1 && twoPair >= 1) {
            return true;
        }
        return false;
    }

    public boolean onePair(Card[] seven) {
        int onePair = 0;
        for (int i = 0; i < seven.length - 1; i++) {
            if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue())) {
                onePair++;
            }
        }
        if (onePair >= 1) {
            return true;
        }
        return false;
    }

    private int getTopPair(Card[] seven) {
        for (int i = 0; i < seven.length - 1; i++) {
            if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue())) {
                return cardValueToInt(seven[i].getValue());
            }
        }
        return -1;
    }

    private int getBottomPair(Card[] seven) {
        boolean byPassOnce = false;
        for (int i = 0; i < seven.length - 1; i++) {
            if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue())) {
                if (byPassOnce) {
                    return cardValueToInt(seven[i].getValue());
                } else byPassOnce = true;
            }
        }
        return -1;
    }

    private int getTopThree(Card[] seven) {
        int three = 0;
        for (int i = 0; i < seven.length - 1; i++) {
            System.out.println(seven[i].getValue() + " " + seven[i + 1].getValue());
            if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue())) {
                three++;
                if (three == 2) return cardValueToInt(seven[i].getValue());
            } else three = 0;
        }
        return -1;
    }

    private int getHighCard(Card[] seven) {
        for (int i = 0; i < seven.length; i++) {
            if (cardValueToInt(seven[i].getValue()) != cardValueToInt(seven[i + 1].getValue())) return cardValueToInt(seven[i].getValue());
        }
        return -1;
    }

    private int get2ndHighCard(Card[] seven) {
        boolean byPassOnce = false;
        for (int i = 0; i < seven.length; i++) {
            if (cardValueToInt(seven[i].getValue()) != cardValueToInt(seven[i + 1].getValue())) {
                if (byPassOnce) return cardValueToInt(seven[i].getValue()); else byPassOnce = true;
            }
        }
        return -1;
    }

    private int get3rdHighCard(Card[] seven) {
        int byPassOnce = 0;
        for (int i = 0; i < seven.length; i++) {
            if (cardValueToInt(seven[i].getValue()) != cardValueToInt(seven[i + 1].getValue())) {
                if (byPassOnce == 2) return cardValueToInt(seven[i].getValue()); else byPassOnce++;
            }
        }
        return -1;
    }

    private int get4thHighCard(Card[] seven) {
        int byPassOnce = 0;
        for (int i = 0; i < seven.length; i++) {
            if (cardValueToInt(seven[i].getValue()) != cardValueToInt(seven[i + 1].getValue())) {
                if (byPassOnce == 3) return cardValueToInt(seven[i].getValue()); else byPassOnce++;
            }
        }
        return -1;
    }

    private int get5thHighCard(Card[] seven) {
        int byPassOnce = 0;
        for (int i = 0; i < seven.length; i++) {
            if (cardValueToInt(seven[i].getValue()) != cardValueToInt(seven[i + 1].getValue())) {
                if (byPassOnce == 4) return cardValueToInt(seven[i].getValue()); else byPassOnce++;
            }
        }
        return -1;
    }

    public int getHighStraight(Card[] seven) {
        int isStraight = 0;
        for (int i = 0; i < seven.length - 1; i++) {
            if (cardValueToInt(seven[i].getValue()) != cardValueToInt(seven[i + 1].getValue())) {
                if (cardValueToInt(seven[i].getValue()) == cardValueToInt(seven[i + 1].getValue()) + 1) {
                    isStraight++;
                    if (isStraight > 3) return cardValueToInt(seven[i - 4].getValue());
                } else isStraight = 0;
            }
        }
        return -1;
    }

    private int cardValueToInt(String in) {
        if (in.equals("A")) return A; else if (in.equals("K")) return K; else if (in.equals("Q")) return Q; else if (in.equals("J")) return J;
        return Integer.parseInt(in);
    }

    private String getFlushSuit(Card[] seven) {
        for (int i = 0; i < seven.length; i++) {
            if (seven[i].getSuit().equals("H")) heart++; else if (seven[i].getSuit().equals("C")) club++; else if (seven[i].getSuit().equals("S")) spade++; else if (seven[i].getSuit().equals("D")) diamond++;
            System.out.println(seven[i].getSuit());
        }
        if (heart < 5 && club < 5 && spade < 5 && diamond < 5) {
            return "";
        }
        if (heart >= 5) return "H"; else if (club >= 5) return "C"; else if (spade >= 5) return "S"; else if (diamond >= 5) return "D";
        return "";
    }

    private void tester() {
        myCombined[0].setCard("7C");
        myCombined[1].setCard("6D");
        myCombined[2].setCard("7D");
        myCombined[3].setCard("AH");
        myCombined[4].setCard("6C");
        myCombined[5].setCard("5C");
        myCombined[6].setCard("7H");
        opCombined[0].setCard("QS");
        opCombined[1].setCard("QD");
        opCombined[2].setCard("7D");
        opCombined[3].setCard("AH");
        opCombined[4].setCard("6C");
        opCombined[5].setCard("5C");
        opCombined[6].setCard("7H");
    }

    private void resetValues() {
        heart = spade = diamond = club = 0;
    }
}
