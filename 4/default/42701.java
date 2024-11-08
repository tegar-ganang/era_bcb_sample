import java.io.*;
import java.util.*;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.attachments.AttachmentPart;

/**
<h4>gdsflybase.java</h4>  

Genome Directory System SOAP client via LuceGene backend <p>

Simple test SOAP access to flybase data via LuceGene.
This Genome Directory Services Interface is part of LuceGene package,
http://www.gmod.org/lucegene/ as org.eugenes.services.Directory

NOTE: the session id, sid parameter DID disappear (http cookies handle it)
See also  gdsflybase.pl and web-face at  http://cricket.bio.indiana.edu/lucegene/
  
june 2004, dgg; updated dec2004

NOTES: 
# -- set up class path with axis client jars --
set tc=$TOMCAT_HOME
set tlibs=(common/endorsed common/lib shared/lib)
# -- need these
set axlibs=(axis.jar jax-qname.jar jaxrpc.jar xalan.jar activation.jar \
mail.jar saaj.jar  wsdl4j.jar commons-discovery.jar commons-logging.jar \
log4j.jar)

set ax=""
foreach l ($tlibs)
  foreach j ($axlibs)
     set jf="${tc}/$l/$j"
     if (-f $jf) then 
       set ax="${ax}:$jf"     
     endif
  end
end

Compile with 'javac -classpath $ax gdsflybase.java'
Run with 'java -cp $ax gdsflybase'
<p>

* @author Don Gilbert
* @version 1.0 05/31/2004 
*/
public class gdsflybase {

    static String url = "http://flybase.net/ws/services/Directory";

    static boolean debug = true, doxmlstream = true;

    static Integer timelimit = new Integer(10 * 60 * 1000);

