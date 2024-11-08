package uk.ac.ebi.pride.tools.converter.gui.forms;

import org.apache.log4j.Logger;
import uk.ac.ebi.pride.tools.converter.gui.NavigationPanel;
import uk.ac.ebi.pride.tools.converter.gui.component.AddTermButton;
import uk.ac.ebi.pride.tools.converter.gui.interfaces.ConverterForm;
import uk.ac.ebi.pride.tools.converter.gui.interfaces.ValidationListener;
import uk.ac.ebi.pride.tools.converter.gui.util.template.TemplateType;
import uk.ac.ebi.pride.tools.converter.gui.util.template.TemplateUtilities;
import uk.ac.ebi.pride.tools.converter.report.model.ReportObject;
import uk.ac.ebi.pride.tools.converter.utils.ConverterException;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rcote
 * Date: 21/10/11
 * Time: 16:25
 */
public abstract class AbstractForm extends JPanel implements ConverterForm {

    public static final String TEMPLATE_EXTENSION = ".xml";

    protected static final Logger logger = Logger.getLogger(AbstractForm.class);

    protected ValidationListener validationListerner;

    protected ResourceBundle bundle = ResourceBundle.getBundle("messages");

    protected ResourceBundle config = ResourceBundle.getBundle("gui-settings");

    protected boolean isLoaded = false;

    protected Set<String> suggestedCVs = new HashSet<String>();

    protected Map<JComponent, Boolean> defaultValueMap = new HashMap<JComponent, Boolean>();

    @Override
    public void loadTemplate(String templateName) {
        throw new UnsupportedOperationException("No handler method available to load templates for " + getClass().getName());
    }

    @Override
    public void addValidationListener(ValidationListener validationListerner) {
        this.validationListerner = validationListerner;
    }

    protected void validateRequiredField(Component source, KeyEvent event) {
        if (source instanceof JTextComponent) {
            JTextComponent text = (JTextComponent) source;
            String toValidate = text.getText();
            if (event != null) {
                toValidate += event.getKeyChar();
            }
            if (isNonNullTextField(toValidate)) {
                text.setBackground(Color.white);
            } else {
                text.setBackground(Color.pink);
            }
        }
    }

    protected boolean isNonNullTextField(String text) {
        return text != null && text.trim().length() > 0;
    }

    protected void updateTermButtonCvList(AddTermButton addTermButton, String configString) {
        String cvList = config.getString(configString);
        if (cvList != null) {
            String[] CVs = cvList.split(",");
            for (String cv : CVs) {
                suggestedCVs.add(cv.trim());
            }
            addTermButton.setSuggestedCVs(suggestedCVs);
        }
    }

    protected void saveTemplate(String templateName, TemplateType type, ReportObject templateObject) {
        File baseTemplatePath = TemplateUtilities.getUserTemplateDir();
        File templatePath = new File(baseTemplatePath, type.getTemplatePath());
        File template = new File(templatePath, templateName + TEMPLATE_EXTENSION);
        if (!template.exists()) {
            TemplateUtilities.writeTemplate(template, templateObject);
            JOptionPane.showMessageDialog(NavigationPanel.getInstance(), "Template saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            int value = JOptionPane.showConfirmDialog(NavigationPanel.getInstance(), "Template already exists. Click on OK to save and overwrite or CANCEL to abort", "Warning", JOptionPane.WARNING_MESSAGE);
            if (value == JOptionPane.OK_OPTION) {
                TemplateUtilities.writeTemplate(template, templateObject);
                JOptionPane.showMessageDialog(NavigationPanel.getInstance(), "Template saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    protected ReportObject loadTemplate(String templateName, TemplateType type) {
        File baseTemplatePath = TemplateUtilities.getUserTemplateDir();
        File templatePath = new File(baseTemplatePath, type.getTemplatePath());
        File template = new File(templatePath, templateName + TEMPLATE_EXTENSION);
        if (template.exists()) {
            return TemplateUtilities.loadTemplate(template, type.getObjectClass());
        } else {
            throw new ConverterException("Template " + templateName + " does not exist in " + templatePath.getAbsolutePath());
        }
    }

    protected String[] getTemplateNames(TemplateType type) {
        File baseTemplatePath = TemplateUtilities.getUserTemplateDir();
        File templatePath = new File(baseTemplatePath, type.getTemplatePath());
        String[] templates = templatePath.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(TEMPLATE_EXTENSION);
            }
        });
        Arrays.sort(templates, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        String[] filtered = new String[templates.length];
        int i = 0;
        for (String str : templates) {
            filtered[i++] = str.substring(0, str.lastIndexOf(TEMPLATE_EXTENSION));
        }
        return filtered;
    }

    protected Icon getFormIcon(String resourceId) {
        URL icomImage = getClass().getClassLoader().getResource(config.getString(resourceId));
        if (icomImage == null) {
            icomImage = getClass().getClassLoader().getResource("images/default.png");
        }
        return new ImageIcon(icomImage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConverterForm)) return false;
        ConverterForm that = (ConverterForm) o;
        return getFormName().equals(that.getFormName());
    }

    @Override
    public int hashCode() {
        return getFormName().hashCode();
    }
}
