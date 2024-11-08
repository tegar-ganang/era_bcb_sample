package org.smartgrape.cofiltering;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import gnu.trove.*;

/**
 * @author Vivek Sagi
 *
 */
public class SGCorrelationCalc {

    public static char fSep = File.separatorChar;

    public static int knn = 18;

    static String completePath;

    static String CustomerRatingFileName = "CustomerRatingBinaryFile.txt";

    static String MovieIndexFileName = "MovieIndex.txt";

    static String MovieRatingFileName = "MovieRatingBinaryFile.txt";

    static String CustIndexFileName = "CustomerIndex.txt";

    static TShortObjectHashMap MovieLimitsTHash = null;

    static TIntObjectHashMap CustomerLimitsTHash = null;

    static TIntObjectHashMap MoviesAndRatingsPerCustomer = null;

    static TShortObjectHashMap CustomersAndRatingsPerMovie = null;

    static TIntObjectHashMap mData;

    static TShortObjectHashMap mDiffMatrix;

    static TShortObjectHashMap mFreqMatrix;

    public static void main(String args[]) {
        try {
            if (args.length == 1) {
                completePath = args[0];
            } else {
                System.out.println("Please provide complete path to training_set parent folder as an argument. EXITING");
                System.exit(0);
            }
            if ((CustomersAndRatingsPerMovie == null) || CustomersAndRatingsPerMovie.size() <= 0) {
                CustomersAndRatingsPerMovie = InitializeCustomerRatingsForMovieHashMap();
                System.out.println("Populated CustomersAndRatingsPerMovie hashmap");
            }
            boolean stepSuccess = true;
            stepSuccess = buildCFItem2ItemStats("MovieToMovieCFCorrelationData.txt", "movieAverageData.txt", "customerAverageData.txt");
            if (!stepSuccess) {
                System.out.println("Step 1:  Creation of CF Stats failed. EXITING");
                System.exit(0);
            } else {
                System.out.println("Step 1:  Processing Complete. Created Master Binary file");
            }
            stepSuccess = buildPerMovieDiffBinary("MovieToMovieCFCorrelationData.txt");
            if (!stepSuccess) {
                System.out.println("Step 2:  Combining master files failed. EXITING");
                System.exit(0);
            } else {
                System.out.println("Step 2:  Processing Complete. Created Master Binary file");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
* Incrementally builds a single binary file with all the standard CF movie-movie statistics
* for these algorithms:
* 1) SlopeOne.
* 2) Pearson Correlation
* 3) Cosine Similarity
* 4) Adjusted Cosine Similariy
*
*
* Format of output binary file is 44 bytes per row with the following data per row:
*
* 		2 bytes for movie 1,
* 		2 bytes for movie 2,
* 		4 bytes for the count of common ratings between these movies,
* 		4 bytes for the sum of differences between the ratings of the two movies
* 		4 bytes for the sum X*Y of common ratings between these movies,
* 		4 bytes for the sum X of ratings for movie 1
* 		4 bytes for the sum y of ratings for movie 2
* 		4 bytes for the sum X*X of ratings for movie 1
* 		4 bytes for the sum y*y of ratings for movie 2
* 		4 bytes for the Pearson Correlation coefficient between  movie 1 and movie 2
* 		4 bytes for the Adjusted Cosine Correlation coefficient between  movie 1 and movie 2
* 		4 bytes for the Cosine Correlation coefficient between  movie 1 and movie 2
*
* There will be 17769*17770/2 = 157,877,565 rows of data in the final output file that capture
* all the correlation stats for every unique movie combination
*
* @param  Complete path with escaped \\ to the training_set parent folder.
* Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains the training_set
* @param  Complete path with escaped \\ to the parent folder where the output statistics file is to
* be created.
 Example: "C:\\DOWNLOADS\\NetflixData\\SmartGrape\\SlopeOneStats
