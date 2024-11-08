package jAudioFeatureExtractor.jAudioTools;

import jAudioFeatureExtractor.Cancel;
import jAudioFeatureExtractor.ExplicitCancel;
import jAudioFeatureExtractor.Updater;
import jAudioFeatureExtractor.ACE.DataTypes.FeatureDefinition;
import jAudioFeatureExtractor.Aggregators.Aggregator;
import jAudioFeatureExtractor.Aggregators.AggregatorContainer;
import jAudioFeatureExtractor.Aggregators.AreaMoments;
import jAudioFeatureExtractor.Aggregators.MFCC;
import jAudioFeatureExtractor.Aggregators.Mean;
import jAudioFeatureExtractor.Aggregators.MultipleFeatureHistogram;
import jAudioFeatureExtractor.Aggregators.StandardDeviation;
import jAudioFeatureExtractor.AudioFeatures.*;
import javax.sound.sampled.*;
import java.io.*;
import java.util.LinkedList;

/**
 * This class is used to pre-process and extract features from audio recordings.
 * An object of this class should be instantiated with parameters indicating the
 * details of how features are to be extracted.
 * <p>
 * The extractFeatures method should be called whenever recordings are available
 * to be analyzed. This mehtod should be called once for each recording. It will
 * write the extracted feature values to an XML file after each call. This will
 * also save feature definitions to another XML file.
 * <p>
 * The finalize method should be called when all features have been extracted.
 * this will finish writing the feature values to the XML file.
 * <p>
 * Features are extracted for each window and, when appropriate, the average and
 * standard deviation of each of these features is extracted for each recording.
 * 
 * @author Cory McKay
 */
public class FeatureProcessor {

    private int window_size;

    private int window_overlap_offset;

    private double sampling_rate;

    private boolean normalise;

    private FeatureExtractor[] feature_extractors;

    private int[][] feature_extractor_dependencies;

    private int[] max_feature_offsets;

    private boolean[] features_to_save;

    private boolean save_features_for_each_window;

    private boolean save_overall_recording_features;

    private DataOutputStream values_writer;

    private DataOutputStream definitions_writer;

    private boolean definitions_written;

    private int outputType;

    private boolean isARFFOverallHeaderWritten = false;

    private Updater updater;

    private Cancel cancel;

    private AggregatorContainer aggregator;

