package com.manydesigns.portofino.methods.scrud;

import com.manydesigns.portofino.base.*;
import com.manydesigns.portofino.base.navigation.Navigation;
import com.manydesigns.portofino.base.operations.MDObjectOperationVisitor;
import com.manydesigns.portofino.base.operations.MDOperation;
import com.manydesigns.portofino.base.permissions.PermissionException;
import com.manydesigns.portofino.base.users.User;
import com.manydesigns.portofino.base.util.MDObjectCanReadFilter;
import com.manydesigns.portofino.base.util.MDObjectInDetailsFilter;
import com.manydesigns.portofino.base.workflow.MDObjectWfTransitionVisitor;
import com.manydesigns.portofino.base.workflow.MDWfState;
import com.manydesigns.portofino.base.workflow.MDWfTransition;
import com.manydesigns.portofino.methods.Breadcrumbs;
import com.manydesigns.portofino.methods.LoginRequiredException;
import com.manydesigns.portofino.util.Defs;
import com.manydesigns.portofino.util.Escape;
import com.manydesigns.portofino.util.Util;
import com.manydesigns.xmlbuffer.XhtmlBuffer;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Date;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

/**
 * @author predo
 */
public class CreateUpdateForm {

    public static final String copyright = "Copyright (c) 2005-2009, ManyDesigns srl";

    public static final String BLOB_KEEP = "1";

    public static final String BLOB_MODIFY = "2";

    public static final String BLOB_DELETE = "3";

    private final HttpServletRequest req;

    private final MDObject obj;

    private final MDClass cls;

    private final MDConfig config;

    private final Locale locale;

    private final String successReturnUrl;

    private final String cancelReturnUrl;

    private final HashMap<MDAttribute, Object> localValues;

    private final HashMap backupValues;

    private final HashMap<Object, String> stringValues;

    private final HashMap<MDBlobAttribute, MDBlob> blobValues;

    private final HashMap<MDAttribute, Boolean> writableMap;

    private final Collection<String> errors;

    private final boolean create;

    private final String oldName;

    private boolean canCreate;

    private boolean canUpdate;

    private boolean actionPerformed;

    private MDWfTransition selectedWft;

    private final User saveUser;

