package com.gcapmedia.dab.epg.ui;

import java.awt.BorderLayout;
import java.awt.MenuBar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import com.gcapmedia.dab.epg.Epg;
import com.gcapmedia.dab.epg.MarshallException;
import com.gcapmedia.dab.epg.binary.EpgBinaryMarshaller;
import com.jgoodies.plaf.windows.ExtWindowsLookAndFeel;

public class EpgStudio extends JFrame implements ActionListener {

    /**
	 * Serial version
	 */
    private static final long serialVersionUID = -3516049878822792399L;

    private Epg epg;

    private EpgViewerPanel viewer;

    /**
	 * @param args
	 * @throws UnsupportedLookAndFeelException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 */
    public static void main(String[] args) throws MarshallException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new ExtWindowsLookAndFeel());
        EpgStudio app = new EpgStudio();
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        app.setSize(800, 800);
        app.setVisible(true);
    }

    public EpgStudio() throws MarshallException {
        epg = new Epg();
        setLayout(new BorderLayout());
        JMenuBar bar = createMenuBar();
        setJMenuBar(bar);
        JToolBar toolbar = createToolBar();
        viewer = new EpgViewerPanel(epg);
        add(viewer, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        bar.add(fileMenu);
        JMenuItem loadItem = new JMenuItem("Load");
        loadItem.addActionListener(this);
        fileMenu.add(loadItem);
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(this);
        fileMenu.add(saveItem);
        fileMenu.add(new JSeparator());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(this);
        fileMenu.add(exitItem);
        JMenu helpMenu = new JMenu("Help");
        bar.add(helpMenu);
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(this);
        helpMenu.add(aboutItem);
        return bar;
    }

    private JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        JButton newButton = new JButton("New");
        toolbar.add(newButton);
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(this);
        toolbar.add(loadButton);
        JButton saveButton = new JButton("Save");
        toolbar.add(saveButton);
        return toolbar;
    }

    /**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals("Load")) {
            JFileChooser fc = new JFileChooser();
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = fc.getSelectedFile();
                    System.out.println("Loading file: " + file);
                    EpgBinaryMarshaller marshaller = new EpgBinaryMarshaller();
                    FileChannel channel = new FileInputStream(file).getChannel();
                    ByteBuffer buf = ByteBuffer.allocate((int) channel.size());
                    channel.read(buf);
                    System.out.println("Unmarshalling data");
                    epg = marshaller.unmarshall(buf.array());
                    System.out.println("Finished");
                    viewer.refresh(epg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (event.getActionCommand().equals("Save")) {
        } else if (event.getActionCommand().equals("Exit")) {
            this.dispose();
        } else if (event.getActionCommand().equals("About")) {
            JDialog dialog = new JDialog(this, "About EPG Studio", true);
            dialog.setSize(400, 250);
            AboutPanel panel = new AboutPanel();
            dialog.add(panel);
            dialog.setVisible(true);
        }
    }
}
