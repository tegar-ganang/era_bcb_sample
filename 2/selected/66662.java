package org.formaria.swt;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.swt.widgets.Button;
import org.formaria.aria.ProjectManager;
import org.formaria.aria.RadioButtonGroup;
import org.formaria.aria.RadioHolder;
import org.formaria.aria.StateHolder;
import org.formaria.aria.ValueHolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A wrapper for the RadioButton class
 * <p>
 * Copyright (c) Formaria Ltd., <br>
 * License: see license.txt $Revision: 2.10 $
 */
public class RadioButton extends Button implements StateHolder, ValueHolder, RadioHolder, RadioButtonGroup {

    protected Object value;

    /**
   * Create a new radio button
   */
    public RadioButton(Object parent) {
        super((Composite) parent, SWT.RADIO);
    }

    /**
   * Suppress the subclassing exception
   */
    protected void checkSubclass() {
    }

    /**
   * Get the component state
   * 
   * @return the Boolean value for the state
   */
    public Object getComponentState() {
        return new Boolean(getSelection());
    }

    /**
   * Set the component state
   * 
   * @param o
   *          the selection state. Possible values: 1 or true and 0 or false
   */
    public void setComponentState(Object o) {
        if (o != null) {
            String objValue = o.toString();
            boolean value = objValue.equals("1");
            if (!value) value |= objValue.equals("true");
            setSelection(value);
        } else setSelection(false);
    }

    /**
   * Get the checkbox's value if it has one or else get the text
   * 
   * @return the value for this button
   */
    public Object getValue() {
        if (value != null) return value;
        return getText();
    }

    /**
   * Set the value associated with this button
   * 
   * @param newValue
   *          the new button value
   */
    public void setValue(Object newValue) {
        value = newValue;
    }

    /**
   * Create a new checkbox group and adds this radio button top the new group
   * 
   * @return the new group
   */
    public Object createGroup() {
        Composite composite = new Composite(getParent(), SWT.NULL);
        composite.setLayout(new RowLayout());
        return composite;
    }

    /**
   * Set the button group
   * 
   * @param grp
   *          the group control this radio button
   */
    public void setRadioButtonGroup(Object grp) {
    }

    /**
   * Gets the group controlling this radio button
   * 
   * @return the ButtonGroup
   */
    public Object getRadioButtonGroup() {
        return getParent();
    }

    /**
   * Gets the selected radio button
   * 
   * @return the selected radio button
   */
    public Object getSelectedObject() {
        Control[] c = getParent().getChildren();
        for (int i = 0; i < c.length; i++) {
            if (c[i] instanceof RadioButton) {
                RadioButton rb = (RadioButton) c[i];
                if (rb.getSelection()) return rb;
            }
        }
        return null;
    }

    /**
   * Select an item
   * 
   * @param object
   *          the index of the item to select
   */
    public void setSelectedObject(Object object) {
        Control[] c = getParent().getChildren();
        for (int i = 0; i < c.length; i++) {
            RadioButton rb = (RadioButton) c[i];
            if (rb.getValue().equals(object)) {
                rb.setSelection(true);
                return;
            }
        }
    }

    /**
   * Set the image url
   * @param imageUrl the relative image URL
   */
    public void setImage(String imageUrl) {
        try {
            InputStream url = ProjectManager.getCurrentProject().getUrl(imageUrl).openStream();
            Image im = new Image(getDisplay(), url);
            if (im != null) setImage(im);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
   * Set the tooltip text
   * @param text the new text
   */
    public void setToolTip(String text) {
        super.setToolTipText(text);
    }

    /**
   * Get the tooltip text
   * @return the existing text if any
   */
    public String getToolTip() {
        return super.getToolTipText();
    }
}
