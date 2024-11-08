package chanukah;

import chanukah.pd.Entity;
import chanukah.pd.Group;
import chanukah.pd.Person;
import chanukah.storage.ChanukahFileFilter;
import chanukah.storage.ChanukahStorage;
import chanukah.ui.MainView;
import chanukah.ui.dialogs.AboutChanukahDialog;
import chanukah.ui.dialogs.GenericChanukahDialog;
import chanukah.ui.model.ChanukahTreeModel;
import chanukah.util.ImageLoader;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.WindowConstants;
import javax.swing.tree.TreePath;

/**
 * <p></p>
 * <p>
 * $Id: Chanukah.java,v 1.27 2004/02/26 22:39:09 phuber Exp $
 * </p>
 *
 * @author Patrick Huber |<a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
 * @version $Revision: 1.27 $
 */
public class Chanukah {

    /** Part of the window frame title */
    private static final String windowTitle = "Chanukah Address Management System";

    /** Treemodel for the JTree used within the leftview */
    private ChanukahTreeModel treeModel = null;

    /** Root group of all PD objects */
    private Group rootGroup = null;

    /** Main view, jframe */
    private MainView mainView = null;

    /** Default location where data is stored */
    private String dataFile = "chanukahData.xml";

    /**
	 * Default Constructor, starts Application
	 */
    public Chanukah() {
        super();
        checkClassPath();
        String userHome = System.getProperty("user.home");
        String pathSep = System.getProperty("file.separator");
        String chanukahDir = ".chanukah";
        String fileName = dataFile;
        this.dataFile = userHome + pathSep + chanukahDir + pathSep + fileName;
        File file = new File(this.dataFile);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        this.rootGroup = ChanukahStorage.loadData(dataFile);
        if (rootGroup == null) {
            rootGroup = new Group("Contacts", "");
            new GenericChanukahDialog(mainView, "Could not load default File " + dataFile + " Starting with empty Addressbook", true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_INFORMATION).show();
        }
        this.treeModel = new ChanukahTreeModel(rootGroup);
        mainView = new MainView(treeModel);
        setTitle();
        mainView.setIconImage(new ImageIcon(ImageLoader.getInstance().getIMAGE_TREE_LEAF()).getImage());
        mainView.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mainView.addQuitActionListener(new QuitApplicationListener());
        mainView.addLoadDataActionListener(new LoadDataApplicationListener());
        mainView.addSaveDataActionListener(new SaveDataApplicationListener());
        mainView.addSaveDataAsActionListener(new SaveDataAsApplicationListener());
        mainView.addPersonAddActionListener(new AddPersonApplicationListener());
        mainView.addPersonDeleteActionListener(new DeletePersonApplicationListener());
        mainView.addGroupAddActionListener(new AddGroupApplicationListener());
        mainView.addGroupDeleteActionListener(new DeleteGroupApplicationListener());
        mainView.addAboutChanukahActionListener(new AboutChanukahApplicationListener());
        mainView.addWindowListener(new ChanukahWindowListener());
        mainView.show();
    }

    /**
	 * Start application
	 *
	 * @param args
	 */
    public static void main(String[] args) {
        new Chanukah();
    }

    /**
	 * Set the title of the mainView
	 */
    private void setTitle() {
        File f = new File(dataFile);
        String dataFileName = f.getName();
        mainView.setTitle(dataFileName + " - " + windowTitle);
    }

    /**
	 * Check if all jars required are present
	 */
    private void checkClassPath() {
        try {
            Class.forName("org.w3c.dom.Document");
        } catch (ClassNotFoundException e) {
            String msg = "xmlParserAPIs.jar does not seem to be in the classpath: " + e.getMessage();
            System.out.println(msg);
            throw new AssertionError(msg);
        }
        try {
            Class.forName("org.apache.xml.serialize.XMLSerializer");
        } catch (ClassNotFoundException e) {
            String msg = "xercesImpl.jar does not seem to be in the classpath: " + e.getMessage();
            System.out.println(msg);
            throw new AssertionError(msg);
        }
        try {
            Class.forName("com.toedter.calendar.JCalendar");
        } catch (ClassNotFoundException e) {
            String msg = "jcalendar.jar does not seem to be in the classpath: " + e.getMessage();
            System.out.println(msg);
            throw new AssertionError(msg);
        }
    }

