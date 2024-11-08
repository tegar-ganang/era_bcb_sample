package net.sf.myra.datamining.statistics;

import static net.sf.myra.datamining.statistics.CurveFactory.NEGATIVE;
import static net.sf.myra.datamining.statistics.CurveFactory.POSITIVE;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sf.myra.datamining.data.Attribute;
import net.sf.myra.datamining.data.Dataset;
import net.sf.myra.datamining.data.Instance;
import net.sf.myra.datamining.data.Metadata;
import net.sf.myra.datamining.io.ArffHelper;
import net.sf.myra.datamining.io.Helper;

/**
 * Generates PR curves from a predictions file.
 * 
 * @author Fernando Esteban Barril Otero
 * @version $Revision: 2262 $ $Date:: 2010-02-04 14:51:23#$
 */
public class Parser implements PropertyChangeListener {

    /**
	 * Property name for the mapping size event.
	 */
    public static final String MAPPING_SIZE = "size";

    /**
	 * Property name for the event of loading a class label.
	 */
    public static final String CLASS_LABEL = "class";

    /**
	 * Prefix for 'original' (true) class attribute flags.
	 */
    public static final String ORIGINAL_PREFIX = "class-a-";

    /**
	 * Prefix for predicted class attribute flags.
	 */
    public static final String PREDICTED_PREFIX = "Original-p-";

    /**
	 * Flag to indicate that the attribute is enabled.
	 */
    public static final String ENABLED = "1";

    /**
	 * The <code>PropertyChangeSupport</code> object.
	 */
    private PropertyChangeSupport bean = new PropertyChangeSupport(this);

    /**
	 * Parses the specified prediction file.
	 * 
	 * @param file the name of the file.
	 */
    public Information read(String file, CurveFactory factory) throws IOException {
        return read(new File(file), factory);
    }

    /**
	 * Parses the specified prediction file.
	 * 
	 * @param file the name of the file.
	 */
    public Information read(String file, CurveFactory factory, Set<String> filter) throws IOException {
        return read(new File(file), factory, filter);
    }

    /**
	 * Parses the specified prediction file.
	 * 
	 * @param file the file to parse.
	 */
    @SuppressWarnings("unchecked")
    public Information read(File file, CurveFactory factory) throws IOException {
        return read(file, factory, Collections.EMPTY_SET);
    }

    /**
	 * Parses the specified prediction file.
	 * 
	 * @param file the dataset file name.
	 * @param factory the curve factory instance.
	 * @param filter the set of attributes to ignore.
	 */
    public Information read(File file, CurveFactory factory, Set<String> filter) throws IOException {
        Dataset dataset = Helper.getHelper(file).read(file);
        return read(dataset, factory, filter);
    }

    /**
	 * Parses the specified prediction file.
	 * 
	 * @param dataset the dataset instance.
	 * @param factory the curve factory instance.
	 * @param filter the set of attributes to ignore.
	 */
    public Information read(Dataset dataset, CurveFactory factory, Set<String> filter) throws IOException {
        Metadata metadata = dataset.getMetadata();
        Map<String, double[][]> matrix = new TreeMap<String, double[][]>();
        for (Attribute attribute : metadata.getAttributes()) {
            String name = attribute.getName();
            if (name.startsWith(ORIGINAL_PREFIX)) {
                name = name.split(ORIGINAL_PREFIX)[1];
                if (!filter.contains(name)) {
                    matrix.put(name, new double[dataset.getSize()][2]);
                }
            }
        }
        int i = 0;
        for (Instance instance : dataset) {
            for (Attribute attribute : metadata.getAttributes()) {
                String name = attribute.getName();
                if (name.startsWith(ORIGINAL_PREFIX)) {
                    name = name.split(ORIGINAL_PREFIX)[1];
                    if (!filter.contains(name)) {
                        double[][] values = matrix.get(name);
                        boolean flag = ENABLED.equals(instance.getValue(attribute));
                        values[i][1] = (flag ? POSITIVE : NEGATIVE);
                        int index = metadata.getIndex(PREDICTED_PREFIX + name);
                        values[i][0] = instance.values()[index];
                    }
                }
            }
            i++;
        }
        bean.firePropertyChange(MAPPING_SIZE, 0, matrix.size());
        Information information = new Information(factory);
        information.addListener(this);
        information.load(matrix);
        return information;
    }

    public void propertyChange(PropertyChangeEvent e) {
        bean.firePropertyChange(CLASS_LABEL, e.getOldValue(), e.getNewValue());
    }

    /**
	 * Adds a listener to this parser.
	 * 
	 * @param listener the listener to be added.
	 */
    public void addListener(PropertyChangeListener listener) {
        bean.addPropertyChangeListener(listener);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        ZipFile zip = new ZipFile(new File(args[0]), ZipFile.OPEN_READ);
        Enumeration entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().endsWith("arff.zip")) {
                File temp = File.createTempFile("PARSER", ".zip");
                temp.deleteOnExit();
                PrintStream writer = new PrintStream(new FileOutputStream(temp));
                BufferedInputStream reader = new BufferedInputStream(zip.getInputStream(entry));
                byte[] buffer = new byte[4096];
                int read = -1;
                while ((read = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, read);
                }
                writer.close();
                reader.close();
                Dataset dataset = new ArffHelper().read(temp);
                Information info = new Parser().read(dataset, CurveFactory.PRECISION_RECALL, new HashSet<String>(Arrays.asList(new String[] { "GO0003674", "GO0005575", "GO0008150" })));
                System.out.println(entry.getName() + ": AU(PRC) " + info.getCurve().area());
            }
        }
    }
}
