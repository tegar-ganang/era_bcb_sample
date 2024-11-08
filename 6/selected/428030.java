package org.xamp.gui;

import java.util.ArrayList;
import java.util.Iterator;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.xamp.gui.xmpp.Connection;
import org.xamp.gui.xmpp.ContactList;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QWidget;

public class MainWindow extends QMainWindow {

    private QMenu fileMenu;

    private QMenu helpMenu;

    private QAction exitAct;

    private QAction aboutAct;

    public static void main(String[] args) {
        QApplication.initialize(args);
        MainWindow testMainWindow = new MainWindow(null);
        testMainWindow.show();
        QApplication.exec();
    }

    public MainWindow(QWidget parent) {
        super(parent);
        createActions();
        createMenus();
        createContents();
    }

    private void createActions() {
        exitAct = new QAction(tr("E&xit"), this);
        exitAct.setShortcut(tr("Ctrl+Q"));
        exitAct.setStatusTip(tr("Exit the application"));
        exitAct.triggered.connect(this, "close()");
        aboutAct = new QAction(tr("&About"), this);
        aboutAct.setStatusTip(tr("Show the application's About box"));
        aboutAct.triggered.connect(this, "about()");
    }

    private void createMenus() {
        fileMenu = menuBar().addMenu(tr("&File"));
        fileMenu.addAction(exitAct);
        helpMenu = menuBar().addMenu(tr("&Help"));
        helpMenu.addAction(aboutAct);
    }

    private void createContents() {
        setWindowTitle("XAMP");
        createContactWidget();
    }

    private void createContactWidget() {
        Connection connection = new Connection("jabber.org");
        ContactList contactList = connection.getContactList();
        if (connection.connect()) {
            if (connection.login("safroe", "fa234sdk", null)) {
                Roster roster = connection.getXmppConnection().getRoster();
                ArrayList<String> groupList = contactList.getGroupList();
                QTreeWidget treeWidget = new QTreeWidget(this);
                treeWidget.setColumnCount(1);
                treeWidget.setHeaderLabel("Kontakte");
                for (int i = 0; i < groupList.size(); i++) {
                    QTreeWidgetItem groupItem = new QTreeWidgetItem();
                    String groupName = groupList.get(i);
                    groupItem.setText(0, groupName);
                    RosterGroup group = roster.getGroup(groupName);
                    Iterator<RosterEntry> entries = group.getEntries().iterator();
                    while (entries.hasNext()) {
                        RosterEntry entry = entries.next();
                        QTreeWidgetItem entryItem = new QTreeWidgetItem(groupItem);
                        entryItem.setText(0, entry.getName());
                    }
                    treeWidget.addTopLevelItem(groupItem);
                }
                treeWidget.expandAll();
                treeWidget.addAction(null);
                treeWidget.clicked.connect(this, "newChat()");
                setCentralWidget(treeWidget);
            }
        }
    }

    public void newChat() {
    }

    protected void about() {
        QMessageBox.information(this, "Info", "It's your turn now :-)");
    }
}
