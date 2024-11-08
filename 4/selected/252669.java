package net.sf.myra.datamining.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.StringTokenizer;
import net.sf.myra.datamining.data.Attribute;
import net.sf.myra.datamining.data.ContinuousAttribute;
import net.sf.myra.datamining.data.Dataset;
import net.sf.myra.datamining.data.Instance;
import net.sf.myra.datamining.data.Metadata;
import net.sf.myra.datamining.data.NominalAttribute;

/**
 * Helper class to manipulate files in C4.5 format.
 * 
 * @author Fernando Esteban Barril Otero
 * @version $Revision: 2367 $ $Date:: 2011-04-14 11:30:59#$
 */
public final class C45Helper extends Helper {

    /**
	 * No instances allowed (private constructor).
	 */
    public C45Helper() {
    }

    public Dataset read(File directory, String filestem) throws IOException {
        Dataset dataset = read(new FileReader(new File(directory, filestem + ".names")), new FileReader(new File(directory, filestem + ".data")));
        dataset.getMetadata().setName(filestem);
        return dataset;
    }

    @Override
    public Dataset read(File file) throws IOException {
        int index = file.getName().lastIndexOf('.');
        return read(file.getParentFile(), file.getName().substring(0, index));
    }

    /**
	 * Returns a <code>Dataset</code> instance that represents the data of
	 * the specified files.
	 * 
	 * @param names the input reader for the .names file.
	 * @param data the input reader for the .data file.
	 * 
	 * @return a <code>Dataset</code> instance.
	 * 
	 * @throws IOException if any file operation fails.
	 */
    public Dataset read(Reader names, Reader data) throws IOException {
        BufferedReader input = null;
        try {
            input = new BufferedReader(names);
            Metadata metadata = new Metadata();
            String line = null;
            NominalAttribute klass = null;
            while ((line = input.readLine()) != null) {
                line = trim(line);
                if (!isBlank(line)) {
                    int colon = line.indexOf(":");
                    if (colon == -1) {
                        if (klass == null) {
                            klass = new NominalAttribute("Class");
                            StringTokenizer values = new StringTokenizer(clean(line), ",");
                            while (values.hasMoreTokens()) {
                                klass.add(trim(values.nextToken().trim()));
                            }
                        } else {
                            throw new IllegalArgumentException("Invalid class attribute information");
                        }
                    } else {
                        String name = line.substring(0, colon);
                        String values = clean(trim(line.substring(colon + 1)));
                        if ("continuous".equals(values)) {
                            metadata.add(new ContinuousAttribute(name));
                        } else {
                            NominalAttribute n = new NominalAttribute(name);
                            StringTokenizer v = new StringTokenizer(values, ",");
                            while (v.hasMoreTokens()) {
                                n.add(v.nextToken().trim());
                            }
                            metadata.add(n);
                        }
                    }
                }
            }
            if (klass != null) {
                metadata.add(klass);
                metadata.setTarget(klass);
            } else {
                throw new IllegalArgumentException("Missing class attribute information");
            }
            input.close();
            input = new BufferedReader(data);
            Dataset dataset = new Dataset(metadata);
            while ((line = input.readLine()) != null) {
                if (!isBlank(line)) {
                    StringTokenizer tokens = new StringTokenizer(line, ",");
                    String[] values = new String[tokens.countTokens()];
                    for (int i = 0; tokens.hasMoreTokens(); i++) {
                        values[i] = trim(tokens.nextToken());
                    }
                    try {
                        dataset.add(values);
                    } catch (IllegalArgumentException e) {
                        System.out.println("(ignoring record) " + e.getMessage());
                    }
                }
            }
            return dataset;
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
	 * Writes the specified dataset in the C4.5 input file format (names/data).
	 * 
	 * @param directory the output directory.
	 * @param dataset the dataset to be written.
	 */
    public void write(File directory, Dataset dataset) throws IOException {
        String name = dataset.getMetadata().getName();
        PrintWriter nWriter = null;
        PrintWriter dWriter = null;
        try {
            nWriter = new PrintWriter(new FileOutputStream(new File(directory, name + ".names")));
            dWriter = new PrintWriter(new FileOutputStream(new File(directory, name + ".data")));
            writeNames(nWriter, dataset.getMetadata());
            writeData(dWriter, dataset);
        } finally {
            if (nWriter != null) {
                nWriter.close();
            }
            if (dWriter != null) {
                dWriter.close();
            }
        }
    }

    /**
	 * Unsupported operation. C45Helper creates two files (.data and .names).
	 */
    @Override
    public void write(Writer writer, Dataset dataset) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void writeNames(PrintWriter out, Metadata metadata) throws IOException {
        NominalAttribute c = (NominalAttribute) metadata.getTarget();
        int index = 0;
        for (String value : c.getValues()) {
            if (index > 0) {
                out.print(", ");
            }
            out.print(value);
            index++;
        }
        out.println(".");
        out.println();
        for (Attribute attribute : metadata.getPredictor()) {
            out.print(attribute.getName());
            out.print(": ");
            if (attribute instanceof ContinuousAttribute) {
                out.println("continuous.");
            } else if (attribute instanceof NominalAttribute) {
                NominalAttribute n = (NominalAttribute) attribute;
                int j = 0;
                for (String value : n.getValues()) {
                    if (j > 0) {
                        out.print(", ");
                    }
                    out.print(value);
                    j++;
                }
                out.println(".");
            }
        }
    }

    private void writeData(PrintWriter out, Dataset dataset) throws IOException {
        for (Instance instance : dataset) {
            for (Attribute attribute : dataset.getMetadata().getPredictor()) {
                out.print(instance.getValue(attribute));
                out.print(", ");
            }
            out.println(instance.getValue(dataset.getMetadata().getTarget()));
        }
    }

    /**
	 * Checks if the line is blank.
	 * 
	 * @param line the line to check.
	 * 
	 * @return <code>true</code> if the line is blank; <code>false</code>
	 *         otherwise.
	 */
    private boolean isBlank(String line) {
        return line.trim().equals("");
    }

    private String trim(String s) {
        String clean = s.replaceAll("(\'|\")+", " ").trim();
        int index = clean.indexOf('|');
        if (index != -1) {
            clean = clean.substring(0, index).trim();
        }
        return clean;
    }

    private static String clean(String s) {
        return s.replaceAll("(\\.)+", " ").trim();
    }

    public static void main(String[] args) throws Exception {
        C45Helper helper = new C45Helper();
        File input = new File(args[0]);
        File output = new File(args[1]);
        for (File d : input.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        })) {
            for (String file : d.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.startsWith("TR") && name.endsWith(".arff");
                }
            })) {
                File dOutput = new File(output, d.getName());
                dOutput.mkdirs();
                helper.write(dOutput, new ArffHelper().read(new File(d, file)));
                helper.write(dOutput, new ArffHelper().read(new File(d, file.replace("TR", "TS"))));
                File testFile = new File(dOutput, file.replace("TR", "TS").replace(".arff", ".data"));
                testFile.renameTo(new File(dOutput, testFile.getName().replace("TS", "TR").replace(".data", ".test")));
                testFile = new File(dOutput, file.replace("TR", "TS").replace(".arff", ".names"));
                testFile.delete();
            }
        }
    }
}
