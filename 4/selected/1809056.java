package org.smartgrape.tools;

import java.util.*;
import java.math.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import gnu.trove.*;

/**
 * @author planet_earth
 *
 */
public class DataUtilities {

    public static char fSep = File.separatorChar;

    public static void main(String[] args) {
        String completePath = null;
        if (args.length > 0) {
            completePath = args[0];
        } else {
            System.out.println("Please provide complete path to training_set parent folder as an argument. EXITING");
            System.exit(0);
        }
        boolean stepSuccess = true;
        stepSuccess = generateMovieCustomerRatingBinaryFile(completePath, "MovieCustomerRatingBinaryFile.txt");
        if (!stepSuccess) {
            System.out.println("Step 1:  Creation of master binary file failed. EXITING");
            System.exit(0);
        } else {
            System.out.println("Step 1:  Processing Complete. Created Master Binary file");
        }
        stepSuccess = genCustRatingFileAndMovieIndexFile(completePath, "MovieCustomerRatingBinaryFile.txt", "CustomerRatingBinaryFile.txt", "MovieIndex.txt");
        if (!stepSuccess) {
            System.out.println("Step 2:  Creation of customer rating binary file and Movie Index file failed. EXITING");
            System.exit(0);
        } else {
            System.out.println("Step 2:  Processing Complete. Created Customer Rating binary file and Movie Index file");
        }
        stepSuccess = genCustomerLocationsFileAndCustomerIndexFile(completePath, "MovieCustomerRatingBinaryFile.txt", "CustLocations.txt", "CustomerIndex.txt");
        if (!stepSuccess) {
            System.out.println("Step 3:  Creation of Movie Rating binary file and Customer Index file failed. EXITING");
            System.exit(0);
        } else {
            System.out.println("Step 3:  Processing Complete. Created Movie Rating binary file and Customer Index file");
        }
        stepSuccess = genMovieRatingFile(completePath, "MovieCustomerRatingBinaryFile.txt", "CustLocations.txt", "MovieRatingBinaryFile.txt");
        if (!stepSuccess) {
            System.out.println("Step 4:  Creation of Movie Rating binary file failed. EXITING");
            System.exit(0);
        } else {
            System.out.println("Step 4:  Processing Complete. Created Movie Rating binary file");
        }
        stepSuccess = prepareProbeFile(completePath, "formattedProbeData.txt");
        if (!stepSuccess) {
            System.out.println("Step 5:  Creation of formattedProbeData binary file failed. EXITING");
            System.exit(0);
        } else {
            System.out.println("Step 5:  Processing Complete. Created formattedProbeData binary file");
        }
        stepSuccess = prepareQualifyingFile(completePath, "formattedQualifyingData.txt");
        if (!stepSuccess) {
            System.out.println("Step 6:  Creation of formattedQualifyingData binary file failed. EXITING");
            System.exit(0);
        } else {
            System.out.println("Step 6:  Processing Complete. Created formattedQualifyingData binary file");
        }
        stepSuccess = computeMovieAverages(completePath, "movieAverageData.txt", "MovieIndex.txt");
        if (!stepSuccess) {
            System.out.println("Step 7:  Creation of movieAverageData binary file failed. EXITING");
            System.exit(0);
        } else {
            System.out.println("Step 7:  Processing Complete. Created movieAverageData binary file");
        }
        stepSuccess = computeCustomerAverages(completePath, "customerAverageData.txt", "CustomerIndex.txt");
        if (!stepSuccess) {
            System.out.println("Step 8:  Creation of customerAverageData binary file failed. EXITING");
            System.exit(0);
        } else {
            System.out.println("Step 8:  Processing Complete. Created customerAverageData binary file");
        }
    }

