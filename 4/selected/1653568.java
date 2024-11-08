package syntelos.lang;

import alto.io.Input;
import alto.io.Uri;
import alto.io.u.Chbuf;
import alto.io.u.Hex;
import alto.io.u.Url;

/**
 * Produce and consume <code>application/x-www-form-Urlencoded</code>
 * content via a query parameters map.
 */
public class HttpBodyUrlEncoded extends alto.io.u.Objmap implements alto.lang.HttpBodyUrlEncoded {

    public static final alto.lang.Type TYPE = Type.Tools.Of("www");

    protected final boolean consume;

    protected HttpRequest request;

    protected Uri uri;

    protected java.lang.String source;

    protected int countQuery, countForm, countTotal;

    /**
     * @see syntelos.sx.WwwForm
     */
    protected HttpBodyUrlEncoded(HttpRequest request, Uri uri) throws java.io.IOException {
        super();
        if (null != request && null != uri) {
            this.consume = request.isRequestServer();
            if (!this.consume) request.setContentType(Type.Tools.Of("www"));
            this.request = request;
            this.uri = uri;
            Input in = request.getInput();
            java.lang.StringBuilder source = new java.lang.StringBuilder();
            java.lang.String line;
            while (null != (line = in.readLine())) {
                source.append(line);
            }
            java.lang.String pairs[] = Chbuf.split(source.toString(), '&'), pair[], name, value;
            if (null != pairs) {
                for (int cc = 0, count = pairs.length; cc < count; cc++) {
                    pair = Chbuf.split(pairs[cc], '=');
                    if (null != pair && 2 == pair.length) {
                        name = Url.decode(pair[0]);
                        value = Url.decode(pair[1]);
                        this.put(name, value);
                    }
                }
            }
            this.source = source.toString();
            this.countQuery = uri.countQuery();
            this.countForm = this.size();
            this.countTotal = (this.countQuery + this.countForm);
        } else throw new alto.sys.Error.Argument();
    }

    /**
     */
    public HttpBodyUrlEncoded(HttpRequest request) {
        super();
        if (null != request) {
            this.consume = request.isRequestServer();
            if (!this.consume) request.setContentType(Type.Tools.Of("www"));
            this.request = request;
            this.uri = request.getUri();
        } else throw new alto.sys.Error.Argument();
    }

    public void destroy() {
        this.request = null;
        this.uri = null;
        super.clear();
    }

    public void close() throws java.io.IOException {
    }

    public final boolean isRequestClient() {
        return (!this.consume);
    }

    public final boolean isRequestServer() {
        return (this.consume);
    }

    public final java.lang.String getParameter(java.lang.String key) {
        return this.getQuery(key);
    }

    public final void setParameter(java.lang.String key, java.lang.String value) {
        if (this.consume) throw new alto.sys.Error.State(); else if (null != key && null != value) this.put(key, value); else throw new alto.sys.Error.Argument();
    }

    public final java.math.BigInteger getParameterHex(java.lang.String key) {
        java.lang.String string = this.getQuery(key);
        if (null != string) return new java.math.BigInteger(Hex.decode(string)); else return null;
    }

    public final void setParameterHex(java.lang.String key, java.math.BigInteger value) {
        if (null != key && null != value) {
            java.lang.String string = Hex.encode(value.toByteArray());
            this.put(key, string);
        }
    }

    public final java.lang.Number getParameterNumber(java.lang.String key) {
        return this.getParameterNumber(key, null);
    }

    public final java.lang.Number getParameterNumber(java.lang.String key, java.lang.Number def) {
        java.lang.String value = this.getQuery(key);
        if (null == value) return def; else return syntelos.uel.Tools.ToNumber(value);
    }

    public final boolean isRelative() {
        return this.uri.isRelative();
    }

    public final boolean isAbsolute() {
        return this.uri.isAbsolute();
    }

    public final boolean hasScheme() {
        return this.uri.hasScheme();
    }

    public final int countScheme() {
        return this.uri.countScheme();
    }

    public final java.lang.String getScheme() {
        return this.uri.getScheme();
    }

    public final java.lang.String getScheme(int idx) {
        return this.uri.getScheme(idx);
    }

    public final java.lang.String getSchemeTail() {
        return this.uri.getSchemeTail();
    }

    public final java.lang.String getSchemeHead() {
        return this.uri.getSchemeHead();
    }

    public final java.lang.String getHostUser() {
        return this.uri.getHostUser();
    }

    public final java.lang.String getHostPass() {
        return this.uri.getHostPass();
    }

    public final java.lang.String getHostName() {
        return this.uri.getHostName();
    }

    public final java.lang.String getHostPort() {
        return this.uri.getHostPort();
    }

    public final boolean hasPath() {
        return this.uri.hasPath();
    }

    public final int countPath() {
        return this.uri.countPath();
    }

