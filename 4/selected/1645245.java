package org.TMSIM.Simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.TMSIM.Exceptions.DirectionNotDefindedException;
import org.TMSIM.Exceptions.SimulationNotInitiatedException;
import org.TMSIM.Exceptions.TapeOutofBoundsException;
import org.TMSIM.Exceptions.TransitionNotContainedException;
import org.TMSIM.Exceptions.TuringMachineValidationException;
import org.TMSIM.Model.BaseConfiguration;
import org.TMSIM.Model.State;
import org.TMSIM.Model.TransitionCollection;
import org.TMSIM.Model.TuringMachine;
import org.TMSIM.Model.XML.TuringMachineTestConfiguration;
import org.TMSIM.Model.XML.XML_IO;
import org.TMSIM.Simulator.ArgumentHandling.ArgumentHandler;
import org.TMSIM.Simulator.ArgumentHandling.Exceptions.ArgumentHandlerException;
import org.TMSIM.Simulator.ArgumentHandling.Option;
import org.xml.sax.SAXException;

/**
 * Main class of the TMSimulator project.<br/><br/>
 * Usage of main function:<br/>
 * The TuringMachine can be provided as a JavaObjekt .tm file made by<br/>
 * the TMSim GUI or as an XML file.<br/>
 * After a successfull simulation the program terminates with exitcode 0.<br/>
 * If a critical error occurs or one or more Simulations encounter problems an<br/>
 * exception is logged and the program terminates with exitcode 1.<br/><br/>
 * <b>Usage:</b><br/>
 * java -jar [-Djava.util.logging.config.file=mylogging.properties] TMSimulator.jar {turingMachine.tm | turingMachine.tmx} [-qsr] [-t testrun.xml] [-p print.properties] [-m maxSteps]<br/><br/>
 * <b>Parameter description:</b><br/>
 * {turingMachine.tm | turingMachine.tmx} - absolute or relative path of TuringMachine definition. (if file has .tmx suffix, XML loading is performed instead of JavaObject loading)<br/>
 * -q   -   quit Simulation on TuringMachine success (e.g. one branch of the SimultationTree ends in a Finalstate)<br/>
 * -s   -   skip validation of given TuringMachine<br/>
 * -r   -   turn off Reportstyle logging (log only Testresults and Warnings)<br/>
 * -t testrun.xml   -   XML Definition of TestRuns (will be used instead of the TuringMachine´s BaseConfiguration)<br/>
 * -p print.properties  -   use custom print formats from given properties file<br/>
 * -m maxSteps  -   maximum count of Simulationsteps to take in every Simulation (will be used instead of any other defined Simulationstep count)<br/>
 * Creation Date: 30.07.2010
 * @author Christoph Prybila <christoph@prybila.at>
 */
public class Main {

    private static final Logger logger = Logger.getLogger("org.TMSIM.Simulator.Main");

    /**Programname for usage()*/
    private static String programname = "TuringMachineSimulator";

    /**Stores the usage description of the program*/
    private static String handler_usage = "";

    /**Stores the parameter description of the program*/
    private static String handler_parameters = "";

    /**Default maximum count of simulation steps - 50*/
    private static int defaultSteps = 50;

    /**program exit codes - constant*/
    private static final int EXIT_SUCCESS = 0;

    /**program exit codes - constant*/
    private static final int EXIT_FAILURE = 1;

    /**program exit codes - runtime variable*/
    private static int program_exitcode = EXIT_SUCCESS;

    /**program argumentflags and values*/
    private static boolean quitOnSuccessFlag = false;

    /**program argumentflags and values*/
    private static boolean reportStyleOffFlag = false;

    /**program argumentflags and values*/
    private static boolean skipValidationFlag = false;

    /**program argumentflags and values*/
    private static boolean testRunFlag = false;

    /**program argumentflags and values*/
    private static String testRunValue = "";

    /**program argumentflags and values*/
    private static boolean printPropertiesFlag = false;

    /**program argumentflags and values*/
    private static String printPropertiesValue = "";

    /**program argumentflags and values*/
    private static boolean maxStepsFlag = false;

    /**program argumentflags and values*/
    private static String maxStepsValue = "";

