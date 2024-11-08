package gov.ornl.nice.niceitem.item.action;

import gov.ornl.nice.nicedatastructures.form.FormStatus;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Dictionary;

/** 
 * <!-- begin-UML-doc -->
 * <p>The LocalExecutorAction is responsible for launching jobs on a local machine. It does not require any information in addition to that provided by the dictionary to execute and getForm() will return null.</p><p>The execute operation implements execute from the Action base class to launch jobs on a local system. It requires that the following key-value pairs are defined in its dictionary in order to execute:</p><table border="1"><col width="50.0%"></col><col width="50.0%"></col><tr><td><p><b>Key</b></p></td><td><p><b>Value</b></p></td></tr><tr><td><p>executable</p></td><td><p>The name of the executable as it exists on the system path or, alternatively, the fully qualified path to the executable.</p></td></tr><tr><td><p>inputFile</p></td><td><p>The path of the input file used by the executable. This may, alternatively, represent any stream of characters.</p></td></tr><tr><td><p>stdOutFile</p></td><td><p>The path of the file to which information printed to stdout by the job should be written.</p></td></tr><tr><td><p>stdErrFile</p></td><td><p>The path of the file to which information printed to stderr by the job should be written.</p></td></tr></table><p>If any of these pairs are missing, the action will return FormStatus.InfoError. It appends to the end of each file listed in the map.</p><p>The cancel() operation attempts to kill the process if it is still running.</p>
 * <!-- end-UML-doc -->
 * @author bkj
 * @generated "UML to Java (com.ibm.xtools.transform.uml2.java5.internal.UML2JavaTransform)"
 */
public class LocalExecutorAction extends Action {

    /** 
	 * <!-- begin-UML-doc -->
	 * <p>The task id for the locally executed job.</p>
	 * <!-- end-UML-doc -->
	 * @generated "UML to Java (com.ibm.xtools.transform.uml2.java5.internal.UML2JavaTransform)"
	 */
    private int taskId;

    /**
	 * A process for storing the process information from the launch.
	 */
    private Process job;

    /** 
	 * <!-- begin-UML-doc -->
	 * <p>The constructor.</p>
	 * <!-- end-UML-doc -->
	 * @generated "UML to Java (com.ibm.xtools.transform.uml2.java5.internal.UML2JavaTransform)"
	 */
    public LocalExecutorAction() {
        taskId = -1;
    }

    /**
	 * (non-Javadoc)
	 * 
	 * @see Action#execute(Dictionary<Object> dictionary)
	 */
    @Override
    public FormStatus execute(Dictionary<String, String> dictionary) {
        FormStatus retVal = FormStatus.InfoError;
        String executable = null, inputFile = null, cmd = null, nextLine = null;
        String stdOutFileName = null, stdErrFileName = null;
        FileWriter stdOut = null, stdErr = null;
        if (dictionary != null) {
            executable = dictionary.get("executable");
            inputFile = dictionary.get("inputFile");
            stdOutFileName = dictionary.get("stdOutFileName");
            stdErrFileName = dictionary.get("stdErrFileName");
            if (executable != null && inputFile != null && stdOutFileName != null && stdErrFileName != null) {
                cmd = executable + " " + inputFile;
                try {
                    job = Runtime.getRuntime().exec(cmd);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("LocalExecutorAction Message: " + "Launching command: " + "\"" + cmd + "\"");
                InputStream istream = job.getInputStream();
                InputStreamReader istreamReader = new InputStreamReader(istream);
                BufferedReader reader = new BufferedReader(istreamReader);
                try {
                    stdOut = new FileWriter(stdOutFileName, true);
                    System.out.println("File location = " + stdOutFileName);
                    BufferedWriter stdOutBW = new BufferedWriter(stdOut);
                    while ((nextLine = reader.readLine()) != null) {
                        stdOut.write(nextLine);
                        System.out.println(nextLine);
                    }
                    stdOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (retVal.equals(FormStatus.InfoError)) {
            return retVal;
        } else {
            status = FormStatus.Processed;
            return retVal;
        }
    }

    /** 
	 * (non-Javadoc)
	 * @see Action#cancel()
	 * @generated "UML to Java (com.ibm.xtools.transform.uml2.java5.internal.UML2JavaTransform)"
	 */
    public FormStatus cancel() {
        return null;
    }
}
