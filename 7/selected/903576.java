package mu.nu.nullpo.game.subsystem.ai;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import mu.nu.nullpo.game.component.Controller;
import mu.nu.nullpo.game.component.Field;
import mu.nu.nullpo.game.component.Piece;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.tool.airankstool.Ranks;
import mu.nu.nullpo.tool.airankstool.AIRanksConstants;
import mu.nu.nullpo.util.CustomProperties;
import org.apache.log4j.Logger;

public class RanksAI extends DummyAI implements Runnable {

    static Logger log = Logger.getLogger(RanksAI.class);

    public int bestXSub;

    public int bestYSub;

    public int bestRtSub;

    public int bestPts;

    public int delay;

    public GameEngine gEngine;

    public GameManager gManager;

    public boolean thinkRequest;

    public boolean thinking;

    public int thinkDelay;

    public volatile boolean threadRunning;

    public Thread thread;

    private Ranks ranks;

    private boolean skipNextFrame;

    private int currentHeightMin;

    private int currentHeightMax;

    private static int THRESHOLD_FORCE_4LINES = 8;

    private int MAX_PREVIEWS = 2;

    private int[] heights;

    private Score bestScore;

    private boolean gameOver;

    private boolean plannedToUseIPiece;

    private String currentRanksFile = "";

    private boolean allowHold;

    private int speedLimit;

    public class Score {

        public float rankStacking;

        public int distanceToSet;

        public boolean iPieceUsedInTheStack;

        public Score() {
            rankStacking = 0;
            distanceToSet = ranks.getStackWidth() * 20;
        }

        public String toString() {
            return " Rank Stacking : " + rankStacking + " distance to set :" + distanceToSet;
        }

        public void computeScore(int heights[]) {
            this.distanceToSet = 0;
            int[] surface = new int[ranks.getStackWidth() - 1];
            int maxJump = ranks.getMaxJump();
            int nbstep = 0;
            int correctedSteepStep = 0;
            int indexSteepStep = 0;
            boolean isCliff = true;
            for (int i = 0; i < ranks.getStackWidth() - 1; i++) {
                int diff = heights[i + 1] - heights[i];
                if (diff > maxJump) {
                    nbstep++;
                    this.distanceToSet += (diff - maxJump);
                    diff = maxJump;
                }
                if (diff < -maxJump) {
                    nbstep++;
                    distanceToSet -= (diff + maxJump);
                    diff = -maxJump;
                }
                surface[i] = diff;
            }
            log.debug("new surface =" + Arrays.toString(surface));
            int surfaceNb = ranks.encode(surface);
            this.rankStacking = ranks.getRankValue(surfaceNb);
            if ((MAX_PREVIEWS > 0) && (this.distanceToSet > 0)) {
                this.rankStacking = 0;
            }
        }

        public int compareTo(Object o) {
            Score otherScore = (Score) o;
            if (this.rankStacking != otherScore.rankStacking) {
                return this.rankStacking > otherScore.rankStacking ? 1 : -1;
            }
            return 0;
        }
    }

    @Override
    public String getName() {
        return "RANKSAI";
    }

