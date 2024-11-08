package cz.langteacher.gui.importexport.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.SwingWorker;
import org.springframework.beans.factory.annotation.Autowired;
import cz.langteacher.I18n;
import cz.langteacher.LTLogger;
import cz.langteacher.LangTeacherException;
import cz.langteacher.dao.iface.DictionaryDAOIface;
import cz.langteacher.dao.iface.LessonDAOIface;
import cz.langteacher.gui.components.LButton;
import cz.langteacher.gui.components.LOptionPane;
import cz.langteacher.gui.components.LProgressBarIface;
import cz.langteacher.gui.importexport.ImportExportTableModelIface;
import cz.langteacher.gui.importexport.ImportWindowIface;
import cz.langteacher.gui.mainwindow.MainWindowIface;
import cz.langteacher.gui.mainwindow.MainWindowModelIface;
import cz.langteacher.gui.mainwindow.table.LessonTableModelIface;
import cz.langteacher.model.Dictionary;
import cz.langteacher.model.Lesson;
import cz.langteacher.util.CharacterCheckerIface;
import cz.langteacher.util.ILTUtil;

public class ImportAction implements ActionListener {

    private static final int COUNT_AFTER_IMPORT_ACTIONS = 5;

    @Autowired
    private ImportExportTableModelIface tableModel;

    @Autowired
    private LProgressBarIface progressBar;

    @Autowired
    private ImportWindowIface importWindow;

    @Autowired
    private DictionaryDAOIface dictionaryDAO;

    @Autowired
    private LessonDAOIface lessonDAO;

    private LOptionPane lOptions = new LOptionPane();

    @Autowired
    private MainWindowIface mainWindow;

    @Autowired
    private MainWindowModelIface mainWindowModel;

    @Autowired
    private CharacterCheckerIface charChecker;

    private LTLogger logger = LTLogger.getInstance();

    @Autowired
    private ILTUtil lttUtil;

    @Override
    public void actionPerformed(ActionEvent e) {
        progressBar.setMessage(I18n.translate("Importing words"));
        progressBar.setParent(importWindow.getDialog());
        if (importWindow.isXMLFormat()) {
            importXML();
        } else {
            Lesson lesson = importWindow.getSelectedLesson();
            if (lesson.getId() < 0) {
                lessonDAO.insertLesson(lesson);
                ((LessonTableModelIface) mainWindow.getLessonsTable().getModel()).addRow(lesson);
                mainWindow.getLessonsTable().revalidate();
            }
            ImportWorker worker = new ImportWorker(lesson);
            progressBar.startDeterminate(tableModel.getRowCount(), true);
            worker.execute();
        }
    }

    private void importXML() {
        List<Lesson> lessons = importWindow.getLessonsForImport();
        ImportXMLWorker worker = new ImportXMLWorker(lessons);
        long allDics = lttUtil.countDictionaries(lessons);
        progressBar.startDeterminate((int) allDics + COUNT_AFTER_IMPORT_ACTIONS, true);
        worker.execute();
    }

    private class ImportXMLWorker extends SwingWorker<Void, Integer> {

        private List<Lesson> lessons;

        private boolean interrupted = false;

        public ImportXMLWorker(List<Lesson> lessons) {
            super();
            this.lessons = lessons;
        }

        @Override
        protected Void doInBackground() {
            int index = 0;
            try {
                for (Lesson lesson : lessons) {
                    lessonDAO.insertLesson(lesson);
                    for (Dictionary dictionary : lesson.getDictionary()) {
                        dictionary.setIdLesson(lesson.getId());
                        dictionaryDAO.insertDictionary(dictionary);
                        publish(++index);
                    }
                }
            } catch (Exception e) {
                interrupted = true;
                logger.fatal(e, "Error when impot XML.");
                new LangTeacherException(e);
            }
            return null;
        }

        @Override
        protected void process(List<Integer> chunks) {
            progressBar.setValue(chunks.get(chunks.size() - 1));
        }

