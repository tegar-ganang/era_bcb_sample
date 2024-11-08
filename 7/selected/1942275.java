package org.teacake.monolith.apk;

public class MonolithGameData implements Game {

    public static int BOUNDARY_CONDITION_ZEROES = 0;

    public static int BOUNDARY_CONDITION_WRAPAROUND = 1;

    public MonolithGameData() {
        this.boundaryCondition = BOUNDARY_CONDITION_ZEROES;
        Block.enableMonolithBlocks = true;
        this.score = 0;
        this.lines = 0;
        this.level = 1;
        this.timer = 1000;
        this.timerEnabled = false;
        this.newLevel = 1;
        this.startingLevel = 1;
        this.gridMaxWidth = 10;
        this.gridMaxHeight = 20;
        this.grid = new int[gridMaxWidth][gridMaxHeight];
        this.newgrid = new int[gridMaxWidth][gridMaxHeight];
        this.oldgrid = new int[gridMaxWidth][gridMaxHeight];
        this.randomgen = new java.util.Random();
        this.energy = 0;
        this.step = 0;
        this.blockPlaced = false;
        for (int y = 0; y < gridMaxHeight; y++) {
            for (int x = 0; x < gridMaxWidth; x++) {
                this.grid[x][y] = -1;
                this.oldgrid[x][y] = -1;
            }
        }
        this.clearedLines = new int[gridMaxHeight];
        for (int y = 0; y < gridMaxHeight; y++) {
            this.clearedLines[y] = 0;
        }
    }

    public void clearGrid() {
        for (int x = 0; x < gridMaxWidth; x++) {
            for (int y = 0; y < gridMaxHeight; y++) {
                this.grid[x][y] = -1;
                this.oldgrid[x][y] = -1;
            }
        }
        for (int y = 0; y < gridMaxHeight; y++) {
            this.clearedLines[y] = 0;
        }
    }

    public int[] getLevels() {
        int[] retval = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        return retval;
    }

