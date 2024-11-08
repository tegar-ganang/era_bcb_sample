package com.explosion.datastream.gui.querywriter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import com.explosion.datastream.EXQLConstants;
import com.explosion.datastream.EXQLModuleManager;
import com.explosion.datastream.gui.EXQLBaseTool;
import com.explosion.datastream.gui.SQLTextActionsListener;
import com.explosion.datastream.gui.transferable.DBEntityDataFlavor;
import com.explosion.expf.dragdrop.DropTargetListenerPlugin;
import com.explosion.expfmodules.dbstore.DbStoreModuleManager;
import com.explosion.expfmodules.rdbmsconn.dbom.DBEntity;
import com.explosion.expfmodules.rdbmsconn.dbom.sql.SingleTableSelectStatement;
import com.explosion.expfmodules.rdbmsconn.dbom.utils.SQLFormatter;
import com.explosion.expfmodules.rdbmsconn.dbom.utils.Transaction;
import com.explosion.expfmodules.texteditor.LineNumberTextEditor;
import com.explosion.utilities.GeneralConstants;
import com.explosion.utilities.exception.ExceptionManagerFactory;

public class SQLCommandController extends JPanel implements DropTargetListenerPlugin {

    private static Logger log = LogManager.getLogger(SQLCommandController.class);

    private static final int MODE_EDIT = 0;

    private static final int MODE_BROWSE_HISTORY = 1;

    private int mode = MODE_EDIT;

    private LineNumberTextEditor textComponent;

    private EXQLBaseTool tool;

    private JTextComponent commandTextComponent;

    private JTextArea marginTextArea;

    private Vector commandVector = new Vector();

    private int commandVectorIndex = 0;

    private int commandNumber = 1;

    private Connection conn;

    private String connectionName;

    private MouseAdapter adapter;

    /**
   * Constructor
   */
    public SQLCommandController(Connection conn, String connectionName, EXQLBaseTool tool) throws Exception {
        this.conn = conn;
        this.tool = tool;
        this.connectionName = connectionName;
        this.textComponent = new LineNumberTextEditor(tool, "SQL>", false);
        SQLTextActionsListener listener = new SQLTextActionsListener(textComponent.getEditorTextComponent(), tool);
        this.commandTextComponent = textComponent.getEditorTextComponent();
        this.marginTextArea = textComponent.getMarginTextArea();
        init();
    }

    /**
   * Sets up the GUI
   */
    private void init() throws Exception {
        this.setLayout(new BorderLayout());
        this.add(textComponent, BorderLayout.CENTER);
        commandTextComponent.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                commandTextArea_keyTyped(e);
            }

