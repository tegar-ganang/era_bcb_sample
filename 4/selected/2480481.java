package com.evaserver.rof.generator;

import java.util.Collection;

/**
 *
 *
 * @author Max Antoni
 * @version $Revision: 172 $
 */
public class Processor {

    private ScriptReader reader;

    private ScriptWriter writer;

    private String aggregatedScriptName;

    public Processor() {
    }

    public Processor(ScriptReader inReader, ScriptWriter inWriter) {
        reader = inReader;
        writer = inWriter;
    }

    /**
	 * @param inScriptNames the script name.
	 * @throws GeneratorException if processing fails.
	 */
    public void process(String[] inScriptNames) throws GeneratorException {
        if (aggregatedScriptName == null) {
            for (int i = 0; i < inScriptNames.length; i++) {
                process(inScriptNames[i]);
            }
        } else {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < inScriptNames.length; i++) {
                String scriptName = inScriptNames[i];
                buffer.append(readScript(scriptName));
            }
            writeScript(aggregatedScriptName, buffer.toString());
        }
    }

    /**
	 * @param inScriptName the script name.
	 * @throws GeneratorException if processing fails.
	 */
    public void process(String inScriptName) throws GeneratorException {
        writeScript(inScriptName, readScript(inScriptName));
    }

    /**
	 * @throws GeneratorException if processing fails.
	 */
    public void process() throws GeneratorException {
        Collection scriptNames = reader.findAll();
        process((String[]) scriptNames.toArray(new String[scriptNames.size()]));
    }

    /**
	 * @param inReader the reader.
	 */
    public void setReader(ScriptReader inReader) {
        reader = inReader;
    }

    /**
	 * @param inWriter the writer.
	 */
    public void setWriter(ScriptWriter inWriter) {
        writer = inWriter;
    }

    /**
	 * @param inAggregatedScriptName the aggregated script name.
	 */
    public void setAggregatedScriptName(String inAggregatedScriptName) {
        aggregatedScriptName = inAggregatedScriptName;
    }

    private String readScript(String inScriptName) throws GeneratorException {
        try {
            return reader.read(inScriptName);
        } catch (ScriptReaderException e) {
            throw new GeneratorException("Cannot read script \"" + inScriptName + "\":", e);
        }
    }

    private void writeScript(String inScriptName, String inScript) throws GeneratorException {
        try {
            writer.write(inScriptName, inScript);
        } catch (ScriptWriterException e) {
            throw new GeneratorException("Cannot write script " + inScriptName, e);
        }
    }
}
