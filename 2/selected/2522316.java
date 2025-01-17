package org.bing.adapter.com.caucho.burlap.client;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.bing.adapter.com.caucho.burlap.io.AbstractBurlapInput;
import org.bing.adapter.com.caucho.burlap.io.BurlapInput;
import org.bing.adapter.com.caucho.burlap.io.BurlapOutput;
import org.bing.adapter.com.caucho.burlap.io.BurlapRemoteObject;
import org.bing.adapter.com.caucho.burlap.io.BurlapRemoteResolver;
import org.bing.adapter.com.caucho.services.client.ServiceProxyFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;

/**
 * Factory for creating Burlap client stubs.  The returned stub will
 * call the remote object for all methods.
 *
 * <pre>
 * String url = "http://localhost:8080/ejb/hello";
 * HelloHome hello = (HelloHome) factory.create(HelloHome.class, url);
 * </pre>
 *
 * After creation, the stub can be like a regular Java class.  Because
 * it makes remote calls, it can throw more exceptions than a Java class.
 * In particular, it may throw protocol exceptions.
 *
 * The factory can also be configured as a JNDI resource.  The factory
 * expects to parameters: "type" and "url", corresponding to the two
 * arguments to <code>create</code>
 *
 * In Resin 3.0, the above example would be configured as:
 * <pre>
 * &lt;reference>
 *   &lt;name>hessian/hello&lt;/name>
 *   &lt;factory>com.caucho.hessian.client.HessianProxyFactory&lt;/factory>
 *   &lt;init url="http://localhost:8080/ejb/hello"/>
 *         type="test.HelloHome"/>
 * &lt;/reference>
 * </pre>
 *
 * To get the above resource, use JNDI as follows:
 * <pre>
 * Context ic = new InitialContext();
 * HelloHome hello = (HelloHome) ic.lookup("java:comp/env/burlap/hello");
 *
 * System.out.println("Hello: " + hello.helloWorld());
 * </pre>
 *
 * <h3>Authentication</h3>
 *
 * <p>The proxy can use HTTP basic authentication if the user and the
 * password are set.
 */
public class BurlapProxyFactory implements ServiceProxyFactory, ObjectFactory {

    private BurlapRemoteResolver _resolver;

    private String _user;

    private String _password;

    private String _basicAuth;

    private long _readTimeout;

    private boolean _isOverloadEnabled = false;

    /**
   * Creates the new proxy factory.
   */
    public BurlapProxyFactory() {
        _resolver = new BurlapProxyResolver(this);
    }

    /**
   * Sets the user.
   */
    public void setUser(String user) {
        _user = user;
        _basicAuth = null;
    }

    /**
   * Sets the password.
   */
    public void setPassword(String password) {
        _password = password;
        _basicAuth = null;
    }

    /**
   * Returns true if overloaded methods are allowed (using mangling)
   */
    public boolean isOverloadEnabled() {
        return _isOverloadEnabled;
    }

    /**
   * set true if overloaded methods are allowed (using mangling)
   */
    public void setOverloadEnabled(boolean isOverloadEnabled) {
        _isOverloadEnabled = isOverloadEnabled;
    }

    /**
   * Returns the remote resolver.
   */
    public BurlapRemoteResolver getRemoteResolver() {
        return _resolver;
    }

    /**
   * Creates the URL connection.
   */
    protected URLConnection openConnection(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        if (_basicAuth != null) conn.setRequestProperty("Authorization", _basicAuth); else if (_user != null && _password != null) {
            _basicAuth = "Basic " + base64(_user + ":" + _password);
            conn.setRequestProperty("Authorization", _basicAuth);
        }
        return conn;
    }