            public void keyPressed(KeyEvent e) {
                commandTextArea_keyPressed(e);
            }
        });
        loadCommandHistory();
        showHelpText();
        textComponent.getEditorTextComponent().addDropTargetPlugin(this);
        this.addComponentListener(new ComponentAdapter() {

            /**
         * Invoked when the component has been made visible.
         */
            public void componentShown(ComponentEvent e) {
                textComponent.getEditorTextComponent().requestFocus();
                textComponent.getEditorTextComponent().requestFocusInWindow();
            }
        });
    }

    /**
   * Shows the help text
   *
   */
    private void showHelpText() {
        commandTextComponent.setText(getHelpString());
        commandTextComponent.setCaretPosition(commandTextComponent.getText().length());
        adapter = new MouseAdapter() {

            /**
         * Invoked when the mouse has been clicked on a component.
         */
            public void mouseClicked(MouseEvent e) {
                if (commandTextComponent.getText().length() > 0 && !commandTextComponent.getText().equals(getHelpString())) {
                    changeModeTo(MODE_EDIT);
                } else {
                    if (commandVector.size() > 0) {
                        commandVectorIndex = commandVector.size() - 1;
                        changeModeTo(MODE_BROWSE_HISTORY);
                        commandTextComponent.setText((String) commandVector.get(commandVector.size() - 1));
                        commandTextComponent.setCaretPosition(commandTextComponent.getText().length());
                    } else {
                        commandTextComponent.setText("");
                        commandTextComponent.setCaretPosition(0);
                        changeModeTo(MODE_EDIT);
                    }
                }
                commandTextComponent.removeMouseListener(adapter);
            }
        };
        commandTextComponent.addMouseListener(adapter);
    }

    /**
   * prints the help tothe message window
   */
    private String getHelpString() {
        return "Welcome" + GeneralConstants.LS + "=-=-=-=" + GeneralConstants.LS + "" + GeneralConstants.LS + " - Press 'Esc' key to toggle between BROWSE-HISTORY and EDIT mode" + GeneralConstants.LS + " - In BROWSE HISTORY mode scroll through previous commands using the up and down arrow keys" + GeneralConstants.LS + " - In EDIT mode, any text terminated with a ';' will be executed when you press 'Enter'. " + GeneralConstants.LS + " - Type 'clear' to clear command history" + GeneralConstants.LS + "" + GeneralConstants.LS + "Click in this window to start" + GeneralConstants.LS;
    }

    /**
   * Something is typed onto the command line
   *
   * This method behaves in a very specific way. Basically, you are using the cursor
   * keys to browse through the command history then it works fine.
   *
   * If the user starts to edit a command ( this is indicated by a key press other than an
   * up or down arrow) then the command history mechanism will be switched off until they
   * execute that command.
   */
    void commandTextArea_keyTyped(KeyEvent e) {
        if (e.getKeyChar() == KeyEvent.VK_ENTER) {
            boolean isExecutable = commandTextComponent.getCaretPosition() + 1 >= commandTextComponent.getText().length();
            String command = commandTextComponent.getText().trim();
            commandVectorIndex = commandVector.size() - 1;
            if (command.equalsIgnoreCase(";")) {
                commandVector.addElement(command);
                commandNumber = 1;
            } else if (command.equalsIgnoreCase("clear") || command.equalsIgnoreCase("clear;")) {
                commandTextComponent.setText("");
                commandVector = new Vector();
                commandNumber = -1;
            } else if (command.equalsIgnoreCase("cls") || command.equalsIgnoreCase("cls;")) {
                commandTextComponent.setText("");
                commandVector.addElement(command);
                commandNumber = 1;
            } else if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("help;") || command.equalsIgnoreCase("?") || command.equalsIgnoreCase("?;") || command.equalsIgnoreCase("h") || command.equalsIgnoreCase("h;")) {
                commandTextComponent.setText(getHelpString());
                commandVector.addElement(command);
                commandNumber = 1;
            } else if (isExecutable && command.endsWith(";")) {
                tool.log("\n\nQuery " + commandNumber + ": \n" + command);
                commandTextComponent.setText(command);
                e.consume();
                commandVector.addElement(command);
                commandNumber++;
                tool.getQueryTool().executeCommand(command.substring(0, command.length() - 1));
            } else {
                changeModeTo(MODE_EDIT);
            }
        } else if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
            if (mode == MODE_EDIT) changeModeTo(MODE_BROWSE_HISTORY); else changeModeTo(MODE_EDIT);
            commandTextComponent.setCaretPosition(commandTextComponent.getText().length());
            commandVectorIndex = commandVector.size() - 1;
        } else {
            commandVectorIndex = commandVector.size() - 1;
            changeModeTo(MODE_EDIT);
        }
    }

    /**
   * Something is typed onto the command line
   */
    void commandTextArea_keyPressed(KeyEvent e) {
        if (mode == MODE_EDIT) {
            if (commandVector.size() < 1 || commandTextComponent.getText().length() >= 1) {
                return;
            }
        }
        switch(e.getKeyCode()) {
            case (38):
                if (mode == MODE_BROWSE_HISTORY && commandVectorIndex >= 0) {
                    if (commandVectorIndex > 0) commandVectorIndex--;
                } else {
                    commandVectorIndex = commandVector.size() - 1;
                    changeModeTo(MODE_BROWSE_HISTORY);
                }
                if (commandVector.size() > 0) {
                    commandTextComponent.setText((String) commandVector.elementAt(commandVectorIndex));
                    commandTextComponent.setCaretPosition(commandTextComponent.getText().length());
                }
                e.consume();
                break;
            case (40):
                if (mode == MODE_BROWSE_HISTORY && commandVectorIndex < (commandVector.size() - 1)) {
                    commandVectorIndex++;
                } else {
                    commandVectorIndex = commandVector.size() - 1;
                    changeModeTo(MODE_BROWSE_HISTORY);
                }
                if (commandVector.size() > 0) {
                    commandTextComponent.setText((String) commandVector.elementAt(commandVectorIndex));
                    commandTextComponent.setCaretPosition(commandTextComponent.getText().length());
                }
                e.consume();
                break;
            case (37):
                changeModeTo(MODE_EDIT);
                break;
            case (39):
                changeModeTo(MODE_EDIT);
                break;
        }
    }

    private void changeModeTo(int newMode) {
        switch(newMode) {
            case (MODE_BROWSE_HISTORY):
                mode = MODE_BROWSE_HISTORY;
                commandTextComponent.setForeground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_FOREGROUND_COLOR).getValue());
                commandTextComponent.setBackground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_BACKGROUND_COLOR).getValue());
                commandTextComponent.setCaretColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_FOREGROUND_COLOR).getValue());
                marginTextArea.setForeground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_FOREGROUND_COLOR).getValue());
                marginTextArea.setBackground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_BACKGROUND_COLOR).getValue());
                marginTextArea.setCaretColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_FOREGROUND_COLOR).getValue());
                break;
            case (MODE_EDIT):
                mode = MODE_EDIT;
                commandTextComponent.setForeground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_FOREGROUND_COLOR).getValue());
                commandTextComponent.setBackground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_BACKGROUND_COLOR).getValue());
                commandTextComponent.setCaretColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_FOREGROUND_COLOR).getValue());
                marginTextArea.setForeground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_FOREGROUND_COLOR).getValue());
                marginTextArea.setBackground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_BACKGROUND_COLOR).getValue());
                marginTextArea.setCaretColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_FOREGROUND_COLOR).getValue());
                break;
        }
    }

    public void clear() {
        commandTextComponent.setText("");
    }

    /**
   * This method returns the text for the margin
   */
    private String getMarginText(int numberOfLines) {
        String returnText = "SQL> ";
        for (int i = 2; i <= numberOfLines; i++) returnText += GeneralConstants.LS + " " + i + " >";
        return returnText;
    }

    public void applyPreferences() {
        if (mode == MODE_EDIT) {
            marginTextArea.setForeground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_FOREGROUND_COLOR).getValue());
            marginTextArea.setBackground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_BACKGROUND_COLOR).getValue());
            marginTextArea.setCaretColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_FOREGROUND_COLOR).getValue());
            commandTextComponent.setForeground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_FOREGROUND_COLOR).getValue());
            commandTextComponent.setBackground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_BACKGROUND_COLOR).getValue());
            commandTextComponent.setCaretColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_EDIT_MODE_FOREGROUND_COLOR).getValue());
        } else {
            marginTextArea.setForeground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_FOREGROUND_COLOR).getValue());
            marginTextArea.setBackground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_BACKGROUND_COLOR).getValue());
            marginTextArea.setCaretColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_FOREGROUND_COLOR).getValue());
            commandTextComponent.setForeground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_FOREGROUND_COLOR).getValue());
            commandTextComponent.setBackground((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_BACKGROUND_COLOR).getValue());
            commandTextComponent.setCaretColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_BROWSE_MODE_FOREGROUND_COLOR).getValue());
        }
        commandTextComponent.setSelectedTextColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_COLOR_SELECTEDFORGROUND).getValue());
        commandTextComponent.setSelectionColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_COLOR_SELECTEDBACKGROUND).getValue());
        commandTextComponent.setFont((Font) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_FONT).getValue());
        marginTextArea.setSelectedTextColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_COLOR_SELECTEDFORGROUND).getValue());
        marginTextArea.setSelectionColor((Color) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_COLOR_SELECTEDBACKGROUND).getValue());
        marginTextArea.setFont((Font) EXQLModuleManager.instance().getPreference(EXQLConstants.COMMANDER_FONT).getValue());
    }

    public void disableComponent() {
        commandTextComponent.setEnabled(false);
        marginTextArea.setEnabled(false);
    }

    public void enableComponent() {
        commandTextComponent.setEnabled(true);
        marginTextArea.setEnabled(true);
    }

    public JTextComponent getCommandTextComponent() {
        return commandTextComponent;
    }

    /**
   * This method persists the command history for later retrieval the next time the user starts the system.
   */
    public void persistCommandHistory() {
        if (!((Boolean) EXQLModuleManager.instance().getPreference(EXQLConstants.RDBMS_OPTION_REMEMBER_COMMAND_HISTORY).getValue()).booleanValue()) return;
        int maxRows = ((Integer) EXQLModuleManager.instance().getPreference(EXQLConstants.RDBMS_OPTION_MAXCOMMANDSPERSISTED).getValue()).intValue();
        List persistMe = null;
        if (commandVector.size() > maxRows) {
            persistMe = commandVector.subList(commandVector.size() - maxRows, commandVector.size());
        } else {
            persistMe = commandVector;
        }
        log.debug("Persisting");
        PreparedStatement st = null;
        Connection conn = null;
        Transaction t = null;
        try {
            conn = DbStoreModuleManager.instance().getConnection();
            t = new Transaction(conn);
            st = conn.prepareStatement("DELETE FROM SQL_COMMAND_HISTORY WHERE CONNECTION_NAME=?");
            st.setString(1, connectionName);
            st.executeUpdate();
            st.close();
            for (int i = 0; i < persistMe.size(); i++) {
                st = conn.prepareStatement("INSERT INTO SQL_COMMAND_HISTORY (ID, CONNECTION_NAME, COMMAND) VALUES (NULL,?,?)");
                st.setString(1, connectionName);
                st.setString(2, (String) persistMe.get(i));
                st.executeUpdate();
                st.close();
            }
            t.commit();
        } catch (SQLException e) {
            if (t != null) {
                t.rollback();
            }
            ExceptionManagerFactory.getExceptionManager().manageException(e, "Exception caught while saving command history");
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e1) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e1) {
                }
            }
        }
    }

    /**
   * This method loads the command history for the user when they connect to a particular db.
   */
    public void loadCommandHistory() {
        if (!((Boolean) EXQLModuleManager.instance().getPreference(EXQLConstants.RDBMS_OPTION_REMEMBER_COMMAND_HISTORY).getValue()).booleanValue()) return;
        log.debug("loading");
        Statement st = null;
        Connection conn = null;
        try {
            conn = DbStoreModuleManager.instance().getConnection();
            st = conn.createStatement();
            ResultSet set = st.executeQuery("SELECT COMMAND FROM SQL_COMMAND_HISTORY WHERE CONNECTION_NAME='" + connectionName + "' order by ID ASC");
            while (set.next()) {
                String s = set.getString(1);
                log.debug(s);
                commandVector.add(s);
            }
            set.close();
            st.close();
        } catch (SQLException e) {
            ExceptionManagerFactory.getExceptionManager().manageException(e, "Exception caught while saving command history");
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e1) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e1) {
                }
            }
        }
    }

    /**
     * Returns text and DBEntity flavours 
     */
    public DataFlavor[] getSupportedDataFlavors() {
        return new DataFlavor[] { DataFlavor.stringFlavor, DBEntityDataFlavor.dbEntityDataFlavor() };
    }

    public String render(Transferable transferable) {
        Object object = null;
        try {
            if (transferable != null) {
                object = transferable.getTransferData(DBEntityDataFlavor.dbEntityDataFlavor());
                DBEntity entity = (DBEntity) object;
                SingleTableSelectStatement st = new SingleTableSelectStatement(entity);
                try {
                    return SQLFormatter.format(st.getSelectString(this.conn.getMetaData())) + ";";
                } catch (Exception e1) {
                    ExceptionManagerFactory.getExceptionManager().manageException(e1, "Exception caught while dropping");
                }
            }
        } catch (UnsupportedFlavorException e) {
            try {
                return transferable.getTransferData(DataFlavor.stringFlavor).toString() + ";";
            } catch (UnsupportedFlavorException e1) {
                try {
                    DataFlavor[] f = transferable.getTransferDataFlavors();
                    if (f != null && f.length > 0) return transferable.getTransferData(f[0]).toString(); else return "";
                } catch (UnsupportedFlavorException e2) {
                    ExceptionManagerFactory.getExceptionManager().manageException(e2, "Exception caught while dropping");
                } catch (IOException e3) {
                    ExceptionManagerFactory.getExceptionManager().manageException(e3, "Exception caught while dropping");
                }
            } catch (IOException e4) {
                ExceptionManagerFactory.getExceptionManager().manageException(e4, "Exception caught while dropping");
            }
        } catch (IOException e5) {
            ExceptionManagerFactory.getExceptionManager().manageException(e5, "Exception caught while dropping");
        }
        return "";
    }

    public boolean isReplace() {
        return true;
    }
}
