package freeboogie.vcgen;

import freeboogie.ast.Command;

/**
  Computes the read index and the write index, optimizing the
  total number of variable versions.

  We have:
    r(n) = max_{m BEFORE n} w(m)
    w(n) = 1 + r(n)   if n writes to X
    w(n) = r(n)       otherwise

  @author rgrig
 */
public class Passivator extends AbstractPassivator {

    int computeReadIndex(Command c) {
        Integer alreadyComputed = readIndexCache().get(c);
        if (alreadyComputed != null) return alreadyComputed;
        int ri = 0;
        for (Command pre : parents(c)) ri = Math.max(ri, computeWriteIndex(pre));
        readIndexCache().put(c, ri);
        return ri;
    }

    int computeWriteIndex(Command c) {
        Integer alreadyComputed = writeIndexCache().get(c);
        if (alreadyComputed != null) return alreadyComputed;
        int wi = computeReadIndex(c);
        if (writes(c)) ++wi;
        writeIndexCache().put(c, wi);
        return wi;
    }
}
