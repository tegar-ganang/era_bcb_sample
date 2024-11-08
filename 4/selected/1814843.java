package de.ddb.conversion.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jcraft.jsch.JSchException;
import de.ddb.charset.PicaCharset;
import de.ddb.conversion.ConverterException;
import de.ddb.conversion.ssh.RemoteExec;
import junit.framework.TestCase;

public class RemoteExecTest extends TestCase {

    private static final Log logger = LogFactory.getLog(RemoteExecTest.class);

    public void testRemoteExec() throws IOException {
        String command = ". .profile; /pica/tolk/bin/csfn_pica32norm -y | " + "/pica/tolk/bin/csfn_fcvnorm -k" + "FCV#pica#mab-ohne077 -t ALPHA |" + "/pica/tolk/bin/ddb_denorm -f mab-exchange |" + "/pica/tolk/bin/ddbflattenrecs -f";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("LD_LIBRARY_PATH", "/pica/sybase/lib");
        properties.put("FILEMAP", "/pica/tolk/confdir/FILEMAP");
        RemoteExec remoteExec = new RemoteExec();
        remoteExec.setCommand(command);
        remoteExec.setHost("merkur.d-nb.de");
        remoteExec.setUser("tolk");
        remoteExec.setPassword("hads%szl");
        remoteExec.setEnvironmentProperties(properties);
        InputStream in = new FileInputStream("test/input/02499250X.pp");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int i;
        while ((i = in.read()) != -1) {
            buffer.write(i);
        }
        byte[] ret = remoteExec.remoteExec(buffer.toByteArray());
        logger.info("output: " + new String(ret));
    }

    public void testRemoteExec2() throws IOException, JSchException, ConverterException {
        String command = ". .profile; /pica/tolk/bin/csfn_pica32norm -y | " + "/pica/tolk/bin/csfn_fcvnorm -k" + "FCV#pica#mab-ohne077 -t ALPHA |" + "/pica/tolk/bin/ddb_denorm -f mab-exchange |" + "/pica/tolk/bin/ddbflattenrecs -f";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("LD_LIBRARY_PATH", "/pica/sybase/lib");
        properties.put("FILEMAP", "/pica/tolk/confdir/FILEMAP");
        RemoteExec remoteExec = new RemoteExec();
        remoteExec.setCommand(command);
        remoteExec.setHost("merkur.d-nb.de");
        remoteExec.setUser("tolk");
        remoteExec.setPassword("hads%szl");
        remoteExec.setEnvironmentProperties(properties);
        StringWriter writer = new StringWriter();
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream("test/input/02499250X.pp"), new PicaCharset()));
        remoteExec.remoteExec(reader, writer, new PicaCharset(), Charset.forName("UTF-8"), null, null);
        logger.info("output2: " + writer.toString());
    }
}
