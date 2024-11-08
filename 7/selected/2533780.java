package echecs.generateur;

import echecs.ia.Evaluation;
import java.applet.Applet;

public class Generation extends Evaluation {

    public Generation(Applet ref) {
        super(ref);
    }

    boolean ababort = false;

    public void genmove() {
        deep++;
        ababort = false;
        if (deep % 2 != 0) {
            minimax[deep] = 2000.0F;
            alphabeta[deep] = 3000.0F;
        } else {
            minimax[deep] = -2000.0F;
            alphabeta[deep] = -3000.0F;
        }
        for (int i = 21; i < 99; i++) {
            if (board[i] % 100 / 10 == color) {
                switch(board[i] % 10) {
                    case 1:
                        if (color == 1) {
                            if (board[i - 10] == 0) {
                                simulize(i, i - 10);
                            }
                            if (board[i - 9] % 100 / 10 == 2) {
                                simulize(i, i - 9);
                            }
                            if (board[i - 11] % 100 / 10 == 2) {
                                simulize(i, i - 11);
                            }
                            if ((i > 80) && ((board[i - 10] == 0) && (board[i - 20] == 0))) {
                                simulize(i, i - 20);
                            }
                        } else {
                            if (board[i + 10] == 0) {
                                simulize(i, i + 10);
                            }
                            if (board[i + 9] % 100 / 10 == 1) {
                                simulize(i, i + 9);
                            }
                            if (board[i + 11] % 100 / 10 == 1) {
                                simulize(i, i + 11);
                            }
                            if ((i < 39) && ((board[i + 10] == 0) && (board[i + 20] == 0))) {
                                simulize(i, i + 20);
                            }
                        }
                        break;
                    case 2:
                        simulize(i, i + 12);
                        simulize(i, i - 12);
                        simulize(i, i + 21);
                        simulize(i, i - 21);
                        simulize(i, i + 19);
                        simulize(i, i - 19);
                        simulize(i, i + 8);
                        simulize(i, i - 8);
                        break;
                    case 5:
                    case 3:
                        multisimulize(i, -9);
                        multisimulize(i, -11);
                        multisimulize(i, +9);
                        multisimulize(i, +11);
                        if (board[i] % 10 == 3) {
                            break;
                        }
                    case 4:
                        multisimulize(i, -10);
                        multisimulize(i, +10);
                        multisimulize(i, -1);
                        multisimulize(i, +1);
                        break;
                    case 6:
                        if ((board[i] / 100 == 1) && (!ischeck())) {
                            if (((board[i + 1] == 0) && (board[i + 2] == 0)) && (board[i + 3] / 100 == 1)) {
                                board[i + 1] = board[i] % 100;
                                board[i] = 0;
                                if (!ischeck()) {
                                    board[i] = board[i + 1];
                                    board[i + 1] = board[i + 3] % 100;
                                    board[i + 3] = 0;
                                    simulize(i, i + 2);
                                    board[i + 3] = board[i + 1] + 100;
                                    board[i + 1] = board[i];
                                }
                                board[i] = board[i + 1] + 100;
                                board[i + 1] = 0;
                            }
                            if (((board[i - 1] == 0) && (board[i - 2] == 0)) && ((board[i - 3] == 0) && (board[i - 4] / 100 == 1))) {
                                board[i - 1] = board[i] % 100;
                                board[i] = 0;
                                if (!ischeck()) {
                                    board[i] = board[i - 1];
                                    board[i - 1] = board[i - 4] % 100;
                                    board[i - 4] = 0;
                                    simulize(i, i - 2);
                                    board[i - 4] = board[i - 1] + 100;
                                    board[i - 1] = board[i];
                                }
                                board[i] = board[i - 1] + 100;
                                board[i - 1] = 0;
                            }
                        }
                        simulize(i, i + 1);
                        simulize(i, i - 1);
                        simulize(i, i + 10);
                        simulize(i, i - 10);
                        simulize(i, i + 9);
                        simulize(i, i - 9);
                        simulize(i, i + 11);
                        simulize(i, i - 11);
                }
            }
            if (i % 10 == 8) {
                i += 2;
            }
        }
        deep--;
        ababort = false;
    }

    public void multisimulize(int start, int inc) {
        int to = start;
        while ((board[to + inc] != 99) && (board[to + inc] % 100 / 10 != color)) {
            to += inc;
            if (board[to] != 0) {
                simulize(start, to);
                return;
            }
            simulize(start, to);
        }
        simulize(start, to);
    }

    public void simulize(int start, int end) {
        if ((board[end] == 99) || (board[end] % 100 / 10 == color)) {
            return;
        }
        if (ababort) {
            return;
        }
        int orgstart = board[start];
        int orgend = board[end];
        board[end] = board[start];
        board[start] = 0;
        if ((board[end] % 10 == 1) && ((end < 29) || (end > 90))) {
            board[end] += 4;
        }
        if (!ischeck()) {
            if (deep == 1) {
                movelist[movecounter] = start * 100 + end;
                movecounter++;
            }
            if (target == deep) {
                value = evaluation();
            } else {
                if (color == 1) {
                    color = 2;
                } else {
                    color = 1;
                }
                genmove();
                value = minimax[deep + 1];
                if (deep % 2 != 0) {
                    if (value < alphabeta[deep]) {
                        alphabeta[deep] = value;
                    }
                } else {
                    if (value > alphabeta[deep]) {
                        alphabeta[deep] = value;
                    }
                }
                if (color == 1) {
                    color = 2;
                } else {
                    color = 1;
                }
            }
            if (deep % 2 == 0) {
                if (value > minimax[deep]) {
                    minimax[deep] = value;
                }
                if (value > alphabeta[deep - 1]) {
                    ababort = true;
                }
            } else {
                if (value <= minimax[deep]) {
                    minimax[deep] = value;
                    if (deep == 1) {
                        move = start * 100 + end;
                    }
                }
                if (value < alphabeta[deep - 1]) {
                    ababort = true;
                }
            }
        }
        board[start] = orgstart;
        board[end] = orgend;
    }

