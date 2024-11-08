package cz.langteacher.gui.importexport;

import static cz.langteacher.I18n.translate;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.springframework.beans.factory.annotation.Autowired;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import cz.langteacher.I18n;
import cz.langteacher.gui.AbstractWindow;
import cz.langteacher.gui.components.LButton;
import cz.langteacher.gui.components.LDialog;
import cz.langteacher.gui.components.LGroupPanel;
import cz.langteacher.gui.components.LOptionPane;
import cz.langteacher.gui.components.LProgressBarIface;
import cz.langteacher.gui.components.LRadioGroupPanel;
import cz.langteacher.gui.components.LTLessonPicker;
import cz.langteacher.gui.components.LRadioGroupPanel.PanelOrientation;
import cz.langteacher.gui.mainwindow.MainWindowIface;
import cz.langteacher.gui.mainwindow.table.LessonTableModelIface;
import cz.langteacher.gui.util.LTGUIUtilsIface;
import cz.langteacher.model.Dictionary;
import cz.langteacher.model.Lesson;
import cz.langteacher.util.HelpContextKeys;

public class ExportWindow extends AbstractWindow implements ExportWindowIface {

    @Autowired
    private MainWindowIface mainWindow;

    @Autowired
    private LessonTableModelIface lessonModel;

    @Autowired
    private ImportExportTableModelIface tableModel;

    @Autowired
    private LOptionPane lOptions;

    @Autowired
    private LTGUIUtilsIface guiUtils;

    @Autowired
    private ExportControllerIface exportController;

    @Autowired
    private LProgressBarIface progressBar;

    private LDialog dialog = null;

    private LButton exportBtn = null;

    private JComboBox lessonCombo = null;

    private JTable dicLTFtable = null;

    private JFileChooser fileChooser = new JFileChooser();

    private JTextField filePath = null;

    private CardLayout cardLayout = null;

    private boolean isXMLFormat = true;

    private JPanel cardPanel;

    private LTLessonPicker lessonPicker;

    public void showWindow() {
        tableModel.setData(new ArrayList<Dictionary>());
        doShow();
    }

    private void doShow() {
        initDialog();
        filePath.setText("");
        lessonCombo.setSelectedIndex(0);
        dicLTFtable.getRowSorter().allRowsChanged();
        dicLTFtable.repaint();
        dialog.pack();
        dialog.setLocationRelativeTo(mainWindow.getFrame());
        handleButtons();
        dialog.setVisible(true);
    }

