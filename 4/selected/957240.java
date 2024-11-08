package org.springframework.binding.form;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.springframework.richclient.util.AbstractPropertyChangePublisher;

/**
 * Default implementation of FieldMetadata. 
 * <p>
 * NOTE: This is a framework internal class and should not be
 * instantiated in user code. 
 * 
 * @author Oliver Hutchison
 */
public class DefaultFieldMetadata extends AbstractPropertyChangePublisher implements FieldMetadata {

    private final DirtyChangeHandler dirtyChangeHandler = new DirtyChangeHandler();

    private final PropertyChangeListener formChangeHandler = new FormModelChangeHandler();

    private final FormModel formModel;

    private final FormModelMediatingValueModel valueModel;

    private final String field;

    private final Class fieldType;

    private final FieldFace fieldFace;

    private final boolean readable;

    private final boolean writeable;

    private boolean readOnly;

    private boolean oldReadOnly;

    private boolean enabled = true;

    private boolean oldEnabled = true;

    /**
     * Constructs a new instance of DefaultFieldMetadata. 
     * 
     * @param formModel the form model 
     * @param valueModel the value model for the property  
     * @param field the name of the field
     * @param fieldType the type of the field
     * @param forceReadOnly should readOnly be forced to true; this is required if the 
     * property can not be modified. e.g. at the PropertyAccessStrategy level.
     */
    public DefaultFieldMetadata(FormModel formModel, FormModelMediatingValueModel valueModel, String field, Class fieldType, FieldFace fieldFace, boolean readable, boolean writeable) {
        this.formModel = formModel;
        this.valueModel = valueModel;
        this.valueModel.addPropertyChangeListener(FormModelMediatingValueModel.DIRTY_PROPERTY, dirtyChangeHandler);
        this.field = field;
        this.fieldType = fieldType;
        this.fieldFace = fieldFace;
        this.readable = readable;
        this.writeable = writeable;
        this.formModel.addPropertyChangeListener(ENABLED_PROPERTY, formChangeHandler);
        this.oldReadOnly = isReadOnly();
        this.oldEnabled = isEnabled();
    }

    public String getName() {
        return field;
    }

    public FieldFace getFieldFace() {
        return fieldFace;
    }

    public boolean isReadOnly() {
        return (!writeable) || readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        readOnlyUpdated();
    }

    protected void readOnlyUpdated() {
        firePropertyChange(READ_ONLY_PROPERTY, oldReadOnly, isReadOnly());
        oldReadOnly = isReadOnly();
    }

    public boolean isEnabled() {
        return enabled && formModel.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        enabledUpdated();
    }

    protected void enabledUpdated() {
        firePropertyChange(ENABLED_PROPERTY, oldEnabled, isEnabled());
        oldEnabled = isEnabled();
    }

    public boolean isDirty() {
        return valueModel.isDirty();
    }

    public Class getType() {
        return fieldType;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWriteable() {
        return writeable;
    }

    public Object getUserMetadata(String key) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Propagates dirty changes from the value model on to 
     * the dirty change listeners attached to this class.
     */
    private class DirtyChangeHandler extends CommitListenerAdapter implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            firePropertyChange(DIRTY_PROPERTY, evt.getOldValue(), evt.getNewValue());
        }
    }

    /**
     * Responsible for listening for changes to the enabled 
     * property of the FormModel
     */
    private class FormModelChangeHandler implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (FormModel.ENABLED_PROPERTY.equals(evt.getPropertyName())) {
                firePropertyChange(ENABLED_PROPERTY, Boolean.valueOf(oldEnabled), Boolean.valueOf(isEnabled()));
                oldEnabled = isEnabled();
            }
        }
    }
}
