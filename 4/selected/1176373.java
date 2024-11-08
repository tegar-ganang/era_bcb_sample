package net.sourceforge.sevents.modules.symplegades;

import net.sourceforge.sevents.mainmodule.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import net.sourceforge.sevents.precond.Preconditioning;
import net.sourceforge.sevents.precond.Variables;
import net.sourceforge.sevents.scripthandle.Log;
import static net.sourceforge.sevents.scripthandle.Analyzers.*;
import net.sourceforge.sevents.scripthandle.SStructs.MyEvent;
import net.sourceforge.sevents.scripthandle.SStructs.VarRefer;
import net.sourceforge.sevents.scripthandle.StringLibrary;
import net.sourceforge.sevents.util.FileUtils;
import net.sourceforge.sevents.util.SSettings;
import static net.sourceforge.sevents.util.SBundle.tr;

/**
 *
 * @author becase
 */
public class Symplegades implements java.io.Serializable {

    private static final int countPlayerStones = 10;

    /**
     * Move action here
     */
    public static final int go = 1 << 0;

    public static final int throwStone = 1 << 1;

    public static final int wait = 1 << 2;

    private static final String goS = "go";

    private static final String throwStoneS = "throwStone";

    private static final String waitS = "wait";

    /**
     * Fill flags here
     */
    public static final int me = 1 << 0;

    public static final int enemy = 1 << 1;

    public static final int mountainUp = 1 << 2;

    public static final int mountainDown = 1 << 3;

    public static final int stone = 1 << 4;

    public static final int free = 1 << 5;

    private static final String meS = "me";

    private static final String enemyS = "enemy";

    private static final String mountainUpS = "mountain";

    private static final String mountainDownS = "mountainDown";

    private static final String stoneS = "stone";

    private static final String freeS = "free";

    private static final Color meC = Color.RED;

    private static final Color enemyC = Color.BLUE;

    private static final Color mountainUpC = Color.DARK_GRAY;

    private static final Color mountainDownC = Color.GRAY;

    private static final Color stoneC = Color.YELLOW;

    private static final Color freeC = new Color(0x2abcec);

    private static final double mountainUpP = 1. / 4;

    private static final double mountainDownP = 1. / 2;

    private static final int MAX = 11;

    private static final int playerPriority = 2;

    private static final int mountainPriority = 4;

    private int sea[][] = new int[MAX][MAX];

    private int mx, my;

    private int ex, ey;

    private String directionS = "direction";

    private String actionS = "action";

    private String mxS = "mx", myS = "my";

    private String exS = "ex", eyS = "ey";

    private static int countPlayers = 0;

    private final int cPlayers = 2;

    private final int cDirections = 6;

    private SPlayer players[] = new SPlayer[cPlayers];

    private Point dirs[] = new Point[cDirections + 1];

    /**
     * Class for player
     */
    public class SPlayer implements java.io.Serializable {

        private String name;

        private String teamName;

        private String fileName;

        private String id;

        private String libraryToLoad;

        private int direction = 0;

        private int action = wait;

        private int countStones = countPlayerStones;

        /**
         * Set direction to player
         * @param dir direction
         */
        public void setDirection(int dir) {
            final int cDir = 6;
            if (dir < 0 || dir > cDir) {
                dir = 0;
            }
            if (StringLibrary.getNumberFromString(id) == 1) {
                direction = dir;
            } else {
                direction = (1 - (dir - 1) + cDir) % cDir + 1;
            }
        }

        /**
         * Set action for next step
         * @param act go, throwStone or wait
         */
        public void setAction(int act) {
            action = act;
        }

