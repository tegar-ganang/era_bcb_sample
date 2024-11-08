package echecs.engine;

import javax.swing.JApplet;

public class Generateur extends GenerateurUtils {

    public Generateur(JApplet ref) {
        super(ref);
    }

    public void genmove() {
        for (int i = 21; i < 99; i++) {
            if (board[i] % 100 / 10 == color) {
                switch(board[i] % 10) {
                    case PAWN:
                        if (color == WHITE) {
                            WhitePawn(i);
                        } else {
                            BlackPawn(i);
                        }
                        break;
                    case KNIGHT:
                        for (int depl = 0; depl < deplacementKnight.length; depl++) {
                            simulize(i, i + deplacementKnight[depl]);
                        }
                        break;
                    case QUEEN:
                    case BISHOP:
                        for (int depl = 0; depl < deplacementBishop.length; depl++) {
                            multisimulize(i, deplacementBishop[depl]);
                        }
                        if (board[i] % 10 == 3) {
                            break;
                        }
                    case ROOK:
                        for (int depl = 0; depl < deplacementRook.length; depl++) {
                            multisimulize(i, deplacementRook[depl]);
                        }
                        break;
                    case KING:
                        if ((board[i] / 100 == 1) && (!ischeck())) {
                            if (((board[i + 1] == 0) && (board[i + 2] == 0)) && (board[i + 3] / 100 == 1)) {
                                petitRoque(i);
                            }
                            if (((board[i - 1] == 0) && (board[i - 2] == 0)) && ((board[i - 3] == 0) && (board[i - 4] / 100 == 1))) {
                                grandRoque(i);
                            }
                        }
                        for (int depl = 0; depl < deplacementKing.length; depl++) {
                            simulize(i, i + deplacementKing[depl]);
                        }
                }
            }
            if (i % 10 == 8) {
                i += 2;
            }
        }
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
        if (isInBoard(end)) {
            int[] ret = simuleMove(start, end);
            changePawnIfLastRange(end);
            if (!ischeck()) {
                addMove(start, end);
            }
            undoSimuleMove(start, ret, end);
        }
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

    public void WhitePawn(int i) {
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
    }

    public void BlackPawn(int i) {
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

    public void grandRoque(int i) {
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

    public void petitRoque(int i) {
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
}
