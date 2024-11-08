package com.scully.korat.map;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import com.scully.korat.map.BeanXmlMapper;
import com.scully.korat.map.CandidateFieldDTO;
import com.scully.korat.map.CandidateStateDTO;
import com.scully.korat.map.StateFieldDTO;
import com.scully.korat.map.StateObjectDTO;
import com.scully.korat.map.TestStateSpaceDTO;
import com.scully.korat.test.SearchTree;

/**
 * @author mscully
 * 
 */
public class BeanXmlMapperTest {

    TestStateSpaceDTO testStateSpaceDTO;

    @Before
    public void setUp() {
        this.testStateSpaceDTO = new TestStateSpaceDTO();
        this.testStateSpaceDTO.setRootClass(SearchTree.class);
        StateObjectDTO stateObjectDTO = new StateObjectDTO();
        stateObjectDTO.setType(SearchTree.Node.class);
        stateObjectDTO.setQuantity(3);
        stateObjectDTO.setNullable(true);
        this.testStateSpaceDTO.addStateObject(stateObjectDTO);
        StateFieldDTO stateFieldDTO = new StateFieldDTO();
        stateFieldDTO.setName("root");
        stateFieldDTO.setParentClass(SearchTree.class);
        stateFieldDTO.setType(SearchTree.Node.class);
        this.testStateSpaceDTO.addStateField(stateFieldDTO);
        stateFieldDTO = new StateFieldDTO();
        stateFieldDTO.setName("size");
        stateFieldDTO.setParentClass(SearchTree.class);
        stateFieldDTO.setType(int.class);
        stateFieldDTO.setMin(0);
        stateFieldDTO.setMax(3);
        this.testStateSpaceDTO.addStateField(stateFieldDTO);
        stateFieldDTO = new StateFieldDTO();
        stateFieldDTO.setName("left");
        stateFieldDTO.setParentClass(SearchTree.Node.class);
        stateFieldDTO.setType(SearchTree.Node.class);
        this.testStateSpaceDTO.addStateField(stateFieldDTO);
        stateFieldDTO = new StateFieldDTO();
        stateFieldDTO.setName("right");
        stateFieldDTO.setParentClass(SearchTree.Node.class);
        stateFieldDTO.setType(SearchTree.Node.class);
        this.testStateSpaceDTO.addStateField(stateFieldDTO);
        stateFieldDTO = new StateFieldDTO();
        stateFieldDTO.setName("value");
        stateFieldDTO.setParentClass(SearchTree.Node.class);
        stateFieldDTO.setType(int.class);
        stateFieldDTO.setMin(1);
        stateFieldDTO.setMax(3);
        this.testStateSpaceDTO.addStateField(stateFieldDTO);
        CandidateStateDTO candidateStateDTO = new CandidateStateDTO();
        CandidateFieldDTO candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree@b8df17.root");
        candidateFieldDTO.setFieldName("root");
        candidateFieldDTO.setFieldType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree");
        candidateFieldDTO.setValueIndex(1);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree@b8df17.size");
        candidateFieldDTO.setFieldName("size");
        candidateFieldDTO.setFieldType("int");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree");
        candidateFieldDTO.setValueIndex(3);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree$Node@5224ee.left");
        candidateFieldDTO.setFieldName("left");
        candidateFieldDTO.setFieldType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setValueIndex(0);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree$Node@5224ee.right");
        candidateFieldDTO.setFieldName("right");
        candidateFieldDTO.setFieldType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setValueIndex(2);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree$Node@5224ee.value");
        candidateFieldDTO.setFieldName("value");
        candidateFieldDTO.setFieldType("int");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setValueIndex(0);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree$Node@f6a746.left");
        candidateFieldDTO.setFieldName("left");
        candidateFieldDTO.setFieldType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setValueIndex(3);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree$Node@f6a746.right");
        candidateFieldDTO.setFieldName("right");
        candidateFieldDTO.setFieldType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setValueIndex(0);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree$Node@f6a746.value");
        candidateFieldDTO.setFieldName("value");
        candidateFieldDTO.setFieldType("int");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setValueIndex(2);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree$Node@15ff48b.left");
        candidateFieldDTO.setFieldName("left");
        candidateFieldDTO.setFieldType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setValueIndex(0);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree$Node@15ff48b.right");
        candidateFieldDTO.setFieldName("right");
        candidateFieldDTO.setFieldType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setValueIndex(0);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        candidateFieldDTO = new CandidateFieldDTO();
        candidateFieldDTO.setFieldId("com.scully.korat.test.SearchTree$Node@15ff48b.value");
        candidateFieldDTO.setFieldName("value");
        candidateFieldDTO.setFieldType("int");
        candidateFieldDTO.setParentType("com.scully.korat.test.SearchTree$Node");
        candidateFieldDTO.setValueIndex(1);
        candidateStateDTO.addCandidateField(candidateFieldDTO);
        this.testStateSpaceDTO.addCandidateState(candidateStateDTO);
    }

    /**
     * Test method for {@link com.scully.korat.map.BeanXmlMapper#print()}.
     */
    @Test
    public void testWriteReadBean() {
        Writer writer = null;
        Reader reader = null;
        writer = new StringWriter();
        BeanXmlMapper.writeBean(writer, this.testStateSpaceDTO);
        reader = new StringReader(writer.toString());
        TestStateSpaceDTO testStateSpaceDTO = (TestStateSpaceDTO) BeanXmlMapper.readBean(reader, "TestStateSpaceDTO", TestStateSpaceDTO.class);
        assertEquals("testStateSpaceDTO failed to map from object to xml and back.", this.testStateSpaceDTO, testStateSpaceDTO);
    }
}
