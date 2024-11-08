package echecs.gen;

import javax.swing.JApplet;

public abstract class AGenerateurA extends AGenerateurB {

    public AGenerateurA(JApplet ref) {
        super(ref);
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
