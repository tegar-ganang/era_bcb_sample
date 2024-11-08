package frost.messaging.frost.gui;

import java.util.*;
import javax.swing.table.*;
import frost.messaging.frost.*;
import frost.messaging.frost.boards.*;
import frost.util.gui.translation.*;

@SuppressWarnings("serial")
public class AttachedBoardTableModel extends DefaultTableModel implements LanguageListener {

    private Language language = null;

    protected static final String columnNames[] = new String[3];

    protected static final Class<?> columnClasses[] = { String.class, String.class, String.class };

    public AttachedBoardTableModel() {
        super();
        language = Language.getInstance();
        language.addLanguageListener(this);
        refreshLanguage();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public void languageChanged(LanguageEvent event) {
        refreshLanguage();
    }

    private void refreshLanguage() {
        columnNames[0] = language.getString("MessagePane.boardAttachmentTable.boardName");
        columnNames[1] = language.getString("MessagePane.boardAttachmentTable.accessRights");
        columnNames[2] = language.getString("MessagePane.boardAttachmentTable.description");
        fireTableStructureChanged();
    }

    /**
     * This method fills the table model with the BoardAttachments
     * in the list passed as a parameter
     * @param boardAttachments list of BoardAttachments fo fill the model with
     */
    public void setData(List<BoardAttachment> boardAttachments) {
        setRowCount(0);
        Iterator<BoardAttachment> boards = boardAttachments.iterator();
        while (boards.hasNext()) {
            Board board = boards.next().getBoardObj();
            Object[] row = new Object[3];
            if (board.getName() != null) {
                row[0] = board.getName();
                if (board.getPublicKey() == null && board.getPrivateKey() == null) {
                    row[1] = "public";
                } else if (board.getPublicKey() != null && board.getPrivateKey() == null) {
                    row[1] = "read - only";
                } else {
                    row[1] = "read / write";
                }
                if (board.getDescription() == null) {
                    row[2] = "Not present";
                } else {
                    row[2] = board.getDescription();
                }
                addRow(row);
            }
        }
    }

    @Override
    public String getColumnName(int column) {
        if (column >= 0 && column < columnNames.length) return columnNames[column];
        return null;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (column >= 0 && column < columnClasses.length) return columnClasses[column];
        return null;
    }
}
