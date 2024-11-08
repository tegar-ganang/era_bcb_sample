package org.dozer.loader.xml;

import java.net.URL;
import java.util.List;
import org.dozer.AbstractDozerTest;
import org.dozer.classmap.ClassMap;
import org.dozer.classmap.MappingFileData;
import org.dozer.fieldmap.FieldMap;
import org.dozer.loader.MappingsSource;
import org.dozer.util.ResourceLoader;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * @author garsombke.franz
 * @author johnsen.knut-erik
 */
public class XMLParserTest extends AbstractDozerTest {

    private MappingsSource parser;

    @Test
    public void testParse() throws Exception {
        ResourceLoader loader = new ResourceLoader();
        URL url = loader.getResource("dozerBeanMapping.xml");
        Document document = XMLParserFactory.getInstance().createParser().parse(url.openStream());
        parser = new XMLParser(document);
        MappingFileData mappings = parser.load();
        assertNotNull(mappings);
    }

    /**
   * This tests checks that the customconverterparam reaches the
   * fieldmapping.      
   */
    @Test
    public void testParseCustomConverterParam() throws Exception {
        ResourceLoader loader = new ResourceLoader();
        URL url = loader.getResource("fieldCustomConverterParam.xml");
        Document document = XMLParserFactory.getInstance().createParser().parse(url.openStream());
        parser = new XMLParser(document);
        MappingFileData mappings = parser.load();
        assertNotNull("The mappings should not be null", mappings);
        List<ClassMap> mapping = mappings.getClassMaps();
        assertNotNull("The list of mappings should not be null", mapping);
        assertEquals("There should be one mapping", 3, mapping.size());
        ClassMap classMap = mapping.get(0);
        assertNotNull("The classmap should not be null", classMap);
        List<FieldMap> fieldMaps = classMap.getFieldMaps();
        assertNotNull("The fieldmaps should not be null", fieldMaps);
        assertEquals("The fieldmap should have one mapping", 1, fieldMaps.size());
        FieldMap fieldMap = fieldMaps.get(0);
        assertNotNull("The fieldmap should not be null", fieldMap);
        assertEquals("The customconverterparam should be correct", "CustomConverterParamTest", fieldMap.getCustomConverterParam());
    }
}
