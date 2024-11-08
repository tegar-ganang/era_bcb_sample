package be.lassi.ui.browser;

import static be.lassi.util.Util.newArrayList;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import be.lassi.context.ShowContext;
import be.lassi.lanbox.commands.Command;
import be.lassi.lanbox.commands.CueListInfo;
import be.lassi.lanbox.commands.CueListRead;
import be.lassi.lanbox.commands.CueListRemove;
import be.lassi.lanbox.commands.CueSceneRead;
import be.lassi.lanbox.commands.GetCueListInfos;
import be.lassi.lanbox.commands.SwingCommandListener;
import be.lassi.lanbox.cuesteps.CueScene;
import be.lassi.lanbox.cuesteps.CueStep;
import be.lassi.ui.util.table.SimpleTableModel;
import be.lassi.util.NLS;
import com.jgoodies.binding.beans.Model;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

/**
 * Presentation model for the cue list browser.
 */
public class BrowserModel extends Model {

    private static final int COLUMN_CUE_LIST_NUMBER = 0;

    private static final int COLUMN_CUE_LIST_STEP_COUNT = 1;

    private static final int COLUMN_CUE_LIST_DESCRIPTION = 2;

    private static final int COLUMN_CUE_STEP_NUMBER = 0;

    private static final int COLUMN_CUE_STEP_DESCRIPTION = 1;

    private static final int COLUMN_CUE_STEP_SCENE = 2;

    private final List<CueListInfo> cueLists = newArrayList();

    private final List<CueStep> cueSteps = newArrayList();

    private final CueListsModel cueListsModel = new CueListsModel();

    private final CueStepsModel cueStepsModel = new CueStepsModel();

    private final ListSelectionModel cueListsSelectionModel = new DefaultListSelectionModel();

    private final ValueModel status = new ValueHolder();

    private final Action deleteAction = new DeleteAction();

    private final ShowContext context;

    public BrowserModel(final ShowContext context) {
        this.context = context;
        cueListsSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        cueListsSelectionModel.addListSelectionListener(new CueListsSelectionListener());
    }

    public TableModel getCueStepsModel() {
        return cueStepsModel;
    }

    public TableModel getCueListsModel() {
        return cueListsModel;
    }

    public ListSelectionModel getCueListsSelectionModel() {
        return cueListsSelectionModel;
    }

    public ValueModel getStatus() {
        return status;
    }

    public Action getDeleteAction() {
        return deleteAction;
    }

    /**
     * Reads the list of cue lists from the Lanbox.
     */
    public void retrieveCueLists() {
        cueLists.clear();
        cueSteps.clear();
        GetCueListInfos command = new GetCueListInfos();
        command.add(new CueListsListener(command));
        context.execute(command);
    }

    private void cueListSelectionChanged() {
        if (cueListsSelectionModel.isSelectionEmpty()) {
            cueSteps.clear();
            cueStepsModel.fireTableDataChanged();
            status.setValue(NLS.get("browser.status.select"));
        } else {
            int min = cueListsSelectionModel.getMinSelectionIndex();
            int max = cueListsSelectionModel.getMaxSelectionIndex();
            if (min != max) {
                cueSteps.clear();
                cueStepsModel.fireTableDataChanged();
                status.setValue(NLS.get("browser.status.selectSingle"));
            } else {
                CueListInfo info = cueLists.get(min);
                status.setValue(NLS.get("browser.status.select"));
                CueListRead command = new CueListRead(info.getNumber(), 1, info.getStepCount());
                command.add(new CueStepsListener(command, info.getNumber()));
                context.execute(command);
            }
        }
    }

    private void deleteSelectedCueLists() {
        List<CueListInfo> delete = newArrayList();
        for (int i = 0; i < cueLists.size(); i++) {
            if (cueListsSelectionModel.isSelectedIndex(i)) {
                delete.add(cueLists.get(i));
            }
        }
        cueListsSelectionModel.clearSelection();
        status.setValue("");
        for (CueListInfo info : delete) {
            Command command = new CueListRemove(info.getNumber());
            command.add(new UpdateList());
            context.execute(command);
        }
        retrieveCueLists();
    }

    private class DeleteAction extends AbstractAction {

        private DeleteAction() {
            super(NLS.get("browser.action.delete"));
        }

        public void actionPerformed(final ActionEvent e) {
            deleteSelectedCueLists();
        }
    }

    /**
     * Listens for selection changes in the cue lists table.
     */
    private class CueListsSelectionListener implements ListSelectionListener {