    /**program argumentflags and values*/
    private static String turingMachinePath = "";

    /**used print formats, may be changed at program start via print.properties*/
    private static String turingMachinePrintFormat = TuringMachine.turingMachinePrintFormat;

    /**used print formats, may be changed at program start via print.properties*/
    private static String transitionCollectionPrintFormat = TuringMachine.transitionCollectionPrintFormat;

    /**used print formats, may be changed at program start via print.properties*/
    private static String collectionRowPrintFormat = TransitionCollection.collectionRowPrintFormat;

    /**used print formats, may be changed at program start via print.properties*/
    private static String collectionRowElementPrintFormat = TransitionCollection.collectionRowElementPrintFormat;

    /**used print formats, may be changed at program start via print.properties*/
    private static String baseConfigurationPrintFormat = BaseConfiguration.baseConfigurationPrintFormat;

    /**used print formats, may be changed at program start via print.properties*/
    private static String statePrintFormat = State.statePrintFormat;

    /**used print formats, may be changed at program start via print.properties*/
    private static String transitionPrintFormat = TransitionTree.smallTransitionPrintFormat;

    /**used print formats, may be changed at program start via print.properties*/
    private static String stepPrintFormat = TransitionTree.smallStepPrintFormat;

    /**
     * Main function to simulate a TuringMachine from the commandline.<br/>
     * The TuringMachine can be provided as a JavaObjekt .tm file made by<br/>
     * the TMSim GUI or as an XML file.<br/>
     * After a successfull simulation the program terminates with exitcode 0.<br/>
     * If a critical error occurs or one or more Simulations encounter problems<br/>
     * an exception is logged and the program terminates with exitcode 1.<br/><br/>
     * <b>Usage:</b><br/>
     * java -jar [-Djava.util.logging.config.file=mylogging.properties] TMSimulator.jar {turingMachine.tm | turingMachine.tmx} [-qsr] [-t testrun.xml] [-p print.properties] [-m maxSteps]<br/><br/>
     * <b>Parameter description:</b><br/>
     * {turingMachine.tm | turingMachine.tmx} - absolute or relative path of TuringMachine definition. (if file has .tmx suffix, XML loading is performed instead of JavaObject loading)<br/>
     * -q   -   quit Simulation on TuringMachine success (e.g. one branch of the SimultationTree endet in a Finalstate)<br/>
     * -s   -   skip validation of given TuringMachine<br/>
     * -r   -   turn off Reportstyle logging (log only Testresults and Warnings)<br/>
     * -t testrun.xml   -   XML Definition of TestRuns (will be used instead of the TuringMachine´s BaseConfiguration)<br/>
     * -p print.properties  -   use custom print formats from given properties file<br/>
     * -m maxSteps  -   maximum count of Simulationsteps to take in every Simulation (will be used instead of any other defined Simulationstep count)<br/>
     * @param args the command line arguments (see above)
     */
    public static void main(String[] args) {
        argumentHandling(args);
        if (turingMachinePath.isEmpty()) usage("The TuringMachine´s filename is missing.", EXIT_FAILURE);
        if (printPropertiesFlag) {
            readProperties(printPropertiesValue);
        }
        if (maxStepsFlag) {
            try {
                defaultSteps = Integer.parseInt(maxStepsValue);
            } catch (NumberFormatException ex) {
                logger.log(Level.WARNING, "Given Maximumstep´s Optionvalue is not an Integer. -> reset to 50", ex);
                defaultSteps = 50;
            }
        }
        if (reportStyleOffFlag) logger.setLevel(Level.OFF);
        logger.info("########################## TuringMachine Simulator ##########################");
        TuringMachine turingMachine = readTuringMachine();
        logger.info("Loaded TuringMachine:");
        turingMachine.printTuringMachine(logger, Level.INFO, turingMachinePrintFormat, transitionCollectionPrintFormat, collectionRowPrintFormat, collectionRowElementPrintFormat, baseConfigurationPrintFormat, statePrintFormat);
        if (!skipValidationFlag) {
            try {
                turingMachine.validate();
            } catch (TuringMachineValidationException ex) {
                if (reportStyleOffFlag) logger.setLevel(Level.ALL);
                logger.log(Level.WARNING, "loadTuringMachineXML(File file): Validation of loaded TuringMachine failed.");
                ArrayList<Integer> flags = ex.getCheckFailed();
                for (int flag : flags) {
                    logger.log(Level.WARNING, ex.getMessageForFlag(flag));
                }
                if (reportStyleOffFlag) logger.setLevel(Level.OFF);
            }
        }
        if (testRunFlag) {
            try {
                File testRunFile = new File(testRunValue);
                ArrayList<TuringMachineTestConfiguration> testRuns = XML_IO.readTMTestConfigFromXML(testRunFile);
                for (int i = 0; i < testRuns.size(); i++) {
                    turingMachine.setBaseConfiguration(testRuns.get(i).getBaseConfiguration());
                    if (!maxStepsFlag) defaultSteps = testRuns.get(i).getMaxSteps();
                    printMandatoryInformation("###### Simulation No. " + (i + 1) + " ######");
                    runSimulation(turingMachine, testRuns.get(i).isTuringMachineSuccess());
                }
            } catch (ParserConfigurationException ex) {
                writeErrorAndExit("An Error occured while parsing the TestRuns XML-Definition file.", ex);
            } catch (SAXException ex) {
                writeErrorAndExit("An Error occured while parsing the TestRuns XML-Definition file.", ex);
            } catch (IOException ex) {
                writeErrorAndExit("An IO Error occured while trying to read the TestRuns XML-Definiton file.", ex);
            } catch (TapeOutofBoundsException ex) {
                writeErrorAndExit("One of the TestRuns seems to be incorrectly configured, one of the TapeHead´s position is outside of it´s Tape´s bounds.", ex);
            }
        } else {
            runSimulation(turingMachine, true);
        }
        System.exit(program_exitcode);
    }

