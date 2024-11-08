package de.grogra.imp;

import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.imageio.ImageReader;
import de.grogra.imp.objects.ImageAdapter;
import de.grogra.pf.io.FileSource;
import de.grogra.pf.io.FileWriterSource;
import de.grogra.pf.io.FilterSource;
import de.grogra.pf.io.IO;
import de.grogra.pf.io.IOFlavor;
import de.grogra.pf.io.ObjectSource;
import de.grogra.pf.io.ObjectSourceImpl;
import de.grogra.pf.ui.Workbench;
import de.grogra.util.Described;
import de.grogra.util.Map;
import de.grogra.util.MimeType;
import de.grogra.util.PPMImageReader;
import de.grogra.util.PPMReader;
import de.grogra.util.StringMap;
import de.grogra.util.Utils;
import de.grogra.xl.util.ObjectList;

public class ExternalRenderer extends Renderer implements javax.imageio.event.IIOReadUpdateListener {

    public static final FilterSource.MetaDataKey<Collection<File>> FILELIST = new FilterSource.MetaDataKey<Collection<File>>("filelist");

    protected Map params;

    protected Process process;

    protected volatile Thread thread;

    protected volatile BufferedImage image;

    protected Thread imageReaderThread;

    protected File in;

    protected File out;

    protected MimeType outMimeType;

    protected ImageReader imageReader;

    volatile IOException imageReaderException;

    ObjectList<File> files;

    public ExternalRenderer(Map params) {
        this.params = params;
    }

    @Override
    public String getName() {
        return (String) params.get(Described.NAME, null);
    }

    @Override
    public void render() throws IOException {
        in = createTempFile();
        final Workbench w = view.getWorkbench();
        w.beginStatus(this);
        w.setStatus(this, IMP.I18N.msg("renderer.exporting"), -1);
        try {
            files = new ObjectList<File>();
            StringMap meta = new StringMap(params);
            meta.putBoolean("temporary", true);
            meta.putObject(FILELIST.toString(), files);
            FilterSource fs = IO.createPipeline(new ObjectSourceImpl(view, "view", view.getFlavor(), w.getRegistry(), meta), new IOFlavor(getMimeType(), IOFlavor.FILE_WRITER, null));
            if (fs == null) {
                throw new IOException(getMimeType() + " not exportable");
            }
            ((FileWriterSource) fs).write(in);
            out = createOutFile();
            files.push(in).push(out);
            outMimeType = getOutputMimeType();
            w.beginStatus(this);
            process = startProcess();
            if (out == null) {
                try {
                    BufferedInputStream in = new BufferedInputStream(process.getInputStream());
                    if (outMimeType.equals(PPMReader.MIME_TYPE)) {
                        imageReader = new PPMImageReader(30);
                        imageReader.setInput(javax.imageio.ImageIO.createImageInputStream(in), true);
                    } else {
                        imageReader = de.grogra.imp.io.ImageReader.createImageIOReader(in, outMimeType);
                    }
                } catch (IOException e) {
                    deleteFiles(files);
                    process.destroy();
                    throw e;
                }
                imageReader.addIIOReadUpdateListener(this);
                imageReaderThread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            image = imageReader.read(0, imageReader.getDefaultReadParam());
                        } catch (IOException e) {
                            if (!Thread.interrupted()) {
                                imageReaderException = e;
                            }
                        } catch (RuntimeException e) {
                            if (!Thread.interrupted()) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                imageReaderThread.setPriority(Thread.MIN_PRIORITY);
            }
            thread = new Thread() {

                @Override
                public void run() {
                    try {
                        watchProcess();
                    } catch (IOException e) {
                        e.printStackTrace();
                        imageUpdate(image, ImageObserver.ABORT | ImageObserver.ERROR, 0, 0, 0, 0);
                    } finally {
                        deleteFiles(files);
                    }
                    w.clearStatusAndProgress(ExternalRenderer.this);
                    thread = null;
                }
            };
            thread.start();
        } finally {
            if (thread == null) {
                w.clearStatusAndProgress(this);
            }
        }
    }

    static void deleteFiles(ObjectList<File> files) {
        while (!files.isEmpty()) {
            File f = files.pop();
            if (f != null) {
                f.delete();
            }
        }
    }

    protected File createTempFile() throws IOException {
        File f = File.createTempFile("renderer", null);
        f.deleteOnExit();
        return f;
    }

