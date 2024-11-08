package ArianneEditor;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.logging.Level;
import ArianneUtil.LogHandler;
import java.awt.image.ImageObserver;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Calendar;

/**
 * IconShape � la classe derivata da Shapes che implementa le regole di
 * visualizzazione di un oggetto icona (immagine).
 * <p>
 *
 * @author      Andrea Annibali
 * @version     1.0
 */
class IconShape extends FillableShape implements ImageObserver {

    private String path = null, defaultPath = null;

    private boolean showBorder = true;

    private long elapsed = -1;

    private Stroke drawingStrokeMiter = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] { 1.0f, 1.0f }, 0);

    private javax.swing.Timer blinkTimer = new javax.swing.Timer(getRefreshPeriod() + 1000, new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            if (isVisible()) {
                setVisible(false);
            } else {
                setVisible(true);
            }
        }
    });

    ImageIcon image, defaultImage;

    JMenuItem menuItem2, menuItem3, menuItem5, menuItem6, menuItem7;

    IconShape(int elId, Point ePoint, Point sPoint, int lt, String ls, float a, Color c, boolean sb, String pth, String defPth, String imgN, boolean isOpaque, EditorDrawingPanel p, boolean bck, int ovl, boolean tr) {
        super(imgN, p, elId, "Icon", 4, lt, ls, a, ovl, isOpaque, bck, ePoint, sPoint, tr);
        this.setPath(pth);
        this.setDefaultPath(defPth);
        this.setShapeBorderColor(c);
        this.setShowBorder(sb);
        createImage();
        if (getDefaultPath() != null) defaultImage = new ImageIcon(getDefaultPath());
    }

    IconShape(int elId, double xPnt[], double yPnt[], int lt, String ls, float a, int sColor, boolean sb, String pth, String defPth, String imgName, boolean isOpaque, EditorDrawingPanel p, boolean bck, int ovl, boolean tr) {
        super(imgName, p, elId, "Icon", 4, xPnt, yPnt, lt, ls, a, ovl, isOpaque, bck, tr);
        this.setPath(pth);
        this.setDefaultPath(defPth);
        this.setShowBorder(sb);
        this.setShapeBorderColor(new Color(sColor));
        setXPoints(xPnt);
        setYPoints(yPnt);
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (int i = 0; i < xPnt.length; i++) if (xPnt[i] < minX) minX = xPnt[i];
        for (int i = 0; i < xPnt.length; i++) if (xPnt[i] > maxX) maxX = xPnt[i];
        for (int i = 0; i < yPnt.length; i++) if (yPnt[i] < minY) minY = yPnt[i];
        for (int i = 0; i < yPnt.length; i++) if (yPnt[i] > maxY) maxY = yPnt[i];
        this.inscribePoints(new Point((int) Math.round(maxX), (int) Math.round(maxY)), new Point((int) Math.round(minX), (int) Math.round(minY)));
        image = new ImageIcon(getPath());
        if (getDefaultPath() != null) defaultImage = new ImageIcon(getDefaultPath());
        setIntCoord();
    }

    IconShape(int elId, Color c, String tV, int lt, String ls, float a, int ovl, EditorDrawingPanel p) {
        super("", p, elId, "Icon", 4, new double[4], new double[4], lt, ls, a, ovl, true, false, false);
        this.setPath(tV);
        this.setShapeBorderColor(Color.BLACK);
        this.setShowBorder(true);
        setShapeBorderColor(c);
        setIntCoord();
    }

    public void createImage() {
        image = new ImageIcon(getPath());
    }

    /**
   * Inizializza i menu di pop-up che si attivano con il tasto destro
   * del mouse
   */
    public void initMenu() {
        super.initMenu();
        menuItem2 = new JMenuItem("Call Up Dialog");
        menuItem2.addActionListener(new Icon_menuItem2_actionAdapter(this));
        popup.add(menuItem2);
        menuItem3 = new JMenuItem("Shape link Dialog");
        menuItem3.addActionListener(new Icon_menuItem3_actionAdapter(this));
        popup.add(menuItem3);
        menuItem5 = new JMenuItem("Image file");
        menuItem5.addActionListener(new Icon_menuItem5_actionAdapter(this));
        popup.add(menuItem5);
        menuItem6 = new JMenuItem("Default image file");
        menuItem6.addActionListener(new Icon_menuItem6_actionAdapter(this));
        popup.add(menuItem6);
        menuItem7 = new JMenuItem("Make border invisible");
        menuItem7.addActionListener(new Icon_menuItem7_actionAdapter(this));
        popup.add(menuItem7);
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
        int orgX = getXCoordinates()[0];
        int orgY = getYCoordinates()[0];
        int sw = getMax(getXCoordinates()) - getMin(getXCoordinates());
        int sh = getMax(getYCoordinates()) - getMin(getYCoordinates());
        if (getFatherPanel() != null && getFatherPanel().getGraphics() != null) {
            getFatherPanel().getGraphics().setClip(orgX, orgY, sw, sh);
            getFatherPanel().repaint();
            getFatherPanel().addToGifList(getElemId());
        }
        try {
            Thread.sleep(getFatherPanel().getGifTiming());
        } catch (InterruptedException ex) {
        }
        return true;
    }

    public void setShowBorder(boolean b) {
        showBorder = b;
    }

    public boolean getShowBorder() {
        return showBorder;
    }

    /**
   * Restituisce il path su disco in cui si trova l'immagine da visualizzare
   * @return String
   */
    public String getPath() {
        return this.path;
    }

    public void setPath(String pth) {
        path = pth;
    }

    public String getDefaultPath() {
        return defaultPath;
    }

    public void rebuildImages() {
        createImage();
        createDefaultImage();
    }

    public void createDefaultImage() {
        setDefaultImage(new ImageIcon(getDefaultPath()));
    }

    public void setDefaultPath(String dpth) {
        defaultPath = dpth;
    }

    public void setDefaultImage(ImageIcon imgToSet) {
        defaultImage = imgToSet;
    }

    public void setImage(ImageIcon imgToSet) {
        image = imgToSet;
    }

    /**
   * Imposta il colore di riempimento
   * @param c l'oggetto Color di riempimento
   */
    public void fill(Color c) {
    }

    public Image getImageRGB() {
        return image.getImage();
    }

    public ImageIcon getImage() {
        return image;
    }

    public ImageIcon getDefaultImage() {
        return defaultImage;
    }

    public void updateValues() {
        try {
            if (getRemoteDb() != null) {
                Statement s = getRemoteDb().createStatement();
                ResultSet r = s.executeQuery(getSqlQuery());
                while (r.next()) {
                    if (ArianneUtil.Util.ICON_VISIBLE <= r.getMetaData().getColumnCount()) {
                        setVisible(r.getInt(ArianneUtil.Util.ICON_VISIBLE) % 2 > 0 ? true : false);
                    }
                    if (ArianneUtil.Util.ICON_BLINK <= r.getMetaData().getColumnCount()) {
                        setBlinking(r.getInt(ArianneUtil.Util.ICON_BLINK) % 2 > 0 ? true : false);
                    }
                }
                r.close();
                s.close();
            }
        } catch (java.sql.SQLException sqlex) {
            String msgString = "Attenzione: ricontrollare la query di reperimento dati. \n" + "Si � verificato il seguente errore: \n" + "DataSetException: " + sqlex.getMessage() + "\n" + "VendorError: " + sqlex.getErrorCode() + "\n" + "Query: " + this.getSqlQuery();
            setSqlQuery("");
            JOptionPane.showMessageDialog(null, msgString.substring(0, Math.min(msgString.length(), getFatherPanel().MAX_DIALOG_MSG_SZ)), "Esecuzione query" + "-" + getClass(), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void drawImage(Graphics2D g, ImageIcon imgToDraw, boolean toDraw) {
        if (g == null) return;
        boolean toShow = ((getFatherPanel() == null && g != null) || isInOverlay());
        g.setColor(getShapeBorderColor());
        if (toDraw && getImage() != null && imgToDraw != null && imgToDraw.getIconWidth() > 0 && imgToDraw.getIconHeight() > 0 && toShow) {
            saveOldComposite(g.getComposite());
            g.setComposite(getCurComposite());
            g.drawImage(imgToDraw.getImage(), getIntMinX(), getIntMinY(), getIntWidth(), getIntHeight(), this);
            g.setComposite(getOldComposite());
        }
        g.setColor(getShapeBorderColor());
        g.setStroke(stroke);
        if (toShow) {
            if (!getShowBorder()) {
            } else {
                drawRect(g, getXCoordinates()[0], getYCoordinates()[0], getXCoordinates()[2] - getXCoordinates()[0], getYCoordinates()[2] - getYCoordinates()[0], toDraw);
            }
        }
        if (isSelected()) {
            int selWidth = ra * 2;
            int selHeight = ra * 2;
            int cX = ra;
            int cY = ra;
            g.setColor(Color.black);
            g.setStroke(defaultStroke);
            for (int i = 0; i < getNumVertex(); i++) if (toShow) drawOval(g, getXCoordinates()[i] - cX, getYCoordinates()[i] - cY, selWidth, selHeight, toDraw);
            for (int i = 0; i < getNumVertex(); i++) {
                double midX = (getXPoints()[i] + getXPoints()[(i + 1) % getNumVertex()]) / 2;
                double midY = (getYPoints()[i] + getYPoints()[(i + 1) % getNumVertex()]) / 2;
                if (toShow) drawRect(g, (int) Math.round(midX) - ra, (int) Math.round(midY) - ra, selWidth, selHeight, toDraw);
            }
        }
        if (this.getSquareDrawActive()) {
            Stroke drawingStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0);
            g.setStroke(drawingStroke);
            if (toShow) drawRect(g, getXCoordinates()[0], getYCoordinates()[0], getXCoordinates()[2] - getXCoordinates()[0], getYCoordinates()[2] - getYCoordinates()[0], toDraw);
        }
    }

    /**
   * Visualizza l'oggetto implementandone le regole di rappresentazione
   * @param g l'oggetto graphics su cui disegnare l'oggetto
   */
    public void draw(Graphics2D g, JPanel p, boolean toDraw) {
        if (((isBlinking() && blinkTimer.isRunning()) || (!blinkTimer.isRunning()))) {
            if (getFatherPanel() != null && getFatherPanel().getTimerToggle()) {
                elapsed = Calendar.getInstance().getTimeInMillis() - lastReadMsecElapsed;
                if (elapsed > getFatherPanel().getRefreshPeriod()) {
                    lastReadMsecElapsed += elapsed;
                    if (getConnection() != null && getSqlQuery() != null && !getSqlQuery().equals("") && !getSqlQuery().equals("null")) {
                        updateValues();
                    }
                }
            } else {
                setVisible(true);
            }
            if (!isVisible() || (p instanceof EditorDrawingPanel && !((EditorDrawingPanel) p).getTimerToggle())) {
                drawImage(g, getDefaultImage(), toDraw);
            } else {
                drawImage(g, getImage(), toDraw);
            }
            if (isBlinking()) {
                if (!blinkTimer.isRunning()) blinkTimer.start();
            } else {
                blinkTimer.stop();
            }
        }
    }

    /**
   * Dati i punti di inizio e fine dragging del mouse, che individuano il
   * rettangolo di inscrizione dell'oggetto, setta le coordinate dei vertici
   * del poligono di inscrizione
   * @param ePoint punto finale del dragging
   * @param sPoint punto iniziale del dragging
   * @return true se il punto � nell'area di selezione
   */
    public void inscribePoints(Point ePoint, Point sPoint) {
        int startX = (int) Math.min(ePoint.getX(), sPoint.getX());
        int endX = (int) Math.max(ePoint.getX(), sPoint.getX());
        int startY = (int) Math.min(ePoint.getY(), sPoint.getY());
        int endY = (int) Math.max(ePoint.getY(), sPoint.getY());
        setXPoint(0, startX);
        setYPoint(0, startY);
        setXPoint(1, endX);
        setYPoint(1, startY);
        setXPoint(2, endX);
        setYPoint(2, endY);
        setXPoint(3, startX);
        setYPoint(3, endY);
        setIntCoord();
    }

    /**
   * Dati il punto di fine dragging del mouse e il vertice che sta subendo l'estensione,
   * effettua la deformazione secondo l'incremento determinato dai parametri incX e incY.
   * @param ePoint Point punto finale del dragging
   * @param size int vertice su cui viene effettuato il dragging
   * @param incX double incremento lungo l'asse X
   * @param incY double incremento lungo l'asse Y
   * @param minXVal double coordinata X minima del poligono entro cui � inscritto l'oggetto
   * @param minYVal double coordinata Y minima del poligono entro cui � inscritto l'oggetto
   * @param maxXVal double coordinata X massima del poligono entro cui � inscritto l'oggetto
   * @param maxYVal double coordinata Y massima del poligono entro cui � inscritto l'oggetto
   * @param XoExtend double coordinata X del punto di riferimento in base al quale effettuare la
   * deformazione
   * @param YoExtend double coordinata Y del punto di riferimento in base al quale effettuare la
   * deformazione
   */
    public void extend(Point ePoint, int size, double incX, double incY, double minXVal, double minYVal, double maxXVal, double maxYVal, double XoExtend, double YoExtend) {
        double xp[] = new double[getNumVertex()];
        double yp[] = new double[getNumVertex()];
        int jX[] = new int[getNumVertex()];
        int jY[] = new int[getNumVertex()];
        boolean trovato_x = false;
        boolean trovato_y = false;
        double hAss, wAss;
        final double ALFA = 0.0250;
        final double BETA = 0.0175;
        for (int i = 0; i < getNumVertex(); i++) {
            xp[i] = getXPoints()[getNumVertex() - i - 1];
            yp[i] = getYPoints()[getNumVertex() - i - 1];
            jX[i] = -1;
            jY[i] = -1;
        }
        hAss = maxYVal - minYVal;
        wAss = maxXVal - minXVal;
        for (int i = 0; i < getNumVertex(); i++) {
            if (incX != 0) {
                if (xp[i] == Math.round(XoExtend) - incX) {
                    trovato_x = true;
                } else if (xp[i] == Math.round(XoExtend)) {
                    jX[i] = i;
                }
                if (trovato_x) {
                    if (size == 2) {
                        incX += -ALFA;
                    } else if (size == 0) {
                        incX += ALFA;
                    }
                }
            }
            if (incY != 0) {
                if (yp[i] == Math.round(YoExtend) - incY) {
                    trovato_y = true;
                } else if (yp[i] == Math.round(YoExtend)) {
                    jY[i] = i;
                }
                if (trovato_y) {
                    if (size == 3) {
                        incY += ALFA;
                    } else if (size == 1) {
                        incY += -ALFA;
                    }
                }
            }
        }
        for (int i = 0; i < getNumVertex(); i++) {
            if (wAss > BETA && i != jX[i] && (size == 0 || size == 2)) {
                xp[i] += incX * Math.abs(Math.round(XoExtend) - xp[i]) / wAss;
            }
            if (hAss > BETA && i != jY[i] && (size == 1 || size == 3)) {
                yp[i] += incY * Math.abs(Math.round(YoExtend) - yp[i]) / hAss;
            }
        }
        for (int i = 0; i < getNumVertex(); i++) {
            setXPoint(i, xp[getNumVertex() - i - 1]);
            setYPoint(i, yp[getNumVertex() - i - 1]);
        }
        setIntCoord();
    }

    /**
   * Dati il punto di fine dragging del mouse e il vertice che sta subendo lo stretching,
   * effettua la deformazione secondo l'incremento determinato dai parametri incX e incY.
   * @param ePoint punto finale del dragging
   * @param vertex vertice su cui viene effettuato il dragging
   * @param incX incremento lungo l'asse X
   * @param incY incremento lungo l'asse Y
   * @param minXVal coordinata X minima del poligono entro cui � inscritto l'oggetto
   * @param minYVal coordinata Y minima del poligono entro cui � inscritto l'oggetto
   * @param maxXVal coordinata X massima del poligono entro cui � inscritto l'oggetto
   * @param maxYVal coordinata Y massima del poligono entro cui � inscritto l'oggetto
   * @param XoStretch coordinata X del punto di riferimento in base al quale effettuare la
   * deformazione
   * @param YoStretch coordinata Y del punto di riferimento in base al quale effettuare la
   * deformazione
   * selezione
   */
    public void stretch(Point ePoint, int vertex, double incX, double incY, double minXVal, double minYVal, double maxXVal, double maxYVal, double XoStretch, double YoStretch) {
        double Y[] = new double[4];
        double X[] = new double[4];
        X[0] = getMinX();
        Y[0] = getMaxY();
        X[1] = getMaxX();
        Y[1] = getMaxY();
        X[2] = getMaxX();
        Y[2] = getMinY();
        X[3] = getMinX();
        Y[3] = getMinY();
        double h = Math.abs(Y[2] - Y[1]);
        double w = Math.abs(X[3] - X[2]);
        for (int i = 0; i < getNumVertex() && h > 0 && w > 0; i++) {
            setYPoint(i, getYPoints()[i] + (incY * Math.abs(getYPoints()[i] - Y[(vertex + 2) % getNumVertex()]) / h));
            if (Math.abs(Math.abs(getXPoints()[i] - X[(vertex + 2) % getNumVertex()])) > 0) setXPoint(i, getXPoints()[i] + (incX * Math.abs(getXPoints()[i] - X[(vertex + 2) % getNumVertex()]) / w));
        }
        setIntCoord();
    }

    /**
   * Richiama la dialog di selezione dell'immagine sul disco fisso
   * @param e l'evento (selezione della prima voce del menu di pop up)
   * che ha causato l'invocazione di questo metodo
   */
    void IconmenuItem5_actionPerformed(ActionEvent e) {
        JFileChooser jFileChooser1 = new JFileChooser();
        String separator = "";
        if (getPath() != null && !getPath().equals("")) {
            jFileChooser1.setCurrentDirectory(new File(getPath()));
            jFileChooser1.setSelectedFile(new File(getPath()));
        }
        if (JFileChooser.APPROVE_OPTION == jFileChooser1.showOpenDialog(this.getFatherFrame())) {
            setPath(jFileChooser1.getSelectedFile().getPath());
            separator = jFileChooser1.getSelectedFile().separator;
            File dirImg = new File("." + separator + "images");
            if (!dirImg.exists()) {
                dirImg.mkdir();
            }
            int index = getPath().lastIndexOf(separator);
            String imgName = getPath().substring(index);
            String newPath = dirImg + imgName;
            try {
                File inputFile = new File(getPath());
                File outputFile = new File(newPath);
                if (!inputFile.getCanonicalPath().equals(outputFile.getCanonicalPath())) {
                    FileInputStream in = new FileInputStream(inputFile);
                    FileOutputStream out = new FileOutputStream(outputFile);
                    int c;
                    while ((c = in.read()) != -1) out.write(c);
                    in.close();
                    out.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                LogHandler.log(ex.getMessage(), Level.INFO, "LOG_MSG", isLoggingEnabled());
                JOptionPane.showMessageDialog(null, ex.getMessage().substring(0, Math.min(ex.getMessage().length(), getFatherPanel().MAX_DIALOG_MSG_SZ)) + "-" + getClass(), "", JOptionPane.ERROR_MESSAGE);
            }
            setPath(newPath);
            if (getDefaultPath() == null || getDefaultPath().equals("")) {
                String msgString = "E' stata selezionata un'immagine da associare all'IconShape, ma non e' " + "stata selezionata ancora nessun'immagine di default. Imposto quella scelta anche come " + "immagine di default?";
                if (JOptionPane.showConfirmDialog(null, msgString.substring(0, Math.min(msgString.length(), getFatherPanel().MAX_DIALOG_MSG_SZ)), "choose one", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    setDefaultPath(newPath);
                    createDefaultImage();
                }
            }
            createImage();
        }
    }

    void IconmenuItem6_actionPerformed(ActionEvent e) {
        JFileChooser jFileChooser1 = new JFileChooser();
        String separator = "";
        if (JFileChooser.APPROVE_OPTION == jFileChooser1.showOpenDialog(this.getFatherFrame())) {
            setDefaultPath(jFileChooser1.getSelectedFile().getPath());
            separator = jFileChooser1.getSelectedFile().separator;
            File dirImg = new File("." + separator + "images");
            if (!dirImg.exists()) {
                dirImg.mkdir();
            }
            int index = getDefaultPath().lastIndexOf(separator);
            String imgName = getDefaultPath().substring(index);
            String newPath = dirImg + imgName;
            try {
                File inputFile = new File(getDefaultPath());
                File outputFile = new File(newPath);
                FileInputStream in = new FileInputStream(inputFile);
                FileOutputStream out = new FileOutputStream(outputFile);
                int c;
                while ((c = in.read()) != -1) out.write(c);
                in.close();
                out.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                LogHandler.log(ex.getMessage(), Level.INFO, "LOG_MSG", isLoggingEnabled());
                JOptionPane.showMessageDialog(null, ex.getMessage().substring(0, Math.min(ex.getMessage().length(), getFatherPanel().MAX_DIALOG_MSG_SZ)) + "-" + getClass(), "", JOptionPane.ERROR_MESSAGE);
            }
            setDefaultPath(newPath);
            createDefaultImage();
        }
    }

    void IconmenuItem7_actionPerformed(ActionEvent e) {
        setShowBorder((!getShowBorder()));
        if (getShowBorder()) menuItem7.setText("Make border invisible"); else menuItem7.setText("Make border visible");
        getFatherPanel().repaint();
    }

    /**
   * Richiama la dialog di impostazione i comandi esterni da associare alla shape
   * @param e l'evento (selezione della terza voce del menu di pop up)
   * che ha causato l'invocazione di questo metodo
   */
    void IconmenuItem2_actionPerformed(ActionEvent e) {
        this.getFatherPanel().disableKeyListening();
        callUpDialog();
        this.getFatherPanel().enableKeyListening();
    }

    /**
   * Richiama la dialog di impostazione dei collegamenti della shape con le altre appartenenti
   * alla stessa immagine
   * @param e l'evento (selezione della quarta voce del menu di pop up)
   * che ha causato l'invocazione di questo metodo
   */
    void IconmenuItem3_actionPerformed(ActionEvent e) {
        this.getFatherPanel().disableKeyListening();
        callUpAdiacencesDialog();
        this.getFatherPanel().enableKeyListening();
    }
}

class Icon_menuItem2_actionAdapter implements java.awt.event.ActionListener {

    IconShape adaptee;

    Icon_menuItem2_actionAdapter(IconShape adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.IconmenuItem2_actionPerformed(e);
    }
}

class Icon_menuItem3_actionAdapter implements java.awt.event.ActionListener {

    IconShape adaptee;

    Icon_menuItem3_actionAdapter(IconShape adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.IconmenuItem3_actionPerformed(e);
    }
}

class Icon_menuItem5_actionAdapter implements java.awt.event.ActionListener {

    IconShape adaptee;

    Icon_menuItem5_actionAdapter(IconShape adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.IconmenuItem5_actionPerformed(e);
    }
}

class Icon_menuItem6_actionAdapter implements java.awt.event.ActionListener {

    IconShape adaptee;

    Icon_menuItem6_actionAdapter(IconShape adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.IconmenuItem6_actionPerformed(e);
    }
}

class Icon_menuItem7_actionAdapter implements java.awt.event.ActionListener {

    IconShape adaptee;

    Icon_menuItem7_actionAdapter(IconShape adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.IconmenuItem7_actionPerformed(e);
    }
}
