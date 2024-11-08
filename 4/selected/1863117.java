package desktop;

import org.wings.*;
import org.wings.externalizer.ExternalizeManager;
import org.wings.resource.FileResource;
import org.wings.script.JavaScriptListener;
import org.wings.script.ScriptListener;
import org.wings.session.ScriptManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The Desktop example demonstrates the use of internal frames as well as
 * file upload and download.
 * SInternalFrames work very similar to their Swing pendants. The file upload
 * is left to the SFileChooser. Beware, that if you use one or more input
 * elements of type="file", you have to set the encoding of the surrounding form
 * to "multipart/form-data". This is a requirement of html. Download is a bit
 * tricky. The text has to be externalized for example by using the class
 * {@see org.wings.FileResource}. A JavaScriptListener, that is hooked to the
 * java script event "onload", is installed in the frame.
 * Look at the source, especially the method "save".
 * <p/>
 * As of now, the menu item "save" in the "file" menu does not work as expected.
 * It is rendered as a href outside the form. Changes to text area don't take
 * effect. We could use javascript again, to trigger the required form submit.
 *
 * @author Holger Engels
 */
public class EditorPanel extends SPanel {

    private SMenuBar menuBar;

    private SToolBar toolBar;

    private STextArea textArea;

    private String backup;

    private static org.wings.util.SessionLocal<String> clip = new org.wings.util.SessionLocal<String>() {

        @Override
        protected String initialValue() {
            return "";
        }
    };

    private static org.wings.util.SessionLocal<Integer> editorNr = new org.wings.util.SessionLocal<Integer>() {

        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    public EditorPanel() {
        setName("editorpanel" + editorNr.get().toString());
        editorNr.set(editorNr.get() + 1);
        menuBar = createMenu();
        setLayout(new SBorderLayout());
        add(menuBar, SBorderLayout.NORTH);
        toolBar = createToolBar();
        textArea = new STextArea();
        textArea.setColumns(500);
        textArea.setRows(12);
        textArea.setPreferredSize(SDimension.FULLWIDTH);
        textArea.setEditable(true);
        SForm form = new SForm(new SFlowDownLayout());
        form.add(toolBar);
        form.add(textArea);
        add(form, SBorderLayout.CENTER);
    }

    public static void resetEditorNo() {
        editorNr.set(0);
    }

    protected SMenuBar createMenu() {
        SMenuItem saveItem = new SMenuItem("Save");
        saveItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                save();
            }
        });
        SMenuItem revertItem = new SMenuItem("Revert");
        revertItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                revert();
            }
        });
        SMenuItem closeItem = new SMenuItem("Close");
        closeItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                close();
            }
        });
        SMenu fileMenu = new SMenu("File");
        fileMenu.add(saveItem);
        fileMenu.add(revertItem);
        fileMenu.add(closeItem);
        SMenuItem cutItem = new SMenuItem("Cut");
        cutItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                cut();
            }
        });
        SMenuItem copyItem = new SMenuItem("Copy");
        copyItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                copy();
            }
        });
        SMenuItem pasteItem = new SMenuItem("Paste");
        pasteItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                paste();
            }
        });
        SMenu editMenu = new SMenu("Edit");
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        SMenuBar menuBar = new SMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        return menuBar;
    }

    protected SToolBar createToolBar() {
        try {
            SButton saveButton = new SButton(new SURLIcon("../icons/filesave.png"));
            saveButton.setToolTipText("save");
            saveButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    save();
                }
            });
            SButton revertButton = new SButton(new SURLIcon("../icons/filerevert.png"));
            revertButton.setToolTipText("revert");
            revertButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    revert();
                }
            });
            SButton closeButton = new SButton(new SURLIcon("../icons/fileclose.png"));
            closeButton.setToolTipText("close");
            closeButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    close();
                }
            });
            SButton cutButton = new SButton(new SURLIcon("../icons/editcut.png"));
            cutButton.setToolTipText("cut");
            cutButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    cut();
                }
            });
            SButton copyButton = new SButton(new SURLIcon("../icons/editcopy.png"));
            copyButton.setToolTipText("copy");
            copyButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    copy();
                }
            });
            SButton pasteButton = new SButton(new SURLIcon("../icons/editpaste.png"));
            pasteButton.setToolTipText("paste");
            pasteButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    paste();
                }
            });
            SToolBar toolBar = new SToolBar();
            toolBar.add(saveButton);
            toolBar.add(revertButton);
            toolBar.add(closeButton);
            toolBar.add(new SLabel("<html>&nbsp;"));
            toolBar.add(cutButton);
            toolBar.add(copyButton);
            toolBar.add(pasteButton);
            return toolBar;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
        return new SToolBar();
    }

    public STextArea getTextArea() {
        return this.textArea;
    }

    public int getEditorNr() {
        return editorNr.get().intValue();
    }

    public void setText(String text) {
        String oldVal = textArea.getText();
        textArea.setText(text);
        propertyChangeSupport.firePropertyChange("text", oldVal, textArea.getText());
    }

    public String getText() {
        return textArea.getText();
    }

    public void setBackup(String backup) {
        String oldVal = this.backup;
        this.backup = backup;
        propertyChangeSupport.firePropertyChange("backup", oldVal, this.backup);
    }

    public String getBackup() {
        return backup;
    }

    public void save() {
        try {
            File file = File.createTempFile("wings", ".txt");
            PrintWriter out = new PrintWriter(new FileOutputStream(file));
            out.print(textArea.getText());
            out.close();
            FileResource resource = new FileResource(file);
            resource.setExternalizerFlags(resource.getExternalizerFlags() | ExternalizeManager.REQUEST);
            Map headers = new HashMap();
            headers.put("Content-Disposition", "attachment; filename=" + file.getName());
            resource.setHeaders(headers.entrySet());
            final ScriptListener listener = new JavaScriptListener(null, null, "location.href='" + resource.getURL() + "'");
            ScriptManager.getInstance().addScriptListener(listener);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public void revert() {
        textArea.setText(backup);
    }

    public void close() {
    }

    public void cut() {
        clip.set(textArea.getText());
        textArea.setText("");
    }

    public void copy() {
        clip.set(textArea.getText());
    }

    public void paste() {
        if (clip != null) {
            textArea.setText(textArea.getText() + clip.get());
        }
    }

    public String openFile(File file) {
        try {
            Reader reader = new FileReader(file);
            StringWriter writer = new StringWriter();
            int b;
            while ((b = reader.read()) >= 0) writer.write(b);
            setText(writer.toString());
            reader.close();
            return writer.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
