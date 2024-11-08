package lu.etat.pch.icewebadf.jsf;

import com.esri.adf.web.data.tasks.TaskParamDescriptor;

/**
 * Created by IntelliJ IDEA.
 * User: schullto
 * Date: 29 avr. 2008
 * Time: 06:26:32
 */
public class ICETaskParamDescriptor extends TaskParamDescriptor {

    private boolean doHighlight = false;

    public static final String LINK_RENDERER_TYPE = "OUTPUT_LINK";

    private boolean immediate;

    public ICETaskParamDescriptor(java.lang.Class taskClass, java.lang.String paramName, java.lang.String displayName) {
        super(taskClass, paramName, displayName);
    }

    public ICETaskParamDescriptor(java.lang.Class taskClass, java.lang.String paramName, java.lang.String displayName, java.lang.String selectMethodName) {
        super(taskClass, paramName, displayName, selectMethodName);
    }

    public ICETaskParamDescriptor(java.lang.Class taskClass, java.lang.String paramName, java.lang.String displayName, java.lang.String selectMethodName, boolean radioRendererType) {
        super(taskClass, paramName, displayName, selectMethodName, radioRendererType);
    }

    public ICETaskParamDescriptor(java.lang.Class taskClass, java.lang.String paramName, java.lang.String displayName, java.lang.String readMethodName, java.lang.String writeMethodName) {
        super(taskClass, paramName, displayName, readMethodName, writeMethodName);
    }

    public ICETaskParamDescriptor(java.lang.Class taskClass, java.lang.String paramName, java.lang.String displayName, java.lang.String readMethodName, java.lang.String writeMethodName, java.lang.String selectMethodName) {
        super(taskClass, paramName, displayName, readMethodName, writeMethodName, selectMethodName);
    }

    public ICETaskParamDescriptor(java.lang.Class taskClass, java.lang.String paramName, java.lang.String displayName, java.lang.String readMethodName, java.lang.String writeMethodName, java.lang.String selectMethodName, boolean radioRendererType) {
        super(taskClass, paramName, displayName, readMethodName, writeMethodName, selectMethodName, radioRendererType);
    }

    public ICETaskParamDescriptor(java.lang.String paramName, java.lang.Class paramClass, java.lang.reflect.Method readMethod, java.lang.reflect.Method writeMethod, java.lang.reflect.Method selectMethod) {
        super(paramName, paramClass, readMethod, writeMethod, selectMethod);
    }

    public boolean isDoHighlight() {
        return doHighlight;
    }

    public void setDoHighlight(boolean doHighlight) {
        this.doHighlight = doHighlight;
    }

    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }

    public boolean isImmediate() {
        return immediate;
    }
}
