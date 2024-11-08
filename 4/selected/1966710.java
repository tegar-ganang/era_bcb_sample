package pnc.controler;

import pnc.client.*;
import pnc.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * The <tt>WorkersPane</tt> class displays a representation
 * of all the workers found in a federation.
 * It allows to display all the workers's state or location.
 * This class also allows to administrate the workers or
 * get remotely their LOG file
 *
 * @author pncTeam
 * @version $Revision: 1.3 $
 * @see WorkersPaneModel
 */
public class WorkersPane extends JPanel {

    /** The model used to manage the JTable */
    private WorkersPaneModel model = new WorkersPaneModel();

    /** The JTable used to display informations on the workers's activity */
    private JTable table = new JTable(model);

    /** The panel's scroller */
    private JScrollPane scroller = new JScrollPane();

    /** This popu menu allows to monitor a worker */
    private JPopupMenu popupMenu = new JPopupMenu();

    /** This popup menu's item allows to download a worker's log */
    private JMenuItem itemDownload = new JMenuItem();

    /** This popup menu's item allows to administrate a worker */
    private JMenuItem itemAdministration = new JMenuItem();

    /** The buffer used to transfer incoming and outgoing datas through the socket */
    protected byte[] buffer = new byte[BUFFER_SIZE];

    /** Constant of the buffer's size used by the socket */
    protected static final int BUFFER_SIZE = 100000;

    /**
   * Constructor
   */
    public WorkersPane() {
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
        popupMenu.add(itemDownload);
        popupMenu.add(itemAdministration);
        scroller.setViewportView(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setModel(model);
        itemDownload.setText("Download the LOG file");
        itemAdministration.setText("Administer this worker");
        table.getColumnModel().getColumn(2).setCellRenderer(new ProgressTableCellRenderer());
        table.addMouseListener(new MouseHandler());
        itemDownload.addActionListener(new DownloadActionHandler());
        itemAdministration.addActionListener(new ItemAdministrationHandler());
    }

    /**
   * The <tt>getModel</tt> method returns the model
   * used by the JTable of this class
   *
   * @return the data model associated to this class's JTable
   */
    public WorkersPaneModel getModel() {
        return model;
    }

    /** 
   * This method allows to display a popup menu where an user's right clic occured
   *
   * @param x an integer representing the X constant of the clic
   * @param y an integer representing the Y constant of the clic
   */
    protected void showPopupMenu(int x, int y) {
        int row = table.rowAtPoint(new Point(x, y));
        table.getSelectionModel().setSelectionInterval(row, row);
        this.popupMenu.show(this, x, y);
    }

    /**
   * The <tt>ProgressTableCellRenderer</tt> allows to manage JProgressBar objects in
   * order to graphically display a worker's progression inside the JTable
   */
    private class ProgressTableCellRenderer extends JProgressBar implements TableCellRenderer {

        /**
     * Constructor
     */
        public ProgressTableCellRenderer() {
            super();
            setStringPainted(true);
        }

        /**
     * The <code>getTableCellRendererComponent</code> method if the the data contained
     * in this cell is an integer and refresh the progress bar consequently
     *
     * @see TableCellRenderer
     */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Integer) {
                setValue(((Integer) value).intValue());
            }
            return this;
        }
    }

    /**
   * The <tt>MouseHandler</tt> class is an event listener managing 
   * the user's clics on the JTable
   */
    private class MouseHandler extends MouseAdapter {

        /**
     * The <code>mousePressed</code> is activated when a clic on the JTable occurs.
     * If it is a right clic, then the popup menu allowing to administrate the worker
     * represented by this JTable's row is displayed
     *
     * @param evt a <code>MouseEvent</code> describing a clic event
     */
        public void mousePressed(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                showPopupMenu(evt.getX(), evt.getY());
            }
        }
    }

    /**
   * The <tt>DownloadActionHandler</tt> is an event listener managing the user's 
   * clic on the "LOG Download" button
   */
    private class DownloadActionHandler implements ActionListener {

        /** 
     * The <code>parseXml</code> method parses the content of a 
     * downloaded XML file in order to print useful informations
     *
     * @param f the fls we'd like to parse
     * @param name The worker's IP Address (only used to prin the worker's name)
     */
        public void parseXml(File f, String name) {
            XMLParser parser = new XMLParser(name);
            parser.parse(f);
        }

        /**
     * The <code>actionPerformed</code> method is activated when a clic on
     * the "Download LOG" button occurs (PopupMenu)
     *
     * @param evt the <code>ActionEvent</code> describing the clic event
     */
        public void actionPerformed(ActionEvent evt) {
            int index = table.getSelectedRow();
            if (index != -1) {
                WorkerServiceInterface proxy = model.getProxyAt(index);
                String ipProxy = model.getProxyIpAt(index);
                proxy.getLastLog();
                try {
                    Socket socket = new Socket(ipProxy, 2000);
                    BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
                    File tempFile = new File("/tmp/worker-temp.xml");
                    FileOutputStream fileStream = new FileOutputStream(tempFile);
                    int read = 0;
                    while (read != -1) {
                        read = input.read(buffer, 0, BUFFER_SIZE);
                        if (read != -1) {
                            fileStream.write(buffer, 0, read);
                            fileStream.flush();
                        }
                    }
                    if (read != 0) {
                        fileStream.write(new String("</log>\n").getBytes());
                    }
                    input.close();
                    fileStream.close();
                    socket.close();
                    parseXml(tempFile, "worker " + ipProxy);
                    tempFile.delete();
                } catch (SocketException ex) {
                    System.out.println("Unable to contact this worker! " + "Is this a version recent enough? Network Problem?");
                } catch (IOException ex) {
                    System.out.println("Socket Error with this worker!");
                }
            }
        }
    }

    /**
   * The <tt>ItemAdministrationHandler</tt> class is an event listener managing
   * the user's clic on the "Administrate this worker" button
   */
    private class ItemAdministrationHandler implements ActionListener {

        /**
     * The <code>actionPerformed</code> method is activated whenever a clic occurs
     * on the "Administration" popup menu's item
     * 
     * @param evt the <code>ActionEvent</code> describing the clic event
     */
        public void actionPerformed(ActionEvent evt) {
            int index = table.getSelectedRow();
            if (index != -1) {
                WorkerServiceInterface proxy = model.getProxyAt(index);
                AdministrationUI adminUI = new AdministrationUI(proxy);
                adminUI.show();
            }
        }
    }
}
