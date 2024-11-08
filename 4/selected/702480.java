package org.paradise.etrc.view.chart;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import static org.paradise.etrc.ETRC._;
import org.paradise.etrc.ETRC;
import org.paradise.etrc.data.*;
import org.paradise.etrc.dialog.*;
import org.paradise.etrc.slice.ChartSlice;
import org.paradise.etrc.view.chart.traindrawing.TrainDrawing;
import org.paradise.etrc.view.chart.traindrawing.TrainLine;
import org.paradise.etrc.view.sheet.SheetTable;
import org.paradise.etrc.wizard.Wizard;
import org.paradise.etrc.wizard.addtrain.AddTrainWizard;

/**
 * @author lguo@sina.com
 * @version 1.0
 */
public class LinesPanel extends JPanel implements MouseListener, MouseMotionListener {

    /**********************************************************************
 * X轴（时间轴）
 * 名称        类型   单位
 * clock       Date
 * coordinate  int    分钟    = clock - startHour
 * point.x     int    像素    = coordinate * timeScale + leftMargin
 *
 * Y轴
 * 名称         类型  单位
 * dist        int   公里
 * point.y     int   像素     = dist * distScale + topMargin
 **********************************************************************/
    private static final long serialVersionUID = 6196666089237432404L;

    private ChartView chartView;

