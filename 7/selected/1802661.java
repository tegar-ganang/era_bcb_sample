package darts.games;

import java.util.*;
import darts.core.*;
import org.apache.log4j.*;

public class SimpleCricketDartGame extends DartGameDecorator implements CricketDartGame {

    private static Category cat = Category.getInstance(SimpleCricketDartGame.class.getName());

    private int possibleScores[];

    private String possibleWordScores[];

    private boolean random;

    private int closeNum;

    public SimpleCricketDartGame() {
        super();
        init();
    }

    public SimpleCricketDartGame(DartGame baseDartGame) {
        super(baseDartGame);
        init();
    }

    private void init() {
        possibleScores = new int[] { 15, 16, 17, 18, 19, 20, 25 };
        setPossibleScores(possibleScores);
    }

    public void setRandom(boolean random) {
        this.random = random;
        if (random) {
            possibleScores = new int[NUM_SCORES];
            for (int i = 0; i < NUM_SCORES - 1; i++) {
                getRandomScore: while (true) {
                    int score = (int) (Math.random() * 20) + 1;
                    for (int j = 0; j < i; j++) {
                        if (score == possibleScores[j]) {
                            continue getRandomScore;
                        }
                    }
                    possibleScores[i] = score;
                    break;
                }
            }
            possibleScores[NUM_SCORES - 1] = 25;
            boolean sorted = false;
            while (!sorted) {
                sorted = true;
                for (int i = 0; i < NUM_SCORES - 1; i++) {
                    if (possibleScores[i] > possibleScores[i + 1]) {
                        int t = possibleScores[i];
                        possibleScores[i] = possibleScores[i + 1];
                        possibleScores[i + 1] = t;
                        sorted = false;
                    }
                }
            }
            setPossibleScores(possibleScores);
        }
    }

    public boolean getRandom() {
        return random;
    }

    public void setCloseNum(int closeNum) {
        this.closeNum = closeNum;
    }

    public int getCloseNum() {
        return closeNum;
    }

    public String getOptions() {
        String options = "Regular";
        if (random) {
            options = "Random";
        }
        options += ", " + closeNum + " to close";
        return options;
    }

    public void setPossibleScores(int possibleScores[]) {
        this.possibleScores = possibleScores;
        possibleWordScores = new String[NUM_SCORES];
        for (int i = 0; i < NUM_SCORES - 1; i++) {
            possibleWordScores[i] = Integer.toString(possibleScores[i]);
            possibleWordScores[NUM_SCORES - 1] = "Bull";
        }
    }

    public int[] getPossibleScores() {
        return possibleScores;
    }

    public String[] getPossibleWordScores() {
        return possibleWordScores;
    }

    public boolean addScore(DartScore dartScore) {
        boolean validScoreNumber = false;
        for (int i = 0; i < possibleScores.length; i++) {
            if (possibleScores[i] == dartScore.getScore()) {
                validScoreNumber = true;
                break;
            }
        }
        cat.debug("addScore(" + dartScore.getPlayer() + ", " + dartScore.getScore() + ", " + dartScore.getHits() + ")");
        boolean scorable = isNumberScorable(dartScore.getScore(), dartScore.getPlayer());
        int oldHits = getCountedHits(dartScore.getPlayer(), dartScore.getScore());
        if (!validScoreNumber || oldHits >= closeNum && !scorable) {
            dartScore.setCountedHits(0);
            dartScore.setScoredHits(0);
        } else {
            int newHits = oldHits + dartScore.getHits();
            int countedHits = dartScore.getHits();
            if (newHits > closeNum && !scorable) {
                countedHits = closeNum - oldHits;
                newHits = closeNum;
            }
            dartScore.setCountedHits(countedHits);
            if (newHits > closeNum) {
                if (oldHits < closeNum) {
                    oldHits = closeNum;
                }
                int scoredHits = newHits - oldHits;
                dartScore.setScoredHits(scoredHits);
            }
        }
        if (!super.addScore(dartScore)) {
            return false;
        }
        return true;
    }

    public boolean isGameComplete() {
        if (super.isGameComplete()) {
            return true;
        }
        boolean closed[] = new boolean[getNumPlayers() + 1];
        boolean allClosed = true;
        for (int i = 1; i < getNumPlayers() + 1; i++) {
            closed[i] = hasPlayerClosedAll(getPlayer(i));
        }
        closed: for (int i = 1; i < closed.length; i++) {
            if (closed[i]) {
                int score = getTotalScore(i);
                checkScores: for (int j = 1; j < getNumPlayers() + 1; j++) {
                    if (i == j) {
                        continue checkScores;
                    }
                    if (score == getTotalScore(j)) {
                        if (closed[j] == true) {
                            continue closed;
                        }
                    } else if (score < getTotalScore(j)) {
                        continue closed;
                    }
                }
                setWinner(getPlayer(i));
                return true;
            }
        }
        return false;
    }

    public boolean hasPlayerClosedAll(String player) {
        for (int i = 0; i < possibleScores.length; i++) {
            if (getCountedHits(player, possibleScores[i]) < closeNum) {
                return false;
            }
        }
        return true;
    }

    public boolean isNumberClosed(int score) {
        return !isNumberScorable(score, "");
    }

    public boolean isNumberScorable(int score, String player) {
        for (int i = 1; i < getNumPlayers() + 1; i++) {
            if (!getPlayer(i).equals(player)) {
                if (getCountedHits(getPlayer(i), score) < closeNum) {
                    return true;
                }
            }
        }
        return false;
    }
}
