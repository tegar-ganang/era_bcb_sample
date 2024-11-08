package com.infomancers.collections.yield.asm;

import com.infomancers.collections.yield.asmbase.AbstractYielderTransformer;
import com.infomancers.collections.yield.asmbase.YielderInformationContainer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * A class transformation file which creates three chains of ASM visitors, all three
 * eventually create the enhanced class which keeps state and supports the yield idea.
 * <p/>
 * The chains are:
 * <p/>
 * 1. reader (of origin) -> returnCounter -> assignMapper -> null
 * 2. reader (of origin) -> promoter -> stateKeeper (using returnCounter) -> writer (to output1)
 * 3. reader (of output1) -> assigner (using promoter) -> writer (to output2)
 * <p/>
 * And then returns output2.
 * <p/>
 * Also, notice that the order of the visitors is important:
 * The promoter counts on the labels to be in the exact same order as the assignMapper sees them.
 */
public final class StreamingYielderTransformer extends AbstractYielderTransformer {

    public StreamingYielderTransformer(boolean debug) {
        super(debug);
    }

    protected byte[] enhanceClass(ClassReader reader, YielderInformationContainer info) {
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        CastChecker caster = new CastChecker(writer);
        StateKeeper stateKeeper = new StateKeeper(caster, info);
        LocalVariablePromoter promoter = new LocalVariablePromoter(stateKeeper, info);
        reader.accept(promoter, 0);
        return writer.toByteArray();
    }
}
