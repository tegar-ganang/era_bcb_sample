package HTTPClient;

import java.net.ProtocolException;
import java.io.IOException;
import java.util.Hashtable;

/**
 * This module handles the redirection status codes 301, 302, 303, 305, 306
 * and 307.
 *
 * @version	0.3-3E  06/05/2001
 * @author	Ronald Tschal�r
 */
class RedirectionModule implements HTTPClientModule {

    /** a list of permanent redirections (301) */
    private static Hashtable perm_redir_cntxt_list = new Hashtable();

    /** a list of deferred redirections (used with Response.retryRequest()) */
    private static Hashtable deferred_redir_list = new Hashtable();

    /** the level of redirection */
    private int level;

    /** the url used in the last redirection */
    private URI lastURI;

    /** used for deferred redirection retries */
    private boolean new_con;

    /** used for deferred redirection retries */
    private Request saved_req;

    /**
     * Start with level 0.
     */
    RedirectionModule() {
        level = 0;
        lastURI = null;
        saved_req = null;
    }

    /**
     * Invoked by the HTTPClient.
     */
    public int requestHandler(Request req, Response[] resp) {
        HTTPConnection con = req.getConnection();
        URI new_loc, cur_loc;
        HttpOutputStream out = req.getStream();
        if (out != null && deferred_redir_list.get(out) != null) {
            copyFrom((RedirectionModule) deferred_redir_list.remove(out));
            req.copyFrom(saved_req);
            if (new_con) return REQ_NEWCON_RST; else return REQ_RESTART;
        }
        try {
            cur_loc = new URI(new URI(con.getProtocol(), con.getHost(), con.getPort(), null), req.getRequestURI());
        } catch (ParseException pe) {
            throw new Error("HTTPClient Internal Error: unexpected exception '" + pe + "'");
        }
        Hashtable perm_redir_list = Util.getList(perm_redir_cntxt_list, req.getConnection().getContext());
        if ((new_loc = (URI) perm_redir_list.get(cur_loc)) != null) {
            String nres = new_loc.getPathAndQuery();
            req.setRequestURI(nres);
            try {
                lastURI = new URI(new_loc, nres);
            } catch (ParseException pe) {
            }
            Log.write(Log.MODS, "RdirM: matched request in permanent " + "redirection list - redoing request to " + lastURI.toExternalForm());
            if (!con.isCompatibleWith(new_loc)) {
                try {
                    con = new HTTPConnection(new_loc);
                } catch (Exception e) {
                    throw new Error("HTTPClient Internal Error: unexpected " + "exception '" + e + "'");
                }
                con.setContext(req.getConnection().getContext());
                req.setConnection(con);
                return REQ_NEWCON_RST;
            } else {
                return REQ_RESTART;
            }
        }
        return REQ_CONTINUE;
    }

    /**
     * Invoked by the HTTPClient.
     */
    public void responsePhase1Handler(Response resp, RoRequest req) throws IOException {
        int sts = resp.getStatusCode();
        if (sts < 301 || sts > 307 || sts == 304) {
            if (lastURI != null) resp.setEffectiveURI(lastURI);
        }
    }