        /**
         * Get library to load
         */
        private void getLibraryToLoad() {
            if (libraryToLoad != null) {
                return;
            }
            String libDir = SSettings.getString(FileUtils.writeLibDirName);
            String libExt = SSettings.getString(FileUtils.writeLibExtention);
            boolean isWinSys = false;
            try {
                isWinSys = System.getProperty("os.name").split(" ")[0].toLowerCase().equals("windows");
            } catch (Exception e) {
            }
            String[] files = FileUtils.getFiles(libDir);
            int i;
            if (isWinSys) {
                for (i = 0; i < files.length; i++) {
                    if (StringLibrary.getFileExtention(files[i]).equalsIgnoreCase(libExt)) {
                        if (this.name.equalsIgnoreCase(StringLibrary.getFileName(files[i]))) {
                            libraryToLoad = files[i];
                            break;
                        }
                    }
                }
            } else {
                for (i = 0; i < files.length; i++) {
                    if (StringLibrary.getFileExtention(files[i]).equals(libExt)) {
                        if (this.name.equals(StringLibrary.getFileName(files[i]))) {
                            libraryToLoad = files[i];
                            break;
                        }
                    }
                }
            }
            return;
        }

        /**
         * Second verion of run library
         */
        public void runThread() {
            if (!LibLoader.isRunning()) {
                try {
                    LibLoader.startProcess();
                } catch (IOException ioe) {
                    Log.war("Cannot run libLoader", Log.WARNING);
                    ioe.printStackTrace(System.err);
                    return;
                }
            }
            if (libraryToLoad != null) {
                net.sourceforge.sevents.scripthandle.Sevents.getProcess().setTeamId(id);
                try {
                    LibLoader.loadLibrary(libraryToLoad);
                    LibLoader.setFunction("run", null);
                    LibLoader.runThread(SSettings.getInt(FileUtils.writeMaxProcDelay, FileUtils.defaultMaxProcDelay));
                    if (LibLoader.isRunning()) {
                        LibLoader.unloadLibrary();
                    }
                } catch (IOException ioe) {
                    Log.war("runThread2: " + ioe.getClass().getName() + " " + ioe.getMessage(), Log.WARNING);
                }
                net.sourceforge.sevents.scripthandle.Sevents.getProcess().setTeamId("");
            }
        }

        /**
         * Init variables
         * @param var
         * @return true, if init successfull, false else
         */
        public boolean initVars(Variables var) {
            String prefix = id + "_";
            var.putVariable(prefix + mxS, mx + 1, true);
            var.putVariable(prefix + myS, my + 1, true);
            var.putVariable(prefix + exS, ex + 1, true);
            var.putVariable(prefix + eyS, ey + 1, true);
            var.putVariable(prefix + directionS, direction, false);
            var.putVariable(prefix + actionS, action, false);
            return Preconditioning.getHandleForProgram(fileName, var, id);
        }

        public boolean initEvents() {
            return net.sourceforge.sevents.scripthandle.Sevents.getProcess().addEvents(fileName, id, playerPriority);
        }

        public void initWin(HashMap<String, MyEvent> eventNames, Vector<MyEvent> allEvents) {
            MyEvent me = new MyEvent();
            String winPrefix = "_win_";
            me.setPriority(SSettings.getInt(FileUtils.writeMaxPriority));
            me.setName(winPrefix + id);
            me.setProbability(1.);
            me.setId(id);
            StringBuilder sb = new StringBuilder();
            sb.append(myS).append(" == ").append(MAX).append(" || ").append(eyS).append(" <= 0").append(" || ").append(exS).append(" <= 0");
            try {
                me.setCondition(getExpression(sb.toString(), id));
            } catch (Exception e) {
                Log.err("initWin_" + id + " " + e.getMessage(), Log.ERROR);
            }
            int n;
            n = allEvents.size();
            eventNames.put(me.getName(), me);
            allEvents.add(n++, me);
        }

