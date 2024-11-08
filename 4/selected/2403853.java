package org.gudy.azureus2.ui.swt.views.tableitems.files;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.core3.internat.MessageText;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class ModeItem extends CoreTableColumn implements TableCellRefreshListener {

    /** Default Constructor */
    public ModeItem() {
        super("mode", ALIGN_LEAD, POSITION_LAST, 60, TableManager.TABLE_TORRENT_FILES);
        setRefreshInterval(INTERVAL_LIVE);
        setMinWidthAuto(true);
    }

    public void fillTableColumnInfo(TableColumnInfo info) {
        info.addCategories(new String[] { CAT_CONTENT });
        info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
    }

    public void refresh(TableCell cell) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) cell.getDataSource();
        long value = (fileInfo == null) ? 0 : fileInfo.getAccessMode();
        if (!cell.setSortValue(value) && cell.isValid()) {
            return;
        }
        String sText = MessageText.getString("FileItem." + ((value == DiskManagerFileInfo.WRITE) ? "write" : "read"));
        cell.setText(sText);
    }
}