    /**
     * Invoked by the HTTPClient.
     */
    public int responsePhase2Handler(Response resp, Request req) throws IOException {
        int sts = resp.getStatusCode();
        switch(sts) {
            case 302:
                if (req.getMethod().equals("POST") || req.getMethod().equals("PUT")) {
                    Log.write(Log.MODS, "RdirM: Received status: " + sts + " " + resp.getReasonLine() + " - treating as 303");
                    sts = 303;
                }
            case 301:
            case 303:
            case 307:
                Log.write(Log.MODS, "RdirM: Handling status: " + sts + " " + resp.getReasonLine());
                if (!req.getMethod().equals("GET") && !req.getMethod().equals("HEAD") && sts != 303) {
                    Log.write(Log.MODS, "RdirM: not redirected because " + "method is neither HEAD nor GET");
                    if (sts == 301 && resp.getHeader("Location") != null) update_perm_redir_list(req, resLocHdr(resp.getHeader("Location"), req));
                    resp.setEffectiveURI(lastURI);
                    return RSP_CONTINUE;
                }
            case 305:
            case 306:
                if (sts == 305 || sts == 306) Log.write(Log.MODS, "RdirM: Handling status: " + sts + " " + resp.getReasonLine());
                if (sts == 305 && req.getConnection().getProxyHost() != null) {
                    Log.write(Log.MODS, "RdirM: 305 ignored because " + "a proxy is already in use");
                    resp.setEffectiveURI(lastURI);
                    return RSP_CONTINUE;
                }
                if (level >= 15 || resp.getHeader("Location") == null) {
                    if (level >= 15) Log.write(Log.MODS, "RdirM: not redirected because " + "of too many levels of redirection"); else Log.write(Log.MODS, "RdirM: not redirected because " + "no Location header was present");
                    resp.setEffectiveURI(lastURI);
                    return RSP_CONTINUE;
                }
                level++;
                URI loc = resLocHdr(resp.getHeader("Location"), req);
                HTTPConnection mvd;
                String nres;
                new_con = false;
                if (sts == 305) {
                    mvd = new HTTPConnection(req.getConnection().getProtocol(), req.getConnection().getHost(), req.getConnection().getPort());
                    mvd.setCurrentProxy(loc.getHost(), loc.getPort());
                    mvd.setContext(req.getConnection().getContext());
                    new_con = true;
                    nres = req.getRequestURI();
                    req.setMethod("GET");
                    req.setData(null);
                    req.setStream(null);
                } else if (sts == 306) {
                    return RSP_CONTINUE;
                } else {
                    if (req.getConnection().isCompatibleWith(loc)) {
                        mvd = req.getConnection();
                        nres = loc.getPathAndQuery();
                    } else {
                        try {
                            mvd = new HTTPConnection(loc);
                            nres = loc.getPathAndQuery();
                        } catch (Exception e) {
                            if (req.getConnection().getProxyHost() == null || !loc.getScheme().equalsIgnoreCase("ftp")) return RSP_CONTINUE;
                            mvd = new HTTPConnection("http", req.getConnection().getProxyHost(), req.getConnection().getProxyPort());
                            mvd.setCurrentProxy(null, 0);
                            nres = loc.toExternalForm();
                        }
                        mvd.setContext(req.getConnection().getContext());
                        new_con = true;
                    }
                    if (sts == 303) {
                        if (!req.getMethod().equals("HEAD")) req.setMethod("GET");
                        req.setData(null);
                        req.setStream(null);
                    } else {
                        if (req.getStream() != null) {
                            if (!HTTPConnection.deferStreamed) {
                                Log.write(Log.MODS, "RdirM: status " + sts + " not handled - request " + "has an output stream");
                                return RSP_CONTINUE;
                            }
                            saved_req = (Request) req.clone();
                            deferred_redir_list.put(req.getStream(), this);
                            req.getStream().reset();
                            resp.setRetryRequest(true);
                        }
                        if (sts == 301) {
                            try {
                                update_perm_redir_list(req, new URI(loc, nres));
                            } catch (ParseException pe) {
                                throw new Error("HTTPClient Internal Error: " + "unexpected exception '" + pe + "'");
                            }
                        }
                    }
                    NVPair[] hdrs = req.getHeaders();
                    for (int idx = 0; idx < hdrs.length; idx++) if (hdrs[idx].getName().equalsIgnoreCase("Referer")) {
                        HTTPConnection con = req.getConnection();
                        hdrs[idx] = new NVPair("Referer", con + req.getRequestURI());
                        break;
                    }
                }
                req.setConnection(mvd);
                req.setRequestURI(nres);
                try {
                    resp.getInputStream().close();
                } catch (IOException ioe) {
                }
                if (sts != 305 && sts != 306) {
                    try {
                        lastURI = new URI(loc, nres);
                    } catch (ParseException pe) {
                    }
                    Log.write(Log.MODS, "RdirM: request redirected to " + lastURI.toExternalForm() + " using method " + req.getMethod());
                } else {
                    Log.write(Log.MODS, "RdirM: resending request using " + "proxy " + mvd.getProxyHost() + ":" + mvd.getProxyPort());
                }
                if (req.getStream() != null) return RSP_CONTINUE; else if (new_con) return RSP_NEWCON_REQ; else return RSP_REQUEST;
            default:
                return RSP_CONTINUE;
        }
    }

    /**
     * Invoked by the HTTPClient.
     */
    public void responsePhase3Handler(Response resp, RoRequest req) {
    }

    /**
     * Invoked by the HTTPClient.
     */
    public void trailerHandler(Response resp, RoRequest req) {
    }

    /**
     * Update the permanent redirection list.
     *
     * @param the original request
     * @param the new location
     */
    private static void update_perm_redir_list(RoRequest req, URI new_loc) {
        HTTPConnection con = req.getConnection();
        URI cur_loc = null;
        try {
            cur_loc = new URI(new URI(con.getProtocol(), con.getHost(), con.getPort(), null), req.getRequestURI());
        } catch (ParseException pe) {
        }
        if (!cur_loc.equals(new_loc)) {
            Hashtable perm_redir_list = Util.getList(perm_redir_cntxt_list, con.getContext());
            perm_redir_list.put(cur_loc, new_loc);
        }
    }

    /**
     * The Location header field must be an absolute URI, but too many broken
     * servers use relative URIs. So, we always resolve relative to the
     * full request URI.
     *
     * @param  loc the Location header field
     * @param  req the Request to resolve relative URI's relative to
     * @return an absolute URI corresponding to the Location header field
     * @exception ProtocolException if the Location header field is completely
     *                            unparseable
     */
    private URI resLocHdr(String loc, RoRequest req) throws ProtocolException {
        try {
            URI base = new URI(req.getConnection().getProtocol(), req.getConnection().getHost(), req.getConnection().getPort(), null);
            base = new URI(base, req.getRequestURI());
            URI res = new URI(base, loc);
            if (res.getHost() == null) throw new ProtocolException("Malformed URL in Location header: `" + loc + "' - missing host field");
            return res;
        } catch (ParseException pe) {
            throw new ProtocolException("Malformed URL in Location header: `" + loc + "' - exception was: " + pe.getMessage());
        }
    }

    private void copyFrom(RedirectionModule other) {
        this.level = other.level;
        this.lastURI = other.lastURI;
        this.saved_req = other.saved_req;
    }
}