        /**
         * Moved
         */
        void move() {
            int x, y, xe, ye, vx, vy;
            int eC;
            boolean isFirst = (StringLibrary.getNumberFromString(id) == 1);
            if (isFirst) {
                x = mx;
                y = my;
                xe = ex;
                ye = ey;
                eC = enemy;
            } else {
                x = ex;
                y = ey;
                xe = mx;
                ye = my;
                eC = me;
            }
            if (x < 0 || y < 0) {
                return;
            }
            if (direction == 0) {
                return;
            }
            vx = dirs[direction].x;
            vy = dirs[direction].y;
            int nx, ny;
            nx = x + vx;
            ny = y + vy;
            if (nx < 0 || nx >= MAX) {
                return;
            }
            if (ny < 0 || ny >= MAX) {
                return;
            }
            if (action == go) {
                if (nx == xe && ny == ye) {
                    return;
                }
                if (sea[nx][ny] == eC) {
                    return;
                }
                if (sea[nx][ny] == mountainUp) {
                    return;
                }
                if (isFirst) {
                    setM(nx, ny);
                } else {
                    setE(nx, ny);
                }
            }
            if (action == throwStone) {
                if (countStones <= 0) {
                    return;
                }
                countStones--;
                if (sea[nx][ny] == free || sea[nx][ny] == mountainDown) {
                    sea[nx][ny] = stone;
                }
            }
            Log.out(id + "_moved", Log.FILE);
        }

        /**
         * Set path to scenario
         * @param name name of scenario
         */
        public void setFileName(String name) {
            fileName = name;
            this.name = StringLibrary.getFileName(name);
            teamName = this.name;
            if (!LibLoader.isRunning()) {
                try {
                    LibLoader.startProcess();
                } catch (IOException ioe) {
                    ioe.printStackTrace(System.err);
                }
            }
            getLibraryToLoad();
            if (LibLoader.isRunning()) {
                if (libraryToLoad != null) {
                    net.sourceforge.sevents.scripthandle.Sevents.getProcess().setTeamId(id);
                    try {
                        LibLoader.loadLibrary(libraryToLoad);
                        LibLoader.setFunction("getName", null);
                        LibLoader.runThread(1000);
                        String teamName = LibLoader.getFunctionResult();
                        if ("failed".compareTo(teamName) != 0) {
                            this.teamName = teamName;
                        }
                        if (LibLoader.isRunning()) {
                            LibLoader.unloadLibrary();
                        }
                    } catch (IOException ioe) {
                        Log.war(ioe.getClass().getName() + ": " + ioe.getMessage(), Log.WARNING);
                        try {
                            LibLoader.stopProcess();
                        } catch (IOException io) {
                        }
                    }
                    net.sourceforge.sevents.scripthandle.Sevents.getProcess().setTeamId("");
                }
            }
            Log.out(id + "_name=" + this.teamName, Log.FILE);
        }

        /**
         * Sets idenfifier of player
         * @param nId Identifier of player
         */
        public void setId(String nId) {
            id = nId;
        }

        /**
         * Get sea value
         * @param i x coordinate
         * @param j y coordinate
         * @return value of sea
         */
        public int getSea(int i, int j) {
            int res = 0;
            if (StringLibrary.getNumberFromString(id) == 1) {
                res = sea[i][j];
            } else {
                res = sea[j][i];
                if (res == me) {
                    res = enemy;
                } else {
                    if (res == enemy) {
                        res = me;
                    }
                }
            }
            if (res == mountainDown) {
                res = free;
            }
            return res;
        }

        /**
         * Get identifier of this player
         * @return identifier string
         */
        public String getId() {
            return id;
        }
    }

    private static class SPoint {

        public double x, y;

        private double sqr(double a) {
            return a * a;
        }

        public double len() {
            return Math.sqrt(sqr(x) + sqr(y));
        }

        public void multiply(double a) {
            x *= a;
            y *= a;
        }

        public void add(SPoint p) {
            x += p.x;
            y += p.y;
        }

        public Point toPoint() {
            return new Point((int) (x), (int) (y));
        }
    }