    /**
	 * Validates and stores the configuration to use for extracting features
	 * from audio recordings. Prepares the feature_vector_file and
	 * feature_key_file XML files for saving.
	 * 
	 * @param window_size
	 *            The size of the windows that the audio recordings are to be
	 *            broken into.
	 * @param window_overlap
	 *            The fraction of overlap between adjacent windows. Must be
	 *            between 0.0 and less than 1.0, with a value of 0.0 meaning no
	 *            overlap.
	 * @param sampling_rate
	 *            The sampling rate that all recordings are to be converted to
	 *            before feature extraction
	 * @param normalise
	 *            Whether or not to normalise recordings before feature
	 *            extraction.
	 * @param all_feature_extractors
	 *            All features that can be extracted.
	 * @param features_to_save_among_all
	 *            Which features are to be saved. Entries correspond to the
	 *            all_feature_extractors parameter.
	 * @param save_features_for_each_window
	 *            Whether or not to save features individually for each window.
	 * @param save_overall_recording_features
	 *            Whetehr or not to save the average and standard deviation of
	 *            each feature accross all windows.
	 * @param feature_values_save_path
	 *            The path of the feature_vector_file XML file to save feature
	 *            values to.
	 * @param feature_definitions_save_path
	 *            The path of the feature_key_file file to save feature
	 *            definitions to.
	 * @throws Exception
	 *             Throws an informative exception if the input parameters are
	 *             invalid.
	 */
    public FeatureProcessor(int window_size, double window_overlap, double sampling_rate, boolean normalise, FeatureExtractor[] all_feature_extractors, boolean[] features_to_save_among_all, boolean save_features_for_each_window, boolean save_overall_recording_features, OutputStream feature_values_save_path, OutputStream feature_definitions_save_path, int outputType, Cancel cancel, AggregatorContainer container) throws Exception {
        this.cancel = cancel;
        if (container != null) {
            if ((container.getNumberOfAggregators() == 0) && (save_overall_recording_features)) {
                throw new Exception("Saving aggregated values for each file without any aggregators specified");
            }
        } else if (save_overall_recording_features) {
            throw new Exception("Saving aggregators for each file but executed without setting a non-null AggregatorContainer object");
        }
        aggregator = container;
        if (!save_features_for_each_window && !save_overall_recording_features) throw new Exception("You must save at least one of the windows-based\n" + "features and the overall file-based features.");
        if (window_overlap < 0.0 || window_overlap >= 1.0) throw new Exception("Window overlap fraction is " + window_overlap + ".\n" + "This value must be 0.0 or above and less than 1.0.");
        if (window_size < 3) throw new Exception("Window size is " + window_size + ".\n" + "This value must be above 2.");
        boolean one_selected = false;
        for (int i = 0; i < features_to_save_among_all.length; i++) if (features_to_save_among_all[i]) one_selected = true;
        if (!one_selected) throw new Exception("No features have been set to be saved.");
        if ((outputType == 0) || (outputType == 1)) {
            this.outputType = outputType;
        } else {
            throw new Exception("INTERNAL ERROR - only ARFF and ACE output files are supported");
        }
        values_writer = new DataOutputStream(feature_values_save_path);
        definitions_writer = new DataOutputStream(feature_definitions_save_path);
        definitions_written = false;
        this.window_size = window_size;
        this.sampling_rate = sampling_rate;
        this.normalise = normalise;
        this.save_features_for_each_window = save_features_for_each_window;
        this.save_overall_recording_features = save_overall_recording_features;
        window_overlap_offset = (int) (window_overlap * (double) window_size);
        findAndOrderFeaturesToExtract(all_feature_extractors, features_to_save_among_all);
        if (outputType == 0) {
            writeValuesXMLHeader();
        } else if (outputType == 1) {
            writeValuesARFFHeader();
        }
    }

    /**
	 * Extract the features from the provided audio file. This includes
	 * pre-processing involving sample rate conversion, windowing and, possibly,
	 * normalisation. The feature values are automatically saved to the
	 * feature_vector_file XML file referred to by the values_writer field. The
	 * definitions of the features that are saved are also saved to the
	 * feature_key_file XML file referred to by the definitions_writer field.
	 * 
	 * @param recording_file
	 *            The audio file to extract features from.
	 */
    public void extractFeatures(File recording_file, Updater updater) throws Exception {
        this.updater = updater;
        double[] samples = preProcessRecording(recording_file);
        if (cancel.isCancel()) {
            throw new ExplicitCancel("Killed after loading data");
        }
        LinkedList<Integer> window_start_indices_list = new LinkedList<Integer>();
        int this_start = 0;
        while (this_start < samples.length) {
            window_start_indices_list.add(new Integer(this_start));
            this_start += window_size - window_overlap_offset;
        }
        Integer[] window_start_indices_I = window_start_indices_list.toArray(new Integer[1]);
        int[] window_start_indices = new int[window_start_indices_I.length];
        if (updater != null) {
            updater.setFileLength(window_start_indices.length);
        }
        for (int i = 0; i < window_start_indices.length; i++) window_start_indices[i] = window_start_indices_I[i].intValue();
        double[][][] window_feature_values = getFeatures(samples, window_start_indices);
        if (save_overall_recording_features) {
            aggregator.add(feature_extractors, features_to_save);
            aggregator.aggregate(window_feature_values);
        }
        if (outputType == 0) {
            saveACEFeatureVectorsForARecording(window_feature_values, window_start_indices, recording_file.getPath(), aggregator);
        } else if (outputType == 1) {
            saveARFFFeatureVectorsForARecording(window_feature_values, window_start_indices, recording_file.getPath(), aggregator);
        }
        if (!definitions_written && (outputType == 0)) {
            saveFeatureDefinitions(window_feature_values, aggregator);
        }
    }

