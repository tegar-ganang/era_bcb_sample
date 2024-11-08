package org.jmove.core.test;

import junit.framework.TestCase;
import org.jmove.core.ModelSerialization;
import org.jmove.java.loader.LoadDetailsFromTypes;
import org.jmove.java.loader.LoadModulesFromClasspath;
import org.jmove.java.loader.LoadSystemTypes;
import org.jmove.java.model.JModel;
import org.jmove.java.model.Type;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

/**
 * Testing model serialization.
 *
 * @author Michael Juergens
 * @version $Revision: 1.3 $
 */
public class ModelSerializationTest extends TestCase {

    public ModelSerializationTest(String s) {
        super(s);
    }

    public void testMinimalModel() throws Exception {
        JModel writeModel = new JModel();
        LoadSystemTypes systemTypeAction = new LoadSystemTypes(writeModel);
        systemTypeAction.perform();
        LoadModulesFromClasspath typeAction = new LoadModulesFromClasspath(writeModel);
        typeAction.perform();
        LoadDetailsFromTypes detailAction = new LoadDetailsFromTypes(writeModel, true);
        detailAction.perform();
        ModelSerialization serialization = new ModelSerialization();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        serialization.writeModelToStream(writeModel, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        JModel readModel = (JModel) serialization.loadModelFromStream(bais);
        assertNotNull(readModel);
        Iterator writeTypes = writeModel.getTypes().iterator();
        while (writeTypes.hasNext()) {
            Type writeType = (Type) writeTypes.next();
            assertNotNull(readModel.lookupType(writeType.id()));
        }
    }
}