    private void setFinished() {
        Variables var;
        var = net.sourceforge.sevents.scripthandle.Sevents.getProcess().getVars();
        var.setBooleanValue("terminate", true);
        var.updateValue("terminate");
    }

    private void setE(int x, int y) {
        Variables var;
        var = net.sourceforge.sevents.scripthandle.Sevents.getProcess().getVars();
        String pr1, pr2;
        if (ex < 0 || ey < 0) {
            return;
        }
        if (ex != x || ey != y) {
            sea[ex][ey] = enemy;
        }
        ex = x;
        ey = y;
        x++;
        y++;
        pr1 = players[0].getId() + "_";
        pr2 = players[1].getId() + "_";
        var.setIntValue(pr1 + exS, x);
        var.updateValue(pr1 + exS);
        var.setIntValue(pr1 + eyS, y);
        var.updateValue(pr1 + eyS);
        var.setIntValue(pr2 + mxS, y);
        var.updateValue(pr2 + mxS);
        var.setIntValue(pr2 + myS, x);
        var.updateValue(pr2 + myS);
    }

    private void setM(int x, int y) {
        Variables var;
        var = net.sourceforge.sevents.scripthandle.Sevents.getProcess().getVars();
        String pr1, pr2;
        if (mx < 0 || my < 0) {
            return;
        }
        if (mx != x || my != y) {
            sea[mx][my] = me;
        }
        mx = x;
        my = y;
        x++;
        y++;
        pr1 = players[0].getId() + "_";
        pr2 = players[1].getId() + "_";
        var.setIntValue(pr1 + mxS, x);
        var.updateValue(pr1 + mxS);
        var.setIntValue(pr1 + myS, y);
        var.updateValue(pr1 + myS);
        var.setIntValue(pr2 + exS, y);
        var.updateValue(pr2 + exS);
        var.setIntValue(pr2 + eyS, x);
        var.updateValue(pr2 + eyS);
    }

    private boolean treatMountain(String eventName) {
        if (eventName.indexOf("_mountain") != 0) {
            return false;
        }
        StringLibrary.StringToken st = new StringLibrary.StringToken(eventName, "_", "");
        int x, y;
        String action = st.nextToken();
        x = getInt(st.nextToken()) - 1;
        y = getInt(st.nextToken()) - 1;
        if (action.equals("mountainUp")) {
            if (x == mx && y == my) {
                setM(-1, -1);
            }
            if (x == ex && y == ey) {
                setE(-1, -1);
            }
            sea[x][y] = mountainUp;
        } else {
            sea[x][y] = mountainDown;
        }
        return true;
    }

    private boolean treatWin(String eventName) {
        if (eventName.indexOf("_win") != 0) {
            return false;
        }
        StringLibrary.StringToken st = new StringLibrary.StringToken(eventName, "_", "");
        if (!"win".equals(st.nextToken())) {
            return false;
        }
        String id = st.nextToken();
        int nid = StringLibrary.getNumberFromString(id);
        nid--;
        players[nid].teamName = players[nid].teamName + " WIN!!!";
        paint();
        setFinished();
        return true;
    }

    public void eventStarted(String eventName) {
        if (treatMountain(eventName)) {
            return;
        }
        if (treatWin(eventName)) {
            return;
        }
    }

    public void varChanged(VarRefer v) {
        int i = v.name.indexOf("_");
        if (i < 0) {
            return;
        }
        String id = v.name.substring(0, i);
        String var = v.name.substring(i + 1);
        if (id.length() == 0) {
            return;
        }
        if (!directionS.equals(var) && !actionS.equals(var)) {
            return;
        }
        int idNum = StringLibrary.getNumberFromString(id);
        idNum--;
        if (idNum < 0 || idNum > 1) {
            return;
        }
        if (directionS.equals(var)) {
            if (v.value instanceof Integer) {
                players[idNum].setDirection(((Integer) v.value).intValue());
            }
        } else {
            if (v.value instanceof Integer) {
                players[idNum].setAction(((Integer) v.value).intValue());
            }
        }
    }