        @Override
        protected void done() {
            mainWindowModel.fillLessonTableModel();
            progressBar.setValue(progressBar.getProgressBarValue() + 1);
            mainWindow.refreshLessonTable();
            progressBar.setValue(progressBar.getProgressBarValue() + 1);
            mainWindow.updateLessonItems();
            progressBar.setValue(progressBar.getProgressBarValue() + 1);
            mainWindow.updateDictionaryItems();
            progressBar.setValue(progressBar.getProgressBarValue() + 1);
            importWindow.refreshWindow();
            progressBar.setValue(progressBar.getProgressBarValue() + 1);
            progressBar.stopProgressBar();
            if (interrupted) {
                lOptions.showOptionPane(importWindow.getDialog(), "Import was interrupted.", "Import Result", LButton.OK);
            } else {
                String succesfull = "Import of all words was succesfull.";
                lOptions.showOptionPane(importWindow.getDialog(), succesfull, "Import Result", LButton.OK);
            }
            importWindow.cleanAfterImport();
        }
    }

    private class ImportWorker extends SwingWorker<Void, Integer> {

        private Lesson selectedLesson;

        private boolean skipAll = false;

        private boolean overwriteAll = false;

        private int importedCounter = 0;

        private boolean interrupted = false;

        public ImportWorker(Lesson selectedLesson) {
            super();
            this.selectedLesson = selectedLesson;
        }

        @Override
        protected Void doInBackground() throws Exception {
            int index = 0;
            for (Dictionary dic : tableModel.getData()) {
                if (interrupted) {
                    break;
                }
                if (isAlreadyExists(dic)) {
                    handleAlreadyExists(dic);
                } else {
                    addNewWord(dic);
                    refreshDictionaryTable();
                }
                publish(++index);
            }
            return null;
        }

        @Override
        protected void process(List<Integer> chunks) {
            progressBar.setValue(chunks.get(chunks.size() - 1));
        }

        @Override
        protected void done() {
            progressBar.stopProgressBar();
            String succesfull = "Import of " + importedCounter + " words was succesfull.";
            if (interrupted) {
                String imported = importedCounter > 0 ? succesfull : "";
                lOptions.showOptionPane(importWindow.getDialog(), "Import was interrupted." + imported, "Import Result", LButton.OK);
            } else {
                lOptions.showOptionPane(importWindow.getDialog(), succesfull, "Import Result", LButton.OK);
            }
            importWindow.cleanAfterImport();
            mainWindow.updateDictionaryItems();
        }

        private void refreshDictionaryTable() {
            mainWindow.getDictionaryTable().revalidate();
        }

        private void addNewWord(Dictionary dic) {
            dic.setIdLesson(selectedLesson.getId());
            dictionaryDAO.insertDictionary(dic);
            selectedLesson.getDictionary().add(dic);
            importedCounter++;
            firePropertyChange(LProgressBarIface.CHANGE_PROPERTY, -1, 0);
        }

        private boolean isAlreadyExists(Dictionary dic) {
            List<Dictionary> allDics = selectedLesson.getDictionary();
            for (int i = 0; i < allDics.size(); i++) {
                Dictionary temp = allDics.get(i);
                if (temp.getWord().equalsIgnoreCase(dic.getWord()) && temp.getTranslation().equalsIgnoreCase(dic.getTranslation()) && temp.getDescription().equalsIgnoreCase(dic.getDescription())) {
                    return true;
                }
            }
            return false;
        }

        private void handleAlreadyExists(Dictionary dic) {
            if (overwriteAll) {
                addNewWord(dic);
            } else if (skipAll) {
                return;
            } else {
                Integer answer = lOptions.showOptionPane(importWindow.getDialog(), "Dictionary: " + dic.printable() + " \nalready exist in lesson " + selectedLesson.getName() + ". Do you want to overwrite, skip or interrupt import?", "Word already exists", "Overwrite", "Overwrite All", "Skip", "Skip All", "Cancel Import");
                switch(answer) {
                    case 0:
                        addNewWord(dic);
                        break;
                    case 1:
                        overwriteAll = true;
                        addNewWord(dic);
                        break;
                    case 2:
                        return;
                    case 3:
                        skipAll = true;
                        return;
                    case 4:
                        interrupted = true;
                        return;
                    default:
                        break;
                }
            }
        }
    }
}