    public static void main(String[] args) {
        if (args.length > 0 && args[0].startsWith("http://")) url = args[0];
        try {
            new gdsflybase().test(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Call serviceCall;

    public void test(String url) throws Exception {
        header("Genome Directory System-FlyBase");
        out("Note: to see WebServices definition or create client classes, use\n " + url + "?wsdl\n");
        serviceCall = new Call(url);
        serviceCall.setOperationStyle("rpc");
        serviceCall.setMaintainSession(true);
        if (timelimit != null) serviceCall.setTimeout(timelimit);
        printres(invoke("sessionid"));
        header("DIRECTORY of FlyBase data libraries");
        printres(invoke("directory"));
        header("LOOKUP(lib,id) in FlyBase Genes");
        printres(invoke("lookup", new Object[] { "fbgn", "FBgn0000014" }));
        header("LOOKUP(lib,id) in FlyBase Unified Gene XML");
        printres(invoke("setformat", new Object[] { "native" }));
        printres(invoke("lookup", new Object[] { "ugpxml", "FlyBase?FBgn0000014" }));
        printres(invoke("setformat", new Object[] { "xml" }));
        header("LIBRARY(lib) info for FlyBase Gene Annotations");
        printres(invoke("library", new Object[] { "fban" }));
        header("SEARCH in FlyBase Gene Annotations (fban)");
        printres(invoke("search", new Object[] { "all:kinase" }));
        Object count = invoke("count");
        printres(count);
        int max = Math.min(10, ((Integer) count).intValue());
        printres(invoke("formats"));
        printres(invoke("fields"));
        printres(invoke("setpage", new Object[] { new Integer(0), new Integer(max) }));
        printres(invoke("setformat", new Object[] { "table" }));
        printres(invoke("nextpage"));
        header("NEXTPAGE as ATTACHEMENT");
        printres(invoke("attachpage"));
        if (hasAttachments()) printAttachments();
        header("LOOKUPLIST(lib,ids) in FlyBase Gene Annotations");
        String[] flds = new String[] { "docid", "SYM", "GSYM", "SCAF", "ARM", "BLOC.start", "BLOC.stop" };
        printres(invoke("setfields", new Object[] { flds }));
        String[] ids = new String[] { "FBgn0052484", "FBgn0014006", "FBgn0004624", "FBgn0051140", "FBgn0030300" };
        printres(invoke("lookuplist", new Object[] { "fban", ids }));
        printres(invoke("close"));
    }

    PrintStream out = System.out, err = System.err;

    void debug(String msg) {
        if (debug) err.println(msg);
    }

    void err(String msg) {
        err.println(msg);
    }

    void out(String msg) {
        out.println(msg);
    }

    void header(String msg) {
        out.println();
        out.println(msg);
        out.println("============================================================");
    }

    private void outparam(Object[] p) {
        out.print("(");
        if (p != null) for (int i = 0; i < p.length; i++) out.print(p[i] + ", ");
        out.print(") ");
    }

    private Object invoke(String method) {
        return invoke(method, null);
    }

    private Object invoke(String method, Object[] params) {
        try {
            out.print("# " + method);
            outparam(params);
            out.println("= ");
            serviceCall.setOperationName(method);
            Object resp = serviceCall.invoke(params);
            extractAttachments(serviceCall);
            return resp;
        } catch (Exception e) {
            err(">> invoke err=" + e.toString());
            return null;
        }
    }

    boolean printres(Object res) {
        if (res == null) return false; else if (res instanceof Object[]) {
            Object[] ss = (Object[]) res;
            if (ss.length == 0) return false;
            for (int i = 0; i < ss.length; i++) {
                out.print(ss[i]);
                out.print(" ");
            }
        } else if (res instanceof String) {
            out.print(res);
        } else if (res instanceof Integer) {
            out.print(res);
        } else if (res instanceof Boolean) {
            out.print(res);
        } else if (res instanceof AttachmentPart) {
            processAttachment(res);
        } else {
            debug("# result content=" + res.getClass().getName());
            out.print(res);
        }
        out.println();
        return true;
    }

    private final int pipesize = 1024;

    private byte[] pipe = null;

    private Vector attachments = new Vector();

    boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    void extractAttachments(Call call) {
        attachments.clear();
        int nat = call.getResponseMessage().countAttachments();
        if (nat > 0) {
            debug("# extractAttachments " + nat);
            Iterator iterator = call.getResponseMessage().getAttachments();
            while (iterator.hasNext()) attachments.add(iterator.next());
        }
    }

    boolean printAttachments() {
        boolean ok = true;
        for (int i = 0; i < attachments.size(); i++) {
            ok = ok && processAttachment(attachments.elementAt(i));
        }
        attachments.clear();
        return ok;
    }

    boolean processAttachment(Object res) {
        AttachmentPart at = null;
        if (res == null) return false; else if (res instanceof AttachmentPart) try {
            boolean ok = true;
            at = (AttachmentPart) res;
            attachments.remove(at);
            Object cont = null;
            try {
                javax.activation.DataHandler dh = at.getDataHandler();
                javax.activation.DataSource ds = dh.getDataSource();
                InputStream is = ds.getInputStream();
                if (doxmlstream && "text/xml".equals(ds.getContentType())) cont = new javax.xml.transform.stream.StreamSource(is); else cont = is;
            } catch (Exception dx) {
                if (debug) dx.printStackTrace();
                ok = false;
            }
            if (cont instanceof String) {
                if (((String) cont).length() == 0) ok = false;
                out.println(cont);
            } else if (cont instanceof javax.xml.transform.Source) {
                processXmlSource((javax.xml.transform.Source) cont);
            } else if (cont instanceof java.io.InputStream) {
                java.io.InputStream ins = (java.io.InputStream) cont;
                if (pipe == null) pipe = new byte[pipesize];
                int len;
                while ((len = ins.read(pipe)) >= 0) out.write(pipe, 0, len);
                out.flush();
                ins.close();
            } else {
                if (cont != null) debug("# attach content=" + cont.getClass().getName());
                out.println(cont);
            }
            at.dispose();
            return ok;
        } catch (Exception e) {
            err(">> attach err=" + e);
            if (at != null) at.dispose();
            return false;
        } else {
            out.println(res);
            return true;
        }
    }

    void processXmlSource(javax.xml.transform.Source xsrc) {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.transform(xsrc, new StreamResult(out));
            out.flush();
        } catch (Exception xe) {
            err(">> processXmlSource error=" + xe);
        }
    }
}