    public void initRanks() {
        delay = 0;
        thinkRequest = false;
        thinking = false;
        threadRunning = false;
        CustomProperties propRanksAI = new CustomProperties();
        try {
            FileInputStream in = new FileInputStream(AIRanksConstants.RANKSAI_CONFIG_FILE);
            propRanksAI.load(in);
            in.close();
        } catch (IOException e) {
        }
        String file = propRanksAI.getProperty("ranksai.file");
        MAX_PREVIEWS = propRanksAI.getProperty("ranksai.numpreviews", 2);
        allowHold = propRanksAI.getProperty("ranksai.allowhold", false);
        speedLimit = propRanksAI.getProperty("ranksai.speedlimit", 0);
        if (ranks == null || !(currentRanksFile.equals(file))) {
            currentRanksFile = file;
            String inputFile = "";
            if (file != null && file.trim().length() > 0) {
                inputFile = AIRanksConstants.RANKSAI_DIR + currentRanksFile;
            }
            FileInputStream fis = null;
            ObjectInputStream in = null;
            if (inputFile.trim().length() == 0) ranks = new Ranks(4, 9); else {
                try {
                    fis = new FileInputStream(inputFile);
                    in = new ObjectInputStream(fis);
                    ranks = (Ranks) in.readObject();
                    in.close();
                } catch (FileNotFoundException e) {
                    ranks = new Ranks(4, 9);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        heights = new int[ranks.getStackWidth()];
        gameOver = false;
    }

    @Override
    public void init(GameEngine engine, int playerID) {
        gEngine = engine;
        gManager = engine.owner;
        initRanks();
        if (((thread == null) || !thread.isAlive()) && (engine.aiUseThread)) {
            thread = new Thread(this, "AI_" + playerID);
            thread.setDaemon(true);
            thread.start();
            thinkDelay = engine.aiThinkDelay;
            thinkCurrentPieceNo = 0;
            thinkLastPieceNo = 0;
        }
    }

    @Override
    public void shutdown(GameEngine engine, int playerID) {
        ranks = null;
        if ((thread != null) && (thread.isAlive())) {
            thread.interrupt();
            threadRunning = false;
            thread = null;
        }
    }

    @Override
    public void newPiece(GameEngine engine, int playerID) {
        if (!engine.aiUseThread) {
            thinkBestPosition(engine, playerID);
        } else {
            thinkRequest = true;
            thinkCurrentPieceNo++;
        }
    }

    @Override
    public void onFirst(GameEngine engine, int playerID) {
    }

    @Override
    public void onLast(GameEngine engine, int playerID) {
    }

    @Override
    public void setControl(GameEngine engine, int playerID, Controller ctrl) {
        if ((engine.nowPieceObject != null) && (engine.stat == GameEngine.STAT_MOVE) && (delay >= engine.aiMoveDelay) && (engine.statc[0] > 0) && (!engine.aiUseThread || (threadRunning && !thinking && (thinkCurrentPieceNo <= thinkLastPieceNo)))) {
            int totalPieceLocked = engine.statistics.totalPieceLocked + 1;
            int tpm = (int) (totalPieceLocked * 3600f) / (int) engine.statistics.time;
            if ((tpm <= speedLimit) || (speedLimit <= 0)) {
                int input = 0;
                Piece pieceNow = engine.nowPieceObject;
                int nowX = engine.nowPieceX;
                int nowY = engine.nowPieceY;
                int rt = pieceNow.direction;
                Field fld = engine.field;
                boolean pieceTouchGround = pieceNow.checkCollision(nowX, nowY + 1, fld);
                if ((bestHold || forceHold)) {
                    if (engine.isHoldOK()) input |= Controller.BUTTON_BIT_D;
                } else {
                    if (rt != bestRt) {
                        int lrot = engine.getRotateDirection(-1);
                        int rrot = engine.getRotateDirection(1);
                        if ((Math.abs(rt - bestRt) == 2) && (engine.ruleopt.rotateButtonAllowDouble) && !ctrl.isPress(Controller.BUTTON_E)) {
                            input |= Controller.BUTTON_BIT_E;
                        } else if (!ctrl.isPress(Controller.BUTTON_B) && engine.ruleopt.rotateButtonAllowReverse && !engine.isRotateButtonDefaultRight() && (bestRt == rrot)) {
                            input |= Controller.BUTTON_BIT_B;
                        } else if (!ctrl.isPress(Controller.BUTTON_B) && engine.ruleopt.rotateButtonAllowReverse && engine.isRotateButtonDefaultRight() && (bestRt == lrot)) {
                            input |= Controller.BUTTON_BIT_B;
                        } else if (!ctrl.isPress(Controller.BUTTON_A)) {
                            input |= Controller.BUTTON_BIT_A;
                        }
                    }
                    int minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld);
                    int maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld);
                    if (!skipNextFrame) {
                        skipNextFrame = true;
                        if (((bestX < minX - 1) || (bestX > maxX + 1) || (bestY < nowY)) && (rt == bestRt)) {
                            thinkRequest = true;
                        } else {
                            if ((nowX == bestX) && (pieceTouchGround) && (rt == bestRt)) {
                                if (bestRtSub != -1) {
                                    bestRt = bestRtSub;
                                    bestRtSub = -1;
                                }
                                if (bestX != bestXSub) {
                                    bestX = bestXSub;
                                    bestY = bestYSub;
                                }
                            }
                            if (nowX > bestX) {
                                if (!ctrl.isPress(Controller.BUTTON_LEFT) || (engine.aiMoveDelay >= 0)) input |= Controller.BUTTON_BIT_LEFT;
                            } else if (nowX < bestX) {
                                if (!ctrl.isPress(Controller.BUTTON_RIGHT) || (engine.aiMoveDelay >= 0)) input |= Controller.BUTTON_BIT_RIGHT;
                            } else if ((nowX == bestX) && (rt == bestRt)) {
                                if ((bestRtSub == -1) && (bestX == bestXSub)) {
                                    if (engine.ruleopt.harddropEnable && !ctrl.isPress(Controller.BUTTON_UP)) input |= Controller.BUTTON_BIT_UP; else if (engine.ruleopt.softdropEnable || engine.ruleopt.softdropLock) input |= Controller.BUTTON_BIT_DOWN;
                                } else {
                                    if (engine.ruleopt.harddropEnable && !engine.ruleopt.harddropLock && !ctrl.isPress(Controller.BUTTON_UP)) input |= Controller.BUTTON_BIT_UP; else if (engine.ruleopt.softdropEnable && !engine.ruleopt.softdropLock) input |= Controller.BUTTON_BIT_DOWN;
                                }
                            }
                        }
                    } else {
                        skipNextFrame = false;
                    }
                }
                delay = 0;
                ctrl.setButtonBit(input);
            }
        } else {
            delay++;
            ctrl.setButtonBit(0);
        }
    }

    /**
	 * Plays a fictitious move (ie not rendering it on the screen) for use in AIRanksTester
	 * It searches for the best possible move and plays it(ie updates the heights array)
	 * @param heights Heights of the columns
	 * @param pieces Current Piece and Next Pieces
	 */
    public void playFictitiousMove(int[] heights, int[] pieces, int[] holdPiece, boolean[] holdOK) {
        currentHeightMin = 25;
        currentHeightMax = 0;
        for (int i = 0; i < ranks.getStackWidth(); i++) {
            if (heights[i] < currentHeightMin) {
                currentHeightMin = heights[i];
            }
            if (heights[i] > currentHeightMax) {
                currentHeightMax = heights[i];
            }
        }
        thinkBestPosition(heights, pieces, holdPiece, holdOK[0]);
        if (bestScore.rankStacking == 0) {
            gameOver = true;
        } else {
            if (bestHold == true) {
                holdOK[0] = false;
            } else {
                holdOK[0] = true;
                if (bestX == 9) {
                    for (int i = 0; i < heights.length; i++) {
                        heights[i] -= 4;
                    }
                } else {
                    ranks.addToHeights(heights, pieces[0], bestRt, bestX);
                    for (int i = 0; i < heights.length; i++) {
                        if (heights[i] > 20) {
                            gameOver = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
	 * Tells other classes if the fictitious game is over
	 * @return true if fictitious game is over
	 */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
	 * Think the best position, for a given engine. It will do the necessary conversion from engine representation of the field to ranks representation
	 * of it, and run the main method thinkBestPosition
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
    public void thinkBestPosition(GameEngine engine, int playerID) {
        int nowY = engine.nowPieceY;
        Piece pieceNow = engine.nowPieceObject;
        for (int i = 0; i < ranks.getStackWidth(); i++) {
            heights[i] = engine.field.getHeight() - engine.field.getHighestBlockY(i);
        }
        int[] pieces = new int[engine.nextPieceArraySize];
        pieces[0] = pieceNow.id;
        for (int i = 1; i < pieces.length; i++) {
            pieces[i] = engine.getNextObject(engine.nextPieceCount + i - 1).id;
        }
        int[] holdPiece = new int[1];
        if (engine.holdPieceObject == null) {
            holdPiece[0] = -1;
        } else {
            holdPiece[0] = engine.holdPieceObject.id;
        }
        boolean holdOK = engine.isHoldOK();
        allowHold &= engine.ruleopt.holdEnable;
        thinkBestPosition(heights, pieces, holdPiece, holdOK);
        bestX -= (pieceNow.dataOffsetX[bestRt] + Ranks.PIECES_LEFTMOSTS[pieceNow.id][bestRt]);
        bestXSub = bestX;
        bestY = pieceNow.getBottom(bestX, nowY, bestRt, engine.field);
        bestYSub = bestY;
        bestYSub = bestY;
        if (bestScore.rankStacking == 0) threadRunning = false;
        thinkLastPieceNo++;
        log.debug("nowX : " + engine.nowPieceX + " X:" + bestX + " Y:" + bestY + " R:" + bestRt + " H:" + bestHold + " Pts:" + bestScore);
    }

    /**
	  * Main method that will return the best move for the current piece, by calling the recursive method thinkmain on each possible move
	  * @param heights Array containing the heights of the columns in the field
	  * @param pieces Array containing the current piece and the next pieces.
	 */
    public void thinkBestPosition(int heights[], int[] pieces, int[] holdPiece, boolean holdOK) {
        bestX = 0;
        bestY = 0;
        bestRt = 0;
        bestXSub = 0;
        bestYSub = 0;
        bestRtSub = -1;
        bestPts = 0;
        bestScore = new Score();
        Score score;
        int[] piecesCopy = new int[pieces.length];
        for (int i = 0; i < piecesCopy.length; i++) piecesCopy[i] = pieces[i];
        int pieceNow = piecesCopy[0];
        int numPreviews = MAX_PREVIEWS;
        currentHeightMin = 99;
        currentHeightMax = 0;
        for (int i = 0; i < ranks.getStackWidth(); i++) {
            if (heights[i] < currentHeightMin) {
                currentHeightMin = heights[i];
            }
            if (heights[i] > currentHeightMax) {
                currentHeightMax = heights[i];
            }
        }
        if (pieceNow == Piece.PIECE_I && currentHeightMin >= THRESHOLD_FORCE_4LINES && currentHeightMin >= 4) {
            bestHold = false;
            bestX = ranks.getStackWidth();
            bestRt = 1;
            bestXSub = bestX;
            bestRtSub = -1;
            bestScore.rankStacking = Integer.MAX_VALUE;
        } else {
            for (int useHold = 0; useHold < ((holdOK && allowHold) ? 2 : 1); useHold++) {
                if (useHold == 1) {
                    if (holdPiece[0] == -1) {
                        holdPiece[0] = piecesCopy[0];
                        for (int i = 0; i < piecesCopy.length - 1; i++) piecesCopy[i] = piecesCopy[i + 1];
                        pieceNow = piecesCopy[0];
                    } else {
                        int tempPiece = piecesCopy[0];
                        piecesCopy[0] = holdPiece[0];
                        holdPiece[0] = tempPiece;
                        pieceNow = piecesCopy[0];
                    }
                }
                for (int rt = 0; rt < Ranks.PIECES_NUM_ROTATIONS[pieceNow]; rt++) {
                    int minX = 0;
                    int maxX = ranks.getStackWidth() - Ranks.PIECES_WIDTHS[pieceNow][rt];
                    for (int x = minX; x <= maxX; x++) {
                        score = thinkMain(x, rt, heights, piecesCopy, holdPiece, (useHold == 1) ? false : true, numPreviews);
                        log.debug("MAIN  id=" + pieceNow + " posX=" + x + " rt=" + rt + " hold :" + useHold + " score:" + score);
                        if (score.compareTo(bestScore) > 0) {
                            log.debug("MAIN new best piece !");
                            if (pieceNow == Piece.PIECE_I) {
                                bestScore.iPieceUsedInTheStack = true;
                            }
                            bestHold = (useHold == 1);
                            bestX = x;
                            bestRt = rt;
                            bestXSub = x;
                            bestRtSub = -1;
                            bestScore = score;
                        }
                    }
                    if (pieceNow == Piece.PIECE_I && ((rt == 1) || (rt == 3)) && currentHeightMin >= 4) {
                        score = thinkMain(maxX + 1, rt, heights, piecesCopy, holdPiece, (useHold == 1) ? false : true, numPreviews);
                        log.debug("MAIN (4 Lines) id=" + pieceNow + " posX=" + (maxX + 1) + " rt=" + rt + " hold :" + useHold + " score:" + score);
                        if (score.compareTo(bestScore) > 0) {
                            log.debug("MAIN (4 Lines) new best piece !");
                            bestHold = (useHold == 1);
                            bestX = maxX + 1;
                            bestRt = rt;
                            bestXSub = maxX + 1;
                            bestRtSub = -1;
                            bestScore = score;
                        }
                    }
                }
            }
        }
        if (numPreviews > 0) plannedToUseIPiece = bestScore.iPieceUsedInTheStack;
    }

    /**
 * Recursive method that returns the score of a given move for a given field and given next pieces
 * @param x Column where the piece has to be put.
 * @param rt Rotation of the piece.
 * @param heights Array containing the heights of the field
 * @param pieces Array containing the Current Piece and Next Pieces
 * @param numPreviews Number of previews to consider in the thinking process
 * @return The score for this move (placing the piece in this column, with this rotation)
 */
    public Score thinkMain(int x, int rt, int[] heights, int pieces[], int holdPiece[], boolean holdOK, int numPreviews) {
        Score score = new Score();
        int heightMin = 99;
        int heightMax = 0;
        ;
        int[] surface = ranks.heightsToSurface(heights);
        log.debug("piece id : " + pieces[0] + " rot : " + rt + " x :" + x + " surface :" + Arrays.toString(surface));
        boolean isVerticalIRightMost = (pieces[0] == Piece.PIECE_I && ((rt == 1) || (rt == 3)) && (x == 9));
        if (isVerticalIRightMost || ranks.surfaceFitsPiece(surface, pieces[0], rt, x)) {
            int[] heightsWork = heights.clone();
            if (!isVerticalIRightMost) {
                ranks.addToHeights(heightsWork, pieces[0], rt, x);
                for (int i = 0; i < ranks.getStackWidth(); i++) {
                    if (heightsWork[i] > heightMax) {
                        heightMax = heights[i];
                    }
                    if (heightsWork[i] < heightMin) {
                        heightMin = heights[i];
                    }
                }
            } else {
                for (int i = 0; i < ranks.getStackWidth(); i++) heightsWork[i] -= 4;
                heightMin -= 4;
                heightMax -= 4;
            }
            if (numPreviews > 0) {
                Score bestScore = new Score();
                Score scoreCurrent;
                int pieceNow = pieces[1];
                int[] pieces2 = new int[pieces.length];
                System.arraycopy(pieces, 1, pieces2, 0, pieces.length - 1);
                int[] holdPiece2 = new int[1];
                holdPiece2[0] = holdPiece[0];
                int numPreviews2 = numPreviews - 1;
                if (pieceNow == Piece.PIECE_I && heightMin >= THRESHOLD_FORCE_4LINES && heightMin >= 4) {
                    int rt2 = 1;
                    int maxX2 = ranks.getStackWidth() - 1;
                    scoreCurrent = thinkMain(maxX2 + 1, rt2, heightsWork, pieces2, holdPiece2, true, numPreviews2);
                    log.debug("SUB (4 Lines)" + numPreviews + " id=" + pieceNow + " posX=" + (maxX2 + 1) + " rt=" + rt2 + " score:" + scoreCurrent);
                    if (scoreCurrent.compareTo(bestScore) > 0) {
                        log.debug("SUB new best piece !");
                        bestScore = scoreCurrent;
                    }
                } else {
                    for (int h2 = 0; h2 < ((holdOK && allowHold) ? 2 : 1); h2++) {
                        if (h2 == 1) {
                            if (holdPiece2[0] == -1) {
                                holdPiece2[0] = pieces2[0];
                                for (int i = 0; i < pieces2.length - 1; i++) pieces2[i] = pieces2[i + 1];
                                pieceNow = pieces2[0];
                            } else {
                                int tempPiece = pieces2[0];
                                pieces2[0] = holdPiece2[0];
                                holdPiece2[0] = tempPiece;
                                pieceNow = pieces2[0];
                            }
                        }
                        for (int rt2 = 0; rt2 < Ranks.PIECES_NUM_ROTATIONS[pieceNow]; rt2++) {
                            boolean isVerticalI2 = (pieceNow == Piece.PIECE_I && ((rt2 == 1) || (rt2 == 3)));
                            int minX2 = 0;
                            int maxX2 = minX2 + ranks.getStackWidth() - Ranks.PIECES_WIDTHS[pieceNow][rt2];
                            for (int x2 = minX2; x2 <= maxX2; x2++) {
                                scoreCurrent = thinkMain(x2, rt2, heightsWork, pieces2, holdPiece2, (h2 == 1) ? false : true, numPreviews2);
                                log.debug("SUB " + numPreviews + " id=" + pieceNow + " posX=" + x2 + " rt=" + rt2 + " hold :" + h2 + " score " + scoreCurrent);
                                if (scoreCurrent.compareTo(bestScore) > 0) {
                                    log.debug("SUB new best piece !");
                                    bestScore = scoreCurrent;
                                }
                            }
                            if (isVerticalI2 && heightMin >= 4) {
                                scoreCurrent = thinkMain(maxX2 + 1, rt2, heightsWork, pieces2, holdPiece2, (h2 == 1) ? false : true, numPreviews2);
                                log.debug("SUB (4 Lines)" + numPreviews + " id=" + pieceNow + " posX=" + (maxX2 + 1) + " rt=" + rt2 + " hold :" + h2 + " score:" + scoreCurrent);
                                if (scoreCurrent.compareTo(bestScore) > 0) {
                                    log.debug("SUB new best piece !");
                                    bestScore = scoreCurrent;
                                }
                            }
                        }
                    }
                }
                return bestScore;
            } else {
                score.computeScore(heightsWork);
                if ((pieces[0] == Piece.PIECE_I) && (x < ranks.getStackWidth()) && numPreviews < MAX_PREVIEWS) {
                    score.iPieceUsedInTheStack = true;
                }
                return score;
            }
        } else {
            return score;
        }
    }

    /**
	  * Get max think level
	  * @return Max think level (1 in this AI)
	 */
    public int getMaxThinkDepth() {
        return 1;
    }

    public void run() {
        log.info("RanksAI: Thread start");
        threadRunning = true;
        while (threadRunning) {
            if (thinkRequest) {
                thinkRequest = false;
                thinking = true;
                try {
                    thinkBestPosition(gEngine, gEngine.playerID);
                } catch (Throwable e) {
                    log.debug("RanksAI: thinkBestPosition Failed", e);
                }
                thinking = false;
                skipNextFrame = false;
            }
            if (thinkDelay > 0) {
                try {
                    Thread.sleep(thinkDelay);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        threadRunning = false;
        ranks = null;
        log.info("RanksAI: Thread end");
    }
}
