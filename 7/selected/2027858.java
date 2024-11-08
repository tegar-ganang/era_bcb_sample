package net.sf.pim.action;

import net.sf.component.table.BindedTableViewer;
import net.sf.pim.UiUtil;
import net.sf.pim.model.psp.Work;

/**
 * @author lzhang <p/> To change the template for this generated type comment go
 *         to Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DownAction extends UiAction {

    /**
	 * @param uc
	 */
    public DownAction() {
        super();
        name = "下移";
        gif = "down.gif";
    }

    public void run() {
        super.run();
        if (UiUtil.getActiveTableEditor().getName().equals("日志")) {
            if (parent.isMulti()) return;
            int index = parent.getTv().getTable().getSelectionIndex();
            if (index != -1) {
                Work[] list = parent.getData().getWorks();
                if (index < list.length - 1) {
                    Work selected = list[index];
                    list[index] = list[index + 1];
                    list[index].setWid(String.valueOf(index + 1));
                    list[index + 1] = selected;
                    list[index + 1].setWid(String.valueOf(index + 2));
                    parent.getTv().setInput(list);
                    parent.getTv().getTable().setSelection(index + 1);
                    parent.setDirty(true);
                }
            }
        } else {
            BindedTableViewer btv = UiUtil.getActiveTableEditor().getViewer();
            if (!btv.getModel().isEditable()) return;
            btv.getModel().moveDown(btv.getTable().getSelectionIndex());
            btv.dataReordered();
            btv.getTableCursor().setSelection(btv.getTable().getSelectionIndex(), btv.getTableCursor().getColumn());
            btv.getModel().setDirty(true);
        }
    }
}
