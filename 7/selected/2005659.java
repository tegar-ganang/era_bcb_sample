package ArianneViewer;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import com.borland.dx.sql.dataset.*;
import javolution.text.*;
import javolution.util.*;

/**
 * Shapes e' la classe astratta di base per tutti gli oggetti
 * disegnabili in un DrawPanel. Ogni oggetto che estende questa classe
 * specializza il proprio metodo draw, che implementa le regole di
 * visualizzazione. Ogni oggetto grafico incapsula diverse informazioni
 * di stato necessarie per la visualizzazione. Taqli informazioni di
 * stato includono:
 * <ul>
 * <li>Le coordinate del poligono entro cui e' inscritta la shape
 * <li>Il colore del bordo della shape
 * <li>Il colore di riempimento della shape
 * <li>Il font (nel caso di oggetto testuale o tabellare)
 * <li>La query che regola la visualizzazione dell'oggetto
 * (vedi <a href="#getSqlQuery()">getSqlQuery()</a>)
 *
 * </ul>
 * <p>
 *
 * @author      Andrea Annibali
 * @version     1.0
 */
public abstract class ViewerShapes extends ArianneUtil.Shapes implements Dynamic, Cloneable, MouseListener {

    static final Text TRUE = Text.intern("true");

    static final Text FALSE = Text.intern("false");

    static final Text BLANK = Text.intern(" ");

    static final Text COMA = Text.intern(",");

    static final Text DOT = Text.intern(".");

    static final Text ZERO = Text.intern(".");

    static final Text AT = Text.intern("@");

    static final Text OR = Text.intern("OR");

    static final Text AND = Text.intern("AND");

    static final Text ID_SEP = Text.intern("$");

    static final int epsilon = 4;

    static final int ra = 5;

    static final String dbConString = "jdbc:borland:dsremote://localhost/";

    private Hashtable dbConnections, globalVariablesHashTable = new Hashtable(), variablesHashTable = new Hashtable(), colorVariablesHashTable = new Hashtable();

    private FastMap substExecuted = new FastMap();

    private FastMap exprDep = new FastMap();

    private int cmdId = 0;

    private boolean visible = true, squareDrawActive = false, queriesActive = true, isSelected = false, blinking = false, enableThreadCreation = false, changes = true, lastChangeDrawn = false;

    private String associatedScript = "";

    private Text assignExpression = Text.EMPTY, fastAssignExpression = Text.EMPTY, visualizationExpression = Text.EMPTY;

    private Text[] changeColorExpression = { Text.EMPTY, Text.EMPTY, Text.EMPTY }, changeBorderColorExpression = { Text.EMPTY, Text.EMPTY, Text.EMPTY }, changeFillColorExpression = { Text.EMPTY, Text.EMPTY, Text.EMPTY }, changeFontColorExpression = { Text.EMPTY, Text.EMPTY, Text.EMPTY }, changeValueExpression = { Text.EMPTY, Text.EMPTY, Text.EMPTY }, absdiffExpression = null, fastCheckFontColorExpression = null, fastCheckColorExpression = null;

    private boolean preserveCommands = false, checked = true;

    private Executor executor;

    private volatile Thread scriptExecutorThread = null;

    private javax.swing.Timer blinkTimer = null;

    private JPopupMenu popup = new JPopupMenu();

    private JMenuItem menuItemSysCall;

    private Color shapeColor = null;

    private Color shapeBorderColorWhenNull = null;

    private Color shapeFillColorWhenNull = null;

    DecimalFormat stringFormatter = new DecimalFormat("##.#");

    private Text oldResolvedFillColorExpr = Text.EMPTY;

    private Text oldResolvedBorderColorExpr = Text.EMPTY;

    private Text oldResolvedColorExpr = Text.EMPTY;

    private Text oldResolvedFontColorExpr = Text.EMPTY;

    private Text oldResolvedChangeValueExpr = Text.EMPTY;

    private Text oldResolvedVisExpr = Text.EMPTY;

    private Text oldResolvedAssignExpr = Text.EMPTY;

    private Text oldResolvedFastAssignExpr = Text.EMPTY;

    private boolean oldVisExprRes = true;

    private String oldAssignExprRes = "";

    private String oldFastAssignExprRes = "";

    private String oldChangeExprRes = "";

    private Vector commandBuffer = new Vector();

    ViewerShapes(String elType, int nV, int id, ArianneViewer.DrawingPanel p, String imgN, int ovl, java.awt.Color sbc, java.awt.Point ePoint, java.awt.Point sPoint, boolean poll, int pollMsec, int bckMsec) {
        super(elType, nV, id, (ArianneUtil.DrawingPanel) p, imgN, ovl, sbc, ePoint, sPoint, poll, pollMsec, bckMsec);
        initMenuSysCall((new Shape_menuItem_actionAdapter(this)));
        collectInternalCommand();
        setExecutor(new Executor(this));
        createExecutorThread(refreshPeriod);
        if (getScriptExecutorThread() != null) getScriptExecutorThread().start();
        initTimer();
    }

    ViewerShapes(String elType, int nV, int id, ArianneViewer.DrawingPanel p, String imgN, int ovl, Color sbc, double xPnt[], double yPnt[], boolean poll, int pollMsec, int bckMsec) {
        super(elType, nV, id, p, imgN, ovl, sbc, xPnt, yPnt, poll, pollMsec, bckMsec);
        initMenuSysCall((new Shape_menuItem_actionAdapter(this)));
        collectInternalCommand();
        setExecutor(new Executor(this));
        createExecutorThread(refreshPeriod);
        if (getScriptExecutorThread() != null) getScriptExecutorThread().start();
        initTimer();
    }