    public CreateUpdateForm(HttpServletRequest req, MDObject obj, String successReturnUrl, String cancelReturnUrl) throws Exception {
        this.req = req;
        this.obj = obj;
        cls = obj.getActualClass();
        config = cls.getConfig();
        locale = config.getLocale();
        this.successReturnUrl = successReturnUrl;
        this.cancelReturnUrl = cancelReturnUrl;
        localValues = new HashMap<MDAttribute, Object>();
        stringValues = new HashMap<Object, String>();
        blobValues = new HashMap<MDBlobAttribute, MDBlob>();
        writableMap = new HashMap<MDAttribute, Boolean>();
        errors = new ArrayList<String>();
        create = obj.getLifecycleState() == MDObject.NEW || obj.getLifecycleState() == MDObject.CREATING;
        if (create) {
            oldName = null;
        } else {
            oldName = obj.getName();
        }
        cleanSelection();
        MDObjectVisitor loadVisitor = new LoadVisitor();
        saveUser = config.getCurrentUser();
        try {
            config.setCurrentUser(null);
            obj.visit(loadVisitor);
            backupValues = (HashMap) localValues.clone();
            MDObjectVisitor applyVisitor = MDObjectInDetailsFilter.wrap(new ApplyVisitor());
            if (create) {
                MDClassVisitor writeBackVisitor = new WriteBackVisitor();
                obj.visit(applyVisitor);
                cls.visit(writeBackVisitor, false);
                if (saveUser != null && !cls.isPermissionFree()) {
                    config.setCurrentUser(saveUser);
                    errors.clear();
                    obj.visit(applyVisitor);
                    cls.visit(writeBackVisitor, false);
                    errors.clear();
                    obj.visit(applyVisitor);
                    cls.visit(writeBackVisitor, false);
                }
            } else {
                config.setCurrentUser(saveUser);
                obj.visit(applyVisitor);
            }
        } finally {
            config.setCurrentUser(saveUser);
        }
        if (!canPerformMainAction()) {
            throw new PermissionException(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Permission_denied"));
        }
    }

    private boolean canPerformMainAction() {
        return (create && canCreate) || (!create && canUpdate);
    }

    private void cleanSelection() {
        canCreate = false;
        canUpdate = false;
        actionPerformed = false;
        selectedWft = null;
    }

    public MDObject getMDObject() {
        return obj;
    }

    public String getReturnurl() {
        return successReturnUrl;
    }

    public Collection<String> getErrors() {
        return errors;
    }

    public boolean isActionPerformed() {
        return actionPerformed;
    }

    public MDWfTransition getSelectedWft() {
        return selectedWft;
    }

    public void consolidate() throws Exception {
        MDClassVisitor visitor = new WriteBackVisitor();
        cls.visit(visitor, false);
    }

    public void render(XhtmlBuffer xb) throws Exception {
        MDObjectVisitor visitor = MDObjectCanReadFilter.wrap(MDObjectInDetailsFilter.wrap(new RenderVisitor(xb)));
        obj.visit(visitor);
    }

    /**
     * *************************************************
     * LoadVisitor                    *
     * **************************************************
     */
    private class LoadVisitor implements MDObjectVisitor, MDObjectAttributeVisitor {

        public void doObjectPre(MDObject obj) throws Exception {
        }

        public void doAttributeListPre() throws Exception {
        }

        public void doAttributeGroupPre(String groupName) throws Exception {
        }

        public void doIntegerAttribute(MDIntegerAttribute attribute, Integer value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            localValues.put(attribute, value);
            stringValues.put(attribute, attribute.formatValue(value));
        }

        public void doTextAttribute(MDTextAttribute attribute, String value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            localValues.put(attribute, value);
        }

        public void doBooleanAttribute(MDBooleanAttribute attribute, Boolean value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            localValues.put(attribute, value);
        }

        public void doDateAttribute(MDDateAttribute attribute, Date value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            localValues.put(attribute, value);
            stringValues.put(attribute, attribute.formatValue(value));
        }

        public void doDecimalAttribute(MDDecimalAttribute attribute, BigDecimal value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            localValues.put(attribute, value);
            stringValues.put(attribute, attribute.formatValue(value));
        }

        public void doWfAttribute(MDWfAttribute attribute, MDWfState state, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            localValues.put(attribute, state);
        }

        public void doRelAttribute(MDRelAttribute attribute, Integer value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (create && config.isLoginEnabled() && attribute.isAutoConnectoToUser()) {
                if (saveUser == null || saveUser.getUserId() == 0) {
                    throw new LoginRequiredException();
                }
            }
            localValues.put(attribute, value);
        }

        public void doBlobAttribute(MDBlobAttribute attribute, MDBlob value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            localValues.put(attribute, value);
        }

        public void doAttributeGroupPost() throws Exception {
        }

        public void doAttributeListPost() throws Exception {
        }

        public void doObjectPost() throws Exception {
        }
    }

    /**
     * *************************************************
     * ApplyVisitor                    *
     * **************************************************
     */
    private class ApplyVisitor implements MDObjectAttributeVisitor, MDObjectWfTransitionVisitor, MDObjectOperationVisitor {

        public void doObjectPre(MDObject obj) throws Exception {
            cleanSelection();
            String actionString = create ? "create" : "update";
            actionPerformed = (req.getParameter(actionString) != null);
        }

        public void doAttributeListPre() throws Exception {
        }

        public void doAttributeGroupPre(String groupName) throws Exception {
        }

        public void doIntegerAttribute(MDIntegerAttribute attribute, Integer value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            Integer localValue = null;
            boolean isWritable = canWrite && !calculated;
            writableMap.put(attribute, isWritable);
            if (isWritable) {
                canUpdate = true;
                String stringValue = req.getParameter("attr_" + attribute.getName());
                if (stringValue == null) {
                    return;
                }
                stringValue = stringValue.trim();
                stringValues.put(attribute, stringValue);
                if (stringValue.length() > 0) {
                    try {
                        localValue = attribute.parseValue(stringValue);
                    } catch (Exception e) {
                        String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Wrong_field_Insert_integer"), attribute.getPrettyName());
                        errors.add(msg);
                    }
                } else if (attribute.isRequired()) {
                    String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Field_required"), attribute.getPrettyName());
                    errors.add(msg);
                }
            } else {
                localValue = (Integer) backupValues.get(attribute);
            }
            localValues.put(attribute, localValue);
        }

        public void doTextAttribute(MDTextAttribute attribute, String value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            String localValue;
            boolean isWritable = canWrite && !calculated;
            writableMap.put(attribute, isWritable);
            if (isWritable) {
                canUpdate = true;
                localValue = req.getParameter("attr_" + attribute.getName());
                if (localValue == null) {
                    return;
                }
                localValue = localValue.trim();
                if (localValue.length() == 0) {
                    localValue = null;
                    if (attribute.isRequired()) {
                        String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Field_required"), attribute.getPrettyName());
                        errors.add(msg);
                    }
                } else if (localValue.length() > attribute.getLength()) {
                    String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Value_of_field_longer_than_permitted"), localValue, attribute.getPrettyName(), attribute.getLength());
                    errors.add(msg);
                }
            } else {
                localValue = (String) backupValues.get(attribute);
            }
            localValues.put(attribute, localValue);
        }

        public void doBooleanAttribute(MDBooleanAttribute attribute, Boolean value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            Boolean localValue;
            boolean isWritable = canWrite && !calculated;
            writableMap.put(attribute, isWritable);
            if (isWritable) {
                canUpdate = true;
                String stringValue = req.getParameter("attr_" + attribute.getName());
                String checkValue = req.getParameter("attr_" + attribute.getName() + "_ctrlcheckbox");
                if (stringValue == null && checkValue == null) {
                    return;
                }
                if (stringValue == null) {
                    localValue = Boolean.FALSE;
                } else {
                    localValue = Boolean.TRUE;
                }
            } else {
                localValue = (Boolean) backupValues.get(attribute);
            }
            localValues.put(attribute, localValue);
        }

        public void doDateAttribute(MDDateAttribute attribute, Date value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            Date localValue = null;
            boolean isWritable = canWrite && !calculated;
            writableMap.put(attribute, isWritable);
            if (isWritable) {
                canUpdate = true;
                String stringValue = req.getParameter("attr_" + attribute.getName());
                if (stringValue == null) {
                    return;
                }
                stringValue = stringValue.trim();
                stringValues.put(attribute, stringValue);
                if (stringValue.length() > 0) {
                    try {
                        localValue = attribute.parseValue(stringValue);
                    } catch (Exception e) {
                        String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Wrong_field_Insert_data"), attribute.getPrettyName());
                        errors.add(msg);
                    }
                } else if (attribute.isRequired()) {
                    String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Field_required"), attribute.getPrettyName());
                    errors.add(msg);
                }
            } else {
                localValue = (Date) backupValues.get(attribute);
            }
            localValues.put(attribute, localValue);
        }

        public void doDecimalAttribute(MDDecimalAttribute attribute, BigDecimal value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            BigDecimal localValue = null;
            boolean isWritable = canWrite && !calculated;
            writableMap.put(attribute, isWritable);
            if (isWritable) {
                canUpdate = true;
                String stringValue = req.getParameter("attr_" + attribute.getName());
                if (stringValue == null) {
                    return;
                }
                stringValue = stringValue.trim();
                stringValues.put(attribute, stringValue);
                if (stringValue.length() > 0) {
                    try {
                        localValue = attribute.parseValue(stringValue);
                    } catch (Exception e) {
                        String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Wrong_field_Insert_decimal"), attribute.getPrettyName());
                        errors.add(msg);
                    }
                } else if (attribute.isRequired()) {
                    String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Field_required"), attribute.getPrettyName());
                    errors.add(msg);
                }
            } else {
                localValue = (BigDecimal) backupValues.get(attribute);
            }
            localValues.put(attribute, localValue);
        }

        public void doWfAttribute(MDWfAttribute attribute, MDWfState state, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            MDWfState localValue = null;
            boolean isWritable = canWrite && !calculated;
            writableMap.put(attribute, isWritable);
            if (isWritable) {
                canUpdate = true;
                String stringValue = req.getParameter("attr_" + attribute.getName());
                if (stringValue == null) {
                    return;
                }
                stringValue = stringValue.trim();
                if (stringValue.length() > 0) {
                    try {
                        int stateId = Integer.parseInt(stringValue);
                        localValue = config.getWfStateById(stateId);
                    } catch (Exception e) {
                        String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Wrong_field_Insert_state"), attribute.getPrettyName());
                        errors.add(msg);
                    }
                } else if (attribute.isRequired()) {
                    String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Field_required"), attribute.getPrettyName());
                    errors.add(msg);
                }
            } else {
                localValue = (MDWfState) backupValues.get(attribute);
            }
            localValues.put(attribute, localValue);
        }

        public void doRelAttribute(MDRelAttribute attribute, Integer value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            Integer localValue = null;
            boolean isWritable = canWrite && !calculated && (create || !attribute.isImmutable()) && (!create || !attribute.isAutoConnectoToUser());
            writableMap.put(attribute, isWritable);
            if (isWritable) {
                canUpdate = true;
                String stringValue = req.getParameter("attr_" + attribute.getName());
                String chkValue = req.getParameter("chk_attr_" + attribute.getName());
                if (chkValue != null && stringValue == null) {
                    stringValue = "";
                }
                if (stringValue == null) {
                    return;
                }
                stringValue = stringValue.trim();
                if (stringValue.length() > 0) {
                    try {
                        localValue = new Integer(stringValue);
                    } catch (Exception e) {
                        String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Wrong_value"), attribute.getPrettyName());
                        errors.add(msg);
                    }
                } else if (attribute.isRequired()) {
                    String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Field_required"), attribute.getPrettyName());
                    errors.add(msg);
                }
            } else {
                localValue = (Integer) backupValues.get(attribute);
            }
            localValues.put(attribute, localValue);
        }

        public void doBlobAttribute(MDBlobAttribute attribute, MDBlob value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            MDBlob localValue;
            boolean isWritable = canWrite && !calculated;
            writableMap.put(attribute, isWritable);
            if (isWritable) {
                canUpdate = true;
                if (create) {
                    localValue = handleBlob(attribute);
                } else {
                    String updateTypeStr = req.getParameter("attrblob_" + attribute.getName());
                    if (updateTypeStr == null) {
                        return;
                    } else if (updateTypeStr.equals(BLOB_KEEP)) {
                        return;
                    } else if (updateTypeStr.equals(BLOB_MODIFY)) {
                        localValue = handleBlob(attribute);
                    } else if (updateTypeStr.equals(BLOB_DELETE)) {
                        localValue = null;
                        if (attribute.isRequired()) {
                            String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Field_required"), attribute.getPrettyName());
                            errors.add(msg);
                        }
                    } else {
                        return;
                    }
                }
            } else {
                localValue = (MDBlob) backupValues.get(attribute);
            }
            localValues.put(attribute, localValue);
        }

        private MDBlob handleBlob(MDBlobAttribute attribute) {
            MDBlob returnValue = blobValues.get(attribute);
            if (returnValue != null) {
                return returnValue;
            }
            FileItem item = (FileItem) req.getParameterMap().get("attr_" + attribute.getName());
            if (item == null) {
                return null;
            }
            String stringValue = FilenameUtils.getName(item.getName()).trim();
            stringValue = stringValue.trim();
            stringValues.put(attribute, stringValue);
            if (stringValue.length() > 0) {
                try {
                    int maxOid = config.getNextBlobId();
                    returnValue = new MDBlob(maxOid, stringValue);
                    attribute.writeBlob("" + maxOid, item.getInputStream());
                    blobValues.put(attribute, returnValue);
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }
            } else if (attribute.isRequired()) {
                String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Field_required"), attribute.getPrettyName());
                errors.add(msg);
            }
            return returnValue;
        }

        public void doAttributeGroupPost() throws Exception {
        }

        public void doAttributeListPost() throws Exception {
        }

        public void doWfTransitionListPre() throws Exception {
        }

        public void doWfTransition(MDWfTransition wft, boolean canDo, boolean enabled) throws Exception {
            if (!enabled || !canDo) {
                return;
            }
            String paramName = "wft" + wft.getId();
            if (req.getParameter(paramName) != null) {
                actionPerformed = true;
                selectedWft = wft;
            }
        }

        public void doWfTransitionListPost() throws Exception {
        }

        public void doOperationListPre() throws Exception {
        }

        public void doOperation(MDOperation op, boolean canDo) throws Exception {
            if (!canDo) {
                return;
            }
            switch(op.getId()) {
                case MDOperation.CREATE:
                    canCreate = true;
                    break;
                default:
            }
        }

        public void doOperationListPost() throws Exception {
        }

        public void doObjectPost() throws Exception {
        }
    }

    /**
     * *************************************************
     * RenderVisitor                   *
     * **************************************************
     */
    private class RenderVisitor implements MDObjectAttributeVisitor, MDObjectWfTransitionVisitor {

        private final XhtmlBuffer xb;

        private boolean required;

        private boolean focusOnFirst;

        private String currentGroupName;

        private boolean currentGroupPrinted;

        public RenderVisitor(XhtmlBuffer xb) {
            this.xb = xb;
        }

        public void doObjectPre(MDObject obj) throws Exception {
            String htmlTitle;
            String pageTitle;
            if (create) {
                htmlTitle = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Create_"), cls.getPrettyName());
                pageTitle = Util.getLocalizedString(Defs.MDLIBI18N, locale, "Creation");
            } else {
                htmlTitle = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Update_object"), oldName);
                pageTitle = htmlTitle;
            }
            Navigation navigation = config.getNavigation();
            navigation.writePageHeader(xb, htmlTitle, cancelReturnUrl, cls, (create ? Navigation.CREATE_REFERENCE : null), req);
            xb.openElement("div");
            xb.addAttribute("id", "body");
            xb.openElement("div");
            xb.addAttribute("id", "title");
            xb.addAttribute("class", "c" + cls.getId());
            xb.openElement("div");
            xb.addAttribute("id", "breadcrumbs");
            Breadcrumbs.doBreadcrumbs(xb, obj);
            xb.closeElement("div");
            xb.writeH1(pageTitle);
            xb.closeElement("div");
            Util.printErrors(xb, errors);
            xb.openElement("div");
            xb.addAttribute("id", "details");
            String actionString = create ? "Create" : "Update";
            xb.openFormElement("inputform", "post", actionString, null);
            if (cls.hasBlobAttribute()) {
                xb.addAttribute("enctype", "multipart/form-data");
            }
            required = false;
            focusOnFirst = true;
        }

        public void doAttributeListPre() throws Exception {
        }

        private void openAttributeGroup(String groupName, String cssClass) {
            xb.openElement("fieldset");
            if (groupName == null) {
                if (cssClass == null) {
                    cssClass = "nolegend";
                } else {
                    cssClass = cssClass + " nolegend";
                }
            }
            if (cssClass != null) {
                xb.addAttribute("class", cssClass);
            }
            if (groupName != null) {
                xb.writeLegend(groupName, null);
            }
            xb.openElement("table");
            xb.addAttribute("class", "details");
            currentGroupPrinted = true;
        }

        private void closeAttributeGroup() {
            xb.closeElement("table");
            xb.closeElement("fieldset");
            currentGroupPrinted = false;
        }

        public void doAttributeGroupPre(String groupName) throws Exception {
            currentGroupName = groupName;
            currentGroupPrinted = false;
        }

        private void handleFocus(String idString) {
            if (focusOnFirst) {
                focusOnFirst = false;
                Util.addFocusOnElementScript(xb, idString);
            }
        }

        public void doIntegerAttribute(MDIntegerAttribute attribute, Integer value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!currentGroupPrinted) {
                openAttributeGroup(currentGroupName, null);
            }
            xb.openElement("tr");
            renderAttributeTh(attribute);
            xb.openElement("td");
            String strValue = stringValues.get(attribute);
            String paramName = "attr_" + attribute.getName();
            String attrIdStr = "attr" + attribute.getId();
            String readonlyStr = req.getParameter("readonly_" + paramName);
            boolean isWritable = writableMap.get(attribute) && (readonlyStr == null);
            if (isWritable) {
                xb.openElement("input");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("type", "text");
                xb.addAttribute("name", paramName);
                xb.addAttribute("value", strValue);
                xb.addAttribute("class", "text");
                String errorMsg = Util.getLocalizedString(Defs.MDLIBI18N, locale, "Invalid_value_An_integer_is_expected");
                String onBlurScript = "ctrTypeInteger(this,'" + Escape.javascriptEscape(errorMsg) + "');";
                xb.addAttribute("onblur", onBlurScript);
                xb.addAttribute("onfocus", "backgroundDelete(this);");
                xb.closeElement("input");
                renderAttributeDescription(attribute);
                handleFocus(attrIdStr);
            } else {
                xb.openElement("div");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("class", "value");
                xb.write(strValue);
                xb.writeInputHidden(paramName, strValue);
                xb.writeInputHidden("readonly_" + paramName, "");
                xb.closeElement("div");
            }
            xb.closeElement("td");
            xb.closeElement("tr");
        }

        private void renderAttributeTh(final MDAttribute attribute) {
            xb.openElement("th");
            xb.openElement("label");
            xb.addAttribute("for", "attr" + attribute.getId());
            if (attribute.isRequired() && !(attribute instanceof MDBooleanAttribute)) {
                required = true;
                xb.openElement("span");
                xb.addAttribute("class", "required");
                xb.write("*");
                xb.closeElement("span");
                xb.writeNoHtmlEscape("&nbsp;");
            }
            xb.write(Util.firstLetterToUpper(attribute.getPrettyName() + ":"));
            xb.closeElement("label");
            xb.closeElement("th");
        }

        public void doTextAttribute(MDTextAttribute attribute, String value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!currentGroupPrinted) {
                openAttributeGroup(currentGroupName, null);
            }
            xb.openElement("tr");
            renderAttributeTh(attribute);
            xb.openElement("td");
            String localValue = (String) localValues.get(attribute);
            String paramName = "attr_" + attribute.getName();
            String attrIdStr = "attr" + attribute.getId();
            String readonlyStr = req.getParameter("readonly_" + paramName);
            boolean isWritable = writableMap.get(attribute) && (readonlyStr == null);
            if (isWritable) {
                if (attribute.isMultiline()) {
                    String funzione = "verifyLine(this);";
                    xb.openElement("textarea");
                    xb.addAttribute("id", attrIdStr);
                    xb.addAttribute("name", paramName);
                    xb.addAttribute("cols", "" + Defs.TEXTAREA_NUM_COLS);
                    xb.addAttribute("rows", "" + numRowTextArea(localValue, Defs.TEXTAREA_NUM_COLS));
                    xb.addAttribute("onkeyup", funzione);
                    if (localValue != null) xb.write(localValue);
                    xb.closeElement("textarea");
                } else {
                    xb.openElement("input");
                    xb.addAttribute("id", attrIdStr);
                    xb.addAttribute("type", attribute.isPassword() ? "password" : "text");
                    xb.addAttribute("name", paramName);
                    xb.addAttribute("value", (localValue == null) ? "" : localValue);
                    xb.addAttribute("class", "text");
                    xb.addAttribute("size", "" + (attribute.getLength() > Defs.TEXTAREA_NUM_COLS ? Defs.TEXTAREA_NUM_COLS : attribute.getLength()));
                    xb.addAttribute("maxlength", "" + attribute.getLength());
                    xb.closeElement("input");
                }
                renderAttributeDescription(attribute);
                handleFocus(attrIdStr);
            } else {
                String formattedValue = attribute.formatValue(localValue);
                xb.openElement("div");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("class", "value");
                xb.write(formattedValue);
                xb.writeInputHidden(paramName, formattedValue);
                xb.writeInputHidden("readonly_" + paramName, "");
                xb.closeElement("div");
            }
            xb.closeElement("td");
            xb.closeElement("tr");
        }

