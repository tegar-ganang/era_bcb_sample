package org.swemas.core.kernel;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.xpath.*;
import org.swemas.core.*;
import org.swemas.core.messaging.*;
import org.swemas.data.xml.IXmlChannel;
import org.swemas.data.xml.NamespaceContext;
import org.swemas.data.xml.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class SwKernel implements IKernel {

    public SwKernel(String appPath, String tempXmlChannel) throws KernelException {
        _appPath = appPath;
        _defaults = new ConcurrentHashMap<Class<?>, IChannel>();
        _modules = new ConcurrentHashMap<String, IChannel>();
        start(tempXmlChannel);
    }

    ;

    public SwKernel(String appPath, String tempXmlChannel, int concurrencyLevel) throws KernelException {
        _appPath = appPath;
        _defaults = new ConcurrentHashMap<Class<?>, IChannel>(16, (float) 0.4, concurrencyLevel);
        _modules = new ConcurrentHashMap<String, IChannel>(16, (float) 0.4, concurrencyLevel);
        start(tempXmlChannel);
    }

    public String name(Locale locale) {
        try {
            IMessagingChannel imesg = (IMessagingChannel) getChannel(IMessagingChannel.class);
            return imesg.getText("name", this.getClass().getName(), locale);
        } catch (ModuleNotFoundException e) {
            return this.getClass().getName();
        }
    }

    public String description(Locale locale) {
        try {
            IMessagingChannel imesg = (IMessagingChannel) getChannel(IMessagingChannel.class);
            return imesg.getText("description", this.getClass().getName(), locale);
        } catch (ModuleNotFoundException e) {
            return "SwKernel version 0.01";
        }
    }

    public String getPath(String relPath) {
        return _appPath + "/WEB-INF/" + relPath;
    }

    ;

    public IChannel getChannel(Class<?> cclass) throws ModuleNotFoundException {
        if (_defaults.containsKey(cclass)) return _defaults.get(cclass); else {
            IXmlChannel ixml = (IXmlChannel) _defaults.get(IXmlChannel.class);
            try {
                Document doc = ixml.open(getPath("/config/default_modules.xml"), getPath("/schema/map.xsd"));
                NamespaceContext ns = new NamespaceContext();
                ns.setNamespace("map", "swemas/map");
                NodeList cands = ixml.evaluate(doc, "/map:map/map:pair[map:key=\"" + cclass.getName() + "\"]/map:value/text()", ns);
                if (cands.getLength() > 0) {
                    String cn = cands.item(0).getNodeValue();
                    IChannel ic = getModule(cn);
                    _defaults.putIfAbsent(cclass, ic);
                    return _defaults.get(cclass);
                } else throw new ModuleNotFoundException(null, this.getClass().getCanonicalName(), cclass.getName(), ErrorCode.DefaultsOpenError.getCode());
            } catch (XmlException e) {
                throw new ModuleNotFoundException(e, this.getClass().getCanonicalName(), cclass.getName(), ErrorCode.DefaultsOpenError.getCode());
            } catch (XPathExpressionException e) {
                throw new ModuleNotFoundException(e, this.getClass().getCanonicalName(), cclass.getName(), ErrorCode.NoDefaultModuleDefinedError.getCode());
            }
        }
    }

    public IChannel getChannel(String cname) throws ModuleNotFoundException {
        try {
            return getChannel(Class.forName(cname));
        } catch (ClassNotFoundException e) {
            throw new ModuleNotFoundException(e, this.getClass().getCanonicalName(), cname, ErrorCode.ClassNotLoadedError.getCode());
        }
    }

    private IChannel getModule(String name) throws ModuleNotFoundException {
        if (_modules.containsKey(name)) return _defaults.get(name); else {
            try {
                Class<?> c = Class.forName(name);
                IChannel ch = (IChannel) c.getConstructor(IKernel.class).newInstance(this);
                _modules.putIfAbsent(name, ch);
                return _modules.get(name);
            } catch (ClassNotFoundException e) {
                throw new ModuleNotFoundException(e, this.getClass().getCanonicalName(), name, ErrorCode.ClassNotLoadedError.getCode());
            } catch (InstantiationException e) {
                throw new ModuleNotFoundException(e, this.getClass().getCanonicalName(), name, ErrorCode.NoStandardPublicConstructorFoundError.getCode());
            } catch (IllegalAccessException e) {
                throw new ModuleNotFoundException(e, this.getClass().getCanonicalName(), name, ErrorCode.NoStandardPublicConstructorFoundError.getCode());
            } catch (InvocationTargetException e) {
                throw new ModuleNotFoundException(e, this.getClass().getCanonicalName(), name, ErrorCode.ObjectConstructionError.getCode());
            } catch (NoSuchMethodException e) {
                throw new ModuleNotFoundException(e, this.getClass().getCanonicalName(), name, ErrorCode.NoStandardPublicConstructorFoundError.getCode());
            }
        }
    }

    private void start(String tempXmlChannel) throws KernelException {
        try {
            try {
                Class<?> txmlc = Class.forName(tempXmlChannel);
                IXmlChannel txmlch = (IXmlChannel) txmlc.getConstructor(IKernel.class).newInstance(this);
                Document doc = txmlch.open(getPath("/config/default_modules.xml"), getPath("/schema/map.xsd"));
                NamespaceContext ns = new NamespaceContext();
                ns.setNamespace("map", "swemas/map");
                String dxmlcn = txmlch.evaluate(doc, "/map:map/map:pair[map:key=\"" + IXmlChannel.class.getName() + "\"]/map:value/text()", ns).item(0).getNodeValue();
                try {
                    Class<?> dxmlc = Class.forName(dxmlcn);
                    IChannel dxmlch = (IChannel) dxmlc.getConstructor(IKernel.class).newInstance(this);
                    _defaults.put(IXmlChannel.class, dxmlch);
                    _modules.put(dxmlcn, dxmlch);
                } catch (ClassNotFoundException e) {
                    throw new KernelException("Kernel error: class " + dxmlcn + " for default XML channel cannot be found", e);
                }
            } catch (ClassNotFoundException e) {
                throw new KernelException("Kernel error: class " + tempXmlChannel + " for default XML channel cannot be found", e);
            }
        } catch (IllegalAccessException e) {
            throw new KernelException("Kernel error: no standard public constructor found for default XML channel implementation", e);
        } catch (InstantiationException e) {
            throw new KernelException("Kernel error: default XML implementation class is incorrect (seemes to be abstract)", e);
        } catch (InvocationTargetException e) {
            throw new KernelException("Kernel error: there was an exception during default XML implementation object construction", e);
        } catch (NoSuchMethodException e) {
            throw new KernelException("Kernel error: no standard public constructor found for default XML channel implementation", e);
        } catch (XmlException e) {
            throw new KernelException("Kernel error: unable to open default_modules.xml file with default modules definitions", e);
        } catch (XPathExpressionException e) {
            throw new KernelException("Kernel error: no default XML channel defined for use", e);
        }
    }

    private String _appPath;

    private ConcurrentMap<Class<?>, IChannel> _defaults;

    private ConcurrentMap<String, IChannel> _modules;
}
