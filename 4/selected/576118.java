package desktop;

import java.awt.event.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.swing.*;
import org.wings.*;
import org.wings.io.Device;
import org.wings.io.ServletDevice;
import org.wings.servlet.*;
import org.wings.session.*;
import org.wings.util.*;
import org.wings.event.SContainerListener;
import org.wings.event.SContainerEvent;

/**
 * TODO: documentation
 *
 * @author <a href="mailto:haaf@mercatis.de">Armin Haaf</a>
 * @version $Revision: 927 $
 */
public class DesktopSession extends SessionServlet implements SConstants {

    SDesktopPane desktop;

    SMenu windowMenu;

    int editorNumber;

    public DesktopSession(Session session) {
        super(session);
        System.out.println("new DesktopSession");
    }

    public void postInit(ServletConfig config) {
        initGUI();
    }

    void initGUI() {
        SContainer contentPane = getFrame().getContentPane();
        editorNumber = 0;
        SMenuBar menuBar = createMenu();
        contentPane.add(menuBar);
        desktop = new SDesktopPane();
        desktop.addContainerListener(new SContainerListener() {

            public void componentAdded(SContainerEvent e) {
                SInternalFrame frame = (SInternalFrame) e.getChild();
                SMenuItem windowItem = new WindowMenuItem(desktop, frame);
                windowMenu.add(windowItem);
            }

            public void componentRemoved(SContainerEvent e) {
                SInternalFrame frame = (SInternalFrame) e.getChild();
                SMenuItem windowItem = new WindowMenuItem(frame);
                windowMenu.remove(windowItem);
            }
        });
        contentPane.add(desktop);
        newEditor().setText(getStory());
    }

    protected String getStory() {
        return "Ein Philosoph ist jemand, der in einem absolut dunklen Raum " + "mit verbundenen Augen nach einer schwarzen Katze sucht, die gar nicht " + "da ist. Ein Theologe ist jemand der genau das gleiche macht und ruft: " + "\"ich hab sie!\"";
    }

    protected SMenuBar createMenu() {
        SMenuItem newItem = new SMenuItem("New");
        newItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                newEditor();
            }
        });
        SMenuItem openItem = new SMenuItem("Open");
        openItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                openEditor();
            }
        });
        SMenu fileMenu = new SMenu("File");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        windowMenu = new SMenu("Window");
        SMenuBar menuBar = new SMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(windowMenu);
        return menuBar;
    }

    /**
     * Servletinfo
     */
    public String getServletInfo() {
        return "Desktop ($Revision: 927 $)";
    }

    /**
     * add a new frame. If there is a frame, that is maximized, then
     * move the maximization to the new frame.
     */
    public void addNewFrame(final SInternalFrame frame) {
        desktop.add(frame, 0);
        try {
            desktop.invite(new ComponentVisitor() {

                public void visit(SComponent c) {
                }

                public void visit(SContainer c) {
                    if (!(c instanceof SInternalFrame)) return;
                    SInternalFrame ff = (SInternalFrame) c;
                    if (ff != frame && ff.isMaximized()) {
                        ff.setMaximized(false);
                        frame.setMaximized(true);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public Editor newEditor() {
        Editor editor = new Editor();
        editor.setTitle("Editor [" + (++editorNumber) + "]");
        addNewFrame(editor);
        return editor;
    }

    public Editor openEditor() {
        final Editor editor = new Editor();
        addNewFrame(editor);
        final SFileChooser chooser = new SFileChooser();
        chooser.setColumns(20);
        final SOptionPane dialog = new SOptionPane();
        dialog.setEncodingType("multipart/form-data");
        dialog.showInput(editor, "Choose file", chooser, "Open file");
        dialog.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if (evt.getActionCommand() == SOptionPane.OK_ACTION) {
                    try {
                        File file = chooser.getFile();
                        Reader reader = new FileReader(file);
                        StringWriter writer = new StringWriter();
                        int b;
                        while ((b = reader.read()) >= 0) writer.write(b);
                        editor.setText(writer.toString());
                        editor.setTitle(chooser.getFileName());
                        chooser.reset();
                    } catch (Exception e) {
                        dialog.show(editor);
                        SOptionPane.showMessageDialog(editor, "Error opening file", e.getMessage());
                    }
                } else {
                    editor.close();
                }
            }
        });
        return editor;
    }

    /**
     * A menu item, that handles the position of an internal frame within
     * the desktop - whenever it is clicked, the frame is put on top. This
     * MenuItem is added in some component listener, that gets activated
     * whenever a window is added or removed from the desktop.
     */
    private static class WindowMenuItem extends SMenuItem {

        private final SInternalFrame frame;

        public WindowMenuItem(SInternalFrame f) {
            frame = f;
        }

        public WindowMenuItem(final SDesktopPane d, final SInternalFrame f) {
            frame = f;
            addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    d.setPosition(f, 0);
                    if (f.isIconified()) {
                        f.setIconified(false);
                    }
                    try {
                        d.invite(new ComponentVisitor() {

                            public void visit(SComponent c) {
                            }

                            public void visit(SContainer c) {
                                if (!(c instanceof SInternalFrame)) return;
                                SInternalFrame ff = (SInternalFrame) c;
                                if (ff != frame && ff.isMaximized()) {
                                    ff.setMaximized(false);
                                    frame.setMaximized(true);
                                }
                            }
                        });
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
            });
        }

        /**
         * returns the title of the frame.
         */
        public String getText() {
            String title = frame.getTitle();
            return (title == null || title.length() == 0) ? "[noname]" : title;
        }

        /**
         * remove menu item by frame ..
         */
        public boolean equals(Object o) {
            if (o instanceof WindowMenuItem) {
                WindowMenuItem wme = (WindowMenuItem) o;
                return (frame != null && wme.frame == frame);
            }
            return false;
        }
    }
}