        private void renderAttributeDescription(final MDAttribute attribute) {
            if (attribute.getDescription() != null) {
                xb.openElement("div");
                xb.addAttribute("class", "inputdescription");
                xb.writeNoHtmlEscape("&nbsp;");
                xb.write(attribute.getDescription());
                xb.closeElement("div");
            }
        }

        public void doBooleanAttribute(MDBooleanAttribute attribute, Boolean value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!currentGroupPrinted) {
                openAttributeGroup(currentGroupName, null);
            }
            xb.openElement("tr");
            renderAttributeTh(attribute);
            xb.openElement("td");
            Boolean localValue = (Boolean) localValues.get(attribute);
            String paramName = "attr_" + attribute.getName();
            String attrIdStr = "attr" + attribute.getId();
            String readonlyStr = req.getParameter("readonly_" + paramName);
            boolean isWritable = writableMap.get(attribute) && (readonlyStr == null);
            if (isWritable) {
                xb.openElement("input");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("type", "checkbox");
                xb.addAttribute("class", "checkbox");
                xb.addAttribute("name", paramName);
                if (Boolean.TRUE.equals(localValue)) xb.addAttribute("checked", "checked");
                xb.closeElement("input");
                xb.openElement("input");
                xb.addAttribute("type", "hidden");
                xb.addAttribute("name", paramName + "_ctrlcheckbox");
                xb.addAttribute("value", "true");
                xb.closeElement("input");
                renderAttributeDescription(attribute);
                handleFocus(attrIdStr);
            } else {
                xb.openElement("div");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("class", "value");
                xb.write(attribute.formatValue(localValue));
                if (Boolean.TRUE.equals(localValue)) {
                    xb.writeInputHidden(paramName, "");
                }
                xb.writeInputHidden(paramName + "_ctrlcheckbox", "");
                xb.writeInputHidden("readonly_" + paramName, "");
                xb.closeElement("div");
            }
            xb.closeElement("td");
            xb.closeElement("tr");
        }

        public void doDateAttribute(MDDateAttribute attribute, Date value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!currentGroupPrinted) {
                openAttributeGroup(currentGroupName, null);
            }
            xb.openElement("tr");
            renderAttributeTh(attribute);
            xb.openElement("td");
            String strValue = stringValues.get(attribute);
            String paramName = "attr_" + attribute.getName();
            String attrIdStr = "attr" + attribute.getId();
            String readonlyStr = req.getParameter("readonly_" + paramName);
            boolean isWritable = writableMap.get(attribute) && (readonlyStr == null);
            if (isWritable) {
                xb.openElement("input");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("type", "text");
                xb.addAttribute("name", paramName);
                xb.addAttribute("value", strValue);
                xb.addAttribute("class", "text");
                String errorMsg = Util.getLocalizedString(Defs.MDLIBI18N, locale, "Invalid_value_A_date_is_expected");
                String localizedPattern = attribute.getLocalizedPattern();
                String onBlurScript = "ctrTypeDate(this, '" + localizedPattern + "','" + Escape.javascriptEscape(errorMsg) + "');";
                xb.addAttribute("onblur", onBlurScript);
                xb.addAttribute("onfocus", "backgroundDelete(this);");
                xb.closeElement("input");
                xb.write(" (" + localizedPattern + ") ");
                String calImgLink = Util.getAbsoluteLink(req, "/jscalendar/img.gif");
                xb.writeImage(calImgLink, "calendar", "calendar", "cal" + attrIdStr, "calendar");
                String dateFormat = localizedPattern.toUpperCase();
                dateFormat = dateFormat.replaceAll("DD", "%d");
                dateFormat = dateFormat.replaceAll("YYYY", "%Y");
                dateFormat = dateFormat.replaceAll("MM", "%m");
                xb.writeNoHtmlEscape("<script type=\"text/javascript\"> " + "Calendar.setup({ " + "inputField     :    \"" + attrIdStr + "\"," + "ifFormat       :    \"" + dateFormat + "\"," + "button         :    \"cal" + attrIdStr + "\"" + "}); </script>");
                renderAttributeDescription(attribute);
                handleFocus(attrIdStr);
            } else {
                xb.openElement("div");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("class", "value");
                xb.write(strValue);
                xb.writeInputHidden(paramName, strValue);
                xb.writeInputHidden("readonly_" + paramName, "");
                xb.closeElement("div");
            }
            xb.closeElement("td");
            xb.closeElement("tr");
        }

        public void doDecimalAttribute(MDDecimalAttribute attribute, BigDecimal value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!currentGroupPrinted) {
                openAttributeGroup(currentGroupName, null);
            }
            xb.openElement("tr");
            renderAttributeTh(attribute);
            xb.openElement("td");
            String strValue = stringValues.get(attribute);
            String paramName = "attr_" + attribute.getName();
            String attrIdStr = "attr" + attribute.getId();
            String readonlyStr = req.getParameter("readonly_" + paramName);
            boolean isWritable = writableMap.get(attribute) && (readonlyStr == null);
            if (isWritable) {
                xb.openElement("input");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("type", "text");
                xb.addAttribute("name", paramName);
                xb.addAttribute("value", strValue);
                xb.addAttribute("class", "text");
                String errorMsg = Util.getLocalizedString(Defs.MDLIBI18N, locale, "Invalid_value_A_decimal_is_expected");
                String onBlurScript = "ctrTypeDecimal(this,'" + Escape.javascriptEscape(errorMsg) + "');";
                xb.addAttribute("onblur", onBlurScript);
                xb.addAttribute("onfocus", "backgroundDelete(this);");
                xb.closeElement("input");
                renderAttributeDescription(attribute);
                handleFocus(attrIdStr);
            } else {
                xb.openElement("div");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("class", "value");
                xb.write(strValue);
                xb.writeInputHidden(paramName, strValue);
                xb.writeInputHidden("readonly_" + paramName, "");
                xb.closeElement("div");
            }
            xb.closeElement("td");
            xb.closeElement("tr");
        }

        public void doWfAttribute(MDWfAttribute attribute, MDWfState value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!currentGroupPrinted) {
                openAttributeGroup(currentGroupName, null);
            }
            xb.openElement("tr");
            renderAttributeTh(attribute);
            xb.openElement("td");
            MDWfState localValue = (MDWfState) localValues.get(attribute);
            String paramName = "attr_" + attribute.getName();
            String attrIdStr = "attr" + attribute.getId();
            String readonlyStr = req.getParameter("readonly_" + paramName);
            boolean isWritable = writableMap.get(attribute) && (readonlyStr == null);
            if (isWritable) {
                xb.openElement("select");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("name", paramName);
                if (!attribute.isRequired()) {
                    boolean selected = (value == null);
                    xb.writeOption("", selected, Util.getLocalizedString(Defs.MDLIBI18N, locale, "Undefined_state"));
                }
                for (MDWfState state : attribute.getWfStates()) {
                    boolean selected = (value == state);
                    xb.writeOption("" + state.getId(), selected, state.getName());
                }
                xb.closeElement("select");
                renderAttributeDescription(attribute);
                handleFocus(attrIdStr);
            } else {
                xb.openElement("div");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("class", "value");
                xb.write(attribute.formatValue(localValue));
                xb.writeInputHidden(paramName, (localValue == null) ? "" : "" + localValue.getId());
                xb.writeInputHidden("readonly_" + paramName, "");
                xb.closeElement("div");
            }
            xb.closeElement("td");
            xb.closeElement("tr");
        }

        public void doRelAttribute(MDRelAttribute attribute, Integer value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!currentGroupPrinted) {
                openAttributeGroup(currentGroupName, null);
            }
            xb.openElement("tr");
            renderAttributeTh(attribute);
            xb.openElement("td");
            Integer localValue = (Integer) localValues.get(attribute);
            String paramName = "attr_" + attribute.getName();
            String attrIdStr = "attr" + attribute.getId();
            String readonlyStr = req.getParameter("readonly_" + paramName);
            boolean isWritable = writableMap.get(attribute) && (readonlyStr == null);
            if (isWritable) {
                Breadcrumbs.doContext(req, xb, cls, attribute, "attr", obj, true, false);
                xb.writeInputHidden("chk_" + paramName, "");
                renderAttributeDescription(attribute);
                handleFocus(attrIdStr);
            } else {
                xb.openElement("div");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("class", "value");
                if (localValue == null) {
                    xb.writeInputHidden(paramName, "");
                } else {
                    MDObject localObject = attribute.getOppositeEndCls().getMDObject(localValue);
                    xb.write(Util.firstLetterToUpper(localObject.getName()));
                    xb.writeInputHidden(paramName, localValue.toString());
                }
                xb.writeInputHidden("readonly_" + paramName, "");
                xb.writeInputHidden("chk_" + paramName, "");
                xb.closeElement("div");
            }
            xb.closeElement("td");
            xb.closeElement("tr");
        }

        public void doBlobAttribute(MDBlobAttribute attribute, MDBlob value, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!currentGroupPrinted) {
                openAttributeGroup(currentGroupName, null);
            }
            xb.openElement("tr");
            renderAttributeTh(attribute);
            xb.openElement("td");
            MDBlob localValue = (MDBlob) localValues.get(attribute);
            String paramName = "attr_" + attribute.getName();
            String attrIdStr = "attr" + attribute.getId();
            String readonlyStr = req.getParameter("readonly_" + paramName);
            boolean isWritable = writableMap.get(attribute) && (readonlyStr == null);
            if (isWritable) {
                printBlobUpdate(attribute, localValue, paramName);
                renderAttributeDescription(attribute);
                handleFocus(attrIdStr);
            } else {
                xb.openElement("div");
                xb.addAttribute("id", attrIdStr);
                xb.addAttribute("class", "value");
                printBlob(attribute, localValue);
            }
            xb.closeElement("td");
            xb.closeElement("tr");
        }

        private void printBlobUpdate(MDBlobAttribute attribute, MDBlob localValue, String paramName) {
            String attrIdStr = "attr" + attribute.getId();
            if (localValue != null && localValue.getName() != null && !localValue.getName().equals("")) {
                xb.openElement("div");
                xb.addAttribute("class", "value");
                xb.openElement("div");
                printBlob(attribute, localValue);
                xb.closeElement("div");
                String attrBlobName = "attrblob_" + attribute.getName();
                xb.openElement("div");
                String keepIdStr = "attrblob" + attribute.getId() + "_keep";
                String script = "var inptxt = document.getElementById('" + Escape.javascriptEscape(attrIdStr) + "');" + "inptxt.disabled=true;inptxt.value='';";
                xb.writeInputRadio(keepIdStr, attrBlobName, BLOB_KEEP, true, false, script);
                xb.writeLabel(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Keep"), keepIdStr, null);
                xb.closeElement("div");
                xb.openElement("div");
                String modifyIdStr = "attrblob" + attribute.getId() + "_modify";
                script = "var inptxt = document.getElementById('" + Escape.javascriptEscape(attrIdStr) + "');" + "inptxt.disabled=false;inptxt.value='';";
                xb.writeInputRadio(modifyIdStr, attrBlobName, BLOB_MODIFY, false, false, script);
                xb.writeLabel(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Update"), modifyIdStr, null);
                xb.closeElement("div");
                xb.openElement("div");
                String deleteIdStr = "attrblob" + attribute.getId() + "_delete";
                script = "var inptxt = document.getElementById('" + Escape.javascriptEscape(attrIdStr) + "');" + "inptxt.disabled=true;inptxt.value='';";
                xb.writeInputRadio(deleteIdStr, attrBlobName, BLOB_DELETE, false, false, script);
                xb.writeLabel(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Delete"), deleteIdStr, null);
                xb.closeElement("div");
                xb.writeInputFile(attrIdStr, paramName, true);
                xb.closeElement("div");
            } else {
                xb.writeInputHidden("attrblob_" + attribute.getName(), BLOB_MODIFY);
                xb.writeInputFile(attrIdStr, paramName, false);
            }
        }

        private void printBlob(MDBlobAttribute attribute, MDBlob localValue) {
            if (attribute.isPreview()) {
                try {
                    xb.writeImage("ReadBlob?class=" + Escape.urlencode(cls.getName()) + "&id=" + obj.getId() + "&blobid=" + localValue.getId() + "&blobname=" + Escape.urlencode(attribute.getName()) + "&thumbnail=", attribute.getPrettyName(), attribute.getPrettyName(), null, null);
                } catch (Exception e) {
                }
            } else {
                if (localValue != null && localValue.getName() != null) xb.write(localValue.getName());
            }
        }

        public void doAttributeGroupPost() throws Exception {
            if (currentGroupPrinted) {
                closeAttributeGroup();
            }
        }

        public void doAttributeListPost() throws Exception {
            if (required) {
                xb.openElement("table");
                xb.addAttribute("class", "details");
                xb.openElement("tr");
                xb.openElement("th");
                xb.writeNoHtmlEscape("&nbsp;");
                xb.closeElement("th");
                xb.openElement("td");
                xb.openElement("div");
                xb.addAttribute("class", "value");
                xb.openElement("em");
                xb.write(Util.getLocalizedString(Defs.MDLIBI18N, cls.getConfig().getLocale(), "The_fields_marked_are_required"));
                xb.closeElement("em");
                xb.closeElement("div");
                xb.closeElement("td");
                xb.closeElement("tr");
                xb.closeElement("table");
            }
            xb.openElement("div");
            xb.addAttribute("class", "actions");
            String buttonName = create ? "create" : "update";
            String buttonValue = Util.getLocalizedString(Defs.MDLIBI18N, locale, (create ? "Create" : "Save"));
            String errorMsg = Util.getLocalizedString(Defs.MDLIBI18N, locale, "Correct_the_errors_before_proceeding");
            xb.writeInputSubmit(buttonName, buttonValue, "return valida(this.form,'" + Escape.javascriptEscape(errorMsg) + "')");
        }

        public void doWfTransitionListPre() throws Exception {
        }

        public void doWfTransition(MDWfTransition wft, boolean canDo, boolean enabled) throws Exception {
            String wftButtonName = "wft" + wft.getId();
            String wftButtonValue = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, create ? "Create_and_" : "Save_and_"), wft.getAction());
            String errorMsg = Util.getLocalizedString(Defs.MDLIBI18N, locale, "Correct_the_errors_before_proceeding");
            xb.writeNoHtmlEscape("&nbsp;");
            xb.writeInputSubmit(wftButtonName, wftButtonValue, "return valida(this.form,'" + Escape.javascriptEscape(errorMsg) + "')");
        }

        public void doWfTransitionListPost() throws Exception {
        }

        public void doObjectPost() throws Exception {
            xb.writeNoHtmlEscape("&nbsp;");
            String cancelButtonValue = Util.getLocalizedString(Defs.MDLIBI18N, locale, "Cancel");
            xb.writeInputSubmit("cancel", cancelButtonValue, null);
            xb.writeInputHidden("class", cls.getName());
            if (create) {
                xb.writeInputHidden("actual", "");
            } else {
                xb.writeInputHidden("id", "" + obj.getId());
            }
            if (successReturnUrl != null) {
                xb.writeInputHidden(Defs.SUCCESS_RETURN_URL_PARAMETER, successReturnUrl);
            }
            if (cancelReturnUrl != null) {
                xb.writeInputHidden(Defs.CANCEL_RETURN_URL_PARAMETER, cancelReturnUrl);
            }
            xb.closeElement("div");
            xb.closeFormElement();
            xb.closeElement("div");
            xb.closeElement("div");
            Navigation navigation = config.getNavigation();
            navigation.writePageFooter(xb);
        }
    }

    /**
     * *************************************************
     * WriteBackVisitor                 *
     * **************************************************
     */
    private class WriteBackVisitor implements MDClassAttributeVisitor {

        public void doClassPre(MDClass cls) throws Exception {
        }

        public void doAttributeListPre() throws Exception {
        }

        public void doAttributeGroupPre(String groupName) throws Exception {
        }

        public void doIntegerAttribute(MDIntegerAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            Integer localValue = (Integer) localValues.get(attribute);
            obj.setIntegerAttribute(attribute, localValue);
        }

        public void doTextAttribute(MDTextAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            String localValue = (String) localValues.get(attribute);
            obj.setTextAttribute(attribute, localValue);
        }

        public void doBooleanAttribute(MDBooleanAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            Boolean localValue = (Boolean) localValues.get(attribute);
            obj.setBooleanAttribute(attribute, localValue);
        }

        public void doDateAttribute(MDDateAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            Date localValue = (Date) localValues.get(attribute);
            obj.setDateAttribute(attribute, localValue);
        }

        public void doDecimalAttribute(MDDecimalAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            BigDecimal localValue = (BigDecimal) localValues.get(attribute);
            obj.setDecimalAttribute(attribute, localValue);
        }

        public void doWfAttribute(MDWfAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            MDWfState localValue = (MDWfState) localValues.get(attribute);
            obj.setWfAttribute(attribute, localValue);
        }

        public void doRelAttribute(MDRelAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            Integer localValue = (Integer) localValues.get(attribute);
            obj.setRelAttribute(attribute, localValue);
        }

        public void doBlobAttribute(MDBlobAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            MDBlob localValue = (MDBlob) localValues.get(attribute);
            obj.setBlobAttribute(attribute, localValue);
        }

        public void doAttributeGroupPost() throws Exception {
        }

        public void doAttributeListPost() throws Exception {
        }

        public void doClassPost() throws Exception {
        }
    }

    private static int numRowTextArea(String stringValue, int cols) {
        if (stringValue == null) return Defs.TEXTAREA_NUM_MINROWS;
        String dim[] = stringValue.split("\n");
        int rows = 0;
        for (String aDim : dim) {
            if (aDim.length() >= cols) rows += aDim.length() / cols;
        }
        rows += dim.length;
        if (rows < Defs.TEXTAREA_NUM_MINROWS) rows = Defs.TEXTAREA_NUM_MINROWS;
        return rows;
    }
}
