package pk.edu.lums.cs5710w08.siteplanner;

import cicalculator.CellArea;
import com.CellPlan.dto.CellPlanDto;
import com.CellPlan.utility.CellPlan;
import com.CellPlan.utility.Channels;
import com.softtechdesign.ga.ChromFloat;
import com.softtechdesign.ga.Crossover;
import com.softtechdesign.ga.GAException;
import com.softtechdesign.ga.GAFloat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Midhat
 */
public class SitePlannerGA extends GAFloat {

    private static final double MIN_GAIN = 1;

    public static final double MAX_GAIN = 10;

    public static final double MIN_HEIGHT = 1;

    public static final double MAX_HEIGHT = 100;

    public static final double MIN_ORIENTATION = 0;

    public static final double MAX_ORIENTATION = 359;

    public static final double MIN_POWER = 200;

    public static final double MAX_POWER = 1000;

    public static final double MIN_C2I = 9;

    public static final double MIN_PATH_LOSS = -70;

    private cicalculator.CellArea c2i;

    private pathlossmodule.Controller pathLoss;

    private MapPoint upperLeft;

    private MapPoint lowerRight;

    private int antennaBudget;

    public int numOfxBTSs;

    private CellPlanElement[][] cellPlan;

    private String pathLossString;

    private int actualChromosomeDim;

    private NetworkInfo netInfo;

    public SitePlannerGA(MapPoint uLeft, MapPoint lRight, int aBudget, String plString, String model, String terrain, String city, CellPlanDto cellPlanDto, NetworkInfo nInfo) throws GAException {
        super(aBudget * 5, 10, 0.7, 6, 1, 0, 0, 0.1, Crossover.ctOnePoint, 10, true, true);
        upperLeft = uLeft;
        lowerRight = lRight;
        antennaBudget = aBudget;
        pathLossString = plString;
        netInfo = nInfo;
        pathLoss = new pathlossmodule.Controller(model, terrain, city);
        c2i = new CellArea();
        CellPlanDto _cellPlan = cellPlanDto;
        ArrayList<CellPlan> cellPlanArrayList = _cellPlan.getcellPlan();
        ArrayList<Channels> channelArrayList = _cellPlan.getchannels();
        cellPlan = new CellPlanElement[3][3];
        try {
            for (CellPlan plan : cellPlanArrayList) {
                if (cellPlan[(plan.getCellId() - 1)][(plan.getSectorId() - 1)] == null) {
                    CellPlanElement cellPlanElement = new CellPlanElement();
                    cellPlanElement.cellID = (plan.getCellId() - 1);
                    cellPlanElement.sectorID = plan.getSectorId() - 1;
                    for (Channels channel : channelArrayList) {
                        if (channel.getchannelId() == plan.getChannelId()) {
                            cellPlanElement.frequency = channel.getdownlinkFreq();
                            break;
                        }
                    }
                    cellPlan[(plan.getCellId() - 1)][(plan.getSectorId() - 1)] = cellPlanElement;
                }
            }
        } catch (Exception e) {
        }
        initPopulation();
    }

