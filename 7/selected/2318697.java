package org.amlfilter.service;

/**
 * Implements the basic comparison between names at the word level.
 * What it does:
 * 	<li>Creates a similarity map between both names.
 * 	<li>Guesses the best matching between the tokens in the sim map.
 * 	<li>Computes the overall similarty.
 * 	<li>Returns the value.
 * <br /><br /><br />
 * @author Marco
 * @version $Id: TextSimilarityMappingPath.java sss Exp $
 */
public final class TextSimilarityMappingPath implements Cloneable {

    public int size = 0;

    public int[] A = new int[20];

    public int[] B = new int[20];

    public float[] similarity = new float[20];

    public float totalSimilarityWeight = 0f;

    public float totalBLWeight = 0f;

    public float relativeWeightedSimilarity = 0f;

    /**
	 * Clones the MappingPath into a new one.
	 */
    public TextSimilarityMappingPath clone() {
        TextSimilarityMappingPath nMP = new TextSimilarityMappingPath();
        nMP.size = size;
        for (int i = 0; i < size; i++) {
            nMP.A[i] = A[i];
            nMP.B[i] = B[i];
            nMP.similarity[i] = similarity[i];
        }
        nMP.totalSimilarityWeight = totalSimilarityWeight;
        nMP.relativeWeightedSimilarity = relativeWeightedSimilarity;
        nMP.totalBLWeight = totalBLWeight;
        return nMP;
    }

    /**
	 * Clones and resizes the mp array, int version.
	 * 
	 * @param pArray
	 * @return
	 */
    protected int[] clone_and_resize_intArray(int[] pArray) {
        int newArraySize = pArray.length * 2;
        int[] new_array = new int[newArraySize];
        System.arraycopy(pArray, pArray.length, new_array, new_array.length, 0);
        return new_array;
    }

    /**
	 * Clones and resizes the mp array, float version.
	 * 
	 * @param pSim_array
	 * @return
	 */
    protected float[] clone_and_resize_floatArray(float[] pSim_array) {
        int newArraySize = pSim_array.length * 2;
        float[] new_sim_array = new float[newArraySize];
        System.arraycopy(pSim_array, pSim_array.length, new_sim_array, new_sim_array.length, 0);
        return new_sim_array;
    }

    /**
	 * Clones the MappingPath into a new one and
	 * 	checks if there is enough room for it in the array.
	 * If not enough room, makes more.
	 * 
	 * @param pMpArray
	 * @param mpCount
	 * @return
	 */
    public TextSimilarityMappingPath[] cloneAndMakeRoomInArray(TextSimilarityMappingPath[] pMpArray, int mpCount) {
        int newMpArraySize = pMpArray.length * 2;
        TextSimilarityMappingPath[] newMpArray = null;
        if (pMpArray.length < (mpCount + 2)) {
            newMpArray = new TextSimilarityMappingPath[newMpArraySize];
            for (int i = 0; i < mpCount; i++) {
                newMpArray[i] = pMpArray[i];
            }
        } else {
            newMpArray = pMpArray;
        }
        if (size > A.length - 3) {
            A = clone_and_resize_intArray(A);
            B = clone_and_resize_intArray(B);
            similarity = clone_and_resize_floatArray(similarity);
        }
        newMpArray[mpCount] = clone();
        return newMpArray;
    }

