package org.xaware.server.engine.channel.jms;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import javax.jms.ConnectionFactory;
import org.jdom.Element;
import org.jdom.Namespace;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IChannelSpecification;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractConnectionChannel;
import org.xaware.server.engine.channel.sql.JdbcChannelSpecification;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.server.engine.instruction.bizcomps.BaseBizComponentInst;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * @author tferguson
 *
 */
public abstract class AbstractJmsXawareChannelSpecification extends AbstractConnectionChannel implements IChannelSpecification {

    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(JdbcChannelSpecification.class.getName());

    private Element m_connectionElement;

    @Override
    public void transformSpecInfo(final IBizViewContext p_bizViewContext) throws XAwareConfigMissingException, XAwareSubstitutionException, XAwareException {
        final Namespace xaNamespace = XAwareConstants.xaNamespace;
        m_connectionElement = m_bizDriverRootElement.getChild(XAwareConstants.BIZDRIVER_CONNECTION, xaNamespace);
        parseConnectionDefinition(m_connectionElement, p_bizViewContext, getBizDriverIdentifier() + ":AbstractJmsXawareChannelSpecification", lf, true, false);
    }

    public Object getChannelObject() throws XAwareException {
        try {
            final Class homeClass = Class.forName(this.getFactoryClassName(), true, BaseBizComponentInst.getClassLoader());
            Object obj = homeClass.newInstance();
            Method[] setterMethods = getSetterMethods(obj.getClass().getMethods());
            Properties connProps = (Properties) this.m_props.get(PN_CONNECTION_PROPERTIES);
            Set<Entry<Object, Object>> properties = connProps.entrySet();
            for (Entry<Object, Object> entry : properties) {
                String property = (String) entry.getKey();
                String value = (String) entry.getValue();
                Method method = getSetMethod(setterMethods, property);
                if (method != null) {
                    Class[] parameterClasses = method.getParameterTypes();
                    Object[] args = new Object[1];
                    if (parameterClasses[0].equals(Boolean.class) || parameterClasses[0].equals(boolean.class)) {
                        args[0] = new Boolean(value);
                    } else if (parameterClasses[0].equals(Byte.class) || parameterClasses[0].equals(byte.class)) {
                        args[0] = new Byte(value);
                    } else if (parameterClasses[0].equals(Character.class) || parameterClasses[0].equals(char.class)) {
                        args[0] = new Character(value.charAt(0));
                    } else if (parameterClasses[0].equals(Double.class) || parameterClasses[0].equals(double.class)) {
                        args[0] = new Double(value);
                    } else if (parameterClasses[0].equals(Float.class) || parameterClasses[0].equals(float.class)) {
                        args[0] = new Float(value);
                    } else if (parameterClasses[0].equals(Integer.class) || parameterClasses[0].equals(int.class)) {
                        args[0] = new Integer(value);
                    } else if (parameterClasses[0].equals(Long.class) || parameterClasses[0].equals(long.class)) {
                        args[0] = new Long(value);
                    } else if (parameterClasses[0].equals(Short.class) || parameterClasses[0].equals(short.class)) {
                        args[0] = new Short(value);
                    } else if (parameterClasses[0].equals(String.class)) {
                        args[0] = value;
                    } else {
                        throw new XAwareConfigurationException("Unsupported method parameter type for the jms factory properties");
                    }
                    method.invoke(obj, args);
                } else {
                    throw new XAwareConfigurationException("Unable to locate method to set " + property);
                }
            }
            return new JmsConnectionFactoryHolder((ConnectionFactory) obj, isJms102(), null, this.getUsername(), this.getPassword());
        } catch (Exception e) {
            lf.printStackTrace(e);
            throw new XAwareConfigurationException(e);
        }
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        throw new XAwareException("Unsupported method, use getChannelObject instead");
    }

    /**
     * Filters the given methods array for methods matching the setter method profile.
     * 
     * @param methods
     *            methods as array to be filtered for setter methods.
     * @return methods mathching the setter profile.
     */
    private Method[] getSetterMethods(Method[] methods) {
        ArrayList<Method> setterMethods = new ArrayList<Method>();
        for (Method method : methods) {
            if (method.getName().startsWith("set") && (method.getReturnType().equals(Void.class) || method.getReturnType().equals(Void.TYPE)) && method.getParameterTypes().length == 1) {
                setterMethods.add(method);
            }
        }
        return setterMethods.toArray(new Method[setterMethods.size()]);
    }

    private Method getSetMethod(Method[] setterMethods, String property) {
        String methodName = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
        for (int i = 0; i < setterMethods.length; i++) {
            String thisMethod = setterMethods[i].getName();
            if (setterMethods[i].getName().equals(methodName)) {
                return setterMethods[i];
            }
        }
        return null;
    }

    public abstract boolean isJms102();
}