    /**
   * Creates a new proxy with the specified URL.  The API class uses
   * the java.api.class value from _hessian_
   *
   * @param url the URL where the client object is located.
   *
   * @return a proxy to the object with the specified interface.
   */
    public Object create(String url) throws MalformedURLException, ClassNotFoundException {
        BurlapMetaInfoAPI metaInfo;
        metaInfo = (BurlapMetaInfoAPI) create(BurlapMetaInfoAPI.class, url);
        String apiClassName = (String) metaInfo._burlap_getAttribute("java.api.class");
        if (apiClassName == null) throw new BurlapRuntimeException(url + " has an unknown api.");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class apiClass = Class.forName(apiClassName, false, loader);
        return create(apiClass, url);
    }

    /**
   * Creates a new proxy with the specified URL.  The returned object
   * is a proxy with the interface specified by api.
   *
   * <pre>
   * String url = "http://localhost:8080/ejb/hello");
   * HelloHome hello = (HelloHome) factory.create(HelloHome.class, url);
   * </pre>
   *
   * @param api the interface the proxy class needs to implement
   * @param url the URL where the client object is located.
   *
   * @return a proxy to the object with the specified interface.
   */
    public Object create(Class api, String urlName) throws MalformedURLException {
        if (api == null) throw new NullPointerException();
        URL url = new URL(urlName);
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10);
            conn.setReadTimeout(10);
            conn.setRequestProperty("Connection", "close");
            InputStream is = conn.getInputStream();
            is.close();
            conn.disconnect();
        } catch (IOException e) {
        }
        BurlapProxy handler = new BurlapProxy(this, url);
        return Proxy.newProxyInstance(api.getClassLoader(), new Class[] { api, BurlapRemoteObject.class }, handler);
    }

    public AbstractBurlapInput getBurlapInput(InputStream is) {
        AbstractBurlapInput in = new BurlapInput(is);
        in.setRemoteResolver(getRemoteResolver());
        return in;
    }

    public BurlapOutput getBurlapOutput(OutputStream os) {
        BurlapOutput out = new BurlapOutput(os);
        return out;
    }

    /**
   * JNDI object factory so the proxy can be used as a resource.
   */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference ref = (Reference) obj;
        String api = null;
        String url = null;
        String user = null;
        String password = null;
        for (int i = 0; i < ref.size(); i++) {
            RefAddr addr = ref.get(i);
            String type = addr.getType();
            String value = (String) addr.getContent();
            if (type.equals("type")) api = value; else if (type.equals("url")) url = value; else if (type.equals("user")) setUser(value); else if (type.equals("password")) setPassword(value);
        }
        if (url == null) throw new NamingException("`url' must be configured for BurlapProxyFactory.");
        if (api == null) throw new NamingException("`type' must be configured for BurlapProxyFactory.");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class apiClass = Class.forName(api, false, loader);
        return create(apiClass, url);
    }

    /**
   * Creates the Base64 value.
   */
    private String base64(String value) {
        StringBuffer cb = new StringBuffer();
        int i = 0;
        for (i = 0; i + 2 < value.length(); i += 3) {
            long chunk = (int) value.charAt(i);
            chunk = (chunk << 8) + (int) value.charAt(i + 1);
            chunk = (chunk << 8) + (int) value.charAt(i + 2);
            cb.append(encode(chunk >> 18));
            cb.append(encode(chunk >> 12));
            cb.append(encode(chunk >> 6));
            cb.append(encode(chunk));
        }
        if (i + 1 < value.length()) {
            long chunk = (int) value.charAt(i);
            chunk = (chunk << 8) + (int) value.charAt(i + 1);
            chunk <<= 8;
            cb.append(encode(chunk >> 18));
            cb.append(encode(chunk >> 12));
            cb.append(encode(chunk >> 6));
            cb.append('=');
        } else if (i < value.length()) {
            long chunk = (int) value.charAt(i);
            chunk <<= 16;
            cb.append(encode(chunk >> 18));
            cb.append(encode(chunk >> 12));
            cb.append('=');
            cb.append('=');
        }
        return cb.toString();
    }

    public static char encode(long d) {
        d &= 0x3f;
        if (d < 26) return (char) (d + 'A'); else if (d < 52) return (char) (d + 'a' - 26); else if (d < 62) return (char) (d + '0' - 52); else if (d == 62) return '+'; else return '/';
    }
}
