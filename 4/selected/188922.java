package de.guidoludwig.jtrade.tradelist;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.apache.commons.csv.writer.CSVWriter;
import org.apache.commons.io.FileUtils;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.guidoludwig.af.ApplicationFrame;
import de.guidoludwig.jtrade.ErrorMessage;
import de.guidoludwig.jtrade.I18n;
import de.guidoludwig.jtrade.domain.Show;
import de.guidoludwig.jtrade.expimp.EtreeCSV;
import de.guidoludwig.jtrade.util.SwingUtil;

class Export {

    private ExportData exportData;

    Export(Show show) {
        exportData = new ExportData(show);
    }

    JComponent buildComponent() {
        FormLayout layout = new FormLayout("fill:pref:grow", "p, 3dlu, p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        ExportEdit editor = new ExportEdit(exportData);
        builder.add(editor.buildEditor(), cc.xy(1, 1));
        builder.add(ButtonBarFactory.buildOKCancelBar(new JButton(new SaveAction()), new JButton(new CancelAction())), cc.xy(1, 3));
        JComponent panel = builder.getPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private boolean save(Runnable runner, String filename) {
        File f = new File(filename);
        if (f.exists()) {
            int option = JOptionPane.showConfirmDialog(ApplicationFrame.INSTANCE.mainFrame(), "File " + filename + " already exists, overwrite ?", "Confirmation Required!", JOptionPane.YES_NO_CANCEL_OPTION);
            if (option == JOptionPane.CANCEL_OPTION) {
                return false;
            } else if (option == JOptionPane.NO_OPTION) {
                return true;
            }
        }
        runner.run();
        return true;
    }

    private class CancelAction extends AbstractAction {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        private static final String TOOLTIP = "action.cancel.tooltip";

        private static final String ICON = "action.cancel.icon";

        private static final String ACTION_NAME = "action.cancel.name";

        private CancelAction() {
            super(I18n.getString(ACTION_NAME), I18n.getIcon(ICON));
            putValue(Action.SHORT_DESCRIPTION, I18n.getString(TOOLTIP));
        }

        public void actionPerformed(ActionEvent e) {
            ApplicationFrame.INSTANCE.resetModal();
        }
    }

    private class SaveAction extends AbstractAction {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        private static final String TOOLTIP = "tradelist.action.export.save.tooltip";

        private static final String ICON = "tradelist.action.export.save.icon";

        private static final String PRINT_NAME = "tradelist.action.export.save.name";

        private SaveAction() {
            super(I18n.getString(PRINT_NAME), I18n.getIcon(ICON));
            putValue(Action.SHORT_DESCRIPTION, I18n.getString(TOOLTIP));
        }

        public void actionPerformed(ActionEvent arg0) {
            SwingUtil.finishCurrentEdit();
            if (exportData.isExportTags()) {
                if (!save(new StringToFileSaver(exportData.getTags(), exportData.getTagsFileName()), exportData.getTagsFileName())) {
                    return;
                }
            }
            if (exportData.isExportFileNames()) {
                if (!save(new StringToFileSaver(exportData.getFileNames(), exportData.getFileNamesFileName()), exportData.getFileNamesFileName())) {
                    return;
                }
            }
            if (exportData.isExportInfo()) {
                if (!save(new StringToFileSaver(exportData.getInfo(), exportData.getInfoFileName()), exportData.getInfoFileName())) {
                    return;
                }
            }
            if (exportData.isExportCueSheet()) {
                if (!save(new StringToFileSaver(exportData.getCueSheet(), exportData.getCueFileName()), exportData.getCueFileName())) {
                    return;
                }
            }
            if (exportData.isExportEtreeCSV()) {
                if (!save(new MapToCSVSaver(true, exportData.getEtreeCsv(), exportData.getEtreeCSVFileName()), exportData.getEtreeCSVFileName())) {
                    return;
                }
            }
            if (exportData.isExportJtradeCSV()) {
                if (!save(new MapToCSVSaver(false, exportData.getJtradeCsv(), exportData.getJtradeCSVFileName()), exportData.getJtradeCSVFileName())) {
                    return;
                }
            }
            ApplicationFrame.INSTANCE.resetModal();
        }
    }

    private class StringToFileSaver implements Runnable {

        private String text;

        private File file;

        private StringToFileSaver(String text, String filename) {
            this.text = text;
            file = new File(filename);
        }

        public void run() {
            try {
                FileUtils.writeStringToFile(file, text, "UTF-8");
            } catch (IOException e) {
                ErrorMessage.handle(e);
            }
        }
    }

    private class MapToCSVSaver implements Runnable {

        private Map<String, String> text;

        private File file;

        private boolean etree;

        private MapToCSVSaver(boolean etree, Map<String, String> text, String filename) {
            this.text = text;
            this.etree = etree;
            file = new File(filename);
        }

        public void run() {
            CSVWriter writer = new CSVWriter();
            try {
                FileWriter fw = new FileWriter(file);
                writer.setConfig(etree ? EtreeCSV.getEtreeConfig() : EtreeCSV.getJTradeConfig());
                writer.setWriter(fw);
                writer.writeRecord(etree ? EtreeCSV.getEtreeHeader() : EtreeCSV.getJTradeHeader());
                writer.writeRecord(text);
                fw.flush();
                fw.close();
            } catch (IOException e) {
                ErrorMessage.handle(e);
            }
        }
    }
}