    public boolean ischeck() {
        int king = 0;
        for (int i = 21; i < 99; i++) {
            if ((board[i] % 100 / 10 == color) && (board[i] % 10 == 6)) {
                king = i;
                break;
            }
            if (i % 10 == 8) {
                i += 2;
            }
        }
        if ((board[king - 21] % 10 == 2) && (board[king - 21] % 100 / 10 != color)) {
            return true;
        }
        if ((board[king + 21] % 10 == 2) && (board[king + 21] % 100 / 10 != color)) {
            return true;
        }
        if ((board[king - 19] % 10 == 2) && (board[king - 19] % 100 / 10 != color)) {
            return true;
        }
        if ((board[king + 19] % 10 == 2) && (board[king + 19] % 100 / 10 != color)) {
            return true;
        }
        if ((board[king - 8] % 10 == 2) && (board[king - 8] % 100 / 10 != color)) {
            return true;
        }
        if ((board[king + 8] % 10 == 2) && (board[king + 8] % 100 / 10 != color)) {
            return true;
        }
        if ((board[king - 12] % 10 == 2) && (board[king - 12] % 100 / 10 != color)) {
            return true;
        }
        if ((board[king + 12] % 10 == 2) && (board[king + 12] % 100 / 10 != color)) {
            return true;
        }
        int j = king;
        while (board[j - 9] != 99) {
            j -= 9;
            if (board[j] % 100 / 10 == color) {
                break;
            }
            if (board[j] == 0) {
                continue;
            }
            if ((board[j] % 10 == 3) || (board[j] % 10 == 5)) {
                return true;
            } else {
                break;
            }
        }
        j = king;
        while (board[j + 9] != 99) {
            j += 9;
            if (board[j] % 100 / 10 == color) {
                break;
            }
            if (board[j] == 0) {
                continue;
            }
            if ((board[j] % 10 == 3) || (board[j] % 10 == 5)) {
                return true;
            } else {
                break;
            }
        }
        j = king;
        while (board[j - 11] != 99) {
            j -= 11;
            if (board[j] % 100 / 10 == color) {
                break;
            }
            if (board[j] == 0) {
                continue;
            }
            if ((board[j] % 10 == 3) || (board[j] % 10 == 5)) {
                return true;
            } else {
                break;
            }
        }
        j = king;
        while (board[j + 11] != 99) {
            j += 11;
            if (board[j] % 100 / 10 == color) {
                break;
            }
            if (board[j] == 0) {
                continue;
            }
            if ((board[j] % 10 == 3) || (board[j] % 10 == 5)) {
                return true;
            } else {
                break;
            }
        }
        j = king;
        while (board[j - 10] != 99) {
            j -= 10;
            if (board[j] % 100 / 10 == color) {
                break;
            }
            if (board[j] == 0) {
                continue;
            }
            if ((board[j] % 10 == 4) || (board[j] % 10 == 5)) {
                return true;
            } else {
                break;
            }
        }
        j = king;
        while (board[j + 10] != 99) {
            j += 10;
            if (board[j] % 100 / 10 == color) {
                break;
            }
            if (board[j] == 0) {
                continue;
            }
            if ((board[j] % 10 == 4) || (board[j] % 10 == 5)) {
                return true;
            } else {
                break;
            }
        }
        j = king;
        while (board[j - 1] != 99) {
            j -= 1;
            if (board[j] % 100 / 10 == color) {
                break;
            }
            if (board[j] == 0) {
                continue;
            }
            if ((board[j] % 10 == 4) || (board[j] % 10 == 5)) {
                return true;
            } else {
                break;
            }
        }
        j = king;
        while (board[j + 1] != 99) {
            j += 1;
            if (board[j] % 100 / 10 == color) {
                break;
            }
            if (board[j] == 0) {
                continue;
            }
            if ((board[j] % 10 == 4) || (board[j] % 10 == 5)) {
                return true;
            } else {
                break;
            }
        }
        if (color == 1) {
            if ((board[king - 11] % 10 == 1) && (board[king - 11] % 100 / 10 == 2)) {
                return true;
            }
            if ((board[king - 9] % 10 == 1) && (board[king - 9] % 100 / 10 == 2)) {
                return true;
            }
        } else {
            if ((board[king + 11] % 10 == 1) && (board[king + 11] % 100 / 10 == 1)) {
                return true;
            }
            if ((board[king + 9] % 10 == 1) && (board[king + 9] % 100 / 10 == 1)) {
                return true;
            }
        }
        if (board[king + 1] % 10 == 6) {
            return true;
        }
        if (board[king - 1] % 10 == 6) {
            return true;
        }
        if (board[king + 10] % 10 == 6) {
            return true;
        }
        if (board[king - 10] % 10 == 6) {
            return true;
        }
        if (board[king + 11] % 10 == 6) {
            return true;
        }
        if (board[king - 11] % 10 == 6) {
            return true;
        }
        if (board[king + 9] % 10 == 6) {
            return true;
        }
        if (board[king - 9] % 10 == 6) {
            return true;
        }
        return false;
    }

    public boolean isvalid(int move) {
        for (int i = 0; i < movecounter; i++) {
            if (movelist[i] == move) {
                return true;
            }
        }
        return false;
    }
}