    public final java.lang.String getPath() {
        return this.uri.getPath();
    }

    public final java.lang.String getPath(int idx) {
        return this.uri.getPath(idx);
    }

    public final java.lang.String getPathTail() {
        return this.uri.getPathTail();
    }

    public final java.lang.String getPathHead() {
        return this.uri.getPathHead();
    }

    public final java.lang.String getPathParent() {
        return this.uri.getPathParent();
    }

    public final java.lang.String getIntern() {
        return this.uri.getIntern();
    }

    public final boolean hasIntern() {
        return this.uri.hasIntern();
    }

    public final int countIntern() {
        return this.uri.countIntern();
    }

    public final java.lang.String getIntern(int idx) {
        return this.uri.getIntern(idx);
    }

    public final java.lang.String getInternTail() {
        return this.uri.getInternTail();
    }

    public final java.lang.String getInternHead() {
        return this.uri.getInternHead();
    }

    public final boolean hasQuery() {
        return (0 < this.countTotal);
    }

    public final boolean hasQuery(java.lang.String key) {
        return (null != this.getQuery(key));
    }

    public final java.lang.String getQuery() {
        if (this.consume) {
            if (0 < this.countQuery) {
                java.lang.String qs = this.uri.getQuery();
                java.lang.String ss = this.source;
                if (null != ss) return (qs + '&' + ss); else return qs;
            } else return this.source;
        } else {
            java.lang.StringBuilder source = this.toStringBuilder();
            source.insert(0, '?');
            return source.toString();
        }
    }

    public final int countQuery() {
        if (this.consume) return this.countTotal; else return this.size();
    }

    public final java.lang.String getQuery(int idx) {
        if (this.consume) {
            int qq = this.countQuery;
            if (idx < qq) return this.uri.getQuery(idx); else if (idx < this.countTotal) {
                idx -= qq;
                if (1 == (idx & 1)) return (java.lang.String) this.value(idx >>> 1); else return (java.lang.String) this.key(idx >>> 1);
            } else return null;
        } else if (-1 < idx && idx < this.size()) {
            if (1 == (idx & 1)) return (java.lang.String) this.value(idx >>> 1); else return (java.lang.String) this.key(idx >>> 1);
        } else return null;
    }

    public final java.lang.String[] getQueryKeys() {
        if (this.consume) {
            int qq = this.countQuery;
            if (0 < qq) {
                java.lang.String[] qv = this.uri.getQueryKeys();
                int ff = this.countForm;
                if (0 < ff) {
                    java.lang.String[] fv = (java.lang.String[]) this.keyary(alto.sys.Properties.Tools.CLASS_STRING);
                    java.lang.String[] re = new java.lang.String[this.countTotal];
                    System.arraycopy(qv, 0, re, 0, qq);
                    System.arraycopy(fv, 0, re, qq, ff);
                    return re;
                } else return qv;
            }
        }
        return (java.lang.String[]) this.keyary(alto.sys.Properties.Tools.CLASS_STRING);
    }

    public final java.lang.String[] getQueryKeysSorted() {
        java.lang.String[] keys = this.getQueryKeys();
        java.util.Arrays.sort(keys);
        return keys;
    }

    public final java.lang.String getQuery(java.lang.String key) {
        java.lang.String value = (java.lang.String) this.get(key);
        if (null == value && this.consume) return this.uri.getQuery(key); else return value;
    }

    public final void setQuery(java.lang.String key, java.lang.String value) {
        if (this.consume) throw new alto.sys.Error.State(); else if (null != key && null != value) this.put(key, value); else throw new alto.sys.Error.Argument();
    }

    public final boolean hasFragment() {
        return this.uri.hasFragment();
    }

    public final int countFragment() {
        return this.uri.countFragment();
    }

    public final java.lang.String getFragment() {
        return this.uri.getFragment();
    }

    public final java.lang.String getFragment(int idx) {
        return this.uri.getFragment(idx);
    }

    public final java.lang.String getFragmentHead() {
        return this.uri.getFragmentHead();
    }

    public final java.lang.String getFragmentTail() {
        return this.uri.getFragmentTail();
    }

    public final int countTerminal() {
        return this.uri.countTerminal();
    }

    public final boolean hasTerminal() {
        return this.uri.hasTerminal();
    }

    public final java.lang.String getTerminal() {
        return this.uri.getTerminal();
    }

    public final java.lang.String getTerminal(int idx) {
        return this.uri.getTerminal(idx);
    }

    public final java.lang.String getTerminalHead() {
        return this.uri.getTerminalHead();
    }

    public final java.lang.String getTerminalTail() {
        return this.uri.getTerminalTail();
    }

    public final java.lang.String[] getTerminalKeys() {
        return this.uri.getTerminalKeys();
    }