    public void initGame(int theStartingLevel) {
        this.score = 0;
        this.lines = 0;
        this.level = startingLevel;
        this.newLevel = startingLevel;
        this.startingLevel = theStartingLevel;
        Block newblock = new Block();
        this.currentBlock = newblock;
        Block newnextblock = new Block();
        this.nextBlock = newnextblock;
        switch(theStartingLevel) {
            case 1:
                this.timer = 500;
                break;
            case 2:
                this.timer = 450;
                break;
            case 3:
                this.timer = 400;
                break;
            case 4:
                this.timer = 350;
                break;
            case 5:
                this.timer = 300;
                break;
            case 6:
                this.timer = 250;
                break;
            case 7:
                this.timer = 200;
                break;
            case 8:
                this.timer = 166;
                break;
            case 9:
                this.timer = 133;
                break;
            case 10:
                this.timer = 100;
                break;
        }
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    public int getGridValue(int x, int y) {
        return grid[x][y];
    }

    public int getPreviousGridValue(int x, int y) {
        return oldgrid[x][y];
    }

    public void setGridValue(int x, int y, int value) {
        grid[x][y] = value;
    }

    public int getNeighbourCount(int x, int y) {
        if (this.boundaryCondition == BOUNDARY_CONDITION_WRAPAROUND) {
            int neighbourx[] = new int[8];
            int neighboury[] = new int[8];
            int xprev = x - 1;
            int xnext = x + 1;
            int yprev = y - 1;
            int ynext = y + 1;
            if (x == 0) {
                xprev = gridMaxWidth - 1;
                xnext = x + 1;
            }
            if (x == gridMaxWidth - 1) {
                xprev = x - 1;
                xnext = 0;
            }
            if (y == 0) {
                yprev = gridMaxHeight - 1;
                ynext = y + 1;
            }
            if (y == gridMaxHeight - 1) {
                yprev = y - 1;
                ynext = 0;
            }
            neighbourx[0] = xprev;
            neighboury[0] = yprev;
            neighbourx[1] = x;
            neighboury[1] = yprev;
            neighbourx[2] = xnext;
            neighboury[2] = yprev;
            neighbourx[3] = xprev;
            neighboury[3] = y;
            neighbourx[4] = xnext;
            neighboury[4] = y;
            neighbourx[5] = xprev;
            neighboury[5] = ynext;
            neighbourx[6] = x;
            neighboury[6] = ynext;
            neighbourx[7] = xnext;
            neighboury[7] = ynext;
            int count = 0;
            for (int i = 0; i < 8; i++) {
                if (grid[neighbourx[i]][neighboury[i]] != -1) {
                    count++;
                }
            }
            return count;
        }
        if (this.boundaryCondition == BOUNDARY_CONDITION_ZEROES) {
            int neighbour[] = new int[8];
            if (x == 0) {
                neighbour[0] = -1;
                neighbour[3] = -1;
                neighbour[5] = -1;
            }
            if (y == 0) {
                neighbour[0] = -1;
                neighbour[1] = -1;
                neighbour[2] = -1;
            }
            if (x == gridMaxWidth - 1) {
                neighbour[2] = -1;
                neighbour[4] = -1;
                neighbour[7] = -1;
            }
            if (y == gridMaxHeight - 1) {
                neighbour[5] = -1;
                neighbour[6] = -1;
                neighbour[7] = -1;
            }
            if (neighbour[0] != -1) {
                neighbour[0] = grid[x - 1][y - 1];
            }
            if (neighbour[1] != -1) {
                neighbour[1] = grid[x][y - 1];
            }
            if (neighbour[2] != -1) {
                neighbour[2] = grid[x + 1][y - 1];
            }
            if (neighbour[3] != -1) {
                neighbour[3] = grid[x - 1][y];
            }
            if (neighbour[4] != -1) {
                neighbour[4] = grid[x + 1][y];
            }
            if (neighbour[5] != -1) {
                neighbour[5] = grid[x - 1][y + 1];
            }
            if (neighbour[6] != -1) {
                neighbour[6] = grid[x][y + 1];
            }
            if (neighbour[7] != -1) {
                neighbour[7] = grid[x + 1][y + 1];
            }
            int count = 0;
            for (int i = 0; i < neighbour.length; i++) {
                if (neighbour[i] != -1) {
                    count++;
                }
            }
            return count;
        }
        return 0;
    }

    public boolean isGridEmpty() {
        for (int y = 0; y < gridMaxHeight; y++) {
            for (int x = 0; x < gridMaxWidth; x++) {
                if (getGridValue(x, y) != -1) {
                    return false;
                }
            }
        }
        return true;
    }

    public void evolve() {
        if (energy > 0 && !isGridEmpty()) {
            for (int y = 0; y < gridMaxHeight; y++) {
                for (int x = 0; x < gridMaxWidth; x++) {
                    oldgrid[x][y] = grid[x][y];
                    int count = getNeighbourCount(x, y);
                    if (grid[x][y] != -1) {
                        if (count < 2) {
                            newgrid[x][y] = -1;
                        }
                        if (count == 3) {
                            newgrid[x][y] = grid[x][y];
                            score = score + 3;
                        }
                        if (count > 3) {
                            newgrid[x][y] = -1;
                        }
                    } else {
                        if (count == 3) {
                            newgrid[x][y] = randomgen.nextInt(7);
                            this.score = this.score + 3;
                        } else {
                            newgrid[x][y] = grid[x][y];
                        }
                    }
                }
            }
            energy--;
            for (int y = 0; y < gridMaxHeight; y++) {
                for (int x = 0; x < gridMaxWidth; x++) {
                    grid[x][y] = newgrid[x][y];
                }
            }
        } else {
            if (this.energy < 0) {
                energy = 0;
            }
            this.status = STATUS_PLAYING;
        }
    }

    public void gameLoop() {
        blockPlaced = false;
        step++;
        if (this.status == STATUS_EVOLVING) {
            evolve();
        } else {
            if (this.moveBlockDown()) {
                this.clearCompleteLines();
                if (this.newLevel != this.level) {
                    level = newLevel;
                }
            } else {
                for (int i = 0; i < 4; i++) {
                    if (this.grid[this.nextBlock.subblocks[0].xpos + this.nextBlock.xPos][this.currentBlock.subblocks[0].ypos + this.nextBlock.yPos] != -1) {
                        this.setStatus(STATUS_GAME_OVER);
                        return;
                    }
                }
                this.currentBlock = this.nextBlock;
                Block bl = new Block();
                blockPlaced = true;
                this.nextBlock = bl;
            }
        }
    }

    public void moveBlockLeft() {
        if ((this.currentBlock.subblocks[0].xpos + this.currentBlock.xPos > 0) && (this.currentBlock.subblocks[1].xpos + this.currentBlock.xPos > 0) && (this.currentBlock.subblocks[2].xpos + this.currentBlock.xPos > 0) && (this.currentBlock.subblocks[3].xpos + this.currentBlock.xPos > 0)) {
            if ((this.grid[this.currentBlock.subblocks[0].xpos + this.currentBlock.xPos - 1][this.currentBlock.subblocks[0].ypos + this.currentBlock.yPos] == -1) && (this.grid[this.currentBlock.subblocks[1].xpos + this.currentBlock.xPos - 1][this.currentBlock.subblocks[1].ypos + this.currentBlock.yPos] == -1) && (this.grid[this.currentBlock.subblocks[2].xpos + this.currentBlock.xPos - 1][this.currentBlock.subblocks[2].ypos + this.currentBlock.yPos] == -1) && (this.grid[this.currentBlock.subblocks[3].xpos + this.currentBlock.xPos - 1][this.currentBlock.subblocks[3].ypos + this.currentBlock.yPos] == -1)) {
                this.currentBlock.xPos--;
            }
        }
    }

    public void moveBlockRight() {
        if ((this.currentBlock.subblocks[0].xpos + this.currentBlock.xPos < gridMaxWidth - 1) && (this.currentBlock.subblocks[1].xpos + this.currentBlock.xPos < gridMaxWidth - 1) && (this.currentBlock.subblocks[2].xpos + this.currentBlock.xPos < gridMaxWidth - 1) && (this.currentBlock.subblocks[3].xpos + this.currentBlock.xPos < gridMaxWidth - 1)) {
            if ((this.grid[this.currentBlock.subblocks[0].xpos + this.currentBlock.xPos + 1][this.currentBlock.subblocks[0].ypos + this.currentBlock.yPos] == -1) && (this.grid[this.currentBlock.subblocks[1].xpos + this.currentBlock.xPos + 1][this.currentBlock.subblocks[1].ypos + this.currentBlock.yPos] == -1) && (this.grid[this.currentBlock.subblocks[2].xpos + this.currentBlock.xPos + 1][this.currentBlock.subblocks[2].ypos + this.currentBlock.yPos] == -1) && (this.grid[this.currentBlock.subblocks[3].xpos + this.currentBlock.xPos + 1][this.currentBlock.subblocks[3].ypos + this.currentBlock.yPos] == -1)) {
                this.currentBlock.xPos++;
            }
        }
    }

    public boolean canMoveBlockDown() {
        for (int i = 0; i < 4; i++) {
            if (this.currentBlock.subblocks[i].ypos + this.currentBlock.yPos > gridMaxHeight + 2) {
                return false;
            }
        }
        if (this.currentBlock.height + this.currentBlock.yPos < gridMaxHeight) {
            if ((grid[currentBlock.subblocks[0].xpos + currentBlock.xPos][this.currentBlock.subblocks[0].ypos + this.currentBlock.yPos + 1] == -1) && (grid[currentBlock.subblocks[1].xpos + currentBlock.xPos][this.currentBlock.subblocks[1].ypos + this.currentBlock.yPos + 1] == -1) && (grid[currentBlock.subblocks[2].xpos + currentBlock.xPos][this.currentBlock.subblocks[2].ypos + this.currentBlock.yPos + 1] == -1) && (grid[currentBlock.subblocks[3].xpos + currentBlock.xPos][this.currentBlock.subblocks[3].ypos + this.currentBlock.yPos + 1] == -1)) {
                return true;
            }
        }
        return false;
    }

    public boolean moveBlockDown() {
        for (int i = 0; i < 4; i++) {
            if (this.currentBlock.subblocks[i].ypos + this.currentBlock.yPos > gridMaxHeight + 2) {
                this.grid[this.currentBlock.subblocks[0].xpos + this.currentBlock.xPos][this.currentBlock.subblocks[0].ypos + this.currentBlock.yPos] = this.currentBlock.color;
                this.grid[this.currentBlock.subblocks[1].xpos + this.currentBlock.xPos][this.currentBlock.subblocks[1].ypos + this.currentBlock.yPos] = this.currentBlock.color;
                this.grid[this.currentBlock.subblocks[2].xpos + this.currentBlock.xPos][this.currentBlock.subblocks[2].ypos + this.currentBlock.yPos] = this.currentBlock.color;
                this.grid[this.currentBlock.subblocks[3].xpos + this.currentBlock.xPos][this.currentBlock.subblocks[3].ypos + this.currentBlock.yPos] = this.currentBlock.color;
                return false;
            }
        }
        if (this.currentBlock.height + this.currentBlock.yPos < gridMaxHeight) {
            if ((grid[currentBlock.subblocks[0].xpos + currentBlock.xPos][this.currentBlock.subblocks[0].ypos + this.currentBlock.yPos + 1] == -1) && (grid[currentBlock.subblocks[1].xpos + currentBlock.xPos][this.currentBlock.subblocks[1].ypos + this.currentBlock.yPos + 1] == -1) && (grid[currentBlock.subblocks[2].xpos + currentBlock.xPos][this.currentBlock.subblocks[2].ypos + this.currentBlock.yPos + 1] == -1) && (grid[currentBlock.subblocks[3].xpos + currentBlock.xPos][this.currentBlock.subblocks[3].ypos + this.currentBlock.yPos + 1] == -1)) {
                this.currentBlock.yPos++;
                return true;
            }
        }
        score++;
        this.grid[this.currentBlock.subblocks[0].xpos + this.currentBlock.xPos][this.currentBlock.subblocks[0].ypos + this.currentBlock.yPos] = this.currentBlock.color;
        this.grid[this.currentBlock.subblocks[1].xpos + this.currentBlock.xPos][this.currentBlock.subblocks[1].ypos + this.currentBlock.yPos] = this.currentBlock.color;
        this.grid[this.currentBlock.subblocks[2].xpos + this.currentBlock.xPos][this.currentBlock.subblocks[2].ypos + this.currentBlock.yPos] = this.currentBlock.color;
        this.grid[this.currentBlock.subblocks[3].xpos + this.currentBlock.xPos][this.currentBlock.subblocks[3].ypos + this.currentBlock.yPos] = this.currentBlock.color;
        return false;
    }

    private int[][] oldgrid;

    private int[][] grid;

    private int[][] newgrid;

    private int[] clearedLines;

    private int status;

    public int score;

    public int lines;

    public int level;

    public int timer;

    public Block currentBlock;

    public Block nextBlock;

    boolean timerEnabled;

    public void setTimer(int time) {
        this.timer = time;
    }

    public void clearCompleteLines() {
        int currentline = gridMaxHeight - 1;
        int linescleared = 0;
        this.newLevel = this.level;
        while (currentline > 0) {
            boolean linecomplete = true;
            for (int x = 0; x < gridMaxWidth; x++) {
                if (grid[x][currentline] == -1) {
                    linecomplete = false;
                }
            }
            if (linecomplete) {
                for (int x = 0; x < gridMaxWidth; x++) {
                    if (grid[x][currentline] == 7 && this.energy > 0) {
                        this.status = STATUS_EVOLVING;
                        for (int ypos = 0; ypos < gridMaxHeight; ypos++) {
                            for (int xpos = 0; xpos < gridMaxWidth; xpos++) {
                                oldgrid[xpos][ypos] = grid[xpos][ypos];
                            }
                        }
                    }
                }
                for (int y = currentline; y > 0; y--) {
                    for (int x = 0; x < gridMaxWidth; x++) {
                        if (y > 0) {
                            grid[x][y] = grid[x][y - 1];
                        } else {
                            grid[x][y] = -1;
                        }
                    }
                }
                linescleared++;
                energy++;
            } else {
                currentline--;
            }
        }
        switch(linescleared) {
            case 0:
                break;
            case 1:
                score = score + 25;
                break;
            case 2:
                score = score + 75;
                break;
            case 3:
                score = score + 100;
                break;
            case 4:
                score = score + 200;
                break;
            default:
                break;
        }
        lines = lines + linescleared;
        if (lines > 20 && lines <= 40) {
            this.newLevel = 2;
            timer = 900;
            return;
        }
        if (lines > 40 && lines <= 60) {
            this.newLevel = 2;
            timer = 800;
            return;
        }
        if (lines > 60 && lines <= 80) {
            this.newLevel = 3;
            timer = 700;
            return;
        }
        if (lines > 80 && lines <= 100) {
            this.newLevel = 4;
            timer = 600;
            return;
        }
        if (lines > 100 && lines <= 120) {
            this.newLevel = 5;
            timer = 500;
            return;
        }
        if (lines > 120 && lines <= 140) {
            this.newLevel = 6;
            timer = 400;
            return;
        }
        if (lines > 140 && lines <= 160) {
            this.newLevel = 7;
            timer = 300;
            return;
        }
        if (lines > 160 && lines <= 180) {
            this.newLevel = 8;
            timer = 200;
            return;
        }
        if (lines > 180 && lines <= 200) {
            this.newLevel = 9;
            timer = 100;
            return;
        }
        if (lines > 200) {
            this.newLevel = 10;
            timer = 50;
            return;
        }
    }

    public void flagCompletedLines() {
        for (int y = 0; y < gridMaxHeight; y++) {
            boolean linecleared = true;
            for (int x = 0; x < gridMaxWidth; x++) {
                if (this.grid[x][y] == -1) {
                    linecleared = false;
                    break;
                }
            }
            if (linecleared) {
                this.clearedLines[y] = 1;
            } else {
                this.clearedLines[y] = 0;
            }
        }
    }

    public int getClearedLineCount() {
        int cleared = 0;
        for (int line = 0; line < gridMaxHeight; line++) {
            if (this.clearedLines[line] != 0) {
                cleared++;
            }
        }
        return cleared;
    }

    public void rotateCurrentBlockClockwise() {
        int currentorientation = this.currentBlock.orientation;
        this.currentBlock.rotateClockwise();
        this.currentBlock.recalcBlockOrientation();
        for (int i = 0; i < 4; i++) {
            if (this.currentBlock.xPos + this.currentBlock.subblocks[i].xpos >= this.gridMaxWidth) {
                this.currentBlock.orientation = currentorientation;
                this.currentBlock.recalcBlockOrientation();
                return;
            }
            if (this.currentBlock.yPos + this.currentBlock.subblocks[i].ypos >= this.gridMaxHeight) {
                this.currentBlock.orientation = currentorientation;
                this.currentBlock.recalcBlockOrientation();
                return;
            }
            if (grid[this.currentBlock.subblocks[i].xpos + this.currentBlock.xPos][this.currentBlock.subblocks[i].ypos + this.currentBlock.yPos] != -1) {
                this.currentBlock.orientation = currentorientation;
                this.currentBlock.recalcBlockOrientation();
                return;
            }
        }
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return this.score;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }

    public String getLevelName() {
        String str = "" + this.level;
        while (str.length() < 2) {
            str = "0" + str;
        }
        str = "M" + str;
        return str;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    public int getLines() {
        return this.lines;
    }

    public void setTimerEnabled(boolean flag) {
        this.timerEnabled = flag;
    }

    public boolean isTimerEnabled() {
        return this.timerEnabled;
    }

    public Block getCurrentBlock() {
        return this.currentBlock;
    }

    public Block getNextBlock() {
        return this.nextBlock;
    }

    public int getTimer() {
        return this.timer;
    }

    public int getEnergy() {
        return this.energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int[] getClearedLines() {
        return this.clearedLines;
    }

    int newLevel;

    int startingLevel;

    public int gridMaxHeight;

    public int gridMaxWidth;

    public int energy;

    private int step;

    public int getCurrentStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    private int boundaryCondition;

    private java.util.Random randomgen;

    private boolean blockPlaced;

    public boolean isBlockPlaced() {
        return blockPlaced;
    }

    public int getGameType() {
        return Game.GAME_MONOLITH;
    }

    public static void main(String[] args) {
    }
}
