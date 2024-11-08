package playground.mrieser.svi.replanning;

import java.io.File;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;

/**
 * @author mrieser
 */
public class SviReplanningMain {

    private static void printUsage() {
        System.out.println("SviReplanning inPopulation outPopulation zonesDescription inMatrices outMatrices");
        System.out.println("");
        System.out.println("Arguments:");
        System.out.println("  inPopulation:     an existing MATSim plans/population file used as input.");
        System.out.println("  outPopulation:    filename of a not yet existing file where the modified");
        System.out.println("                    MATSim population is written to.");
        System.out.println("  zonesDescription: filename of an existing ESRI Shape file containing the");
        System.out.println("                    zones used to assign coordinates to zones.");
        System.out.println("  inMatrices:       name of an existing directory where matrices");
        System.out.println("                    containing travel times and other data can be found.");
        System.out.println("  outMatrices:      name of a directory where new travel demand matrices");
        System.out.println("                    are written to.");
    }

    public static void main(final String[] args) {
        if (args.length != 5) {
            printUsage();
            return;
        }
        String inputPopulationFilename = args[0];
        String outputPopulationFilename = args[1];
        String zonesFilename = args[2];
        String inputMatricesDirname = args[3];
        String outputMatricesDirname = args[4];
        if (inputPopulationFilename.equals(outputPopulationFilename)) {
            System.err.println("Input and Output population file must be different.");
            return;
        }
        File inputPopulationFile = new File(inputPopulationFilename);
        File outputPopulationFile = new File(outputPopulationFilename);
        File zonesFile = new File(zonesFilename);
        if (!inputPopulationFile.exists()) {
            System.err.println("Input population file does not exist.");
            return;
        }
        if (outputPopulationFile.exists()) {
            System.err.println("Output population file already exists. Will NOT overwrite it. Aborting.");
            return;
        }
        if (!zonesFile.exists()) {
            System.err.println("zones file does not exist.");
            return;
        }
        ShapeFileReader shpReader = new ShapeFileReader();
        shpReader.readFileAndInitialize(zonesFilename);
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
        pop.setIsStreaming(true);
        new MatsimPopulationReader(scenario).parse(inputPopulationFilename);
        System.out.println("All done.");
    }
}