    /**
     * Parses the programs arguments, sets internal flags and fetches optionvalues.<br/>
     * Valid flags:<br/>
     * TMSimulator.jar {turingMachine.tm | turingMachine.tmx} [-qsr] [-t testrun.xml] [-p print.properties] [-m maxSteps]
     * @param args the command line arguments (see above)
     */
    private static void argumentHandling(String[] args) {
        ArrayList<Option> options = new ArrayList();
        Option quitOnSuccess = new Option("q", "onQuit", "quit Simulation on TuringMachine success (e.g. one branch of the SimultationTree ends in a Finalstate)", false);
        Option skipValidation = new Option("s", "skip", "skip validation of given TuringMachine", false);
        Option reportOff = new Option("r", "reportOff", "turn off Reportstyle logging (log only Testresults and Warnings)", false);
        Option testRun = new Option("t", null, "XML Definition of TestRuns (will be used instead of the TuringMachine´s BaseConfiguration)", false, "testrun.xml", true);
        Option printProperties = new Option("p", null, "use custom print formats from given properties file", false, "print.properties", true);
        Option maxSteps = new Option("m", null, "maximum count of Simulationsteps to take in every Simulation (will be used instead of any other defined Simulationstep count)", false, "maxSteps", true);
        Option file = new Option(null, null, "absolute or relative path of TuringMachine definition. (if file has .tmx suffix, XML loading is performed instead of JavaObject loading)", true, "turingMachine.tm | turingMachine.tmx", true);
        options.add(file);
        options.add(quitOnSuccess);
        options.add(skipValidation);
        options.add(reportOff);
        options.add(testRun);
        options.add(printProperties);
        options.add(maxSteps);
        ArgumentHandler handler = new ArgumentHandler(args, options);
        handler_usage = handler.getUsage(" ");
        handler_parameters = handler.getParameter("\n");
        try {
            handler.accept();
        } catch (ArgumentHandlerException ex) {
            usage("An error occured while parsing the programm´s arguments.args.\n" + ex.getMessage(), EXIT_FAILURE);
        }
        if (handler.checkForOption(printProperties) != -1) {
            printPropertiesFlag = true;
            printPropertiesValue = handler.checkForArgumentOfOption(printProperties);
        }
        if (handler.checkForOption(maxSteps) != -1) {
            maxStepsFlag = true;
            maxStepsValue = handler.checkForArgumentOfOption(maxSteps);
        }
        if (handler.checkForOption(quitOnSuccess) != -1) {
            quitOnSuccessFlag = true;
        }
        if (handler.checkForOption(reportOff) != -1) {
            reportStyleOffFlag = true;
        }
        if (handler.checkForOption(testRun) != -1) {
            testRunFlag = true;
            testRunValue = handler.checkForArgumentOfOption(testRun);
        }
        if (handler.checkForOption(skipValidation) != -1) {
            skipValidationFlag = true;
        }
        if (handler.getOptionLessArguments().size() > 0) {
            turingMachinePath = handler.getOptionLessArguments().get(0);
        }
    }

