package org.pfyshnet.bc_codec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.pfyshnet.core.CoreCodecInterface;
import org.pfyshnet.core.NodeHello;
import org.pfyshnet.core.RouteTransfer;

public class PfyshNodeTransfer extends RouteTransfer {

    private CoreCodecInterface Core;

    private File[] Headers;

    private NodeHello[] Destinations;

    private File HeadlessData;

    private int Index;

    public PfyshNodeTransfer(File[] headers, NodeHello[] dest, File headlessdata, CoreCodecInterface core) throws IOException {
        Headers = headers;
        Destinations = dest;
        HeadlessData = headlessdata;
        Core = core;
        Index = 0;
        setPayload();
    }

    private boolean setPayload() throws IOException {
        if (Index < Headers.length) {
            FileOutputStream fos = new FileOutputStream(Headers[Index], true);
            FileInputStream fis = new FileInputStream(HeadlessData);
            FileChannel fic = fis.getChannel();
            FileChannel foc = fos.getChannel();
            fic.transferTo(0, fic.size(), foc);
            fic.close();
            foc.close();
            setDestination(Destinations[Index]);
            setPayload(Headers[Index]);
            Index++;
            return true;
        }
        return false;
    }

    public void Failure() {
        try {
            if (setPayload()) {
                Core.SendRoute(this);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void Success() {
    }
}
