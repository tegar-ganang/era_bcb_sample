package org.ttalbott.mytelly;

import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.text.*;
import javax.swing.*;
import org.ttalbott.mytelly.Config.ColorValue;

/**
 *
 * @author  Tom Talbott
 * @version 
 */
public class TVGridPanel extends javax.swing.JPanel implements Printable {

    private static final int actualRowsPerPage = 41;

    private static int lastPage = 0;

    private TVGridHeader m_colHeader = null;

    private TVGridHeader m_rowHeader = null;

    private TVGrid m_grid = null;

    public JScrollBar m_horzScroll = null;

    public JScrollBar m_vertScroll = null;

    private Programs m_programs = null;

    private org.ttalbott.mytelly.Config m_config = null;

    private TVGrid.TVGridCellData m_selectedCell = null;

    private TVGrid.TVGridCellData m_pressedCellData = null;

    private int m_selectedRow = -1;

    private int m_selectedCol = -1;

    private static Font defaultFont = null;

    private static Font boldFont = null;

    /** Creates new TVGridPanel */
    public TVGridPanel() {
        m_grid = new TVGrid();
        String colHeadings[] = generateColHeadings();
        m_colHeader = new TVGridHeader(m_grid, TVGridHeader.COLHEADER, colHeadings);
        m_rowHeader = new TVGridHeader(m_grid, TVGridHeader.ROWHEADER, null);
        m_horzScroll = new JScrollBar(JScrollBar.HORIZONTAL);
        m_vertScroll = new JScrollBar(JScrollBar.VERTICAL);
        m_horzScroll.addAdjustmentListener(new java.awt.event.AdjustmentListener() {

            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                horzScrollAdjustmentValueChanged(evt);
            }
        });
        m_horzScroll.setValues(0, 1, 0, m_grid.getColCount());
        Utilities.addAdvancedScrollSupport(m_grid, m_vertScroll, 6);
        m_vertScroll.addAdjustmentListener(new java.awt.event.AdjustmentListener() {

            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                vertScrollAdjustmentValueChanged(evt);
            }
        });
        m_grid.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                gridMousePressed(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                gridMouseReleased(evt);
            }
        });
        super.setLayout(new java.awt.BorderLayout());
        add(m_colHeader, java.awt.BorderLayout.NORTH);
        add(m_rowHeader, java.awt.BorderLayout.WEST);
        add(m_grid, java.awt.BorderLayout.CENTER);
        add(m_vertScroll, java.awt.BorderLayout.EAST);
        add(m_horzScroll, java.awt.BorderLayout.SOUTH);
    }

    private void vertScrollAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
        m_grid.scrollValueChanged(evt);
        m_rowHeader.scrollValueChanged(evt);
    }

    private void horzScrollAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
        m_grid.scrollValueChanged(evt);
        m_colHeader.scrollValueChanged(evt);
    }

    private void gridMousePressed(java.awt.event.MouseEvent evt) {
        if (SwingUtilities.isLeftMouseButton(evt)) {
            Point click = evt.getPoint();
            m_pressedCellData = m_grid.getCellData(click);
        }
    }

    private void gridMouseReleased(java.awt.event.MouseEvent evt) {
        if (SwingUtilities.isLeftMouseButton(evt)) {
            Point click = evt.getPoint();
            TVGrid.TVGridCellData data = m_grid.getCellData(click);
            if (data != null && data == m_pressedCellData) {
                Rectangle outRect = new Rectangle();
                m_grid.calcVisibleCellRect(click, outRect);
                if (click.x > outRect.x + 3 && click.x < outRect.x + m_grid.m_checksize + 5) {
                    Schedule.getInstance().toggleSchedule(data.prog);
                    m_grid.repaint(outRect);
                }
            }
        }
        m_pressedCellData = null;
    }

    private void gridMouseClicked(java.awt.event.MouseEvent evt) {
        Point click = evt.getPoint();
        TVGrid.TVGridCellData data = m_grid.getCellData(click);
        if (data != null) {
            Rectangle outRect = new Rectangle();
            m_grid.calcVisibleCellRect(click, outRect);
            if (click.x > outRect.x + 3 && click.x < outRect.x + m_grid.m_checksize + 5) {
                Schedule.getInstance().toggleSchedule(data.prog);
                m_grid.repaint(outRect);
            }
        }
    }

    private String[] generateColHeadings() {
        int colCount = m_grid.getColCount() + 1;
        String[] headings = new String[colCount];
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.AM_PM, Calendar.AM);
        cal.clear(Calendar.HOUR);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        headings[0] = "Channels";
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
        for (int i = 1; i < colCount; i++) {
            headings[i] = sdf.format(cal.getTime());
            cal.add(Calendar.MINUTE, 30);
        }
        return headings;
    }

    public void setConfig(Config config) {
        m_config = config;
    }

    public Config getConfig() {
        return m_config;
    }

    public Programs getPrograms() {
        return m_programs;
    }

    public void setPrograms(Programs programs) {
        m_programs = programs;
        m_selectedCell = null;
        m_pressedCellData = null;
        m_grid.setPrograms(m_programs);
    }

    public void setDayToDisplay(int dayToDisplay) {
        m_grid.setDataForDay(dayToDisplay);
        m_grid.repaint();
    }

    public TVGridHeader getRowHeader() {
        return m_rowHeader;
    }

    public TVGridHeader getColHeader() {
        return m_colHeader;
    }

    public void setPrinting(boolean b) {
        m_colHeader.setPrinting(b);
        m_rowHeader.setPrinting(b);
        m_grid.setPrinting(b);
    }

    public void setLayout(java.awt.LayoutManager layoutManager) {
    }

    public void addMouseListener(java.awt.event.MouseListener mouseListener) {
        m_grid.addMouseListener(mouseListener);
    }

    protected void setSelectedCell(TVGrid.TVGridCellData data) {
        TVGrid.TVGridCellData old = m_selectedCell;
        m_selectedCell = data;
        firePropertyChange("selectedCell", old, m_selectedCell);
    }

    public void setSelectedCell(Point point) {
        m_grid.setSelectedCell(point);
    }

    public TVGrid.TVGridCellData getCellData(Point point) {
        return m_grid.getCellData(point);
    }

    public TVGrid.TVGridCellData getSelectedCell() {
        return m_selectedCell;
    }

    /**
     * @return The index representing the current day in the programs file.
     */
    public int setNow() {
        return m_grid.setNow();
    }

    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.black);
        int fontHeight = g2.getFontMetrics().getHeight();
        int fontDesent = g2.getFontMetrics().getDescent();
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        Paper p = new Paper();
        p.setImageableArea(30, 20, 670, 900);
        p.setSize(630, 900);
        pageFormat.setPaper(p);
        double pageHeight = pageFormat.getImageableHeight();
        double pageWidth = pageFormat.getImageableWidth();
        double tableWidth = (double) getWidth();
        double scale = 0.6d;
        double headerHeightOnPage = m_colHeader.getPreferredSize().getHeight() * scale;
        double tableWidthOnPage = tableWidth * scale;
        double oneRowHeight = m_grid.getRowHeight() * scale;
        int numRowsOnAPage = actualRowsPerPage;
        double pageHeightForTable = oneRowHeight * numRowsOnAPage;
        int totalNumPages = (int) Math.ceil(((double) m_grid.getRowCount()) / numRowsOnAPage);
        if (m_config != null && m_config.getDebug()) System.out.println("pageHeight:" + pageHeight + " pageHeightForTable:" + pageHeightForTable + " m_grid.rowcount:" + m_grid.getRowCount() + " numRowsOnAPage:" + numRowsOnAPage + " totalNumPages:" + totalNumPages);
        if (pageIndex >= totalNumPages) {
            return NO_SUCH_PAGE;
        }
        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g.setClip(0, 0, (int) pageFormat.getImageableWidth(), (int) pageFormat.getImageableHeight());
        g2.scale(scale, scale);
        if (pageIndex > 0 && pageIndex != lastPage) {
            m_vertScroll.setValue(m_vertScroll.getValue() + actualRowsPerPage);
        }
        lastPage = pageIndex;
        paint(g2);
        return Printable.PAGE_EXISTS;
    }

    public class TVGridHeader extends javax.swing.JPanel {

        public static final int ROWHEADER = 1;

        public static final int COLHEADER = 2;

        private int m_height = -1;

        private int m_width = -1;

        private TVGrid m_grid = null;

        private int m_type = -1;

        private Color m_bkgrndColor = new Color(127, 63, 63);

        private Color m_frgrndColor = new Color(255, 255, 0);

        private Object m_headings[];

        public TVGridHeader(TVGrid grid, int type, Object headings[]) {
            m_type = (type >= ROWHEADER && type <= COLHEADER ? type : -1);
            m_grid = grid;
            setHeadings(headings);
        }

        public void setHeadings(Object headings[]) {
            m_headings = headings;
        }

        protected void scrollValueChanged(java.awt.event.AdjustmentEvent evt) {
            repaint();
        }

        public void setPrinting(boolean b) {
            if (b) {
                m_bkgrndColor = Color.WHITE;
                m_frgrndColor = Color.BLACK;
            } else {
                m_bkgrndColor = new Color(127, 63, 63);
                m_frgrndColor = new Color(255, 255, 0);
            }
        }

        public java.awt.Dimension getPreferredSize() {
            java.awt.Dimension retValue;
            int height = 0;
            int width = 0;
            int x = 0;
            int y = 0;
            TVGridPanel parent = (TVGridPanel) getParent();
            if (m_grid != null && m_type != -1) {
                Insets insets = parent.getInsets();
                if (m_type == COLHEADER) {
                    height = (m_height < 0 ? m_grid.getRowHeight() : m_height);
                    width = m_grid.getWidth() - insets.right - insets.left;
                } else if (m_type == ROWHEADER) {
                    TVGridHeader colHeader = ((TVGridPanel) getParent()).getColHeader();
                    int chHeight = 0;
                    if (colHeader != null) {
                        chHeight = colHeader.getBounds().height;
                    }
                    y = insets.top + chHeight;
                    width = (m_width < 0 ? m_grid.getColWidth() : m_width);
                    height = m_grid.getHeight() - y - insets.bottom - insets.top;
                }
            }
            retValue = new Dimension(width, height);
            return retValue;
        }

        protected void paintComponent(java.awt.Graphics graphics) {
            super.paintComponent(graphics);
            Rectangle clipRect = graphics.getClipBounds();
            Insets insets = m_grid.getInsets();
            int rows = 1;
            int cols = 1;
            int colHdrHeight = -1;
            int rowHdrWidth = -1;
            int colWidth = m_grid.getColWidth();
            int rowHeight = m_grid.getRowHeight();
            int horzScrollPos = 0;
            int vertScrollPos = 0;
            if (m_type == COLHEADER) {
                cols = m_grid.getColCount();
                if (m_rowHeader != null) {
                    rowHdrWidth = m_rowHeader.getBounds().width;
                    cols++;
                }
                horzScrollPos = m_horzScroll.getValue();
            } else if (m_type == ROWHEADER) {
                rows = m_grid.getRowCount();
                vertScrollPos = m_vertScroll.getValue();
            }
            Color oldColor = graphics.getColor();
            graphics.setColor(m_bkgrndColor);
            for (int row = vertScrollPos; row < rows; row++) {
                for (int col = horzScrollPos; col < cols; col++) {
                    int x = insets.left;
                    if (rowHdrWidth < 0) {
                        x += (col - horzScrollPos) * colWidth;
                    } else if (col != vertScrollPos) {
                        x += rowHdrWidth + (col - horzScrollPos - 1) * colWidth;
                    }
                    int y = insets.top + (row - vertScrollPos) * rowHeight;
                    Rectangle cellRect = new Rectangle(x, y, colWidth - 1, rowHeight - 1);
                    if (clipRect == null || clipRect.intersects(cellRect)) {
                        paintCell(graphics, (rowHdrWidth >= 0 && col == horzScrollPos ? 0 : col), row, cellRect);
                    }
                }
            }
            graphics.setColor(oldColor);
        }

        protected void paintCell(java.awt.Graphics graphics, int col, int row, Rectangle cellRect) {
            graphics.fill3DRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height, true);
            if (m_headings != null) {
                int i = (m_type == COLHEADER ? col : row);
                if (i < m_headings.length && m_headings[i] != null) {
                    Color oldColor = graphics.getColor();
                    graphics.setColor(m_frgrndColor);
                    graphics.drawString(m_headings[i].toString(), cellRect.x + 3, cellRect.y + cellRect.height - 5);
                    graphics.setColor(oldColor);
                }
            }
        }
    }

    public class TVGrid extends javax.swing.JPanel {

        private int m_rowCount = 100;

        private int m_colCount = 52;

        private int m_rowHeight = -1;

        private int m_baseline = -1;

        private int m_colWidth = 130;

        private TVGridCellData m_cellData[][] = null;

        private StringBuffer m_descBuffer;

        private Color m_selectedBgColor = UIManager.getColor("Table.selectionBackground");

        private Color m_selectedFgColor = UIManager.getColor("Table.selectionForeground");

        private ImageIcon m_favorite = null;

        private JCheckBox m_checkbox = new JCheckBox();

        int m_checksize = 0;

        private Programs m_programs;

        private Vector m_channelDescs = null;

        public TVGrid() {
            setBackground(new Color(230, 230, 230));
            setPreferredSize(new Dimension(500, 300));
            enableEvents(MouseEvent.MOUSE_EVENT_MASK);
            m_checkbox.setBorderPaintedFlat(true);
            int iconGap = UIManager.getInt("CheckBox.textIconGap");
            Icon icon = UIManager.getIcon("CheckBox.icon");
            Insets inset = UIManager.getInsets("CheckBox.margin");
            int iconWidth = icon.getIconWidth();
            m_checksize = iconGap + iconWidth + inset.left + inset.right;
            try {
                m_favorite = new ImageIcon(getClass().getResource("favorite.gif"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setPrinting(boolean b) {
            if (b) {
                setBackground(Color.WHITE);
            } else {
                setBackground(new Color(230, 230, 230));
            }
        }

        protected void scrollValueChanged(java.awt.event.AdjustmentEvent evt) {
            repaint();
        }

        public TVGridCellData getCellData(Point point) {
            int horzScrollPos = m_horzScroll.getValue();
            int vertScrollPos = m_vertScroll.getValue();
            int rowClicked = (point.y / m_rowHeight) + vertScrollPos;
            int colClicked = (point.x / m_colWidth) + horzScrollPos;
            return getCellData(rowClicked, colClicked);
        }

        public TVGridCellData getCellData(int row, int col) {
            if (m_cellData != null && row < m_rowCount && col < m_colCount) {
                int dataCol = col;
                TVGridCellData cell = null;
                while (dataCol >= 0 && (cell = m_cellData[row][dataCol]) == null) {
                    dataCol--;
                }
                if (cell != null && dataCol + cell.colSpan - 1 < col) cell = null;
                if (cell != null) cell.actualCol = dataCol;
                return cell;
            } else return null;
        }

        protected void paintComponent(java.awt.Graphics graphics) {
            super.paintComponent(graphics);
            Rectangle clipRect = graphics.getClipBounds();
            graphics.setColor(getBackground());
            int horzScrollPos = m_horzScroll.getValue();
            int vertScrollPos = m_vertScroll.getValue();
            int span;
            if (m_cellData != null) {
                Insets insets = getInsets();
                for (int row = vertScrollPos; row < m_rowCount; row++) {
                    for (int col = horzScrollPos; col < m_colCount; col++) {
                        TVGridCellData cell = getCellData(row, col);
                        Rectangle cellRect = new Rectangle();
                        span = calcVisibleCellRect(row, col, cellRect);
                        if (span == -1) continue;
                        if (clipRect == null || clipRect.intersects(cellRect)) {
                            paintCell(graphics, row, col, cell, cellRect);
                        }
                        col += span - 1;
                    }
                }
            } else System.err.println("No cell data set in TVGrid");
        }

        /**
         * @param outRect returns the rectangle
         * @return The number of columns spanned.
         */
        protected int calcVisibleCellRect(Point point, Rectangle outRect) {
            int horzScrollPos = m_horzScroll.getValue();
            int vertScrollPos = m_vertScroll.getValue();
            int rowClicked = (point.y / m_rowHeight) + vertScrollPos;
            int colClicked = (point.x / m_colWidth) + horzScrollPos;
            return calcVisibleCellRect(rowClicked, colClicked, outRect);
        }

        /**
         * @param outRect returns the rectangle
         * @return The number of columns spanned.
         */
        protected int calcVisibleCellRect(int row, int col, Rectangle outRect) {
            TVGridCellData cell = getCellData(row, col);
            int horzScrollPos = m_horzScroll.getValue();
            int vertScrollPos = m_vertScroll.getValue();
            int span = 1;
            int firstVisibleCol = (cell == null || horzScrollPos > cell.actualCol ? horzScrollPos : cell.actualCol);
            if (cell != null && cell.colSpan > (firstVisibleCol - cell.actualCol)) span = (cell.colSpan - (firstVisibleCol - cell.actualCol));
            if (cell != null && (cell.prog != null || cell.colSpan > 1)) {
                Insets insets = getInsets();
                outRect.setRect(insets.left + ((firstVisibleCol - horzScrollPos) * m_colWidth), insets.top + ((row - vertScrollPos) * getRowHeight()), span * m_colWidth, getRowHeight());
            } else {
                outRect.setRect(0, 0, 0, 0);
                return -1;
            }
            return span;
        }

        protected void paintCell(java.awt.Graphics graphics, int row, int col, TVGrid.TVGridCellData cell, Rectangle cellRect) {
            boolean raised = true;
            if (defaultFont == null) {
                defaultFont = graphics.getFont();
                boldFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
            }
            if (row == m_selectedRow && cell.actualCol == m_selectedCol) {
                graphics.setColor(m_selectedBgColor);
                m_checkbox.setBackground(m_selectedBgColor);
            } else {
                graphics.setColor(getBackground());
                m_checkbox.setBackground(getBackground());
            }
            graphics.fill3DRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height, raised);
            if (row == m_selectedRow && cell.actualCol == m_selectedCol) {
                graphics.setColor(m_selectedFgColor);
                m_checkbox.setForeground(m_selectedFgColor);
            } else {
                graphics.setColor(getForeground());
                m_checkbox.setForeground(getForeground());
            }
            if (cell != null && cell.prog != null) {
                Rectangle drawRect = new Rectangle(cellRect);
                BufferedImage buffer = (BufferedImage) createImage(drawRect.width - 7, drawRect.height - 4);
                Graphics2D gBuffer = buffer.createGraphics();
                gBuffer.setClip(0, 0, cellRect.width, cellRect.height);
                m_checkbox.setSelected(Schedule.getInstance().checkInSchedule(cell.prog));
                m_checkbox.setBounds(0, 0, drawRect.width, drawRect.height - 2);
                m_checkbox.paint(gBuffer);
                graphics.drawImage(buffer, drawRect.x + 5, drawRect.y + 2, this);
                int xOffset = 5 + m_checksize;
                if (Favorites.getInstance().isFavorite(m_programs.getData(cell.prog, ProgramData.TITLE))) {
                    m_favorite.paintIcon(this, graphics, drawRect.x + xOffset, drawRect.y + drawRect.height - 4 - 12);
                    xOffset += 15;
                }
                drawRect.translate(xOffset, 2);
                drawRect.setSize(cellRect.width - xOffset - 5, cellRect.height - 2);
                StringBuffer desc = getDescBuffer();
                desc.append(m_programs.getData(cell.prog, ProgramData.TITLE));
                String subtitle = m_programs.getData(cell.prog, ProgramData.SUBTITLE);
                if (subtitle != null) {
                    desc.append(" \"");
                    desc.append(subtitle);
                    desc.append('\"');
                }
                String cat = m_programs.getData(cell.prog, ProgramData.CATEGORY);
                ColorValue textColor = m_config.getCategoryColor((cat != null ? cat : "NONE"));
                if (Hiddens.getInstance().isHidden(m_programs.getData(cell.prog, ProgramData.TITLE))) textColor = m_config.getCategoryColor("HIDDEN PROGRAMS");
                String prevShown = m_programs.getData(cell.prog, ProgramData.PREVIOUSLYSHOWN);
                if (prevShown == null || !(prevShown.equalsIgnoreCase("true")) && m_config.getFirstRunBold()) {
                    graphics.setFont(boldFont);
                    drawStringRect(graphics, desc.toString(), drawRect, (textColor != null ? textColor.color : null));
                    graphics.setFont(defaultFont);
                } else {
                    drawStringRect(graphics, desc.toString(), drawRect, (textColor != null ? textColor.color : null));
                }
            }
        }

        private StringBuffer getDescBuffer() {
            if (m_descBuffer == null) m_descBuffer = new StringBuffer(); else m_descBuffer.delete(0, m_descBuffer.length());
            return m_descBuffer;
        }

        protected void drawStringRect(java.awt.Graphics graphics, String string, Rectangle drawRect, Color color) {
            FontMetrics fm = graphics.getFontMetrics();
            Color oldColor = graphics.getColor();
            if (color != null) {
                graphics.setColor(color);
            }
            int length = string.length();
            for (int i = length; i >= 1; i--) {
                java.awt.geom.Rectangle2D rect2D = fm.getStringBounds(string, 0, i, graphics);
                int strWidth = rect2D.getBounds().width;
                if (strWidth < drawRect.width) {
                    graphics.drawString(string.substring(0, i), drawRect.x, drawRect.y + getCellTextBaseline());
                    break;
                }
            }
            graphics.setColor(oldColor);
        }

        public void reshape(int x, int y, int w, int h) {
            super.reshape(x, y, w, h);
            adjustScrollbarVisibleAmount();
        }

        public int getColWidth() {
            return m_colWidth;
        }

        public void adjustScrollbarVisibleAmount() {
            int horizPage = getWidth() / m_colWidth;
            m_horzScroll.setVisibleAmount(horizPage);
            m_horzScroll.setBlockIncrement(horizPage);
            int vertPage = getHeight() / m_rowHeight - ((getHeight() % m_rowHeight) > 0 ? 1 : 0);
            m_vertScroll.setVisibleAmount(vertPage);
            m_vertScroll.setBlockIncrement(vertPage);
        }

        public int getRowHeight() {
            if (m_rowHeight == -1) {
                Graphics g = getGraphics();
                FontMetrics fm = g.getFontMetrics(getFont());
                m_rowHeight = fm.getHeight() + 6;
                Dimension cbSize = m_checkbox.getPreferredSize();
                m_rowHeight = Math.max(m_rowHeight, cbSize.height + 2);
            }
            return m_rowHeight;
        }

        private int getCellTextBaseline() {
            if (m_baseline == -1) {
                Graphics g = getGraphics();
                FontMetrics fm = g.getFontMetrics(getFont());
                m_baseline = fm.getAscent() + 3;
            }
            return m_baseline;
        }

        public int getColCount() {
            return m_colCount;
        }

        public int getRowCount() {
            return m_rowCount;
        }

        public void setPrograms(Programs programs) {
            m_programs = programs;
            if (m_programs != null) {
                Channels channels = Programs.getChannels();
                if (channels != null) {
                    m_channelDescs = new Vector();
                    channels.getSortedChannelDescriptions(m_channelDescs);
                    int size = m_channelDescs.size();
                    if (m_config != null) {
                        for (int i = 0; i < size; i++) {
                            if (!m_config.isChannelMarked(m_channelDescs.get(i).toString())) {
                                m_channelDescs.remove(i--);
                                size--;
                            }
                        }
                    }
                    m_rowCount = m_channelDescs.size();
                    m_rowHeader.setHeadings(m_channelDescs.toArray());
                    m_vertScroll.setValues(0, 1, 0, m_rowCount - 1);
                }
                setNow();
            } else m_cellData = null;
            adjustScrollbarVisibleAmount();
        }

        public int setNow() {
            ProgramList progs = m_programs.getProgramsSortedByTime();
            Calendar now = Calendar.getInstance();
            int day = 0;
            if (progs != null && progs.getLength() > 0) {
                String strStart = m_programs.getStartTime(progs.item(0));
                Calendar start = Utilities.makeCal(strStart);
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                long noOfDays = 0;
                long dateTime1 = now.getTime().getTime();
                long dateTime2 = start.getTime().getTime();
                long diff = dateTime1 - dateTime2;
                noOfDays = diff / 86400000 + 1;
                day = (int) noOfDays;
                setDataForDay((int) noOfDays);
                m_horzScroll.setValue(now.get(Calendar.HOUR_OF_DAY) * 2);
                m_colHeader.repaint();
                m_grid.repaint();
            }
            return day;
        }

        /** Fill out the m_cellData array.
         *
         * @param day The index into the days in the current programs data
         */
        public void setDataForDay(int day) {
            if (m_programs != null) {
                Channels channels = Programs.getChannels();
                if (channels != null) {
                    m_cellData = new TVGridCellData[m_rowCount][m_colCount];
                    ProgramList progs = m_programs.getProgramsSortedByTime();
                    Iterator it = progs.iterator();
                    String curDate = "###";
                    int curDay = 0;
                    TVGridCellData cell;
                    while (it.hasNext()) {
                        ProgItem prog = (ProgItem) it.next();
                        String start = m_programs.getStartTime(prog);
                        if (start.indexOf(curDate) != 0) {
                            curDate = start.substring(0, 8);
                            curDay++;
                        }
                        if (curDay == day) {
                            int channelIndex = m_channelDescs.indexOf(Programs.getChannelDesc(m_programs.getChannel(prog)));
                            if (channelIndex != -1) {
                                int hour = Integer.valueOf(start.substring(8, 10)).intValue();
                                int min = Integer.valueOf(start.substring(10, 12)).intValue();
                                int hourIndex = hour * 2;
                                if (min >= 30) hourIndex++;
                                cell = new TVGridCellData();
                                cell.prog = prog;
                                String end = m_programs.getStopTime(prog);
                                int endHourIndex = 0;
                                try {
                                    int endhour = Integer.valueOf(end.substring(8, 10)).intValue();
                                    min = Integer.valueOf(end.substring(10, 12)).intValue();
                                    if (endhour < hour) endhour += 24;
                                    endHourIndex = endhour * 2;
                                    if (min >= 30) endHourIndex++;
                                } catch (Exception e) {
                                    endHourIndex = hourIndex + 1;
                                }
                                cell.colSpan = endHourIndex - hourIndex;
                                m_cellData[channelIndex][hourIndex] = cell;
                            }
                        }
                    }
                }
            }
        }

        protected void processMouseEvent(java.awt.event.MouseEvent mouseEvent) {
            if (SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.getID() == MouseEvent.MOUSE_RELEASED) {
                Point clickedAt = mouseEvent.getPoint();
                setSelectedCell(clickedAt);
            }
            super.processMouseEvent(mouseEvent);
        }

        public void setSelectedCell(Point point) {
            int horzScrollPos = m_horzScroll.getValue();
            int vertScrollPos = m_vertScroll.getValue();
            int rowClicked = (point.y / m_rowHeight) + vertScrollPos;
            int colClicked = (point.x / m_colWidth) + horzScrollPos;
            Rectangle cellRect = new Rectangle();
            int oldSelCol = m_selectedCol;
            int oldSelRow = m_selectedRow;
            ((TVGridPanel) getParent()).setSelectedCell(getCellData(rowClicked, colClicked));
            if (m_selectedCell != null) {
                m_selectedCol = m_selectedCell.actualCol;
                m_selectedRow = rowClicked;
                if (calcVisibleCellRect(m_selectedRow, m_selectedCol, cellRect) > 0) {
                    repaint(cellRect);
                }
                if (oldSelCol != -1) {
                    if (calcVisibleCellRect(oldSelRow, oldSelCol, cellRect) > 0) {
                        repaint(cellRect);
                    }
                }
            }
        }

        protected class TVGridCellData extends Object {

            ProgItem prog = null;

            int colSpan = 1;

            int actualCol = -1;

            public TVGridCellData() {
            }

            public TVGridCellData(TVGridCellData data) {
                prog = data.prog;
                colSpan = data.colSpan;
                actualCol = data.actualCol;
            }
        }
    }
}