    /**
     * Execute one move of players
     */
    public void movePlayers() {
        int ind[] = new int[2];
        int i;
        ind[0] = (int) (Math.random() * 2);
        ind[1] = (ind[0] == 0) ? 1 : 0;
        for (i = 0; i < 2; i++) {
            players[ind[i]].move();
        }
    }

    /**
     * Is free sea cell?
     * @param i x coordinate to check
     * @param j y coordinate to check
     * @return true, if cell is free, false else
     */
    private boolean isFree(int i, int j) {
        int cMax = MAX / 2;
        if (i == cMax && (j == 0 || j == MAX - 1)) {
            return false;
        }
        if (j == cMax && (i == 0 || i == MAX - 1)) {
            return false;
        }
        int d = sea[i][j];
        return (d != (int) me) && (d != (int) enemy);
    }

    private static final int length = 2;

    private static final int radix = 10;

    /**
     * Get numerical string from number
     * @param n number to convert
     * @return String, consists from 2 digits
     */
    public static String getString(int n) {
        int i;
        StringBuilder sb = new StringBuilder();
        for (i = 0; i < length; i++) {
            sb.insert(0, (char) ('0' + n % radix));
            n /= radix;
        }
        return sb.toString();
    }

    /**
     * Get number from numerical string
     * @param s numerical string, consists from 2 digits
     * @return number
     */
    public static int getInt(String s) {
        int res;
        res = 0;
        int i;
        for (i = 0; i < length; i++) {
            res *= radix;
            res += (int) (s.charAt(i) - '0');
        }
        return res;
    }

    public boolean initVars(Variables var) {
        var.putVariable(goS, go, true);
        var.putVariable(throwStoneS, throwStone, true);
        var.putVariable(waitS, wait, true);
        var.putVariable(meS, me, true);
        var.putVariable(enemyS, enemy, true);
        var.putVariable(mountainUpS, mountainUp, true);
        var.putVariable(stoneS, stone, true);
        var.putVariable(freeS, free, true);
        int i;
        for (i = 0; i < 2; i++) {
            if (!players[i].initVars(var)) {
                return false;
            }
        }
        return true;
    }

    private final double sideLen = 20;

    private double vecLen = sideLen * Math.sqrt(3.);

    private final int movex = 50, movey = 250;

    private SPoint getCenter(int i, int j) {
        double x, y;
        x = y = 0;
        SPoint v1, v2;
        v1 = new SPoint();
        v2 = new SPoint();
        double phi = Math.PI / 6;
        v1.x = v2.x = vecLen * Math.cos(phi);
        v1.y = vecLen * Math.sin(phi);
        v2.y = -v1.y;
        v1.multiply(i);
        v2.multiply(j);
        v1.add(v2);
        return v1;
    }

