package informaclient;

import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.awt.*;
import javax.swing.*;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeSelectionModel;

/**
 * <p>Title: InformaClient</p>
 * <p>Description: RSS Client based on Informa library</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Pito Salas
 * @version 1.0
 */
public class HomeFrame extends JFrame {

    static ResourceBundle res = ResourceBundle.getBundle("informaclient.Res");

    private TreeSelectionModel channelTreeSelModel = new DefaultTreeSelectionModel();

    private JMenuItem ChannelsCommand = new JMenuItem();

    private JMenu jMenuHelp = new JMenu();

    private JMenuItem jMenuHelpAbout = new JMenuItem();

    private JMenuBar homeMenuBar = new JMenuBar();

    private JMenu jMenuUtils = new JMenu();

    private JMenuItem InitCommand = new JMenuItem();

    private JMenuItem jMenuFileExit = new JMenuItem();

    private JMenuItem loggerInformationCommand = new JMenuItem();

    private JMenu jMenuFile = new JMenu();

    private JCheckBoxMenuItem loggingWindow = new JCheckBoxMenuItem();

    private BorderLayout borderLayout1 = new BorderLayout();

    private JFrame logFrame;

    public GlobalController manager;

    private JMenuItem packItem = new JMenuItem();

    private JMenuItem validateItem = new JMenuItem();

    private JMenuItem dumpChannelsAndItems = new JMenuItem();

    public Logger log;

    private JMenuItem dumpWindowDimensions = new JMenuItem();

    private JSplitPane jSplitPane1 = new JSplitPane();

    public JTree channelTree = new JTree();

    private BorderLayout borderLayout2 = new BorderLayout();

    public JPanel channelStampView = new JPanel();

    public JScrollPane treeScrollPanel;

    private JPanel leftSide;

    private BorderLayout borderLayout4 = new BorderLayout();

    private JPanel rightSide;

    private JList channelItemList = new JList();

    public ItemDetailView itemDetailView;

    public HomeFrame(GlobalController theModel) {
        manager = theModel;
        channelStampView = new ChannelStampView(manager);
        manager.setChannelStampView((ChannelStampView) channelStampView);
        itemDetailView = new ItemDetailView();
        itemDetailView.setUserObject(theModel);
        theModel.setItemDetailView(itemDetailView);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        manager.setChannelTree(channelTree);
        channelItemList.setModel(manager.getChannelItemListModel());
        log = Logger.getLogger(this.getClass().getName());
    }

    private void jbInit() throws Exception {
        this.setForeground(Color.white);
        this.setSize(new Dimension(740, 420));
        this.setTitle("Informa Client");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addComponentListener(new HomeFrameComponentListener());
        leftSide = new JPanel();
        leftSide.setMinimumSize(new Dimension(50, 110));
        channelTree.setPreferredSize(new Dimension(200, 300));
        channelTree.setForeground(Color.black);
        channelTree.setMinimumSize(new Dimension(0, 0));
        channelTree.setOpaque(false);
        channelTree.setRequestFocusEnabled(false);
        channelTree.setToolTipText("Click on channels to select");
        channelTree.setSelectionModel(channelTreeSelModel);
        channelTree.setModel(null);
        channelTreeSelModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        treeScrollPanel = new JScrollPane(channelTree);
        treeScrollPanel.setAlignmentX((float) 0.5);
        treeScrollPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treeScrollPanel.setPreferredSize(new Dimension(240, 400));
        treeScrollPanel.getViewport().setBackground(Color.white);
        treeScrollPanel.setForeground(Color.gray);
        treeScrollPanel.setMinimumSize(new Dimension(0, 100));
        leftSide.setLayout(borderLayout2);
        leftSide.add(treeScrollPanel, BorderLayout.CENTER);
        leftSide.add(channelStampView, BorderLayout.SOUTH);
        rightSide = new JPanel();
        rightSide.setLayout(borderLayout4);
        channelItemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        channelItemList.addListSelectionListener(new ItemSelectionListener());
        rightSide.add(itemDetailView, BorderLayout.CENTER);
        rightSide.add(channelItemList, BorderLayout.NORTH);
        jSplitPane1.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        jSplitPane1.setLeftComponent(leftSide);
        jSplitPane1.setRightComponent(rightSide);
        this.setContentPane(jSplitPane1);
        jMenuHelp.add(jMenuHelpAbout);
        homeMenuBar.add(jMenuFile);
        homeMenuBar.add(jMenuUtils);
        homeMenuBar.add(jMenuHelp);
        this.setJMenuBar(homeMenuBar);
        jMenuUtils.add(ChannelsCommand);
        jMenuUtils.add(InitCommand);
        jMenuUtils.add(loggerInformationCommand);
        jMenuUtils.add(loggingWindow);
        jMenuUtils.add(packItem);
        jMenuUtils.add(validateItem);
        jMenuUtils.add(dumpChannelsAndItems);
        jMenuUtils.add(dumpWindowDimensions);
        jMenuFile.add(jMenuFileExit);
        ChannelsCommand.setText(res.getString("Channels"));
        jMenuHelp.setText(res.getString("Help"));
        jMenuHelpAbout.setText(res.getString("About"));
        jMenuHelpAbout.addActionListener(new AboutBoxCommand(this));
        jMenuUtils.setText(res.getString("Utils"));
        InitCommand.setText(res.getString("Initialize_Informa"));
        InitCommand.addActionListener(new InitCommand(this));
        dumpWindowDimensions.setText("Dump Window Dimensions");
        dumpWindowDimensions.addActionListener(new DumpWindowsCommand(this));
        dumpChannelsAndItems.setText("Dump Channels and Items");
        dumpChannelsAndItems.addActionListener(new DumpChannelsAndItems(this));
        jMenuFileExit.setText(res.getString("Exit"));
        jMenuFileExit.addActionListener(new ExitCommand(this));
        loggerInformationCommand.setText(res.getString("Logger_Information"));
        loggerInformationCommand.addActionListener(new LoggerInformation(this));
        jMenuFile.setText(res.getString("File"));
        loggingWindow.setText("Logging Window");
        loggingWindow.addActionListener(new LogFrameCommand(this));
        packItem.setText("Pack Main Window");
        packItem.addActionListener(new PackWindowCommand(this));
        validateItem.setText("Validate Main Window");
        validateItem.addActionListener(new ValidateCommand(this));
    }
}