    @Override
    protected void initPopulation() {
        double height = lowerRight.lat - upperLeft.lat;
        double width = lowerRight.lng - upperLeft.lng;
        double totalArea = height * width;
        int numOfBTSs = antennaBudget / 3;
        double areaPerBTS = (totalArea / numOfBTSs);
        double hexagonSide = Math.sqrt(areaPerBTS / 2.598076211);
        int numOfyBTSs = (int) Math.round(height / (Math.sqrt(3) * hexagonSide));
        numOfxBTSs = (int) Math.round(numOfBTSs / numOfyBTSs);
        this.actualChromosomeDim = numOfyBTSs * numOfxBTSs * 3 * 5;
        double yDimEven = upperLeft.lat + (Math.sqrt(3) * hexagonSide) / 2;
        double xDimEven = upperLeft.lng + hexagonSide;
        double yDimOdd = upperLeft.lat + (Math.sqrt(3) * hexagonSide);
        double xDimOdd = upperLeft.lng + (5 * hexagonSide) / 2;
        double yDimEvenInit = yDimEven;
        double yDimOddInit = yDimOdd;
        double[] yOfBTSs = new double[numOfBTSs];
        double[] xOfBTSs = new double[numOfBTSs];
        int BTSindex = 0;
        for (int j = 0; j < numOfxBTSs; j++) {
            for (int k = 0; k < numOfyBTSs; k++) {
                if (j % 2 == 0) {
                    xOfBTSs[BTSindex] = xDimEven;
                    yOfBTSs[BTSindex] = yDimEven;
                    yDimEven = yDimEven + (Math.sqrt(3)) * hexagonSide;
                } else {
                    xOfBTSs[BTSindex] = xDimOdd;
                    yOfBTSs[BTSindex] = yDimOdd;
                    yDimOdd = yDimOdd + (Math.sqrt(3)) * hexagonSide;
                }
                BTSindex++;
            }
            yDimEven = yDimEvenInit;
            yDimOdd = yDimOddInit;
            if (j % 2 == 0) xDimEven = xDimEven + 3 * hexagonSide; else xDimOdd = xDimOdd + 3 * hexagonSide;
        }
        Random generator = new Random();
        int arrayIndex = 0;
        for (int i = 0; i < populationDim; i++) {
            this.chromosomes[i] = new ChromFloat(this.chromosomeDim);
            ChromFloat thischromosome = (ChromFloat) this.chromosomes[i];
            for (int iGene = 0; iGene < actualChromosomeDim; iGene++) {
                switch(iGene % 5) {
                    case 0:
                        thischromosome.genes[iGene] = yOfBTSs[(int) Math.floor(arrayIndex / 3)];
                        break;
                    case 1:
                        thischromosome.genes[iGene] = xOfBTSs[(int) Math.floor(arrayIndex / 3)];
                        arrayIndex++;
                        break;
                    case 2:
                        thischromosome.genes[iGene] = generator.nextDouble() * (MAX_POWER - MIN_POWER);
                        break;
                    case 3:
                        thischromosome.genes[iGene] = generator.nextDouble() * (MAX_GAIN - MIN_GAIN) + MIN_GAIN;
                        break;
                    case 4:
                        thischromosome.genes[iGene] = generator.nextDouble() * (MAX_HEIGHT - MIN_HEIGHT) + MIN_HEIGHT;
                        break;
                }
            }
            arrayIndex = 0;
            this.chromosomes[i].fitness = getFitness(i);
        }
    }

    @Override
    protected double getFitness(int iChromIndex) {
        ChromFloat chromosome = (ChromFloat) this.chromosomes[iChromIndex];
        double[] genes = chromosome.getGenes();
        List<Antenna> antennaList = getAntennasFromGene(genes);
        double[][] pathLossMatrix = pathLoss.getPathLoss(antennaList, pathLossString);
        double[][] c2iMatrix = c2i.getC2I(antennaList, pathLoss);
        double total = 0;
        double dark = 0;
        for (int i = 0; i < pathLossMatrix.length; i++) {
            for (int j = 0; j < pathLossMatrix.length; j++) {
                total++;
                if ((c2iMatrix[i][j] <= MIN_C2I) || (pathLossMatrix[i][j] <= MIN_PATH_LOSS)) {
                    dark++;
                }
            }
        }
        return (total - dark) / total;
    }

    @Override
    public int evolve() {
        int output = super.evolve();
        double[] genes = ((ChromFloat) this.chromosomes[this.bestFitnessChromIndex]).genes;
        List<Antenna> antennaList = getAntennasFromGene(genes);
        netInfo.setAntennaList(antennaList);
        netInfo.setPathLoss(pathLoss.getPathLoss(antennaList, pathLossString));
        netInfo.setC2i(c2i.getC2I(antennaList, pathLoss));
        return output;
    }

    private double[][] dummygetC2I() {
        double[][] pathloss = new double[90][90];
        for (int i = 0; i < 90; i++) for (int j = 0; i < 90; i++) pathloss[i][j] = 90;
        return pathloss;
    }

    private double[][] dummygetPathLoss() {
        double[][] c2i = new double[90][90];
        for (int i = 0; i < 90; i++) for (int j = 0; i < 90; i++) c2i[i][j] = 90;
        return c2i;
    }

    private List<Antenna> getAntennasFromGene(double[] genes) {
        List<Antenna> antennaList = new ArrayList<Antenna>();
        Antenna antenna = null;
        int antennaNumber = 0;
        int cellid = 0;
        int sectorid = 0;
        for (int i = 0; i < actualChromosomeDim; i++) {
            switch(i % 5) {
                case 0:
                    antenna = new Antenna();
                    antenna.setLat(genes[i]);
                    break;
                case 1:
                    antenna.setLng(genes[i]);
                    break;
                case 2:
                    antenna.setPower(genes[i]);
                    break;
                case 3:
                    antenna.setGain(genes[i]);
                    break;
                case 4:
                    antenna.setHeight(genes[i]);
                    antennaNumber++;
                    if (antennaNumber / 3 == numOfxBTSs) {
                        cellid++;
                        if (cellid >= 3) {
                            cellid = 0;
                        }
                    }
                    if (sectorid == 3) {
                        sectorid = 0;
                    }
                    antenna.setFrequency(cellPlan[cellid][sectorid].frequency);
                    antenna.setOrientation(sectorid * 120);
                    sectorid++;
                    antennaList.add(antenna);
                    break;
            }
        }
        return antennaList;
    }
}
