package org.logitest.variablereader;

import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import java.util.*;
import org.jdom.*;
import org.apache.oro.text.perl.Perl5Util;
import org.logitest.*;

/** A variable reader that uses regular expressions to find
	variable values from a resource's HTML
	
	@author Clancy Malcolm
*/
public class RegexVariableReader extends VariableReader {

    /** Default constructor. */
    public RegexVariableReader() {
    }

    /** Get the regular expression.
	
		@return The regular expression
	*/
    public String getRegex() {
        return regexString;
    }

    /** Set the regular expression.
	
		@param regexString The regular expression
	*/
    public void setRegex(String regexString) {
        this.regexString = regexString;
    }

    /** Get the variableName.
	
		@return The variableName
	*/
    public String getVariableName() {
        return variableName;
    }

    /** Set the variableName.
	
		@param variableName The variableName
	*/
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    /** Reads the variables from the resource.

		@param resource The resource to read the variables from
		@param writeTo The variables object to write the variables to
	*/
    public void readVariables(Resource resource, Variables writeTo) {
        Hashtable variables = new Hashtable();
        String text = resource.getText();
        Perl5Util matcher = new Perl5Util();
        if (matcher.match("/" + getRegex() + "/", text)) {
            String value = matcher.group(1);
            writeTo.set(getVariableName(), value);
        } else {
            writeTo.set(getVariableName(), null);
        }
    }

    public void readConfiguration(Element element) {
        Element regexElement = element.getChild("regex");
        setVariableName(regexElement.getAttributeValue("variableName"));
        setRegex(regexElement.getTextTrim());
    }

    public void writeConfiguration(Element element) {
        Element regexElement = new Element("regex");
        regexElement.addAttribute("variableName", getVariableName());
        regexElement.addContent(getRegex());
        element.addContent(regexElement);
    }

    public boolean save() {
        Editor editor = (Editor) getEditor();
        setRegex(editor.regexField.getText());
        setVariableName(editor.variableNameField.getText());
        return true;
    }

    public void revert() {
        Editor editor = (Editor) getEditor();
        editor.regexField.setText(getRegex());
        editor.variableNameField.setText(getVariableName());
    }

    public Component getEditor() {
        if (editor == null) {
            editor = new Editor();
        }
        return editor;
    }

    /** Get a String to display in the title bar.
	
		@return A title String
	*/
    public String getEditorTitle() {
        return toString();
    }

    protected Map getProperties() {
        if (properties == null) {
            properties = new HashMap();
            properties.put(DISPLAY_NAME, "Regex VariableReader");
            properties.put(DESCRIPTION, "Set a variable using a regular expression match.");
            properties.put(AUTHOR, "Clancy Malcolm");
            properties.put(VERSION, "1.0");
        }
        return properties;
    }

    class Editor extends JPanel {

        public Editor() {
            init();
        }

        private void init() {
            GridBagLayout gbl = new GridBagLayout();
            GridBagConstraints gbc = new GridBagConstraints();
            setLayout(gbl);
            gbc.insets = new Insets(1, 1, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            regexLabel = new JLabel("Regex:");
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            gbl.setConstraints(regexLabel, gbc);
            add(regexLabel);
            regexField = new JTextField();
            regexField.setColumns(36);
            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbl.setConstraints(regexField, gbc);
            add(regexField);
            variableNameLabel = new JLabel("VariableName:");
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            gbl.setConstraints(variableNameLabel, gbc);
            add(variableNameLabel);
            variableNameField = new JTextField();
            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbl.setConstraints(variableNameField, gbc);
            add(variableNameField);
        }

        JLabel regexLabel;

        JTextField regexField;

        JLabel variableNameLabel;

        JTextField variableNameField;
    }

    private Map properties;

    private String regexString;

    private String variableName;

    private Editor editor;
}
