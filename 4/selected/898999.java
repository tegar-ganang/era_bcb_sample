package savenews.app.gui.listeners.button;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import javax.swing.JOptionPane;
import savenews.app.gui.UIUtils;
import savenews.app.gui.form.ArticleForm;
import savenews.app.gui.panel.MainWindowPanel;
import savenews.app.outputs.OutputProcessorFactory;
import savenews.backend.exceptions.AppInfrastructureException;
import savenews.backend.exceptions.FileAlreadyExistsException;
import savenews.backend.exceptions.ValidationException;
import savenews.backend.util.ConfigurationResources;
import savenews.backend.util.I18NResources;

/**
 * Listens to "Save button" clicks 
 * @author Eduardo Ferreira
 */
public class SaveButtonListener implements ActionListener {

    /** Exports contents from screen */
    private OutputProcessorFactory opf;

    /** Panel from which this event was triggered */
    private MainWindowPanel parentPanel;

    /**
	 * Default constructor
	 */
    public SaveButtonListener(MainWindowPanel parentPanel) {
        this.parentPanel = parentPanel;
        this.opf = OutputProcessorFactory.getInstance();
    }

    /**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
    public void actionPerformed(ActionEvent e) {
        ArticleForm articleForm = new ArticleForm();
        articleForm.setTitle(parentPanel.getArticleTitle());
        articleForm.setOrigin(parentPanel.getArticleOrigin());
        articleForm.setContents(parentPanel.getArticleContents());
        String dateOnOutput = ConfigurationResources.getInstance().getDateOnOutput();
        if ("yes".equals(dateOnOutput)) {
            articleForm.setArticleDate(new Date(System.currentTimeMillis()));
        }
        try {
            int outputProcessorType;
            switch(parentPanel.getSelectedTextArea()) {
                case MainWindowPanel.PLAIN_TEXT_AREA_SELECTED:
                    outputProcessorType = OutputProcessorFactory.PLAIN_TEXT_OUTPUT_PROCESSOR;
                    break;
                case MainWindowPanel.RICH_TEXT_AREA_SELECTED:
                    outputProcessorType = OutputProcessorFactory.RICH_TEXT_OUTPUT_PROCESSOR;
                    break;
                default:
                    throw new AppInfrastructureException("Unknown text area selected");
            }
            try {
                generateOutput(articleForm, outputProcessorType, false);
            } catch (FileAlreadyExistsException ex) {
                int confirmation = JOptionPane.showOptionDialog(parentPanel, I18NResources.getInstance().get(I18NResources.FILE_ALREADY_EXISTS_MESSAGE), I18NResources.getInstance().get(I18NResources.ERROR_MESSAGE_TITLE), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, I18NResources.getInstance().getDialogYesNoOption(), I18NResources.getInstance().getDialogYesNoOption()[1]);
                if (JOptionPane.OK_OPTION == confirmation) {
                    try {
                        generateOutput(articleForm, outputProcessorType, true);
                    } catch (FileAlreadyExistsException e1) {
                        throw new AppInfrastructureException("REACHING UNREACHABLE CODE");
                    }
                }
            }
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(parentPanel, I18NResources.getInstance().get(ex.getMessage()), I18NResources.getInstance().get(I18NResources.ERROR_MESSAGE_TITLE), JOptionPane.OK_OPTION);
        } catch (Error ex) {
            UIUtils.raiseInfrastructureError(ex, parentPanel);
        } catch (RuntimeException ex) {
            UIUtils.raiseInfrastructureError(ex, parentPanel);
        }
    }

    private void generateOutput(ArticleForm articleForm, int outputProcessorType, boolean overwriteFile) throws ValidationException, FileAlreadyExistsException {
        opf.newOutputProcessor(outputProcessorType).process(articleForm, overwriteFile);
        int confirmation = JOptionPane.showOptionDialog(parentPanel, I18NResources.getInstance().get(I18NResources.SAVE_SUCCESSFUL_MESSAGE), I18NResources.getInstance().get(I18NResources.QUESTION_MESSAGE_TITLE), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, I18NResources.getInstance().getDialogYesNoOption(), I18NResources.getInstance().getDialogYesNoOption()[0]);
        if (JOptionPane.OK_OPTION == confirmation) {
            parentPanel.cleanFields();
        }
    }
}
