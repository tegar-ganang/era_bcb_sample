import java.util.Collections;
import java.util.*;

public class e2_basicbacktracker implements Runnable {

    e2piecelist[] piecelisttab;

    e2piece[] currentpiecetab;

    int level;

    int levelmin, levelmax;

    int best_level;

    int[] rotation;

    int[] collist;

    int[] rowlist;

    long count_from_start;

    long count_from_last;

    long start_time;

    long previous_time_count;

    long last_time_count;

    long next_time_count;

    long temp_time;

    long time_between_count;

    long display_every_count_nb;

    int count_bestlevel;

    int count_worstlevel;

    boolean stop;

    boolean killed;

    boolean solved;

    boolean checkrotation;

    boolean rotationfound;

    e2game maingame;

    e2Applet topapplet;

    Thread mainthread;

    public e2_basicbacktracker(e2game in_game) {
        maingame = in_game;
        init_solver();
    }

    public void init_solver() {
        int i, j, index;
        int nbpositions;
        nbpositions = maingame.rownb * maingame.colnb;
        levelmin = 0;
        levelmax = -1;
        level = 0;
        best_level = 0;
        solved = false;
        stop = false;
        killed = false;
        piecelisttab = new e2piecelist[nbpositions];
        currentpiecetab = new e2piece[nbpositions];
        rotation = new int[nbpositions];
        collist = new int[nbpositions];
        rowlist = new int[nbpositions];
        checkrotation = true;
        rotationfound = false;
        mainthread = new Thread(this);
        start_time = (long) (new Date()).getTime();
        count_from_start = 0;
        count_from_last = 0;
        count_bestlevel = 0;
        count_worstlevel = 0;
        last_time_count = start_time;
        display_every_count_nb = 1000000;
        System.out.println(" backtracker init");
    }

    public void add_all_positions() {
        int index, i, j;
        index = 0;
        levelmax = 0;
        for (i = 0; i < maingame.rownb; i++) {
            for (j = 0; j < maingame.colnb; j++) {
                collist[index] = j;
                rowlist[index] = i;
                piecelisttab[index] = null;
                index++;
                levelmax++;
            }
        }
    }

    public boolean is_position_selected(int in_col, int in_row) {
        int i;
        boolean found;
        found = false;
        for (i = 0; i <= levelmax; i++) {
            if (collist[i] == in_col && rowlist[i] == in_row) {
                found = true;
            }
        }
        return found;
    }

    public boolean add_position(int in_col, int in_row) {
        boolean found;
        found = is_position_selected(in_col, in_row);
        if (found == false) {
            levelmax++;
            collist[levelmax] = in_col;
            rowlist[levelmax] = in_row;
        }
        return found;
    }

    public void invert_position_selection(int in_col, int in_row) {
        int i, j;
        boolean found;
        found = false;
        for (i = 0; i <= levelmax; i++) {
            if (collist[i] == in_col && rowlist[i] == in_row) {
                found = true;
                break;
            }
        }
        if (found == true) {
            for (j = i; j < levelmax; j++) {
                collist[j] = collist[j + 1];
                rowlist[j] = rowlist[j + 1];
            }
            levelmax--;
        } else {
            levelmax++;
            collist[levelmax] = in_col;
            rowlist[levelmax] = in_row;
        }
    }

    public void restart_solver() {
        solved = false;
        System.out.println("solver restarted");
    }

    public void kill() {
        killed = true;
    }

    public void run() {
        e2colorpattern temppattern;
        e2piece temppiece;
        System.out.println(" backtracker started");
        while (killed == false) {
            while (solved == false && stop == false && level <= levelmax) {
                if (piecelisttab[level] == null) {
                    temppattern = maingame.get_pattern_at(collist[level], rowlist[level]);
                    piecelisttab[level] = maingame.get_unplaced_matching_pieces(temppattern);
                    Collections.shuffle(piecelisttab[level].list);
                    piecelisttab[level].restart();
                    if (piecelisttab[level].hasNext()) {
                        currentpiecetab[level] = piecelisttab[level].next();
                        currentpiecetab[level].reset_rotation();
                        rotation[level] = -1;
                    } else {
                        currentpiecetab[level] = null;
                    }
                }
                temppattern = maingame.get_pattern_at(collist[level], rowlist[level]);
                rotationfound = false;
                rotation[level]++;
                while (currentpiecetab[level] != null && rotation[level] < 4 && rotationfound == false) {
                    if (currentpiecetab[level].is_matching_pattern_with_rotation(temppattern)) {
                        rotationfound = true;
                    } else {
                        rotation[level]++;
                        currentpiecetab[level].reset_rotation();
                        if (rotation[level] < 4) {
                            currentpiecetab[level].rotate(rotation[level]);
                        }
                    }
                }
                if (rotationfound == true) {
                    maingame.place_piece_at(currentpiecetab[level].id, collist[level], rowlist[level], 0);
                    level++;
                    if (level > levelmax) {
                        solved = true;
                        System.out.println(" solution found");
                    }
                    if (level > best_level) {
                        best_level = level;
                    }
                    if (level > count_bestlevel) {
                        count_bestlevel = level;
                    }
                    count_from_start++;
                    count_from_last++;
                    if (count_from_last == display_every_count_nb) {
                        temp_time = (long) (new Date()).getTime();
                        time_between_count = temp_time - last_time_count;
                        System.out.println(count_from_last + " pieces placed in " + time_between_count + " ms (time=" + temp_time + ") (count=" + count_from_start + ") (bestlevel=" + best_level + ") (incrbestlevel=" + count_bestlevel + ") (incrworstlevel=" + count_worstlevel + ")");
                        count_from_last = 0;
                        last_time_count = temp_time;
                        count_bestlevel = level;
                        count_worstlevel = level;
                    }
                } else {
                    if (piecelisttab[level].hasNext()) {
                        currentpiecetab[level] = piecelisttab[level].next();
                        currentpiecetab[level].reset_rotation();
                        rotation[level] = -1;
                    } else {
                        piecelisttab[level] = null;
                        level--;
                        if (level < count_worstlevel) {
                            count_worstlevel = level;
                        }
                        if (level < levelmin) {
                            stop = true;
                            System.out.println(" NO SOLUTION");
                        } else {
                            maingame.unplace_piece_at(collist[level], rowlist[level]);
                        }
                    }
                }
                try {
                    Thread.sleep(0000);
                } catch (InterruptedException ex) {
                }
            }
        }
        System.out.println(" backtracker finished");
    }
}