    private void drawCommand(String name, int x, int y, Color c, int width, int gap) {
        Graphics g = net.sourceforge.sevents.scripthandle.Sevents.getProcess().getWindow().imgGr;
        if (g == null) {
            return;
        }
        g.setColor(c);
        g.fillRect(x, y, width, width);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, width);
        y += (width - g.getFontMetrics().getStringBounds(name, g).getY()) / 2;
        x += width + gap;
        g.drawString(name, x, y);
    }

    private Polygon getPolygon(int i, int j) {
        int x[], y[], n, k;
        n = 6;
        x = new int[n];
        y = new int[n];
        SPoint o = getCenter(i, j);
        SPoint v = new SPoint();
        double phi = Math.PI / 3;
        o.x += movex;
        o.y += movey;
        for (k = 0; k < n; k++) {
            v.x = sideLen * Math.cos(phi * k);
            v.y = sideLen * Math.sin(phi * k);
            v.add(o);
            x[k] = (int) Math.floor(v.x + 0.5);
            y[k] = (int) Math.floor(v.y + 0.5);
        }
        return new Polygon(x, y, n);
    }

    private void paintGrid() {
        Graphics g = net.sourceforge.sevents.scripthandle.Sevents.getProcess().getWindow().imgGr;
        if (g == null) {
            return;
        }
        g.setColor(Color.BLACK);
        int i, j;
        for (i = 0; i < MAX; i++) {
            for (j = 0; j < MAX; j++) {
                g.drawPolygon(getPolygon(i, j));
            }
        }
    }

    private void paintColors() {
        Graphics g = net.sourceforge.sevents.scripthandle.Sevents.getProcess().getWindow().imgGr;
        if (g == null) {
            return;
        }
        int i, j;
        Color c;
        for (i = 0; i < MAX; i++) {
            for (j = 0; j < MAX; j++) {
                switch(sea[i][j]) {
                    case me:
                        c = meC;
                        break;
                    case enemy:
                        c = enemyC;
                        break;
                    case mountainUp:
                        c = mountainUpC;
                        break;
                    case mountainDown:
                        c = mountainDownC;
                        break;
                    case stone:
                        c = stoneC;
                        break;
                    case free:
                        c = freeC;
                        break;
                    default:
                        c = Color.WHITE;
                }
                g.setColor(c);
                g.fillPolygon(getPolygon(i, j));
            }
        }
    }

    private void paintShip(int i, int j, Color color) {
        Graphics g = net.sourceforge.sevents.scripthandle.Sevents.getProcess().getWindow().imgGr;
        if (g == null) {
            return;
        }
        if (i < 0 || j < 0) {
            return;
        }
        int r1, r2, r3;
        r1 = (int) ((sideLen * 3) / 4);
        r2 = (int) (sideLen / 2);
        r3 = (int) (sideLen / 4);
        SPoint o = getCenter(i, j);
        int x, y;
        x = movex + (int) Math.floor(o.x + 0.5);
        y = movey + (int) Math.floor(o.y + 0.5);
        g.setColor(Color.BLACK);
        g.fillOval(x - r1, y - r1, r1 * 2, r1 * 2);
        g.setColor(color);
        g.fillOval(x - r2, y - r2, r2 * 2, r2 * 2);
        g.setColor(Color.WHITE);
        g.fillOval(x - r3, y - r3, r3 * 2, r3 * 2);
    }

    public void paint() {
        MainHandle mh = net.sourceforge.sevents.scripthandle.Sevents.getProcess();
        if (mh == null) {
            return;
        }
        SeventsWindow sw = mh.getWindow();
        if (sw == null) {
            return;
        }
        Graphics g = sw.imgGr;
        if (g == null) {
            return;
        }
        final int width = 20;
        final int gap = 10;
        paintColors();
        paintGrid();
        paintShip(mx, my, meC);
        paintShip(ex, ey, enemyC);
        int y = (int) (movey * 1.5);
        drawCommand(players[0].teamName, movex, y, meC, width, gap);
        drawCommand(players[1].teamName, movex, y + width + gap, enemyC, width, gap);
        net.sourceforge.sevents.scripthandle.Sevents.getProcess().getWindow().repaint();
    }

    public void seekPlayer(int i) {
        countPlayers = i;
    }

    /**
     * Set name of scenario and name of command
     * 
     * @param scenario
     */
    public void initPlayer(String scenario) {
        if (countPlayers >= 2) {
            Log.err("Symplegades.initPlayer(): count players exceeded", Log.ERROR);
        }
        players[countPlayers] = new SPlayer();
        SPlayer sp = players[countPlayers++];
        sp.setId(StringLibrary.getStringFromNumber(countPlayers));
        sp.setFileName(scenario);
    }

    public int getSea(int i, int j, String teamId) {
        if (sea == null) {
            throw new NullPointerException("sea[][] == " + sea);
        }
        if (i < 1 || i > MAX || j < 1 || j > MAX) {
            Log.out("Out of bounds sea: (" + i + ", " + j + "), teamId = " + teamId, Log.FILE);
            return 0;
        }
        i--;
        j--;
        if (teamId == null || teamId.length() == 0) {
            int res = sea[i][j];
            if (res == mountainDown) {
                res = free;
            }
            return res;
        }
        int k;
        for (k = 0; k < 2; k++) {
            if (players[k].getId().equals(teamId)) {
                return players[k].getSea(i, j);
            }
        }
        return 0;
    }

    public void startThreads() {
        int ind[] = new int[2];
        int i;
        ind[0] = (int) (Math.random() * 2);
        ind[1] = (ind[0] == 0) ? 1 : 0;
        for (i = 0; i < 2; i++) {
            players[ind[i]].runThread();
        }
    }

    private void initDirs() {
        int i;
        for (i = 0; i <= cDirections; i++) {
            dirs[i] = new Point(0, 0);
        }
        dirs[1].x = 0;
        dirs[1].y = 1;
        dirs[2].x = 1;
        dirs[2].y = 0;
        dirs[3].x = 1;
        dirs[3].y = -1;
        dirs[4].x = 0;
        dirs[4].y = -1;
        dirs[5].x = -1;
        dirs[5].y = 0;
        dirs[6].x = -1;
        dirs[6].y = 1;
    }

    public void initSea() {
        int i, j, maxCount, count;
        maxCount = (MAX * MAX) / 2;
        initDirs();
        for (count = 0; count < maxCount; ) {
            i = (int) (Math.random() * MAX);
            j = (int) (Math.random() * MAX);
            if (isFree(i, j)) {
                sea[i][j] = mountainDown;
                count++;
            }
        }
        for (i = 0; i < MAX; i++) {
            for (j = 0; j < MAX; j++) {
                if (sea[i][j] != mountainDown) {
                    sea[i][j] = free;
                }
            }
        }
        sea[MAX / 2][0] = me;
        sea[0][MAX / 2] = enemy;
        mx = MAX / 2;
        my = 0;
        ex = 0;
        ey = MAX / 2;
    }

    public boolean initEvents(HashMap<String, MyEvent> eventNames, Vector<MyEvent> allEvents) {
        countPlayers = 0;
        int i, j;
        MyEvent se;
        String eName;
        int n = allEvents.size();
        for (i = 0; i < MAX; i++) {
            for (j = 0; j < MAX; j++) {
                if (sea[i][j] != mountainDown) {
                    continue;
                }
                se = new MyEvent();
                eName = "_mountainUp_" + getString(i + 1) + "_" + getString(j + 1);
                se.setName(eName);
                try {
                    String s = "sea(" + (i + 1) + ", " + (j + 1) + ")";
                    se.setCondition(getExpression(s + " == " + freeS + " && a_mx > 0 && a_ex > 0"));
                } catch (Exception e) {
                    Log.war("Cond add: " + e.getMessage(), Log.WARNING);
                }
                se.setProbability(mountainUpP);
                se.setPriority(mountainPriority);
                se.setRenewable(true);
                eventNames.put(eName, se);
                allEvents.add(n++, se);
                se = new MyEvent();
                eName = "_mountainDown_" + getString(i + 1) + "_" + getString(j + 1);
                se.setName(eName);
                try {
                    String s = "sea(" + (i + 1) + ", " + (j + 1) + ")";
                    se.setCondition(getExpression(s + " == " + mountainUpS));
                } catch (Exception e) {
                    Log.war("Cond add: " + e.getLocalizedMessage(), Log.WARNING);
                }
                se.setProbability(mountainDownP);
                se.setPriority(mountainPriority);
                se.setRenewable(true);
                eventNames.put(eName, se);
                allEvents.add(n++, se);
            }
        }
        for (i = 0; i < 2; i++) {
            if (!players[i].initEvents()) {
                return false;
            }
            players[i].initWin(eventNames, allEvents);
        }
        return true;
    }
}
