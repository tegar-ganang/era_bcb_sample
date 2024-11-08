package com.bitec.colorizer;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.bitec.graph.Graph;

/**
 * Class witch coloring graph
 */
public class GraphColorizer {

    private int minColors;

    private int maxColors;

    private Graph graph;

    private int resultThreadIndex;

    Lock resultLock = new ReentrantLock();

    List<MinColoringFinder> threads = new ArrayList<MinColoringFinder>();

    public GraphColorizer(Graph graph) {
        this.graph = graph;
        minColors = -1;
        maxColors = 25;
    }

    /**
	 * method for colorize graph
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws BrokenBarrierException 
	 */
    public void colorize() throws InterruptedException, IOException, BrokenBarrierException {
        ExecutorService executorService = Executors.newFixedThreadPool(maxColors);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(maxColors + 1);
        int threadsNum = maxColors;
        for (int i = 0; i < threadsNum; i++) {
            MinColoringFinder minColoringFinder = new MinColoringFinder(graph, 1, 1, i, this, cyclicBarrier);
            threads.add(minColoringFinder);
            executorService.execute(minColoringFinder);
        }
        cyclicBarrier.await();
        executorService.shutdown();
        saveResult();
    }

    /**
	 * Method witch save result coloring in out.txt
	 * @throws IOException 
	 */
    void saveResult() throws IOException {
        BufferedWriter file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("out.txt")));
        for (int i = 0; i < graph.getSize(); i++) {
            file.write(graph.getVertex(i).getName() + " " + threads.get(resultThreadIndex).getResult().get(i));
            file.newLine();
        }
        file.close();
    }

    public int getMinColors() {
        return minColors;
    }

    public void setMinColors(int minColors) {
        this.minColors = minColors;
    }

    public int getMaxColors() {
        return maxColors;
    }

    public void setMaxColors(int maxColors) {
        this.maxColors = maxColors;
    }

    public int getResultThreadIndex() {
        return resultThreadIndex;
    }

    public void setResultThreadIndex(int resultThreadIndex) {
        this.resultThreadIndex = resultThreadIndex;
    }

    public void lock() {
        resultLock.lock();
    }

    public void unlock() {
        resultLock.unlock();
    }
}
