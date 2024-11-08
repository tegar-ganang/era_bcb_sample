package net.sf.xtvdclient.gui;

import net.sf.xtvdclient.xtvd.DataDirectException;
import net.sf.xtvdclient.xtvd.datatypes.Crew;
import net.sf.xtvdclient.xtvd.datatypes.CrewMember;
import net.sf.xtvdclient.xtvd.datatypes.Genre;
import net.sf.xtvdclient.xtvd.datatypes.Lineup;
import net.sf.xtvdclient.xtvd.datatypes.Map;
import net.sf.xtvdclient.xtvd.datatypes.MovieAdvisories;
import net.sf.xtvdclient.xtvd.datatypes.Program;
import net.sf.xtvdclient.xtvd.datatypes.ProgramGenre;
import net.sf.xtvdclient.xtvd.datatypes.Schedule;
import net.sf.xtvdclient.xtvd.datatypes.Station;
import net.sf.xtvdclient.xtvd.datatypes.Xtvd;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ProgressMonitor;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

/**
 * A class that builds a <code>JInternalFrame</code> that displays
 * a <code>XTVD XML document</code> as a <code>tree</code>.
 * <p/>
 * <p><b>Note:</b> This class uses a <code>JTree</code> to display
 * the <code>XTVD XML document</code>.  It is not recommended that you
 * use this class to display large documents.</p>
 *
 * @author Rakesh Vidyadharan 9<sup><small>th</small></sup> March, 2004
 *         <p/>
 *         <p>Copyright 2004, Tribune Media Services</p>
 *         <p/>
 *         $Id: XTVDTreeViewer.java,v 1.4 2004/04/01 19:33:37 rakesh Exp $
 */
public class XTVDTreeViewer extends JInternalFrame {

    /**
   * A text-area that is used to display log/progress messages.
   */
    private JTextArea logArea = null;

    /**
   * The desktop for the parent frame to which this frame is to be
   * added.
   */
    private JDesktopPane desktop = null;

    /**
   * The <code>root</code> element to use for the tree.
   */
    private DefaultMutableTreeNode root;

    /**
   * The object representation of the XML data.
   */
    private Xtvd xtvd;

    /**
   * The formatter used display date-time values in the {@link
   * #logArea}.
   */
    private SimpleDateFormat sdf;

    /**
   * The only constructor method supported.  Create a new
   * <code>JInternalFrame</code> instance that is <code>resizable,
   * closable, maximisable,</code> and <code>iconifiable</code>.
   * Create the tree from the {@link #xtvd} object, and display it.
   *
   * @param xtvd The object representation of the XTVD document that is used to build the tree.
   * @param desktop The desktop onto which the internal frame is to be added.
   * @param logArea The text area to which progress/log messages are to be written.
   */
    public XTVDTreeViewer(Xtvd xtvd, JDesktopPane desktop, JTextArea logArea) {
        super("XTVD from: " + xtvd.getFrom().toString() + " to: " + xtvd.getTo().toString(), true, true, true, true);
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        setXtvd(xtvd);
        setDesktop(desktop);
        setLogArea(logArea);
    }

    /**
   * Build all the nodes in the tree and display it.  Add
   * the lineups and stations top-level
   * children to the {@link #root} of the tree.
   *
   * @see #createProgramNodes(DefaultMutableTreeNode)
   */
    public void buildTree() {
        logArea.append(sdf.format(new Date()));
        logArea.append("\tStart building tree.");
        logArea.append(XTVDClient.END_OF_LINE);
        root = new DefaultMutableTreeNode("xtvd");
        createProgramNodes(root);
        logArea.append(sdf.format(new Date()));
        logArea.append("\tFinished building tree.");
        logArea.append(XTVDClient.END_OF_LINE);
    }

    /**
   * Create a <code>JTree</code> with the {@link #root} node, and
   * display it within the frame.
   *
   * @see DisplayThread
   */
    public void displayTree() {
        logArea.append(sdf.format(new Date()));
        logArea.append("\tDisplaying tree.");
        logArea.append(XTVDClient.END_OF_LINE);
        JTree tree = new JTree(root);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        getContentPane().add(scrollPane);
        javax.swing.SwingUtilities.invokeLater(new DisplayThread());
    }

