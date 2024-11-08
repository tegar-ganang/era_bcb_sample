package de.ddb.conversion.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import de.ddb.charset.CharsetUtil;
import de.ddb.conversion.BinaryConverter;
import de.ddb.conversion.ConversionParameters;
import de.ddb.conversion.Converter;
import de.ddb.conversion.ConverterException;
import de.ddb.conversion.PipedConverter;
import de.ddb.conversion.context.ConversionContext;
import de.ddb.conversion.context.ConversionContextFactory;
import de.ddb.conversion.format.Format;
import junit.framework.TestCase;

public class DdbInternToMarcxmlTest extends TestCase {

    ConversionContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = ConversionContextFactory.newDefaultEnvironment();
        Format ddbInternFormat = new Format();
        Format marc21Format = new Format();
        Format marc21xmlFormat = new Format();
        ddbInternFormat.setName("ddb-intern");
        ddbInternFormat.setNamespaceString("http://www.d-nb.de/internals/ddb-intern");
        ddbInternFormat.setDescription("DNB-Internformat");
        ddbInternFormat.setContentType("application/x-pica+");
        ddbInternFormat.setEndOfRecordPatternString("\\u001D|\\n");
        marc21Format.setName("MARC21");
        marc21Format.setContentType("application/marc");
        marc21Format.setNamespaceString("http://www.ddb.de/standards/");
        marc21Format.setSchemaLocationString("http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd");
        marc21Format.setDescription("Maschinelles Austauschformat MARC21");
        marc21Format.setEndOfRecordPatternString("\\u001D|\\n");
        marc21xmlFormat.setName("MARC21-xml");
        marc21xmlFormat.setContentType("application/xml");
        marc21xmlFormat.setNamespaceString("http://www.loc.gov/MARC21/slim");
        marc21xmlFormat.setSchemaLocationString("http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd");
        marc21xmlFormat.setDescription("XML-Variante von MARC21 / DNB-Titeldaten, ZDB-Titeldaten, Normdaten");
        marc21xmlFormat.setRecordPatternString("&lt;record.*?/record>");
        context.addFormat(ddbInternFormat);
        context.addFormat(marc21Format);
        context.addFormat(marc21xmlFormat);
        List<String> envProp = new ArrayList<String>();
        envProp.add("LD_LIBRARY_PATH=/pica/sybase/lib");
        envProp.add("FILEMAP=/pica/tolk/confdir/FILEMAP");
        Converter marc21Conv = new BinaryConverter("\n" + ". .profile;\n" + "/pica/tolk/bin/csfn_pica32norm -y |\n" + "/pica/tolk/bin/csfn_fcvnorm -k FCV#pica#marc21-exchange -t ALPHA\n" + "| /pica/tolk/bin/ddb_denorm -f marc21-exchange |\n" + "/pica/tolk/bin/ddbflattenrecs -f\n", envProp, "x-PICA", "x-PICA", 0);
        context.addConverter(marc21Conv, "ddb-intern", "MARC21", "x-PICA", "x-PICA", null);
        Converter marc21xmlConv = new MarcToMarcxmlConverter();
        context.addConverter(marc21xmlConv, "MARC21", "MARC21-xml", "x-PICA", "UTF-8", null);
        Converter marcPipe = new PipedConverter(marc21Conv, marc21xmlConv);
        context.addConverter(marcPipe, "ddb-intern", "MARC21-xml", "x-PICA", "UTF-8", null);
    }

    @Override
    protected void tearDown() throws Exception {
        this.context = null;
        super.tearDown();
    }

    public void testConvert() throws IOException, ConverterException {
        InputStreamReader reader = new InputStreamReader(new FileInputStream("test" + File.separator + "input" + File.separator + "A0851ohneex.dat"), CharsetUtil.forName("x-PICA"));
        FileWriter writer = new FileWriter("test" + File.separator + "output" + File.separator + "ddbInterToMarcxmlTest.out");
        Converter c = context.getConverter("ddb-intern", "MARC21-xml", "x-PICA", "UTF-8");
        ConversionParameters params = new ConversionParameters();
        params.setSourceCharset("x-PICA");
        params.setTargetCharset("UTF-8");
        params.setAddCollectionHeader(true);
        params.setAddCollectionFooter(true);
        c.convert(reader, writer, params);
    }
}
