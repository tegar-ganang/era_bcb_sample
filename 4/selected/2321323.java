package net.sf.myra.datamining.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import net.sf.myra.datamining.data.Attribute;
import net.sf.myra.datamining.data.Dataset;
import net.sf.myra.datamining.data.Instance;
import net.sf.myra.datamining.data.Metadata;
import net.sf.myra.datamining.data.NominalAttribute;

/**
 * @author Fernando Esteban Barril Otero
 * @version $Revision: 2369 $ $Date:: 2011-04-14 11:33:19#$
 */
public class CN2Helper extends Helper {

    @Override
    public Dataset read(File file) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void write(File directory, Dataset dataset) throws IOException {
        Metadata metadata = dataset.getMetadata();
        PrintWriter out = new PrintWriter(new FileWriter(new File(directory, metadata.getName() + ".att")));
        out.println("**ATTRIBUTE FILE**");
        out.println();
        for (Attribute attribute : metadata.getPredictor()) {
            out.print(attribute.getName().replace('/', '-'));
            out.print(":\t");
            if (attribute instanceof NominalAttribute) {
                for (String value : ((NominalAttribute) attribute).getValues()) {
                    if (!"-".equals(value)) {
                        out.print("\"");
                        out.print(value);
                        out.print("\" ");
                    }
                }
                out.println(";");
            } else {
                out.println("(FLOAT)");
            }
        }
        out.print("classes:\t");
        for (String value : metadata.getTarget().getValues()) {
            out.print("\"");
            out.print(value);
            out.print("\" ");
        }
        out.println(";");
        out.println();
        out.close();
        out = new PrintWriter(new FileWriter(new File(directory, metadata.getName() + ".exs")));
        out.println("**EXAMPLE FILE**");
        out.println();
        for (Instance instance : dataset) {
            for (Attribute attribute : metadata.getPredictor()) {
                if (attribute instanceof NominalAttribute && !Metadata.MISSING_VALUE.equals(instance.getValue(attribute))) {
                    out.print("\"");
                    out.print(instance.getValue(attribute));
                    out.print("\" ");
                } else {
                    out.print(instance.getValue(attribute));
                    out.print(" ");
                }
            }
            out.print("\"");
            out.print(instance.getLabel().toString());
            out.println("\" ;");
        }
        out.println();
        out.close();
    }

    @Override
    public void write(Writer writer, Dataset dataset) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public static void main(String[] args) throws Exception {
        CN2Helper helper = new CN2Helper();
        File input = new File(args[0]);
        File output = new File(args[1]);
        for (File d : input.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        })) {
            for (String file : d.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.endsWith(".arff");
                }
            })) {
                File dOutput = new File(output, d.getName());
                dOutput.mkdirs();
                helper.write(dOutput, new ArffHelper().read(new File(d, file)));
            }
        }
    }
}