        public void valueChanged(final ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                cueListSelectionChanged();
            }
        }
    }

    private class UpdateList extends SwingCommandListener {

        @Override
        public void action() {
            retrieveCueLists();
        }
    }

    /**
     * Updates the user interface after the list of cue lists
     * has been read from the Lanbox.
     */
    private class CueListsListener extends SwingCommandListener {

        private final GetCueListInfos command;

        private CueListsListener(final GetCueListInfos command) {
            this.command = command;
        }

        @Override
        public void action() {
            List<CueListInfo> newList = command.getCueListInfos();
            cueLists.clear();
            cueLists.addAll(newList);
            cueListsModel.fireTableDataChanged();
            retrieveCueListDescriptions();
        }
    }

    private void retrieveCueListDescriptions() {
        for (int i = 0; i < cueLists.size(); i++) {
            CueListInfo info = cueLists.get(i);
            int stepCount = Math.min(5, info.getStepCount());
            if (stepCount > 0) {
                CueListRead command = new CueListRead(info.getNumber(), 1, stepCount);
                command.add(new CueListDescriptionListener(command, i));
                context.execute(command);
            }
        }
    }

    /**
     * Updates the user interface after the cue steps for a given
     * cue list have been read from the Lanbox.
     */
    private class CueStepsListener extends SwingCommandListener {

        private final CueListRead command;

        private final int cueListId;

        private CueStepsListener(final CueListRead command, final int cueListId) {
            this.command = command;
            this.cueListId = cueListId;
        }

        @Override
        public void action() {
            cueSteps.clear();
            cueSteps.addAll(command.getCueSteps());
            cueStepsModel.fireTableDataChanged();
            for (int i = 0; i < cueSteps.size(); i++) {
                CueStep cueStep = cueSteps.get(i);
                if (cueStep instanceof CueScene) {
                    CueScene cueScene = (CueScene) cueStep;
                    CueSceneRead csr = new CueSceneRead(cueListId, i + 1);
                    csr.add(new CueSceneListener(csr, cueScene, i));
                    context.execute(csr);
                }
            }
        }
    }

    /**
     * Updates the user interface after the scene for a given
     * CueScene cue step has been read from the Lanbox.
     */
    private class CueSceneListener extends SwingCommandListener {

        private final CueSceneRead command;

        private final CueScene cueScene;

        private final int cueStepIndex;

        private CueSceneListener(final CueSceneRead command, final CueScene cueScene, final int cueStepIndex) {
            this.command = command;
            this.cueScene = cueScene;
            this.cueStepIndex = cueStepIndex;
        }

        @Override
        public void action() {
            cueScene.getChanges().set(command.getChannelChanges());
            cueStepsModel.fireTableCellUpdated(cueStepIndex, COLUMN_CUE_STEP_SCENE);
        }
    }

    /**
     * Updates the cue list description in the user interface after
     * the cue steps for a given cue list have been read from the Lanbox,
     * and a description can be derived from the cue list comments.
     */
    private class CueListDescriptionListener extends SwingCommandListener {

        private final CueListRead command;

        private final int cueListIndex;

        private CueListDescriptionListener(final CueListRead command, final int cueListIndex) {
            this.command = command;
            this.cueListIndex = cueListIndex;
        }

        @Override
        protected void action() {
            List<CueStep> steps = command.getCueSteps();
            String description = CueListDescription.getString(steps);
            cueLists.get(cueListIndex).setDescription(description);
            cueListsModel.fireTableCellUpdated(cueListIndex, COLUMN_CUE_LIST_DESCRIPTION);
        }
    }

    private class CueStepsModel extends SimpleTableModel {

        private CueStepsModel() {
            super(NLS.get("browser.cuesteps.column.number"), NLS.get("browser.cuesteps.column.description"), NLS.get("browser.cuesteps.column.levels"));
        }

        public int getRowCount() {
            return cueSteps.size();
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            Object value = "";
            switch(columnIndex) {
                case COLUMN_CUE_STEP_NUMBER:
                    value = rowIndex + 1;
                    break;
                case COLUMN_CUE_STEP_DESCRIPTION:
                    value = cueSteps.get(rowIndex).getString();
                    break;
                case COLUMN_CUE_STEP_SCENE:
                    CueStep cueStep = cueSteps.get(rowIndex);
                    if (cueStep instanceof CueScene) {
                        CueScene cueScene = (CueScene) cueStep;
                        value = cueScene.getChanges().getString();
                    }
                    break;
                default:
                    value = "";
            }
            return value;
        }
    }

    private class CueListsModel extends SimpleTableModel {

        private CueListsModel() {
            super(NLS.get("browser.cuelists.column.number"), NLS.get("browser.cuelists.column.stepCount"), NLS.get("browser.cuelists.column.description"));
        }

        public int getRowCount() {
            return cueLists.size();
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            Object value = null;
            switch(columnIndex) {
                case COLUMN_CUE_LIST_NUMBER:
                    value = cueLists.get(rowIndex).getNumber();
                    break;
                case COLUMN_CUE_LIST_STEP_COUNT:
                    value = cueLists.get(rowIndex).getStepCount();
                    break;
                case COLUMN_CUE_LIST_DESCRIPTION:
                    value = cueLists.get(rowIndex).getDescription();
                    break;
                default:
                    value = "";
            }
            return value;
        }
    }
}