    protected File createOutFile() throws IOException {
        if (useStdOut()) {
            return null;
        }
        File f = File.createTempFile("renderer", ".out");
        f.deleteOnExit();
        return f;
    }

    protected boolean useStdOut() {
        return Boolean.TRUE.equals(params.get("stdout", null));
    }

    private static void copyErr(InputStream err, byte[] buf) throws IOException {
        int av;
        while ((av = err.available()) > 0) {
            System.err.write(buf, 0, err.read(buf, 0, Math.min(av, buf.length)));
        }
    }

    protected void watchProcess() throws IOException {
        Process p = process;
        if (p == null) {
            return;
        }
        Workbench w = view.getWorkbench();
        InputStream err = p.getErrorStream();
        byte[] buf = new byte[1024];
        int n = 0;
        boolean startIRThread = out == null;
        while (thread != null) {
            if (imageReaderException != null) {
                throw imageReaderException;
            }
            if (startIRThread && (p.getInputStream().available() > 0)) {
                startIRThread = false;
                Thread t = imageReaderThread;
                if (t != null) {
                    t.start();
                }
                w.setStatus(this, IMP.I18N.msg("renderer.running.stdout"), -1);
            }
            copyErr(err, buf);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
            if (((++n & 7) == 0) && (out != null) && out.exists() && (out.length() > 0)) {
                w.setStatus(this, IMP.I18N.msg("renderer.running.file", new Long(out.length())), -1);
            }
            try {
                copyErr(err, buf);
                if (p.exitValue() != 0) {
                    throw new IOException("Exit value " + p.exitValue());
                }
                if (out != null) {
                    FilterSource fs = IO.createPipeline(new FileSource(out, outMimeType, w.getRegistry(), null), de.grogra.imp.io.ImageReader.FLAVOR);
                    if (!(fs instanceof ObjectSource)) {
                        throw new IOException("Cannot read output file");
                    }
                    image = ((ImageAdapter) ((ObjectSource) fs).getObject()).getBufferedImage();
                }
                if (image != null) {
                    imageUpdate(image, ImageObserver.ALLBITS, 0, 0, image.getWidth(), image.getHeight());
                }
                return;
            } catch (IllegalThreadStateException e) {
            }
        }
    }

    protected MimeType getMimeType() {
        return new MimeType((String) params.get("mimein", null), null);
    }

    protected MimeType getOutputMimeType() {
        return new MimeType((String) params.get("mimeout", null), null);
    }

    protected Process startProcess() throws IOException {
        StringMap map = new StringMap(params);
        map.putObject("in", in.getAbsolutePath()).putObject("out", (out == null) ? "-" : out.getAbsolutePath()).putObject("dir", in.getParentFile().getAbsolutePath()).putInt("width", width).putInt("height", height).putObject("mimeout", outMimeType.getMediaType());
        String cmd = Utils.eval((String) params.get("command", null), map);
        view.getWorkbench().setStatus(this, IMP.I18N.msg("renderer.starting", cmd), -1);
        return Runtime.getRuntime().exec(cmd, null, in.getParentFile());
    }

    public synchronized void dispose() {
        Thread t = thread;
        if (t != null) {
            thread = null;
            t.interrupt();
        }
        t = imageReaderThread;
        if (t != null) {
            imageReaderThread = null;
            t.interrupt();
        }
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    public void passComplete(ImageReader source, BufferedImage theImage) {
    }

    public void thumbnailPassComplete(ImageReader source, BufferedImage theThumbnail) {
    }

    public void passStarted(ImageReader source, BufferedImage theImage, int pass, int minPass, int maxPass, int minX, int minY, int periodX, int periodY, int[] bands) {
    }

    public void thumbnailPassStarted(ImageReader source, BufferedImage theThumbnail, int pass, int minPass, int maxPass, int minX, int minY, int periodX, int periodY, int[] bands) {
    }

    public void imageUpdate(ImageReader source, BufferedImage theImage, int minX, int minY, int width, int height, int periodX, int periodY, int[] bands) {
        imageUpdate(theImage, ImageObserver.SOMEBITS, minX, minY, width, height);
    }

    public void thumbnailUpdate(ImageReader source, BufferedImage theThumbnail, int minX, int minY, int width, int height, int periodX, int periodY, int[] bands) {
    }
}