    public LinesPanel(ChartView _mainView) {
        chartView = _mainView;
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private java.util.Timer myTimer;

    private boolean hasMouseDoubleClicked;

    public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 1) {
            myTimer = new java.util.Timer();
            myTimer.schedule(new TimerTask() {

                public void run() {
                    if (!hasMouseDoubleClicked) {
                        if (e.getButton() == MouseEvent.BUTTON1) mouseClickedOneLeft(e); else mouseClickedOneRight(e);
                    }
                    hasMouseDoubleClicked = false;
                    myTimer.cancel();
                }
            }, 250);
        } else if (e.getClickCount() == 2) {
            hasMouseDoubleClicked = true;
            if (e.getButton() == MouseEvent.BUTTON1) mouseClickedDoubleLeft(e); else mouseClickedDoubleRight(e);
        }
    }

    private void mouseClickedOneLeft(MouseEvent e) {
        selectTrain(e.getPoint());
    }

    private void mouseClickedOneRight(MouseEvent e) {
        if (chartView.activeTrain != null) {
            if (chartView.activeTrainDrawing.pointOnMe(e.getPoint()) || chartView.activeTrainDrawing.pointOnMyRect(e.getPoint())) {
                popupMenuOnActiveTrain(e.getPoint());
            } else {
                popupMenuNoTrain(e.getPoint());
            }
        } else {
            popupMenuNoTrain(e.getPoint());
        }
    }

    private void mouseClickedDoubleLeft(MouseEvent e) {
        if (chartView.activeTrain == null) selectTrain(e.getPoint());
        if (chartView.activeTrain != null) {
            setStationTime(e.getPoint(), true);
        } else {
            chartView.setActiveSation(e.getPoint().y);
        }
    }

    private void mouseClickedDoubleRight(MouseEvent e) {
        if (chartView.activeTrain == null) selectTrain(e.getPoint());
        if (chartView.activeTrain != null) {
            setStationTime(e.getPoint(), false);
        } else {
            popupMenuNoTrain(e.getPoint());
        }
    }

    void jbInit() throws Exception {
        this.setBackground(Color.white);
        this.setFont(new java.awt.Font(_("FONT_NAME"), 0, 12));
        this.setDebugGraphicsOptions(0);
        this.setLayout(new BorderLayout());
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
    }

    public void paint(Graphics g) {
        super.paint(g);
        for (int i = 0; i < 24; i++) {
            drawClockLine(g, i);
        }
        Chart chart = chartView.mainFrame.chart;
        if (chart.circuit != null) {
            for (int i = 0; i < chart.circuit.stationNum; i++) {
                drawStationLine(g, chart.circuit.stations[i], chart.distScale);
            }
        }
        drawTrains(g);
    }

    /**
	 * DrawTrains
	 * 
	 * @param g
	 *            Graphics
	 */
    private void drawTrains(Graphics g) {
        chartView.buildTrainDrawings();
        for (Enumeration<TrainDrawing> e = chartView.underDrawings.elements(); e.hasMoreElements(); ) {
            Object obj = e.nextElement();
            if (obj instanceof TrainDrawing) {
                TrainDrawing trainDrawing = ((TrainDrawing) obj);
                if (!(trainDrawing.equals(chartView.activeTrainDrawing))) {
                    trainDrawing.draw(g);
                }
            }
        }
        for (Enumeration<TrainDrawing> e = chartView.normalDrawings.elements(); e.hasMoreElements(); ) {
            Object obj = e.nextElement();
            if (obj instanceof TrainDrawing) {
                TrainDrawing trainDrawing = ((TrainDrawing) obj);
                if (!(trainDrawing.equals(chartView.activeTrainDrawing))) {
                    trainDrawing.draw(g);
                } else {
                }
            }
        }
        if (chartView.activeTrainDrawing != null) {
            chartView.activeTrainDrawing.draw(g);
        }
    }

    public Dimension getPreferredSize() {
        Chart chart = chartView.mainFrame.chart;
        int w, h;
        w = 60 * 24 * chart.minuteScale + chartView.leftMargin + chartView.rightMargin;
        if (chart.circuit != null) h = chart.circuit.length * chart.distScale + chartView.topMargin + chartView.bottomMargin; else h = 480;
        return new Dimension(w, h);
    }

    /**
	 * DrawHour
	 * 
	 * @param g
	 *            Graphics
	 */
    private void drawClockLine(Graphics g, int clock) {
        Color oldColor = g.getColor();
        g.setColor(chartView.gridColor);
        Chart chart = chartView.mainFrame.chart;
        int start = clock * 60 * chart.minuteScale + chartView.leftMargin;
        int h = chart.circuit.length * chart.distScale;
        g.drawLine(start, 0, start, h + chartView.topMargin + chartView.bottomMargin);
        g.drawLine(start + 1, 0, start + 1, h + chartView.topMargin + chartView.bottomMargin);
        for (int i = 1; i < 60 / chart.timeInterval; i++) {
            int x = start + chart.timeInterval * chart.minuteScale * i;
            int y1 = chartView.topMargin;
            int y2 = y1 + h;
            g.drawLine(x, y1, x, y2);
        }
        if (clock == 23) {
            int end = 24 * 60 * chart.minuteScale + chartView.leftMargin;
            g.drawLine(end, 0, end, h + chartView.topMargin + chartView.bottomMargin);
            g.drawLine(end + 1, 0, end + 1, h + chartView.topMargin + chartView.bottomMargin);
        }
        g.setColor(oldColor);
    }

    private void drawStationLine(Graphics g, Station st, int scale) {
        if (st.hide) return;
        Chart chart = chartView.mainFrame.chart;
        Color oldColor = g.getColor();
        if (st.equals(chartView.activeStation)) g.setColor(chartView.activeGridColor); else g.setColor(chartView.gridColor);
        int y = st.dist * scale + chartView.topMargin;
        int w = 60 * 24 * chart.minuteScale + chartView.leftMargin + chartView.rightMargin;
        if (st.level <= chart.displayLevel) {
            g.drawLine(0, y, w, y);
            if (st.level <= chart.boldLevel) {
                g.drawLine(0, y + 1, w, y + 1);
            }
        }
        g.setColor(oldColor);
    }

    /**
	 * mouseDragged
	 * 
	 * @param e
	 *            MouseEvent
	 */
    public void mouseDragged(MouseEvent e) {
    }

    /**
	 * mouseMoved
	 * 
	 * @param e
	 *            MouseEvent
	 */
    public void mouseMoved(MouseEvent e) {
        Image img = null;
        try {
            img = ImageIO.read(ChartView.class.getResource("/pic/cursor.gif"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (img != null) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            Graphics g = img.getGraphics();
            g.setFont(new Font("Dialog", Font.PLAIN, 10));
            g.setColor(Color.black);
            g.fillRect(2, 10, 30, 11);
            g.setColor(Color.white);
            g.fillRect(3, 11, 28, 9);
            g.setColor(Color.black);
            g.drawString(chartView.getClockString(e.getPoint()), 4, 19);
            Cursor cursor = tk.createCustomCursor(img, new Point(0, 0), "Name");
            this.setCursor(cursor);
        }
        Point p = e.getPoint();
        chartView.setCoordinateCorner(p);
        Train nearTrain[] = findTrains(p);
        for (int i = 0; i < nearTrain.length; i++) chartView.mainFrame.statusBarMain.setText(nearTrain[i].getTrainName(chartView.mainFrame.chart.circuit) + "次 " + chartView.mainFrame.statusBarMain.getText());
    }

    /**
	 * findTrain
	 * 
	 * @param p
	 *            Point
	 * @return Train[]
	 */
    private Train[] findTrains(Point p) {
        Vector<Train> trainsFound = new Vector<Train>();
        for (Enumeration<TrainDrawing> e = chartView.normalDrawings.elements(); e.hasMoreElements(); ) {
            TrainDrawing trainDrawing = (TrainDrawing) e.nextElement();
            if (trainDrawing.pointOnMe(p)) {
                trainsFound.add(trainDrawing.train);
            }
        }
        if (chartView.underDrawingColor != null) {
            for (Enumeration<TrainDrawing> e = chartView.underDrawings.elements(); e.hasMoreElements(); ) {
                TrainDrawing trainDrawing = (TrainDrawing) e.nextElement();
                if (trainDrawing.pointOnMe(p)) {
                    trainsFound.add(trainDrawing.train);
                }
            }
        }
        Train array[] = new Train[trainsFound.size()];
        int i = 0;
        for (Enumeration<Train> e = trainsFound.elements(); e.hasMoreElements(); ) {
            array[i++] = (Train) e.nextElement();
        }
        return array;
    }

    private void setStationTime(Point p, boolean isArrive) {
        chartView.setActiveSation(p.y);
        SheetTable table = chartView.mainFrame.sheetView.table;
        if (table.getCellEditor() != null) table.getCellEditor().stopCellEditing();
        String theTime = chartView.getTime(p.x);
        Train theTrain = chartView.activeTrain;
        String staName = chartView.activeStation.name;
        if (theTrain.hasStop(staName)) {
            if (isArrive) theTrain.setArrive(staName, theTime); else theTrain.setLeave(staName, theTime);
        } else {
            Stop stop = new Stop(staName, theTime, theTime, false);
            chartView.mainFrame.chart.insertNewStopToTrain(theTrain, stop);
        }
        repaint();
    }

    /**
	 * 选择p点附近的Train作为ActiveTrain 若p点不靠近任何一个车次则ActiveTrain设为null
	 * 
	 * @param p
	 *            Point
	 */
    private void selectTrain(Point p) {
        TrainDrawing selectedTrainDrawing = null;
        if (chartView.underDrawingColor != null) {
            for (Enumeration<TrainDrawing> e = chartView.underDrawings.elements(); e.hasMoreElements(); ) {
                TrainDrawing trainDrawing = (TrainDrawing) e.nextElement();
                if (trainDrawing.pointOnMyRect(p) || trainDrawing.pointOnMe(p)) {
                    selectedTrainDrawing = trainDrawing;
                }
            }
        }
        for (Enumeration<TrainDrawing> e = chartView.normalDrawings.elements(); e.hasMoreElements(); ) {
            TrainDrawing trainDrawing = (TrainDrawing) e.nextElement();
            if (trainDrawing.pointOnMyRect(p) || trainDrawing.pointOnMe(p)) {
                selectedTrainDrawing = trainDrawing;
            }
        }
        chartView.setActiveTrain((selectedTrainDrawing == null) ? null : selectedTrainDrawing.train);
    }

    private void doEditActiveTrain() {
        TrainDialog dialog = new TrainDialog(chartView.mainFrame, chartView.activeTrain);
        dialog.editTrain();
        if (!dialog.isCanceled) {
            Train editedTrain = dialog.getTrain();
            Chart chart = chartView.mainFrame.chart;
            if (chart.isLoaded(editedTrain)) {
                chart.updateTrain(editedTrain);
            } else {
                chart.delTrain(chartView.activeTrain);
                chart.addTrain(editedTrain);
            }
            chartView.setActiveTrain(editedTrain);
            chartView.mainFrame.sheetView.updateData();
            chartView.mainFrame.runView.refresh();
        }
    }

    private void doDeleteActiveTrain() {
        Chart chart = chartView.mainFrame.chart;
        chart.delTrain(chartView.activeTrain);
        chartView.setActiveTrain(null);
        chartView.mainFrame.sheetView.updateData();
        chartView.mainFrame.runView.refresh();
    }

    /**
	 * mouseEntered
	 * 
	 * @param e
	 *            MouseEvent
	 */
    public void mouseEntered(MouseEvent e) {
    }

    /**
	 * mouseExited
	 * 
	 * @param e
	 *            MouseEvent
	 */
    public void mouseExited(MouseEvent e) {
    }

    /**
	 * mousePressed
	 * 
	 * @param e
	 *            MouseEvent
	 */
    public void mousePressed(MouseEvent e) {
    }

    /**
	 * mouseReleased
	 * 
	 * @param e
	 *            MouseEvent
	 */
    public void mouseReleased(MouseEvent e) {
    }

    public String getToolTipText(MouseEvent event) {
        Point p = event.getPoint();
        if (chartView.activeTrainDrawing != null) {
            TrainLine line = chartView.activeTrainDrawing.findNearTrainLine(p);
            if (line != null) return line.getInfo(); else if (chartView.activeTrainDrawing.pointOnMyRect(p)) return chartView.activeTrainDrawing.getInfo();
        }
        return null;
    }

    public JToolTip createToolTip() {
        JToolTip toolTip = new JToolTip();
        toolTip.setFont(new Font("Dialog", 0, 12));
        return toolTip;
    }

    private void popupMenuNoTrain(final Point p) {
        MenuItem miNewTrain = new MenuItem(_("Add New Train"));
        miNewTrain.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doAddNewTrain();
            }
        });
        MenuItem miGif = new MenuItem(_("Export Graph..."));
        miGif.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ETRC.getInstance().getMainFrame().doExportChart();
            }
        });
        MenuItem miFindTrains = new MenuItem(_("Load Train..."));
        miFindTrains.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ETRC.getInstance().getMainFrame().doLoadTrain();
            }
        });
        MenuItem miLoad = new MenuItem(_("Open..."));
        miLoad.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ETRC.getInstance().getMainFrame().doLoadChart();
            }
        });
        MenuItem miSave = new MenuItem(_("Save"));
        miSave.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ETRC.getInstance().getMainFrame().doSaveChart();
            }
        });
        MenuItem miSaveAs = new MenuItem(_("Save As..."));
        miSaveAs.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ETRC.getInstance().getMainFrame().doSaveChartAs();
            }
        });
        PopupMenu pop = new PopupMenu();
        pop.add(miNewTrain);
        pop.add(miFindTrains);
        pop.addSeparator();
        pop.add(miGif);
        pop.add(miLoad);
        pop.add(miSave);
        pop.add(miSaveAs);
        this.add(pop);
        pop.show(this, p.x, p.y);
    }

    private void popupMenuOnActiveTrain(final Point p) {
        if (chartView.activeTrain == null) return;
        if (!chartView.activeTrainDrawing.pointOnMe(p) && !chartView.activeTrainDrawing.pointOnMyRect(p)) return;
        MenuItem miDelTrain = new MenuItem(_("Delete"));
        miDelTrain.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doDeleteActiveTrain();
            }
        });
        MenuItem miEditTimes = new MenuItem(_("Edit Time Table"));
        miEditTimes.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doEditActiveTrain();
            }
        });
        MenuItem miColor = new MenuItem(_("Change Color"));
        miColor.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doSetColor();
            }
        });
        MenuItem miTrainSlice = new MenuItem(_("Train Slice"));
        miTrainSlice.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new ChartSlice(chartView.mainFrame.chart).makeTrainSlice(chartView.activeTrain);
            }
        });
        PopupMenu pop = new PopupMenu();
        pop.add(miDelTrain);
        pop.add(miColor);
        pop.add(miEditTimes);
        pop.add(miTrainSlice);
        this.add(pop);
        pop.show(this, p.x, p.y);
    }

    private void doAddNewTrain() {
        AddTrainWizard wizard = new AddTrainWizard(this.chartView);
        if (wizard.doWizard() == Wizard.FINISHED) {
            Train train = wizard.getTrain();
            if (train == null) return;
            Chart chart = chartView.mainFrame.chart;
            if (chart.containTrain(train)) {
                if (new YesNoBox(chartView.mainFrame, String.format(_("Train %s is already in the graph, overwrite?"), train.getTrainName())).askForYes()) {
                    chartView.mainFrame.chart.delTrain(train);
                    chartView.addTrain(train);
                }
            } else {
                chartView.addTrain(train);
            }
        }
    }

    private void doSetColor() {
        final JColorChooser colorChooser = new JColorChooser();
        ActionListener listener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                chartView.activeTrainDrawing.train.color = colorChooser.getColor();
                LinesPanel.this.repaint();
            }
        };
        JDialog dialog = JColorChooser.createDialog(chartView.mainFrame, _("Select the color for the line"), true, colorChooser, listener, null);
        colorChooser.setColor(chartView.activeTrainDrawing.train.color);
        ETRC.setFont(dialog);
        Dimension dlgSize = dialog.getPreferredSize();
        Dimension frmSize = chartView.mainFrame.getSize();
        Point loc = chartView.mainFrame.getLocation();
        dialog.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
        dialog.setVisible(true);
    }

    public void moveToTrainDrawing(TrainDrawing trainDrawing) {
        Rectangle bounds = trainDrawing.getPreferredBounds();
        bounds.y = 0;
        bounds.height = this.getHeight();
        scrollRectToVisible(bounds);
        chartView.setActiveTrain((trainDrawing == null) ? null : trainDrawing.train);
    }
}
