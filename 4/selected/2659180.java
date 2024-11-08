package net.sf.fir4j.generator;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataController;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import net.sf.fir4j.Main;
import net.sf.fir4j.Messages;
import net.sf.fir4j.dialog.SelectionDialog;
import net.sf.fir4j.options.Options;
import net.sf.fir4j.view.Message;

/**
 * This class do the main work and resize the images.
 */
public class Generator {

    Messages mes = Messages.getInstance();

    private Options o = Options.getInstance();

    private Message log = Message.getInstance();

    public final String fs = System.getProperty("file.separator");

    public final String ls = System.getProperty("line.separator");

    Main m;

    /**
	 * @param m
	 *          a reference to the Main Class.
	 */
    public Generator(Main m) {
        super();
        this.m = m;
    }

    /**
	 * @param zipFileName
	 *          File, the Name of the new ZIP-File
	 * @param selected
	 *          Vector, the Images for the ZIP-File
	 */
    public void createZip(File zipFileName, Vector<File> selected) {
        try {
            byte[] buffer = new byte[4096];
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFileName), 8096));
            out.setLevel(Deflater.BEST_COMPRESSION);
            out.setMethod(ZipOutputStream.DEFLATED);
            for (int i = 0; i < selected.size(); i++) {
                FileInputStream in = new FileInputStream(selected.get(i));
                String file = selected.get(i).getPath();
                if (file.indexOf("\\") != -1) file = file.substring(file.lastIndexOf(fs) + 1, file.length());
                ZipEntry ze = new ZipEntry(file);
                out.putNextEntry(ze);
                int len;
                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                out.closeEntry();
                in.close();
                selected.get(i).delete();
            }
            out.close();
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private ProgressMonitor progressMonitor;

    public void generateZip() {
        JFileChooser fo = new JFileChooser();
        fo.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".zip");
            }

            public String getDescription() {
                return "*.zip";
            }
        });
        fo.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fo.setCurrentDirectory(new File(o.getOutput_dir()));
        int returnVal = fo.showSaveDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;
        o.setOutput_dir(fo.getSelectedFile().getParent());
        o.saveOptions();
        File zipFile = fo.getSelectedFile();
        if (!zipFile.getName().endsWith(".zip")) zipFile = new File(zipFile.getParentFile(), zipFile.getName() + ".zip");
        generate(true, zipFile);
    }

    public void generate() {
        JFileChooser fo = new JFileChooser();
        fo.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory();
            }

            public String getDescription() {
                return mes.getString("Generator.folders");
            }
        });
        fo.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fo.setCurrentDirectory(new File(o.getOutput_dir()));
        int returnVal = fo.showSaveDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;
        File path = fo.getSelectedFile();
        o.setOutput_dir(path.getPath());
        o.saveOptions();
        generate(false, path);
    }

    public void generate(final boolean zippen, File f) {
        File path;
        File zipFile = null;
        if (zippen) {
            zipFile = f;
            path = new File(".temp");
            path.mkdirs();
            path.deleteOnExit();
        } else {
            path = f;
        }
        File[] dir = new File[0];
        final Vector<File> selected = m.list.getSelectedValues();
        if (selected.size() == 0 || selected.size() == m.list.getPictures().length) {
            dir = m.list.getPictures();
        } else {
            SelectionDialog selectionDialog = new SelectionDialog(m);
            selectionDialog.setTitle(mes.getString("SelectDialog.title"));
            selectionDialog.setSelectedCount(selected.size());
            selectionDialog.setAllCount(m.list.getPictures().length);
            selectionDialog.setVisible(true);
            SelectionDialog.Result response = selectionDialog.getResult();
            switch(response) {
                case SELECTED:
                    Vector<File> vf = m.list.getSelectedValues();
                    dir = new File[vf.size()];
                    for (int i = 0; i < dir.length; i++) dir[i] = vf.get(i);
                    break;
                case ALL:
                    dir = m.list.getPictures();
                    break;
                default:
                    return;
            }
        }
        final File thePath = path;
        final File files[] = dir;
        final File theZip = zipFile;
        Thread t = new Thread() {

            public void run() {
                Vector<File> result = generate(thePath, files);
                if (zippen && theZip != null) {
                    progressMonitor = new ProgressMonitor(m, "Zip...", mes.getString("Generator.createZip"), 0, result.size());
                    progressMonitor.setMillisToPopup(0);
                    progressMonitor.setMillisToDecideToPopup(0);
                    createZip(theZip, result);
                    log.insert(ls + mes.getString("Generator.45", result.size(), theZip) + ls);
                    progressMonitor.close();
                } else {
                    log.insert(ls + mes.getString("Generator.44", result.size(), thePath) + ls);
                }
                m.status.setStatusOff();
            }
        };
        t.start();
    }

    /**
	 * <p>
	 * rotate the Image and write it to the File
	 * </p>
	 *
	 * @param file
	 *          File
	 */
    public void rotate(File file) {
        BufferedImage i = null;
        IIOMetadata imeta = null;
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            ImageReader reader = ImageIO.getImageReadersByFormatName("jpg").next();
            reader.setInput(iis, true);
            ImageReadParam params = reader.getDefaultReadParam();
            i = reader.read(0, params);
            imeta = reader.getImageMetadata(0);
        } catch (IOException e) {
            System.err.println("Error while reading File: " + file.getAbsolutePath());
            e.printStackTrace();
            return;
        }
        try {
            ImageGenerator.rotateImage(i, 90);
            System.out.println("Speichere Bild:" + file.getAbsolutePath());
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            writer.setOutput(new MemoryCacheImageOutputStream(new FileOutputStream(file)));
            ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
            iwparam.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
            IIOMetadata meta_convert = writer.convertImageMetadata(imeta, new ImageTypeSpecifier(i), iwparam);
            IIOMetadataController imc = meta_convert.getController();
            imc.activate(meta_convert);
            imeta = writer.convertImageMetadata(imeta, new ImageTypeSpecifier(i), iwparam);
            writer.write(meta_convert, new IIOImage(i, null, null), iwparam);
            writer.dispose();
            System.out.println("Bild gespeichert!");
        } catch (Exception l) {
            l.printStackTrace();
            log.insert(mes.getString("Generator.42"));
        }
    }

    /**
	 * <p>
	 * Resize a single image
	 * </p>
	 *
	 * @param f
	 *          File, input Image
	 */
    public void generateSingle() {
        File f = m.list.getPicture();
        if (f == null) {
            JOptionPane.showMessageDialog(null, "Generator.noFileSelected");
            return;
        }
        JFileChooser fo = new JFileChooser();
        fo.setDialogTitle(mes.getString("Generator.15"));
        fo.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg");
            }

            public String getDescription() {
                return "JPEG";
            }
        });
        fo.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fo.setSelectedFile(f);
        int returnVal = fo.showSaveDialog(m);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (f.compareTo(fo.getSelectedFile()) == 0 || fo.getSelectedFile().exists()) {
                if (JOptionPane.showConfirmDialog(m, mes.getString("Generator.confirmOverwrite")) != JOptionPane.OK_OPTION) {
                    log.insertLine(mes.getString("Generator.aborted"));
                    return;
                }
            }
            m.status.setStatusOn();
            try {
                new ImageGenerator(o).generateImage(fo.getSelectedFile(), f, ImageIO.read(f));
            } catch (Exception e) {
                e.printStackTrace();
            }
            m.status.setStatusOff();
            log.insertLine(mes.getString("Generator.19"));
        }
    }

    /**
	 * <p>
	 * Resize the Images without the GUI, when the Programm is started with
	 * Arguments
	 * </p>
	 *
	 * @param input
	 *          File, the Input Directory
	 * @param output
	 *          File, the Output Directory
	 * @param width
	 *          int, width of the scaled image
	 * @param height
	 *          int, heigth of the scaled image
	 */
    public void generateText(File input, File output, int width, int height) {
        ImageGenerator imageGenerator = new ImageGenerator(o);
        if (input.isDirectory() && output.isDirectory()) try {
            File[] dir = input.listFiles();
            Vector<File> v = new Vector<File>();
            for (int i = 0; i < dir.length; i++) try {
                String end = dir[i].toString().substring(dir[i].toString().lastIndexOf(".") + 1, dir[i].toString().length());
                if (dir[i].isFile() && (end.equalsIgnoreCase("jpg") || end.equalsIgnoreCase("jpeg"))) v.addElement(dir[i]);
            } catch (Exception st) {
                st.printStackTrace();
            }
            System.out.println(mes.getString("Generator.28", v.size(), input.toString(), Integer.toString((int) (o.getQuality() * 100))) + ls + ls);
            for (int i = 0; i < v.size(); i++) {
                System.out.print(mes.getString("Generator.10") + v.elementAt(i).getName() + "\t . . . ");
                try {
                    imageGenerator.generateImage(output, v.elementAt(i), ImageIO.read(v.elementAt(i)));
                    System.out.println(mes.getString("Generator.12"));
                } catch (Exception e) {
                    System.out.println("error");
                    e.printStackTrace();
                }
            }
            System.out.println(ls + mes.getString("Generator.44", v.size(), output) + ls);
        } catch (Exception ex) {
            System.out.println(ex);
        } else if (input.isFile()) {
            try {
                imageGenerator.generateImage(output, input, ImageIO.read(input));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Vector<File> generate(final File path, File source[]) {
        final ConcurrentLinkedQueue<File> queue = new ConcurrentLinkedQueue<File>();
        for (File f : source) queue.add(f);
        final ImageGenerator imageGenerator = new ImageGenerator(o);
        try {
            final Vector<File> outFiles = new Vector<File>();
            class Producer implements Runnable {

                private File outputdir;

                Producer(File theOutputDir) {
                    this.outputdir = theOutputDir;
                }

                public void run() {
                    try {
                        File file;
                        while ((file = queue.poll()) != null) {
                            if (progressMonitor.isCanceled()) break;
                            final String currentFile = file.getName();
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    progressMonitor.setNote(mes.getString("Generator.currentImage") + ": " + currentFile);
                                }
                            });
                            BufferedImage image = ImageIO.read(file);
                            boolean error = false;
                            File out;
                            try {
                                out = imageGenerator.generateImage(outputdir, file, image);
                                outFiles.addElement(out);
                            } catch (Exception e) {
                                error = true;
                                e.printStackTrace();
                            }
                            StringBuilder message = new StringBuilder();
                            message.append(mes.getString("Generator.10"));
                            message.append(file.getName());
                            message.append("\t . . . ");
                            if (error == false) message.append(mes.getString("Generator.40")); else message.append(mes.getString("Generator.42"));
                            log.insertLine(message.toString());
                            incrementIndex();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                int index = 0;

                public synchronized void incrementIndex() {
                    progressMonitor.setProgress(++index);
                }
            }
            if (source.length == 0) return new Vector<File>();
            m.status.setStatusOn();
            String quality = Integer.toString((int) (o.getQuality() * 100));
            String info = mes.getString("Generator.28", source.length, source[0].getParentFile().getName(), quality);
            progressMonitor = new ProgressMonitor(m, info, mes.getString("Generator.10"), 0, source.length);
            progressMonitor.setMillisToPopup(0);
            progressMonitor.setMillisToDecideToPopup(0);
            log.insertLine("");
            Producer producer = new Producer(path);
            Thread producerThread1 = new Thread(producer);
            Thread producerThread2 = new Thread(producer);
            log.insert(ls + ls + info + ls);
            long start = System.currentTimeMillis();
            producerThread1.start();
            producerThread2.start();
            try {
                producerThread1.join();
                producerThread2.join();
            } catch (InterruptedException ignore) {
            }
            System.out.println("duration: " + (System.currentTimeMillis() - start) + " millis");
            return outFiles;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new Vector<File>();
    }
}
