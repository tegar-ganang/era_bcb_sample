package com.evaserver.rof.generator;

import junit.framework.TestCase;

/**
 *
 *
 * @author Max Antoni
 * @version $Revision: 172 $
 */
public abstract class AbstractGeneratorTest extends TestCase {

    protected String generate(String inScriptName) {
        InMemoryScriptWriter writer = new InMemoryScriptWriter();
        ScriptReader reader = createScriptReader();
        Processor processor = new Processor(reader, writer);
        processor.process(inScriptName);
        return writer.getScript();
    }

    protected ScriptReader createScriptReader() {
        ScriptReaderImpl scriptReader = new ScriptReaderImpl();
        scriptReader.setClasspathPrefix(AbstractGeneratorTest.class.getPackage().getName().replace('.', '/'));
        scriptReader.setGenerateScripts(getGenerateScripts());
        return scriptReader;
    }

    protected boolean getGenerateScripts() {
        return true;
    }
}