    /**
	 * Creates a single binary file of movie, customer and rating data
	 * from the 17770 training_set/*.txt files.
	 * The file format is 7 bytes per row with 100480506 total rows.
	 * Movie is represented as a 2 byte short, customer as a 4 byte int and
	 * rating as a 1 byte number.
	 *
	 * @param  Complete path with escaped \\ to the training_set parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains the training_set
	 * @param  File Name of output file to store results into.
	 * This file is stored in the SmartGRAPE folder
	 *
	 * @return boolean. true if successful. false otherwise
	 */
    private static boolean generateMovieCustomerRatingBinaryFile(String completePath, String outFileName) {
        try {
            File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + outFileName);
            FileChannel outC = new FileOutputStream(outFile, true).getChannel();
            File src = new File(completePath + fSep + "training_set" + fSep);
            if (src.isDirectory()) {
                File list[] = src.listFiles();
                for (int i = 0; i < list.length; i++) {
                    String fileName = list[i].getName();
                    if (!fileName.startsWith("mv_")) continue;
                    System.out.println("Processing movie file: " + fileName);
                    BufferedReader br1 = new BufferedReader(new FileReader(completePath + fSep + "training_set" + fSep + fileName));
                    boolean endOfFile = true;
                    short movieName = 0;
                    int customer = 0;
                    byte rating = 0;
                    while (endOfFile) {
                        String line = br1.readLine();
                        if (line != null) {
                            if (line.indexOf(":") >= 0) {
                                movieName = new Short(line.substring(0, line.length() - 1)).shortValue();
                            } else {
                                StringTokenizer tokens = new StringTokenizer(line, ",");
                                if (tokens.countTokens() == 3) {
                                    customer = new Integer(tokens.nextToken()).intValue();
                                    rating = new Integer(tokens.nextToken()).byteValue();
                                    ByteBuffer outBuf = ByteBuffer.allocate(7);
                                    outBuf.putShort(movieName);
                                    outBuf.putInt(customer);
                                    outBuf.put(rating);
                                    outBuf.flip();
                                    outC.write(outBuf);
                                } else {
                                    outC.close();
                                    return false;
                                }
                            }
                        } else endOfFile = false;
                    }
                    br1.close();
                }
                outC.close();
            } else {
                System.out.println("Incorrect path provided. Please provide the complete path to the training_set data files ");
                return false;
            }
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
    }

