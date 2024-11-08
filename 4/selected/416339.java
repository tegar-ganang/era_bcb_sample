package org.logitest.variablereader;

import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import java.util.*;
import org.jdom.*;
import org.apache.oro.text.perl.Perl5Util;
import org.logitest.*;

/** A variable reader that just returns a literal value
	
	@author Clancy Malcolm
*/
public class LiteralVariable extends VariableReader {

    /** Default constructor. */
    public LiteralVariable() {
    }

    /** Get the regular expression.
	
		@return The regular expression
	*/
    public String getValue() {
        return valueString;
    }

    /** Set the regular expression.
	
		@param valueString The regular expression
	*/
    public void setValue(String valueString) {
        this.valueString = valueString;
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
        writeTo.set(getVariableName(), getValue());
    }

    public void readConfiguration(Element element) {
        Element valueElement = element.getChild("value");
        setVariableName(valueElement.getAttributeValue("variableName"));
        setValue(valueElement.getTextTrim());
    }

    public void writeConfiguration(Element element) {
        Element valueElement = new Element("value");
        valueElement.addAttribute("variableName", getVariableName());
        valueElement.addContent(getValue());
        element.addContent(valueElement);
    }

    public boolean save() {
        Editor editor = (Editor) getEditor();
        setValue(editor.valueField.getText());
        setVariableName(editor.variableNameField.getText());
        return true;
    }

    public void revert() {
        Editor editor = (Editor) getEditor();
        editor.valueField.setText(getValue());
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
            properties.put(DISPLAY_NAME, "Literal Variable");
            properties.put(DESCRIPTION, "Set a variable to a literal value.");
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
            valueLabel = new JLabel("Value:");
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            gbl.setConstraints(valueLabel, gbc);
            add(valueLabel);
            valueField = new JTextField();
            valueField.setColumns(36);
            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbl.setConstraints(valueField, gbc);
            add(valueField);
            variableNameLabel = new JLabel("Variable Name:");
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

        JLabel valueLabel;

        JTextField valueField;

        JLabel variableNameLabel;

        JTextField variableNameField;
    }

    private Map properties;

    private String valueString;

    private String variableName;

    private Editor editor;
}
