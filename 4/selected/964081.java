package org.smartgrape.cofiltering;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import gnu.trove.*;
import org.smartgrape.tools.*;

/**
 * @author planet_earth
 *
 */
public class SGPearsonWeightedSlopeOneRecommender {

    public static char fSep = File.separatorChar;

    public static int knn = 18;

    static String CustomerRatingFileName = "CustomerRatingBinaryFile.txt";

    static String MovieIndexFileName = "MovieIndex.txt";

    static String MovieRatingFileName = "MovieRatingBinaryFile.txt";

    static String CustIndexFileName = "CustomerIndex.txt";

    static TShortObjectHashMap MovieLimitsTHash;

    static TIntObjectHashMap CustomerLimitsTHash = null;

    static TShortObjectHashMap CustomersAndRatingsPerMovie;

    public static TIntObjectHashMap MoviesAndRatingsPerCustomer;

    static TShortFloatHashMap movieAverages;

    public static void main(String[] args) {
        try {
            String completePath = null;
            String predictionFileName = null;
            String CFDataFolderName = null;
            String submissionFileName = null;
            if (args.length == 4) {
                completePath = args[0];
                predictionFileName = args[1];
                CFDataFolderName = args[2];
                submissionFileName = args[3];
            } else {
                System.out.println("Please provide complete path to training_set parent folder as an argument. EXITING");
                System.exit(0);
            }
            loadMovieAverages(completePath);
            File inputFile = new File(completePath + fSep + "SmartGRAPE" + fSep + CustIndexFileName);
            FileChannel inC = new FileInputStream(inputFile).getChannel();
            int filesize = (int) inC.size();
            ByteBuffer mappedfile = inC.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
            CustomerLimitsTHash = new TIntObjectHashMap(480189, 1);
            int startIndex, endIndex;
            TIntArrayList a;
            int custid;
            while (mappedfile.hasRemaining()) {
                custid = mappedfile.getInt();
                startIndex = mappedfile.getInt();
                endIndex = mappedfile.getInt();
                a = new TIntArrayList(2);
                a.add(startIndex);
                a.add(endIndex);
                CustomerLimitsTHash.put(custid, a);
            }
            inC.close();
            mappedfile = null;
            System.out.println("Loaded customer index hash");
            if ((MoviesAndRatingsPerCustomer == null) || MoviesAndRatingsPerCustomer.size() <= 0) {
                MoviesAndRatingsPerCustomer = InitializeMovieRatingsForCustomerHashMap(completePath, CustomerLimitsTHash);
                System.out.println("Populated MoviesAndRatingsPerCustomer hashmap: ");
            }
            CustomerLimitsTHash.clear();
            CustomerLimitsTHash = null;
            boolean success = true;
            success = predictDataSet(completePath, "Probe", predictionFileName, CFDataFolderName);
            if (success) {
                System.out.println("Binary probe prediction file successfully created");
            } else {
                System.out.println("Binary probe prediction file creation failed");
            }
            DataUtilities.computeProbeRMSE(completePath, predictionFileName);
            success = predictDataSet(completePath, "Qualifying", predictionFileName, CFDataFolderName);
            if (success) {
                System.out.println("Binary qualifying prediction file successfully created");
            } else {
                System.out.println("Binary qualifying prediction file creation failed");
            }
            DataUtilities.computeProbeRMSE(completePath, predictionFileName);
            success = DataUtilities.prepareSubmissionFile(completePath, predictionFileName, submissionFileName);
            if (success) {
                System.out.println("Prediction file for submission to Netflix successfully created");
            } else {
                System.out.println("Prediction file creation for submission to Netflix failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Generates predictions for either the qualifying or probe set
	 *
	 * The count and sum of ratings diff are stored seperately instead of the average rating diff
	 * because the count is useful for a weighted average computation during predictions later on
	 *
	 * @param  Complete path with escaped \\ to the training_set parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains the training_set
	 * @param  Prediction output file name.
	 * Example: "SlopeOnePrediction.txt"
	 * @param  folder name containing slope one stat ata
	 *
	 * @return boolean indicating is predictions were made successfully
	 */
    public static boolean predictDataSet(String completePath, String Type, String predictionOutputFileName, String CFDataFolderName) {
        try {
            if (Type.equalsIgnoreCase("Qualifying")) {
                File inputFile = new File(completePath + fSep + "SmartGRAPE" + fSep + "CompleteQualifyingDataInByteFormat.txt");
                FileChannel inC = new FileInputStream(inputFile).getChannel();
                int filesize = (int) inC.size();
                TShortObjectHashMap qualMap = new TShortObjectHashMap(17770, 1);
                ByteBuffer qualmappedfile = inC.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
                while (qualmappedfile.hasRemaining()) {
                    short movie = qualmappedfile.getShort();
                    int customer = qualmappedfile.getInt();
                    if (qualMap.containsKey(movie)) {
                        TIntArrayList arr = (TIntArrayList) qualMap.get(movie);
                        arr.add(customer);
                        qualMap.put(movie, arr);
                    } else {
                        TIntArrayList arr = new TIntArrayList();
                        arr.add(customer);
                        qualMap.put(movie, arr);
                    }
                }
                System.out.println("Populated qualifying hashmap");
                File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + predictionOutputFileName);
                FileChannel outC = new FileOutputStream(outFile).getChannel();
                ByteBuffer buf;
                TShortObjectHashMap movieDiffStats;
                double finalPrediction;
                short[] movies = qualMap.keys();
                Arrays.sort(movies);
                for (int i = 0; i < movies.length; i++) {
                    short movieToProcess = movies[i];
                    movieDiffStats = loadMovieDiffStats(completePath, movieToProcess, CFDataFolderName);
                    TIntArrayList customersToProcess = (TIntArrayList) qualMap.get(movieToProcess);
                    for (int j = 0; j < customersToProcess.size(); j++) {
                        int customerToProcess = customersToProcess.getQuick(j);
                        finalPrediction = predictPearsonWeightedSlopeOneRating(knn, movieToProcess, customerToProcess, movieDiffStats);
                        if (finalPrediction == finalPrediction) {
                            if (finalPrediction < 1.0) finalPrediction = 1.0; else if (finalPrediction > 5.0) finalPrediction = 5.0;
                        } else finalPrediction = movieAverages.get(movieToProcess);
                        buf = ByteBuffer.allocate(10);
                        buf.putShort(movieToProcess);
                        buf.putInt(customerToProcess);
                        buf.putFloat(new Double(finalPrediction).floatValue());
                        buf.flip();
                        outC.write(buf);
                    }
                }
                outC.close();
                return true;
            } else if (Type.equalsIgnoreCase("Probe")) {
                File inputFile = new File(completePath + fSep + "SmartGRAPE" + fSep + "CompleteProbeDataInByteFormat.txt");
                FileChannel inC = new FileInputStream(inputFile).getChannel();
                int filesize = (int) inC.size();
                TShortObjectHashMap probeMap = new TShortObjectHashMap(17770, 1);
                ByteBuffer probemappedfile = inC.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
                while (probemappedfile.hasRemaining()) {
                    short movie = probemappedfile.getShort();
                    int customer = probemappedfile.getInt();
                    byte rating = probemappedfile.get();
                    if (probeMap.containsKey(movie)) {
                        TIntByteHashMap actualRatings = (TIntByteHashMap) probeMap.get(movie);
                        actualRatings.put(customer, rating);
                        probeMap.put(movie, actualRatings);
                    } else {
                        TIntByteHashMap actualRatings = new TIntByteHashMap();
                        actualRatings.put(customer, rating);
                        probeMap.put(movie, actualRatings);
                    }
                }
                System.out.println("Populated probe hashmap");
                File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + predictionOutputFileName);
                FileChannel outC = new FileOutputStream(outFile).getChannel();
                ByteBuffer buf;
                double finalPrediction;
                TShortObjectHashMap movieDiffStats;
                short[] movies = probeMap.keys();
                Arrays.sort(movies);
                for (int i = 0; i < movies.length; i++) {
                    short movieToProcess = movies[i];
                    movieDiffStats = loadMovieDiffStats(completePath, movieToProcess, CFDataFolderName);
                    TIntByteHashMap custRatingsToProcess = (TIntByteHashMap) probeMap.get(movieToProcess);
                    TIntArrayList customersToProcess = new TIntArrayList(custRatingsToProcess.keys());
                    for (int j = 0; j < customersToProcess.size(); j++) {
                        int customerToProcess = customersToProcess.getQuick(j);
                        byte rating = custRatingsToProcess.get(customerToProcess);
                        finalPrediction = predictPearsonWeightedSlopeOneRating(knn, movieToProcess, customerToProcess, movieDiffStats);
                        if (finalPrediction == finalPrediction) {
                            if (finalPrediction < 1.0) finalPrediction = 1.0; else if (finalPrediction > 5.0) finalPrediction = 5.0;
                        } else {
                            finalPrediction = movieAverages.get(movieToProcess);
                            System.out.println("NaN Prediction");
                        }
                        buf = ByteBuffer.allocate(11);
                        buf.putShort(movieToProcess);
                        buf.putInt(customerToProcess);
                        buf.put(rating);
                        buf.putFloat(new Double(finalPrediction).floatValue());
                        buf.flip();
                        outC.write(buf);
                    }
                }
                outC.close();
                return true;
            } else return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * Generates predictions for a given customer and movie
	 *
	 * The count and sum of ratings diff are stored seperately instead of the average rating diff
	 * because the count is useful for a weighted average computation during predictions later on
	 *
	 * @param  int no neighbors to use for knn
	 * @param  short movie for which prediction needs to be made
	 * @param  int customer id for whom prediction needs to be made
	 * @param  TShortObjectHashMap containing movie2movie diff stats for movie of interest
	 * vs all other movies
	 *
	 * @return float prediction for this customer movie pair.
	 */
    public static float predictPearsonWeightedSlopeOneRating(int kNeighbors, short movieToProcess, int customerToProcess, TShortObjectHashMap movieDiffStats) {
        try {
            float prediction = 0.0f;
            int overlapCount = 0;
            TShortByteHashMap custMovieAndRatingsMap = (TShortByteHashMap) MoviesAndRatingsPerCustomer.get(customerToProcess);
            TShortByteIterator itr = custMovieAndRatingsMap.iterator();
            int noMoviesRated = custMovieAndRatingsMap.size();
            TFloatShortHashMap neighbors = new TFloatShortHashMap();
            short movie;
            PearsonSlopeOneStats pSOS;
            double pearsonCorr = 0, zprime = 0, LCL = 0, tmp = 0;
            for (int i = 0; i < noMoviesRated; i++) {
                itr.advance();
                movie = itr.key();
                pSOS = (PearsonSlopeOneStats) movieDiffStats.get(movie);
                if (pSOS != null) {
                    overlapCount = pSOS.getCount();
                    pearsonCorr = pSOS.getpearsonR();
                    tmp = Math.exp(2 * (0.5 * Math.log((1 + pearsonCorr) / (1 - pearsonCorr)) - (1 / Math.sqrt(overlapCount - 3)) * (0.5 * Math.log((1 + 0.9) / (1 - 0.9)))));
                    zprime = 0.5 * Math.log((1 + pearsonCorr) / (1 - pearsonCorr));
                    LCL = zprime - (1.645 / Math.sqrt(overlapCount - 3));
                    pearsonCorr = Math.tanh(LCL) * Math.log(overlapCount);
                    pearsonCorr *= pearsonCorr;
                    if (pearsonCorr == pearsonCorr) {
                        neighbors.put(new Double(pearsonCorr).floatValue(), movie);
                    }
                }
            }
            int validCorrelations = neighbors.size();
            if (kNeighbors < validCorrelations) {
                float[] correlations = neighbors.keys();
                Arrays.sort(correlations);
                for (int i = 0; i < validCorrelations - kNeighbors; i++) {
                    neighbors.remove(correlations[i]);
                }
            }
            int finalNoNeighbors = neighbors.size();
            TFloatShortIterator iter = neighbors.iterator();
            float weight;
            float numerator = 0, denominator = 0, count = 0, diffRating = 0;
            while (iter.hasNext()) {
                iter.advance();
                weight = iter.key();
                movie = iter.value();
                pSOS = (PearsonSlopeOneStats) movieDiffStats.get(movie);
                count = pSOS.getCount();
                diffRating = pSOS.getDiff();
                numerator += weight * (custMovieAndRatingsMap.get(movie) + movieAverages.get(movieToProcess) - movieAverages.get(movie));
                denominator += weight;
            }
            return numerator / denominator;
        } catch (Exception e) {
            e.printStackTrace();
            return Float.NaN;
        }
    }

    /**
	 * Loads up all the CF statistics for a given movie.
	 *
	 * @param  Complete path with escaped \\ to the training_set parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains the training_set
	 * @param  short movie for which diff stats have to be loaded
	 *
	 * @return a TShortObjectHashMap [key = movie, value = slopeOneStats objects one
	 * per movie-movie pair]
	 *
	 */
    public static TShortObjectHashMap loadMovieDiffStats(String completePath, short movieToProcess, String CFDataFolderName) {
        try {
            File inFile = new File(completePath + fSep + "SmartGRAPE" + fSep + CFDataFolderName + fSep + "Movie-" + movieToProcess + "-MatrixData.txt");
            FileChannel inC = new FileInputStream(inFile).getChannel();
            TShortObjectHashMap returnMap = new TShortObjectHashMap(17770, 1);
            int size = (int) inC.size();
            ByteBuffer mapped = inC.map(FileChannel.MapMode.READ_ONLY, 0, size);
            short otherMovie;
            int count;
            float diffRating, sumXY, sumX, sumY, sumX2, sumY2, pearsonCorr, adjustedCosineCorr, cosineCorr;
            while (mapped.hasRemaining()) {
                otherMovie = mapped.getShort();
                count = mapped.getInt();
                diffRating = mapped.getFloat();
                sumXY = mapped.getFloat();
                sumX = mapped.getFloat();
                sumY = mapped.getFloat();
                sumX2 = mapped.getFloat();
                sumY2 = mapped.getFloat();
                pearsonCorr = mapped.getFloat();
                adjustedCosineCorr = mapped.getFloat();
                cosineCorr = mapped.getFloat();
                PearsonSlopeOneStats newD = new PearsonSlopeOneStats(count, diffRating, pearsonCorr, adjustedCosineCorr, 0);
                returnMap.put(otherMovie, newD);
            }
            inC.close();
            return returnMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void loadMovieAverages(String completePath) {
        try {
            File infile = new File(completePath + fSep + "SmartGRAPE" + fSep + "movieAverageData.txt");
            FileChannel inC = new FileInputStream(infile).getChannel();
            int size = (int) inC.size();
            ByteBuffer map = inC.map(FileChannel.MapMode.READ_ONLY, 0, size);
            movieAverages = new TShortFloatHashMap(17770, 1);
            inC.close();
            while (map.hasRemaining()) {
                movieAverages.put(map.getShort(), map.getFloat());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Returns a list of all customers who saw a particular movie and the rating
	 * they assigned that movie.
	 *
	 * @param  Complete path with escaped \\ to the training_set parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains the training_set
	 * @param  movieLimitsTHash that contains the start and end index for each movie
	 * into the customer rating binary file.
	 *
	 * @return a TShortObjectHashMap [key = movie, value = TIntByteHashMap[key=customer, value = rating]]
	 * with all the ratings for a movie
	 */
    public static TShortObjectHashMap InitializeCustomerRatingsForMovieHashMap(String completePath, TShortObjectHashMap MovieLimitsTHash) {
        try {
            TShortObjectHashMap returnMap = new TShortObjectHashMap(MovieLimitsTHash.size(), 1);
            int totalMovies = MovieLimitsTHash.size();
            File movieMMAPDATAFile = new File(completePath + fSep + "SmartGRAPE" + fSep + CustomerRatingFileName);
            FileChannel inC = new FileInputStream(movieMMAPDATAFile).getChannel();
            short[] itr = MovieLimitsTHash.keys();
            int startIndex = 0;
            int endIndex = 0;
            TIntArrayList a = null;
            TIntByteHashMap result;
            ByteBuffer buf;
            for (int i = 0; i < totalMovies; i++) {
                if (i % 1000 == 0) System.out.println("Loade movie: " + i);
                short currentMovie = itr[i];
                a = (TIntArrayList) MovieLimitsTHash.get(currentMovie);
                startIndex = a.get(0);
                endIndex = a.get(1);
                if (endIndex > startIndex) {
                    result = new TIntByteHashMap(endIndex - startIndex + 1, 1);
                    buf = ByteBuffer.allocate((endIndex - startIndex + 1) * 5);
                    inC.read(buf, (startIndex - 1) * 5);
                } else {
                    result = new TIntByteHashMap(1, 1);
                    buf = ByteBuffer.allocate(5);
                    inC.read(buf, (startIndex - 1) * 5);
                }
                buf.flip();
                int bufsize = buf.capacity() / 5;
                for (int q = 0; q < bufsize; q++) {
                    result.put(buf.getInt(), buf.get());
                }
                returnMap.put(currentMovie, result.clone());
                buf.clear();
                buf = null;
                a.clear();
                a = null;
            }
            inC.close();
            return returnMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Returns a list of all movies and ratings sorted by customers.
	 *
	 * @param  Complete path with escaped \\ to the training_set parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains the training_set
	 * @param  TIntObjectHashMap with the indexes to retrieve all movies and ratings
	 * for a specific customer.
	 *
	 * @return a TIntObjectHashMap [key = customer, value = TShortByteHashMap[key=movie, value = rating]]
	 * with all the ratings for a movie
	 */
    public static TIntObjectHashMap InitializeMovieRatingsForCustomerHashMap(String completePath, TIntObjectHashMap custList) {
        try {
            TIntObjectHashMap returnMap = new TIntObjectHashMap(custList.size(), 1);
            int totalCusts = custList.size();
            File movieMMAPDATAFile = new File(completePath + fSep + "SmartGRAPE" + fSep + MovieRatingFileName);
            FileChannel inC = new FileInputStream(movieMMAPDATAFile).getChannel();
            int[] itr = custList.keys();
            int startIndex = 0;
            int endIndex = 0;
            TIntArrayList a = null;
            TShortByteHashMap result;
            ByteBuffer buf;
            for (int i = 0; i < totalCusts; i++) {
                if (i % 100000 == 0) System.out.println("Loaded a 4th of the data");
                int currentCust = itr[i];
                a = (TIntArrayList) custList.get(currentCust);
                startIndex = a.get(0);
                endIndex = a.get(1);
                if (endIndex > startIndex) {
                    result = new TShortByteHashMap(endIndex - startIndex + 1, 1);
                    buf = ByteBuffer.allocate((endIndex - startIndex + 1) * 3);
                    inC.read(buf, (startIndex - 1) * 3);
                } else {
                    result = new TShortByteHashMap(1, 1);
                    buf = ByteBuffer.allocate(3);
                    inC.read(buf, (startIndex - 1) * 3);
                }
                buf.flip();
                int bufsize = buf.capacity() / 3;
                for (int q = 0; q < bufsize; q++) {
                    result.put(buf.getShort(), buf.get());
                }
                returnMap.put(currentCust, result.clone());
                buf.clear();
                buf = null;
                a.clear();
                a = null;
            }
            return returnMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

class PearsonSlopeOneStats {

    int count;

    float diff;

    float pearsonR;

    float cosineR;

    float adjcosineR;

    public PearsonSlopeOneStats() {
        count = 0;
        diff = 0;
        pearsonR = 0;
        cosineR = 0;
        adjcosineR = 0;
    }

    public PearsonSlopeOneStats(int tcount, float tdiff, float tpearsonR, float tadjcosineR, float tcosineR) {
        count = tcount;
        diff = tdiff;
        pearsonR = tpearsonR;
        adjcosineR = tadjcosineR;
        cosineR = tcosineR;
    }

    public int getCount() {
        return count;
    }

    public float getDiff() {
        return diff;
    }

    public float getpearsonR() {
        return pearsonR;
    }

    public float getcosineR() {
        return cosineR;
    }

    public float getadjcosineR() {
        return adjcosineR;
    }
}
