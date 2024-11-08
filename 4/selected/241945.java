package cunei.cli;

import java.io.BufferedReader;
import java.io.PrintStream;
import cunei.config.EnumParameter;
import cunei.config.FileParameter;
import cunei.config.FloatParameter;
import cunei.config.IntegerParameter;
import cunei.config.ValueOfParameter;
import cunei.corpus.CorpusReader;
import cunei.corpus.CorpusReaders;
import cunei.corpus.CorpusWriter;
import cunei.corpus.CorpusWriters;
import cunei.corpus.Language;
import cunei.corpus.LanguagePair;
import cunei.corpus.Languages;
import cunei.corpus.MultiFileCorpusReader;
import cunei.corpus.MultiFileCorpusWriter;
import cunei.corpus.MultiSentence;
import cunei.corpus.SampledCorpusReader;

public class ProcessCorpus extends MultiFileCorpusArguments {

    private FileParameter inputDirArgument;

    private FileParameter outputDirArgument;

    private EnumParameter<CorpusReaders> readerArgument;

    private EnumParameter<CorpusWriters> writerArgument;

    private FloatParameter sampleFrequencyArgument;

    private IntegerParameter sampleWindowArgument;

    private ValueOfParameter<Language> sourceLanguageArgument;

    private ValueOfParameter<Language> targetLanguageArgument;

    public static void main(String[] arguments) {
        new ProcessCorpus(arguments).run();
    }

    public ProcessCorpus(String[] arguments) {
        super(arguments, true);
    }

    protected void initialize() {
        super.initialize();
        inputDirArgument = FileParameter.get(commandArguments, "input-dir", null);
        outputDirArgument = FileParameter.get(commandArguments, "output-dir", null);
        readerArgument = EnumParameter.get(commandArguments, "reader", CorpusReaders.class, CorpusReaders.MULTI);
        writerArgument = EnumParameter.get(commandArguments, "writer", CorpusWriters.class, CorpusWriters.MULTI);
        sampleFrequencyArgument = FloatParameter.get(commandArguments, "sample-freq", 1f);
        sampleWindowArgument = IntegerParameter.get(commandArguments, "sample-window", 1);
        sourceLanguageArgument = ValueOfParameter.get(commandArguments, "src-lang", Languages.class, Languages.SOURCE);
        targetLanguageArgument = ValueOfParameter.get(commandArguments, "tgt-lang", Languages.class, Languages.TARGET);
    }

    protected boolean validate() {
        final boolean useMultiInput = readerArgument.getValue() == CorpusReaders.MULTI;
        final boolean useMultiOutput = writerArgument.getValue() == CorpusWriters.MULTI;
        if (!inputDirArgument.isDefault() && !useMultiInput) {
            System.out.println("Argument '" + inputDirArgument.getName() + "' cannot be used when '" + readerArgument.getName() + "' is set to '" + readerArgument.getValueString() + "'");
            return false;
        }
        if (!outputDirArgument.isDefault() && !useMultiOutput) {
            System.out.println("Argument '" + outputDirArgument.getName() + "' and '" + writerArgument.getName() + " cannot both be used");
            return false;
        }
        if (sourceLanguageArgument.getValue() == targetLanguageArgument.getValue()) {
            System.out.println("Source language and target language cannot be the same");
            return false;
        }
        return useMultiInput || useMultiOutput ? super.validate() : true;
    }

    protected void run(BufferedReader input, PrintStream output) {
        final LanguagePair languagePair = new LanguagePair(sourceLanguageArgument.getValue(), targetLanguageArgument.getValue());
        final CorpusWriters writerType = writerArgument.getValue();
        final CorpusWriter writer = writerType.newInstance(output, languagePair);
        if (writerType == CorpusWriters.MULTI) init((MultiFileCorpusWriter) writer, outputDirArgument.getValue(), languagePair);
        final CorpusReaders readerType = readerArgument.getValue();
        CorpusReader reader = readerType.newInstance(input, languagePair);
        if (readerType == CorpusReaders.MULTI) init((MultiFileCorpusReader) reader, inputDirArgument.getValue(), languagePair);
        reader = new SampledCorpusReader(reader, sampleFrequencyArgument.getValue(), sampleWindowArgument.getValue());
        MultiSentence sentence;
        while ((sentence = reader.next()) != null) writer.write(sentence);
        writer.complete();
    }
}
