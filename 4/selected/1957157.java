package atv;

import java.io.*;
import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.*;
import java.net.URL;

/**
 * 
 * @author Christian M. Zmasek
 * 
 * @version 2.110 -- last modified: 10/04/01
 * 
 */
public class ATVjframe extends JFrame implements ActionListener {

    private Tree reload_tree_ = null, species_tree_ = null;

    private File treefile = null;

    static int decalage = 0;

    private JMenuBar jmenubar;

    private JMenu file_jmenu, edit_jmenu, view_jmenu, options_jmenu, help_jmenu, print_size_jmenu, SDI_jmenu, search_jmenu;

    private JMenuItem open_item, open_url_item, save_item, saveas_item, print_item, close_item, reload_item, remove_root_item, remove_root_tri_item, tiny_fonts_item, small_fonts_item, medium_fonts_item, large_fonts_item, switch_colors_item, view_as_NH_item, view_as_NHX_item, about_item, help_item, load_species_tree_item, infer_dups_item, root_min_dups_height_item, root_min_L_height_item, root_min_height_item, find_item, find_reset_item, comments_item;

    private ATVpanel atvpanel;

    public JTextArea comments;

    public JScrollPane commentScrollPane;

    private Container contentpane;

    private ATVtextframe atvtextframe;

    private JFileChooser open_jfilechooser, saveas_jfilechooser;

    private JSlider x_slider, y_slider;

    private JLabel xs_label, ys_label;

    private JCheckBox print_in_color;

    private static final int SLIDER_MIN = 0, SLIDER_MAX = 200, SLIDER_VAL = 100, SLIDER_MAJS = 50, SLIDER_MINS = 10;

    private static final Color menu_background_color = new Color(215, 215, 215), menu_text_color = new Color(0, 0, 0);

    private static final Font menu_font = new Font("Helvetica", Font.PLAIN, 10);

    private static final int FRAME_X_SIZE = 640, FRAME_Y_SIZE = 580, PRINT_X_SIZE = 580, PRINT_Y_SIZE = 700;

    private static final NHXFilter nhxfilter = new NHXFilter();

    private static final NHFilter nhfilter = new NHFilter();

    public int ind;

    public String title;

    public String treeComments;