    private void initDialog() {
        dialog = LDialog.createDialog(new String[] { LButton.HELP, "Close", "Export" }, mainWindow.getFrame(), true);
        dialog.setTitle(I18n.translate("Export"));
        addHelpAction(dialog, HelpContextKeys.IMPORT_EXPORT);
        exportBtn = dialog.getButton("Export");
        exportBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                File file = new File(filePath.getText());
                if (file.exists()) {
                    int answer = lOptions.showOptionPane(dialog, "Selected file already exists. Do you want to overwrite it?", "Rewrite file", "Rewrite", "Cancel");
                    if (answer == 1) {
                        return;
                    }
                }
                if (isXMLFormat) {
                    exportController.export(file, lessonPicker.getPickedLessons());
                } else {
                    exportController.export(file, (Lesson) lessonCombo.getSelectedItem());
                }
            }
        });
        LButton cancel = dialog.getButton("Close");
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                closeWindow();
            }
        });
        filePath = new JTextField();
        filePath.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                handleButtons();
            }
        });
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(getLTFContent(), LTF_TAB);
        cardPanel.add(getXMLContent(), XML_TAB);
        cardLayout.show(cardPanel, XML_TAB);
        final LRadioGroupPanel radioGroupPanel = LRadioGroupPanel.createRadioButtonGroup(PanelOrientation.VERTICAL, I18n.translate("Export to XML Format"), I18n.translate("Export to LangTeacher Format (ltf)"));
        radioGroupPanel.setSelectRadioButton(0);
        radioGroupPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (radioGroupPanel.getIndexOfSelectedRadioButton() == 0) {
                    cardLayout.show(cardPanel, XML_TAB);
                    isXMLFormat = true;
                } else {
                    cardLayout.show(cardPanel, LTF_TAB);
                    isXMLFormat = false;
                }
            }
        });
        JLabel selectFileLabel = new JLabel(translate("Export to file:"));
        LButton selectFileBtn = new LButton(I18n.translate("Choose file"));
        selectFileBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser.showOpenDialog(dialog);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    final File file = fileChooser.getSelectedFile();
                    filePath.setText(file.getAbsolutePath());
                    filePath.repaint();
                    handleButtons();
                }
            }
        });
        FormLayout layout = guiUtils.getFormLayoutExport();
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(LGroupPanel.createSimpleGroup(radioGroupPanel, I18n.translate("Select Format of Export")), 5);
        builder.nextLine(2);
        builder.append(cardPanel, 5);
        builder.nextLine(2);
        builder.append(selectFileLabel);
        builder.append(filePath);
        builder.append(selectFileBtn);
        JPanel mainPanel = builder.getPanel();
        dialog.setMainPane(mainPanel);
    }

    private JPanel getXMLContent() {
        FormLayout layout = guiUtils.getFormLayoutImportExportXML();
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        LTLessonPicker.Settings settings = new LTLessonPicker.Settings();
        settings.setAllLessons(new ArrayList<Lesson>(lessonModel.getAllLessons()));
        settings.setPickedLessons(mainWindow.getSelectedLessons()).setI18nProvider(new I18n());
        settings.setGroupTitle("Lessons to Export").simpleGroup();
        lessonPicker = LTLessonPicker.create(settings);
        builder.append(lessonPicker.getPanel());
        return builder.getPanel();
    }

    private JPanel getLTFContent() {
        fileChooser.addChoosableFileFilter(guiUtils.getFileFilterImportExport());
        buildLessonCombo();
        dicLTFtable = new JTable(tableModel);
        guiUtils.buildImportExportTable(dicLTFtable);
        dicLTFtable.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(dicLTFtable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(Color.WHITE);
        FormLayout layout = guiUtils.getFormLayoutImportExportLTF();
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(getComboRow());
        builder.nextLine(2);
        builder.append(scroll);
        return LGroupPanel.createSimpleGroup(builder.getPanel(), I18n.translate("Select Lesson to Export"));
    }

    private void buildLessonCombo() {
        List<Lesson> lessons = new ArrayList<Lesson>();
        lessons.addAll(new ArrayList<Lesson>(lessonModel.getAllLessons()));
        lessonCombo = guiUtils.getLessonComboBox(lessons);
        lessonCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                progressBar.setMessage(I18n.translate("Reading words..."));
                progressBar.setParent(dialog);
                progressBar.startIndeterminate();
                Thread th = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Lesson lesson = (Lesson) lessonCombo.getSelectedItem();
                        tableModel.setData(getRidOfEmptyDictionaries(lesson));
                        dicLTFtable.getRowSorter().allRowsChanged();
                        dicLTFtable.revalidate();
                        progressBar.stopProgressBar();
                        handleButtons();
                    }

                    private List<Dictionary> getRidOfEmptyDictionaries(Lesson lesson) {
                        List<Dictionary> result = new ArrayList<Dictionary>();
                        for (Dictionary dic : lesson.getDictionary()) {
                            if (!dic.isEmpty()) {
                                result.add(dic);
                            }
                        }
                        return result;
                    }
                });
                th.start();
            }
        });
    }

    private JPanel getComboRow() {
        JLabel selectLessonl = new JLabel(translate("Export all words from lesson:"));
        FormLayout layout = new FormLayout("pref, ${labelGap}, max(80dlu; default)", "", guiUtils.getLayoutMap());
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(selectLessonl);
        builder.append(lessonCombo);
        return builder.getPanel();
    }

    private void closeWindow() {
        dialog.dispose();
        dialog.setVisible(false);
    }

    private void handleButtons() {
        if (isXMLFormat) {
            exportBtn.setEnabled(!lessonPicker.getPickedLessons().isEmpty() && !filePath.getText().isEmpty());
        } else {
            exportBtn.setEnabled(tableModel.getRowCount() > 0 && !filePath.getText().isEmpty());
        }
    }

    public LDialog getDialog() {
        return dialog;
    }

    public Lesson getSelectedLesson() {
        return (Lesson) lessonCombo.getSelectedItem();
    }

    @Override
    public void showWindow(List<Lesson> selectedLessons) {
        lessonPicker.setPickedLessons(selectedLessons);
        doShow();
    }
}
