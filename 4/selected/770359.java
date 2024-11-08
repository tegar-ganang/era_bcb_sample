package com.aptana.parsing.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import com.aptana.ide.editor.js.runtime.Environment;
import com.aptana.ide.editor.scriptdoc.parsing.reader.NativeObjectsReader;
import com.aptana.ide.editor.scriptdoc.parsing.reader.ScriptDocException;
import com.aptana.ide.editor.scriptdoc.parsing.reader.ScriptDocInitializationException;
import com.aptana.ide.io.TabledOutputStream;

/**
 * @author Kevin Lindsey
 */
public final class ScriptDocToBinary {

    /**
	 * ScriptDocToBinary
	 */
    private ScriptDocToBinary() {
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        if (args.length == 2) {
            String inputFilename = args[0];
            String outputFilename = args[1];
            try {
                generateFile(inputFilename, outputFilename);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ScriptDocInitializationException e) {
                e.printStackTrace();
            } catch (ScriptDocException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(Messages.getString("ScriptDocToBinary.Usage_Syntax"));
            System.out.println(Messages.getString("ScriptDocToBinary.Usage_Input_File"));
            System.out.println(Messages.getString("ScriptDocToBinary.Usage_Output_File"));
        }
    }

    private static void generateFile(String inputFilename, String outputFilename) throws Exception {
        File inputFile = new File(inputFilename);
        if (inputFile.exists() == false) {
            throw new Exception(Messages.getString("ScriptDocToBinary.Input_File_Does_Not_Exist") + inputFilename);
        }
        Environment environment = new Environment();
        environment.initBuiltInObjects();
        NativeObjectsReader reader = new NativeObjectsReader(environment);
        InputStream input = new FileInputStream(inputFile);
        System.out.println(Messages.getString("ScriptDocToBinary.Reading_Documentation") + inputFilename);
        reader.loadXML(input);
        checkFile(outputFilename);
        File binaryFile = new File(outputFilename);
        FileOutputStream outputStream = new FileOutputStream(binaryFile);
        TabledOutputStream output = new TabledOutputStream(outputStream);
        reader.getScriptDoc().write(output);
        output.close();
        System.out.println(Messages.getString("ScriptDocToBinary.Finished"));
        System.out.println();
    }

    private static void checkFile(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            if (file.canWrite() == false) {
                String message = MessageFormat.format(Messages.getString("ScriptDocToBinary.Cannot_Write_File"), new Object[] { filename });
                throw new IllegalStateException(message);
            }
        }
    }
}
