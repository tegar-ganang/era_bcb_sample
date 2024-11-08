package com.rbnb.web;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InvalidAttributesException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.servlet.ServletContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import com.rbnb.api.Rmap;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.ChannelTree;
import com.rbnb.sapi.Sink;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class RBNBDirContext implements DirContext {

    private void checkEnv(InitialContext webEnv, String propertyEnv, String propertyLocal, Object _default) {
        try {
            Object result = webEnv.lookup(propertyEnv);
            defaultProperties.put(propertyLocal, result);
            return;
        } catch (NamingException ne) {
        }
        defaultProperties.put(propertyLocal, _default);
    }

    public RBNBDirContext() {
        defaultProperties.put("hostname", "localhost:3333");
        defaultProperties.put("user", "");
        defaultProperties.put("password", "");
        try {
            InitialContext webEnv = new InitialContext();
            checkEnv(webEnv, "java:comp/env/com.rbnb.web.defaultarchive", "archivesize", new Integer(10000));
            checkEnv(webEnv, "java:comp/env/com.rbnb.web.defaultarchivemode", "archivemode", "none");
            checkEnv(webEnv, "java:comp/env/com.rbnb.web.defaultcache", "cachesize", new Integer(100));
            checkEnv(webEnv, "java:comp/env/com.rbnb.web.isuserbrowser", "userbrowser", new Boolean(false));
            checkEnv(webEnv, "java:comp/env/com.rbnb.web.defaultuser", "defaultuser", "");
            checkEnv(webEnv, "java:comp/env/com.rbnb.web.sinkname", "sinkname", "rbnbDirContextSink");
            if ("true".equals(defaultProperties.get("userbrowser").toString())) {
                try {
                    Object user = defaultProperties.get("defaultuser");
                    if (user != null && user.toString().length() != 0) getSource(user.toString());
                    SourceNode sn = getSource("info");
                    synchronized (sn) {
                        sn.map.Clear();
                        int index = sn.map.Add("serverName");
                        sn.map.PutTimeAuto("timeofday");
                        sn.map.PutDataAsString(index, sn.source.GetServerName());
                        sn.map.PutMime(index, "text/plain");
                        sn.source.Flush(sn.map);
                    }
                } catch (SAPIException se) {
                    se.printStackTrace();
                }
            }
        } catch (NamingException ne) {
            ne.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        properties.putAll(defaultProperties);
    }

    public RBNBDirContext(Map props) {
        this();
        if (props != null) {
            properties.putAll(props);
            if ("true".equals(properties.get("debug"))) {
                debug = true;
                System.err.println("Debug is on!");
            }
            Object o = properties.get("userbrowser");
            if (o != null && "true".equals(o.toString())) userBrowser = true;
            servletContext = (ServletContext) properties.get("ServletContext");
        }
    }

    public RBNBDirContext(Map props, String initialDir) {
        this(props);
        currentDirectory = initialDir;
    }

    /**
	  * Binds a name to an object, along with associated attributes.
	  * <p>Calls {@link #bind(String, Object, Attributes)} </p>
	  * <p> @author WHF
	  * <p> @since V2.0
	  */
    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        bind(nameToString(name), obj, attrs);
    }

    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        name = calcAbsolutePath(name);
        if (debug) System.err.println("JNDI: bind(" + name + ")");
        if (!userBrowser) throw new InvalidAttributesException("An RBNB Source, \"" + name + "\", cannot be created from this webapplication.");
        if (hiddenSet.contains(name)) {
            if (debug) System.err.println("-- Unhiding " + name);
            hiddenSet.remove(name);
        }
        String contextName = null, sourceName = null;
        try {
            if (name.charAt(0) == Rmap.PATHDELIMITER) {
                String serverName = getServerName();
                if (!name.startsWith(serverName)) throw new InvalidAttributesException("An RBNB Source, \"" + name + "\", cannot be created from this entry point, \"" + serverName + "\".");
                contextName = name.substring(serverName.length() + 1);
                int firstSlash = contextName.indexOf(Rmap.PATHDELIMITER);
                if (firstSlash == -1) throw new InvalidAttributesException("It is illegal to place files at this location."); else sourceName = contextName.substring(0, firstSlash);
                name = contextName.substring(firstSlash + 1);
                SourceNode sourceNode = getSource(sourceName);
                synchronized (sourceNode) {
                    Source source = sourceNode.source;
                    ChannelMap sourceMap = sourceNode.map;
                    sourceMap.Clear();
                    byte[] bytes = getBytesFromResource(obj, sourceNode);
                    sourceNode.pBlock.parseQueryString(name, null, (Map) properties.get("parameterMap"));
                    name = sourceNode.pBlock.name;
                    if (bytes.length == 0) {
                        if (sourceNode.pBlock.mux == 1) {
                            int index = sourceMap.Add(name);
                            sourceMap.PutMime(index, "text/xml");
                            sourceMap.PutDataAsString(index, "<rbnb><size>0</size><mime>" + ZEROFILE_MIME + "</mime></rbnb>");
                        } else {
                            for (int ii = 0; ii < sourceNode.pBlock.mux; ++ii) {
                                int index = sourceMap.Add(name + ii);
                                sourceMap.PutMime(index, "text/xml");
                                sourceMap.PutDataAsString(index, "<rbnb><size>0</size><mime>" + ZEROFILE_MIME + "</mime></rbnb>");
                            }
                        }
                        if (debug) System.err.println("registering null channel(s): " + sourceMap);
                        source.Register(sourceMap);
                    } else {
                        if (sourceNode.pBlock.mux == 1) {
                            int index = sourceMap.Add(name);
                            sourceMap.PutMime(index, "text/xml");
                            sourceMap.PutDataAsString(index, new String("<rbnb><size>" + bytes.length + "</size></rbnb>"));
                            source.Register(sourceMap);
                            if (debug) System.err.println("clearing null registration: " + sourceMap);
                        }
                        if (sourceNode.pBlock.isTimeSet()) sourceMap.PutTime(sourceNode.pBlock.start, sourceNode.pBlock.duration); else sourceMap.PutTimeAuto("timeofday");
                        String theMime;
                        {
                            Attribute attr;
                            if (attrs != null && (attr = attrs.get(ResourceAttributes.CONTENT_TYPE)) != null) theMime = attr.get().toString(); else theMime = sourceNode.pBlock.getMime(servletContext, sourceNode.pBlock.name);
                        }
                        if (sourceNode.pBlock.mux == 1) {
                            int index = sourceMap.Add(name);
                            sourceMap.PutData(index, bytes, sourceNode.pBlock.datatype, sourceNode.pBlock.byteorder);
                            sourceMap.PutMime(index, theMime);
                        } else {
                            ChannelMap regMap = new ChannelMap();
                            int wordSize = sourceNode.pBlock.getWordSize();
                            int repeatSize = wordSize * sourceNode.pBlock.blockSize;
                            int totalSize = bytes.length / sourceNode.pBlock.mux;
                            for (int ii = 0; ii < sourceNode.pBlock.mux; ++ii) {
                                int index = regMap.Add(name + ii);
                                regMap.PutMime(index, "text/xml");
                                regMap.PutDataAsString(index, new String("<rbnb><size>" + totalSize + "</size></rbnb>"));
                                index = sourceMap.Add(name + ii);
                                byte[] temp = new byte[totalSize];
                                for (int iii = 0; iii < totalSize / repeatSize; ++iii) System.arraycopy(bytes, iii * repeatSize * sourceNode.pBlock.mux + ii * repeatSize, temp, iii * repeatSize, repeatSize);
                                sourceMap.PutData(index, temp, sourceNode.pBlock.datatype, sourceNode.pBlock.byteorder);
                                sourceMap.PutMime(index, theMime);
                            }
                            source.Register(regMap);
                        }
                        source.Flush(sourceMap, false);
                    }
                }
            } else throw new javax.naming.OperationNotSupportedException("Relative context creation not supported.");
        } catch (SAPIException se) {
            removeSource(sourceName);
            throw convertException("RBNB Error", se);
        }
    }

    /**
	  * Creates and binds a new context, along with associated attributes.
	  */
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return createSubcontext(nameToString(name), attrs);
    }

    /**
	  * Creates and binds a new context, along with associated attributes.
	  */
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        name = calcAbsolutePath(name);
        if (debug) System.err.println("JNDI: createSubcontext(" + name + ")*");
        if (!userBrowser) throw new InvalidAttributesException("An RBNB Source, \"" + name + "\", cannot be created from this web application");
        try {
            if (name.charAt(0) == Rmap.PATHDELIMITER) {
                String serverName = getServerName();
                if (!name.startsWith(serverName)) throw new InvalidAttributesException("An RBNB Source, \"" + name + "\", cannot be created from this entry point, \"" + serverName + "\".");
                String contextName;
                contextName = name.substring(serverName.length() + 1);
                int firstSlash = contextName.indexOf(Rmap.PATHDELIMITER);
                if (firstSlash == -1) getSource(contextName); else {
                    String sourceName = contextName.substring(0, firstSlash), chanName = contextName.substring(firstSlash + 1);
                    SourceNode sourceNode = getSource(sourceName);
                    synchronized (sourceNode) {
                        Source s = sourceNode.source;
                        ChannelMap cmap = sourceNode.map;
                        cmap.Clear();
                        int index = cmap.Add(chanName);
                        cmap.PutMime(index, "text/xml");
                        cmap.PutDataAsString(index, "<rbnb><size>0</size><mime>" + COLLECTION_MIME + "</mime></rbnb>");
                        s.Register(cmap);
                    }
                }
            } else throw new javax.naming.OperationNotSupportedException("Relative context creation not supported.");
            return this;
        } catch (Throwable se) {
            throw convertException("RBNB Error: createSubcontext()", se);
        }
    }

    /**
	  * Retrieves all of the attributes associated with a named object.
	  */
    public Attributes getAttributes(Name name) throws NamingException {
        return getAttributes(nameToString(name));
    }

    /** 
	  * Retrieves selected attributes associated with a named object.
	  */
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        return getAttributes(nameToString(name), attrIds);
    }

    /**
	  * Retrieves all of the attributes associated with a named object.
	  */
    public Attributes getAttributes(String name) throws NamingException {
        if (debug) System.err.println("JNDI: getAttributes(" + name + ")");
        ResourceAttributes ra = new ResourceAttributes(new BasicAttributes());
        if (name == null || name.length() == 0 || name.charAt(name.length() - 1) == '/') {
            ra.setCollection(true);
            ra.setContentLength(0);
            return ra;
        }
        name = calcAbsolutePath(name);
        {
            SinkNode sn = null;
            try {
                sn = checkSinkConnection();
                sn.paramBlock.parseQueryString(name, null, (Map) properties.get("parameterMap"));
                name = sn.paramBlock.name;
                if (sn.map.GetIfFetchTimedOut()) {
                    sn.sink.CloseRBNBConnection();
                    sn = null;
                    throw new NamingException("Timed out waiting for response.");
                }
                ChannelTree.Node resNode = checkRegistrationCache(sn, name, name, false);
                if (resNode != null) {
                    ra.setCreation((long) (resNode.getStart() * 1000));
                    ra.setLastModified((long) ((resNode.getStart() + resNode.getDuration()) * 1000));
                    switch(sn.paramBlock.fetchtype) {
                        case RequestParameters.FETCH_DATA:
                            {
                                String mime = resNode.getMime();
                                if (ZEROFILE_MIME.equals(mime)) {
                                    ra.setCollection(false);
                                    ra.setContentLength(0);
                                } else if (COLLECTION_MIME.equals(mime) || resNode.getType() != ChannelTree.CHANNEL) {
                                    ra.setCollection(true);
                                    ra.setContentLength(0);
                                } else {
                                    ra.setCollection(false);
                                    ra.setContentLength(resNode.getSize());
                                    if (resNode.getMime() != null) ra.setResourceType(resNode.getMime());
                                }
                            }
                            break;
                        case RequestParameters.FETCH_TIMES:
                        case RequestParameters.FETCH_SIZE:
                        case RequestParameters.FETCH_DATATYPE:
                            ra.setResourceType("application/octet-stream");
                            break;
                        case RequestParameters.FETCH_MIME:
                        case RequestParameters.FETCH_INFO:
                            ra.setResourceType("text/plain");
                            break;
                        default:
                            throw new Error("Bad case in RBNBDirContext::getAttributes().");
                    }
                }
                if (debug) {
                    System.err.println(ra + ":");
                    System.err.println("Name: " + ra.getName());
                    System.err.println("ResourceType: " + ra.getResourceType());
                    System.err.println("Creation: " + ra.getCreation());
                    System.err.println("ContentLength: " + ra.getContentLength());
                    System.err.println("LastModified: " + ra.getLastModified());
                }
                return ra;
            } catch (SAPIException se) {
                throw convertException("RBNB Error", se);
            } finally {
                if (sn != null) recycleSinkNode(sn);
            }
        }
    }

    /**
	  * Retrieves selected attributes associated with a named object.
	  */
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return getAttributes(name);
    }

    /**
	  * Retrieves the schema associated with the named object.
	  */
    public DirContext getSchema(Name name) {
        return null;
    }

    /**
	  * Retrieves the schema associated with the named object.
	  */
    public DirContext getSchema(String name) {
        return null;
    }

    /**
	  * Retrieves a context containing the schema objects of the named 
	  *  object's class definitions.
	  */
    public DirContext getSchemaClassDefinition(Name name) {
        return null;
    }

    /**
	  * Retrieves a context containing the schema objects of the named 
	  *  object's class definitions.
	  */
    public DirContext getSchemaClassDefinition(String name) {
        return null;
    }

    /**
	  * Modifies the attributes associated with a named object.
	  */
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) {
    }

    /**
	  * Modifies the attributes associated with a named object using an an
	  *  ordered list of modifications.
	  */
    public void modifyAttributes(Name name, ModificationItem[] mods) {
    }

    /**
	  * Modifies the attributes associated with a named object.
	  */
    public void modifyAttributes(String name, int mod_op, Attributes attrs) {
    }

    /**
	  * Modifies the attributes associated with a named object using an an 
	  *  ordered list of modifications.
	  */
    public void modifyAttributes(String name, ModificationItem[] mods) {
    }

    /**
	  * Binds a name to an object, along with associated attributes, 
	  *  overwriting any existing binding.
	  */
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        bind(name, obj, attrs);
    }

    /**
	  * Binds a name to an object, along with associated attributes, 
	  *  overwriting any existing binding.
	  */
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        bind(name, obj, attrs);
    }

    /**
	  * Searches in a single context for objects that contain a specified set
	  *  of attributes.
	  */
    public NamingEnumeration search(Name name, Attributes matchingAttributes) {
        return null;
    }

    /**
	  * Searches in a single context for objects that contain a specified set 
	  *  of attributes, and retrieves selected attributes.
	  */
    public NamingEnumeration search(Name name, Attributes matchingAttributes, String[] attributesToReturn) {
        return null;
    }

    /**
	  * Searches in the named context or object for entries that satisfy the 
	  *  given search filter.
	  */
    public NamingEnumeration search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) {
        return null;
    }

    /**
	  * Searches in the named context or object for entries that satisfy the 
	  *  given search filter.
	  */
    public NamingEnumeration search(Name name, String filter, SearchControls cons) {
        return null;
    }

    /**
	  * Searches in a single context for objects that contain a specified set 
	  *  of attributes.
	  */
    public NamingEnumeration search(String name, Attributes matchingAttributes) {
        return null;
    }

    /**
	  * Searches in a single context for objects that contain a specified set 
	  *  of attributes, and retrieves selected attributes.
	  */
    public NamingEnumeration search(String name, Attributes matchingAttributes, String[] attributesToReturn) {
        return null;
    }

    /**
	  * Searches in the named context or object for entries that satisfy the 
	  *  given search filter.
	  */
    public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) {
        return null;
    }

    /**
	  * Searches in the named context or object for entries that satisfy the 
	  *  given search filter.
	  */
    public NamingEnumeration search(String name, String filter, SearchControls cons) {
        return null;
    }

    /**
     * Retrieves the named object.  If <code>name</code> refers to more than 
	 *   one object, the first one is returned.
	 * <p>  Returns a 
	 *  <code>org.apache.naming.resources.Resource</code> object, as required by 
	 *  <code>org.apache.catalina.servlets.WebdavServlet</code>.
     * <p>TODO: If <tt>name</tt> is empty, returns a new instance of this 
	 *  context
     * (which represents the same naming context as this context, but its
     * environment may be modified independently and it may be accessed
     * concurrently).
     *
     * @param name
     *		the name of the object to look up
     * @return	the object bound to <tt>name</tt>
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #lookup(String)
     * @see #lookupLink(Name)
     */
    public Object lookup(Name name) throws NamingException {
        return lookup(nameToString(name));
    }

    /**
     * Retrieves the named object.  If <code>name</code> refers to more than 
	 *   one object, the first one is returned.
     * See {@link #lookup(Name)} for details.
     */
    public Object lookup(String name) throws NamingException {
        if (debug) System.err.println("JNDI: lookup(" + name + "): " + Thread.currentThread());
        if (name.length() == 0 || "/".equals(name)) return this;
        name = calcAbsolutePath(name);
        {
            {
                SinkNode sn = null;
                try {
                    sn = checkSinkConnection();
                    if (name.charAt(name.length() - 1) != '/') {
                        sn.paramBlock.parseQueryString(name, null, (Map) properties.get("parameterMap"));
                        switch(checkRegistration(sn, sn.paramBlock.name)) {
                            case REG_COLLECTION:
                                return new RBNBDirContext(properties, sn.paramBlock.name + '/');
                            case REG_ZEROFILE:
                                return new Resource(new byte[0]);
                            case REG_RESOURCE:
                                if (debug) System.err.println("requestQueued(" + sn.paramBlock.name + ',' + sn.paramBlock.start + ',' + sn.paramBlock.duration + ',' + sn.paramBlock.reference + ") " + "RequestData =" + sn.paramBlock.requestData + Thread.currentThread());
                                return new Resource(new RBNBInputStream(this, sn.paramBlock));
                            default:
                                throw new Error("Bad case found in RBNBDirContext.lookup().");
                        }
                    } else {
                        name = name.substring(0, name.length() - 1);
                        switch(checkRegistration(sn, name)) {
                            case REG_COLLECTION:
                                return new RBNBDirContext(properties, name + '/');
                            case REG_ZEROFILE:
                                return new Resource(new byte[0]);
                            case REG_RESOURCE:
                                throw new InvalidNameException("RBNBDirContext.lookup() referenced a resource" + " using a trailing slash, which is reserved for " + "DirContexts.");
                            default:
                                throw new Error("Bad case found in RBNBDirContext.lookup().");
                        }
                    }
                } catch (SAPIException se) {
                    throw convertException("RBNB Error", se);
                } finally {
                    if (sn != null) recycleSinkNode(sn);
                }
            }
        }
    }

    /**
     * Binds a name to an object.
     * All intermediate contexts and the target context (that named by all
     * but terminal atomic component of the name) NEED NOT already exist.
     *
	 * <p>Calls {@link #bind(Name,Object,Attributes)} with a null for the 
	 *  attributes parameter.
     */
    public void bind(Name name, Object obj) throws NamingException {
        bind(name, obj, null);
    }

    /**
     * Binds a name to an object.
     * All intermediate contexts and the target context (that named by all
     * but terminal atomic component of the name) NEED NOT already exist.
     *
	 * <p>Calls {@link #bind(String,Object,Attributes)} with a null for the 
	 *  attributes parameter.
     */
    public void bind(String name, Object obj) throws NamingException {
        bind(name, obj, null);
    }

    /**
     * Binds a name to an object, overwriting any existing binding.
     * All intermediate contexts and the target context (that named by all
     * but terminal atomic component of the name) must already exist.
     *
     * <p> If the object is a <tt>DirContext</tt>, any existing attributes
     * associated with the name are replaced with those of the object.
     * Otherwise, any existing attributes associated with the name remain
     * unchanged.
     *
     * @param name
     *		the name to bind; may not be empty
     * @param obj
     *		the object to bind; possibly null
     * @throws	javax.naming.directory.InvalidAttributesException
     *	 	if object did not supply all mandatory attributes
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #rebind(String, Object)
     * @see #bind(Name, Object)
     * @see javax.naming.directory.DirContext#rebind(Name, Object,
     *		javax.naming.directory.Attributes)
     * @see javax.naming.directory.DirContext
     */
    public void rebind(Name name, Object obj) throws NamingException {
        bind(name, obj);
    }

    /**
     * Binds a name to an object, overwriting any existing binding.
     * See {@link #rebind(Name, Object)} for details.
     *
     * @param name
     *		the name to bind; may not be empty
     * @param obj
     *		the object to bind; possibly null
     * @throws	javax.naming.directory.InvalidAttributesException
     *	 	if object did not supply all mandatory attributes
     * @throws	NamingException if a naming exception is encountered
     */
    public void rebind(String name, Object obj) throws NamingException {
        bind(name, obj);
    }

    /**
     * Unbinds the named object.
     * Removes the terminal atomic name in <code>name</code>
     * from the target context--that named by all but the terminal
     * atomic part of <code>name</code>.
     *
     * <p> This method is idempotent.
     * It succeeds even if the terminal atomic name
     * is not bound in the target context, but throws
     * <tt>NameNotFoundException</tt>
     * if any of the intermediate contexts do not exist.
     *
     * <p> Any attributes associated with the name are removed.
     * Intermediate contexts are not changed.
     *
     * @param name
     *		the name to unbind; may not be empty
     * @throws	NameNotFoundException if an intermediate context does not exist
     * @throws	NamingException if a naming exception is encountered
     * @see #unbind(String)
     */
    public void unbind(Name name) throws NamingException {
        unbind(nameToString(name));
    }

    /**
     * Unbinds the named object.
     * See {@link #unbind(Name)} for details.
     *
     * @param name
     *		the name to unbind; may not be empty
     * @throws	NameNotFoundException if an intermediate context does not exist
     * @throws	NamingException if a naming exception is encountered
     */
    public void unbind(String name) throws NamingException {
        if (debug) System.err.println("JNDI: unbind(" + name + ")");
        destroySubcontext(name);
    }

    /**
     * Binds a new name to the object bound to an old name, and unbinds
     * the old name.  Both names are relative to this context.
     * Any attributes associated with the old name become associated
     * with the new name.
     * Intermediate contexts of the old name are not changed.
     *
     * @param oldName
     *		the name of the existing binding; may not be empty
     * @param newName
     *		the name of the new binding; may not be empty
     * @throws	NameAlreadyBoundException if <tt>newName</tt> is already bound
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #rename(String, String)
     * @see #bind(Name, Object)
     * @see #rebind(Name, Object)
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        throw new UnsupportedOperationException();
    }

    /**
     * Binds a new name to the object bound to an old name, and unbinds
     * the old name.
     * See {@link #rename(Name, Name)} for details.
     *
     * @param oldName
     *		the name of the existing binding; may not be empty
     * @param newName
     *		the name of the new binding; may not be empty
     * @throws	NameAlreadyBoundException if <tt>newName</tt> is already bound
     * @throws	NamingException if a naming exception is encountered
     */
    public void rename(String oldName, String newName) throws NamingException {
        throw new UnsupportedOperationException();
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * class names of objects bound to them.  Each element of the
     *		enumeration is of type <tt>NameClassPair</tt>.
     * <p>The contents of any subcontexts are not included.
     *
     * <p> If a binding is added to or removed from this context,
     * its effect on an enumeration previously returned is undefined.
     *
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the names and class names of the
     *		bindings in this context.  Each element of the
     *		enumeration is of type <tt>NameClassPair</tt>.
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #list(String)
     * @see #listBindings(Name)
     * @see NameClassPair
     */
    public NamingEnumeration list(Name name) throws NamingException {
        return list(nameToString(name));
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * class names of objects bound to them.
     * See {@link #list(Name)} for details.
     *
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the names and class names of the
     *		bindings in this context.  Each element of the
     *		enumeration is of type <tt>NameClassPair</tt>.
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration list(String name) throws NamingException {
        if (debug) System.err.println("JNDI: list(" + name + ")");
        String currPath, nodeName = name;
        if (name.length() == 0 || "/".equals(name)) {
            if (userBrowser) {
                Object[] keys = sources.keySet().toArray();
                Arrays.sort(keys);
                return new RBNBNamingEnumeration(keys, debug);
            }
            name = "/*";
            currPath = "/";
            nodeName = null;
        } else {
            currPath = nodeName = calcAbsolutePath(name);
            if (currPath.charAt(currPath.length() - 1) != '/') currPath += '/'; else nodeName = nodeName.substring(0, nodeName.length() - 1);
            name = currPath + '*';
        }
        {
            int ii = 0;
            SinkNode sn = null;
            while (true) {
                try {
                    sn = checkSinkConnection();
                    ChannelTree.Node resNode = checkRegistrationCache(sn, nodeName, name, true);
                    NamingEnumeration _enum = new RBNBNamingEnumeration((resNode == null ? cacheTree.rootIterator() : resNode.getChildren().iterator()), hiddenSet);
                    return _enum;
                } catch (SAPIException se) {
                    if (sn != null) sn.sink.CloseRBNBConnection();
                    if (ii++ == 0) continue;
                    throw convertException("RBNB Error", se);
                } finally {
                    if (sn != null) recycleSinkNode(sn);
                }
            }
        }
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * objects bound to them.  Each element of the enumeration is of type
     *		<tt>Binding</tt>.
     * The contents of any subcontexts are not included.
     *
     * <p> If a binding is added to or removed from this context,
     * its effect on an enumeration previously returned is undefined.
     *
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the bindings in this context.
     *		Each element of the enumeration is of type
     *		<tt>Binding</tt>.
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #listBindings(String)
     * @see #list(Name)
     * @see Binding
      */
    public NamingEnumeration listBindings(Name name) throws NamingException {
        return listBindings(nameToString(name));
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * objects bound to them.
     * See {@link #listBindings(Name)} for details.
     *
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the bindings in this context.
     *		Each element of the enumeration is of type
     *		<tt>Binding</tt>.
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration listBindings(String name) throws NamingException {
        name = calcAbsolutePath(name);
        SinkNode sn = null;
        try {
            {
                sn = checkSinkConnection();
                ChannelMap result = new ChannelMap();
                result.Add(name);
                sn.sink.Request(result, 0, 0, "newest");
                sn.sink.Fetch(TIMEOUT, result);
                if (sn.map.GetIfFetchTimedOut()) {
                    sn.sink.CloseRBNBConnection();
                    sn = null;
                    throw new NamingException("Timed out waiting for response.");
                }
                RBNBBindingEnumeration _enum = new RBNBBindingEnumeration(result, debug);
                return _enum;
            }
        } catch (SAPIException se) {
            throw convertException("Server Exception", se);
        } finally {
            if (sn != null) recycleSinkNode(sn);
        }
    }

    /**
     * Destroys the named context and removes it from the namespace.
     * Any attributes associated with the name are also removed.
     * Intermediate contexts are not destroyed.
     *
     * <p> This method is idempotent.
     * It succeeds even if the terminal atomic name
     * is not bound in the target context, but throws
     * <tt>NameNotFoundException</tt>
     * if any of the intermediate contexts do not exist.
     *
     * <p> In a federated naming system, a context from one naming system
     * may be bound to a name in another.  One can subsequently
     * look up and perform operations on the foreign context using a
     * composite name.  However, an attempt destroy the context using
     * this composite name will fail with
     * <tt>NotContextException</tt>, because the foreign context is not
     * a "subcontext" of the context in which it is bound.
     * Instead, use <tt>unbind()</tt> to remove the
     * binding of the foreign context.  Destroying the foreign context
     * requires that the <tt>destroySubcontext()</tt> be performed
     * on a context from the foreign context's "native" naming system.
     *
     * @param name
     *		the name of the context to be destroyed; may not be empty
     * @throws	NameNotFoundException if an intermediate context does not exist
     * @throws	NotContextException if the name is bound but does not name a
     *		context, or does not name a context of the appropriate type
     * @throws	ContextNotEmptyException if the named context is not empty
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #destroySubcontext(String)
     */
    public void destroySubcontext(Name name) throws NamingException {
        destroySubcontext(nameToString(name));
    }

    /**
     * Destroys the named context and removes it from the namespace.
     * See {@link #destroySubcontext(Name)} for details.
     *
     * @param name
     *		the name of the context to be destroyed; may not be empty
     * @throws	NameNotFoundException if an intermediate context does not exist
     * @throws	NotContextException if the name is bound but does not name a
     *		context, or does not name a context of the appropriate type
     * @throws	ContextNotEmptyException if the named context is not empty
     * @throws	NamingException if a naming exception is encountered
     */
    public void destroySubcontext(String name) throws NamingException {
        if (debug) System.err.println("JNDI: destroySubcontext(" + name + ")");
        name = calcAbsolutePath(name);
        if (userBrowser) {
            if (name.charAt(name.length() - 1) == Rmap.PATHDELIMITER) name = name.substring(0, name.length() - 1);
            try {
                String serverName = getServerName();
                if (!name.startsWith(serverName)) throw new NotContextException();
                int nextSlash = name.indexOf(Rmap.PATHDELIMITER, serverName.length() + 1);
                if (nextSlash == -1) removeSource(name.substring(serverName.length() + 1)); else deleteChannel(name.substring(serverName.length() + 1, nextSlash), name, name.substring(nextSlash + 1));
            } catch (SAPIException se) {
                throw convertException("RBNB Error", se);
            }
        } else throw new OperationNotSupportedException();
    }

    private void deleteChannel(String sourceName, String fullName, String channel) throws SAPIException, NamingException {
        SourceNode sourceNode = getSource(sourceName);
        if (sourceNode.protect) {
            hiddenSet.add(fullName);
        } else {
            sourceNode.map.Clear();
            sourceNode.map.Add(channel);
            if (debug) System.err.println("Deleting: " + sourceNode.map);
            ChannelMap result = sourceNode.source.Delete(sourceNode.map);
            if (debug) System.err.println(result.GetDataAsString(0)[0]);
            clearCache();
        }
    }

    /**
     * Creates and binds a new context.
     * Creates a new context with the given name and binds it in
     * the target context (that named by all but terminal atomic
     * component of the name).  All intermediate contexts and the
     * target context must already exist.
     *
     * @param name
     *		the name of the context to create; may not be empty
     * @return	the newly created context
     *
     * @throws	NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException
     *		if creation of the subcontext requires specification of
     *		mandatory attributes
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #createSubcontext(String)
     * @see javax.naming.directory.DirContext#createSubcontext
     */
    public Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(name, null);
    }

    /**
     * Creates and binds a new context.
     * See {@link #createSubcontext(Name)} for details.
     *
     * @param name
     *		the name of the context to create; may not be empty
     * @return	the newly created context
     *
     * @throws	NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException
     *		if creation of the subcontext requires specification of
     *		mandatory attributes
     * @throws	NamingException if a naming exception is encountered
     */
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(name, null);
    }

    /**
     * Retrieves the named object, following links except
     * for the terminal atomic component of the name.
     * If the object bound to <tt>name</tt> is not a link,
     * returns the object itself.
     *
     * @param name
     *		the name of the object to look up
     * @return	the object bound to <tt>name</tt>, not following the
     *		terminal link (if any).
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #lookupLink(String)
     */
    public Object lookupLink(Name name) throws NamingException {
        return lookup(name);
    }

    /**
     * Retrieves the named object, following links except
     * for the terminal atomic component of the name.
     * See {@link #lookupLink(Name)} for details.
     *
     * @param name
     *		the name of the object to look up
     * @return	the object bound to <tt>name</tt>, not following the
     *		terminal link (if any)
     * @throws	NamingException if a naming exception is encountered
     */
    public Object lookupLink(String name) throws NamingException {
        return lookup(name);
    }

    /**
     * Retrieves the parser associated with the named context.
     * In a federation of namespaces, different naming systems will
     * parse names differently.  This method allows an application
     * to get a parser for parsing names into their atomic components
     * using the naming convention of a particular naming system.
     * Within any single naming system, <tt>NameParser</tt> objects
     * returned by this method must be equal (using the <tt>equals()</tt>
     * test).
     *
     * @param name
     *		the name of the context from which to get the parser
     * @return	a name parser that can parse compound names into their atomic
     *		components
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #getNameParser(String)
     * @see CompoundName
     */
    public NameParser getNameParser(Name name) throws NamingException {
        return null;
    }

    /**
     * Retrieves the parser associated with the named context.
     * See {@link #getNameParser(Name)} for details.
     *
     * @param name
     *		the name of the context from which to get the parser
     * @return	a name parser that can parse compound names into their atomic
     *		components
     * @throws	NamingException if a naming exception is encountered
     */
    public NameParser getNameParser(String name) throws NamingException {
        return null;
    }

    /**
     * Composes the name of this context with a name relative to
     * this context.
     * Given a name (<code>name</code>) relative to this context, and
     * the name (<code>prefix</code>) of this context relative to one
     * of its ancestors, this method returns the composition of the
     * two names using the syntax appropriate for the naming
     * system(s) involved.  That is, if <code>name</code> names an
     * object relative to this context, the result is the name of the
     * same object, but relative to the ancestor context.  None of the
     * names may be null.
     * <p>
     * For example, if this context is named "wiz.com" relative
     * to the initial context, then
     * <pre>
     *	composeName("east", "wiz.com")	</pre>
     * might return <code>"east.wiz.com"</code>.
     * If instead this context is named "org/research", then
     * <pre>
     *	composeName("user/jane", "org/research")	</pre>
     * might return <code>"org/research/user/jane"</code> while
     * <pre>
     *	composeName("user/jane", "research")	</pre>
     * returns <code>"research/user/jane"</code>.
     *
     * @param name
     *		a name relative to this context
     * @param prefix
     *		the name of this context relative to one of its ancestors
     * @return	the composition of <code>prefix</code> and <code>name</code>
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #composeName(String, String)
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        return ((Name) prefix.clone()).addAll(name);
    }

    /**
     * Composes the name of this context with a name relative to
     * this context.
     * See {@link #composeName(Name, Name)} for details.
     *
     * @param name
     *		a name relative to this context
     * @param prefix
     *		the name of this context relative to one of its ancestors
     * @return	the composition of <code>prefix</code> and <code>name</code>
     * @throws	NamingException if a naming exception is encountered
     */
    public String composeName(String name, String prefix) throws NamingException {
        return prefix + Rmap.PATHDELIMITER + name;
    }

    /**
     * Adds a new environment property to the environment of this
     * context.  If the property already exists, its value is overwritten.
     * <p>Properties supported:
	 * <dl compact>
	 *	<dt>hostname</dt><dd>The internet address of the rbnb to which this 
	 *   context refers.</dd>
	 *  <dt>parameterMap</dt><dd>The map of parameter names to values for the
	 *	 latest request.</dd>
	 * </dl>
     *
     * @param propName
     *		the name of the environment property to add; may not be null
     * @param propVal
     *		the value of the property to add; may not be null
     * @return	the previous value of the property, or null if the property was
     *		not in the environment before
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #getEnvironment()
     * @see #removeFromEnvironment(String)
     */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return properties.put(propName, propVal);
    }

    /**
     * Removes an environment property from the environment of this
     * context.
     *
     * @param propName
     *		the name of the environment property to remove; may not be null
     * @return	the previous value of the property, or null if the property was
     *		not in the environment
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #getEnvironment()
     * @see #addToEnvironment(String, Object)
     */
    public Object removeFromEnvironment(String propName) throws NamingException {
        return properties.remove(propName);
    }

    /**
     * Retrieves the environment in effect for this context.
     * See {@link #addToEnvironment(String,Object)} for more details on 
	 *   environment properties.
     *
     * <p> The caller should not make any changes to the object returned:
     * their effect on the context is undefined.
     * The environment of this context may be changed using
     * <tt>addToEnvironment()</tt> and <tt>removeFromEnvironment()</tt>.
     *
     * @return	the environment of this context; never null
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #addToEnvironment(String, Object)
     * @see #removeFromEnvironment(String)
     */
    public Hashtable getEnvironment() throws NamingException {
        try {
            properties.put("serverName", getServerName());
        } catch (SAPIException se) {
            throw convertException("Could not determine server name: ", se);
        }
        return properties;
    }

    /**
     * Closes this context.
     * This method releases this context's resources immediately, instead of
     * waiting for them to be released automatically by the garbage collector.
     *
     * <p> This method is idempotent:  invoking it on a context that has
     * already been closed has no effect.  Invoking any other method
     * on a closed context is not allowed, and results in undefined behaviour.
     *
     * @throws	NamingException if a naming exception is encountered
     */
    public void close() throws NamingException {
        while (!sinkPool.empty()) {
            SinkNode sn = (SinkNode) sinkPool.pop();
            sn.sink.CloseRBNBConnection();
        }
        synchronized (sources) {
            Enumeration sourceList = sources.elements();
            while (sourceList.hasMoreElements()) ((Source) sourceList.nextElement()).CloseRBNBConnection();
        }
    }

    /**
     * Retrieves the full name of this context within its own namespace.
     *
     * <p> In RBNB terms, this returns the fully qualified name of the 
	 *  sink, if connected.
     *
     * @return	this context's name in its own namespace; never null
     * @throws	NamingException if not connected to a server.
     *
     * @since 1.3
     */
    public String getNameInNamespace() throws NamingException {
        try {
            return currentDirectory;
        } catch (Exception e) {
            throw convertException("Not connected to a server.", e);
        }
    }

    /**
	  * Returns the state of the debug flag.
	  */
    boolean getDebug() {
        return debug;
    }

    SinkNode checkSinkConnection() throws SAPIException {
        SinkNode sn;
        boolean needConnect = true;
        if (sinkPool.empty()) {
            sn = new SinkNode(defaultProperties);
        } else {
            synchronized (sinkPool) {
                sn = (SinkNode) sinkPool.pop();
            }
            try {
                sn.sink.GetServerName();
                needConnect = !sn.sink.VerifyConnection();
            } catch (IllegalStateException ise) {
            }
        }
        if (needConnect) {
            sn.sink.OpenRBNBConnection(getPropertyInternal("hostname").toString(), getPropertyInternal("sinkname").toString(), getPropertyInternal("user").toString(), getPropertyInternal("password").toString());
        }
        return sn;
    }

    static void recycleSinkNode(SinkNode sn) {
        boolean doClose = false;
        synchronized (sinkPool) {
            if (sinkPool.size() < MAX_POOL_SIZE) {
                sinkPool.push(sn);
            } else {
                doClose = true;
            }
        }
        if (doClose) {
            sn.sink.CloseRBNBConnection();
        }
    }

    /**
	  * Returns a property, including checks to see if it is a default
	  *  property.
	  */
    private Object getPropertyInternal(String propName) {
        Object result;
        if ((result = properties.get(propName)) == null) return defaultProperties.get(propName); else return result;
    }

    /**
	  * Converts a name to an RBNB channel name.
	  */
    private String nameToString(Name name) {
        StringBuffer fullName = new StringBuffer();
        for (int ii = 0; ii < name.size() - 1; ++ii) fullName.append(name.get(ii)).append(Rmap.PATHDELIMITER);
        fullName.append(name.get(name.size() - 1));
        return fullName.toString();
    }

    /**
	  * Resets the cache.
	  */
    private void clearCache() {
        cacheFreshnessDate = 0;
    }

    /**
	  * Converts a random exception into a nested CommunicationException.
	  */
    private NamingException convertException(String msg, Throwable t) {
        t.printStackTrace();
        CommunicationException ce = new CommunicationException(msg);
        ce.setRootCause(t);
        return ce;
    }

    /**
	  * Returns the name of the current server.  It will begin with a slash,
	  *   but not end in one.
	  */
    private String getServerName() throws SAPIException {
        SinkNode sn = null;
        try {
            sn = checkSinkConnection();
            return sn.sink.GetServerName();
        } finally {
            if (sn != null) recycleSinkNode(sn);
        }
    }

    private SourceNode getSource(String sourceName) throws SAPIException {
        RequestParameters params = new RequestParameters(defaultProperties);
        params.parseQueryString(sourceName, null, (Map) properties.get("parameterMap"));
        sourceName = params.name;
        synchronized (sources) {
            SourceNode s = (SourceNode) sources.get(sourceName);
            if (s == null || !s.source.VerifyConnection()) {
                s = new SourceNode(params);
                s.source.OpenRBNBConnection(getPropertyInternal("hostname").toString(), sourceName, getPropertyInternal("user").toString(), getPropertyInternal("password").toString());
                sources.put(sourceName, s);
            }
            return s;
        }
    }

    /**
	  * Calculates the absolute path based on the current directory and
	  *  the input, which may be relative or absolute.
	  * <p>05/06/2003  WHF  Now looks for /User magic name, remaps it to
	  *  the absolute name.
	  */
    private String calcAbsolutePath(String name) throws NamingException {
        try {
            String toReturn = name;
            if (currentDirectory.length() > 0 && !name.startsWith(currentDirectory)) {
                toReturn = currentDirectory + name;
            }
            if (userBrowser) {
                String sname = getServerName();
                if (!toReturn.startsWith(sname)) toReturn = sname + toReturn;
            }
            if (debug) System.err.println("Converted: '" + name + "' to '" + toReturn + '\'');
            return toReturn;
        } catch (SAPIException se) {
            throw convertException("RBNB Error finding current server name.", se);
        }
    }

    /**
	  * Remove trailing slashes.
	  */
    private String removeTrailingSlash(String name) {
        if (name.charAt(name.length() - 1) != '/') return name.substring(0, name.length() - 1);
        return name;
    }

    /**
	  * Removes a source matching <code>name</code> if it exists, and returns
	  *  <code>true</code>; otherwise returns false.
	  */
    private boolean removeSource(String name) {
        synchronized (sources) {
            SourceNode node = (SourceNode) sources.get(name);
            if (node == null) return false;
            synchronized (node.source) {
                node.source.CloseRBNBConnection();
                sources.remove(name);
                clearCache();
            }
            return true;
        }
    }

    /**
	  * @param toAdd If true, this is a <code>list</code> call, and the 
	  *  result should be placed in the cache.
	  */
    private ChannelTree.Node checkRegistrationCache(SinkNode sn, String name, String requestString, boolean toAdd) throws SAPIException {
        ChannelTree.Node result = null;
        synchronized (cacheTree) {
            if (!hiddenSet.contains(name) && (toAdd || ((System.currentTimeMillis()) - cacheFreshnessDate > 60000) || (result = cacheTree.findNode(name)) == null)) {
                sn.map.Clear();
                sn.map.Add(requestString);
                if (debug) System.err.println("RequestRegistration(" + requestString + ')');
                sn.sink.RequestRegistration(sn.map);
                sn.sink.Fetch(TIMEOUT, sn.map);
                if (sn.map.GetIfFetchTimedOut()) {
                    sn.sink.CloseRBNBConnection();
                    sn = null;
                    throw new SAPIException("Timed out waiting for response.");
                }
                ChannelTree newTree = ChannelTree.createFromChannelMap(sn.map);
                result = newTree.findNode(name);
                if (toAdd) {
                    cacheFreshnessDate = System.currentTimeMillis();
                    cacheTree = newTree;
                    if (debug) System.err.println("New registration tree: " + cacheTree);
                }
            }
        }
        if (debug) System.err.println("Seeking \"" + name + "\" in cache, found: " + result);
        return result;
    }

    /**
	  * Checks the registration for 'name'.  Returns an integer constant
	  *  identifying the type of result discovered.
	  *
	  * <p>Note that checkRegistration on a MUX channel set will return 
	  *  a result of 'zero-length-file', even if the channel set doesn't 
	  *  actually exist.  This is a kluge to support file systems, which 
	  *  always perform a zero length PUT before the actual PUT.
	  */
    private int checkRegistration(SinkNode sn, String name) throws NamingException, SAPIException {
        if (sn.paramBlock.mux > 1) return REG_ZEROFILE;
        ChannelTree.Node resNode = checkRegistrationCache(sn, name, name, false);
        if (resNode != null) {
            String mime = resNode.getMime();
            if (COLLECTION_MIME.equals(mime)) return REG_COLLECTION; else if (ZEROFILE_MIME.equals(mime)) return REG_ZEROFILE; else if (resNode.getType() != ChannelTree.CHANNEL) return REG_COLLECTION; else return REG_RESOURCE;
        } else {
            throw new NamingException("No data matching \"" + name + "\".");
        }
    }

    /**
	  * Returns the byte array which represents this resource object.
	  *
	  * @since 04/17/2003
	  */
    private byte[] getBytesFromResource(Object obj, SourceNode sourceNode) throws NamingException {
        byte[] bytes;
        if (obj instanceof org.apache.naming.resources.Resource) {
            org.apache.naming.resources.Resource resource = (org.apache.naming.resources.Resource) obj;
            bytes = resource.getContent();
            if (bytes == null) {
                try {
                    java.io.InputStream is = resource.streamContent();
                    int len;
                    while ((len = is.read(sourceNode.buff)) != -1) sourceNode.baos.write(sourceNode.buff, 0, len);
                    bytes = sourceNode.baos.toByteArray();
                    sourceNode.baos.reset();
                } catch (java.io.IOException ie) {
                    throw convertException("IO Error", ie);
                }
            }
        } else if (obj instanceof byte[]) bytes = (byte[]) obj; else throw new NamingException("Could not convert resource to byte array.");
        return bytes;
    }

    private final Hashtable properties = new Hashtable(), defaultProperties = new Hashtable();

    private boolean debug = false, userBrowser = false;

    private String currentDirectory = "";

    private final XMLMetaDataHandler metaDataHandler = new XMLMetaDataHandler();

    private ServletContext servletContext;

    private static final Hashtable sources = new Hashtable();

    private static ChannelTree cacheTree = ChannelTree.EMPTY_TREE;

    private static long cacheFreshnessDate = 0L;

    private static HashSet hiddenSet = new HashSet();

    /**
	  * A pool of reusable sinks and channelmaps.  The type is 
	  *  SinkNode.
	  */
    private static final Stack sinkPool = new Stack();

    static class SinkNode {

        public SinkNode(java.util.Map defaultProps) {
            paramBlock = new RequestParameters(defaultProps);
        }

        public final Sink sink = new Sink();

        public final ChannelMap map = new ChannelMap();

        public final RequestParameters paramBlock;
    }

    private static class SourceNode {

        public SourceNode(boolean toProt, java.util.Map defaultProperties) {
            protect = toProt;
            pBlock = new RequestParameters(defaultProperties);
            source = new Source(pBlock.getCacheSize(), pBlock.getArchiveMode(), pBlock.getArchiveSize());
        }

        public SourceNode(RequestParameters _pBlock) {
            pBlock = _pBlock;
            protect = pBlock.getProtect();
            source = new Source(pBlock.getCacheSize(), pBlock.getArchiveMode(), pBlock.getArchiveSize());
        }

        public final Source source;

        public final ChannelMap map = new ChannelMap();

        public final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public final RequestParameters pBlock;

        public final boolean protect;

        /**
		  *  Buffer used for streaming into the above array in chunks.  Size
		  *   same as used by FileDirContext.
		  */
        public final byte[] buff = new byte[2048];
    }

    public static final long TIMEOUT = 60000;

    private static final int REG_COLLECTION = 1, REG_ZEROFILE = 2, REG_RESOURCE = 3, MAX_POOL_SIZE = 4;

    private static final String COLLECTION_MIME = "application/collection", ZEROFILE_MIME = "application/nullfile";
}

class RBNBNamingEnumeration implements NamingEnumeration {

    void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void close() throws NamingException {
        elements = null;
    }

    public boolean hasMore() throws NamingException {
        return index < elements.length;
    }

    public Object next() throws NamingException {
        if (index >= elements.length) throw new NoSuchElementException("Number of entries exceeded.");
        return elements[index++];
    }

    public boolean hasMoreElements() {
        try {
            return hasMore();
        } catch (NamingException ne) {
            return false;
        }
    }

    public Object nextElement() {
        try {
            return next();
        } catch (NamingException ne) {
            throw new NoSuchElementException(ne.getMessage());
        }
    }

    public RBNBNamingEnumeration(Iterator iter, java.util.Set hidden) {
        ArrayList list = new ArrayList();
        while (iter.hasNext()) {
            ChannelTree.Node node = (ChannelTree.Node) iter.next();
            if (node.getType() != ChannelTree.SINK && node.getType() != ChannelTree.CONTROLLER && !hidden.contains(node.getFullName())) {
                list.add(new javax.naming.NameClassPair(node.getName(), byte[].class.getName(), true));
            }
        }
        elements = list.toArray();
    }

    public RBNBNamingEnumeration(Object[] names, boolean debug) {
        this.debug = debug;
        elements = new Object[names.length];
        for (int ii = 0; ii < elements.length; ++ii) {
            if (debug) System.err.println(names[ii]);
            elements[ii] = new javax.naming.NameClassPair(names[ii].toString(), byte[].class.getName(), true);
        }
    }

    protected RBNBNamingEnumeration() {
    }

    protected Object[] elements;

    int index = 0;

    private boolean debug = false;
}

class RBNBBindingEnumeration extends RBNBNamingEnumeration {

    public RBNBBindingEnumeration(ChannelMap map, boolean debug) {
        setDebug(debug);
        elements = new Object[map.NumberOfChannels()];
        for (int ii = 0; ii < map.NumberOfChannels(); ++ii) elements[ii] = new javax.naming.Binding(map.GetName(ii), map.GetData(ii), true);
    }

    public RBNBBindingEnumeration(ChannelMap map) {
        this(map, false);
    }
}

class XMLMetaDataHandler extends DefaultHandler {

    public XMLMetaDataHandler() {
        try {
            parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
            parser.setFeature(NAMESPACES_FEATURE_ID, true);
            parser.setContentHandler(this);
            parser.setErrorHandler(this);
        } catch (SAXException se) {
            se.printStackTrace();
        }
    }

    public void parse(String toParse) throws SAXException, java.io.IOException {
        init();
        parser.parse(new InputSource(new java.io.StringReader(toParse)));
    }

    public void startElement(String nsURI, String localName, String qName, org.xml.sax.Attributes atts) throws SAXException {
        if ("size".equals(localName)) nextCharHandler = sizeHandler; else if ("mime".equals(localName)) nextCharHandler = mimeHandler;
    }

    public void characters(char[] ch, int start, int end) throws SAXException {
        String s = new String(ch, start, end).trim();
        if (s.length() > 0) {
            nextCharHandler.handle(s);
            nextCharHandler = defaultCharHandler;
        }
    }

    private interface CharHandler {

        public void handle(String s);
    }

    private final CharHandler defaultCharHandler = new CharHandler() {

        public void handle(String s) {
        }
    };

    private final CharHandler sizeHandler = new CharHandler() {

        public void handle(String s) {
            try {
                size = Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                size = 0;
            }
        }
    };

    private final CharHandler mimeHandler = new CharHandler() {

        public void handle(String s) {
            mime = s;
        }
    };

    private CharHandler nextCharHandler = defaultCharHandler;

    public int getSize() {
        return size;
    }

    public String getMime() {
        return mime;
    }

    private void init() {
        size = 0;
        mime = "";
    }

    private XMLReader parser;

    private int size;

    private String mime;

    protected static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    /** Namespaces feature id (http://xml.org/sax/features/namespaces). */
    protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";

    /** Validation feature id (http://xml.org/sax/features/validation). */
    protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

    /** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";

    /** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";
}