    /**
   * Add all the {@link net.sf.xtvdclient.xtvd.datatypes.Xtvd#programs}
   * in the {@link #xtvd} document to the specified parent node.
   *
   * @param parent The parent node to which the stations nodes are to be added.
   * @see #createScheduleNode(Schedule, DefaultMutableTreeNode)
   * @see #createCrewNodes(Program, DefaultMutableTreeNode)
   * @see #createGenreNodes(Program, DefaultMutableTreeNode)
   */
    private void createProgramNodes(DefaultMutableTreeNode parent) {
        logArea.append(sdf.format(new Date()));
        logArea.append("\tCreating program nodes");
        logArea.append(XTVDClient.END_OF_LINE);
        int counter = 0;
        int total = xtvd.getPrograms().size();
        ProgressMonitor progressMonitor = new ProgressMonitor(this, "Building Program nodes.", "", counter, total);
        String previousTitle = "";
        DefaultMutableTreeNode programNode = null;
        for (Iterator programs = xtvd.getPrograms().keySet().iterator(); programs.hasNext(); ) {
            if (progressMonitor.isCanceled()) {
                logArea.append(sdf.format(new Date()));
                logArea.append("\tTree building canceled.  Incomplete tree being displayed.");
                logArea.append(XTVDClient.END_OF_LINE);
                break;
            } else {
                Program program = (Program) xtvd.getPrograms().get(programs.next());
                if (!previousTitle.equals(program.getTitle())) {
                    if (programNode != null) {
                        parent.add(programNode);
                    }
                    programNode = new DefaultMutableTreeNode(program.getTitle());
                }
                DefaultMutableTreeNode idNode = new DefaultMutableTreeNode(program.getId());
                for (Schedule schedule : xtvd.getSchedules()) {
                    if (program.getId().equals(schedule.getProgram())) {
                        createScheduleNode(schedule, idNode);
                    }
                }
                createAdvisoriesNode(program, idNode);
                createCrewNodes(program, idNode);
                createGenreNodes(program, idNode);
                if (programNode != null) {
                    programNode.add(idNode);
                }
                previousTitle = program.getTitle();
                progressMonitor.setProgress(++counter);
            }
        }
        if (programNode != null) {
            parent.add(programNode);
        }
        progressMonitor.close();
    }

    /**
   * Create a schedule node for the specified schedule
   * record and add it to the specified parent.
   *
   * @param schedule The schedule for which the schedule node is to be created.
   * @param parent The parent node to which the schedule node is to be added.
   */
    private void createScheduleNode(Schedule schedule, DefaultMutableTreeNode parent) {
        StringBuffer buffer = new StringBuffer(32);
        buffer.append("Schedule time: ");
        buffer.append(schedule.getTime().getLocalDate());
        buffer.append(", duration: ");
        buffer.append(schedule.getDuration().getHours());
        buffer.append(":");
        buffer.append(schedule.getDuration().getMinutes());
        DefaultMutableTreeNode scheduleNode = new DefaultMutableTreeNode(buffer);
        createStationNode(schedule.getStation(), scheduleNode);
        parent.add(scheduleNode);
    }

    /**
   * Create a node for the specified station, and add it to the
   * specified parent node.
   *
   * @param stationId The stationId of the station for which a node is to be added.
   * @param parent The parent node to which the new station node is to be added
   */
    private void createStationNode(int stationId, DefaultMutableTreeNode parent) {
        Station station = (Station) xtvd.getStations().get(stationId);
        DefaultMutableTreeNode stationNode = new DefaultMutableTreeNode(station.getCallSign());
        createLineupNode(stationId, stationNode);
        parent.add(stationNode);
    }

    /**
   * Create a node for each lineup that features the specified
   * station, and add it to the specified parent node.
   *
   * @param stationId The stationId of the station for which the lineup nodes are to be created.
   * @param parent The parent node to which the new lineup node(s) are to be added
   */
    private void createLineupNode(int stationId, DefaultMutableTreeNode parent) {
        for (Iterator lineups = xtvd.getLineups().entrySet().iterator(); lineups.hasNext(); ) {
            Lineup lineup = (Lineup) ((java.util.Map.Entry) lineups.next()).getValue();
            for (Map map : lineup.getMaps()) {
                if (map.getStation() == stationId) {
                    StringBuffer buffer = new StringBuffer(32);
                    buffer.append("Lineup: ").append(lineup.getName());
                    buffer.append(", type: ").append(lineup.getType().toString());
                    buffer.append(", channel: ").append(map.getChannel());
                    parent.add(new DefaultMutableTreeNode(buffer));
                }
            }
        }
    }

