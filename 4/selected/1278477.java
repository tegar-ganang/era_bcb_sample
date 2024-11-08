package com.vircon.myajax.web;

import java.util.Locale;
import com.vircon.myajax.validation.Validator;
import com.vircon.myajax.web.event.ChangeEvent;

/**
 * This class is abstract base class for form fields, it contain many capabilities share by form fields.
 *
 * @version     
 * @author 	Byron Warner
 * @see S
 */
public abstract class FormField extends BrowserComponentImpl implements SubmitTarget {

    public static final String ID = "id";

    public static final String DISABLED = "disabled";

    public static final String READONLY = "readonly";

    public static final String VALIDATION_EVENT = "validationEvent";

    public static final String VALIDATION_DELAY = "validationDelay";

    public static final String LABEL_SEPARATOR = "labelSeparator";

    public static final String INVALID_TEXT = "invalidText";

    public static final String TAB_INDEX = "tabIndex";

    public static final String TARGET_ERROR_ID = "targetErrorId";

    public static final String NAME = "name";

    public static final String LABEL_TEXT = "labelText";

    public FormField(String aId) {
        super(aId);
    }

    public FormField() {
        super();
    }

    public void validate(ChangeEvent event) {
        String changedValue = event.getChangedValue();
        if (validator != null) {
            if (!validator.isValid(changedValue)) {
                event.getResponse().getMessages().add(new ValidationMessage(getId(), validator.getMessage(Locale.US), targetErrorId));
                event.getResponse().setFieldValid(false);
                this.setCssClass(getPage().getErrorClass());
            } else {
                if (getPage().getErrorClass().equals(getCssClass())) {
                    this.setCssClass("");
                }
            }
        }
    }

    protected void internalOnChange(ChangeEvent event) {
        validate(event);
        super.onChange(event);
    }

    public Validator getValidator() {
        return validator;
    }

    public String getTargetErrorId() {
        return targetErrorId;
    }

    public void setTargetErrorId(String aTargetErrorId) {
        targetErrorId = aTargetErrorId;
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String aLabelText) {
        labelText = aLabelText;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int aTabIndex) {
        tabIndex = aTabIndex;
    }

    public String getInvalidText() {
        return invalidText;
    }

    public void setInvalidText(String aInvalidText) {
        invalidText = aInvalidText;
    }

    public String getLabelSeparator() {
        return labelSeparator;
    }

    public void setLabelSeparator(String aLabelSeparator) {
        labelSeparator = aLabelSeparator;
    }

    public int getValidationDelay() {
        return validationDelay;
    }

    public void setValidationDelay(int aValidationDelay) {
        validationDelay = aValidationDelay;
    }

    public String getValidationEvent() {
        return validationEvent;
    }

    public void setValidationEvent(String aValidationEvent) {
        validationEvent = aValidationEvent;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean aDisabled) {
        disabled = aDisabled;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean aReadonly) {
        readonly = aReadonly;
    }

    public String getName() {
        return name == null ? getId() : name;
    }

    public void setName(String aName) {
        name = aName;
    }

    @Override
    public void setProperty(String name, String value) {
        if (LABEL_TEXT.equalsIgnoreCase(name)) {
            setLabelText(value);
        } else if (NAME.equalsIgnoreCase(name)) {
            setName(value);
        } else if (TARGET_ERROR_ID.equalsIgnoreCase(name)) {
            setTargetErrorId(value);
        } else if (TAB_INDEX.equalsIgnoreCase(name)) {
            setTabIndex(Integer.valueOf(value));
        } else if (INVALID_TEXT.equalsIgnoreCase(name)) {
            setInvalidText(value);
        } else if (LABEL_SEPARATOR.equalsIgnoreCase(name)) {
            setLabelSeparator(value);
        } else if (LABEL_TEXT.equalsIgnoreCase(name)) {
            setLabelText(value);
        } else if (VALIDATION_DELAY.equalsIgnoreCase(name)) {
            setValidationDelay(Integer.valueOf(value));
        } else if (VALIDATION_EVENT.equalsIgnoreCase(name)) {
            setValidationEvent(value);
        } else if (READONLY.equalsIgnoreCase(name)) {
            setReadonly(Boolean.valueOf(name));
        } else if (DISABLED.equalsIgnoreCase(name)) {
            this.setDisabled(Boolean.valueOf(name));
        } else {
            super.setProperty(name, value);
        }
    }

    @Override
    public void writeAttributes(XMLWriter writer) {
        super.writeAttributes(writer);
        writer.addAttribute(FormField.LABEL_TEXT, labelText);
        writer.addAttribute(FormField.NAME, getName());
        writer.addAttribute(FormField.TARGET_ERROR_ID, targetErrorId);
        if (tabIndex != -1) {
            writer.addAttribute(FormField.TAB_INDEX, tabIndex);
        }
        writer.addAttribute(FormField.INVALID_TEXT, invalidText);
        writer.addAttribute(FormField.LABEL_SEPARATOR, labelSeparator);
        if (validationDelay > 0) {
            writer.addAttribute(FormField.VALIDATION_DELAY, validationDelay);
        }
        writer.addAttribute(FormField.VALIDATION_EVENT, validationEvent);
        if (readonly) {
            writer.addAttribute(FormField.READONLY, readonly);
        }
        if (disabled) {
            writer.addAttribute(FormField.DISABLED, disabled);
        }
    }

    public void setValidator(Validator aValidator) {
        validator = aValidator;
    }

    private Validator validator;

    private String labelText;

    private String name;

    private String targetErrorId;

    private int tabIndex = -1;

    private String invalidText;

    private String labelSeparator;

    private int validationDelay;

    private String validationEvent;

    private boolean disabled;

    private boolean readonly;

    private static final long serialVersionUID = 4491145517831218515L;
}