    /**
     * Returns a TuringMachine red from the given path, if file has .tmx suffix,<br/>
     * XML loading is performed instead of JavaObject loading.<br/>
     * If an error occurs during loading, the program terminates with an error.
     * @return a TuringMachine red from the given path, if file has .tmx suffix, XML loading is performed instead of JavaObject loading.
     */
    private static TuringMachine readTuringMachine() {
        TuringMachine turingMachine = null;
        File tmFile = new File(turingMachinePath);
        if (tmFile.getName().endsWith(".tmx")) {
            try {
                turingMachine = TuringMachine.loadTuringMachineXML(tmFile);
            } catch (ParserConfigurationException ex) {
                writeErrorAndExit("An Error occured while parsing the TuringMachine XML-Definition file.", ex);
            } catch (SAXException ex) {
                writeErrorAndExit("An Error occured while parsing the TuringMachine XML-Definition file.", ex);
            } catch (IOException ex) {
                writeErrorAndExit("An IO Error occured while trying to read the TuringMachine XML-Definiton file.", ex);
            }
        } else {
            try {
                turingMachine = TuringMachine.loadTuringMachine(tmFile);
            } catch (FileNotFoundException ex) {
                writeErrorAndExit("An IO Error occured, the TuringMachine JavaObject file was not found.", ex);
            } catch (IOException ex) {
                writeErrorAndExit("An IO Error occured while trying to read the TuringMachine JavaObject file.", ex);
            } catch (ClassNotFoundException ex) {
                writeErrorAndExit("THIS ERROR SHOULD NOT HAPPEN. An Error occured while calling the java.io.ObjectInputStream.class readObject() function.", ex);
            }
        }
        return turingMachine;
    }

    /**
     * Prints the given message, even if logging is currently disabled.
     * @param msg Message to print
     */
    private static void printMandatoryInformation(String msg) {
        if (reportStyleOffFlag) logger.setLevel(Level.ALL);
        logger.info(msg);
        if (reportStyleOffFlag) logger.setLevel(Level.OFF);
    }

    /**
     * Activates the logger, writes out the given message and exception and<br/>
     * terminates the program with an error.
     * @param msg error message
     * @param ex occured exception
     */
    private static void writeErrorAndExit(String msg, Exception ex) {
        logger.setLevel(Level.ALL);
        logger.log(Level.SEVERE, msg, ex);
        System.exit(EXIT_FAILURE);
    }

    /**
     * Activates the logger, writes out the given message and exception.
     * @param msg error message
     * @param ex occured exception
     */
    private static void writeErrorWithoutExit(String msg, Exception ex) {
        logger.setLevel(Level.ALL);
        logger.log(Level.WARNING, msg, ex);
        if (reportStyleOffFlag) logger.setLevel(Level.OFF);
    }

    /**
     * Prints Usage Message and terminates the programm with exitcode 1. (e.g. Error)
     */
    private static void usage() {
        usage("", EXIT_FAILURE);
    }

    /**
     * Prints Usage Message and terminates the programm
     * @param msg Warning message to display
     */
    private static void usage(String msg, int exitcode) {
        if (!msg.isEmpty()) logger.info(msg);
        logger.log(Level.INFO, "Usage:\n" + programname + " " + handler_usage);
        logger.info("Parameter description:\n" + handler_parameters);
        System.exit(exitcode);
    }