    /**
	 * Write the ending tags to the feature_vector_file XML file. Close the
	 * DataOutputStreams that were used to write it.
	 * <p>
	 * This method should be called when all features have been extracted.
	 * 
	 * @throws Exception
	 *             Throws an exception if cannot write or close the output
	 *             streams.
	 */
    public void finalize() throws Exception {
        if (outputType == 0) {
            values_writer.writeBytes("</feature_vector_file>");
        }
        values_writer.close();
    }

    /**
	 * Fills the feature_extractors, feature_extractor_dependencies,
	 * max_feature_offsets and features_to_save fields. This involves finding
	 * which features need to be extracted and in what order and finding the
	 * indices of dependencies and the maximum offsets for each feature.
	 * <p>
	 * Daniel McEnnis 05-07-05 added feature offset of dependancies to
	 * max_offset
	 * 
	 * @param all_feature_extractors
	 *            All features that can be extracted.
	 * @param features_to_save_among_all
	 *            Which features are to be saved. Entries correspond to the
	 *            all_feature_extractors parameter.
	 */
    private void findAndOrderFeaturesToExtract(FeatureExtractor[] all_feature_extractors, boolean[] features_to_save_among_all) {
        String[] all_feature_names = new String[all_feature_extractors.length];
        for (int feat = 0; feat < all_feature_extractors.length; feat++) all_feature_names[feat] = all_feature_extractors[feat].getFeatureDefinition().name;
        String[][] dependencies = new String[all_feature_extractors.length][];
        for (int feat = 0; feat < all_feature_extractors.length; feat++) {
            if (features_to_save_among_all[feat]) dependencies[feat] = all_feature_extractors[feat].getDepenedencies(); else dependencies[feat] = null;
        }
        boolean done = false;
        boolean[] features_to_extract = new boolean[dependencies.length];
        for (int feat = 0; feat < features_to_extract.length; feat++) {
            if (features_to_save_among_all[feat]) features_to_extract[feat] = true; else features_to_extract[feat] = false;
        }
        while (!done) {
            done = true;
            for (int feat = 0; feat < dependencies.length; feat++) if (dependencies[feat] != null) for (int i = 0; i < dependencies[feat].length; i++) {
                String name = dependencies[feat][i];
                for (int j = 0; j < all_feature_names.length; j++) {
                    if (name.equals(all_feature_names[j])) {
                        if (!features_to_extract[j]) {
                            features_to_extract[j] = true;
                            dependencies[j] = all_feature_extractors[j].getDepenedencies();
                            if (dependencies[j] != null) done = false;
                        }
                        j = all_feature_names.length;
                    }
                }
            }
        }
        int number_features_to_extract = 0;
        for (int i = 0; i < features_to_extract.length; i++) if (features_to_extract[i]) number_features_to_extract++;
        feature_extractors = new FeatureExtractor[number_features_to_extract];
        features_to_save = new boolean[number_features_to_extract];
        for (int i = 0; i < features_to_save.length; i++) features_to_save[i] = false;
        boolean[] feature_added = new boolean[dependencies.length];
        for (int i = 0; i < feature_added.length; i++) feature_added[i] = false;
        int current_position = 0;
        done = false;
        while (!done) {
            done = true;
            for (int feat = 0; feat < dependencies.length; feat++) {
                if (features_to_extract[feat] && !feature_added[feat]) if (dependencies[feat] == null) {
                    feature_added[feat] = true;
                    feature_extractors[current_position] = all_feature_extractors[feat];
                    features_to_save[current_position] = features_to_save_among_all[feat];
                    current_position++;
                    done = false;
                    for (int i = 0; i < dependencies.length; i++) if (features_to_extract[i] && dependencies[i] != null) {
                        int num_defs = dependencies[i].length;
                        for (int j = 0; j < num_defs; j++) {
                            if (dependencies[i][j].equals(all_feature_names[feat])) {
                                if (dependencies[i].length == 1) {
                                    dependencies[i] = null;
                                    j = num_defs;
                                } else {
                                    String[] temp = new String[dependencies[i].length - 1];
                                    int m = 0;
                                    for (int k = 0; k < dependencies[i].length; k++) {
                                        if (k != j) {
                                            temp[m] = dependencies[i][k];
                                            m++;
                                        }
                                    }
                                    dependencies[i] = temp;
                                    j--;
                                    num_defs--;
                                }
                            }
                        }
                    }
                }
            }
        }
        feature_extractor_dependencies = new int[feature_extractors.length][];
        String[] feature_names = new String[feature_extractors.length];
        for (int feat = 0; feat < feature_names.length; feat++) {
            feature_names[feat] = feature_extractors[feat].getFeatureDefinition().name;
        }
        String[][] feature_dependencies_str = new String[feature_extractors.length][];
        for (int feat = 0; feat < feature_dependencies_str.length; feat++) feature_dependencies_str[feat] = feature_extractors[feat].getDepenedencies();
        for (int i = 0; i < feature_dependencies_str.length; i++) if (feature_dependencies_str[i] != null) {
            feature_extractor_dependencies[i] = new int[feature_dependencies_str[i].length];
            for (int j = 0; j < feature_dependencies_str[i].length; j++) for (int k = 0; k < feature_names.length; k++) if (feature_dependencies_str[i][j].equals(feature_names[k])) feature_extractor_dependencies[i][j] = k;
        }
        max_feature_offsets = new int[feature_extractors.length];
        for (int i = 0; i < max_feature_offsets.length; i++) {
            if (feature_extractors[i].getDepenedencyOffsets() == null) max_feature_offsets[i] = 0; else {
                int[] these_offsets = feature_extractors[i].getDepenedencyOffsets();
                max_feature_offsets[i] = Math.abs(these_offsets[0] + max_feature_offsets[feature_extractor_dependencies[i][0]]);
                for (int k = 0; k < these_offsets.length; k++) {
                    int val = Math.abs(these_offsets[k]) + max_feature_offsets[feature_extractor_dependencies[i][k]];
                    if (val > max_feature_offsets[i]) {
                        max_feature_offsets[i] = val;
                    }
                }
            }
        }
    }

