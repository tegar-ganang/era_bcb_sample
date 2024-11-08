package pnc.controler;

import pnc.client.*;
import pnc.util.*;
import net.jini.core.lookup.*;
import net.jini.lookup.*;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.core.discovery.LookupLocator;
import java.rmi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.logging.*;

/**
 * The <tt>Controler</tt> class allows to observe and
 * control differents services such as the JINI  
 * JavaSpaces or the PNC federation's workers
 *
 * @author pncTeam
 * @version $Revision: 1.6 $
 */
public class Controler extends JFrame {

    /** The ServiceDiscoveryManager used to find the services */
    protected ServiceDiscoveryManager sdm;

    /** The template used to find WorkerServiceInterface into the Reggie */
    protected ServiceTemplate template;

    /** The LookupCache used to manage the WorkerServiceInterface discovery */
    protected LookupCache serviceCache;

    /** This is a Swing componant used to display different tabs */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** The Workers's activity panel */
    private WorkersPane workerActivity = new WorkersPane();

    /** The JavaSpaces's activity panel */
    private JavaSpacesPane javaspaceActivity = new JavaSpacesPane();

    /** This object allows us to create XML log files */
    private Logger log;

    /** This is a listener used to notify the SDM whenever a service is discovered */
    private ServiceDiscoveryListener serviceDiscoveryListener = new ServiceDiscoveryHandler();

    /** A JMenuBar object */
    private JMenuBar menuBar = new JMenuBar();

    /** The "File" menu */
    private JMenu menu = new JMenu("File");

    /** This menu item allows to display the log file */
    private JMenuItem logDownload = new JMenuItem("Display the Controler's LOG");

    /** The buffer used to copy datas from file to file */
    protected byte[] buffer = new byte[BUFFER_SIZE];

    /** Constant of the buffer's size used by the file copy */
    protected static final int BUFFER_SIZE = 100000;

    /**
   * Constructor
   */
    public Controler() {
        super("PNC: Federation's Control Application");
        log = Logging.getLogger("controler.xml", "Controler");
        tabbedPane.add("Workers", workerActivity);
        tabbedPane.add("JavaSpaces", javaspaceActivity);
        Container container = this.getContentPane();
        setJMenuBar(menuBar);
        menuBar.add(menu);
        menu.add(logDownload);
        container.setLayout(new BorderLayout());
        container.add(tabbedPane, BorderLayout.CENTER);
        setSize(500, 300);
        logDownload.addActionListener(new MenuActionHandler());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
   * Main execution method
   *
   * @param args an array containing the command line arguments
   */
    public static void main(String[] args) {
        String[] groupList = null;
        LookupLocator[] locatorList = null;
        if (args != null && args.length != 0) {
            ParsedArgs parsedArgs = CommandLineParser.parseStringList(args);
            groupList = parsedArgs.getGroupList();
            locatorList = parsedArgs.getLookupLocatorList();
        }
        if (System.getSecurityManager() == null) System.setSecurityManager(new RMISecurityManager());
        Controler gui = new Controler();
        gui.setGroupListAndLocatorList(groupList, locatorList);
        gui.show();
    }

    /**
   * The <tt>setGroupListAndLocatorList</tt> method allows to configure the groups and
   * the jini federations's URL that will be used by the Controler
   *
   * @param groupList the groups list in a String array
   * @param locatorList the list of all the jini URL's used (jini://host:port).
   */
    public void setGroupListAndLocatorList(String[] groupList, LookupLocator[] locatorList) {
        try {
            if (sdm != null) {
                sdm.terminate();
            }
            LookupDiscoveryManager disco = new LookupDiscoveryManager(groupList, locatorList, null);
            sdm = new ServiceDiscoveryManager(disco, null);
            javaspaceActivity.setServiceDiscoveryManager(sdm);
            Class[] name = new Class[] { WorkerServiceInterface.class };
            template = new ServiceTemplate(null, name, null);
            serviceCache = sdm.createLookupCache(template, null, serviceDiscoveryListener);
            log.logp(Level.INFO, "Controler", "setGroupListAndLocator()", "The service cache has been successfully set up. Contoler's now running");
        } catch (Exception ex) {
            log.logp(Level.WARNING, "Controler", "setGroupListAndLocator()", "An error occured during the creation of the LookupDiscoveryManager, " + "the ServiceDiscoveryManager or the service cache", ex);
        }
    }

    /**
   * The <tt>ServiceDiscoveryHandler</tt> is used to react whenever an event generated
   * by the du ServiceDiscoveryManager lookup cache has been caught
   */
    private class ServiceDiscoveryHandler implements ServiceDiscoveryListener {

        /**
     * The <tt>serviceAdded</tt> is called whenever a new service corresponding to 
     * the WorkerServiceInterface template has been detected
     *
     * @param ev the event thrown by the service lookup cache
     */
        public void serviceAdded(ServiceDiscoveryEvent ev) {
            log.logp(Level.INFO, "Controler", "serviceAdded()", "A new service has been detected. " + "ServiceID: " + ev.getPostEventServiceItem().serviceID);
            ServiceItem item = ev.getPostEventServiceItem();
            workerActivity.getModel().addService(item);
        }

        /**
     * The <tt>serviceRemoved</tt> method is called whenever a service corresponding to 
     * the WorkerServiceInterface template has been destroyed
     *
     * @param ev the event thrown by the service lookup cache
     */
        public void serviceRemoved(ServiceDiscoveryEvent ev) {
            log.logp(Level.INFO, "Controler", "serviceRemoved()", "The worker correponding to the" + " ServiceID: " + ev.getPreEventServiceItem().serviceID + " has been destoyed");
            ServiceItem item = ev.getPreEventServiceItem();
            workerActivity.getModel().removeService(item);
        }

        /**
     * The <tt>serviceChanged</tt> method is called whenever a service corresponding to 
     * the WorkerServiceInterface template has been modified 
     *
     * @param ev the event thrown by the service lookup cache
     */
        public void serviceChanged(ServiceDiscoveryEvent ev) {
            log.logp(Level.INFO, "Controler", "serviceChanged()", "The worker correponding to the" + " ServiceID: " + ev.getPreEventServiceItem().serviceID + " has been altered");
            ServiceItem item = ev.getPostEventServiceItem();
            workerActivity.getModel().changeService(item);
        }
    }

    /**
   * The <tt>MenuActionHandler</tt> is used to manage
   * the differents user's clics on a JMenu object
   */
    private class MenuActionHandler implements ActionListener {

        /**
     * The <code>actionPerformed</code> method is activated when
     * a user's clic occurs on a menu item
     *
     * @param evt the <code>ActionEvent</code> describing the clic event
     */
        public void actionPerformed(ActionEvent evt) {
            try {
                File tempFile = new File("/tmp/controler.xml");
                File f = new File("/tmp/controler-temp.xml");
                BufferedInputStream copySource = new BufferedInputStream(new FileInputStream(tempFile));
                BufferedOutputStream copyDestination = new BufferedOutputStream(new FileOutputStream(f));
                int read = 0;
                while (read != -1) {
                    read = copySource.read(buffer, 0, BUFFER_SIZE);
                    if (read != -1) {
                        copyDestination.write(buffer, 0, read);
                    }
                }
                copyDestination.write(new String("</log>\n").getBytes());
                copySource.close();
                copyDestination.close();
                XMLParser parser = new XMLParser("Controler");
                parser.parse(f);
                f.delete();
            } catch (IOException ex) {
                System.out.println("An error occured during the file copy!");
            }
        }
    }
}
