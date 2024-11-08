package visad.data.bio;

import java.awt.Image;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.RemoteException;
import java.net.URL;
import java.util.Hashtable;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import loci.formats.*;
import loci.formats.gui.GUITools;
import visad.*;
import visad.data.*;
import visad.java2d.DisplayImplJ2D;
import visad.util.*;

/**
 * LociForm is the VisAD data adapter for images handled by the Bio-Formats
 * package (loci.formats). It works by wrapping a loci.formats.IFormatReader
 * and/or loci.formats.IFormatWriter object.
 */
public class LociForm extends Form implements FormBlockReader, FormFileInformer, FormProgressInformer, MetadataReader {

    private static int formCount = 0;

    /** Reader to use for open-related functions. */
    protected IFormatReader reader;

    /** Writer to use for save-related functions. */
    protected IFormatWriter writer;

    /** Percent complete for current operation. */
    protected double percent;

    /** File filters for reader formats. */
    protected FileFilter[] rFilters;

    /** File filters for writer formats. */
    protected FileFilter[] wFilters;

    /** Constructs a new LociForm that handles anything from loci.formats. */
    public LociForm() {
        this(new ImageReader(), new ImageWriter());
    }

    /** Constructs a new LociForm that handles the given reader. */
    public LociForm(IFormatReader reader) {
        this(reader, null);
    }

    /** Constructs a new LociForm that handles the given writer. */
    public LociForm(IFormatWriter writer) {
        this(null, writer);
    }

    /** Constructs a new LociForm that handles the given reader/writer pair. */
    public LociForm(IFormatReader reader, IFormatWriter writer) {
        super("LociForm" + formCount++);
        this.reader = reader;
        this.writer = writer;
    }

    /** Gets the IFormatReader backing this form's reading capabilities. */
    public IFormatReader getReader() {
        return reader;
    }

    /** Gets the IFormatWriter backing this form's writing capabilities. */
    public IFormatWriter getWriter() {
        return writer;
    }

    /** Sets the frames per second to use when writing files. */
    public void setFrameRate(int fps) {
        if (writer == null) return;
        writer.setFramesPerSecond(fps);
    }

    /**
   * A utility method for test reading a file from the command line,
   * and displaying the results in a simple display.
   */
    public void testRead(String[] args) throws VisADException, IOException {
        if (reader == null) return;
        String className = getClass().getName();
        String format = reader.getFormat();
        if (args == null || args.length < 1) {
            System.out.println("To test read a file in " + format + " format, run:");
            System.out.println("  java " + className + " in_file");
            return;
        }
        String id = args[0];
        System.out.print("Checking " + format + " format ");
        System.out.println(isThisType(id) ? "[yes]" : "[no]");
        System.out.print("Reading " + id + " pixel data ");
        Data data = open(args[0]);
        System.out.println("[done]");
        System.out.println("MathType =\n" + data.getType());
        FunctionType ftype = (FunctionType) data.getType();
        RealTupleType domain = ftype.getDomain();
        RealType[] xy = domain.getRealComponents();
        RealType time = null;
        if (xy.length == 1) {
            time = xy[0];
            ftype = (FunctionType) ftype.getRange();
            domain = ftype.getDomain();
            xy = domain.getRealComponents();
        }
        MathType range = ftype.getRange();
        RealType[] values = range instanceof RealType ? new RealType[] { (RealType) range } : ((RealTupleType) range).getRealComponents();
        DisplayImpl display = new DisplayImplJ2D("display");
        ScalarMap timeMap = null;
        if (time != null) {
            timeMap = new ScalarMap(time, Display.Animation);
            display.addMap(timeMap);
        }
        display.addMap(new ScalarMap(xy[0], Display.XAxis));
        display.addMap(new ScalarMap(xy[1], Display.YAxis));
        ScalarMap colorMap = null;
        if (values.length == 2 || values.length == 3) {
            display.addMap(new ScalarMap(values[0], Display.Red));
            display.addMap(new ScalarMap(values[1], Display.Green));
            if (values.length == 3) {
                display.addMap(new ScalarMap(values[2], Display.Blue));
            }
        } else {
            colorMap = new ScalarMap(values[0], Display.RGB);
            display.addMap(colorMap);
        }
        DataReferenceImpl ref = new DataReferenceImpl("ref");
        ref.setData(data);
        display.addReference(ref);
        display.getGraphicsModeControl().setScaleEnable(true);
        JFrame frame = new JFrame(format + " Results");
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        JPanel p = new JPanel();
        frame.setContentPane(p);
        p.setLayout(new BorderLayout());
        p.add(display.getComponent());
        if (timeMap != null) {
            AnimationWidget aw = new AnimationWidget(timeMap);
            p.add(BorderLayout.SOUTH, aw);
        }
        if (colorMap != null) {
            RangeWidget rw = new RangeWidget(colorMap);
            LabeledColorWidget lcw = new LabeledColorWidget(colorMap);
            p.add(BorderLayout.NORTH, rw);
            p.add(BorderLayout.EAST, lcw);
        }
        frame.pack();
        frame.setLocation(300, 300);
        frame.setVisible(true);
    }

    /** Gets file filters for use with formats supported for reading. */
    public FileFilter[] getReaderFilters() {
        if (reader == null) return null;
        if (rFilters == null) rFilters = GUITools.buildFileFilters(reader);
        return rFilters;
    }

    /** Gets file filters for use with formats supported for writing. */
    public FileFilter[] getWriterFilters() {
        if (writer == null) return null;
        if (wFilters == null) wFilters = GUITools.buildFileFilters(writer);
        return wFilters;
    }

