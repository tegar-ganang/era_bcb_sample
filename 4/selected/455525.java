package src.packin;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class MainFrame extends javax.swing.JFrame {

    /** Creates new form MainFrame */
    public MainFrame() {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        initComponents();
    }

    private void initComponents() {
        TabPane = new javax.swing.JTabbedPane();
        StartTab = new javax.swing.JPanel();
        StartLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        IntroText = new javax.swing.JTextPane();
        StartButton = new javax.swing.JButton();
        GeneralTab = new javax.swing.JPanel();
        PublisherPanel = new javax.swing.JPanel();
        NameLabel = new javax.swing.JLabel();
        EmailLabel = new javax.swing.JLabel();
        PublisherTextBox = new javax.swing.JTextField();
        EmailTextBox = new javax.swing.JTextField();
        SofwarePanel = new javax.swing.JPanel();
        PackageNameLabel = new javax.swing.JLabel();
        PackageTextBox = new javax.swing.JTextField();
        VersionLabel = new javax.swing.JLabel();
        VersionTextBox = new javax.swing.JTextField();
        OverviewLabel = new javax.swing.JLabel();
        OverviewTextBox = new javax.swing.JTextField();
        AreaScroll = new javax.swing.JScrollPane();
        DescriptionArea = new javax.swing.JTextArea();
        DescriptionLabel = new javax.swing.JLabel();
        ComboBox = new javax.swing.JComboBox();
        NextButton = new javax.swing.JButton();
        ParameterTab = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        FilesLabel = new javax.swing.JLabel();
        FilesTextBox = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        HelpArea = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        DependsLabel = new javax.swing.JLabel();
        PreDebpendsLabel = new javax.swing.JLabel();
        RecommendsLabel = new javax.swing.JLabel();
        ConflictsLabel = new javax.swing.JLabel();
        DependsTextBox = new javax.swing.JTextField();
        ReplacesLabel = new javax.swing.JLabel();
        RecommendsTextBox = new javax.swing.JTextField();
        PreDependsTextBox = new javax.swing.JTextField();
        ConflictsTextBox = new javax.swing.JTextField();
        ReplacesTextBox = new javax.swing.JTextField();
        NextButton2 = new javax.swing.JButton();
        BackButton2 = new javax.swing.JButton();
        BuildTab = new javax.swing.JPanel();
        BuildButton = new javax.swing.JButton();
        BackButton3 = new javax.swing.JButton();
        BuildLabel = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        BuildArea = new javax.swing.JTextArea();
        ProgressBar = new javax.swing.JProgressBar();
        jScrollPane4 = new javax.swing.JScrollPane();
        StatusArea = new javax.swing.JTextArea();
        MenuBar = new javax.swing.JMenuBar();
        FileMenu = new javax.swing.JMenu();
        FileExitItem = new javax.swing.JMenuItem();
        EditMenu = new javax.swing.JMenu();
        BuildMenu = new javax.swing.JMenu();
        BuildMenuItem = new javax.swing.JMenuItem();
        HelpMenu = new javax.swing.JMenu();
        VisitPackinMenuItem = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Packin Linux Debian Package Creator");
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });
        StartLabel.setBackground(new java.awt.Color(200, 200, 200));
        StartLabel.setFont(new java.awt.Font("Dialog", 1, 24));
        StartLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        StartLabel.setText("Packin Creator");
        StartLabel.setOpaque(true);
        StartLabel.getAccessibleContext().setAccessibleName("StartLabel");
        jScrollPane1.setEnabled(false);
        IntroText.setBackground(java.awt.SystemColor.window);
        IntroText.setEditable(false);
        IntroText.setText("Welcome to Packin Linux Package Creator! This tool enables you to create packages in the popular .deb format. Other package format support will likely be added in the future. \n\nIn order to start creating a package, please click the \"Next\" button below.\n\nNote: All fields marked with a (*) are required in order to conform to the deb package specifications.");
        IntroText.setSelectionColor(new java.awt.Color(255, 255, 255));
        jScrollPane1.setViewportView(IntroText);
        StartButton.setText(" Next ->");
        StartButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton1MouseClicked(evt);
                StartButtonMouseClicked(evt);
            }
        });
        javax.swing.GroupLayout StartTabLayout = new javax.swing.GroupLayout(StartTab);
        StartTab.setLayout(StartTabLayout);
        StartTabLayout.setHorizontalGroup(StartTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(StartTabLayout.createSequentialGroup().addContainerGap(465, Short.MAX_VALUE).addComponent(StartButton).addContainerGap()).addComponent(StartLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 565, Short.MAX_VALUE).addGroup(StartTabLayout.createSequentialGroup().addGap(135, 135, 135).addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(130, Short.MAX_VALUE)));
        StartTabLayout.setVerticalGroup(StartTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(StartTabLayout.createSequentialGroup().addGap(60, 60, 60).addComponent(StartLabel).addGap(18, 18, 18).addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 16, Short.MAX_VALUE).addComponent(StartButton).addContainerGap()));
        TabPane.addTab("Start", StartTab);
        PublisherPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Publisher Information"));
        NameLabel.setText("Publisher Name:");
        EmailLabel.setText("Publisher Email:");
        javax.swing.GroupLayout PublisherPanelLayout = new javax.swing.GroupLayout(PublisherPanel);
        PublisherPanel.setLayout(PublisherPanelLayout);
        PublisherPanelLayout.setHorizontalGroup(PublisherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(PublisherPanelLayout.createSequentialGroup().addContainerGap().addGroup(PublisherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(NameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(EmailLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(PublisherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(PublisherTextBox).addComponent(EmailTextBox, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)).addContainerGap(71, Short.MAX_VALUE)));
        PublisherPanelLayout.setVerticalGroup(PublisherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(PublisherPanelLayout.createSequentialGroup().addContainerGap().addGroup(PublisherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(NameLabel).addComponent(PublisherTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(PublisherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(EmailLabel, javax.swing.GroupLayout.Alignment.TRAILING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PublisherPanelLayout.createSequentialGroup().addComponent(EmailTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(1, 1, 1))).addContainerGap(58, Short.MAX_VALUE)));
        SofwarePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Software Information"));
        PackageNameLabel.setText("Package Name(*):");
        PackageTextBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PackageTextBoxActionPerformed(evt);
            }
        });
        VersionLabel.setText("Version(*):");
        OverviewLabel.setText("Overview:");
        DescriptionArea.setColumns(20);
        DescriptionArea.setRows(5);
        AreaScroll.setViewportView(DescriptionArea);
        DescriptionLabel.setText("Description(*):");
        ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "i386", "amd64", "all" }));
        javax.swing.GroupLayout SofwarePanelLayout = new javax.swing.GroupLayout(SofwarePanel);
        SofwarePanel.setLayout(SofwarePanelLayout);
        SofwarePanelLayout.setHorizontalGroup(SofwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(SofwarePanelLayout.createSequentialGroup().addContainerGap().addGroup(SofwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(PackageNameLabel).addComponent(OverviewLabel).addComponent(DescriptionLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(SofwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(AreaScroll, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.LEADING, SofwarePanelLayout.createSequentialGroup().addComponent(PackageTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(4, 4, 4).addComponent(VersionLabel).addGap(10, 10, 10).addComponent(VersionTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(OverviewTextBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)).addGap(75, 75, 75)));
        SofwarePanelLayout.setVerticalGroup(SofwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(SofwarePanelLayout.createSequentialGroup().addContainerGap().addGroup(SofwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(PackageNameLabel).addComponent(PackageTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(VersionLabel).addComponent(ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(VersionTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(SofwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(OverviewLabel).addComponent(OverviewTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(SofwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(AreaScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE).addComponent(DescriptionLabel)).addContainerGap()));
        NextButton.setText("Next ->");
        NextButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                NextButtonMouseClicked(evt);
            }
        });
        javax.swing.GroupLayout GeneralTabLayout = new javax.swing.GroupLayout(GeneralTab);
        GeneralTab.setLayout(GeneralTabLayout);
        GeneralTabLayout.setHorizontalGroup(GeneralTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(GeneralTabLayout.createSequentialGroup().addContainerGap().addGroup(GeneralTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(PublisherPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(NextButton, javax.swing.GroupLayout.Alignment.TRAILING).addComponent(SofwarePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        GeneralTabLayout.setVerticalGroup(GeneralTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(GeneralTabLayout.createSequentialGroup().addContainerGap().addComponent(PublisherPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(SofwarePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(NextButton).addContainerGap()));
        TabPane.addTab("General", GeneralTab);
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Package Files"));
        FilesLabel.setText("Root Package Files Folder(*):");
        HelpArea.setBackground(new java.awt.Color(255, 255, 228));
        HelpArea.setColumns(20);
        HelpArea.setEditable(false);
        HelpArea.setFont(new java.awt.Font("Dialog", 0, 10));
        HelpArea.setLineWrap(true);
        HelpArea.setRows(5);
        HelpArea.setText("HELP:    The directory you specify must contain a virtual directory of a UNIX file system, with your package files placed appropriately. For example, if you want to have program.exe installed to \"/usr/share/\" and your directory containing the virtual file system was \"/home/USERNAME/packages/\", then you would place program.exe file in \"/home/USERNAME/packages/usr/bin/\" \nYour root vitual file system directory would then be \"/home/USERNAME/packages\"\n\nNOTE: do not add an extra slash after the root folder. For example, specify \"home/USERNAME/programs\" and NOT \"/home/USERNAME/programs/\"\nNOTE: You can NOT use the \"~\" variable when specifying your home directory. Instead, specify the full path.\n\n-----------------------------------------------------\n\nTUTORIAL: Here is an example of what your root folder might look like. Pretend that this is a list of your package files and the locations that they should be installed to on the computer:\n\n/usr/share/program.exe\n/usr/share/README.txt\n/usr/share/HELP.txt\n\nGo to your your home directory and create a folder  called \"TestProgram\". This will be your root directory. Now, create a virtual file system within this root directory. The \"TestProgram\" directory should now look like this:\n\n/home/USERNAME/TestProgram/usr/share/program.exe\n/home/USERNAME/TestProgram/usr/share/README.txt\n/home/USERNAME/TestProgram/usr/share/HELP.txt\n\nYou have successfully created your root folder and you would now type \"/home/USERNAME/TestProgram\" into the textbox above. \n\n*****************************************************************");
        HelpArea.setWrapStyleWord(true);
        HelpArea.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        jScrollPane2.setViewportView(HelpArea);
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 507, Short.MAX_VALUE).addGroup(jPanel1Layout.createSequentialGroup().addComponent(FilesLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(FilesTextBox, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE))).addContainerGap()));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(FilesLabel).addComponent(FilesTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(23, Short.MAX_VALUE)));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Dependencies & Mis. (ADVANCED)"));
        DependsLabel.setText("Depends:");
        PreDebpendsLabel.setText("Pre-Depends:");
        RecommendsLabel.setText("Recommends:");
        ConflictsLabel.setText("Conflicts:");
        ReplacesLabel.setText("Replaces:");
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addComponent(DependsLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 78, Short.MAX_VALUE).addComponent(DependsTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, 371, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(jPanel2Layout.createSequentialGroup().addComponent(PreDebpendsLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE).addComponent(PreDependsTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, 371, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(jPanel2Layout.createSequentialGroup().addComponent(RecommendsLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE).addComponent(RecommendsTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, 371, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(jPanel2Layout.createSequentialGroup().addComponent(ConflictsLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 77, Short.MAX_VALUE).addComponent(ConflictsTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, 371, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(jPanel2Layout.createSequentialGroup().addComponent(ReplacesLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 79, Short.MAX_VALUE).addComponent(ReplacesTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, 371, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap()));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(DependsLabel).addComponent(DependsTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(PreDebpendsLabel).addComponent(PreDependsTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(RecommendsLabel).addComponent(RecommendsTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(ConflictsLabel).addComponent(ConflictsTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(ReplacesLabel).addComponent(ReplacesTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        NextButton2.setText(" Next ->");
        NextButton2.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                NextButton2MouseClicked(evt);
            }
        });
        NextButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NextButton2ActionPerformed(evt);
            }
        });
        BackButton2.setText("<- Back");
        BackButton2.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                BackButton2MouseClicked(evt);
            }
        });
        javax.swing.GroupLayout ParameterTabLayout = new javax.swing.GroupLayout(ParameterTab);
        ParameterTab.setLayout(ParameterTabLayout);
        ParameterTabLayout.setHorizontalGroup(ParameterTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ParameterTabLayout.createSequentialGroup().addContainerGap().addGroup(ParameterTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(ParameterTabLayout.createSequentialGroup().addComponent(BackButton2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(NextButton2))).addContainerGap()));
        ParameterTabLayout.setVerticalGroup(ParameterTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(ParameterTabLayout.createSequentialGroup().addContainerGap().addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(ParameterTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(BackButton2).addComponent(NextButton2)).addContainerGap()));
        TabPane.addTab("Parameters", ParameterTab);
        BuildButton.setText("   Build Package   ");
        BuildButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                BuildButtonMouseClicked(evt);
            }
        });
        BackButton3.setText("<- Back");
        BackButton3.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                BackButton3MouseClicked(evt);
            }
        });
        BuildLabel.setBackground(new java.awt.Color(200, 200, 200));
        BuildLabel.setFont(new java.awt.Font("Dialog", 1, 24));
        BuildLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BuildLabel.setText("Build Package(s)");
        BuildLabel.setOpaque(true);
        BuildArea.setBackground(java.awt.SystemColor.window);
        BuildArea.setColumns(20);
        BuildArea.setEditable(false);
        BuildArea.setLineWrap(true);
        BuildArea.setRows(5);
        BuildArea.setText("Packin is ready to create your .deb package. Please Click the \"Build\" button to proceed. If you wish to make changes to your package before building, click \"Back\".");
        BuildArea.setWrapStyleWord(true);
        jScrollPane3.setViewportView(BuildArea);
        StatusArea.setColumns(20);
        StatusArea.setEditable(false);
        StatusArea.setRows(5);
        jScrollPane4.setViewportView(StatusArea);
        javax.swing.GroupLayout BuildTabLayout = new javax.swing.GroupLayout(BuildTab);
        BuildTab.setLayout(BuildTabLayout);
        BuildTabLayout.setHorizontalGroup(BuildTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BuildTabLayout.createSequentialGroup().addContainerGap(318, Short.MAX_VALUE).addComponent(BackButton3).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(BuildButton).addContainerGap()).addGroup(BuildTabLayout.createSequentialGroup().addContainerGap().addComponent(ProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 541, Short.MAX_VALUE).addContainerGap()).addComponent(BuildLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 565, Short.MAX_VALUE).addGroup(BuildTabLayout.createSequentialGroup().addContainerGap().addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 541, Short.MAX_VALUE).addContainerGap()).addGroup(BuildTabLayout.createSequentialGroup().addGap(150, 150, 150).addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE).addGap(155, 155, 155)));
        BuildTabLayout.setVerticalGroup(BuildTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BuildTabLayout.createSequentialGroup().addGap(30, 30, 30).addComponent(BuildLabel).addGap(15, 15, 15).addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(14, 14, 14).addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(16, 16, 16).addComponent(ProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE).addGroup(BuildTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(BuildButton).addComponent(BackButton3)).addContainerGap()));
        TabPane.addTab("Build", BuildTab);
        FileMenu.setText("File");
        FileMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileMenuActionPerformed(evt);
            }
        });
        FileExitItem.setText("Exit");
        FileExitItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileExitItemActionPerformed(evt);
            }
        });
        FileMenu.add(FileExitItem);
        MenuBar.add(FileMenu);
        EditMenu.setText("Edit");
        EditMenu.setEnabled(false);
        MenuBar.add(EditMenu);
        BuildMenu.setText("Package");
        BuildMenuItem.setText("Build Current Package...");
        BuildMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BuildMenuItemActionPerformed(evt);
            }
        });
        BuildMenu.add(BuildMenuItem);
        MenuBar.add(BuildMenu);
        HelpMenu.setText("Help");
        VisitPackinMenuItem.setLabel("Visit Packin Project Website...");
        VisitPackinMenuItem.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                VisitPackinMenuItemMouseClicked(evt);
            }
        });
        VisitPackinMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VisitPackinMenuItemActionPerformed(evt);
            }
        });
        HelpMenu.add(VisitPackinMenuItem);
        MenuBar.add(HelpMenu);
        setJMenuBar(MenuBar);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(TabPane, javax.swing.GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(TabPane, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE));
        pack();
    }

    private void BuildMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        TabPane.setSelectedIndex(3);
        java.awt.event.MouseEvent me = null;
        BuildButtonMouseClicked(me);
    }

    private void VisitPackinMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Desktop d = null;
            d = Desktop.getDesktop();
            URI u = new URI("http://kurtech.sourceforge.net/");
            d.browse(u);
        } catch (Exception ex) {
        }
    }

    private void VisitPackinMenuItemMouseClicked(java.awt.event.MouseEvent evt) {
    }

    private void BuildPackage(String params[]) {
        StatusArea.setText("");
        StatusArea.setText(StatusArea.getText() + "Starting Build...\n");
        ProgressBar.setValue(2);
        StatusArea.setText(StatusArea.getText() + "Preparing to write Configuration file...\n");
        ProgressBar.setValue(4);
        StatusArea.setText(StatusArea.getText() + "Reading parameter values...\nList of values:\n\n");
        int i = 0;
        while (i != 13) {
            StatusArea.setText(StatusArea.getText() + "Value " + String.valueOf(i) + " = " + params[i] + "\n");
            i++;
        }
        ProgressBar.setValue(6);
        params[6] = params[6].trim();
        String strTmp = params[6];
        if (!strTmp.startsWith("/")) {
            params[6] = "/" + strTmp;
        }
        try {
            String directory = params[6] + "/DEBIAN";
            boolean success = (new File(directory)).mkdir();
            ProgressBar.setValue(8);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Packin has encountered an exception while creating the configuration directory.\nPackin will try to bypass this exception and continue building the package.\n\nNOTE: This exception was likely caused by ineffecient read/write permissions with the \nspecified root folder. If this is the case, modify permissions and rerun Packin.");
        }
        try {
            boolean exists = (new File(params[6] + "/DEBIAN/control")).exists();
            if (exists) {
                boolean success2 = (new File(params[6] + "/DEBIAN/control")).delete();
            }
            File configFile = new File(params[6] + "DEBIAN/config");
            configFile.createNewFile();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Packin has encountered an exception while creating the configuration files(s).\nPackin will try to bypass this exception and continue building the package.\n\nLIKELY CAUSES:\n 1.   This exception may have been caused by ineffecient read/write permissions with the \nspecified root folder. If this is the case, modify permissions and rerun Packin.\n\n2.   The root folder that you specified may not exist and/or was specified incorrectly.");
        }
        ProgressBar.setValue(7);
        try {
            FileWriter writer = new FileWriter(params[6] + "/DEBIAN/control");
            writer.write("Package: " + params[2].trim() + "\n");
            writer.write("Version: " + params[3].trim() + "\n");
            writer.append("Architecture: " + params[12].trim() + "\n");
            writer.write("Depends: " + params[7].trim() + "\n");
            writer.write("Pre-Depends: " + params[8].trim() + "\n");
            writer.write("Recommends: " + params[9].trim() + "\n");
            writer.write("Maintainer: " + params[0].trim() + " [" + params[1].trim() + "]\n");
            writer.write("Conflicts: " + params[10].trim() + "\n");
            writer.write("Replaces: " + params[11].trim() + "\n");
            writer.write("Provides: " + params[2].trim() + "\n");
            params[5] = params[5].trim();
            params[5] = params[5].replaceAll("\n", "\n             ");
            writer.write("Description: " + params[5] + "\n\n");
            writer.close();
            ProgressBar.setValue(10);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
        }
        String command = "";
        String location = "";
        String s = null;
        params[2] = params[2].replaceAll(" ", "");
        try {
            int i1 = params[6].lastIndexOf("/");
            String store = params[6].substring(0, i1);
            command = "dpkg -b " + params[6] + " " + store.trim() + "/" + params[2].trim() + "_" + params[3].trim() + "_" + params[12].trim() + ".deb";
            location = store.trim() + "/" + params[2].trim() + "_" + params[3].trim() + "_" + params[12].trim() + ".deb";
        } catch (Exception ex) {
            command = "dpkg -b " + params[6];
            location = "";
        }
        ProgressBar.setValue(15);
        try {
            StatusArea.setText(StatusArea.getText() + "Starting dpkg with command:\n" + command + "\n");
            Process p = Runtime.getRuntime().exec(command);
            ProgressBar.setValue(45);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StatusArea.setText(StatusArea.getText() + ":\n");
            while ((s = stdInput.readLine()) != null) {
                StatusArea.setText(StatusArea.getText() + s);
            }
            ProgressBar.setValue(86);
            StatusArea.setText(StatusArea.getText() + "\nAny errors or warnings listed:\n");
            int err = 0;
            while ((s = stdError.readLine()) != null) {
                StatusArea.setText(StatusArea.getText() + s);
                err = 1;
            }
            ProgressBar.setValue(95);
            if (err == 0) {
                if (location != "") {
                    JOptionPane.showMessageDialog(null, "Packin has successfully created the deb package. The package has been saved to:\n " + location);
                } else {
                    JOptionPane.showMessageDialog(null, "Packin has successfully created the deb package. The package has been stored one level above the specified root folder. It has been named deb.deb");
                }
            } else {
                JOptionPane.showMessageDialog(null, "An error or warning has occurred! Please refer to the status list in order to \nread more about the errors and/or warnings encountered.\n\nNOTE: The package may have been successfully created even with warnings. \nIt is recommended, however, that any warnings be resolved and the package rebuilt.");
            }
            ProgressBar.setValue(100);
        } catch (Exception ex) {
        }
    }

    private void BuildButtonMouseClicked(java.awt.event.MouseEvent evt) {
        try {
            String params[] = new String[13];
            params[0] = PublisherTextBox.getText();
            params[1] = EmailTextBox.getText();
            params[2] = PackageTextBox.getText();
            params[3] = VersionTextBox.getText();
            params[4] = OverviewTextBox.getText();
            params[5] = DescriptionArea.getText();
            params[6] = FilesTextBox.getText();
            params[7] = DependsTextBox.getText();
            params[8] = PreDependsTextBox.getText();
            params[9] = RecommendsTextBox.getText();
            params[10] = ConflictsTextBox.getText();
            params[11] = ReplacesTextBox.getText();
            int arch = ComboBox.getSelectedIndex();
            if (arch == 0) {
                params[12] = "i386";
            } else if (arch == 1) {
                params[12] = "amd64";
            } else {
                params[12] = "all";
            }
            String controls[] = new String[7];
            controls[0] = "Publisher Name";
            controls[1] = "Publisher Email";
            controls[2] = "Package List";
            controls[3] = "Package Version";
            controls[4] = "Overiew";
            controls[5] = "Description";
            controls[6] = "Files";
            this.BuildPackage(params);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "An Error Occured! Please make sure that all required fields are filled out and try again! If this problem persits, visit the Packin forum to get help.");
        }
    }

    private void StartButtonMouseClicked(java.awt.event.MouseEvent evt) {
        TabPane.setSelectedIndex(1);
    }

    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {
    }

    private void BackButton3MouseClicked(java.awt.event.MouseEvent evt) {
        TabPane.setSelectedIndex(2);
    }

    private void NextButton2MouseClicked(java.awt.event.MouseEvent evt) {
        TabPane.setSelectedIndex(3);
    }

    private void BackButton2MouseClicked(java.awt.event.MouseEvent evt) {
        TabPane.setSelectedIndex(1);
    }

    private void NextButton2ActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void NextButtonMouseClicked(java.awt.event.MouseEvent evt) {
        TabPane.setSelectedIndex(2);
    }

    private void formWindowOpened(java.awt.event.WindowEvent evt) {
    }

    private void PackageTextBoxActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void FileExitItemActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    private void FileMenuActionPerformed(java.awt.event.ActionEvent evt) {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }

    private javax.swing.JScrollPane AreaScroll;

    private javax.swing.JButton BackButton2;

    private javax.swing.JButton BackButton3;

    private javax.swing.JTextArea BuildArea;

    private javax.swing.JButton BuildButton;

    private javax.swing.JLabel BuildLabel;

    private javax.swing.JMenu BuildMenu;

    private javax.swing.JMenuItem BuildMenuItem;

    private javax.swing.JPanel BuildTab;

    private javax.swing.JComboBox ComboBox;

    private javax.swing.JLabel ConflictsLabel;

    private javax.swing.JTextField ConflictsTextBox;

    private javax.swing.JLabel DependsLabel;

    private javax.swing.JTextField DependsTextBox;

    private javax.swing.JTextArea DescriptionArea;

    private javax.swing.JLabel DescriptionLabel;

    private javax.swing.JMenu EditMenu;

    private javax.swing.JLabel EmailLabel;

    private javax.swing.JTextField EmailTextBox;

    private javax.swing.JMenuItem FileExitItem;

    private javax.swing.JMenu FileMenu;

    private javax.swing.JLabel FilesLabel;

    private javax.swing.JTextField FilesTextBox;

    private javax.swing.JPanel GeneralTab;

    private javax.swing.JTextArea HelpArea;

    private javax.swing.JMenu HelpMenu;

    private javax.swing.JTextPane IntroText;

    private javax.swing.JMenuBar MenuBar;

    private javax.swing.JLabel NameLabel;

    private javax.swing.JButton NextButton;

    private javax.swing.JButton NextButton2;

    private javax.swing.JLabel OverviewLabel;

    private javax.swing.JTextField OverviewTextBox;

    private javax.swing.JLabel PackageNameLabel;

    private javax.swing.JTextField PackageTextBox;

    private javax.swing.JPanel ParameterTab;

    private javax.swing.JLabel PreDebpendsLabel;

    private javax.swing.JTextField PreDependsTextBox;

    private javax.swing.JProgressBar ProgressBar;

    private javax.swing.JPanel PublisherPanel;

    private javax.swing.JTextField PublisherTextBox;

    private javax.swing.JLabel RecommendsLabel;

    private javax.swing.JTextField RecommendsTextBox;

    private javax.swing.JLabel ReplacesLabel;

    private javax.swing.JTextField ReplacesTextBox;

    private javax.swing.JPanel SofwarePanel;

    private javax.swing.JButton StartButton;

    private javax.swing.JLabel StartLabel;

    private javax.swing.JPanel StartTab;

    private javax.swing.JTextArea StatusArea;

    private javax.swing.JTabbedPane TabPane;

    private javax.swing.JLabel VersionLabel;

    private javax.swing.JTextField VersionTextBox;

    private javax.swing.JMenuItem VisitPackinMenuItem;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JScrollPane jScrollPane4;

    private JLabel imageLabel;
}
