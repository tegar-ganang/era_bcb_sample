package org.norecess.noparagraph;

import java.io.IOException;
import org.norecess.nolatte.ast.IText;
import org.norecess.nolatte.ast.support.IDatumFactory;
import org.norecess.nolatte.system.IReaderFactory;
import org.norecess.nolatte.system.IStringWriter;
import org.norecess.nolatte.system.IWriterFactory;
import org.norecess.noparagraph.frontend.IParagraphInserterFactory;
import org.norecess.noparagraph.statemachine.IParagraphInserterStateMachine;
import org.norecess.noparagraph.statemachine.IParagraphInserterStateMachineFactory;
import org.norecess.noparagraph.statemachine.ParagraphInserterStateMachine;

public class ParagraphInserter implements IParagraphInserter {

    private static final long serialVersionUID = -2175099214406399679L;

    private final IDatumFactory myDatumFactory;

    private final IWriterFactory myWriterFactory;

    private final IReaderFactory myReaderFactory;

    private final IParagraphInserterFactory myInserterFactory;

    private final IParagraphInserterStateMachineFactory myStateMachineFactory;

    public ParagraphInserter(IDatumFactory datumFactory, IWriterFactory writerFactory, IReaderFactory readerFactory, IParagraphInserterFactory inserterFactory, IParagraphInserterStateMachineFactory stateMachineFactory) {
        myDatumFactory = datumFactory;
        myWriterFactory = writerFactory;
        myReaderFactory = readerFactory;
        myInserterFactory = inserterFactory;
        myStateMachineFactory = stateMachineFactory;
    }

    public IText process(IText input) throws IOException {
        IStringWriter writer = myWriterFactory.createStringWriter();
        IParagraphInserterStateMachine machine = myStateMachineFactory.create(myInserterFactory.createTokenSource(myReaderFactory.createStringReader(input.getString())), myWriterFactory.createPrintWriter(writer));
        machine.process(ParagraphInserterStateMachine.State.BODY_START);
        return myDatumFactory.createText(writer.getBuffer().toString());
    }
}
