package org.sss.common.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sss.common.model.Event;
import org.sss.common.model.EventType;
import org.sss.common.model.IBaseObject;
import org.sss.common.model.IModule;
import org.sss.common.model.IDatafield;
import org.sss.common.model.IModuleList;
import org.sss.common.model.IModuleSession;
import org.sss.common.model.IParent;
import org.sss.common.model.IPresentation;
import org.sss.common.model.RowOpType;

/**
 * IModelSession示例现实
 * @author Jason.Hoo (latest modification by $Author: hujianxin78728 $)
 * @version $Revision: 377 $ $Date: 2009-03-14 05:31:31 -0400 (Sat, 14 Mar 2009) $
 */
public class ModuleSessionImpl implements IModuleSession {

    static final Log log = LogFactory.getLog(ModuleSessionImpl.class);

    private IPresentation presentation;

    private IModule entity;

    private String name = null;

    public ModuleSessionImpl(IPresentation presentation, IModule entity) {
        log.info("Create instance of " + this.getClass().getName());
        this.entity = entity;
        this.entity.setAttribute(IDatafield.URL, "");
        entity.addChild();
        this.presentation = presentation;
    }

    public void chain(String name) {
        this.name = name;
        log.info("Chain to transaction : " + name);
    }

    public void close() {
        log.info("close");
    }

    public void select(Event event) {
        if (this.name == null) log.warn("transaction is null...");
        try {
            Thread thread = new ModuleSessionThread(presentation, event);
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            log.info(e);
        }
    }

    public IBaseObject getBaseObject(String fieldUrl) {
        if (this.name == null) log.warn("transaction is null...");
        log.info("getField----->" + fieldUrl);
        if ("locale".equals(fieldUrl)) return new FieldImpl(Locale.SIMPLIFIED_CHINESE);
        int position = fieldUrl.indexOf(IDatafield.URL_DELIMITER);
        if (position >= 0) return getField(entity, fieldUrl.substring(position + 1));
        return entity;
    }

    public IBaseObject setBaseObject(String fieldUrl, IBaseObject field) {
        if (this.name == null) log.warn("transaction is null...");
        log.info("setField----->" + fieldUrl);
        int position = fieldUrl.lastIndexOf(IDatafield.URL_DELIMITER);
        IBaseObject parent = this.getBaseObject(fieldUrl.substring(0, position));
        String subUrl = fieldUrl.substring(position + 1);
        if (parent instanceof IModuleList) return ((IModuleList<IModule>) parent).set(Integer.parseInt(subUrl), (IModule) field); else return ((IModule) parent).put(subUrl, (IDatafield) field);
    }

    public String getResourcePath() {
        return null;
    }

    public String getThemes() {
        return "tundra";
    }

    private IBaseObject getField(IBaseObject field, String subUrl) {
        int position = subUrl.indexOf(IDatafield.URL_DELIMITER);
        if (position >= 0) return getField(getField(field, subUrl.substring(0, position)), subUrl.substring(position + 1)); else {
            if (field instanceof IModuleList) return ((IModuleList<IModule>) field).get(Integer.parseInt(subUrl)); else return ((IModule) field).get(subUrl);
        }
    }
}

class ModuleSessionThread extends Thread {

    static final Log log = LogFactory.getLog(ModuleSessionThread.class);

    private IPresentation presentation;

    private Event event;

    public ModuleSessionThread(IPresentation presentation, Event event) {
        this.presentation = presentation;
        this.event = event;
    }