    /**
   * Create the sub-tree that will hold all the
   * advisory messages associated with the specified
   * program.
   *
   * @param program The program whose crew are to be added as nodes.
   * @param parent The parent node to which the genres are to be added.
   */
    private void createAdvisoriesNode(Program program, DefaultMutableTreeNode parent) {
        if (!program.getAdvisories().isEmpty()) {
            DefaultMutableTreeNode advisories = new DefaultMutableTreeNode("advisories");
            for (MovieAdvisories advisor : program.getAdvisories()) {
                advisories.add(new DefaultMutableTreeNode(advisor.toString()));
            }
            parent.add(advisories);
        }
    }

    /**
   * Create node(s) for all the <code>crew</code> that are
   * associated with the specified program.
   *
   * @param program The program whose crew are to be added as nodes.
   * @param parent The parent node to which the genres are to be added.
   */
    private void createCrewNodes(Program program, DefaultMutableTreeNode parent) {
        Crew crew = xtvd.getProductionCrew().get(program.getId());
        if (crew != null) {
            DefaultMutableTreeNode crewNode = new DefaultMutableTreeNode("productionCrew");
            for (CrewMember member : crew.getMember()) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(member.getGivenname());
                buffer.append(" ").append(member.getSurname());
                buffer.append(" - ").append(member.getRole());
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(buffer);
                crewNode.add(node);
            }
            parent.add(crewNode);
        }
    }

    /**
   * Create node(s) for all the <code>genres</code> that are
   * associated with the specified program.
   *
   * @param program The program whose genres are to be added as nodes.
   * @param parent The parent node to which the genres are to be added.
   */
    private void createGenreNodes(Program program, DefaultMutableTreeNode parent) {
        ProgramGenre programGenre = (ProgramGenre) xtvd.getGenres().get(program.getId());
        if (programGenre != null) {
            DefaultMutableTreeNode genresNode = new DefaultMutableTreeNode("genres");
            for (Genre genre : programGenre.getGenres()) {
                genresNode.add(new DefaultMutableTreeNode(genre.getClassValue()));
            }
            parent.add(genresNode);
        }
    }

    /**
   * An inner class <code>Thread</code> that is used to display the
   * frame in a safe manner.
   */
    class DisplayThread extends Thread {

        /**
     * Display this frame, and bring it to the front.
     */
        public void run() {
            pack();
            setVisible(true);
            desktop.add(XTVDTreeViewer.this);
            try {
                setSelected(true);
            } catch (java.beans.PropertyVetoException pvex) {
                logArea.append(sdf.format(new Date()));
                logArea.append("\tpropertyVetoException while bringing XTVDTreeViewer to front.");
                logArea.append(XTVDClient.END_OF_LINE);
                logArea.append(DataDirectException.getStackTraceString(pvex));
                logArea.append(XTVDClient.END_OF_LINE);
            }
        }
    }

    /**
   * Returns {@link #logArea}.
   *
   * @return JTextArea - The value/reference of/to logArea.
   */
    public final JTextArea getLogArea() {
        return logArea;
    }

    /**
   * Set {@link #logArea}.
   *
   * @param logArea The value to set.
   */
    public final void setLogArea(JTextArea logArea) {
        this.logArea = logArea;
    }

    /**
   * Returns {@link #xtvd}.
   *
   * @return Xtvd - The value/reference of/to xtvd.
   */
    public final Xtvd getXtvd() {
        return xtvd;
    }

    /**
   * Set {@link #xtvd}.
   *
   * @param xtvd The value to set.
   */
    public final void setXtvd(Xtvd xtvd) {
        this.xtvd = xtvd;
    }

    /**
   * Returns {@link #desktop}.
   *
   * @return JDesktopPane - The value/reference of/to desktop.
   */
    public final JDesktopPane getDesktop() {
        return desktop;
    }

    /**
   * Set {@link #desktop}.
   *
   * @param desktop The value to set.
   */
    public final void setDesktop(JDesktopPane desktop) {
        this.desktop = desktop;
    }
}