    public final java.lang.String getTerminalLookup(java.lang.String key) {
        return this.uri.getTerminalLookup(key);
    }

    public final Uri getUri() {
        return this.uri;
    }

    public final java.nio.channels.ReadableByteChannel openChannelReadable() throws java.io.IOException {
        return this.request.openChannelReadable();
    }

    public final java.nio.channels.ReadableByteChannel getChannelReadable() {
        return this.request.getChannelReadable();
    }

    public final long lastModified() {
        return this.request.lastModified();
    }

    public final java.lang.String lastModifiedString() {
        return this.request.lastModifiedString();
    }

    public final long getLastModified() {
        return this.request.getLastModified();
    }

    public final java.lang.String getLastModifiedString() {
        return this.request.getLastModifiedString();
    }

    public final java.io.InputStream openInputStream() throws java.io.IOException {
        return this.request.openInputStream();
    }

    public final alto.io.Input openInput() throws java.io.IOException {
        return this.request.openInput();
    }

    public final java.io.InputStream getInputStream() throws java.io.IOException {
        return this.request.getInputStream();
    }

    public final alto.io.Input getInput() throws java.io.IOException {
        return this.request.getInput();
    }

    public java.nio.channels.WritableByteChannel openChannelWritable() throws java.io.IOException {
        return null;
    }

    public java.nio.channels.WritableByteChannel getChannelWritable() {
        return null;
    }

    public boolean setLastModified(long last) {
        return false;
    }

    public java.io.OutputStream openOutputStream() throws java.io.IOException {
        return null;
    }

    public alto.io.Output openOutput() throws java.io.IOException {
        return null;
    }

    public java.io.OutputStream getOutputStream() throws java.io.IOException {
        return null;
    }

    public alto.io.Output getOutput() throws java.io.IOException {
        return null;
    }

    public final boolean hasHeader(java.lang.String name) {
        return this.request.hasHeader(name);
    }

    public final Boolean getHeaderBoolean(java.lang.String name) {
        return this.request.getHeaderBoolean(name);
    }

    public final boolean getHeaderBool(java.lang.String name) {
        return this.request.getHeaderBool(name);
    }

    public final boolean getHeaderBool(java.lang.String name, boolean defv) {
        return this.request.getHeaderBool(name, defv);
    }

    public final Number getHeaderNumber(java.lang.String name) {
        return this.request.getHeaderNumber(name);
    }

    public final Integer getHeaderInteger(java.lang.String name) {
        return this.request.getHeaderInteger(name);
    }

    public final int getHeaderInt(java.lang.String name, int defv) {
        return this.request.getHeaderInt(name, defv);
    }

    public final Float getHeaderFloat(java.lang.String name) {
        return this.request.getHeaderFloat(name);
    }

    public final float getHeaderFloat(java.lang.String name, float defv) {
        return this.request.getHeaderFloat(name, defv);
    }

    public final Long getHeaderLong(java.lang.String name) {
        return this.request.getHeaderLong(name);
    }

    public final long getHeaderLong(java.lang.String name, long defv) {
        return this.request.getHeaderLong(name, defv);
    }

    public final Object getHeaderObject(java.lang.String name) {
        return this.request.getHeaderObject(name);
    }

    public final java.lang.String getHeaderString(java.lang.String name) {
        return this.request.getHeaderString(name);
    }

    public final java.lang.String getHeaderString(java.lang.String name, java.lang.String defv) {
        return this.request.getHeaderString(name, defv);
    }

    public final java.lang.String[] getHeaderStringArray(java.lang.String name) {
        return this.request.getHeaderStringArray(name);
    }

    public final long getHeaderDate(java.lang.String name) {
        return this.request.getHeaderDate(name);
    }

    public final java.net.URL getHeaderURL(java.lang.String name) {
        return this.request.getHeaderURL(name);
    }

    public final java.lang.Class getHeaderClass(java.lang.String name) {
        return this.request.getHeaderClass(name);
    }

    public final java.lang.String toString() {
        if (this.consume || 1 > this.size()) return this.uri.toString(); else {
            java.lang.StringBuilder source = this.toStringBuilder();
            java.lang.String uri = this.uri.toString();
            source.insert(0, uri);
            source.insert(uri.length(), '&');
            return source.toString();
        }
    }

    public final java.lang.StringBuilder toStringBuilder() {
        java.lang.StringBuilder strbuf = new java.lang.StringBuilder();
        int count = this.size();
        if (0 < count) {
            for (int cc = 0; cc < count; cc++) {
                if (0 < cc) strbuf.append('&');
                java.lang.String name = Url.encode((java.lang.String) this.key(cc));
                java.lang.String value = Url.encode((java.lang.String) this.value(cc));
                strbuf.append(name);
                strbuf.append('=');
                strbuf.append(value);
            }
        }
        return strbuf;
    }
}