* @param  file name of output file for slope one stat data
*
* @return void
*/
    public static boolean buildCFItem2ItemStats(String outFileName, String movieAvgFileName, String custAvgFileName) {
        try {
            File infile = new File(completePath + fSep + "SmartGRAPE" + fSep + movieAvgFileName);
            FileChannel inC = new FileInputStream(infile).getChannel();
            int size = (int) inC.size();
            ByteBuffer map = inC.map(FileChannel.MapMode.READ_ONLY, 0, size);
            TShortFloatHashMap movieAverages = new TShortFloatHashMap(17770, 1);
            inC.close();
            while (map.hasRemaining()) {
                movieAverages.put(map.getShort(), map.getFloat());
            }
            map = null;
            infile = new File(completePath + fSep + "SmartGRAPE" + fSep + custAvgFileName);
            inC = new FileInputStream(infile).getChannel();
            size = (int) inC.size();
            map = inC.map(FileChannel.MapMode.READ_ONLY, 0, size);
            TIntFloatHashMap custAverages = new TIntFloatHashMap(480189, 1);
            inC.close();
            while (map.hasRemaining()) {
                custAverages.put(map.getInt(), map.getFloat());
            }
            File outfile = new File(completePath + fSep + "SmartGRAPE" + fSep + outFileName);
            FileChannel outC = new FileOutputStream(outfile, true).getChannel();
            short[] movies = CustomersAndRatingsPerMovie.keys();
            Arrays.sort(movies);
            int noMovies = movies.length;
            for (int i = 0; i < noMovies - 1; i++) {
                short movie1 = movies[i];
                TIntByteHashMap testMovieCustAndRatingsMap = (TIntByteHashMap) CustomersAndRatingsPerMovie.get(movie1);
                int[] customers1 = testMovieCustAndRatingsMap.keys();
                Arrays.sort(customers1);
                System.out.println("Processing movie: " + movie1);
                for (int j = i + 1; j < noMovies; j++) {
                    short movie2 = movies[j];
                    TIntByteHashMap otherMovieCustAndRatingsMap = (TIntByteHashMap) CustomersAndRatingsPerMovie.get(movie2);
                    int[] customers2 = otherMovieCustAndRatingsMap.keys();
                    TIntArrayList intersectSet = CustOverLapForTwoMoviesCustom(customers1, customers2);
                    int count = 0;
                    float diffRating = 0;
                    float pearsonCorr = 0;
                    float cosineCorr = 0;
                    float adjustedCosineCorr = 0;
                    float sumX = 0;
                    float sumY = 0;
                    float sumXY = 0;
                    float sumX2 = 0;
                    float sumY2 = 0;
                    float sumXYPearson = 0;
                    float sumX2Pearson = 0;
                    float sumY2Pearson = 0;
                    float sumXYACos = 0;
                    float sumX2ACos = 0;
                    float sumY2ACos = 0;
                    if ((intersectSet.size() == 0) || (intersectSet == null)) {
                        count = 0;
                        diffRating = 0;
                    } else {
                        count = intersectSet.size();
                        for (int l = 0; l < count; l++) {
                            int commonCust = intersectSet.getQuick(l);
                            byte ratingX = testMovieCustAndRatingsMap.get(commonCust);
                            sumX += ratingX;
                            byte ratingY = otherMovieCustAndRatingsMap.get(commonCust);
                            sumY += ratingY;
                            sumX2 += ratingX * ratingX;
                            sumY2 += ratingY * ratingY;
                            sumXY += ratingX * ratingY;
                            diffRating += ratingX - ratingY;
                            sumXYPearson += (ratingX - movieAverages.get(movie1)) * (ratingY - movieAverages.get(movie2));
                            sumX2Pearson += Math.pow((ratingX - movieAverages.get(movie1)), 2);
                            sumY2Pearson += Math.pow((ratingY - movieAverages.get(movie2)), 2);
                            float custAverage = custAverages.get(commonCust);
                            sumXYACos += (ratingX - custAverage) * (ratingY - custAverage);
                            sumX2ACos += Math.pow((ratingX - custAverage), 2);
                            sumY2ACos += Math.pow((ratingY - custAverage), 2);
                        }
                    }
                    double pearsonDenominator = Math.sqrt(sumX2Pearson) * Math.sqrt(sumY2Pearson);
                    if (pearsonDenominator == 0.0) {
                        pearsonCorr = 0;
                    } else {
                        pearsonCorr = new Double(sumXYPearson / pearsonDenominator).floatValue();
                    }
                    double adjCosineDenominator = Math.sqrt(sumX2ACos) * Math.sqrt(sumY2ACos);
                    if (adjCosineDenominator == 0.0) {
                        adjustedCosineCorr = 0;
                    } else {
                        adjustedCosineCorr = new Double(sumXYACos / adjCosineDenominator).floatValue();
                    }
                    double cosineDenominator = Math.sqrt(sumX2) * Math.sqrt(sumY2);
                    if (cosineDenominator == 0.0) {
                        cosineCorr = 0;
                    } else {
                        cosineCorr = new Double(sumXY / cosineDenominator).floatValue();
                    }
                    ByteBuffer buf = ByteBuffer.allocate(44);
                    buf.putShort(movie1);
                    buf.putShort(movie2);
                    buf.putInt(count);
                    buf.putFloat(diffRating);
                    buf.putFloat(sumXY);
                    buf.putFloat(sumX);
                    buf.putFloat(sumY);
                    buf.putFloat(sumX2);
                    buf.putFloat(sumY2);
                    buf.putFloat(pearsonCorr);
                    buf.putFloat(adjustedCosineCorr);
                    buf.putFloat(cosineCorr);
                    buf.flip();
                    outC.write(buf);
                    buf.clear();
                }
            }
            outC.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
* Splits the single binary file generated by the buildDiffMatrix method into 17770 individual files
* one per movie for easy access during prediction generation
*
* The format of each output binary file is 42 bytes per row with the following data per row:
*
* Format of output binary file is 44 bytes per row with the following data per row:
*
* 		2 bytes for movie 2,
* 		4 bytes for the count of common ratings between these movies,
* 		4 bytes for the sum of differences between the ratings of the two movies
* 		4 bytes for the sum X*Y of common ratings between these movies,
* 		4 bytes for the sum X of ratings for movie 1
* 		4 bytes for the sum y of ratings for movie 2
* 		4 bytes for the sum X*X of ratings for movie 1
* 		4 bytes for the sum y*y of ratings for movie 2
* 		4 bytes for the Pearson Correlation coefficient between  movie 1 and movie 2
* 		4 bytes for the Adjusted Cosine Correlation coefficient between  movie 1 and movie 2
* 		4 bytes for the Cosine Correlation coefficient between  movie 1 and movie 2
* Each file will have 17770 rows of data that capture all the slope one stats for every unique movie
* combination. The corrletaion data for a movie compared to iteself is set to 0 as a dummy data
*
* @param  Complete path with escaped \\ to the training_set parent folder.
* Example: "C:\\DOWNLOADS\\NetflixData if NetflixData contains the training_set
* @param  Complete path with escaped \\ to the parent folder where the output statistics file is to
* be created.
* Example: "C:\\DOWNLOADS\\NetflixData\\SmartGrape\\SlopeOneStats
* @param  file name of output file for slope one stat data
*
* @return boolean */
    public static boolean buildPerMovieDiffBinary(String masterFile) {
        try {
            File inFile = new File(completePath + fSep + "SmartGRAPE" + fSep + masterFile);
            FileChannel inC = new FileInputStream(inFile).getChannel();
            System.out.println(inC.size());
            short movie1, movie2;
            int count;
            float diffRating, sumXY, sumX, sumY, sumX2, sumY2, pearsonCorr, adjustedCosineCorr, cosineCorr;
            long position;
            for (long i = 1; i < 17770; i++) {
                File outFile = new File("C:\\NetflixData\\download\\SmartGrape\\CFItemToItemStats\\Movie--" + i + "-MatrixData.txt");
                FileChannel outC = new FileOutputStream(outFile, true).getChannel();
                ByteBuffer buf = ByteBuffer.allocate(17770 * 44);
                for (long j = 1; j < i; j++) {
                    ByteBuffer bbuf = ByteBuffer.allocate(44);
                    position = 0;
                    position += new Long(17769).longValue() * new Long(17770).longValue() * new Long(22).longValue();
                    position -= new Long((17769 - (j - 1))).longValue() * new Long((17770 - (j - 1))).longValue() * new Long(22).longValue();
                    position += new Long((i - j - 1) * 44).longValue();
                    inC.position(position);
                    inC.read(bbuf);
                    bbuf.flip();
                    buf.putShort(bbuf.getShort());
                    bbuf.getShort();
                    buf.putInt(bbuf.getInt());
                    buf.putFloat(-bbuf.getInt());
                    buf.putFloat(bbuf.getFloat());
                    buf.putFloat(bbuf.getFloat());
                    buf.putFloat(bbuf.getFloat());
                    buf.putFloat(bbuf.getFloat());
                    buf.putFloat(bbuf.getFloat());
                    buf.putFloat(bbuf.getFloat());
                    buf.putFloat(bbuf.getFloat());
                    buf.putFloat(bbuf.getFloat());
                }
                buf.putShort(new Long(i).shortValue());
                buf.putInt(0);
                buf.putFloat(0);
                buf.putFloat(0);
                buf.putFloat(0);
                buf.putFloat(0);
                buf.putFloat(0);
                buf.putFloat(0);
                buf.putFloat(0);
                buf.putFloat(0);
                buf.putFloat(0);
                position = 0;
                position += new Long(17769).longValue() * new Long(17770).longValue() * new Long(22).longValue();
                position -= new Long((17769 - (i - 1))).longValue() * new Long((17770 - (i - 1))).longValue() * new Long(22).longValue();
                ByteBuffer remainingBuf = inC.map(FileChannel.MapMode.READ_ONLY, position, (17770 - i) * 44);
                while (remainingBuf.hasRemaining()) {
                    remainingBuf.getShort();
                    buf.putShort(remainingBuf.getShort());
                    buf.putInt(remainingBuf.getInt());
                    buf.putFloat(remainingBuf.getFloat());
                    buf.putFloat(remainingBuf.getFloat());
                    buf.putFloat(remainingBuf.getFloat());
                    buf.putFloat(remainingBuf.getFloat());
                    buf.putFloat(remainingBuf.getFloat());
                    buf.putFloat(remainingBuf.getFloat());
                    buf.putFloat(remainingBuf.getFloat());
                    buf.putFloat(remainingBuf.getFloat());
                    buf.putFloat(remainingBuf.getFloat());
                }
                buf.flip();
                outC.write(buf);
                buf.clear();
                outC.close();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void CustOverLapForTwoMovies(int[] Customers1, TIntHashSet Customers2) {
        try {
            Customers2.retainAll(Customers1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static TIntArrayList CustOverLapForTwoMoviesCustom(int[] Customers1, int[] Customers2) {
        try {
            Arrays.sort(Customers2);
            TIntArrayList returnVal = new TIntArrayList();
            int size1 = Customers1.length;
            int size2 = Customers2.length;
            if (size1 > size2) {
                for (int i = 0; i < size1; i++) {
                    if (Arrays.binarySearch(Customers2, Customers1[i]) > 0) {
                        returnVal.add(Customers1[i]);
                    }
                }
            } else {
                for (int i = 0; i < size2; i++) {
                    if (Arrays.binarySearch(Customers1, Customers2[i]) > 0) {
                        returnVal.add(Customers2[i]);
                    }
                }
            }
            return returnVal;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static TIntObjectHashMap InitializeMovieRatingsForCustomerHashMap(TIntObjectHashMap custList) {
        try {
            TIntObjectHashMap returnMap = new TIntObjectHashMap(custList.size(), 1);
            int totalCusts = custList.size();
            File movieMMAPDATAFile = new File(completePath + fSep + "SmartGRAPE" + fSep + "MovieRatingBinaryFile.txt");
            FileChannel inC = new FileInputStream(movieMMAPDATAFile).getChannel();
            int[] itr = custList.keys();
            int startIndex = 0;
            int endIndex = 0;
            TIntArrayList a = null;
            TShortByteHashMap result;
            ByteBuffer buf;
            for (int i = 0; i < totalCusts; i++) {
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

    public static TShortObjectHashMap InitializeCustomerRatingsForMovieHashMap() {
        try {
            File inputFile = new File(completePath + fSep + "SmartGRAPE" + fSep + MovieIndexFileName);
            FileChannel inC = new FileInputStream(inputFile).getChannel();
            int filesize = (int) inC.size();
            ByteBuffer mappedfile = inC.map(FileChannel.MapMode.READ_ONLY, 0, filesize);
            MovieLimitsTHash = new TShortObjectHashMap(17770, 1);
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
            mappedfile.clear();
            mappedfile = null;
            System.out.println("Loaded movie index hash");
            a = null;
            TShortObjectHashMap returnMap = new TShortObjectHashMap(MovieLimitsTHash.size(), 1);
            int totalMovies = MovieLimitsTHash.size();
            File movieMMAPDATAFile = new File(completePath + fSep + "SmartGRAPE" + fSep + "CustomerRatingBinaryFile.txt");
            inC = new FileInputStream(movieMMAPDATAFile).getChannel();
            short[] itr = MovieLimitsTHash.keys();
            Arrays.sort(itr);
            TIntByteHashMap result;
            ByteBuffer buf;
            for (i = 1610; i < totalMovies; i++) {
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
}
