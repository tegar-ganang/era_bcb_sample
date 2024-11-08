package com.greentea.relaxation.jnmf.localization;

import static com.greentea.relaxation.jnmf.localization.Localizer.lineSeparator;

/**
 * Created by IntelliJ IDEA. User: GreenTea Date: 02.06.2009 Time: 11:21:11 To change this template
 * use File | Settings | File Templates.
 */
public class StringsBundle_en extends AbstractStringsBundle {

    @Override
    protected Object[][] getContents() {
        return new Object[][] { { StringId.CAUSE, "cause" }, { StringId.CANT_LOAD_DATA, "Can't load dataset" }, { StringId.CANT_TRANSFORM_DATA, "Can't transform dataset" }, { StringId.NEURONS_COUNT, "neurons count" }, { StringId.CREATION_TIME, "creation time" }, { StringId.LAYERS_COUNT, "layers count" }, { StringId.LAYER, "layer" }, { StringId.HISTORY, "history" }, { StringId.NO_DESCRIPTION, "no description" }, { StringId.MIN, "min" }, { StringId.MAX, "max" }, { StringId.DATA_FILE, "Dataset file" }, { StringId.ZERO_OUTPUTS_AFTER_FILTRATION, "Count of inputs and outputs in learning dataset" + " after filtration should be greater than 0" }, { StringId.CANT_FILTER_DATA, "can't filter dataset" }, { StringId.ZERO_OUTPUTS_COUNT, "Count of outputs should be greater than 0" }, { StringId.DATA_SHOULD_BE_SPLITTED_ON_CLASSES, "dataset should be splitted on classes" }, { StringId.LEARNING_PAIRS_COUNT, "count of learning pairs" }, { StringId.CLASSES_COUNT, "classes count" }, { StringId.IN_EVERY_CLASS, "in every class" }, { StringId.CLASS, "class" }, { StringId.CANT_PARSE_METADATA, "can't parse metadata" }, { StringId.MIN_COLUMN_INDEX, "min column index" }, { StringId.MAX_COLUMN_INDEX, ": max column index >= count of columns in dataset." }, { StringId.COLUMN_INDEX_IS_NOT_UNIQUE, "column index {0} is not unique." }, { StringId.CLUSTER, "cluster" }, { StringId.WRONG_FORMAT_OF_DATA_FILE_1, "wrong format of dataset file. " + "First row should contain 2 integer values" }, { StringId.WRONG_FORMAT_DATA_ROW, "data row " + lineSeparator + "{0}" + lineSeparator + " should contain {1} " + "value{NumeralEnd({1})}, but in contain {2}" }, { StringId.DATA_FILE_NOT_FOUND, "dataset file was not found" }, { StringId.ROW_CONTAINS_BAD_FLOAT_VALUE, "dat row" + lineSeparator + "{0}" + lineSeparator + " contains bad float value" }, { StringId.WRONG_METADATA_FORMAT, "wrong metadata format" }, { StringId.DATA_FILE_IS_NULL, "dataset file - null and data in text form not set" }, { StringId.CANT_READ_FILE, "can't read file " }, { StringId.CANT_READ_DATA, "can't read dataset " }, { StringId.CANT_RESTORE_SPLITTED_DATA, "can't restore splitted dataset" }, { StringId.CANT_SPLIT_DATA_ON_CLASSES, "can't split dataset on clases" }, { StringId.CANT_CLUSTERIZE_DATA, "can't clusterize dataset" }, { StringId.HIDEN_LAYERS, "Hiden layers" }, { StringId.NEURONS_IN_EACH_LAYER, "Count of neurons in every hiden layer" }, { StringId.INITIAL_WEIGHTS_DISPERSION, "Initial weights dispersion" }, { StringId.CAS_COR_ALGORITHM, "Cascade Correlation algorithm" }, { StringId.PARAMETERS, "Parameters" }, { StringId.OUTPUTS_LEARNING_ALGORITHM, "outputs learning algorithm" }, { StringId.CANDIDATES_COUNT, "count of candidates neurons" }, { StringId.MIN_NETWORK_ERROR, "min-acceptable network error" }, { StringId.MIN_LEARNING_SPEED, "min-acceptable learning speed of outputs neurons" }, { StringId.MIN_CORRELATION_GROWTH_SPEED, "min-acceptable growth of correlation" }, { StringId.NU_MUL_FACTOR_FOR_OUTPUTS, "fading coefficient of NU for output neurons" }, { StringId.NU_MUL_FACTOR_FOR_CANDIDATES, "fading coefficient of NU for candidates" }, { StringId.MAX_OUTPUTS_EPOCHES_COUNT, "max count of epoches for learning of output neurons" }, { StringId.MAX_CANDIDATES_EPOCHES_COUNT, "max count of epoches for learning of candidates" }, { StringId.EPOCHES_COUNT, "learning epoches count" }, { StringId.LEARNING_TIME, "learning time" }, { StringId.HIDEN_NEURONS_ADDED, "hiden neurons added" }, { StringId.NETWORK_ERROR, "network error" }, { StringId.UNKNOWN_ALGORITHM, "unknown algorithm" }, { StringId.NEW_GUI_TITLE, "Degree work on a theme: " + "\"Information technology of dinamical neuron networks\". " + "Voronyuk Evgeniy SW-08m-1. 2009 year" }, { StringId.FILE, "File" }, { StringId.NEW_PROJECT, "New Project..." }, { StringId.NEW_PROJECT_2, "New Project" }, { StringId.OPEN_PROJECT, "Open Project..." }, { StringId.SAVE, "Save" }, { StringId.CLOSE_PROJECT, "Close Project" }, { StringId.EXIT, "Exit" }, { StringId.TO_PREVIOUS_COMPONENT, "To previous component" }, { StringId.TO_NEXT_COMPONENT, "To next component" }, { StringId.PROJECTS_COMPONENT, "Projects" }, { StringId.FILE_X_NOT_FOUND, "File {0} not found." }, { StringId.CANT_READ_FILE_2, "Can't read file " }, { StringId.CANT_LOAD_PROJECT_FROM_FILE, "Can't load project from file " }, { StringId.FILE_X_CANT_BE_REWRITEN, "File {0} can't be rewriten " }, { StringId.CANT_CREATE_FILE, "Can't create file " }, { StringId.ERROR_SAVING_PROJECT, "Error saving project" }, { StringId.PARAMETER, "Parameter" }, { StringId.POSSIBLE_VALUES_COLUMN_NAME, "Possible values" }, { StringId.DEFAULT_VALUE_COLUMN_NAME, "Default value" }, { StringId.CURRENT_VALUE_COLUMN_NAME, "Current value" }, { StringId.COMPONENT_IS_ENABLED, "Component is enabled" }, { StringId.SAVE_PROJECT, "Save Project" }, { StringId.LOADER, "Loader" }, { StringId.PRELIMINARY_SETTINGS_PANEL_TITLE, "Preliminary settings" }, { StringId.DATA, "Dataset" }, { StringId.LOADER_LABEL_TEXT, "Loader: " }, { StringId.DATA_FILE_LABEL_TEXT, "Dataset file: " }, { StringId.SPLIT_ON_CLASSES_CHECKBOX_TEXT, "Split on classes" }, { StringId.NORMALIZE_INPUTS_CHECKBOX_TEXT, "Normalize inputs" }, { StringId.NORMALIZE_OUTPUTS_CHECKBOX_TEXT, "Normalize outputs" }, { StringId.CLUSTERIZE_DATA_CHECKBOX_TEXT, "Clusterize dataset" }, { StringId.NETWORK_PANEL_TITLE, "Network" }, { StringId.TYPE_LABEL_TEXT, "Type: " }, { StringId.CREATE_PROJECT_BUTTON_TEXT, "Create Project" }, { StringId.DATA_LOADING, "Data loading" }, { StringId.INITIAL_DATA, "Initial dataset" }, { StringId.LEARNING_DATA_X_VECTORS, "Learning dataset  [{0} samples]" }, { StringId.TEST_DATA_X_VECTORS, "Test dataset  [{0} samples]" }, { StringId.PREPARED_DATA, "Prepared dataset" }, { StringId.ROWS_IN_TABLE_LABEL_TEXT, "Rows in table:" }, { StringId.COLUMNS_IN_TABLE_LABEL_TEXT, "columns:" }, { StringId.CLASSES_FILTRATION, "Classes filtration" }, { StringId.FILTER_CLASSES_PANEL_TITLE, "Filter of classes" }, { StringId.APPLY_FILTER_BUTTON_TEXT, "Apply filter" }, { StringId.FILTER_CLASSES_LABEL_TEXT, "Filter classes:" }, { StringId.FILTER_COLUMNS, "+ Filter columns" }, { StringId.UNFILTER_COLUMNS, "- Unfilter columns" }, { StringId.FILTER_ROWS, "+ Filter rows" }, { StringId.UNFILTER_ROWS, "- Unfilter rows" }, { StringId.APPLY_FILTRATION_BUTTON_TEXT, "Apply filtration" }, { StringId.LOWER_BOUND_OF_RANGE, "Lower bound of range" }, { StringId.UPPER_BOUND_OF_RANGE, "Upper bound of range" }, { StringId.NORMALIZATION, "Normalization" }, { StringId.OF_INPUTS, "of inputs" }, { StringId.OF_OUTPUTS, "of outputs" }, { StringId.SPLITTING_ON_CLASSES, "Splitting on classes" }, { StringId.TEST_DATA_PERSENT, "Test data percent" }, { StringId.SPLITTING_ON_LEARNING_AND_TEST, "Splitting on learning and test" }, { StringId.PREPARATION_OF_DATA, "Preparation of the data" }, { StringId.CLUSTERIZATION_CANT_BE_PERFORMED, "Clusterization can't be performed, " + "because dataset inputs was not normalized" }, { StringId.PROXIMITY_COEFFICIENT_P, "proximity coefficient (p)" }, { StringId.CLESSES_IN_CLUSTERS, "Classes in clusters" }, { StringId.CLUSTERS, "clusters" }, { StringId.CLASSES_COUNT_SHORT, "count of classes" }, { StringId.CLUSTERS_IN_CLASSES, "Clusters in classes" }, { StringId.CLASSES, "classes" }, { StringId.CLUSTERS_COUNT_SHORT, "count of clusters" }, { StringId.CLUSTERIZATION, "Clusterization" }, { StringId.EXECUTION_FINISHING_CONDITION_FULFILED, "Execution finishing condition is fulfiled" }, { StringId.EXECUTION_FINISHED, "Execution finished" }, { StringId.MIN_PERCENT_ON_LEARNING_DATA, "Min. % not guessed samples on learning dataset" }, { StringId.MIN_PERSENT_ON_TEST_DATA, "Min. % not guessed samples on test dataset" }, { StringId.MIN_ERROR_ON_LEARNING_DATA, "Min. mean square error on learning dataset" }, { StringId.MIN_ERROR_ON_TEST_DATA, "Min. mean square error on test dataset" }, { StringId.SHOW_DIAGRAM_1, "Show diagram 1" }, { StringId.SHOW_DIAGRAM_2, "Show diagram 2" }, { StringId.SHOW_DIAGRAM_3, "Show diagram 3" }, { StringId.SHOW_DIAGRAM_4, "Show diagram 4" }, { StringId.LEARNING_COMPONENT, "Learning of network" }, { StringId.PERSENT_ON_LEARNING_DATA, "% not guessed samples on learning dataset" }, { StringId.PERSENT_ON_TEST_DATA, "% not guessed samples on test dataset" }, { StringId.ERROR_ON_LEARNING_DATA, "Mean square error on learning dataset" }, { StringId.ERROR_ON_TEST_DATA, "Mean square error on test dataset" }, { StringId.LEARNING_PROGRESS, "Progress of learning" }, { StringId.EPOCHES, "epoches" }, { StringId.ERRORS, "errors" }, { StringId.LEARNING_TIME_LABEL_TEXT, "Learning time:" }, { StringId.LEARNING_FINISHED, "Learning finished" }, { StringId.UNEXPECTED_LEARNING_FINISHED, "The algorithm itself has finished the work, in spite of the fact, " + "that any of conditions of end of work " + lineSeparator + "has not been fulfiled." }, { StringId.LEARNING_PAUSED, "Learning paused" }, { StringId.RESUME_LEARNING, "Resume learning" }, { StringId.START_LEARNING, "Start learning" }, { StringId.STOP_LEARNING, "Stop learning" }, { StringId.LEARNING_IN_PROGRESS, "Learning in progress..." }, { StringId.NETWORK_NOT_LEARNED, "Network not learned" }, { StringId.CANCEL_LEARNING, "Cancel learning" }, { StringId.VISUALIZATION_2D, "2D visualization" }, { StringId.NETWORK_OUTPUTS_PICTURE, "Network outputs picture" }, { StringId.REFRESH, "Refresh" }, { StringId.NETWORK_SHOULD_CONTAIN_2IN_1_2_OUT, "Network should have 2 inputs and 1-2 outpus" }, { StringId.ON_LEARNING_DATA, "On learning dataset" }, { StringId.ON_TEST_DATA, "On test dataset" }, { StringId.PROGRESS_BY_CLASSES, "Progress by classes" }, { StringId.PERCENT_NOT_GUESSED_CLASSES, "% not guessed samples" }, { StringId.PROGRESS_BY_CLASSES_ON_LEARNING_DATA, "Progress by classes on learning dataset" }, { StringId.PROGRESS_BY_CLASSES_ON_TEST_DATA, "Progress by classes on test dataset" }, { StringId.IN_CURRENT_MOMENT, "In current moment" }, { StringId.DETAIL_FOR_LEARNING_DATA, "Detail for learning dataset" }, { StringId.DETAIL_FOR_TEST_DATA, "Detail for test dataset" }, { StringId.VISUALIZATION, "Visualization" }, { StringId.INPUT, "Input" }, { StringId.AVERAGE, "average" }, { StringId.SELF_DESCRIPTIVENESS_SHORT, "self-descr. " }, { StringId.SELF_DESCRIPTIVENESS, "self-descriptiveness" }, { StringId.LOG_REGRESSION_COEFFICIENTS, "Logistic regression coefficients" }, { StringId.CAN_BE_SHOWN_ONLY_AFTER_START_LEARNING, "Logistic regression coefficients can be shown only after start of learning" }, { StringId.LAYER_WITH_EVEN_INDEX, "Layer with even index" }, { StringId.LAYER_WITH_ODD_INDEX, "Layer with odd index" }, { StringId.STRAIGHT_CONNECTION_NEIGHBOUR_NEURONS, "Straight connection between neurons in two next layers" }, { StringId.STRAIGHT_CONNECTION_MORE_THAN_ONE, "Direct connection between neurons, where intermediate layers > 1" }, { StringId.BACK_CONNECTION, "Back connection" }, { StringId.NO_CONNECTION, "No connection" }, { StringId.USING_ALGORITHM, "Using algorithm" }, { StringId.NEURAL_NETWORKS, "Neural networks" }, { StringId.LAYERS, "Layers" }, { StringId.NEURONS, "Neurons" }, { StringId.CONNECTIONS_TABLE_SCROLL_PANE_TITLE, "Table of network connections" }, { StringId.NOT_LEARNED, "Not learned" }, { StringId.LEARNING, "Learning..." }, { StringId.LEARNED, "Learned" }, { StringId.POPULATION_SIZE, "Population size (should be even number)" }, { StringId.TIME_OF_ONE_EPOCH, "Time of one epoch (seconds)" }, { StringId.LEARNING_EPOCHES_COUNT, "Count of learning epoches" }, { StringId.MUTATION_COEFFICIENT, "Mutation coefficient" }, { StringId.TOTAL_LEFT, "Total left " }, { StringId.EPOCHE, "epoche" }, { StringId.CHROMOSOME, "chromosome" }, { StringId.GA_FINISHED_SUCCESSFULLY, "GA finished successfully" }, { StringId.GA_FINISHED_SUCCESSFULLY_MSG, "Current algorithm will be initialized by parameters of algorithm, " + lineSeparator + "which has been shown min error." }, { StringId.CHROMOSOME_LEARNING_PROGRESS, "Chromosome learning progress" }, { StringId.ERROR, "error" }, { StringId.CHROMOSOME_PROGRESS, "Chromosome progress" }, { StringId.CURRENT_ERROR, "Current error" }, { StringId.MIN_ERROR, "Minimum error" }, { StringId.VALUE, "Value" }, { StringId.START_GA_BUTTON_TEXT, "Start GA" }, { StringId.CANCEL, "Cancel" }, { StringId.CHROMOSOMES, "Chromosomes" }, { StringId.CHROMOSOME_PARAMETERS, "Parameters of chromosome" }, { StringId.APPLY_CHROMOSOME_PARAMETERS, "Apply parameters of selected chromosome to current algorithm" }, { StringId.CHANGE_POSSIBLE_VALUES, "Change possible values" }, { StringId.PARAMETERS_OPTIMIZATION, "Parameters optimization" }, { StringId.GUESSED_CLASSES, "Guessed samples" }, { StringId.NOT_GUESSED_CLASSES, "Not guessed samples" }, { StringId.USE_FOR_ANALYSIS, "Use for analysis" }, { StringId.RESULT, "Result" }, { StringId.REAL_CLASS, "Real class" }, { StringId.QUALITY_CONTROL, "Quality control" }, { StringId.ROC_ANALYSIS, "ROC analysis" }, { StringId.CLASS_2, "Class" }, { StringId.THRESHOLD, "Threshold" }, { StringId.TRUE_POSITIVE, "True positive" }, { StringId.TRUE_NEGATIVE, "True negative" }, { StringId.FALSE_POSITIVE, "False positive" }, { StringId.FALSE_NEGATIVE, "False negative" }, { StringId.SENSITIVITY, "Sensitivity" }, { StringId.SPECIFICITY, "Specificity" }, { StringId.AREA_UDER_CURVE_SE, "Area under curve SE" }, { StringId.OPTIMUM, "Optimum" }, { StringId.TOTAL_GUESSED, "Total guessed" }, { StringId.CLASSES_REPORT, "Classes report" }, { StringId.ROC, "ROC" }, { StringId.ERRORS_ON_DATA, "Errors on data" }, { StringId.STANDARD_ANALYSIS, "Standard analysis" }, { StringId.NETWORK_FORECAST, "Network forecast" }, { StringId.USING_NETWORK, "Using of network" }, { StringId.LEARNING_ON_NETWORK_NOT_FINISHED, "Learning of network not finished" }, { StringId.INCORRECT_DIMENSION_OF_DATA, "dimension of selected dataset does not coincide with dimension of a network" }, { StringId.INCORRECT_DIMENSION_OF_DATA_2, "dimension of selected dataset does not coincide with dimension of dataset ," + lineSeparator + "which where selected for network learning" }, { StringId.NO_DATA_FOR_ANALYSIS, "No dataset for analysis" }, { StringId.ANALYSIS, "Analysis" }, { StringId.TRANSFORM_DATA_AS_LEARNING_DATA, "Transform dataset the same way as learning dataset" }, { StringId.INTERACTIVE_INPUT, "Interactive input" }, { StringId.USING_DATA_FROM_FILE, "Using dataset from file" }, { StringId.ERROR_2, "Error" }, { StringId.INTERNAL_ERROR, "Internal error" }, { StringId.SORRY_INTERNAL_ERROR_OCCURED, "Sorry, internal error occured." }, { StringId.INFORMATION, "Information" }, { StringId.DATA_FILTRATION, "Data filtration" }, { StringId.NO_DATA_FOR_DISPLAY_ON_THIS_PANEL, "No data for display on this panel" }, { StringId.GA, "GA" }, { StringId.EPOCHES_COUNT_OF_TEST_ERROR_GROWTH_CRITERION, "Epoches count of test error growth criterion" }, { StringId.AVERAGE_ERROR_ON_TEST_DATA_START_GROWTH, "Average error on test dataset for {0} epoches has started to growth" }, { StringId.ACTIVATE, "Activate" }, { StringId.DELETE, "Delete" }, { StringId.ADD_NETWORK, "Add network" }, { StringId.CANT_DELETE_LAST_NETWORK, "Can't delete last neural network" }, { StringId.FILE_X_IS_NOT_EXISTS, "File {0} is not exists." + lineSeparator + "Need to select right path to file" }, { StringId.CANT_CREATE_PROJECT, "Can't create project" }, { StringId.ID_OF_NOT_GUESSED_SAMPLES, "ids of NOT guessed samples" }, { StringId.ID_OF_GUESSED_SAMPLES, "ids of guessed samples" }, { StringId.SHOW_TABLE, "Show table" }, { StringId.FileChooser_filesOfTypeLabelText, "Files of Type:" }, { StringId.FileChooser_lookInLabelText, "Look In:" }, { StringId.FileChooser_upFolderToolTipText, "Up One Level" }, { StringId.FileChooser_fileNameLabelText, "File Name:" }, { StringId.FileChooser_homeFolderToolTipText, "Home" }, { StringId.FileChooser_newFolderToolTipText, "Create New Folder" }, { StringId.FileChooser_listViewButtonToolTipTextlist, "" }, { StringId.FileChooser_detailsViewButtonToolTipText, "Details" }, { StringId.FileChooser_saveButtonText, "Save" }, { StringId.FileChooser_openButtonText, "Open" }, { StringId.FileChooser_cancelButtonText, "Cancel" }, { StringId.FileChooser_updateButtonText, "Update" }, { StringId.FileChooser_helpButtonText, "Help" }, { StringId.FileChooser_saveButtonToolTipText, "Save selected file" }, { StringId.FileChooser_openButtonToolTipText, "Open selected file" }, { StringId.FileChooser_cancelButtonToolTipText, "Abort file chooser dialog" }, { StringId.FileChooser_updateButtonToolTipText, "Update directory listing" }, { StringId.FileChooser_helpButtonToolTipText, "FileChooser help" }, { StringId.FileChooser_acceptAllFileFilterText, "All Files" }, { StringId.FileChooser_detailsViewButtonAccessibleName, "Details" }, { StringId.FileChooser_directoryDescriptionText, "Directory" }, { StringId.FileChooser_directoryOpenButtonText, "Open" }, { StringId.FileChooser_directoryOpenButtonToolTipText, "Open selected directory" }, { StringId.FileChooser_fileDescriptionText, "Generic File" }, { StringId.FileChooser_homeFolderAccessibleName, "Home" }, { StringId.FileChooser_listViewButtonAccessibleName, "List" }, { StringId.FileChooser_listViewButtonToolTipText, "List" }, { StringId.FileChooser_newFolderAccessibleName, "New Folder" }, { StringId.FileChooser_newFolderErrorText, "Error creating new folder" }, { StringId.FileChooser_openDialogTitleText, "Open" }, { StringId.FileChooser_saveDialogTitleText, "Save" }, { StringId.FileChooser_saveInLabelText, "Save In:" }, { StringId.FileChooser_upFolderAccessibleName, "Up" }, { StringId.FileChooser_upFolderToolTipText, "Up one level" }, { StringId.FILE_DOESNT_CONTAIN_TEXT_DATA, "File doesn't contain the text data" }, { StringId.LOADED_DATA, "Loaded dataset" }, { StringId.VALUES_DELIMITER, "Delimiter of values in line" }, { StringId.SPACES_AND_OR_TABS, "Spaces (or/and) tabulations" }, { StringId.ANOTHER, "Another" }, { StringId.FIRST_ROW_CONTAINS_COLUMN_NAMES, "First row contains column names" }, { StringId.FIRST_COLUMN_CONTAINS_IDENTIFIERS, "First column contains identifiers" }, { StringId.PRELOADING, "Preload data" }, { StringId.OF_FIRST, "first" }, { StringId.SAMPLES, "samples" }, { StringId.COLUMN, "Column" }, { StringId.EXCLUDE_FROM_DATA, "Exclude from data" }, { StringId.SETTINGS, "Settings" }, { StringId.NAME, "Name" }, { StringId.TYPE_OF_VALUES, "Type of values" }, { StringId.INPUT_VALUE, "Input value" }, { StringId.OUTPUT_VALUE, "Output value" }, { StringId.IDENTIFIER_COLUMN, "Identifier column" }, { StringId.LOAD_DATA, "Load data" }, { StringId.NUMBER_TYPE_IS_IMPOSSIBLE, "Type Number is impossible. Data contains not number values" }, { StringId.BOOLEAN_TYPE_IS_IMPOSSIBLE, "Type Boolean is impossible. " + "Not all data contains values True/False" }, { StringId.DATA_NOT_LOADED, "Data not loaded. Select dataset file and preload data" }, { StringId.PRELOAD_FINISHED, "Preloadiing finished. Specify, how columns in a dataset file " + "will be interpreted" }, { StringId.DATA_LOADED, "Data loaded successfull" }, { StringId.GO_TO_LEARNING, "Go to learning" }, { StringId.DESCRIPTION, "Description" }, { StringId.NETWORK, "Network" }, { StringId.PARAMETERS_STATE, "Parameters state" }, { StringId.LEARNING_STATE, "Learning state" }, { StringId.REFRESH_TABLE, "Refresh table" }, { StringId.BY_DEFAULT, "By default" }, { StringId.CHANGED, "Changed" }, { StringId.OPTIMIZED, "Optimized" }, { StringId.OPTIMIZED_AND_CHANGED, "Optimized & changed" }, { StringId.RADIAL_BASIS_FUNCTIONS_TRANSFORMATION, "Radial basis functions transformation" }, { StringId.TRANSFORM_INPUTS_USING_RBF, "Transform inputs using radial basis functions" }, { StringId.DATA_TRANSFORMATIONS, "Data transformations" }, { StringId.SHOW_DIAGRAM_FOR_CLASS, "Show diagram for class" }, { StringId.ADD_FORECAST_GROUP, "Add forecast group" }, { StringId.FORECAST_GROUP, "Forecast group" }, { StringId.COUNT, "Count" }, { StringId.INPUTS, "Inputs" }, { StringId.OUTPUTS, "Outputs" }, { StringId.NETWORKS_STATE, "Networks state" }, { StringId.TAKE_DATA_FROM, "Data for test will be taken from network" }, { StringId.TRAINED_NETWORKS, "Trained networks" }, { StringId.NETWORKS_IN_GROUP, "Networks belonging to group" }, { StringId.APPLY_CHANGES_AND_TEST, "Apply changes and perform test of grop" }, { StringId.ADD_TO_GROUP, "Add to group" }, { StringId.REMOVE_FROM_GROP, "Remove from group" }, { StringId.NETWORKS_GROP_FAIL_VALIDATION1, "All selected networks should have the same count of inputs and outputs" }, { StringId.NETWORKS_GROP_FAIL_VALIDATION2, "All selected networks should have the same count of inputs and outputs as in group" } };
    }

    @Override
    public String resolveNumeralEnd(int value) {
        return value == 1 ? "" : "s";
    }
}