    /**
	 * Returns true if the MappingPath already has that element 
	 * 	stored for the B name (the second one).
	 * 
	 * @param valOf_A_ToCheck
	 * @param startingPosition
	 * @return
	 */
    public int getprevious_B_PositionForTheSame_A(int valOf_A_ToCheck, int startingPosition) {
        if (startingPosition > 0) {
            for (int i = startingPosition - 1; i >= 0; i--) {
                if (A[i] == valOf_A_ToCheck) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
	 * Returns the position of a target name in the array.
	 * 
	 * @param b
	 * @return
	 */
    public int getBlWordUsagePosition(int b) {
        for (int i = 0; i < size - 1; i++) {
            if (B[i] == b) {
                return i;
            }
            if (B[i] > b) {
                return -1;
            }
        }
        return -1;
    }

    /**
	 * Method for retrieving the best match.
	 * 
	 * This method implements a more efficient method for computing the 
	 * 	best path to the token matching, without recursiveness.
	 * It replaces the deprecated one: populate
	 * 
	 * 
	 * @param mappingPaths
	 * @param pMappingPathsCount
	 * @param max_A_Val
	 * @param max_B_Val
	 * @param pSimilarityArray
	 * @param pAlreadyFishedInThisRow_Column
	 * @param pBlackListWeights
	 * @return
	 */
    public TextSimilarityMappingPath[] populateDirectly(TextSimilarityMappingPath[] mappingPaths, int pMappingPathsCount, int max_A_Val, int max_B_Val, float[][] pSimilarityArray, int pAlreadyFishedInThisRow_Column, float[] pBlackListWeights) {
        boolean smallerIsFirst = false;
        if (pSimilarityArray[0].length > pSimilarityArray.length) {
            smallerIsFirst = true;
        }
        float[][] simArray = null;
        if (!smallerIsFirst) {
            int firstLen = pSimilarityArray.length;
            int secondLen = pSimilarityArray[0].length;
            simArray = new float[secondLen][firstLen];
            for (int i = 0; i < firstLen; i++) {
                for (int j = 0; j < secondLen; j++) {
                    simArray[j][i] = pSimilarityArray[i][j];
                }
            }
        } else {
            simArray = pSimilarityArray;
        }
        int smallName_len = simArray.length;
        int bigName_len = simArray[0].length;
        size = 0;
        A = new int[smallName_len];
        B = new int[smallName_len];
        similarity = new float[smallName_len];
        boolean[] smallNamePos_Taken = new boolean[smallName_len];
        boolean[] bigNamePos_Taken = new boolean[bigName_len];
        for (int i = 0; i < smallName_len; i++) {
            smallNamePos_Taken[i] = false;
        }
        for (int j = 0; j < bigName_len; j++) {
            bigNamePos_Taken[j] = false;
        }
        float maxValTilNow = 0f;
        int a = -1;
        int b = -1;
        for (int pass = 0; pass < smallName_len; pass++) {
            maxValTilNow = -1f;
            a = -1;
            b = -1;
            for (int i = 0; i < smallName_len; i++) {
                if (!smallNamePos_Taken[i]) {
                    for (int j = 0; j < bigName_len; j++) {
                        if (!bigNamePos_Taken[j]) {
                            if (simArray[i][j] > maxValTilNow) {
                                maxValTilNow = simArray[i][j];
                                a = i;
                                b = j;
                            }
                        }
                    }
                }
            }
            for (int j = 0; j < bigName_len; j++) {
                if (!bigNamePos_Taken[j]) {
                    for (int i = 0; i < smallName_len; i++) {
                        if (!smallNamePos_Taken[i]) {
                            if (simArray[i][j] > maxValTilNow) {
                                maxValTilNow = simArray[i][j];
                                a = i;
                                b = j;
                            }
                        }
                    }
                }
            }
            size++;
            similarity[pass] = simArray[a][b];
            if (smallerIsFirst) {
                A[pass] = a;
                B[pass] = b;
            } else {
                A[pass] = b;
                B[pass] = a;
            }
            smallNamePos_Taken[a] = true;
            bigNamePos_Taken[b] = true;
        }
        return mappingPaths;
    }

    /** 
	 * This method joins the broken words from a name.
	 * 
	 * @param numOfTokensInA
	 * @param numOfTokensInB
	 * @param pSimilarityArray
	 * @param tokens1
	 * @param tokens2
	 * @return
	 */
    public int joinWords(int numOfTokensInA, int numOfTokensInB, float[][] pSimilarityArray, String[] tokens1, String[] tokens2) {
        int pAlreadyFishedInThisRow_Column = -1;
        int previous_B_PositionForTheSame_A = -1;
        for (int b = 0; b < numOfTokensInB; b++) {
            for (int a = 0; a < numOfTokensInA; a++) {
                if (pSimilarityArray[a][b] > 0.3) {
                    if (size > 0) {
                        previous_B_PositionForTheSame_A = getprevious_B_PositionForTheSame_A(a, size);
                    } else {
                        previous_B_PositionForTheSame_A = -1;
                    }
                    if (previous_B_PositionForTheSame_A > -1 && pAlreadyFishedInThisRow_Column > -1) {
                    } else if (pAlreadyFishedInThisRow_Column > -1) {
                    } else if (previous_B_PositionForTheSame_A > -1) {
                    } else {
                    }
                }
            }
            pAlreadyFishedInThisRow_Column = -1;
        }
        return 1;
    }

    /**
	 * Deletes an item in the mapping path array.
	 * 
	 * @param posToDelete
	 */
    public void deleteItem(int posToDelete) {
        for (int i = posToDelete; i < size - 1; i++) {
            A[i] = A[i + 1];
            B[i] = B[i + 1];
            similarity[i] = similarity[i + 1];
        }
        size--;
    }

    /**
	 * Convert the text similarity mapping path to a string.
	 */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("size = " + size);
        sb.append("<br>\n");
        for (int i = 0; i < size; i++) {
            sb.append("[" + i + "]  A= " + A[i] + "; B= " + B[i] + "; similarity= " + similarity[i]);
            sb.append("<br>\n");
        }
        sb.append("<br>\n");
        sb.append("totalBLWeight = " + totalBLWeight);
        sb.append("<br>\n");
        sb.append("totalSimilarity = " + totalSimilarityWeight);
        sb.append("<br>\n");
        sb.append("totalNeatSimilarity = " + relativeWeightedSimilarity);
        sb.append("<br>\n");
        return sb.toString();
    }

    /**
	 * Computes the total values of the comparison.
	 */
    public void computeTotals(float pTotalBlWeight, float pTotalWlWeight, float[] pBlackListWeights, float[] pWhiteListWeights) {
        int totalSimilarities = 0;
        int bb = 0;
        for (int j = 0; j < size; j++) {
            bb = B[j];
            totalSimilarities += similarity[j] * (pBlackListWeights[bb] + pWhiteListWeights[A[j]]);
        }
        totalSimilarityWeight = totalSimilarities;
        relativeWeightedSimilarity = (totalSimilarityWeight) / (pTotalBlWeight + pTotalWlWeight);
        totalBLWeight = pTotalBlWeight;
    }
}