    public void initTimer() {
        blinkTimer = new javax.swing.Timer(1000, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if (isBlinking()) {
                    setBlinking(false);
                } else {
                    setBlinking(true);
                }
                draw((Graphics2D) getFatherPanel().getGraphics(), getFatherPanel(), true);
            }
        });
    }

    public JPopupMenu getPopUp() {
        return popup;
    }

    /**
   * Fa iniziare a lampeggiare la Shape
   */
    public void startBlinking() {
        if (!isBlinkTimerRunning()) blinkTimer.start();
    }

    /**
   * Imposta la Shape come non lampeggiante
   */
    public void stopBlinking() {
        if (isBlinkTimerRunning()) blinkTimer.stop();
    }

    public boolean isBlinkTimerRunning() {
        return blinkTimer.isRunning();
    }

    public void setChanges(boolean b) {
        changes = b;
    }

    public boolean getChanges() {
        return changes;
    }

    public void setLastChangeDrawn(boolean b) {
        lastChangeDrawn = b;
    }

    public boolean isLastChangeDrawn() {
        return lastChangeDrawn;
    }

    public void terminateScriptExecutorThread() {
        if (scriptExecutorThread != null) {
            Thread moribund = scriptExecutorThread;
            scriptExecutorThread = null;
            moribund.interrupt();
            moribund = null;
        }
    }

    public void setCommandPreserve(boolean allowed) {
        preserveCommands = allowed;
    }

    /**
   * Imposta il periodo di sleep del thread
   * @param msec il periodo di sleep del thread
   */
    public void setThreadRefreshPeriod(int msec) {
        if (scriptExecutorThread != null) {
            Thread moribund = scriptExecutorThread;
            scriptExecutorThread = null;
            moribund.interrupt();
        }
        createExecutorThread(msec);
        scriptExecutorThread.start();
    }

    /**
   * Crea il thread di esecuzione dello script associato all'oggetto
   * @param msec il periodo di sleep del thread
   */
    public void createExecutorThread(final int msec) {
        if (enableThreadCreation) scriptExecutorThread = new Thread() {

            public void run() {
                Thread thisThread = Thread.currentThread();
                while (scriptExecutorThread == thisThread) {
                    try {
                        Thread.sleep(msec);
                    } catch (InterruptedException e) {
                    }
                    if (getExecutor() != null && !getAssociatedScript().equals("")) getExecutor().execStatement(Text.valueOf(getAssociatedScript()));
                }
            }
        };
    }

    /**
   * Restituisce true se la Shape sta lampeggiando
   * @return true se la Shape sta lampeggiando
   */
    public boolean isBlinking() {
        return blinking;
    }

    /**
   * Imposta il flag di lampeggio
   * @param b il valore da assegnare al flag
   */
    public void setBlinking(boolean b) {
        blinking = b;
        if (blinking) setChanges(true);
    }

    public void setSqlQuery(String q) {
        super.setSqlQuery(q);
    }

    /**
   * Esegue esplicitamente (per chiamata diretta) il repaint della shape
   */
    public void refresh(boolean toDraw) {
        draw((Graphics2D) getFatherPanel().getGraphics(), getFatherPanel(), toDraw);
    }

    /**
   * Restituisce il puntatore al thread di esecuzione dello script associato alla
   * shape
   * @return l'oggetto Thread in questione
   */
    public Thread getScriptExecutorThread() {
        return scriptExecutorThread;
    }

    /**
   * Restituisce l'oggetto executor associato alla shape, che ha la funzione di
   * eseguire i comandi nel linguaggio di Arianne
   * @return l'oggetto Executor
   */
    public Executor getExecutor() {
        return executor;
    }

    /**
   * Imposta l'executor della shape
   * @param exct l'oggetto Executor da associare alla shape
   */
    public void setExecutor(Executor exct) {
        executor = exct;
    }

    /**
   * Restituisce lo script associato all'oggetto
   * @return l'oggetto String contenente lo script
   */
    public String getAssociatedScript() {
        return associatedScript.replaceAll("\r", "").replaceAll("\t", "").replaceAll("\n", "");
    }

    /**
   * Restituisce il puntatore allo shape corrente
   * @param shapeId l'identificativo dello shape
   * @return l'oggetto Shapes corrente
   */
    public ViewerShapes getInfo(int shapeId) {
        Integer id = new Integer(shapeId);
        return (ViewerShapes) ((ArianneViewer.DrawingPanel) getFatherPanel()).getShapeMap().get(id);
    }

    /**
   * Restituisce il colore del bordo quando il valore (value) e' null
   * @return l'oggetto Color da assegnare al bordo quando value risulta null
   */
    public Color getShapeBorderColorWhenNull() {
        return shapeBorderColorWhenNull;
    }

    /**
   * Imposta il colore da assegnare al bordo quando value risulta null
   * @param c l'oggetto Color da assegnare al bordo quando value risulta null
   */
    public void setShapeBorderColorWhenNull(Color c) {
        shapeBorderColorWhenNull = c;
    }

    /**
   * Restituisce il colore di riempimento quando il valore (value) e' null
   * @return l'oggetto Color di riempimento da assegnare quando value risulta null
   */
    public Color getShapeFillColorWhenNull() {
        return shapeFillColorWhenNull;
    }

    /**
   * Imposta il colore di riempimento da assegnare quando value risulta null
   * @param c l'oggetto Color di riempimento da assegnare quando value risulta null
   */
    public void setShapeFillColorWhenNull(Color c) {
        shapeFillColorWhenNull = c;
    }

    abstract void shape_menuItem_actionPerformed(ActionEvent e);

    public abstract boolean isInSelectArea(Point p);

    public abstract boolean isInsideArea(Point p);

    public abstract void enqueueTabularCommands(Hashtable commandHashTable, Hashtable commandsOrder);

    public abstract void enqueueButtonCommands(Hashtable commandHashTable, Hashtable commandsOrder);

    public abstract void enqueueNShapesCommands(Hashtable commandHashTable, Hashtable commandsOrder);

    public abstract void enqueueSShapesCommands(Hashtable commandHashTable, Hashtable commandsOrder);

    public abstract double getCurVal();

    /**
   * Visualizza l'ora attuale
   * @param comment String
   */
    public void dumpTime(String comment) {
        GregorianCalendar calendar = new GregorianCalendar();
        java.util.Date trialTime = new java.util.Date();
        calendar.setTime(trialTime);
        String timeOutput = comment + " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "." + calendar.get(Calendar.MILLISECOND);
        System.out.println(timeOutput);
    }

    /**
   * Restituisce l'ora attuale in millisecondi
   * @param comment String
   */
    public int getTimeInMsec() {
        GregorianCalendar calendar = new GregorianCalendar();
        java.util.Date trialTime = new java.util.Date();
        calendar.setTime(trialTime);
        int res = calendar.get(Calendar.MILLISECOND) + calendar.get(Calendar.SECOND) * 1000 + calendar.get(Calendar.MINUTE) * 60000 + calendar.get(Calendar.HOUR_OF_DAY) * 3600000;
        return res;
    }

    /**
   * Restituisce l'espressione che regola la visualizzazione dell'oggetto
   * @return String
   */
    public Text getVisualizationExpression() {
        return visualizationExpression;
    }

    /**
   * Imposta l'espressione che regola la visualizzazione dell'oggetto
   * @param v la stringa di visualizzazione da impostare
   */
    public void setVisualizationExpression(Text v) {
        visualizationExpression = v;
    }

    /**
   * Restituisce l'espressione che regola l'assegnazione nell'oggetto
   * @return l'espressione che regola l'assegnazione
   */
    public Text getAssignExpression() {
        return assignExpression;
    }

    /**
   * Restituisce l'espressione che regola l'assegnazione nell'oggetto
   * @return l'espressione che regola l'assegnazione
   */
    public Text[] getAbsDiffExpression() {
        return absdiffExpression;
    }

    /**
   * Restituisce l'espressione che regola l'assegnazione nell'oggetto
   * @return l'espressione che regola l'assegnazione
   */
    public void setAbsDiffExpression(Text[] s) {
        absdiffExpression = s;
    }

    /**
   * Restituisce l'espressione che regola l'assegnazione nell'oggetto
   * @return l'espressione che regola l'assegnazione
   */
    public Text getFastAssignExpression() {
        return fastAssignExpression;
    }

    /**
   * Imposta l'espressione che regola l'assegnazione nell'oggetto
   * @param v la stringa che contiene l'espressione che regola l'assegnazione
   */
    public void setAssignExpression(Text v) {
        assignExpression = v;
    }

    /**
   * Imposta l'espressione che regola l'assegnazione nell'oggetto
   * @param v la stringa che contiene l'espressione che regola l'assegnazione
   */
    public void setFastAssignExpression(Text v) {
        fastAssignExpression = v;
    }

    /**
   * Restituisce l'array di stringhe che gestisce la regola di colorazione del bordo
   * dell'oggetto
   * @return l'array di stringhe contenente la regola di visualizzazione
   */
    public Text[] getBorderColorExpression() {
        return changeBorderColorExpression;
    }

    /**
   * Restituisce l'array di stringhe che gestisce la regola di colorazione del font
   * dell'oggetto
   * @return l'array di stringhe contenente la regola di visualizzazione
   */
    public Text[] getFontColorExpression() {
        return changeFontColorExpression;
    }

    /**
   * Imposta l'array di stringhe che gestisce la regola di colorazione del bordo
   * dell'oggetto
   * @param s l'array di stringhe contenente la regola di visualizzazione
   */
    public void setBorderColorExpression(Text[] s) {
        changeBorderColorExpression = s;
    }

    /**
   * Imposta l'array di stringhe che gestisce la regola di colorazione del font
   * dell'oggetto
   * @param s l'array di stringhe contenente la regola di visualizzazione
   */
    public void setFontColorExpression(Text[] s) {
        changeFontColorExpression = s;
    }

    /**
   * Imposta l'array di stringhe che gestisce la regola di colorazione del bordo
   * dell'oggetto
   * @param s l'array di stringhe contenente la regola di visualizzazione
   */
    public void setBorderColorExpression(int idx, Text s) {
        changeBorderColorExpression[idx] = s;
    }

    /**
   * Imposta l'array di stringhe che gestisce la regola di colorazione del bordo
   * dell'oggetto
   * @param s l'array di stringhe contenente la regola di visualizzazione
   */
    public void setFontColorExpression(int idx, Text s) {
        changeFontColorExpression[idx] = s;
    }

    /**
   * Imposta l'array di stringhe che gestisce la regola di colorazione del bordo
   * dell'oggetto
   * @param s l'array di stringhe contenente la regola di visualizzazione
   */
    public void setFillColorExpression(int idx, Text s) {
        changeFillColorExpression[idx] = s;
    }

    /**
   *
   * @param idx int
   * @param s Text
   */
    public void setFastCheckColorExpression(Text[] s) {
        fastCheckColorExpression = s;
    }

    public void setFastCheckFontColorExpression(Text[] s) {
        fastCheckFontColorExpression = s;
    }

    /**
   *
   * @param idx int
   */
    public Text getFastCheckColorExpression(int idx) {
        return fastCheckColorExpression[idx];
    }

    /**
   *
   * @param idx int
   */
    public Text getFastCheckFontColorExpression(int idx) {
        return fastCheckFontColorExpression[idx];
    }

    /**
   *
   * @param idx int
   */
    public Text[] getFastCheckColorExpression() {
        return fastCheckColorExpression;
    }

    /**
   *
   * @param idx int
   */
    public Text[] getFastCheckFontColorExpression() {
        return fastCheckFontColorExpression;
    }

    /**
   *
   * @param idx int
   * @param s Text
   */
    public void setFastCheckColorExpression(int idx, Text s) {
        fastCheckColorExpression[idx] = s;
    }

    /**
   *
   * @param idx int
   * @param s Text
   */
    public void setFastCheckFontColorExpression(int idx, Text s) {
        fastCheckFontColorExpression[idx] = s;
    }

    /**
   * Imposta l'array di stringhe che gestisce la regola di colorazione del bordo
   * dell'oggetto
   * @param s l'array di stringhe contenente la regola di visualizzazione
   */
    public void setColorExpression(int idx, Text s) {
        changeColorExpression[idx] = s;
    }

    /**
   * Restituisce l'array di stringhe che gestisce la regola di colorazione del riempimento
   * dell'oggetto
   * @return l'array di stringhe contenente la regola di visualizzazione
   */
    public Text[] getFillColorExpression() {
        return changeFillColorExpression;
    }

    /**
   * Restituisce l'array di stringhe che gestisce la regola di colorazione del riempimento
   * dell'oggetto
   * @return l'array di stringhe contenente la regola di visualizzazione
   */
    public Text[] getColorExpression() {
        return changeColorExpression;
    }

    /**
   * Restituisce l'array di stringhe che gestisce la regola di cambiamento del valore
   * dell'oggetto
   * @return l'array di stringhe contenente la regola di visualizzazione
   */
    public Text[] getChangeValueExpression() {
        return changeValueExpression;
    }

    /**
   * Imposta l'array di stringhe che gestisce la regola di colorazione del bordo
   * dell'oggetto
   * @param s l'array di stringhe contenente la regola di visualizzazione
   */
    public void setChangeValueExpression(int idx, Text s) {
        changeValueExpression[idx] = s;
    }

    /**
   * Imposta l'array di stringhe che gestisce la regola di colorazione del riempimento
   * dell'oggetto
   * @param s l'array di stringhe contenente la regola di visualizzazione
   */
    public void setFillColorExpression(Text[] s) {
        changeFillColorExpression = s;
    }

    /**
   * Imposta l'array di stringhe che gestisce la regola di cambiamento del valore
   * dell'oggetto
   * @param s l'array di stringhe contenente la regola di visualizzazione
   */
    public void setChangeValueExpression(Text[] s) {
        changeValueExpression = s;
    }

    /**
 * Imposta la label associata al bottone
 * @param bLabel la label da assocuare al bottone
 */
    public void addCommand(String command) {
        this.commandBuffer.addElement(command.trim());
    }

    public Vector getCommandBuffer() {
        return commandBuffer;
    }

    public void collectInternalCommand() {
        Database localDb = this.getFatherPanel().getLocalDb();
        try {
            String query = "SELECT ID_SH2, IMAGE_NAME_SH2, ID_SH1, IMAGE_NAME_SH1, LINE_NUMBER, LINK_ACTION " + "FROM LINK_SHAPES " + "WHERE ID_SH1=" + getElemId() + " AND IMAGE_NAME_SH1='" + getImgName() + "' AND " + "IMAGE_NAME_SH1=IMAGE_NAME_SH2 ORDER BY LINE_NUMBER";
            Statement sp = localDb.createStatement();
            ResultSet rp = sp.executeQuery(query);
            Text command = Text.EMPTY;
            while (rp.next()) {
                command = Text.valueOf(rp.getString("LINK_ACTION"));
                Text token[] = ParserUtils.split(command, " ");
                if (token[0].toUpperCase().contentEquals(Text.valueOf("ASSIGNVALUETO"))) {
                    variablesHashTable.put(token[1], new Integer(rp.getInt("ID_SH2")));
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("ASSIGNVALUE"))) {
                    this.setAssignExpression(token[1]);
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("FASTASSIGN"))) {
                    this.setFastAssignExpression(token[1]);
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("ABSDIFF"))) {
                    Text[] expr = new Text[token.length - 1];
                    System.arraycopy(token, 1, expr, 0, expr.length);
                    setAbsDiffExpression(expr);
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("ASSIGNCOLORTO"))) {
                    colorVariablesHashTable.put(token[1], new Integer(rp.getInt("ID_SH2")));
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("CHANGEFONTCOLORIF"))) {
                    for (int i = 1; i < token.length - 2; i++) {
                        setFontColorExpression(0, getFontColorExpression()[0].concat(BLANK).concat(token[i]));
                    }
                    setFontColorExpression(1, getFontColorExpression()[1].concat(BLANK).concat(token[token.length - 2]));
                    setFontColorExpression(2, getFontColorExpression()[2].concat(BLANK).concat(token[token.length - 1]));
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("CHANGECOLORSIF"))) {
                    for (int i = 1; i < token.length - 2; i++) {
                        setColorExpression(0, getColorExpression()[0].concat(BLANK).concat(token[i]));
                    }
                    setColorExpression(1, getColorExpression()[1].concat(BLANK).concat(token[token.length - 2]));
                    setColorExpression(2, getColorExpression()[2].concat(BLANK).concat(token[token.length - 1]));
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("ORCHECKCOLORS"))) {
                    Text[] colExpr = new Text[token.length];
                    colExpr[0] = OR;
                    int j = 1;
                    for (int i = 0; i < token.length - 1; i++) {
                        if (!token[i + 1].contentEquals(Text.EMPTY)) {
                            colExpr[j] = token[i + 1];
                            j++;
                        }
                    }
                    Text[] res = new Text[j];
                    setFastCheckColorExpression(res);
                    System.arraycopy(colExpr, 0, res, 0, res.length);
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("ANDCHECKCOLORS"))) {
                    Text[] colExpr = new Text[token.length];
                    colExpr[0] = AND;
                    int j = 1;
                    for (int i = 0; i < token.length - 1; i++) {
                        if (!token[i + 1].contentEquals(Text.EMPTY)) {
                            colExpr[j] = token[i + 1];
                            j++;
                        }
                    }
                    Text[] res = new Text[j];
                    setFastCheckColorExpression(res);
                    System.arraycopy(colExpr, 0, res, 0, res.length);
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("ORCHECKFONTCOLORS"))) {
                    Text[] colExpr = new Text[token.length];
                    colExpr[0] = OR;
                    int j = 1;
                    for (int i = 0; i < token.length - 1; i++) {
                        if (!token[i + 1].contentEquals(Text.EMPTY)) {
                            colExpr[j] = token[i + 1];
                            j++;
                        }
                    }
                    Text[] res = new Text[j];
                    setFastCheckFontColorExpression(res);
                    System.arraycopy(colExpr, 0, res, 0, res.length);
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("ANDCHECKFONTCOLORS"))) {
                    Text[] colExpr = new Text[token.length];
                    colExpr[0] = AND;
                    int j = 1;
                    for (int i = 0; i < token.length - 1; i++) {
                        if (!token[i + 1].contentEquals(Text.EMPTY)) {
                            colExpr[j] = token[i + 1];
                            j++;
                        }
                    }
                    Text[] res = new Text[j];
                    setFastCheckFontColorExpression(res);
                    System.arraycopy(colExpr, 0, res, 0, res.length);
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("VISIBLEIF"))) {
                    Text visExpr = Text.EMPTY;
                    for (int i = 1; i < token.length; i++) {
                        visExpr = visExpr.concat(token[i]);
                    }
                    setVisualizationExpression(visExpr);
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("CHANGEBORDERCOLORIF"))) {
                    for (int i = 1; i < token.length - 2; i++) {
                        setBorderColorExpression(0, getBorderColorExpression()[0].concat(BLANK).concat(token[i]));
                    }
                    setBorderColorExpression(1, getBorderColorExpression()[1].concat(BLANK).concat(token[token.length - 2]));
                    setBorderColorExpression(2, getBorderColorExpression()[2].concat(BLANK).concat(token[token.length - 1]));
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("CHANGEFILLCOLORIF"))) {
                    for (int i = 1; i < token.length - 2; i++) {
                        setFillColorExpression(0, getFillColorExpression()[0].concat(BLANK).concat(token[i]));
                    }
                    setFillColorExpression(1, getFillColorExpression()[1].concat(BLANK).concat(token[token.length - 2]));
                    setFillColorExpression(2, getFillColorExpression()[2].concat(BLANK).concat(token[token.length - 1]));
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("CHANGEVALUEIF"))) {
                    for (int i = 1; i < token.length - 2; i++) {
                        setChangeValueExpression(0, getFillColorExpression()[0].concat(BLANK).concat(token[i]));
                    }
                    setChangeValueExpression(1, getFillColorExpression()[1].concat(BLANK).concat(token[token.length - 2]));
                    setChangeValueExpression(2, getFillColorExpression()[2].concat(BLANK).concat(token[token.length - 1]));
                } else if (token[0].toUpperCase().contentEquals(Text.valueOf("EXECUTE"))) {
                    enableThreadCreation = true;
                    String path = command.toString().substring(token[0].length() + 1, command.length());
                    if (path.startsWith("%CURDIR/")) {
                        path = getFatherPanel().getCurDir() + "/" + path.replaceAll("%CURDIR/", "");
                    } else if (path.startsWith("%CURDIR\\")) {
                        path = getFatherPanel().getCurDir() + path.replaceAll("%CURDIR", "");
                    }
                    path = path.replaceAll("//", "\\");
                    byte buff[] = new byte[2048];
                    try {
                        InputStream fileIn = new FileInputStream(path);
                        int i = fileIn.read(buff);
                        associatedScript = new String(buff);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
   * Questo metodo sostituisce nell'espressione data in input tutte le variabili
   * con i valori reali
   * @param expression l'espressione da risolvere
   */
    public String resolve(String expression) {
        for (Enumeration e = variablesHashTable.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            int id = ((Integer) variablesHashTable.get(key)).intValue();
            ViewerShapes connectedShape = getInfo(id);
            if (connectedShape instanceof TextShape) {
                TextShape ts = (TextShape) connectedShape;
                if (ts.getTextVal().matches("[a-z]+[0-9]*")) {
                    expression = execSubst(expression, "" + key, ts.getTextVal());
                    if (expression.equals("")) expression = ts.getTextVal();
                } else {
                    double val = (connectedShape == null ? 0 : connectedShape.getCurVal());
                    expression = execSubst(expression, "" + key, "" + val);
                }
            } else {
                double val = (connectedShape == null ? 0 : connectedShape.getCurVal());
                expression = execSubst(expression, "" + key, "" + val);
            }
        }
        for (Enumeration e = colorVariablesHashTable.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            int id = ((Integer) colorVariablesHashTable.get(key)).intValue();
            ViewerShapes obtainedShape = getInfo(id);
            double val = ((obtainedShape instanceof FillableShape) ? (obtainedShape instanceof TextShape ? ((TextShape) getInfo(id)).getRGBTextColor() : ((FillableShape) getInfo(id)).getShapeFillColorRGB()) : getInfo(id).getShapeBorderColorRGB());
            expression = execSubst(expression, "" + key, "" + val);
        }
        return expression;
    }

    /**
   * Questo metodo sostituisce nell'espressione data in input tutte le variabili
   * con i valori reali
   * @param expression l'espressione da risolvere
   */
    public Text resolve(Text expression) {
        if (expression.contentEquals(Text.EMPTY)) return expression;
        Hashtable depVarsFnd = (Hashtable) exprDep.get(expression);
        if (depVarsFnd != null) {
            for (Enumeration e = depVarsFnd.keys(); e.hasMoreElements(); ) {
                Object key = e.nextElement();
                ViewerShapes connectedShape = (ViewerShapes) key;
                Text keyVal = (Text) depVarsFnd.get(key);
                if (connectedShape instanceof TextShape) {
                    TextShape ts = (TextShape) connectedShape;
                    Text tsTextVal = Text.valueOf(ts.getTextVal());
                    if (ts.getTextVal().matches("[a-z]+[0-9]*")) {
                        expression = execSubst(expression, keyVal, tsTextVal);
                        if (expression.contentEquals(Text.EMPTY)) expression = tsTextVal;
                    } else {
                        expression = execSubst(expression, keyVal, (connectedShape == null ? ZERO : ts.getInTxtCurVal()));
                    }
                } else {
                    expression = execSubst(expression, keyVal, Text.valueOf((connectedShape == null ? 0 : connectedShape.getCurVal())));
                }
            }
        } else {
            Hashtable depVars = new Hashtable();
            Text orgExpr = expression;
            for (Enumeration e = variablesHashTable.keys(); e.hasMoreElements(); ) {
                Object key = e.nextElement();
                Text keyVal = Text.valueOf(key);
                if (expression.indexOf(keyVal) != -1) {
                    int id = ((Integer) variablesHashTable.get(key)).intValue();
                    ViewerShapes connectedShape = getInfo(id);
                    if (connectedShape instanceof TextShape) {
                        TextShape ts = (TextShape) connectedShape;
                        Text tsTextVal = Text.valueOf(ts.getTextVal());
                        if (tsTextVal.toString().matches("[a-z]+[0-9]*")) {
                            depVars.put(connectedShape, keyVal);
                            expression = execSubst(expression, keyVal, tsTextVal);
                            if (expression.contentEquals(Text.EMPTY)) expression = tsTextVal;
                        } else {
                            depVars.put(connectedShape, keyVal);
                            expression = execSubst(expression, keyVal, (connectedShape == null ? ZERO : ts.getInTxtCurVal()));
                        }
                    } else {
                        depVars.put(connectedShape, keyVal);
                        expression = execSubst(expression, keyVal, Text.valueOf((connectedShape == null ? 0 : connectedShape.getCurVal())));
                    }
                }
            }
            exprDep.put(orgExpr, depVars);
        }
        for (Enumeration e = colorVariablesHashTable.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            int id = ((Integer) colorVariablesHashTable.get(key)).intValue();
            ViewerShapes obtainedShape = getInfo(id);
            double val = ((obtainedShape instanceof FillableShape) ? (obtainedShape instanceof TextShape ? ((TextShape) getInfo(id)).getRGBTextColor() : ((FillableShape) getInfo(id)).getShapeFillColorRGB()) : getInfo(id).getShapeBorderColorRGB());
            expression = execSubst(expression, Text.valueOf(key), Text.valueOf(val));
        }
        return expression;
    }

    public String execSubst(String expr, String key, String val) {
        return execSubstWithDels(expr, key, val, "[<>=|&+-/!\\s]");
    }

    /**
   * Esegue la sostituzione di una variabile con il suo valore reale
   * @param expr l'espressione in cui effettuare la sostituzione
   * @param key la variabile da sostituire
   * @param val il valore reale da sostituire alla variabile
   */
    public String execSubstWithDels(String expr, String key, String val, String dels) {
        String res = "";
        String[] tokens = expr.split(dels);
        String[] operators = new String[tokens.length - 1];
        String remainder = expr;
        for (int i = 0; i < tokens.length - 1; i++) {
            remainder = remainder.substring(tokens[i].length(), remainder.length());
            operators[i] = remainder.substring(0, remainder.indexOf(tokens[i + 1]));
            remainder = remainder.substring(remainder.indexOf(tokens[i + 1]), remainder.length());
        }
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].indexOf(key) != -1) tokens[i] = tokens[i].replaceAll(key, val);
        }
        res = tokens[0];
        int j = 0;
        for (int i = 0; i < operators.length; i++) {
            res += operators[i];
            if (++j < tokens.length) res += tokens[j];
        }
        return res;
    }

    /**
   * Esegue la sostituzione di una variabile con il suo valore reale
   * @param expr l'espressione in cui effettuare la sostituzione
   * @param key la variabile da sostituire
   * @param val il valore reale da sostituire alla variabile
   */
    public Text execSubst(Text expr, Text key, Text val) {
        Text oldRes = (Text) substExecuted.get(expr.concat(AT).concat(key).concat(AT).concat(val));
        if (oldRes != null) return oldRes;
        Text res = Text.EMPTY;
        Text[] tokens = ParserUtils.split(expr, "[<>=|&+-/!\\s]");
        Text[] operators = new Text[tokens.length - 1];
        Text remainder = expr;
        for (int i = 0; i < tokens.length - 1; i++) {
            remainder = remainder.subtext(tokens[i].length(), remainder.length());
            operators[i] = remainder.subtext(0, remainder.indexOf(tokens[i + 1]));
            remainder = remainder.subtext(remainder.indexOf(tokens[i + 1]), remainder.length());
        }
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].indexOf(key) != -1) tokens[i] = tokens[i].replace(key, val);
        }
        res = tokens[0];
        int j = 0;
        for (int i = 0; i < operators.length; i++) {
            res = res.concat(operators[i]);
            if (++j < tokens.length) res = res.concat(tokens[j]);
        }
        if (substExecuted.size() < 200) substExecuted.put(expr.concat(AT).concat(key).concat(AT).concat(val), res);
        return res;
    }

    /**
   * Restituisce il risultato della regola di colorazione del bordo
   * @return il colore risultante
   */
    public Color resultOfBorderColouringRule() {
        Text resolvedBorderColorExpr = resolve(getBorderColorExpression()[0]);
        if (!resolvedBorderColorExpr.contentEquals(Text.EMPTY) && !resolvedBorderColorExpr.contentEquals(oldResolvedBorderColorExpr)) {
            oldResolvedBorderColorExpr = resolvedBorderColorExpr;
            setChanges(true);
            if (Evaluator.evaluateExpression(resolvedBorderColorExpr, this).contentEquals(TRUE)) {
                if (!getBorderColorExpression()[1].contentEquals(Text.EMPTY)) {
                    return new Color(TypeFormat.parseInt(getBorderColorExpression()[1].trim()));
                }
            } else {
                if (!getBorderColorExpression()[2].contentEquals(Text.EMPTY)) {
                    return new Color(TypeFormat.parseInt(getBorderColorExpression()[2].trim()));
                }
            }
        }
        return getShapeBorderColor();
    }

    /**
   * Restituisce il risultato della regola di colorazione
   * @return il colore risultante
   */
    public Color resultOfFastCheckColouringRule() {
        if (getFastCheckColorExpression() == null || getFastCheckColorExpression()[0] == null || getFastCheckColorExpression()[0].contentEquals(Text.EMPTY)) return getShapeBorderColor();
        boolean res = false;
        if (getFastCheckColorExpression()[0].contentEquals(OR)) {
            for (int i = 1; i < getFastCheckColorExpression().length - 2; i += 2) {
                ViewerShapes connectedShape = getInfo(TypeFormat.parseInt(getFastCheckColorExpression()[i]));
                int c1 = (connectedShape instanceof TextShape) ? ((TextShape) connectedShape).getTextColor().getRGB() : connectedShape.getShapeBorderColor().getRGB();
                if (c1 == TypeFormat.parseInt(getFastCheckColorExpression()[i + 1])) res = true;
            }
        } else if (getFastCheckColorExpression()[0].contentEquals(AND)) {
            res = true;
            for (int i = 1; i < getFastCheckColorExpression().length - 2; i += 2) {
                ViewerShapes connectedShape = getInfo(TypeFormat.parseInt(getFastCheckColorExpression()[i]));
                int c1 = (connectedShape instanceof TextShape) ? ((TextShape) connectedShape).getTextColor().getRGB() : connectedShape.getShapeBorderColor().getRGB();
                if (c1 != TypeFormat.parseInt(getFastCheckColorExpression()[i + 1])) res = false;
            }
        }
        Color newColor = null;
        if (res) {
            newColor = new Color(TypeFormat.parseInt(getFastCheckColorExpression()[getFastCheckColorExpression().length - 2]));
        } else {
            newColor = new Color(TypeFormat.parseInt(getFastCheckColorExpression()[getFastCheckColorExpression().length - 1]));
        }
        setChanges(getChanges() | (newColor.getRGB() != getShapeBorderColor().getRGB()));
        return newColor;
    }

    /**
   * Restituisce il risultato della regola di colorazione
   * @return il colore risultante
   */
    public Color resultOfFastCheckFontColouringRule() {
        if (getFastCheckFontColorExpression() == null || getFastCheckFontColorExpression()[0] == null || getFastCheckFontColorExpression()[0].contentEquals(Text.EMPTY)) return getFontColor();
        boolean res = false;
        if (getFastCheckFontColorExpression()[0].contentEquals(OR)) {
            for (int i = 1; i < getFastCheckFontColorExpression().length - 2; i += 2) {
                ViewerShapes connectedShape = getInfo(TypeFormat.parseInt(getFastCheckFontColorExpression()[i]));
                int c1 = (connectedShape instanceof TextShape) ? ((TextShape) connectedShape).getTextColor().getRGB() : connectedShape.getShapeBorderColor().getRGB();
                if (c1 == TypeFormat.parseInt(getFastCheckFontColorExpression()[i + 1])) res = true;
            }
        } else if (getFastCheckFontColorExpression()[0].contentEquals(AND)) {
            res = true;
            for (int i = 1; i < getFastCheckFontColorExpression().length - 2; i += 2) {
                ViewerShapes connectedShape = getInfo(TypeFormat.parseInt(getFastCheckFontColorExpression()[i]));
                int c1 = (connectedShape instanceof TextShape) ? ((TextShape) connectedShape).getTextColor().getRGB() : connectedShape.getShapeBorderColor().getRGB();
                if (c1 != TypeFormat.parseInt(getFastCheckFontColorExpression()[i + 1])) res = false;
            }
        }
        Color newColor = null;
        if (res) {
            newColor = new Color(TypeFormat.parseInt(getFastCheckFontColorExpression()[getFastCheckFontColorExpression().length - 2]));
        } else {
            newColor = new Color(TypeFormat.parseInt(getFastCheckFontColorExpression()[getFastCheckFontColorExpression().length - 1]));
        }
        setChanges(getChanges() | (newColor.getRGB() != getFontColor().getRGB()));
        return newColor;
    }

    /**
   * Restituisce il risultato della regola di colorazione del bordo
   * @return il colore risultante
   */
    public Color resultOfColouringRule() {
        Text resolvedColorExpr = resolve(getColorExpression()[0]);
        if (!resolvedColorExpr.contentEquals(Text.EMPTY) && !resolvedColorExpr.contentEquals(oldResolvedColorExpr)) {
            oldResolvedColorExpr = resolvedColorExpr;
            setChanges(true);
            if (Evaluator.evaluateExpression(resolvedColorExpr, this).contentEquals(TRUE)) {
                if (!getColorExpression()[1].contentEquals(Text.EMPTY)) {
                    return new Color(TypeFormat.parseInt(getColorExpression()[1].trim()));
                }
            } else {
                if (!getColorExpression()[2].contentEquals(Text.EMPTY)) {
                    return new Color(TypeFormat.parseInt(getColorExpression()[2].trim()));
                }
            }
        }
        return shapeColor;
    }

    /**
   * Restituisce il risultato della regola di colorazione del font
   * @return il colore risultante
   */
    public Color resultOfFontColouringRule() {
        if (this instanceof TextShape) {
            Text resolvedFontColorExpr = resolve(getFontColorExpression()[0]);
            if (!resolvedFontColorExpr.contentEquals(Text.EMPTY) && !resolvedFontColorExpr.contentEquals(oldResolvedFontColorExpr)) {
                oldResolvedFontColorExpr = resolvedFontColorExpr;
                setChanges(true);
                if (Evaluator.evaluateExpression(resolvedFontColorExpr, this).contentEquals(TRUE)) {
                    if (!getFontColorExpression()[1].contentEquals(Text.EMPTY)) {
                        return new Color(TypeFormat.parseInt(getFontColorExpression()[1].trim()));
                    }
                } else {
                    if (!getFontColorExpression()[2].contentEquals(Text.EMPTY)) {
                        return new Color(TypeFormat.parseInt(getFontColorExpression()[2].trim()));
                    }
                }
            }
            return ((TextShape) this).getTextColor();
        }
        return null;
    }

    /**
   * Restituisce il risultato della regola di assegnazione
   * @return il valore risultante
   */
    public String resultOfAssignRule() {
        Text resolvedAssignExpr = resolve(assignExpression);
        if (resolvedAssignExpr.contentEquals(oldResolvedAssignExpr)) return oldAssignExprRes;
        if (!resolvedAssignExpr.contentEquals(Text.EMPTY)) {
            setChanges(true);
            oldResolvedAssignExpr = resolvedAssignExpr;
            oldAssignExprRes = Evaluator.evaluateExpression(resolvedAssignExpr, this).toString();
            return oldAssignExprRes;
        }
        return "";
    }

    public Text fastResolve(Text expr) {
        int start = 0, end = 0;
        for (int i = 0; start != -1 && i < expr.length(); i++) {
            start = expr.indexOf(ID_SEP);
            if (start != -1) {
                end = start + 1 + expr.subtext(start + 1, expr.length()).indexOf(ID_SEP);
                ViewerShapes connectedShape = getInfo(TypeFormat.parseInt(expr.subtext(start + 1, end)));
                expr = expr.delete(start, end + 1);
                if (connectedShape instanceof TextShape) expr = expr.insert(start, ((TextShape) connectedShape).getValInText());
            }
        }
        return expr;
    }

    /**
   * Restituisce il risultato della regola di assegnazione
   * @return il valore risultante
   */
    public String resultOfFastAssignRule() {
        Text resolvedFastAssignExpr = fastResolve(fastAssignExpression);
        if (resolvedFastAssignExpr.contentEquals(oldResolvedFastAssignExpr)) return oldFastAssignExprRes;
        if (!resolvedFastAssignExpr.contentEquals(Text.EMPTY)) {
            setChanges(true);
            oldResolvedAssignExpr = resolvedFastAssignExpr;
            oldFastAssignExprRes = Evaluator.evaluateExpression(resolvedFastAssignExpr, this).toString();
            return oldFastAssignExprRes;
        }
        return "";
    }

    /**
   * Restituisce il risultato della regola di assegnazione
   * @return il valore risultante
   */
    public String resultOfAbsDiffRule() {
        int start = 0, end = 0;
        double res = 0;
        for (int i = 0; i < absdiffExpression.length; i++) {
            double val = 0;
            if (absdiffExpression[i].indexOf(ID_SEP) != -1) {
                ViewerShapes connectedShape = getInfo(TypeFormat.parseInt(absdiffExpression[i].subtext(1, absdiffExpression[i].length())));
                val = Double.parseDouble(((TextShape) connectedShape).getValInText().toString());
            } else {
                val = Double.parseDouble(absdiffExpression[i].toString());
            }
            if (i == 0) res = val; else res -= val;
        }
        return "" + Math.abs(res);
    }

    /**
   * Restituisce il risultato della regola di colorazione del riempimento
   * @return il colore risultante
   */
    public Color resultOfFillColouringRule() {
        Text resolvedFillColorExpr = resolve(getFillColorExpression()[0]);
        if (!resolvedFillColorExpr.contentEquals(Text.EMPTY) && !resolvedFillColorExpr.contentEquals(oldResolvedFillColorExpr)) {
            setChanges(true);
            oldResolvedFillColorExpr = resolvedFillColorExpr;
            if (Evaluator.evaluateExpression(resolvedFillColorExpr, this).contentEquals(TRUE)) {
                if (!getFillColorExpression()[1].contentEquals(Text.EMPTY)) return new Color(TypeFormat.parseInt(getFillColorExpression()[1].trim()));
            } else {
                if (!getFillColorExpression()[2].contentEquals(Text.EMPTY)) return new Color(TypeFormat.parseInt(getFillColorExpression()[2].trim()));
            }
        }
        if (this instanceof FillableShape) return ((FillableShape) this).getShapeFillColor(); else return null;
    }

    /**
   * Restituisce il risultato della regola di cambiamento del valore
   * @return il colore risultante
   */
    public String resultOfChangeValueRule() {
        Text resolvedChangeValueExpr = resolve(getChangeValueExpression()[0]);
        if (!resolvedChangeValueExpr.contentEquals(Text.EMPTY)) {
            if (!resolvedChangeValueExpr.contentEquals(oldResolvedChangeValueExpr)) {
                setChanges(true);
                oldResolvedChangeValueExpr = resolvedChangeValueExpr;
                if (Evaluator.evaluateExpression(resolvedChangeValueExpr, this).contentEquals(TRUE)) {
                    if (!getChangeValueExpression()[1].contentEquals(Text.EMPTY)) {
                        oldChangeExprRes = getChangeValueExpression()[1].toString();
                        return oldChangeExprRes;
                    }
                } else {
                    if (!getChangeValueExpression()[2].contentEquals(Text.EMPTY)) {
                        oldChangeExprRes = getChangeValueExpression()[2].toString();
                        return oldChangeExprRes;
                    }
                }
            }
        } else {
            oldChangeExprRes = "";
        }
        if (this instanceof TextShape) return ((TextShape) this).getTextVal(); else return "";
    }

    public boolean isVisualizationRuleValid(Text resolvedVisExpr) {
        return !resolvedVisExpr.contentEquals(Text.EMPTY);
    }

    /**
   * Controlla se e' verificata o meno l'espressione che regola la visualizzazione
   * @return true se la regola e' verificata
   */
    public boolean verifiedVisualizationRule(Text resolvedVisExpr) {
        if (!resolvedVisExpr.contentEquals(Text.EMPTY) && !resolvedVisExpr.contentEquals(oldResolvedVisExpr)) {
            setChanges(true);
            oldResolvedVisExpr = resolvedVisExpr;
            if (Evaluator.evaluateExpression(resolvedVisExpr, this).contentEquals(FALSE)) {
                oldVisExprRes = false;
                return false;
            } else {
                oldVisExprRes = true;
                return true;
            }
        }
        return oldVisExprRes;
    }

    /**
   * Inizializzazione del menu di pop-up associato all'oggetto grafico
   * @param actionAdapter l'ActionListener da associare
   */
    public void initMenuSysCall(ActionListener actionAdapter) {
        if (this.getImgName() != null && this.getElemId() != 0) {
            Database localDb = this.getFatherPanel().getLocalDb();
            try {
                String query = "SELECT DISTINCT SC.DESCR FROM SYSCALL as SC, SHAPE as SH " + "WHERE SH.IMAGE_NAME = SC.IMAGE_NAME and SH.IMAGE_NAME = '" + this.getImgName() + "' AND SH.ID = SC.FK_SHAPE_ID and SH.ID = " + this.getElemId();
                Statement sp = localDb.createStatement();
                ResultSet rp = sp.executeQuery(query);
                while (rp.next()) {
                    menuItemSysCall = new JMenuItem(rp.getString("DESCR"));
                    menuItemSysCall.addActionListener(actionAdapter);
                    popup.add(menuItemSysCall);
                }
                rp.close();
                sp.close();
            } catch (java.sql.SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }
    }

    public abstract void doWhenSelected(Graphics g, boolean toDraw);

    /**
   * Seleziona l'oggetto (imposta il flag 'isSelected' a true)
   */
    public void select() {
        setChanges(true);
        isSelected = true;
    }

    /**
   * Deseleziona l'oggetto (imposta il flag 'isSelected' a false)
   */
    public void unSelect() {
        setChanges(true);
        isSelected = false;
    }

    /**
   * Restituisce true se l'oggetto � selezionato
   * @return il valore del flag 'isSelected'
   */
    public boolean isSelected() {
        return isSelected;
    }

    /**
   * Restituisce true se l'oggetto pu� effettuare query al DB remoto di connessione
   * @return il valore del flag 'queriesActive'
   */
    public boolean areQueriesAllowed() {
        return queriesActive;
    }

    /**
   * Imposta l'eseguibilit� delle query al DB remoto di connessione
   * @param b il valore da assegnare al flag 'queriesActive'
   */
    public void setQueriesAllowed(boolean b) {
        queriesActive = b;
    }

    /**
   * Restituisce true se l'oggetto grafico � visibile
   * @return il valore del flag 'visible'
   */
    public boolean isVisible() {
        return this.visible;
    }

    /**
   * Imposta la visibilita' dell'oggetto
   * @param v il valore del flag 'visible'
   */
    public void setVisible(boolean v) {
        visible = v;
    }

    /**
     * Esegue una query tramite la connessione associata all'oggetto grafico
     * @param q la stringa contenente la query da sottoporre al DB remoto
     * @return false se la query non da' luogo a risultati
     */
    public boolean doQuery(String q) {
        boolean res = true;
        if (getConnection() != null && q != null && !q.equals("")) {
            try {
                Statement s = getRemoteDb().createStatement();
                if (q.toUpperCase().startsWith("SELECT")) {
                    ResultSet r = s.executeQuery(q);
                    if (!r.next()) res = false; else {
                        r.previous();
                        while (r.next()) {
                            res = (r.getInt(1) == 1 ? true : false);
                        }
                    }
                    r.close();
                    s.close();
                } else {
                    s.executeUpdate(q);
                }
            } catch (Exception ex) {
                setQueriesAllowed(false);
                String msg = "Attenzione: non e' stato possibile eseguire la query " + q + " per l'oggetto avente id=" + getElemId() + " Ricontrollare la sintassi della query assegnata e lo stato" + " della connessione al DB.";
                System.out.println(msg + " - " + ex.getMessage());
            }
        } else {
            System.out.println("Attenzione: si e' cercato di eseguire una query senza che fosse" + "definita una connessione per l'oggetto");
        }
        return res;
    }

    /**
     * Richiama il metodo 'doQuery', controllando prima pero' che le query siano
     * consentite (tramite il metodo 'areQueriesAllowed')
     * @param q la stringa contenente la query da sottoporre al DB remoto
     * @return false se la query non da' luogo a risultati
     */
    public boolean executeQuery(String q) {
        boolean res = true;
        if (areQueriesAllowed()) {
            res = doQuery(q);
        } else res = false;
        return res;
    }

    /**
   * Imposta il rettangolo di inscrizione visualizzato durante il disegno
   * @param v se true, visualizza il triangolo di inscrizione dell'oggetto
   */
    public void setSquareDrawActive(boolean v) {
        squareDrawActive = v;
    }

    /**
   * Restituisce true se e' impostata la visualizzazione del rettangolo di
   * inscrizione dell'oggetto durante il disegno
   * @return il valore del flag 'squareDrawActive'
   */
    public boolean getSquareDrawActive() {
        return squareDrawActive;
    }

    public boolean isInOverlay() {
        return (getFatherPanel() != null && (((((ArianneViewer.DrawingPanel) getFatherPanel()).getFather()).getOverlayMask() & getOverlay()) > 0));
    }

    public void drawPolygon(Graphics2D g, int x[], int y[], int v, boolean toDraw) {
        if (toDraw && isInOverlay()) {
            g.drawPolygon(x, y, v);
        }
    }

    public void drawOval(Graphics2D g, int x, int y, int w, int h, boolean toDraw) {
        if (toDraw && isInOverlay()) {
            g.drawOval(x, y, w, h);
        }
    }

    public void drawRect(Graphics2D g, int x, int y, int w, int h, boolean toDraw) {
        if (toDraw && isInOverlay()) {
            g.drawRect(x, y, w, h);
        }
    }

    public void fillRect(Graphics2D g, int x, int y, int w, int h, boolean toDraw) {
        if (toDraw && isInOverlay()) {
            g.fillRect(x, y, w, h);
        }
    }

    public void drawArc(Graphics2D g, int x, int y, int a1, int a2, int alfa1, int alfa2, boolean toDraw) {
        if (toDraw && isInOverlay()) {
            drawArc(g, x, y, a1, a2, alfa1, alfa2, toDraw);
        }
    }

    public void drawString(Graphics2D g, String s, int x, int y, boolean toDraw) {
        if (toDraw && isInOverlay()) {
            g.drawString(s, x, y);
        }
    }

    /**
   * Esegue il pop-up del menu richiamabile con il tasto destro del mouse
   * @param e il MouseEvent che ha scatenato il richiamo di questo metodo
   * @param p il DrawingPanel di appartenenza dell'oggetto
   * @param xOffSet l'offset x rispetto al punto in cui e' stato cliccato il tasto
   * destro del mouse, che stabilisce la posizione di visualizzazione del menu di pop-up
   * @param yOffSet l'offset y rispetto al punto in cui � stato cliccato il tasto
   * destro del mouse, che stabilisce la posizione di visualizzazione del menu di pop-up
   */
    public void popMenuSysCall(MouseEvent e, ArianneViewer.DrawingPanel p, int xOffSet, int yOffSet) {
        this.setFatherFrame(p.getFatherFrame());
        this.setFatherPanel(p);
        this.setDbConnections(((PictureViewer) this.getFatherFrame()).getDrawingPanel().dbConnections);
        popup.setInvoker(p);
        int x = e.getX() + xOffSet;
        int y = e.getY() + yOffSet;
        popup.show(p, x, y);
    }

    public static BufferedImage stretchImage(Image inImage, int maxWidth, int maxHeight) {
        int origHeight = inImage.getHeight(null);
        int origWidth = inImage.getWidth(null);
        double heightScale = (double) maxHeight / (double) origHeight;
        double widthScale = (double) maxWidth / (double) origWidth;
        int outputWidth, outputHeight;
        outputWidth = (int) (widthScale * origWidth);
        outputHeight = (int) (heightScale * origHeight);
        BufferedImage outImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        AffineTransform tx = new AffineTransform();
        tx.scale(widthScale, heightScale);
        Graphics2D g2d = outImage.createGraphics();
        g2d.drawImage(inImage, tx, null);
        g2d.dispose();
        return outImage;
    }

    /**
   * Svuota il commandBuffer
   */
    public void emptyCommandBuffer() {
        this.commandBuffer.clear();
    }

    public boolean isCommandBufferEmpty() {
        return (commandBuffer.size() == 0);
    }

    public void execute(String command) {
        command = command.trim();
        String[] keyTokens = command.split("\\s");
        if (command.startsWith("sql")) {
            String actKey = keyTokens[0] + " " + keyTokens[1];
            String parms = command.substring(actKey.length(), command.length());
            parseCommand(actKey, parms);
        } else if (command.toUpperCase().startsWith("INVERT")) {
            String actKey = keyTokens[0];
            String parms = keyTokens[1];
            parseCommand(actKey, parms);
        } else if (command.toUpperCase().startsWith("INVERTONEMPTYQUEUE")) {
            String actKey = keyTokens[0];
            String parms = keyTokens[1];
            parseCommand(actKey, parms);
        } else if (command.toUpperCase().startsWith("EMPTYCOMMANDQUEUE")) {
            String actKey = keyTokens[0];
            String parms = keyTokens[1];
            parseCommand(actKey, parms);
        } else if (command.toUpperCase().startsWith("EMPTYCOMMANDQUEUEIFCHECKED")) {
            String actKey = keyTokens[0];
            String parms = keyTokens[1];
            parseCommand(actKey, parms);
        } else if (command.toUpperCase().toUpperCase().startsWith("PRESERVECOMMANDS")) {
            String actKey = keyTokens[0];
            String parms = keyTokens[1];
            parseCommand(actKey, parms);
        } else if (command.toUpperCase().startsWith("STOPBLINKING")) {
            String actKey = keyTokens[0];
            String parms = keyTokens[1];
            parseCommand(actKey, parms);
        } else if (command.toUpperCase().startsWith("STARTBLINKING")) {
            String actKey = keyTokens[0];
            String parms = keyTokens[1];
            parseCommand(actKey, parms);
        } else if (command.toUpperCase().startsWith("EXECUTEQUERY ")) {
            command = command.substring(12, command.length());
            doQuery(command);
        } else {
            try {
                Runtime.getRuntime().exec(command);
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
    }

    public void parseCommand(String actKey, Vector parms) {
        if (actKey.toUpperCase().startsWith("TRANSFERQUEUE ")) {
            actKey = actKey.substring(13, actKey.length()).trim();
            ViewerShapes s = (ViewerShapes) ((ArianneViewer.DrawingPanel) getFatherPanel()).getElem(Integer.parseInt(actKey));
            Vector commandBuffer = parms;
            if (s != null) {
                Iterator it = commandBuffer.iterator();
                while (it.hasNext()) {
                    String key = "" + it.next();
                    if (!key.equals("") && !key.toUpperCase().equals("TRANSFERQUEUE")) {
                        if (s != null && s instanceof ButtonShape) {
                            ((ButtonShape) s).addCommand(key);
                        }
                    }
                }
                commandBuffer.removeAllElements();
            }
        }
    }

    public void parseCommand(String actKey, String parms) {
        int startGetInput = actKey.toUpperCase().indexOf("$GETINPUT('");
        while (startGetInput != -1) {
            int stop = startGetInput + "$GETINPUT('".length();
            String substr = actKey.substring(stop, actKey.length());
            String msg = substr.substring(0, substr.indexOf("')"));
            String in = JOptionPane.showInputDialog(null, msg);
            String usedKeyword = actKey.substring(startGetInput, startGetInput + "$GETINPUT('".length());
            actKey = actKey.replace(usedKeyword + msg + "')", in);
            startGetInput = actKey.toUpperCase().indexOf("$GETINPUT('");
        }
        for (Enumeration keyList = globalVariablesHashTable.keys(); keyList.hasMoreElements(); ) {
            String var = (String) keyList.nextElement();
            if (actKey.toUpperCase().startsWith("ASSIGNGLOBAL")) {
                if (keyList.hasMoreElements()) {
                    var = (String) keyList.nextElement();
                    actKey = execSubstWithDels(actKey, var, (String) globalVariablesHashTable.get(var), "[<>=|&+-/!\\s],;'");
                }
            } else {
                actKey = execSubstWithDels(actKey, var, (String) globalVariablesHashTable.get(var), "[<>=|&+-/!\\s],;'");
            }
        }
        if (actKey.toUpperCase().startsWith("TRANSFERQUEUE ")) {
            actKey = actKey.substring(13, actKey.length()).trim();
            ViewerShapes s = (ViewerShapes) ((ArianneViewer.DrawingPanel) getFatherPanel()).getElem(Integer.parseInt(actKey));
        } else if (actKey.toUpperCase().startsWith("TRANSFERCOMMAND ")) {
            actKey = actKey.substring(15, actKey.length()).trim();
            ViewerShapes s = (ViewerShapes) ((ArianneViewer.DrawingPanel) getFatherPanel()).getElem(Integer.parseInt(parms.replaceAll(" ", "")));
            if (s != null) {
                ((ButtonShape) s).addCommand(actKey);
            }
        } else if (actKey.toUpperCase().startsWith("EXECUTEQUERY ")) {
            actKey = actKey.substring(12, actKey.length()).trim();
            this.doQuery(actKey);
        } else if (actKey.toUpperCase().startsWith("SQL ")) {
            actKey = actKey.substring(4, actKey.length()).trim();
            ViewerShapes s = (ViewerShapes) ((ArianneViewer.DrawingPanel) getFatherPanel()).getElem(Integer.parseInt(actKey.replaceAll(" ", "")));
            if (s != null) s.executeQuery(parms);
        } else if (actKey.toUpperCase().equals("ENABLE")) {
            String[] id = parms.split("\\s");
            for (int i = 0; i < id.length; i++) {
                if (!id[i].equals("") && !id[i].equals(" ")) {
                    ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(id[i].replaceAll(" ", "")));
                    if (s != null) s.setEnabled(true);
                }
            }
        } else if (actKey.toUpperCase().equals("INVERT")) {
            ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            if (s != null) {
                if (s.isEnabled()) s.setEnabled(false); else s.setEnabled(true);
            }
        } else if (actKey.toUpperCase().equals("INVERTONEMPTYQUEUE")) {
            ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            if (s != null && s.isCommandBufferEmpty()) {
                if (s.isEnabled()) s.setEnabled(false); else s.setEnabled(true);
            }
        } else if (actKey.toUpperCase().equals("STARTBLINKING")) {
            ViewerShapes fndS = getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            if (fndS instanceof ButtonShape) {
                ButtonShape s = (ButtonShape) fndS;
                if (s != null) s.startBlinking();
            } else if (fndS instanceof IconShape) {
                IconShape s = (IconShape) fndS;
                if (s != null) s.startBlinking();
            }
        } else if (actKey.toUpperCase().equals("STOPBLINKING")) {
            ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            if (s != null) s.stopBlinking();
        } else if (actKey.toUpperCase().equals("PRESERVECOMMANDS")) {
            ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            if (s != null) s.setCommandPreserve(true);
        } else if (actKey.toUpperCase().equals("EMPTYCOMMANDQUEUE")) {
            ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            if (s != null) {
                s.setCommandPreserve(false);
                s.emptyCommandBuffer();
            }
        } else if (actKey.toUpperCase().equals("EMPTYCOMMANDQUEUEIFCHECKED")) {
            ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            if (s != null) {
                if (s.isChecked()) {
                    s.setCommandPreserve(false);
                    s.emptyCommandBuffer();
                    s.unCheck();
                }
            }
        } else if (actKey.toUpperCase().equals("DISMISS")) {
            ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            if (s != null) s.emptyCommandBuffer();
        } else if (actKey.toUpperCase().equals("DISABLE")) {
            ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            if (s != null) s.setEnabled(false);
        } else if (actKey.toUpperCase().startsWith("ASSIGNGLOBAL ")) {
            String[] res = actKey.split("[=\\s]");
            if (res[1].startsWith("%")) {
                globalVariablesHashTable.put(res[1], parms);
            }
        } else if (actKey.toUpperCase().startsWith("CHANGETEXT ")) {
            ButtonShape s = (ButtonShape) getInfo(Integer.parseInt(parms.replaceAll(" ", "")));
            actKey = actKey.substring(11, actKey.length());
            if (s != null) s.changeText(actKey);
        } else if (actKey.toUpperCase().startsWith("LOADIMAGE ")) {
            actKey = actKey.substring(9, actKey.length());
            String imgToLoad = (actKey.equals("") ? parms : actKey);
            String[] result = imgToLoad.split("[\\s]");
            int i = 0;
            while (i < result.length && result[i].equals("")) {
                i++;
            }
            PictureViewer fatherViewer = ((PictureViewer) this.getFatherPanel().getFatherFrame());
            if (i + 1 < result.length) fatherViewer.execLoadImage(result[i], result[i + 1].replaceAll(" ", "")); else fatherViewer.execLoadImage(getFatherPanel().getLastChoosenDir(), result[i].replaceAll(" ", ""));
        } else if (actKey.toUpperCase().startsWith("TRANSFER ")) {
            actKey = actKey.substring(9, actKey.length());
            String[] keyTokens = actKey.split("\\s");
            try {
                ViewerShapes s = (ViewerShapes) ((ArianneViewer.DrawingPanel) getFatherPanel()).getElem(Integer.parseInt(keyTokens[0]));
                if (s != null) {
                    if (keyTokens.length > 1) {
                        if (keyTokens[1].toUpperCase().equals("SQL")) {
                            actKey = actKey.substring(keyTokens[0].length(), actKey.length());
                            ((ButtonShape) s).addCommand(actKey + " " + parms);
                        } else {
                            actKey = actKey.substring(keyTokens[0].length(), actKey.length());
                            ((ButtonShape) s).addCommand(actKey + " " + parms);
                        }
                    } else {
                        if (keyTokens.length > 1) {
                            actKey = actKey.substring(keyTokens[0].length(), actKey.length());
                            ((ButtonShape) s).addCommand(actKey + " " + parms);
                        }
                    }
                }
            } catch (NumberFormatException nfe) {
                ViewerShapes s = (ViewerShapes) ((ArianneViewer.DrawingPanel) getFatherPanel()).getElem(Integer.parseInt(parms.replaceAll(" ", "")));
                if (s != null) {
                    if (s instanceof ButtonShape) ((ButtonShape) s).addCommand(actKey);
                }
            }
        } else {
            execute(actKey + " " + parms);
        }
    }

    /**
   * Esegue lo scodamento dei comandi accodati nella commandHashTable
   * @param l'hash table contenente i comandi accodati
   */
    public void deQueue(Hashtable commandHashTable, Hashtable commandsOrder) {
        Vector v = new Vector(commandsOrder.keySet());
        Collections.sort(v);
        for (Enumeration e = v.elements(); e.hasMoreElements(); ) {
            Integer lnNo = (Integer) e.nextElement();
            String actKeyN = (String) commandsOrder.get(lnNo);
            Object parms = commandHashTable.get(actKeyN);
            String actKey = actKeyN.substring(actKeyN.indexOf(" "), actKeyN.length()).trim();
            if (!actKey.equals("") && parms != null) {
                if (parms instanceof String) {
                    parseCommand(actKey, "" + parms);
                    commandHashTable.remove(actKeyN);
                    commandBuffer.remove(actKey + " " + parms);
                } else if (parms instanceof Vector) {
                    parseCommand(actKey, (Vector) parms);
                }
            }
        }
        executeCommandQueue();
    }

    public boolean isCommandDelAllowed() {
        return preserveCommands;
    }

    /**
   * Esegue la coda di comandi presente nel command buffer, che alla fine
   * viene svuotato.
   */
    public void executeCommandQueue() {
        Iterator it = commandBuffer.iterator();
        while (it.hasNext()) {
            execute((String) it.next());
        }
        if (isCommandDelAllowed()) {
            emptyCommandBuffer();
        }
    }

    /**
   * Restituisce lo stato del flag 'checked'
   * @return lo stato del flag 'checked'
   */
    public boolean isChecked() {
        return checked;
    }

    /**
   * Imposta il flag 'checked' a true
   */
    public void check() {
        checked = true;
    }

    public int getNextCmdId() {
        return cmdId++;
    }

    /**
   * Imposta il flag 'checked' a false
   */
    public void unCheck() {
        checked = false;
    }

    public boolean mouseEventToRegister() {
        boolean res = false;
        Database localDb = this.getFatherPanel().getLocalDb();
        try {
            String query = "SELECT LINE_NUMBER, LINK_ACTION " + "FROM LINK_SHAPES " + "WHERE ID_SH1=" + getElemId() + " AND IMAGE_NAME_SH1='" + getImgName() + "' AND " + "IMAGE_NAME_SH1=IMAGE_NAME_SH2 ORDER BY LINE_NUMBER";
            Statement sp = localDb.createStatement();
            ResultSet rp = sp.executeQuery(query);
            while (rp.next() && !res) {
                res = true;
            }
        } catch (Exception ex) {
        }
        return res;
    }

    /**
   * Accoda nella commandHashTable i comandi collegati con lo shape corrente
   */
    public void enqueueCommands() {
        Hashtable commandHashTable = new Hashtable();
        Hashtable commandsOrder = new Hashtable();
        boolean noentry = false;
        Database localDb = this.getFatherPanel().getLocalDb();
        check();
        try {
            String query = "SELECT LINE_NUMBER, LINK_ACTION " + "FROM LINK_SHAPES " + "WHERE ID_SH1=" + getElemId() + " AND IMAGE_NAME_SH1='" + getImgName() + "' AND " + "IMAGE_NAME_SH1=IMAGE_NAME_SH2 ORDER BY LINE_NUMBER";
            Statement sp = localDb.createStatement();
            ResultSet rp = sp.executeQuery(query);
            while (rp.next()) {
                String action = rp.getString("LINK_ACTION");
                if (action != null && !action.equals("")) {
                    String[] result = action.split("\\s");
                    if (result[0].startsWith("$SB")) {
                        noentry = true;
                    }
                }
            }
        } catch (Exception ex) {
        }
        if (!noentry) {
            enqueueTabularCommands(commandHashTable, commandsOrder);
            enqueueButtonCommands(commandHashTable, commandsOrder);
            enqueueNShapesCommands(commandHashTable, commandsOrder);
            enqueueSShapesCommands(commandHashTable, commandsOrder);
            deQueue(commandHashTable, commandsOrder);
        }
        ((ArianneViewer.DrawingPanel) getFatherPanel()).lastSelect(getElemId());
    }

    public void addCommand(Hashtable commandHashTable, Hashtable commandsOrder, int lnNo, String command, Object parms) {
        String key = getNextCmdId() + " " + command.trim();
        commandHashTable.put(key, parms);
        commandsOrder.put(new Integer(lnNo), key);
    }

    public void sysCallAction(ActionEvent e) {
        Database localDb = this.getFatherPanel().getLocalDb();
        QueryDataSet queryMenuSQL = new QueryDataSet();
        try {
            String query = "SELECT SYS_CALL_NAME FROM SYSCALL " + "WHERE DESCR = '" + e.getActionCommand() + "' AND " + "FK_SHAPE_ID=" + this.getElemId() + " AND " + "IMAGE_NAME='" + this.getImgName() + "'";
            queryMenuSQL.setQuery(new com.borland.dx.sql.dataset.QueryDescriptor(localDb, query, null, true, Load.ALL));
            queryMenuSQL.open();
            String command = queryMenuSQL.getString("SYS_CALL_NAME");
            if (!command.equals("") && !command.equals("null") && command != null) parseCommand(command, "");
            queryMenuSQL.close();
            queryMenuSQL = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

class Shape_menuItem_actionAdapter implements java.awt.event.ActionListener {

    ViewerShapes adaptee;

    Shape_menuItem_actionAdapter(ViewerShapes adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.shape_menuItem_actionPerformed(e);
    }
}
