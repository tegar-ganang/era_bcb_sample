package org.easyway.editor.forms.components.extended;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.easyway.editor.forms.UEditor;
import org.easyway.editor.forms.components.Group;
import org.easyway.editor.forms.components.InputText;
import org.easyway.editor.forms.components.interfaces.IValidity;
import org.easyway.objects.Camera;
import org.easyway.system.Core;

public class LoaderButton extends JPanel implements IValidity {

    static String dir;

    static {
        dir = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        if (Core.getOS() == 0) {
            if (dir.charAt(0) == '/') dir = dir.substring(1);
        }
        dir = dir.replace("%20", " ");
        System.out.println("directory: " + dir);
    }

    private static final long serialVersionUID = -8499445364325771438L;

    static Camera getTextureCamera = new Camera(-10000, -10000, 800, 600);

    String name;

    String filename;

    ArrayList<Group> groups;

    Camera oldCamera;

    JLabel label;

    JButton button;

    public LoaderButton(String label, String name) {
        super();
        this.name = name;
        GridLayout gridLayout = new GridLayout();
        gridLayout.setRows(1);
        gridLayout.setColumns(2);
        setLayout(gridLayout);
        this.setSize(300, 200);
        groups = new ArrayList<Group>(10);
        Group.AllObjects.add(this);
        this.label = new JLabel();
        this.label.setText(label);
        this.label.setIcon(InputText.iconX);
        button = new JButton("load data");
        button.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                new Thread() {

                    public void run() {
                        command();
                    }
                }.start();
            }
        });
        final Color oldColor = button.getBackground();
        final Color greenColor = new Color(255, 128, 128);
        final Color blackColor = new Color(0, 0, 0);
        MouseListener ml = new MouseListener() {

            public void mouseClicked(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
                button.setForeground(greenColor);
                setBackground(greenColor);
            }

            public void mouseExited(MouseEvent e) {
                button.setForeground(blackColor);
                setBackground(oldColor);
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }
        };
        addMouseListener(ml);
        button.addMouseListener(ml);
        add(this.label, null);
        add(button, null);
        UEditor.getLeftPanel().add(this);
    }

    public void command() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(dir));
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filename = chooser.getSelectedFile().getAbsolutePath();
            String f2 = "";
            for (int i = 0; i < filename.length(); ++i) {
                if (filename.charAt(i) != '\\') {
                    f2 = f2 + filename.charAt(i);
                } else f2 = f2 + '/';
            }
            filename = f2;
            if (filename.contains(dir)) {
                filename = filename.substring(dir.length());
            } else {
                try {
                    FileChannel srcFile = new FileInputStream(filename).getChannel();
                    FileChannel dstFile;
                    filename = "ueditor_files/" + chooser.getSelectedFile().getName();
                    File newFile;
                    if (!(newFile = new File(dir + filename)).createNewFile()) {
                        dstFile = new FileInputStream(dir + filename).getChannel();
                        newFile = null;
                    } else {
                        dstFile = new FileOutputStream(newFile).getChannel();
                    }
                    dstFile.transferFrom(srcFile, 0, srcFile.size());
                    srcFile.close();
                    dstFile.close();
                    System.out.println("file copyed to: " + dir + filename);
                } catch (Exception e) {
                    e.printStackTrace();
                    label.setIcon(InputText.iconX);
                    filename = null;
                    for (Group g : groups) {
                        g.updateValidity(true);
                    }
                    return;
                }
            }
            label.setIcon(InputText.iconV);
            for (Group g : groups) {
                g.updateValidity(true);
            }
        }
    }

    public boolean getValidity() {
        return filename != null;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return filename;
    }

    public ArrayList<Group> getGroups() {
        return groups;
    }
}
