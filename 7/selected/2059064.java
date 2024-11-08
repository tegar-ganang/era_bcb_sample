package de.skeptix.evomusic.ea.evaluation.nn;

import java.util.Iterator;
import de.skeptix.evomusic.ea.evaluation.IEvaluationFunction;
import de.skeptix.evomusic.ea.functions.TangensHyperbolicus;
import de.skeptix.evomusic.ea.song.Song;
import de.skeptix.evomusic.ea.song.Track;
import de.skeptix.evomusic.nn.NeuralNetwork;

/**
 * @author skchang
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NewNNEvaluationFunction implements IEvaluationFunction {

    NeuralNetwork neuralNetwork;

    public NewNNEvaluationFunction(String[] fileNames) {
        neuralNetwork = new NeuralNetwork(4, new int[] { 7, 5, 3, 1 }, new TangensHyperbolicus());
        Song[] songs = new Song[fileNames.length];
        for (int a = 0; a < fileNames.length; a++) songs[a] = new Song(fileNames[a]);
        for (int a = 0; a < 100; a++) for (int b = 0; b < fileNames.length; b++) {
            System.out.print(".");
            learnNetwork(songs[b]);
        }
    }

    public double evaluateSong(Song song) {
        double score = 8.0d;
        Track.AbsoluteNote[] notes = new Track.AbsoluteNote[8];
        for (int a = 0; a < song.getNumberOfTracks(); a++) {
            if (song.getTrack(a).getNumberOfNotes() < 8) return song.getTrack(a).getNumberOfNotes();
            if (song.getInstrument(a) < 100) {
                Iterator iterator = song.getTrack(a).getAbsoluteIterator();
                while (iterator.hasNext()) {
                    notes[7] = (Track.AbsoluteNote) iterator.next();
                    if (notes[0] != null) {
                        double[] input = new double[8];
                        for (int b = 0; b < 7; b++) input[b] = (notes[b].getHeight() - notes[b + 1].getHeight()) / 16.0d;
                        score += neuralNetwork.simulate(input)[0];
                    }
                    for (int b = 0; b < 7; b++) notes[b] = notes[b + 1];
                }
            }
        }
        return score;
    }

    public void learnNetwork(Song song) {
        Track.AbsoluteNote[] notes = new Track.AbsoluteNote[8];
        for (int a = 0; a < song.getNumberOfTracks(); a++) {
            if (song.getInstrument(a) < 100) {
                Iterator iterator = song.getTrack(a).getAbsoluteIterator();
                while (iterator.hasNext()) {
                    notes[7] = (Track.AbsoluteNote) iterator.next();
                    double[] input = new double[8];
                    if (notes[0] != null) {
                        for (int b = 0; b < 7; b++) input[b] = (notes[b].getHeight() - notes[b + 1].getHeight()) / 16.0d;
                        neuralNetwork.learn(input, new double[] { 1.0d });
                    }
                    for (int c = 0; c < 3; c++) {
                        for (int b = 0; b < 7; b++) input[b] = Math.random() * 2.0d - 1.0d;
                        neuralNetwork.learn(input, new double[] { -1.0d });
                    }
                    for (int b = 0; b < 7; b++) notes[b] = notes[b + 1];
                }
            }
        }
    }
}