    /**
	 * 
	 * This constructor creates and displays a JFrame containing the image of a
	 * Tree t plus all the necessary controls. It is recommended that method
	 * "showWhole()" is called after a ATVframe has been constructed to ensure
	 * the whole Tree is displayed. Requirement: JDK 1.1 plus Swing/JFC, or JDK
	 * 1.2 or greater.
	 * 
	 * @param t
	 *            the Tree to display
	 * 
	 * @see #showWhole()
	 * 
	 */
    public ATVjframe(Tree t) {
        setVisible(false);
        if (t != null && !t.isEmpty()) {
            reload_tree_ = t.copyTree();
        }
        atvtextframe = null;
        species_tree_ = null;
        setTitle("ATV");
        decalage += 20;
        treeComments = t.comments;
        open_jfilechooser = new JFileChooser();
        open_jfilechooser.setCurrentDirectory(new File("."));
        open_jfilechooser.setMultiSelectionEnabled(false);
        open_jfilechooser.setFileFilter(nhxfilter);
        open_jfilechooser.addChoosableFileFilter(nhfilter);
        open_jfilechooser.addChoosableFileFilter(open_jfilechooser.getAcceptAllFileFilter());
        saveas_jfilechooser = new JFileChooser();
        saveas_jfilechooser.setCurrentDirectory(new File("."));
        saveas_jfilechooser.setMultiSelectionEnabled(false);
        saveas_jfilechooser.setFileFilter(nhxfilter);
        saveas_jfilechooser.addChoosableFileFilter(nhfilter);
        saveas_jfilechooser.addChoosableFileFilter(saveas_jfilechooser.getAcceptAllFileFilter());
        atvpanel = new ATVpanel(t);
        jmenubar = new JMenuBar();
        jmenubar.setBackground(menu_background_color);
        file_jmenu = new JMenu("Tree");
        edit_jmenu = new JMenu("Edit");
        search_jmenu = new JMenu("Search");
        view_jmenu = new JMenu("View");
        options_jmenu = new JMenu("Options");
        help_jmenu = new JMenu("Help");
        print_size_jmenu = new JMenu("Print Options");
        file_jmenu.setFont(menu_font);
        file_jmenu.setBackground(menu_background_color);
        file_jmenu.setForeground(menu_text_color);
        edit_jmenu.setFont(menu_font);
        edit_jmenu.setBackground(menu_background_color);
        edit_jmenu.setForeground(menu_text_color);
        search_jmenu.setFont(menu_font);
        search_jmenu.setBackground(menu_background_color);
        search_jmenu.setForeground(menu_text_color);
        view_jmenu.setFont(menu_font);
        view_jmenu.setBackground(menu_background_color);
        view_jmenu.setForeground(menu_text_color);
        options_jmenu.setFont(menu_font);
        options_jmenu.setBackground(menu_background_color);
        options_jmenu.setForeground(menu_text_color);
        help_jmenu.setFont(menu_font);
        help_jmenu.setBackground(menu_background_color);
        help_jmenu.setForeground(menu_text_color);
        print_size_jmenu.setFont(menu_font);
        print_size_jmenu.setBackground(menu_background_color);
        print_size_jmenu.setForeground(menu_text_color);
        file_jmenu.add(reload_item = new JMenuItem("Reload"));
        file_jmenu.addSeparator();
        file_jmenu.add(open_item = new JMenuItem("Open"));
        file_jmenu.add(saveas_item = new JMenuItem("Save As"));
        file_jmenu.add(save_item = new JMenuItem("Save"));
        file_jmenu.addSeparator();
        file_jmenu.add(print_size_jmenu);
        file_jmenu.add(print_item = new JMenuItem("Print"));
        file_jmenu.addSeparator();
        file_jmenu.add(close_item = new JMenuItem("Close"));
        file_jmenu.addSeparator();
        x_slider = new JSlider(SLIDER_MIN, SLIDER_MAX, SLIDER_VAL);
        y_slider = new JSlider(SLIDER_MIN, SLIDER_MAX, SLIDER_VAL);
        xs_label = new JLabel("X size:");
        ys_label = new JLabel("Y size:");
        xs_label.setFont(menu_font);
        xs_label.setForeground(menu_text_color);
        ys_label.setFont(menu_font);
        ys_label.setForeground(menu_text_color);
        x_slider.setFont(menu_font);
        y_slider.setFont(menu_font);
        print_in_color = new JCheckBox("Print in color");
        print_in_color.setFont(menu_font);
        print_in_color.setForeground(menu_text_color);
        print_size_jmenu.add(print_in_color);
        x_slider.setMajorTickSpacing(SLIDER_MAJS);
        x_slider.setMinorTickSpacing(SLIDER_MINS);
        x_slider.setPaintTicks(true);
        x_slider.setPaintLabels(true);
        x_slider.setSnapToTicks(true);
        print_size_jmenu.add(xs_label);
        print_size_jmenu.add(x_slider);
        y_slider.setMajorTickSpacing(SLIDER_MAJS);
        y_slider.setMinorTickSpacing(SLIDER_MINS);
        y_slider.setPaintTicks(true);
        y_slider.setPaintLabels(true);
        y_slider.setSnapToTicks(true);
        print_size_jmenu.add(ys_label);
        print_size_jmenu.add(y_slider);
        edit_jmenu.add(remove_root_item = new JMenuItem("Remove root"));
        edit_jmenu.add(remove_root_tri_item = new JMenuItem("Remove root and trifurcate"));
        search_jmenu.add(find_item = new JMenuItem("Search"));
        search_jmenu.addSeparator();
        search_jmenu.add(find_reset_item = new JMenuItem("Reset"));
        view_jmenu.add(view_as_NHX_item = new JMenuItem("Newick format"));
        options_jmenu.add(switch_colors_item = new JMenuItem("Switch colors"));
        options_jmenu.addSeparator();
        options_jmenu.add(tiny_fonts_item = new JMenuItem("Tiny fonts"));
        options_jmenu.add(small_fonts_item = new JMenuItem("Small fonts"));
        options_jmenu.add(medium_fonts_item = new JMenuItem("Medium fonts"));
        options_jmenu.add(large_fonts_item = new JMenuItem("Large fonts"));
        help_jmenu.add(help_item = new JMenuItem("Help"));
        help_jmenu.add(about_item = new JMenuItem("About"));
        customizeJMenuItem(reload_item);
        customizeJMenuItem(open_item);
        customizeJMenuItem(save_item);
        customizeJMenuItem(saveas_item);
        customizeJMenuItem(print_item);
        customizeJMenuItem(close_item);
        customizeJMenuItem(remove_root_item);
        customizeJMenuItem(remove_root_tri_item);
        customizeJMenuItem(tiny_fonts_item);
        customizeJMenuItem(small_fonts_item);
        customizeJMenuItem(medium_fonts_item);
        customizeJMenuItem(large_fonts_item);
        customizeJMenuItem(switch_colors_item);
        customizeJMenuItem(view_as_NHX_item);
        customizeJMenuItem(about_item);
        customizeJMenuItem(help_item);
        customizeJMenuItem(find_item);
        customizeJMenuItem(find_reset_item);
        jmenubar.add(file_jmenu);
        jmenubar.add(edit_jmenu);
        jmenubar.add(search_jmenu);
        jmenubar.add(view_jmenu);
        jmenubar.add(options_jmenu);
        jmenubar.add(help_jmenu);
        setJMenuBar(jmenubar);
        contentpane = getContentPane();
        contentpane.setLayout(new BorderLayout());
        comments = new JTextArea(treeComments);
        comments.setLineWrap(true);
        comments.setRows(3);
        commentScrollPane = new JScrollPane(comments);
        contentpane.add(atvpanel, BorderLayout.CENTER);
        contentpane.add(commentScrollPane, BorderLayout.NORTH);
        setSize(FRAME_X_SIZE, FRAME_Y_SIZE);
        setBounds(decalage, decalage, FRAME_X_SIZE, FRAME_Y_SIZE);
        setSize(FRAME_X_SIZE + 1, FRAME_Y_SIZE + 1);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                ((TreeContainer) ToolBoxPanel.editTrees.vTree.get(ind)).isDisplayed = false;
                close();
            }
        });
        addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                atvpanel.getATVgraphic().setParametersForPainting(atvpanel.getATVgraphic().getWidth(), atvpanel.getATVgraphic().getHeight());
            }
        });
        setVisible(true);
    }

    /**
	 * 
	 * Resizes the Tree, so that it is displayed in its entirety. It is
	 * recommended to call this method after the constructor ATVframe(Tree t)
	 * has been called.
	 * 
	 * @see #ATVjframe(Tree)
	 * 
	 */
    public void showWhole() {
        atvpanel.getATVcontrol().showWhole();
    }

    /**
	 * 
	 * Sets the maximal number a sequence is expected to be orthologous towards
	 * another, i.e. the number of resampling steps. (Last modified: 12/05/00)
	 * 
	 */
    public void setMaxOrtho(int m) {
        atvpanel.getATVgraphic().setMaxOrtho(m);
    }

    /**
	 * 
	 * Called automatically.
	 * 
	 */
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o == reload_item) {
            reLoad();
        } else if (o == open_item) {
            openFile();
        } else if (o == save_item) {
            save(atvpanel.getATVgraphic().getTree());
        } else if (o == saveas_item) {
            saveAs(atvpanel.getATVgraphic().getTree());
        } else if (o == print_item) {
            printTree();
        } else if (o == close_item) {
            ((TreeContainer) ToolBoxPanel.editTrees.vTree.get(ind)).isDisplayed = false;
            close();
        } else if (o == remove_root_item) {
            removeRoot();
        } else if (o == remove_root_tri_item) {
            removeRootTri();
        } else if (o == switch_colors_item) {
            switchColors();
        } else if (o == tiny_fonts_item) {
            atvpanel.getATVgraphic().tinyFonts();
            atvpanel.getATVgraphic().repaint();
        } else if (o == small_fonts_item) {
            atvpanel.getATVgraphic().smallFonts();
            atvpanel.getATVgraphic().repaint();
        } else if (o == medium_fonts_item) {
            atvpanel.getATVgraphic().mediumFonts();
            atvpanel.getATVgraphic().repaint();
        } else if (o == large_fonts_item) {
            atvpanel.getATVgraphic().largeFonts();
            atvpanel.getATVgraphic().repaint();
        } else if (o == view_as_NH_item) {
            viewAsNH();
        } else if (o == view_as_NHX_item) {
            viewAsNHX();
        } else if (o == about_item) {
            about();
        } else if (o == help_item) {
            help();
        } else if (o == load_species_tree_item) {
            openSpeciesTreeFile();
        } else if (o == infer_dups_item) {
            inferDups();
        } else if (o == root_min_dups_height_item) {
            rootMinDupsHeight();
        } else if (o == root_min_L_height_item) {
            rootMinLHeight();
        } else if (o == root_min_height_item) {
            rootMinHeight();
        } else if (o == find_item) {
            find();
        } else if (o == find_reset_item) {
            findReset();
        }
    }

    private void findReset() {
        if (atvpanel.getATVgraphic().getTree() == null || atvpanel.getATVgraphic().getTree().isEmpty()) {
            return;
        }
        atvpanel.getATVgraphic().setFoundNodes(null);
        contentpane.repaint();
    }

    private void find() {
        if (atvpanel.getATVgraphic().getTree() == null || atvpanel.getATVgraphic().getTree().isEmpty()) {
            return;
        }
        String message = "String to search for in sequence and species names,\nEC numbers; or integer for taxonomy IDs:";
        String query = JOptionPane.showInputDialog(this, message, "Search", JOptionPane.QUESTION_MESSAGE);
        if (query != null) {
            query = query.trim();
            if (!query.equals("")) {
                Vector nodes = null;
                try {
                    nodes = atvpanel.getATVgraphic().getTree().findInNameSpecECid(query);
                } catch (Exception e) {
                    System.err.println("Unexpected exception: " + e);
                }
                if (nodes != null && nodes.size() > 0) {
                    atvpanel.getATVgraphic().setFoundNodes(nodes);
                } else {
                    JOptionPane.showMessageDialog(this, "Could not find \"" + query + "\"", "Search", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        contentpane.repaint();
    }

    private void openFile() {
        boolean exception = false;
        Tree t = null;
        int result = open_jfilechooser.showOpenDialog(contentpane);
        File file = open_jfilechooser.getSelectedFile();
        if (file != null && result == JFileChooser.APPROVE_OPTION) {
            if (open_jfilechooser.getFileFilter() == nhfilter || open_jfilechooser.getFileFilter() == nhxfilter) {
                try {
                    t = TreeHelper.readNHtree(file);
                } catch (Exception e) {
                    exception = true;
                    exceptionOccuredDuringOpenFile(e);
                }
            } else {
                try {
                    t = TreeHelper.readNHtree(file);
                } catch (Exception e) {
                    exception = true;
                    exceptionOccuredDuringOpenFile(e);
                }
            }
            if (!exception && t != null && !t.isEmpty()) {
                reload_tree_ = t.copyTree();
                removeatvtextframe();
                atvpanel.terminate();
                contentpane.removeAll();
                atvpanel = new ATVpanel(t);
                contentpane.add(atvpanel, BorderLayout.CENTER);
                setVisible(true);
                treefile = file;
                setTitle("" + treefile);
                atvpanel.getATVgraphic().setParametersForPainting(atvpanel.getATVgraphic().getWidth(), atvpanel.getATVgraphic().getHeight());
                atvpanel.getATVcontrol().showWhole();
            }
            contentpane.repaint();
        }
    }

    private void openURL() {
        URL url = null;
        Tree t = null;
        String message = "Please enter a complete URL";
        String url_string = JOptionPane.showInputDialog(this, message, "Open URL to read a NH/NHX tree", JOptionPane.QUESTION_MESSAGE);
        if (url_string != null && url_string.length() > 4) {
            try {
                url = new URL(url_string);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "TreeJAppletJFrame: openURL(): " + "Exception: " + e, "Malformed URL", JOptionPane.ERROR_MESSAGE);
            }
            if (url != null) {
                try {
                    t = TreeHelper.readNHtree(url);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "TreeJAppletJFrame: openURL(): " + "\nException: " + e, "Could not read Tree", JOptionPane.ERROR_MESSAGE);
                }
                if (t != null && !t.isEmpty()) {
                    reload_tree_ = t.copyTree();
                    removeatvtextframe();
                    atvpanel.terminate();
                    contentpane.removeAll();
                    atvpanel = new ATVpanel(t);
                    contentpane.add(atvpanel, BorderLayout.CENTER);
                    setVisible(true);
                    contentpane.repaint();
                    setTitle("ATV: " + url);
                    treefile = null;
                    atvpanel.getATVgraphic().setParametersForPainting(atvpanel.getATVgraphic().getWidth(), atvpanel.getATVgraphic().getHeight());
                }
            }
            contentpane.repaint();
        }
    }

    private void reLoad() {
        if (reload_tree_ != null && !reload_tree_.isEmpty()) {
            Tree t = reload_tree_.copyTree();
            removeatvtextframe();
            atvpanel.terminate();
            contentpane.removeAll();
            atvpanel = new ATVpanel(t);
            contentpane.add(atvpanel, BorderLayout.CENTER);
            setVisible(true);
            if (treefile != null) {
                setTitle("ATV: " + treefile);
            } else {
                setTitle("ATV");
            }
            atvpanel.getATVgraphic().setParametersForPainting(atvpanel.getATVgraphic().getWidth(), atvpanel.getATVgraphic().getHeight());
            atvpanel.getATVcontrol().showWhole();
            contentpane.repaint();
        }
    }

    void save(Tree t) {
        try {
            ((TreeContainer) ToolBoxPanel.editTrees.vTree.get(ind)).str = toNHX.toNewick(t.toNewHampshireX());
            treeComments = comments.getText();
            treeComments = this.concatenateComments(treeComments);
            ((TreeContainer) ToolBoxPanel.editTrees.vTree.get(ind)).comments = "[" + treeComments + "]";
            MainFrame.isChanged = true;
            MainFrame.isSaved = false;
            int i = JOptionPane.showConfirmDialog(this, " Tree Saved", "File|SaveAs", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Exception: " + e, "Error during File|Save", JOptionPane.ERROR_MESSAGE);
        }
    }

    void saveAs(Tree t) {
        if (t == null) {
            return;
        }
        boolean ow1 = false;
        boolean ow2 = false;
        boolean exception = false;
        int result = saveas_jfilechooser.showSaveDialog(contentpane);
        File file = saveas_jfilechooser.getSelectedFile();
        if (file != null && result == JFileChooser.APPROVE_OPTION) {
            if (file.exists()) {
                int i = JOptionPane.showConfirmDialog(this, file + " already exists. Overwrite?", "File|SaveAs", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (i == JOptionPane.OK_OPTION) {
                    ow1 = ow2 = true;
                } else {
                    return;
                }
            }
            try {
                BufferedWriter bf = new BufferedWriter(new FileWriter(file));
                treeComments = comments.getText();
                treeComments = this.concatenateComments(treeComments);
                bf.write("[" + treeComments + " ]\n");
                bf.write(FormatFileTo80Cols.formatString(toNHX.toNewick(t.toNewHampshireX())));
                bf.close();
            } catch (Exception e) {
                exception = true;
                exceptionOccuredDuringSaveAs(e);
            }
            if (!exception) {
                treefile = file;
                setTitle("" + treefile);
            }
        }
    }

    public String concatenateComments(String com) {
        StringBuffer sb = new StringBuffer();
        int index = com.indexOf('\n');
        int l = com.length();
        int index_begin = 0;
        String line;
        com = com.replace('[', '(');
        com = com.replace(']', ')');
        try {
            while (index != -1) {
                line = com.substring(index_begin, index);
                sb = sb.append(line).append('\\');
                index_begin = index + 1;
                index = com.indexOf('\n', index_begin);
            }
            sb = sb.append(com.substring(index_begin, l));
            return (sb.toString());
        } catch (Exception e) {
            System.out.println("Probleme dans formatComments");
            return null;
        }
    }

    private void printTree() {
        if (atvpanel.getATVgraphic().getTree() == null || atvpanel.getATVgraphic().getTree().isEmpty()) {
            return;
        }
        ATVprinter atvprinter = null;
        atvpanel.getATVgraphic().setParametersForPainting((int) (PRINT_X_SIZE * x_slider.getValue() / SLIDER_VAL), (int) (PRINT_Y_SIZE * y_slider.getValue() / SLIDER_VAL));
        if (treefile != null) {
            atvprinter = new ATVprinter(atvpanel.getATVgraphic(), treefile.toString(), print_in_color.isSelected());
        } else {
            atvprinter = new ATVprinter(atvpanel.getATVgraphic(), "ATVprinter", print_in_color.isSelected());
        }
        atvpanel.getATVgraphic().setParametersForPainting(atvpanel.getATVgraphic().getWidth(), atvpanel.getATVgraphic().getHeight());
        contentpane.repaint();
    }

    private void close() {
        removeatvtextframe();
        atvpanel.terminate();
        contentpane.removeAll();
        setVisible(false);
        dispose();
    }

    private void removeRoot() {
        atvpanel.getATVgraphic().removeRoot();
    }

    private void removeRootTri() {
        atvpanel.getATVgraphic().removeRootTri();
    }

    private void switchColors() {
        atvpanel.getATVgraphic().switchColors();
    }

    private void viewAsNH() {
        removeatvtextframe();
        if (atvpanel.getATVgraphic().getTree() == null) {
            return;
        }
        if (atvpanel.getATVgraphic().getTree().isEmpty()) {
            return;
        }
        atvtextframe = new ATVtextframe(atvpanel.getATVgraphic().getTree().toNewHampshire(false), title);
    }

    public void viewAsNHX() {
        removeatvtextframe();
        if (atvpanel.getATVgraphic().getTree() == null) {
            return;
        }
        if (atvpanel.getATVgraphic().getTree().isEmpty()) {
            return;
        }
        atvtextframe = new ATVtextframe(toNHX.toNewick(atvpanel.getATVgraphic().getTree().toNewHampshireX()), title);
    }

    private void viewComments() {
        removeatvtextframe();
        if (atvpanel.getATVgraphic().getTree() == null) {
            return;
        }
        if (atvpanel.getATVgraphic().getTree().isEmpty()) {
            return;
        }
        atvtextframe = new ATVtextframe(atvpanel.getATVgraphic().getTree().comments, title);
    }

    private void inferDups() {
        if (!isOKforSDI()) {
            return;
        }
        if (!atvpanel.getATVgraphic().getTree().isRooted()) {
            JOptionPane.showMessageDialog(this, "Gene tree is not rooted.", "Error during SDI", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Tree gene_tree = atvpanel.getATVgraphic().getTree().copyTree();
        SDI sdi = null;
        int duplications = -1;
        gene_tree.setAllNodesToNotCollapse();
        gene_tree.adjustNodeCount(false);
        gene_tree.recalculateAndReset();
        TreeHelper.extractSpeciesNameFromSeqName(gene_tree);
        SDI.stripTree(species_tree_, gene_tree);
        if (!GAndSDoHaveMoreThanOneSpeciesInComman(gene_tree)) {
            return;
        }
        try {
            sdi = new SDIse(gene_tree, species_tree_.copyTree());
            duplications = sdi.infer(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.toString(), "Exception during SDI", JOptionPane.ERROR_MESSAGE);
        }
        String title = getTitle();
        if (title.indexOf(" [") > -1) {
            title = title.substring(0, title.indexOf(" ["));
        }
        if (duplications >= 0) {
            title = title + " [" + duplications + " duplications]";
        }
        setTitle(title);
        atvpanel.getATVgraphic().setTree(gene_tree);
        showWhole();
        contentpane.repaint();
    }

    private void rootMinDupsHeight() {
        if (!isOKforSDI()) {
            return;
        }
        Tree gene_tree = atvpanel.getATVgraphic().getTree().copyTree();
        SDIunrooted sdiunrooted = new SDIunrooted();
        gene_tree.setAllNodesToNotCollapse();
        gene_tree.adjustNodeCount(false);
        gene_tree.recalculateAndReset();
        TreeHelper.extractSpeciesNameFromSeqName(gene_tree);
        SDI.stripTree(species_tree_, gene_tree);
        if (!GAndSDoHaveMoreThanOneSpeciesInComman(gene_tree)) {
            return;
        }
        try {
            gene_tree = sdiunrooted.infer(gene_tree, species_tree_, false, true, true, true, 1)[0];
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.toString(), "Exception during SDI", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int duplications = sdiunrooted.getMinimalDuplications();
        String title = getTitle();
        if (title.indexOf(" [") > -1) {
            title = title.substring(0, title.indexOf(" ["));
        }
        if (duplications >= 0) {
            title = title + " [" + duplications + " duplications]";
        }
        setTitle(title);
        atvpanel.getATVgraphic().setTree(gene_tree);
        showWhole();
        contentpane.repaint();
    }

    private void rootMinLHeight() {
        if (!isOKforSDI()) {
            return;
        }
        Tree gene_tree = atvpanel.getATVgraphic().getTree().copyTree();
        SDIunrooted sdiunrooted = new SDIunrooted();
        gene_tree.setAllNodesToNotCollapse();
        gene_tree.adjustNodeCount(false);
        gene_tree.recalculateAndReset();
        TreeHelper.extractSpeciesNameFromSeqName(gene_tree);
        SDI.stripTree(species_tree_, gene_tree);
        if (!GAndSDoHaveMoreThanOneSpeciesInComman(gene_tree)) {
            return;
        }
        try {
            gene_tree = sdiunrooted.infer(gene_tree, species_tree_, true, false, true, true, 1)[0];
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.toString(), "Exception during SDI", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int duplications = sdiunrooted.getMinimalDuplications();
        int L = sdiunrooted.getMinimalMappingCost();
        String title = getTitle();
        if (title.indexOf(" [") > -1) {
            title = title.substring(0, title.indexOf(" ["));
        }
        if (duplications >= 0 && L >= 0) {
            title = title + " [" + duplications + " duplications  L=" + L + "]";
        }
        setTitle(title);
        atvpanel.getATVgraphic().setTree(gene_tree);
        showWhole();
        contentpane.repaint();
    }

    private void rootMinHeight() {
        if (!isOKforSDI()) {
            return;
        }
        Tree gene_tree = atvpanel.getATVgraphic().getTree().copyTree();
        SDIunrooted sdiunrooted = new SDIunrooted();
        gene_tree.setAllNodesToNotCollapse();
        gene_tree.adjustNodeCount(false);
        gene_tree.recalculateAndReset();
        TreeHelper.extractSpeciesNameFromSeqName(gene_tree);
        SDI.stripTree(species_tree_, gene_tree);
        if (!GAndSDoHaveMoreThanOneSpeciesInComman(gene_tree)) {
            return;
        }
        try {
            gene_tree = sdiunrooted.infer(gene_tree, species_tree_, false, false, true, true, 1)[0];
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.toString(), "Exception during SDI", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int duplications = sdiunrooted.getMinimalDuplications();
        String title = getTitle();
        if (title.indexOf(" [") > -1) {
            title = title.substring(0, title.indexOf(" ["));
        }
        if (duplications >= 0) {
            title = title + " [" + duplications + " duplications]";
        }
        setTitle(title);
        atvpanel.getATVgraphic().setTree(gene_tree);
        showWhole();
        contentpane.repaint();
    }

    private void openSpeciesTreeFile() {
        Tree t = null;
        boolean exception = false;
        int result = open_jfilechooser.showOpenDialog(contentpane);
        File file = open_jfilechooser.getSelectedFile();
        if (file != null && result == JFileChooser.APPROVE_OPTION) {
            if (open_jfilechooser.getFileFilter() == nhfilter || open_jfilechooser.getFileFilter() == nhxfilter) {
                try {
                    t = TreeHelper.readNHtree(file);
                } catch (Exception e) {
                    exception = true;
                    exceptionOccuredDuringOpenFile(e);
                }
            } else {
                try {
                    t = TreeHelper.readNHtree(file);
                } catch (Exception e) {
                    exception = true;
                    exceptionOccuredDuringOpenFile(e);
                }
            }
            if (!exception && t != null && !t.isRooted()) {
                exception = true;
                t = null;
                JOptionPane.showMessageDialog(this, "Species tree is not rooted.", "Species tree not loaded", JOptionPane.ERROR_MESSAGE);
            }
            if (!exception && t != null && !t.isCompletelyBinary()) {
                exception = true;
                t = null;
                JOptionPane.showMessageDialog(this, "Species tree is not completely binary.", "Species tree not loaded", JOptionPane.ERROR_MESSAGE);
            }
            if (!exception && t != null) {
                TreeHelper.cleanSpeciesNamesInExtNodes(t);
                String s = "";
                Vector v1 = t.getRoot().getAllExternalChildren(), v2 = new Vector();
                for (int i = 0; i < v1.size(); ++i) {
                    s = ((Node) v1.elementAt(i)).getSpecies();
                    if (s == null || s == "") {
                        exception = true;
                        t = null;
                        JOptionPane.showMessageDialog(this, "Species tree contains empty species fields.", "Species tree not loaded", JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                    v2.addElement(s);
                }
                for (int i = 0; i < v2.size(); ++i) {
                    s = (String) v2.elementAt(i);
                    if (v2.indexOf(s) != v2.lastIndexOf(s)) {
                        exception = true;
                        t = null;
                        JOptionPane.showMessageDialog(this, "Species tree contains duplicate species.", "Species tree not loaded", JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                }
            }
            if (!exception && t != null) {
                species_tree_ = t;
                JOptionPane.showMessageDialog(this, "Species tree successfully loaded.", "SDI", JOptionPane.INFORMATION_MESSAGE);
            }
            contentpane.repaint();
        }
    }

    private boolean isOKforSDI() {
        if (atvpanel.getATVgraphic().getTree() == null || atvpanel.getATVgraphic().getTree().isEmpty()) {
            return false;
        } else if (species_tree_ == null || species_tree_.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No species tree loaded.", "Error during SDI", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (!atvpanel.getATVgraphic().getTree().isCompletelyBinary()) {
            JOptionPane.showMessageDialog(this, "Gene tree is not completely binary.", "Error during SDI", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            return true;
        }
    }

    private boolean GAndSDoHaveMoreThanOneSpeciesInComman(Tree gene_tree) {
        if (gene_tree == null || gene_tree.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Gene tree and species tree have no species in common.", "Error during SDI", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (gene_tree.getNumberOfExtNodes() < 2) {
            JOptionPane.showMessageDialog(this, "Gene tree and species tree have only one species in common.", "Error during SDI", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            return true;
        }
    }

    private void about() {
        String about = "This Tree Viewer is derived from ATV (A Tree Viewer)\nVersion 1.92\n ";
        about = "A reduced set of options have been kept with no more possibilities to edit SDI tree format";
        about = "Only Newick form is supported in this phylojava version";
        about += "ATV Copyright (C) 1999-2002 Washington University School of Medicine\n";
        about += "and Howard Hughes Medical Institute\n";
        about += "All Rights Reserved\n";
        about += "Author: Christian M. Zmasek\n";
        about += "Last modified: 02/17/02\n";
        about += "Reference: Zmasek C.M. and Eddy S.R. Bioinformatics, 17, 383 (2001)\n";
        about += "For more information & download:\n";
        about += "http://www.genetics.wustl.edu/eddy/atv/\n";
        about += "Comments: zmasek@genetics.wustl.edu";
        JOptionPane.showMessageDialog(this, about, "ATV application (Java 1.2 or greater)", JOptionPane.PLAIN_MESSAGE);
    }

    private void help() {
        String help = "(Left) click on nodes of the tree to:\n";
        help += "o  Display and edit information of a node.\n";
        help += "    To edit information, box \"Editable\" needs to be checked.\n";
        help += "o  Collapse and uncollapse subtrees.\n";
        help += "o  Place a root in the middle of the parent branch.\n";
        help += "o  Display a subtree.\n";
        help += "    To go back to the parent tree, click on the root node of the subtree.\n";
        help += "o  Swap the children of a node (a pure cosmetic operation).\n\n";
        help += "Right clicking always displays the information of a node.\n\n";
        help += "\"SaveAs\" \"Save\" save the (sub)tree which is currently shown in the frame.\n";
        help += "\"Print\" prints the (sub)tree which is currently shown in the frame.\n\n";
        help += "General remarks:\n";
        help += "o  ATV can deal with trees with an arbitrary number of \n";
        help += "    children per parent.\n";
        help += "o  The application version allows to copy to the clipboard \n";
        help += "    in the \"View\"|\"View as ...\" frame (either by control-c or button press).\n";
        help += "o  Changes made to a subtree affect this subtree and its subtrees,\n";
        help += "    but not any of its parent tree(s).\n";
        JOptionPane.showMessageDialog(this, help, "Help", JOptionPane.PLAIN_MESSAGE);
    }

    private void exceptionOccuredDuringOpenFile(Exception e) {
        JOptionPane.showMessageDialog(this, "Exception: " + e, "Error during File|Open", JOptionPane.ERROR_MESSAGE);
    }

    private void exceptionOccuredDuringSaveAs(Exception e) {
        JOptionPane.showMessageDialog(this, "Exception" + e, "Error during File|SaveAs", JOptionPane.ERROR_MESSAGE);
    }

    private void customizeJMenuItem(JMenuItem jmi) {
        jmi.setFont(menu_font);
        jmi.setBackground(menu_background_color);
        jmi.setForeground(menu_text_color);
        jmi.addActionListener(this);
    }

    private void removeatvtextframe() {
        if (atvtextframe != null) {
            atvtextframe.close();
            atvtextframe = null;
        }
    }
}

class NHFilter extends FileFilter {

    public boolean accept(File f) {
        return f.getName().trim().toLowerCase().endsWith(".nh") || f.isDirectory();
    }

    public String getDescription() {
        return "NH (*.nh)";
    }
}

class NHXFilter extends FileFilter {

    public boolean accept(File f) {
        return f.getName().trim().toLowerCase().endsWith(".nhx") || f.isDirectory();
    }

    public String getDescription() {
        return "NHX (*.nhx)";
    }
}