    /**
	 * Exits the Application and stores all data
	 */
    private void exitApp() {
        Group root = mainView.getRoot();
        ChanukahStorage.saveData(root, dataFile);
    }

    /**
	 * <p>Listener for MenuBar, Displays About Chanukah Dialog</p>
	 * @author Manuel Baumgartner  | <a href="mailto:m1baumga@hsr.ch">m1baumga@hsr.ch</a>
	 */
    private class AboutChanukahApplicationListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            new AboutChanukahDialog(mainView).show();
        }
    }

    /**
	 * <p>Listener for MenuBar, adds a Group</p>
	 * @author Patrick Huber |<a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
	 */
    private class AddGroupApplicationListener implements ActionListener {

        /**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
        public void actionPerformed(ActionEvent event) {
            Entity lastSelectedEntity = (Entity) mainView.getTree().getLastSelectedPathComponent();
            if (lastSelectedEntity == null) {
                lastSelectedEntity = (Entity) treeModel.getRoot();
            }
            if (lastSelectedEntity.getClass() == Group.class) {
                Group parent = (Group) lastSelectedEntity;
                Group child = new Group("new group", "new group's description");
                ArrayList childPath = treeModel.addGroup(parent, child);
                mainView.getTree().setSelectionPath(new TreePath(childPath.toArray()));
                mainView.displayEntity(child);
            } else {
                new GenericChanukahDialog(mainView, "Cannot add Group as child of a non-Group", true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_INFORMATION).show();
            }
        }
    }

    /**
	 * <p>Listener for MenuBar, adds a Person</p>
	 * @author Patrick Huber |<a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
	 */
    private class AddPersonApplicationListener implements ActionListener {

        /**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
        public void actionPerformed(ActionEvent event) {
            Entity lastSelectedEntity = (Entity) mainView.getTree().getLastSelectedPathComponent();
            if (lastSelectedEntity == null) {
                lastSelectedEntity = (Entity) treeModel.getRoot();
            }
            if (lastSelectedEntity.getClass() == Group.class) {
                Group parent = (Group) lastSelectedEntity;
                Person child = new Person("", "First Name", "Middle Name", "Last Name", "", new Date());
                ArrayList childPath = treeModel.addPerson(parent, child);
                mainView.getTree().setSelectionPath(new TreePath(childPath.toArray()));
                mainView.displayEntity(child);
            } else {
                new GenericChanukahDialog(mainView, "Cannot add Person as child of a non-Group", true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_INFORMATION).show();
            }
        }
    }

    /**
	 * <p>Window Listener, acts on window events</p>
	 * @author Patrick Huber |<a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
	 */
    private class ChanukahWindowListener implements WindowListener {

        /**
		 * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
		 */
        public void windowActivated(WindowEvent e) {
        }

        /**
		 * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
		 */
        public void windowClosed(WindowEvent e) {
        }

        /**
		 * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
		 */
        public void windowClosing(WindowEvent e) {
            exitApp();
        }

        /**
		 * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
		 */
        public void windowDeactivated(WindowEvent e) {
        }

        /**
		 * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
		 */
        public void windowDeiconified(WindowEvent e) {
        }

        /**
		 * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
		 */
        public void windowIconified(WindowEvent e) {
        }

        /**
		 * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
		 */
        public void windowOpened(WindowEvent e) {
        }
    }

    /**
	 * <p>Listener for MenuBar, deletes a Group</p>
	 * @author Patrick Huber |<a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
	 */
    private class DeleteGroupApplicationListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            Entity lastSelectedEntity = (Entity) mainView.getTree().getLastSelectedPathComponent();
            if (lastSelectedEntity == null) {
                new GenericChanukahDialog(mainView, "No Group is selected", true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_INFORMATION).show();
                return;
            }
            if (lastSelectedEntity.getClass() == Group.class) {
                Group child = (Group) lastSelectedEntity;
                Group parent = (Group) child.getParent();
                treeModel.removeGroup(parent, child);
                mainView.displayEntity(parent);
                mainView.getTree().setSelectionPath(new TreePath(parent.getPath().toArray()));
            } else {
                new GenericChanukahDialog(mainView, "The selected Item is not a Group", true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_INFORMATION).show();
            }
        }
    }

    /**
	 * <p>Listener for MenuBar, deletes a Person</p>
	 * @author Patrick Huber |<a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
	 */
    private class DeletePersonApplicationListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            Entity lastSelectedEntity = (Entity) mainView.getTree().getLastSelectedPathComponent();
            if (lastSelectedEntity == null) {
                new GenericChanukahDialog(mainView, "No Person is selected", true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_INFORMATION).show();
                return;
            }
            if (lastSelectedEntity.getClass() == Person.class) {
                Person child = (Person) lastSelectedEntity;
                Group parent = (Group) child.getParent();
                treeModel.removePerson(parent, child);
                mainView.displayEntity(parent);
                mainView.getTree().setSelectionPath(new TreePath(parent.getPath().toArray()));
            } else {
                new GenericChanukahDialog(mainView, "The selected Item is not a Person", true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_INFORMATION).show();
            }
        }
    }

    /**
	 * <p>Listener for MenuBar, Loads Data</p>
	 * @author Patrick Huber |<a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
	 */
    private class LoadDataApplicationListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            JFileChooser fileDialog = new JFileChooser(dataFile);
            ChanukahFileFilter filter = new ChanukahFileFilter("xml", "Chanukah Address Database");
            fileDialog.addChoosableFileFilter(filter);
            int status = fileDialog.showOpenDialog(mainView);
            if (status == JFileChooser.APPROVE_OPTION) {
                dataFile = fileDialog.getSelectedFile().getPath();
                rootGroup = ChanukahStorage.loadData(dataFile);
                if (rootGroup != null) {
                    mainView.setRoot(rootGroup);
                    treeModel = new ChanukahTreeModel(rootGroup);
                    mainView.getTree().setModel(treeModel);
                    mainView.displayEntity(null);
                } else {
                    mainView.setRoot(null);
                    treeModel = null;
                    mainView.getTree().setModel(treeModel);
                    mainView.displayEntity(null);
                    new GenericChanukahDialog(mainView, "Couldn't load " + dataFile, true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_ERROR).show();
                }
            }
            setTitle();
        }
    }

    /**
	 * <p>Listener for MenuBar, Quits Application</p>
	 * @author Patrick Huber |<a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
	 */
    private class QuitApplicationListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            mainView.hide();
            exitApp();
            mainView.dispose();
        }
    }

    /**
	 * <p></p>
	 * @author Patrick Huber  | <a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
	 */
    private class SaveDataApplicationListener implements ActionListener {

        /**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
        public void actionPerformed(ActionEvent e) {
            if (ChanukahStorage.saveData(rootGroup, dataFile)) {
            } else {
                new GenericChanukahDialog(mainView, "Couldn't save " + dataFile, true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_ERROR).show();
            }
            setTitle();
        }
    }

    /**
	 * <p>Listener for MenuBar, Saves Data</p>
	 * @author Patrick Huber |<a href="mailto:phuber@users.sf.net">phuber@users.sf.net</a>
	 */
    private class SaveDataAsApplicationListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            JFileChooser fileDialog = new JFileChooser(dataFile);
            ChanukahFileFilter filter = new ChanukahFileFilter("xml", "Chanukah Address Database");
            fileDialog.addChoosableFileFilter(filter);
            int status = fileDialog.showSaveDialog(mainView);
            if (status == JFileChooser.APPROVE_OPTION) {
                String file = fileDialog.getSelectedFile().getAbsolutePath();
                if ((fileDialog.getFileFilter().getClass() == ChanukahFileFilter.class) && (file.charAt(file.length() - 4) != '.')) {
                    file += ("." + ((ChanukahFileFilter) fileDialog.getFileFilter()).getExtension());
                }
                if (new File(file).exists()) {
                    GenericChanukahDialog dialog = new GenericChanukahDialog(mainView, "The selected file exists already, Do you want to overwrite it?", true, GenericChanukahDialog.BUTTONS.BUTTON_YES | GenericChanukahDialog.BUTTONS.BUTTON_NO, GenericChanukahDialog.TYPES.TYPE_QUESTION);
                    dialog.show();
                    if (dialog.getStatus() == GenericChanukahDialog.BUTTONS.BUTTON_NO) {
                        return;
                    }
                }
                dataFile = file;
                if (ChanukahStorage.saveData(rootGroup, dataFile)) {
                } else {
                    new GenericChanukahDialog(mainView, "Couldn't save " + dataFile, true, GenericChanukahDialog.BUTTONS.BUTTON_OK, GenericChanukahDialog.TYPES.TYPE_ERROR).show();
                }
            }
            setTitle();
        }
    }
}