    /**
   * Opens an existing image file from the given filename.
   *
   * @return VisAD Data object containing image data
   */
    public DataImpl open(String id) throws BadFormException, IOException, VisADException {
        percent = 0;
        int nImages = getBlockCount(id);
        FieldImpl[] fields = new FieldImpl[nImages];
        for (int i = 0; i < nImages; i++) {
            fields[i] = (FieldImpl) open(id, i);
            percent = (double) (i + 1) / nImages;
        }
        DataImpl data;
        if (nImages == 1) data = fields[0]; else {
            RealType index = RealType.getRealType("index");
            FunctionType indexFunction = new FunctionType(index, fields[0].getType());
            Integer1DSet indexSet = new Integer1DSet(nImages);
            FieldImpl indexField = new FieldImpl(indexFunction, indexSet);
            indexField.setSamples(fields, false);
            data = indexField;
        }
        close();
        percent = Double.NaN;
        return data;
    }

    /** Saves a VisAD Data object at the given location. */
    public void save(String id, Data data, boolean replace) throws BadFormException, IOException, RemoteException, VisADException {
        if (writer == null) throw new BadFormException("No writer");
        percent = 0;
        FlatField[] fields = DataUtility.getImageFields(data);
        try {
            initHandler(writer, id);
            for (int i = 0; i < fields.length; i++) {
                Image image;
                if (fields[i] instanceof ImageFlatField) {
                    image = ((ImageFlatField) fields[i]).getImage();
                } else image = DataUtility.extractImage(fields[i], false);
                writer.saveImage(image, i == fields.length - 1);
                percent = (double) (i + 1) / fields.length;
            }
        } catch (FormatException exc) {
            throw new BadFormException(exc);
        }
        percent = Double.NaN;
    }

    /**
   * Adds data to an existing image file.
   *
   * @exception BadFormException Always thrown (this method not
   * implemented).
   */
    public void add(String id, Data data, boolean replace) throws BadFormException {
        throw new BadFormException("LociForm.add");
    }

    /**
   * Opens an existing image file from the given URL.
   *
   * @return VisAD data object containing image data
   * @exception UnimplementedException Always thrown (this method not
   * implemented).
   */
    public DataImpl open(URL url) throws BadFormException, IOException, VisADException {
        throw new UnimplementedException("LociForm.open(URL)");
    }

    /** Returns the data forms that are compatible with a data object. */
    public FormNode getForms(Data data) {
        return null;
    }

    /** Obtains the specified image from the given image file. */
    public DataImpl open(String id, int block_number) throws BadFormException, IOException, VisADException {
        if (reader == null) throw new BadFormException("No reader");
        BufferedImage image;
        try {
            initHandler(reader, id);
            image = reader.openImage(block_number);
        } catch (FormatException exc) {
            throw new BadFormException(exc);
        }
        int width = image.getWidth(), height = image.getHeight();
        int num = image.getRaster().getNumBands();
        RealType x = RealType.getRealType("ImageElement");
        RealType y = RealType.getRealType("ImageLine");
        RealType[] v = new RealType[num];
        for (int i = 0; i < num; i++) v[i] = RealType.getRealType("value" + i);
        RealTupleType domain = new RealTupleType(x, y);
        RealTupleType range = new RealTupleType(v);
        FunctionType fieldType = new FunctionType(domain, range);
        Linear2DSet fieldSet = new Linear2DSet(domain, 0, width - 1, width, height - 1, 0, height);
        ImageFlatField field = new ImageFlatField(fieldType, fieldSet);
        field.setImage(image);
        return field;
    }

    /** Determines the number of images in the given image file. */
    public int getBlockCount(String id) throws BadFormException, IOException, VisADException {
        if (reader == null) throw new BadFormException("No reader");
        initHandler(reader, id);
        return reader.getImageCount();
    }

    /** Closes any open files. */
    public void close() throws BadFormException, IOException, VisADException {
        if (reader == null) throw new BadFormException("No reader");
        reader.close();
    }

    /** Checks if the given string is a valid filename for an image file. */
    public boolean isThisType(String name) {
        if (reader == null) return false;
        return reader.isThisType(name);
    }

    /** Checks if the given block is a valid header for an image file. */
    public boolean isThisType(byte[] block) {
        if (reader == null) return false;
        return reader.isThisType(block);
    }

    /** Returns the default file suffixes for this file format. */
    public String[] getDefaultSuffixes() {
        if (reader != null) return reader.getSuffixes();
        if (writer != null) return writer.getSuffixes();
        return null;
    }

    /** Gets the percentage complete of the form's current operation. */
    public double getPercentComplete() {
        return percent;
    }

    /**
   * Obtains the specified metadata field's value for the given file.
   *
   * @param field the name associated with the metadata field
   * @return the value, or null if the field doesn't exist
   */
    public Object getMetadataValue(String id, String field) throws BadFormException, IOException, VisADException {
        if (reader == null) throw new BadFormException("No reader");
        initHandler(reader, id);
        return reader.getMetadataValue(field);
    }

    /**
   * Obtains the hashtable containing the metadata field/value pairs from
   * the given image file.
   *
   * @param id the filename
   * @return the hashtable containing all metadata from the file
   */
    public Hashtable getMetadata(String id) throws BadFormException, IOException, VisADException {
        if (reader == null) throw new BadFormException("No reader");
        initHandler(reader, id);
        return reader.getMetadata();
    }

    public static void main(String[] args) throws Exception {
        new LociForm().testRead(args);
    }

    public void initHandler(IFormatHandler h, String id) throws BadFormException, IOException {
        try {
            h.setId(id);
        } catch (FormatException exc) {
            throw new BadFormException(exc);
        }
    }
}