    /**
	 * Creates a binary file of customer and rating data and
	 * a file with movie index information to access that file
	 *
	 * The file format for the cust rating file is 5 bytes per row with
	 * 100480506 total rows.
	 *
	 * Customer is represented as a 4 byte int and rating as a 1 byte number.
	 *
	 * Rows are sorted by movie. i.e. thr first 547 rows correspond to movie 1
	 * the next 145 to movie 2 etc.
	 *
	 * The file format for the movie index file is 10 bytes per row
	 * with 17770 total rows (1 per movie).
	 *
	 * movie is represented as a 2 byte short, startIndex for the movie data
	 * in the cust rating file is represented as a 4 byte int and the
	 * endIndex for the movie data in the cust rating file is represented
	 * as a 4 byte int
	 *
	 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
	 * @param  File Name of  master binary file.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * @param  File Name of output file to store CustRatings into.
	 * This file is stored in the SmartGRAPE folder too
	 * @param  File Name of output file to store MovieIndex info.
	 * This file is stored in the SmartGRAPE folder too
	 *
	 * @return boolean. true if successful. false otherwise
	 */
    private static boolean genCustRatingFileAndMovieIndexFile(String completePath, String masterFile, String CustRatingFileName, String MovieIndexFileName) {
        try {
            File inFile = new File(completePath + fSep + "SmartGRAPE" + fSep + masterFile);
            FileChannel inC = new FileInputStream(inFile).getChannel();
            File outFile1 = new File(completePath + fSep + "SmartGRAPE" + fSep + MovieIndexFileName);
            FileChannel outC1 = new FileOutputStream(outFile1, true).getChannel();
            File outFile2 = new File(completePath + fSep + "SmartGRAPE" + fSep + CustRatingFileName);
            FileChannel outC2 = new FileOutputStream(outFile2, true).getChannel();
            int fileSize = (int) inC.size();
            int totalNoDataRows = fileSize / 7;
            ByteBuffer mappedBuffer = inC.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            int startIndex = 1, count = 0;
            short currentMovie = 1;
            while (mappedBuffer.hasRemaining()) {
                count++;
                short movieName = mappedBuffer.getShort();
                int customer = mappedBuffer.getInt();
                byte rating = mappedBuffer.get();
                if (movieName != currentMovie) {
                    ByteBuffer outBuf1 = ByteBuffer.allocate(10);
                    outBuf1.putShort(currentMovie);
                    outBuf1.putInt(startIndex);
                    outBuf1.putInt(count - 1);
                    outBuf1.flip();
                    outC1.write(outBuf1);
                    currentMovie = movieName;
                    startIndex = count;
                }
                ByteBuffer outBuf2 = ByteBuffer.allocate(5);
                outBuf2.putInt(customer);
                outBuf2.put(rating);
                outBuf2.flip();
                outC2.write(outBuf2);
            }
            ByteBuffer endOfIndexFile = ByteBuffer.allocate(10);
            endOfIndexFile.putShort(currentMovie);
            endOfIndexFile.putInt(startIndex);
            endOfIndexFile.putInt(100480506);
            endOfIndexFile.flip();
            outC1.write(endOfIndexFile);
            outC1.close();
            outC2.close();
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
    }

    /**
	 * Creates an intermediate binary file of customer locations
	 * which serves as an index into the master file to retrieve all the ratings
	 * for a given customer
	 *
	 * Also creates a binary file with customer index information to access the
	 * movie and rating data file generated in Step 4
	 *
	 * The file format for the intermediate file is 4 bytes per row with
	 * 100480506 total rows. Each row points to an entry in the master file
	 * that has a rating for this customer
	 *
	 * The file format for the customer index file is 12 bytes per row
	 * with 480189 total rows (1 per customer).
	 *
	 * customer is represented as a 4 byte int, startIndex for the data
	 * in the movie rating file (this file is generated after the index file is
	 * created) is represented as a 4 byte int and the
	 * endIndex for the  data in the movie rating file is represented
	 * as a 4 byte int.
	 *
	 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
	 * @param  File Name of  master binary file.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * @param  File Name of  output file to store sorted cust rating locations in master file.
	 * This file is stored in the SmartGRAPE folder too
	 * @param  File Name of output file to store CustomerIndex info.
	 * This file is stored in the SmartGRAPE folder too
	 *
	 * @return boolean. true if successful. false otherwise
	 */
    private static boolean genCustomerLocationsFileAndCustomerIndexFile(String completePath, String masterFile, String CustLocationsFileName, String CustIndexFileName) {
        try {
            TIntObjectHashMap CustInfoHash = new TIntObjectHashMap(480189, 1);
            File inFile = new File(completePath + fSep + "SmartGRAPE" + fSep + masterFile);
            FileChannel inC = new FileInputStream(inFile).getChannel();
            File outFile1 = new File(completePath + fSep + "SmartGRAPE" + fSep + CustIndexFileName);
            FileChannel outC1 = new FileOutputStream(outFile1, true).getChannel();
            File outFile2 = new File(completePath + fSep + "SmartGRAPE" + fSep + CustLocationsFileName);
            FileChannel outC2 = new FileOutputStream(outFile2, true).getChannel();
            int fileSize = (int) inC.size();
            int totalNoDataRows = fileSize / 7;
            for (int i = 1; i <= totalNoDataRows; i++) {
                ByteBuffer mappedBuffer = ByteBuffer.allocate(7);
                inC.read(mappedBuffer);
                mappedBuffer.position(0);
                short movieName = mappedBuffer.getShort();
                int customer = mappedBuffer.getInt();
                byte rating = mappedBuffer.get();
                mappedBuffer.clear();
                if (CustInfoHash.containsKey(customer)) {
                    TIntArrayList locations = (TIntArrayList) CustInfoHash.get(customer);
                    locations.add(i);
                    CustInfoHash.put(customer, locations);
                } else {
                    TIntArrayList locations = new TIntArrayList();
                    locations.add(i);
                    CustInfoHash.put(customer, locations);
                }
            }
            int[] customers = CustInfoHash.keys();
            Arrays.sort(customers);
            int count = 1;
            for (int i = 0; i < customers.length; i++) {
                int customer = customers[i];
                TIntArrayList locations = (TIntArrayList) CustInfoHash.get(customer);
                int noRatingsForCust = locations.size();
                ByteBuffer outBuf1 = ByteBuffer.allocate(12);
                outBuf1.putInt(customer);
                outBuf1.putInt(count);
                outBuf1.putInt(count + noRatingsForCust - 1);
                outBuf1.flip();
                outC1.write(outBuf1);
                count += noRatingsForCust;
                for (int j = 0; j < locations.size(); j++) {
                    ByteBuffer outBuf2 = ByteBuffer.allocate(4);
                    outBuf2.putInt(locations.get(j));
                    outBuf2.flip();
                    outC2.write(outBuf2);
                }
            }
            inC.close();
            outC1.close();
            outC2.close();
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
    }

    /**
	 * Creates a binary file of movie and rating data and. The file format for
	 * this file is 3 bytes per row with 100480506 total rows.
	 *
	 * Movie is represented as a 2 byte short and rating as a 1 byte number.
	 * and the rows are sorted by customer.
	 *
	 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
	 * @param  File Name of  master binary file.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * @param  File Name of input file with Cust locations info.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * @param  File Name of output file to store MovieRatings into.
	 * This file is stored in the SmartGRAPE folder too

	 *
	 * @return boolean. true if successful. false otherwise
	 */
    private static boolean genMovieRatingFile(String completePath, String masterFile, String CustLocationsFileName, String MovieRatingFileName) {
        try {
            File inFile1 = new File(completePath + fSep + "SmartGRAPE" + fSep + masterFile);
            FileChannel inC1 = new FileInputStream(inFile1).getChannel();
            int fileSize1 = (int) inC1.size();
            int totalNoDataRows = fileSize1 / 7;
            ByteBuffer mappedBuffer = inC1.map(FileChannel.MapMode.READ_ONLY, 0, fileSize1);
            System.out.println("Loaded master binary file");
            File inFile2 = new File(completePath + fSep + "SmartGRAPE" + fSep + CustLocationsFileName);
            FileChannel inC2 = new FileInputStream(inFile2).getChannel();
            int fileSize2 = (int) inC2.size();
            System.out.println(fileSize2);
            File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + MovieRatingFileName);
            FileChannel outC = new FileOutputStream(outFile, true).getChannel();
            for (int i = 0; i < 1; i++) {
                ByteBuffer locBuffer = inC2.map(FileChannel.MapMode.READ_ONLY, i * fileSize2, fileSize2);
                System.out.println("Loaded cust location file chunk: " + i);
                while (locBuffer.hasRemaining()) {
                    int locationToRead = locBuffer.getInt();
                    mappedBuffer.position((locationToRead - 1) * 7);
                    short movieName = mappedBuffer.getShort();
                    int customer = mappedBuffer.getInt();
                    byte rating = mappedBuffer.get();
                    ByteBuffer outBuf = ByteBuffer.allocate(3);
                    outBuf.putShort(movieName);
                    outBuf.put(rating);
                    outBuf.flip();
                    outC.write(outBuf);
                }
            }
            mappedBuffer.clear();
            inC1.close();
            inC2.close();
            outC.close();
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
    }

    /**
	 * Creates a formatted binary probe file of the following format:
	 * Movie is represented as a 2 byte short and customer as a 4 byte int.
	 *
	 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
	 * @param  File Name of  desired output file.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 *
	 * @return boolean. true if successful. false otherwise
	 */
    private static boolean prepareProbeFile(String completePath, String outputFile) {
        try {
            File inFile = new File(completePath + fSep + "probe.txt");
            FileChannel inC = new FileInputStream(inFile).getChannel();
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + outputFile);
            FileChannel outC = new FileOutputStream(outFile, true).getChannel();
            boolean endOfFile = true;
            short movieName = 0;
            int customer = 0;
            while (endOfFile) {
                String line = br.readLine();
                if (line != null) {
                    if (line.indexOf(":") >= 0) {
                        movieName = new Short(line.substring(0, line.length() - 1)).shortValue();
                    } else {
                        customer = new Integer(line).intValue();
                        ByteBuffer outBuf = ByteBuffer.allocate(6);
                        outBuf.putShort(movieName);
                        outBuf.putInt(customer);
                        outBuf.flip();
                        outC.write(outBuf);
                    }
                } else endOfFile = false;
            }
            br.close();
            outC.close();
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
    }

    /**
	 * Creates a formatted binary qualifying file of the following format:
	 * Movie is represented as a 2 byte short and customer as a 4 byte int.
	 *
	 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
	 * @param  File Name of  desired output file.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 *
	 * @return boolean. true if successful. false otherwise
	 */
    private static boolean prepareQualifyingFile(String completePath, String outputFile) {
        try {
            File inFile = new File(completePath + fSep + "qualifying.txt");
            FileChannel inC = new FileInputStream(inFile).getChannel();
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + outputFile);
            FileChannel outC = new FileOutputStream(outFile, true).getChannel();
            boolean endOfFile = true;
            short movieName = 0;
            int customer = 0;
            while (endOfFile) {
                String line = br.readLine();
                if (line != null) {
                    if (line.indexOf(":") >= 0) {
                        movieName = new Short(line.substring(0, line.length() - 1)).shortValue();
                    } else {
                        customer = new Integer(line.substring(0, line.indexOf(","))).intValue();
                        ByteBuffer outBuf = ByteBuffer.allocate(6);
                        outBuf.putShort(movieName);
                        outBuf.putInt(customer);
                        outBuf.flip();
                        outC.write(outBuf);
                    }
                } else endOfFile = false;
            }
            br.close();
            outC.close();
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
    }

    /**
	 * Computes RMSE from the predicted probe data file. This  file should have
	 * data in the following format: actual rating as 1 byte number and predicted
	 * rating as 4 byte float
	 *
	 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
	 * @param  File Name of  probedataandpredictions.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * This file is stored in the SmartGRAPE folder too
	 */
    public static void computeProbeRMSE(String completePath, String ProbeDataAndPredictionFileName) {
        try {
            File custratings = new File(completePath + fSep + "SmartGRAPE" + fSep + ProbeDataAndPredictionFileName);
            FileChannel in = new FileInputStream(custratings).getChannel();
            int filesize = (int) in.size();
            ByteBuffer mappedfile = in.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
            in.close();
            double rmse = 0;
            ;
            double squaredvalue = 0;
            double delta = 0;
            int numvalues = 0;
            while (mappedfile.hasRemaining()) {
                double prediction, actual;
                actual = new Byte(mappedfile.get()).doubleValue();
                prediction = new Float(mappedfile.getFloat()).doubleValue();
                delta = prediction - actual;
                squaredvalue += delta * delta;
                numvalues++;
            }
            rmse = Math.sqrt(squaredvalue / numvalues);
            System.out.println("The rmse for the probe data set is: " + rmse);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
	 * The output format for all the algorithms after prediction is a binary file.
	 * This is because it makes combining predictions from multiple algorithms via
	 * metal learning easier later on.
	 *
	 * However, these predictions need to be converted into a text format for submission
	 * to netflixprize. This utility method performs that conversion and prepares the
	 * file for submission in the format netflix wants.
	 *
	 * NOTE: You still need to run Netflix's check_format.pl perl script to check
	 * the content in the resulting file from this method for accuracy.
	 *
	 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
	 * @param  File Name of binary predictions that needs to be prepared for submission.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * @param  File Name of output predictions in the format needed by netflix.
	 * It is also assumed this file is in the SmartGRAPE  folder.	 *
	 **/
    public static boolean prepareSubmissionFile(String completePath, String QualifyingPredictionFileName, String SubmissionFileName) {
        try {
            TShortObjectHashMap qualMap = new TShortObjectHashMap(17770, 1);
            File custratings = new File(completePath + fSep + "SmartGRAPE" + fSep + QualifyingPredictionFileName);
            FileChannel in = new FileInputStream(custratings).getChannel();
            int filesize = (int) in.size();
            ByteBuffer mappedfile = in.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
            while (mappedfile.hasRemaining()) {
                short movie = mappedfile.getShort();
                int customer = mappedfile.getInt();
                float prediction = mappedfile.getFloat();
                if (qualMap.containsKey(movie)) {
                    TIntFloatHashMap custPredictions = (TIntFloatHashMap) qualMap.get(movie);
                    custPredictions.put(customer, prediction);
                    qualMap.put(movie, custPredictions);
                } else {
                    TIntFloatHashMap custPredictions = new TIntFloatHashMap();
                    custPredictions.put(customer, prediction);
                    qualMap.put(movie, custPredictions);
                }
            }
            short movie;
            TShortObjectIterator itr = qualMap.iterator();
            System.out.println("Populated custratings hashmap");
            File inFile = new File(completePath + fSep + "SmartGRAPE" + fSep + "qualifying.txt");
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + SubmissionFileName);
            BufferedWriter bufW = new BufferedWriter(new FileWriter(outFile));
            boolean endOfFile = true;
            short movieName = 0;
            int customer = 0;
            TIntFloatHashMap currentMoviePredictions = null;
            int decimalPlaces = 4;
            BigDecimal bd;
            while (endOfFile) {
                String line = br.readLine();
                if (line != null) {
                    if (line.indexOf(":") >= 0) {
                        movieName = new Short(line.substring(0, line.length() - 1)).shortValue();
                        bufW.write(line);
                        bufW.newLine();
                        currentMoviePredictions = (TIntFloatHashMap) qualMap.get(movieName);
                    } else {
                        customer = new Integer(line.substring(0, line.indexOf(','))).intValue();
                        float prediction = (float) currentMoviePredictions.get(customer);
                        if (prediction == prediction) {
                            bd = new BigDecimal(prediction);
                            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_UP);
                            bufW.write(new Float(bd.floatValue()).toString());
                        } else System.out.println("got a Nan");
                        bufW.newLine();
                    }
                } else endOfFile = false;
            }
            br.close();
            bufW.close();
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
    }

    /**
		 * Creates a binary file of enhanced movie average data. The file format for
		 * this file is 6 bytes per row with 17770 total rows.
		 *
		 * Movie is represented as a 2 byte short and average as a 4 byte float.
		 * and the rows are sorted by movie.
		 *
		 * Note instead of the plain average a statistical estimate of the average is
		 * used per the suggestion in simonfunk's post at:
		 *
		 * http://sifter.org/~simon/journal/20061211.html
		 * The first row in the return array contains the global count and sum across
		 * all movies *
		 * The first 17770 rows contain two doubles: the first representing the count of ratings
		 * for a movie and the second representing the sum of all ratings for that movie
		 *

		 *
		 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
		 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
		 * @param  File Name of  master binary file.
		 * It is assumed this file is in the SmartGRAPE  folder.
		 * @param  File Name of input file with Cust locations info.
		 * It is assumed this file is in the SmartGRAPE  folder.
		 * @param  File Name of output file to store MovieRatings into.
		 * This file is stored in the SmartGRAPE folder too

		 *
		 * @return boolean. true if successful. false otherwise
		 */
    public static float[] loadMovieAverages(String completePath, double K, String MovieAveragesRawDataFileName) {
        try {
            TShortObjectHashMap movieAveragesRawData = new TShortObjectHashMap(17771, 1);
            File inputFile = new File(completePath + fSep + "SmartGRAPE" + fSep + MovieAveragesRawDataFileName);
            FileChannel inC = new FileInputStream(inputFile).getChannel();
            int filesize = (int) inC.size();
            ByteBuffer mappedfile = inC.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
            short movie;
            int startIndex, endIndex;
            double globalAvg = 0;
            double count = 0, sumrating = 0;
            TDoubleArrayList a;
            for (short i = 0; i <= 17770; i++) {
                movie = mappedfile.getShort();
                count = mappedfile.getDouble();
                sumrating = mappedfile.getDouble();
                a = new TDoubleArrayList(2);
                a.add(count);
                a.add(sumrating);
                movieAveragesRawData.put(i, a);
            }
            inC.close();
            mappedfile = null;
            float[] movieAverages = new float[17771];
            TShortObjectIterator iterator = movieAveragesRawData.iterator();
            TDoubleArrayList arr;
            iterator.advance();
            arr = (TDoubleArrayList) iterator.value();
            movieAverages[0] = new Double(arr.getQuick(1) / arr.getQuick(0)).floatValue();
            System.out.println("MOVIE GLOBAL AVERAGE IS: " + movieAverages[0]);
            for (int i = 1; i <= 17770; i++) {
                iterator.advance();
                arr = (TDoubleArrayList) iterator.value();
                movieAverages[i] = new Double((movieAverages[0] * K + arr.getQuick(1)) / (K + arr.getQuick(0))).floatValue();
            }
            System.out.println("Loaded movie averages raw data");
            return movieAverages;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Creates a binary file of movie average data. The file format for
	 * this file is 6 bytes per row with 17770 total rows.
	 *
	 * Movie is represented as a 2 byte short and average as a 4 byte float.
	 * and the rows are sorted by movie.
	 *
	 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
	 * @param  File Name of  master binary file.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * @param  File Name of input file with Cust locations info.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * @param  File Name of output file to store MovieRatings into.
	 * This file is stored in the SmartGRAPE folder too

	 *
	 * @return boolean. true if successful. false otherwise
	 */
    private static boolean computeMovieAverages(String completePath, String MovieAveragesOutputFileName, String MovieIndexFileName) {
        try {
            File inputFile = new File(completePath + fSep + "SmartGRAPE" + fSep + MovieIndexFileName);
            FileChannel inC = new FileInputStream(inputFile).getChannel();
            int filesize = (int) inC.size();
            ByteBuffer mappedfile = inC.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
            TShortObjectHashMap MovieLimitsTHash = new TShortObjectHashMap(17770, 1);
            int i = 0, totalcount = 0;
            short movie;
            int startIndex, endIndex;
            TIntArrayList a;
            while (mappedfile.hasRemaining()) {
                movie = mappedfile.getShort();
                startIndex = mappedfile.getInt();
                endIndex = mappedfile.getInt();
                a = new TIntArrayList(2);
                a.add(startIndex);
                a.add(endIndex);
                MovieLimitsTHash.put(movie, a);
            }
            inC.close();
            mappedfile = null;
            System.out.println("Loaded movie index hash");
            File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + MovieAveragesOutputFileName);
            FileChannel outC = new FileOutputStream(outFile, true).getChannel();
            int totalMovies = MovieLimitsTHash.size();
            File movieMMAPDATAFile = new File(completePath + fSep + "SmartGRAPE" + fSep + "CustomerRatingBinaryFile.txt");
            inC = new FileInputStream(movieMMAPDATAFile).getChannel();
            short[] itr = MovieLimitsTHash.keys();
            Arrays.sort(itr);
            ByteBuffer buf;
            for (i = 0; i < totalMovies; i++) {
                short currentMovie = itr[i];
                a = (TIntArrayList) MovieLimitsTHash.get(currentMovie);
                startIndex = a.get(0);
                endIndex = a.get(1);
                if (endIndex > startIndex) {
                    buf = ByteBuffer.allocate((endIndex - startIndex + 1) * 5);
                    inC.read(buf, (startIndex - 1) * 5);
                } else {
                    buf = ByteBuffer.allocate(5);
                    inC.read(buf, (startIndex - 1) * 5);
                }
                buf.flip();
                int bufsize = buf.capacity() / 5;
                float sum = 0;
                for (int q = 0; q < bufsize; q++) {
                    buf.getInt();
                    sum += buf.get();
                }
                ByteBuffer outbuf = ByteBuffer.allocate(6);
                outbuf.putShort(currentMovie);
                outbuf.putFloat(sum / bufsize);
                outbuf.flip();
                outC.write(outbuf);
                buf.clear();
                buf = null;
                a.clear();
                a = null;
            }
            inC.close();
            outC.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * Creates a binary file of movie average data. The file format for
	 * this file is 6 bytes per row with 17770 total rows.
	 *
	 * Movie is represented as a 2 byte short and average as a 4 byte float.
	 * and the rows are sorted by movie.
	 *
	 * @param  Complete path with escaped \\ to the SmartGRAPE parent folder.
	 * Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains SmartGRAPE
	 * @param  File Name of  master binary file.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * @param  File Name of input file with Cust locations info.
	 * It is assumed this file is in the SmartGRAPE  folder.
	 * @param  File Name of output file to store MovieRatings into.
	 * This file is stored in the SmartGRAPE folder too

	 *
	 * @return boolean. true if successful. false otherwise
	 */
    private static boolean computeCustomerAverages(String completePath, String CustomerAveragesOutputFileName, String CustIndexFileName) {
        try {
            File inputFile = new File(completePath + fSep + "SmartGRAPE" + fSep + CustIndexFileName);
            FileChannel inC = new FileInputStream(inputFile).getChannel();
            int filesize = (int) inC.size();
            ByteBuffer mappedfile = inC.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
            TIntObjectHashMap CustomerLimitsTHash = new TIntObjectHashMap(480189, 1);
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
            File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + CustomerAveragesOutputFileName);
            FileChannel outC = new FileOutputStream(outFile, true).getChannel();
            int totalCusts = CustomerLimitsTHash.size();
            File movieMMAPDATAFile = new File(completePath + fSep + "SmartGRAPE" + fSep + "MovieRatingBinaryFile.txt");
            inC = new FileInputStream(movieMMAPDATAFile).getChannel();
            int[] itr = CustomerLimitsTHash.keys();
            startIndex = 0;
            endIndex = 0;
            a = null;
            ByteBuffer buf;
            for (int i = 0; i < totalCusts; i++) {
                int currentCust = itr[i];
                a = (TIntArrayList) CustomerLimitsTHash.get(currentCust);
                startIndex = a.get(0);
                endIndex = a.get(1);
                if (endIndex > startIndex) {
                    buf = ByteBuffer.allocate((endIndex - startIndex + 1) * 3);
                    inC.read(buf, (startIndex - 1) * 3);
                } else {
                    buf = ByteBuffer.allocate(3);
                    inC.read(buf, (startIndex - 1) * 3);
                }
                buf.flip();
                int bufsize = buf.capacity() / 3;
                float sum = 0;
                for (int q = 0; q < bufsize; q++) {
                    buf.getShort();
                    sum += buf.get();
                }
                ByteBuffer outbuf = ByteBuffer.allocate(8);
                outbuf.putInt(currentCust);
                outbuf.putFloat(sum / bufsize);
                outbuf.flip();
                outC.write(outbuf);
                buf.clear();
                buf = null;
                a.clear();
                a = null;
            }
            inC.close();
            outC.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean prepareSubmissionFile(String completePath, String QualifyingPredictionFileName) {
        try {
            TShortObjectHashMap qualMap = new TShortObjectHashMap(17770, 1);
            File custratings = new File(completePath + fSep + "SmartGRAPE" + fSep + QualifyingPredictionFileName);
            FileChannel in = new FileInputStream(custratings).getChannel();
            int filesize = (int) in.size();
            ByteBuffer mappedfile = in.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
            while (mappedfile.hasRemaining()) {
                short movie = mappedfile.getShort();
                int customer = mappedfile.getInt();
                float prediction = mappedfile.getFloat();
                if (qualMap.containsKey(movie)) {
                    TIntFloatHashMap custPredictions = (TIntFloatHashMap) qualMap.get(movie);
                    custPredictions.put(customer, prediction);
                    qualMap.put(movie, custPredictions);
                } else {
                    TIntFloatHashMap custPredictions = new TIntFloatHashMap();
                    custPredictions.put(customer, prediction);
                    qualMap.put(movie, custPredictions);
                }
            }
            short movie;
            TShortObjectIterator itr = qualMap.iterator();
            System.out.println("Populated custratings hashmap");
            File inFile = new File(completePath + fSep + "SmartGRAPE" + fSep + "qualifying.txt");
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            File outFile = new File(completePath + fSep + "SmartGRAPE" + fSep + "SubmissionFile.txt");
            BufferedWriter bufW = new BufferedWriter(new FileWriter(outFile));
            boolean endOfFile = true;
            short movieName = 0;
            int customer = 0;
            TIntFloatHashMap currentMoviePredictions = null;
            int decimalPlaces = 4;
            BigDecimal bd;
            while (endOfFile) {
                String line = br.readLine();
                if (line != null) {
                    if (line.indexOf(":") >= 0) {
                        movieName = new Short(line.substring(0, line.length() - 1)).shortValue();
                        bufW.write(line);
                        bufW.newLine();
                        currentMoviePredictions = (TIntFloatHashMap) qualMap.get(movieName);
                    } else {
                        customer = new Integer(line.substring(0, line.indexOf(','))).intValue();
                        float prediction = (float) currentMoviePredictions.get(customer);
                        if (prediction == prediction) {
                            bd = new BigDecimal(prediction);
                            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_UP);
                            bufW.write(new Float(bd.floatValue()).toString());
                        } else System.out.println("got a Nan");
                        bufW.newLine();
                    }
                } else endOfFile = false;
            }
            br.close();
            bufW.close();
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
    }
}
