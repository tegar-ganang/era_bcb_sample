package org.xmlsh.commands.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.xmlsh.core.InputPort;
import org.xmlsh.core.Options;
import org.xmlsh.core.XCommand;
import org.xmlsh.core.XValue;
import org.xmlsh.sh.shell.SerializeOpts;

public class xgzip extends XCommand {

    public int run(List<XValue> args) throws Exception {
        Options opts = new Options("f=file:", SerializeOpts.getOptionDefs());
        opts.parse(args);
        XValue zipfile = opts.getOptValue("f");
        args = opts.getRemainingArgs();
        SerializeOpts serializeOpts = getSerializeOpts(opts);
        GZIPOutputStream zos = new GZIPOutputStream(zipfile == null ? getStdout().asOutputStream(serializeOpts) : this.getOutputStream(zipfile.toString(), false, serializeOpts));
        XValue xin = args.size() > 0 ? args.get(0) : null;
        InputPort iport = this.getInput(xin);
        try {
            int ret = 0;
            ret = gzip(iport.asInputStream(serializeOpts), zos);
            zos.finish();
            return ret;
        } finally {
            zos.close();
            iport.release();
        }
    }

    private int gzip(InputStream is, GZIPOutputStream zos) throws IOException {
        int ret = 0;
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) zos.write(buf, 0, len);
        is.close();
        zos.close();
        return ret;
    }
}