    /**
	 * Returns the samples stored in the given audio file.
	 * <p>
	 * The samples are re-encoded using the sampling rate in the sampling_rate
	 * field. All channels are projected into one channel. Samples are
	 * normalised if the normalise field is true.
	 * 
	 * @param recording_file
	 *            The audio file to extract samples from.
	 * @return The processed audio samples. Values will fall between a minimum
	 *         of -1 and +1. The indice identifies the sample number.
	 * @throws Exception
	 *             An exception is thrown if a problem occurs during file
	 *             reading or pre- processing.
	 */
    private double[] preProcessRecording(File recording_file) throws Exception {
        AudioInputStream original_stream = AudioSystem.getAudioInputStream(recording_file);
        AudioFormat original_format = original_stream.getFormat();
        int bit_depth = original_format.getSampleSizeInBits();
        if (bit_depth != 8 && bit_depth != 16) bit_depth = 16;
        AudioInputStream second_stream = original_stream;
        if (original_format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || original_format.isBigEndian() == false) {
            AudioFormat new_format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, original_format.getSampleRate(), bit_depth, original_format.getChannels(), original_format.getChannels() * (bit_depth / 8), original_format.getSampleRate(), true);
            second_stream = AudioSystem.getAudioInputStream(new_format, original_stream);
        }
        AudioInputStream new_stream = second_stream;
        if (original_format.getSampleRate() != (float) sampling_rate || bit_depth != original_format.getSampleSizeInBits()) {
            AudioFormat new_format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (float) sampling_rate, bit_depth, original_format.getChannels(), original_format.getChannels() * (bit_depth / 8), original_format.getSampleRate(), true);
            new_stream = AudioSystem.getAudioInputStream(new_format, second_stream);
        }
        AudioSamples audio_data = new AudioSamples(new_stream, recording_file.getPath(), false);
        if (normalise) audio_data.normalizeMixedDownSamples();
        return audio_data.getSamplesMixedDown();
    }

    /**
	 * Breaks the given samples into the appropriate windows and extracts
	 * features from each window.
	 * 
	 * @param samples
	 *            The samples to extract features from. Sample values should
	 *            generally be between -1 and +1.
	 * @param window_start_indices
	 *            The indices of samples that correspond to where each window
	 *            should start.
	 * @return The extracted feature values for this recording. The first indice
	 *         identifies the window, the second identifies the feature and the
	 *         third identifies the feature value. The third dimension will be
	 *         null if the given feature could not be extracted for the given
	 *         window.
	 * @throws Exception
	 *             Throws an exception if a problem occurs.
	 */
    private double[][][] getFeatures(double[] samples, int[] window_start_indices) throws Exception {
        double[][][] results = new double[window_start_indices.length][feature_extractors.length][];
        int updateThreshold = 1;
        if (window_start_indices.length > 100) {
            updateThreshold = window_start_indices.length / 100;
        }
        for (int win = 0; win < window_start_indices.length; win++) {
            if ((updater != null) && (win % updateThreshold == 0)) {
                updater.announceUpdate(win);
                if (cancel.isCancel()) {
                    throw new ExplicitCancel("Killed while processing features");
                }
            }
            double[] window = new double[window_size];
            int start_sample = window_start_indices[win];
            int end_sample = start_sample + window_size - 1;
            if (end_sample < samples.length) for (int samp = start_sample; samp <= end_sample; samp++) window[samp - start_sample] = samples[samp]; else for (int samp = start_sample; samp <= end_sample; samp++) {
                if (samp < samples.length) window[samp - start_sample] = samples[samp]; else window[samp - start_sample] = 0.0;
            }
            for (int feat = 0; feat < feature_extractors.length; feat++) {
                if (win >= max_feature_offsets[feat]) {
                    FeatureExtractor feature = feature_extractors[feat];
                    double[][] other_feature_values = null;
                    if (feature_extractor_dependencies[feat] != null) {
                        other_feature_values = new double[feature_extractor_dependencies[feat].length][];
                        for (int i = 0; i < feature_extractor_dependencies[feat].length; i++) {
                            int feature_indice = feature_extractor_dependencies[feat][i];
                            int offset = feature.getDepenedencyOffsets()[i];
                            other_feature_values[i] = results[win + offset][feature_indice];
                        }
                    }
                    results[win][feat] = feature.extractFeature(window, sampling_rate, other_feature_values);
                } else results[win][feat] = null;
            }
        }
        return results;
    }

    /**
	 * Calculates the averages and standard deviations over a whole recording of
	 * each of the windows-based features. Generates a feature definition for
	 * each such feature.
	 * 
	 * @param window_feature_values
	 *            The extracted window feature values for this recording. The
	 *            first indice identifies the window, the second identifies the
	 *            feature and the third identifies the feature value. The third
	 *            dimension will be null if the given feature could not be
	 *            extracted for the given window.
	 * @param overall_feature_definitions
	 *            The feature definitions of the features that are returned by
	 *            this method. This array will be filled by this method, and
	 *            should be an empty FeatureDefintion[1][] when it is passed to
	 *            this method. The first indice will be filled by this method
	 *            with a single array of FeatureDefinitions, which have the same
	 *            order as the returned feature values.
	 * @return The extracted overall average and standard deviations of the
	 *         window feature values that were passed to this method. The first
	 *         indice identifies the feature and the second iddentifies the
	 *         feature value. The order of the features correspond to the
	 *         FeatureDefinitions that the overall_feature_definitions parameter
	 *         is filled with.
	 */
    private double[][] getOverallRecordingFeatures(double[][][] window_feature_values, FeatureDefinition[][] overall_feature_definitions) {
        LinkedList<double[]> values = new LinkedList<double[]>();
        LinkedList<FeatureDefinition> definitions = new LinkedList<FeatureDefinition>();
        for (int feat = 0; feat < feature_extractors.length; feat++) if (window_feature_values[window_feature_values.length - 1][feat] != null && features_to_save[feat]) {
            FeatureDefinition this_def = feature_extractors[feat].getFeatureDefinition();
            FeatureDefinition average_definition = new FeatureDefinition(this_def.name + " Overall Average", this_def.description + "\nThis is the overall average over all windows.", this_def.is_sequential, window_feature_values[window_feature_values.length - 1][feat].length);
            FeatureDefinition stdv_definition = new FeatureDefinition(this_def.name + " Overall Standard Deviation", this_def.description + "\nThis is the overall standard deviation over all windows.", this_def.is_sequential, window_feature_values[window_feature_values.length - 1][feat].length);
            double[] averages = new double[window_feature_values[window_feature_values.length - 1][feat].length];
            double[] stdvs = new double[window_feature_values[window_feature_values.length - 1][feat].length];
            for (int val = 0; val < window_feature_values[window_feature_values.length - 1][feat].length; val++) {
                int count = 0;
                for (int win = 0; win < window_feature_values.length; win++) if (window_feature_values[win][feat] != null) count++;
                double[] values_to_process = new double[count];
                int current = 0;
                for (int win = 0; win < window_feature_values.length; win++) if (window_feature_values[win][feat] != null) {
                    values_to_process[current] = window_feature_values[win][feat][val];
                    current++;
                }
                averages[val] = jAudioFeatureExtractor.GeneralTools.Statistics.getAverage(values_to_process);
                stdvs[val] = jAudioFeatureExtractor.GeneralTools.Statistics.getStandardDeviation(values_to_process);
            }
            values.add(averages);
            definitions.add(average_definition);
            values.add(stdvs);
            definitions.add(stdv_definition);
        }
        overall_feature_definitions[0] = definitions.toArray(new FeatureDefinition[1]);
        return values.toArray(new double[1][]);
    }

    /**
	 * Writes the headers, consisting mainly of the DTD, to the
	 * feature_vector_file..
	 * 
	 * @throws Exception
	 *             Throws an exception if cannot write.
	 */
    private void writeValuesXMLHeader() throws Exception {
        String feature_vector_header = new String("<?xml version=\"1.0\"?>\n" + "<!DOCTYPE feature_vector_file [\n" + "   <!ELEMENT feature_vector_file (comments, data_set+)>\n" + "   <!ELEMENT comments (#PCDATA)>\n" + "   <!ELEMENT data_set (data_set_id, section*, feature*)>\n" + "   <!ELEMENT data_set_id (#PCDATA)>\n" + "   <!ELEMENT section (feature+)>\n" + "   <!ATTLIST section start CDATA \"\"\n" + "                     stop CDATA \"\">\n" + "   <!ELEMENT feature (name, v+)>\n" + "   <!ELEMENT name (#PCDATA)>\n" + "   <!ELEMENT v (#PCDATA)>\n" + "]>\n\n" + "<feature_vector_file>\n\n" + "   <comments></comments>\n\n");
        values_writer.writeBytes(feature_vector_header);
    }

    /**
	 * Write headers for an ARFF file. If saving for overall features, this must
	 * be postponed until the overall features have been calculated. If this a
	 * perWindow arff file, then all the feature headers can be extracted now
	 * and no hacks are needed.
	 * <p>
	 * <b>NOTE</b>: This procedure breaks if a feature to be saved has a
	 * variable number of dimensions
	 * 
	 * @throws Exception
	 */
    private void writeValuesARFFHeader() throws Exception {
        String sep = System.getProperty("line.separator");
        String feature_value_header = "@relation jAudio" + sep;
        values_writer.writeBytes(feature_value_header);
        if (save_features_for_each_window && !save_overall_recording_features) {
            for (int i = 0; i < feature_extractors.length; ++i) {
                if (features_to_save[i]) {
                    String name = feature_extractors[i].getFeatureDefinition().name;
                    int dimension = feature_extractors[i].getFeatureDefinition().dimensions;
                    for (int j = 0; j < dimension; ++j) {
                        values_writer.writeBytes("@ATTRIBUTE \"" + name + j + "\" NUMERIC" + sep);
                    }
                }
            }
            values_writer.writeBytes(sep);
            values_writer.writeBytes("@DATA" + sep);
        }
    }

    /**
	 * Writes the given feature values extracted from a recording to the
	 * feature_vector_file ARFF file referred to by the values_writer field.
	 * Writes either the individual window features or the overall recording
	 * features to disk.
	 * 
	 * @param feature_values
	 *            The extracted feature values for this recording. The first
	 *            indice identifies the window, the second identifies the
	 *            feature and the third identifies the feature value. The third
	 *            dimension will be null if the given feature could not be
	 *            extracted for the given window.
	 * @param window_start_indices
	 *            The indices of samples that correspond to where each window
	 *            should start.
	 * @param identifier
	 *            A string to use for identifying this recording. Often a file
	 *            path.
	 * @param overall_feature_values
	 *            The extracted overall average and standard deviations of the
	 *            window feature values. The first indice identifies the feature
	 *            and the second identifies the feature value. The order of the
	 *            features correspond to the overall_feature_definitions
	 *            parameter. This value is null if overall feature values were
	 *            not extracted.
	 * @param overall_feature_definitions
	 *            The feature definitions of the features that are in the
	 *            overall_feature_values parameter. Will be null if no overall
	 *            features were extracted.
	 * @throws Exception
	 *             Throws an exception if cannot write.
	 */
    private void saveARFFFeatureVectorsForARecording(double[][][] feature_values, int[] window_start_indices, String identifier, AggregatorContainer aggContainer) throws Exception {
        if (save_overall_recording_features) {
            if (!isARFFOverallHeaderWritten) {
                aggContainer.outputARFFHeaderEntries(values_writer);
                isARFFOverallHeaderWritten = true;
            }
            aggContainer.outputARFFValueEntries(values_writer);
        } else {
            for (int win = 0; win < feature_values.length; ++win) {
                for (int feat = 0; feat < feature_values[win].length; ++feat) {
                    if (features_to_save[feat]) {
                        if (feature_values[win][feat] == null) {
                            int dim = feature_extractors[feat].getFeatureDefinition().dimensions;
                            for (int d = 0; d < dim; ++d) {
                                values_writer.writeBytes("?");
                                if (d < dim - 1) {
                                    values_writer.writeBytes(",");
                                }
                            }
                        } else {
                            for (int d = 0; d < feature_values[win][feat].length; ++d) {
                                String value = jAudioFeatureExtractor.GeneralTools.StringMethods.getDoubleInScientificNotation(feature_values[win][feat][d], 4);
                                values_writer.writeBytes(value);
                                if (d < feature_values[win][feat].length - 1) {
                                    values_writer.writeBytes(",");
                                }
                            }
                        }
                        if (feat < feature_values[win].length - 1) {
                            values_writer.writeBytes(",");
                        }
                    }
                }
                values_writer.writeBytes(System.getProperty("line.separator"));
            }
        }
    }

    /**
	 * Writes the given feature values extracted from a recording to the
	 * feature_vector_file XML file referred to by the values_writer field.
	 * Writes both the individual window features and the overall recording
	 * features to disk.
	 * 
	 * @param feature_values
	 *            The extracted feature values for this recording. The first
	 *            indice identifies the window, the second identifies the
	 *            feature and the third identifies the feature value. The third
	 *            dimension will be null if the given feature could not be
	 *            extracted for the given window.
	 * @param window_start_indices
	 *            The indices of samples that correspond to where each window
	 *            should start.
	 * @param identifier
	 *            A string to use for identifying this recording. Often a file
	 *            path.
	 * @param overall_feature_values
	 *            The extracted overall average and standard deviations of the
	 *            window feature values. The first indice identifies the feature
	 *            and the second identifies the feature value. The order of the
	 *            features correspond to the overall_feature_definitions
	 *            parameter. This value is null if overall feature values were
	 *            not extracted.
	 * @param overall_feature_definitions
	 *            The feature definitions of the features that are in the
	 *            overall_feature_values parameter. Will be null if no overall
	 *            features were extracted.
	 * @throws Exception
	 *             Throws an exception if cannot write.
	 */
    private void saveACEFeatureVectorsForARecording(double[][][] feature_values, int[] window_start_indices, String identifier, AggregatorContainer aggContainer) throws Exception {
        values_writer.writeBytes("\t<data_set>\n");
        values_writer.writeBytes("\t\t<data_set_id>" + identifier + "</data_set_id>\n");
        if (save_features_for_each_window) for (int win = 0; win < feature_values.length; win++) {
            double start_time = ((double) window_start_indices[win]) / sampling_rate;
            double end_time = ((double) (window_start_indices[win] + window_size - 1)) / sampling_rate;
            values_writer.writeBytes("\t\t<section start=\"" + start_time + "\" stop=\"" + end_time + "\">\n");
            for (int feat = 0; feat < feature_values[win].length; feat++) {
                if (features_to_save[feat]) if (feature_values[win][feat] != null) {
                    String feature_name = feature_extractors[feat].getFeatureDefinition().name;
                    values_writer.writeBytes("\t\t\t<feature>\n");
                    values_writer.writeBytes("\t\t\t\t<name>" + feature_name + "</name>\n");
                    for (int val = 0; val < feature_values[win][feat].length; val++) {
                        String value = jAudioFeatureExtractor.GeneralTools.StringMethods.getDoubleInScientificNotation(feature_values[win][feat][val], 4);
                        values_writer.writeBytes("\t\t\t\t<v>" + value + "</v>\n");
                    }
                    values_writer.writeBytes("\t\t\t</feature>\n");
                }
            }
            values_writer.writeBytes("\t\t</section>\n");
        }
        if (save_overall_recording_features) aggContainer.outputACEValueEntries(values_writer);
        values_writer.writeBytes("\t</data_set>\n\n");
    }

    /**
	 * Writes feature definitions to the XML file referred to by the
	 * definitions_writer field. Writes both overall and individual feature
	 * definitions.
	 * 
	 * @param feature_values
	 *            The extracted feature values for a recording. The first indice
	 *            identifies the window, the second identifies the feature and
	 *            the third identifies the feature value.
	 * @param overall_feature_definitions
	 *            The feature definitions of the features that are in the
	 *            features for the recording. Will be null if no overallfeatures
	 *            were extracted.
	 * @throws Exception
	 *             Throws an exception if cannot write.
	 */
    private void saveFeatureDefinitions(double[][][] feature_values, AggregatorContainer aggContainer) throws Exception {
        String feature_key_header = new String("<?xml version=\"1.0\"?>\n" + "<!DOCTYPE feature_key_file [\n" + "   <!ELEMENT feature_key_file (comments, feature+)>\n" + "   <!ELEMENT comments (#PCDATA)>\n" + "   <!ELEMENT feature (name, description?, is_sequential, parallel_dimensions)>\n" + "   <!ELEMENT name (#PCDATA)>\n" + "   <!ELEMENT description (#PCDATA)>\n" + "   <!ELEMENT is_sequential (#PCDATA)>\n" + "   <!ELEMENT parallel_dimensions (#PCDATA)>\n" + "]>\n\n" + "<feature_key_file>\n\n" + "   <comments></comments>\n\n");
        definitions_writer.writeBytes(feature_key_header);
        double[][] last_window_features = feature_values[feature_values.length - 1];
        if (save_features_for_each_window) for (int feat = 0; feat < feature_extractors.length; feat++) if (features_to_save[feat]) if (last_window_features[feat] != null) {
            FeatureDefinition def = feature_extractors[feat].getFeatureDefinition();
            definitions_writer.writeBytes("   <feature>\n");
            definitions_writer.writeBytes("      <name>" + def.name + "</name>\n");
            definitions_writer.writeBytes("      <description>" + def.description + "</description>\n");
            definitions_writer.writeBytes("      <is_sequential>" + def.is_sequential + "</is_sequential>\n");
            definitions_writer.writeBytes("      <parallel_dimensions>" + last_window_features[feat].length + "</parallel_dimensions>\n");
            definitions_writer.writeBytes("   </feature>\n\n");
        }
        if (save_overall_recording_features) {
            aggContainer.outputACEFeatureKeyEntries(definitions_writer);
        }
        definitions_writer.writeBytes("</feature_key_file>");
        definitions_writer.close();
        definitions_written = true;
    }
}
