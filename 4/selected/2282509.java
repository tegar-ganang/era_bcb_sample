package org.xmlsh.commands.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.xmlsh.core.InputPort;
import org.xmlsh.core.Options;
import org.xmlsh.core.OutputPort;
import org.xmlsh.core.XCommand;
import org.xmlsh.core.XValue;
import org.xmlsh.sh.shell.SerializeOpts;

public class xgunzip extends XCommand {

    public int run(List<XValue> args) throws Exception {
        Options opts = new Options("f=file:,o=out:", SerializeOpts.getOptionDefs());
        opts.parse(args);
        XValue zipfile = opts.getOptValue("f");
        args = opts.getRemainingArgs();
        SerializeOpts serializeOpts = getSerializeOpts(opts);
        InputPort iport = (zipfile == null ? getStdin() : getInput(zipfile));
        InputStream is = iport.asInputStream(serializeOpts);
        GZIPInputStream zis = new GZIPInputStream(is);
        XValue xout = opts.getOptValue("out");
        OutputPort oport = this.getOutput(xout, false);
        try {
            int ret = 0;
            ret = gunzip(zis, oport.asOutputStream(serializeOpts), args);
            zis.close();
            return ret;
        } finally {
            zis.close();
            is.close();
            iport.release();
            oport.release();
        }
    }

    private int gunzip(GZIPInputStream zis, OutputStream out, List<XValue> args) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = zis.read(buf)) > 0) out.write(buf, 0, len);
        zis.close();
        out.close();
        return 0;
    }
}
