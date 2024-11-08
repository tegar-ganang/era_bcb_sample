package cunei.cli;

import java.io.BufferedReader;
import java.io.PrintStream;
import cunei.config.EnumParameter;
import cunei.config.FileParameter;
import cunei.config.FloatParameter;
import cunei.config.IntegerParameter;
import cunei.config.Parameter;
import cunei.config.ValueOfParameter;
import cunei.corpus.Language;
import cunei.corpus.Languages;
import cunei.document.DocumentReader;
import cunei.document.DocumentReaders;
import cunei.document.DocumentWriter;
import cunei.document.MultiFileDocumentReader;
import cunei.document.Phrase;
import cunei.document.PreSampledDocumentReader;
import cunei.document.SampledDocumentReader;
import cunei.document.Sentence;
import cunei.document.SimpleDocumentWriter;
import cunei.util.BufferedReaderManager;

public class ProcessDocument extends MultiFileDocumentArguments {

    private FileParameter inputDirArgument;

    private EnumParameter<DocumentReaders> readerArgument;

    private FileParameter sampleSentencesArgument;

    private FloatParameter sampleFrequencyArgument;

    private IntegerParameter sampleWindowArgument;

    private ValueOfParameter<Language> langArgument;

    public static void main(String[] arguments) {
        new ProcessDocument(arguments).run();
    }

    public ProcessDocument(String[] arguments) {
        super(arguments, true);
    }

    protected void initialize() {
        super.initialize();
        inputDirArgument = FileParameter.get(commandArguments, "input-dir", null);
        readerArgument = EnumParameter.get(commandArguments, "reader", DocumentReaders.class, DocumentReaders.SIMPLE);
        sampleSentencesArgument = FileParameter.get(commandArguments, "sample-sentences", null);
        sampleFrequencyArgument = FloatParameter.get(commandArguments, "sample-freq", 1f);
        sampleWindowArgument = IntegerParameter.get(commandArguments, "sample-window", 1);
        langArgument = ValueOfParameter.get(commandArguments, "lang", Languages.class, Languages.SOURCE);
    }

    protected boolean validate() {
        final boolean useMulti = readerArgument.getValue() == DocumentReaders.MULTI;
        if (!inputDirArgument.isDefault() && !useMulti) {
            System.out.println("Argument '" + inputDirArgument.getName() + "' cannot be used when '" + readerArgument.getName() + "' is set to '" + readerArgument.getValueString() + "'");
            return false;
        }
        return useMulti ? super.validate() : true;
    }

    protected Parameter[] getRequired() {
        return new Parameter[] { langArgument };
    }

    ;

    protected void run(BufferedReader input, PrintStream output) {
        final DocumentWriter<Phrase> writer = new SimpleDocumentWriter(output, langArgument.getValue());
        final DocumentReaders readerType = readerArgument.getValue();
        DocumentReader<Phrase> reader = readerType.newInstance(input, langArgument.getValue());
        if (readerType == DocumentReaders.MULTI) init((MultiFileDocumentReader) reader, inputDirArgument.getValue());
        if (!sampleSentencesArgument.isDefault()) {
            final BufferedReader sampleSentencesReader = BufferedReaderManager.open(sampleSentencesArgument.getValue());
            reader = new PreSampledDocumentReader<Phrase>(reader, sampleSentencesReader);
        }
        reader = new SampledDocumentReader<Phrase>(reader, sampleFrequencyArgument.getValue(), sampleWindowArgument.getValue());
        Sentence<Phrase> sentence;
        while ((sentence = reader.next()) != null) writer.write(sentence);
        writer.complete();
    }
}