    @Override
    public void run() {
        EventType type = event.getEventType();
        IBaseObject field = event.getField();
        log.info("select----->" + field.getAttribute(IDatafield.URL));
        try {
            IParent parent = field.getParent();
            String name = field.getName();
            if (type == EventType.ON_BTN_CLICK) {
                invoke(parent, "eventRule_" + name);
                Object value = event.get(Event.ARG_VALUE);
                if (value != null && value instanceof String[]) {
                    String[] args = (String[]) value;
                    for (String arg : args) log.info("argument data: " + arg);
                }
            } else if (type == EventType.ON_BEFORE_DOWNLOAD) invoke(parent, "eventRule_" + name); else if (type == EventType.ON_VALUE_CHANGE) {
                String pattern = (String) event.get(Event.ARG_PATTERN);
                Object value = event.get(Event.ARG_VALUE);
                Class cls = field.getDataType();
                if (cls == null || value == null || value.getClass().equals(cls)) field.setValue(value); else if (pattern == null) field.setValue(ConvertUtils.convert(value.toString(), cls)); else if (Date.class.isAssignableFrom(cls)) field.setValue(new SimpleDateFormat(pattern).parse((String) value)); else if (Number.class.isAssignableFrom(cls)) field.setValue(new DecimalFormat(pattern).parse((String) value)); else field.setValue(new MessageFormat(pattern).parse((String) value));
                invoke(parent, "checkRule_" + name);
                invoke(parent, "defaultRule_" + name);
            } else if (type == EventType.ON_ROW_SELECTED) {
                log.info("table row selected.");
                Object selected = event.get(Event.ARG_ROW_INDEX);
                if (selected instanceof Integer) presentation.setSelectedRowIndex((IModuleList) field, (Integer) selected); else if (selected instanceof List) {
                    String s = "";
                    String conn = "";
                    for (Integer item : (List<Integer>) selected) {
                        s = s + conn + item;
                        conn = ",";
                    }
                    log.info("row " + s + " line(s) been selected.");
                }
            } else if (type == EventType.ON_ROW_DBLCLICK) {
                log.info("table row double-clicked.");
                presentation.setSelectedRowIndex((IModuleList) field, (Integer) event.get(Event.ARG_ROW_INDEX));
            } else if (type == EventType.ON_ROW_INSERT) {
                log.info("table row inserted.");
                listAdd((IModuleList) field, (Integer) event.get(Event.ARG_ROW_INDEX));
            } else if (type == EventType.ON_ROW_REMOVE) {
                log.info("table row removed.");
                listRemove((IModuleList) field, (Integer) event.get(Event.ARG_ROW_INDEX));
            } else if (type == EventType.ON_FILE_UPLOAD) {
                log.info("file uploaded.");
                InputStream is = (InputStream) event.get(Event.ARG_VALUE);
                String uploadFileName = (String) event.get(Event.ARG_FILE_NAME);
                log.info("<-----file name:" + uploadFileName);
                OutputStream os = (OutputStream) field.getValue();
                IOUtils.copy(is, os);
                is.close();
                os.close();
            }
        } catch (Exception e) {
            if (field != null) log.info("field type is :" + field.getDataType().getName());
            log.info("select", e);
        }
    }

    private void invoke(IBaseObject object, String name) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method method = MethodUtils.getAccessibleMethod(object.getClass(), name, new Class[] {});
        if (method != null) method.invoke(object);
    }

    private void listRemove(IModuleList list, int index) {
        list.remove(index);
        refreshFieldUrl(list, index);
        presentation.listRowChange(list, RowOpType.OP_REMOVE, index, 1);
    }

    private void listAdd(IModuleList list, int index) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class entityClass = ((EntityListImpl) list).getEntityClass();
        list.add(index, (IModule) ConstructorUtils.invokeConstructor(entityClass, new Object[] { list }));
        refreshFieldUrl(list, index);
        presentation.listRowChange(list, RowOpType.OP_INSERT, index, 1);
    }

    private void refreshFieldUrl(IModuleList<IModule> list, int index) {
        String url = (String) list.getAttribute(IDatafield.URL);
        for (int i = index; i < list.size(); i++) refreshFieldUrl(list.get(i), url + IDatafield.URL_DELIMITER + i);
    }

    private void refreshFieldUrl(IBaseObject field, String url) {
        field.setAttribute(IDatafield.URL, url);
        if (field instanceof IModuleList) {
            IModuleList<IModule> list = (IModuleList) field;
            for (int i = 0; i < list.size(); i++) refreshFieldUrl(list.get(i), url + IDatafield.URL_DELIMITER + i);
        } else if (field instanceof IModule) {
            IModule entity = (IModule) field;
            for (String key : entity.keySet()) refreshFieldUrl(entity.get(key), url + IDatafield.URL_DELIMITER + key);
        }
    }
}
