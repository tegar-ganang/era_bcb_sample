package com.metanology.mde.core.ui.metaProgramExplorer;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.TraverseEvent;
import com.metanology.mde.core.ui.plugin.MDEPlugin;
import com.metanology.mde.core.codeFactory.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.jface.dialogs.MessageDialog;

public class MetaProjectPropertiesForm extends Window {

    protected transient PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
        listeners.addPropertyChangeListener(listener);
    }

    public void firePropertyChange(String prop, Object old, Object newValue) {
        listeners.firePropertyChange(prop, old, newValue);
    }

    public void fireStructureChange(String prop, Object child) {
        listeners.firePropertyChange(prop, null, child);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
    }

    private String oldName = "";

    private static final int FORM_H = 100;

    private static final int FORM_W = 400;

    /**
     * Creates a new MetaProjectPropertiesForm.
     */
    public MetaProjectPropertiesForm(Shell parentShell) {
        super(parentShell);
        this.setShellStyle(SWT.CLOSE | SWT.RESIZE);
    }

    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(MDEPlugin.getResourceString(MSG_TITLE));
        Rectangle rect = newShell.getBounds();
        rect.height = FORM_H;
        rect.width = FORM_W;
        Rectangle dis_rect = MDEPlugin.getShell().getBounds();
        rect.x = dis_rect.x + Math.max(0, dis_rect.width / 2 - rect.width / 2);
        rect.y = dis_rect.y + Math.max(0, dis_rect.height / 2 - rect.height / 2);
        newShell.setBounds(rect);
    }

    void handleSave() {
        String newName = name.getText();
        boolean toRename = oldName.length() > 0 && !newName.equals(oldName);
        boolean toCreate = oldName.length() <= 0 && newName.length() > 0;
        if (toRename || toCreate) {
            MetaProject mp = MDEPlugin.getDefault().getRuntime().getMetaSolution().getMetaProject(newName);
            if (mp != null) {
                if (MessageDialog.openConfirm(this.getShell(), this.getShell().getText(), "MetaProject [" + newName + "] already exists. Choose <OK> to overwrite it.")) {
                    MDEPlugin.getDefault().getRuntime().getMetaSolution().removeMetaProject(mp);
                } else {
                    this.handleCancel();
                    return;
                }
            }
        }
        this.populateServerObjects();
        if (toCreate) {
            java.util.Collection mprojs = MDEPlugin.getDefault().getRuntime().getMetaSolution().getAllMetaProjects();
            if (!mprojs.contains(mproj)) mprojs.add(this.mproj);
        }
        String err = null;
        try {
            MDEPlugin.getDefault().getRuntime().saveMetaSolution();
            if (toRename) {
                this.firePropertyChange("", oldName, this.getMproj());
            }
        } catch (IOException e) {
            err = e.getMessage();
        }
        this.close();
        if (err != null) {
            if (toRename) {
                mproj.setName(oldName);
            } else if (toCreate) {
                MDEPlugin.getDefault().getRuntime().getMetaSolution().removeMetaProject(mproj);
            }
            MDEPlugin.showMessage(err);
        }
    }

    void handleCancel() {
        this.close();
    }

    protected Control createContents(Composite parent) {
        Composite composite = (Composite) super.createContents(parent);
        this.init(composite);
        this.doLayout(composite);
        this.populateUIControls();
        parent.pack(true);
        Rectangle r = parent.getBounds();
        r.height += 10;
        r.width = FORM_W;
        parent.setBounds(r);
        return composite;
    }

    private void registerListener() {
        this.save.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                handleSave();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                handleSave();
            }
        });
        this.getShell().setDefaultButton(this.save);
        this.cancel.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                handleCancel();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                handleCancel();
            }
        });
        this.getShell().addTraverseListener(new TraverseListener() {

            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    e.doit = true;
                }
            }
        });
        this.name.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                int s = name.getSelection().x;
                String t = name.getText();
                if (t != null) {
                    StringBuffer b = new StringBuffer();
                    for (int i = 0; i < t.length(); i++) {
                        char c = t.charAt(i);
                        boolean skiped = false;
                        if (i == 0 && (Character.isLowerCase(c) || Character.isUpperCase(c) || c == '$' || c == '_')) {
                            b.append(c);
                        } else if (i > 0 && (Character.isLowerCase(c) || Character.isUpperCase(c) || Character.isDigit(c) || c == '$' || c == '_')) {
                            b.append(c);
                        } else {
                            skiped = true;
                        }
                        if (skiped && i <= s) {
                            s--;
                        }
                    }
                    t = b.toString();
                }
                if (t != null && t.length() > 0) {
                    if (!t.equals(name.getText())) {
                        name.setText(t);
                        name.setSelection(s);
                    }
                    name.setSelection(s);
                    save.setEnabled(true);
                } else {
                    save.setEnabled(false);
                }
            }
        });
    }

    protected void init(Composite composite) {
        labelName = new Label(composite, SWT.RIGHT);
        labelName.setText(MDEPlugin.getResourceString(MetaProjectPropertiesForm.MSG_NAME));
        name = new Text(composite, SWT.SINGLE | SWT.BORDER);
        save = new Button(composite, SWT.PUSH);
        save.setText(MDEPlugin.getResourceString(MetaProjectPropertiesForm.MSG_SAVE));
        cancel = new Button(composite, SWT.PUSH);
        cancel.setText(MDEPlugin.getResourceString(MetaProjectPropertiesForm.MSG_CANCEL));
        this.registerListener();
    }

    protected void doLayout(Composite composite) {
        FormLayout layout = new FormLayout();
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        FormData data = null;
        data = new FormData();
        data.top = new FormAttachment(0, 10);
        data.left = new FormAttachment(0, 10);
        labelName.setLayoutData(data);
        data = new FormData();
        data.top = new FormAttachment(0, 10);
        data.left = new FormAttachment(labelName, 10);
        data.right = new FormAttachment(100, -10);
        name.setLayoutData(data);
        data = new FormData();
        data.right = new FormAttachment(100, -10);
        data.top = new FormAttachment(name, +10);
        cancel.setLayoutData(data);
        data = new FormData();
        data.right = new FormAttachment(cancel, -10);
        data.top = new FormAttachment(name, +10);
        save.setLayoutData(data);
    }

    /** 
     * Populate all the ui controls
     * It will be invoked by createContents / createDialogArea
     */
    public void populateUIControls() {
        if (mproj != null) {
            name.setText(mproj.getName());
        }
        if (mproj != null) {
            oldName = mproj.getName();
        }
    }

    /** 
     * Populate all the objects providing data
     * for the UI controls
     * It should be invoked by user defined action 
     * implementation methods.
     */
    public void populateServerObjects() {
        this.getMproj().setName(name.getText().trim());
    }

    public com.metanology.mde.core.codeFactory.MetaProject getMproj() {
        if (mproj == null) {
            mproj = new com.metanology.mde.core.codeFactory.MetaProject();
        }
        return mproj;
    }

    public void setMproj(com.metanology.mde.core.codeFactory.MetaProject val) {
        mproj = val;
    }

    protected Label labelName;

    protected Text name;

    protected Button save;

    protected Button cancel;

    private com.metanology.mde.core.codeFactory.MetaProject mproj;

    public static final String MSG_TITLE = "MetaProjectPropertiesForm.title";

    public static final String MSG_NAME = "MetaProjectPropertiesForm.name";

    public static final String MSG_SAVE = "MetaProjectPropertiesForm.save";

    public static final String MSG_CANCEL = "MetaProjectPropertiesForm.cancel";
}