    /**
     * Reads given Propertyfile and fetches the print parameters:<br>
     * turingMachinePrintFormat,transitionCollectionPrintFormat,collectionRowElementPrintFormat,<br/>
     * baseConfigurationPrintFormat,statePrintFormat,transitionPrintFormat,stepPrintFormat<br/>
     * @param propertiesName absolute or realtive path of propertyfile
     */
    private static void readProperties(String propertiesName) {
        FileInputStream fileStream = null;
        try {
            Properties properties = new Properties();
            fileStream = new FileInputStream(propertiesName);
            properties.load(fileStream);
            turingMachinePrintFormat = properties.getProperty("turingMachinePrintFormat", turingMachinePrintFormat);
            transitionCollectionPrintFormat = properties.getProperty("transitionCollectionPrintFormat", transitionCollectionPrintFormat);
            collectionRowElementPrintFormat = properties.getProperty("collectionRowElementPrintFormat", collectionRowElementPrintFormat);
            baseConfigurationPrintFormat = properties.getProperty("baseConfigurationPrintFormat", baseConfigurationPrintFormat);
            statePrintFormat = properties.getProperty("statePrintFormat", statePrintFormat);
            transitionPrintFormat = properties.getProperty("transitionPrintFormat", transitionPrintFormat);
            stepPrintFormat = properties.getProperty("stepPrintFormat", stepPrintFormat);
        } catch (FileNotFoundException ex) {
            writeErrorWithoutExit("An IO Error occured, the print.properties file was not found. The Programm continues with the printFormat defaults.", ex);
        } catch (IOException ex) {
            writeErrorWithoutExit("An IO Error occured while trying to read the print.properties file. The Programm continues with the printFormat defaults.", ex);
        }
    }

    /**
     * Runs a Simulation for the given TuringMachine.<br/>
     * Prints a message if the result equals the 'expected' parameter.
     * @param turingMachine to simulate
     * @param expected result of the simulation
     */
    private static void runSimulation(TuringMachine turingMachine, boolean expected) {
        logger.info("### Start Simulation ###");
        logger.info("Configuration of Simulation:");
        turingMachine.getBaseConfiguration().printBaseConfiguration(logger, Level.INFO, baseConfigurationPrintFormat);
        logger.log(Level.INFO, "Maximum of Simulationsteps: {0}", defaultSteps);
        if (Simulation.getInstance().initSimulation(turingMachine)) {
            try {
                int e = 0;
                for (e = 0; e < defaultSteps && (Simulation.getInstance().next() || !quitOnSuccessFlag); e++) {
                }
                if (Simulation.getInstance().isTMSuccess()) {
                    printMandatoryInformation("Simulation reached a Finalstate.\nSUCCESS");
                } else {
                    printMandatoryInformation("Simulation didn´t reach a Finalstate.\nFAILURE");
                }
                if (Simulation.getInstance().isTMSuccess() == expected) {
                    printMandatoryInformation("The Simulation´s result is as expected.");
                } else {
                    printMandatoryInformation("The Simulation´s result is not as expected.");
                }
                printMandatoryInformation("Needed Steps: " + e);
                logger.info("### Printing Simulation Tree ###");
                Simulation.getInstance().getTransitionTree().printTree(logger, Level.INFO, Simulation.getInstance().getTransitionTree().getRoot(), statePrintFormat, transitionPrintFormat, stepPrintFormat, baseConfigurationPrintFormat);
            } catch (SimulationNotInitiatedException ex) {
                writeErrorWithoutExit("THIS ERROR SHOULD NOT HAPPEN. The Simulation was not initalized.", ex);
                program_exitcode = EXIT_FAILURE;
            } catch (DirectionNotDefindedException ex) {
                writeErrorWithoutExit("A Transition´s Head Direction has not one of the predfined values (constants in Transition.class) and therefore has not been correctly set.", ex);
                program_exitcode = EXIT_FAILURE;
            } catch (TransitionNotContainedException ex) {
                writeErrorWithoutExit("THIS ERROR SHOULD NOT HAPPEN. During the Simulation a Transition was used that is not an element of the TuringMachine´s transitionFunction attribute.", ex);
                program_exitcode = EXIT_FAILURE;
            }
        } else {
            writeErrorWithoutExit("The initalization of the current Simulation failed.", null);
            program_exitcode = EXIT_FAILURE;
        }
        Simulation.getInstance().endSimulation();
    }
}
