package org.mmtk.plan.sapphire;

import org.mmtk.plan.*;
import org.mmtk.plan.concurrent.ConcurrentMutator;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.SegregatedFreeListSpace;
import org.mmtk.policy.Space;
import org.mmtk.utility.ForwardingWord;
import org.mmtk.utility.Log;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.utility.options.Options;
import org.mmtk.vm.VM;
import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

@Uninterruptible
public class SapphireMutator extends ConcurrentMutator {

    /****************************************************************************
   * Instance fields
   */
    protected final CopyLocal fromSpaceLocal;

    protected final CopyLocal toSpaceLocal;

    private TraceWriteBuffer mutatorLocalTraceWriteBuffer;

    /**
   * Constructor
   */
    public SapphireMutator() {
        fromSpaceLocal = new CopyLocal();
        toSpaceLocal = new CopyLocal();
        mutatorLocalTraceWriteBuffer = new TraceWriteBuffer(global().globalFirstTrace);
    }

    /** @return The active global plan as a <code>SS</code> instance. */
    @Inline
    private static Sapphire global() {
        return (Sapphire) VM.activePlan.global();
    }

    /**
   * Called before the MutatorContext is used, but after the context has been
   * fully registered and is visible to collection.
   */
    public void initMutator(int id) {
        super.initMutator(id);
        fromSpaceLocal.rebind(Sapphire.fromSpace());
        toSpaceLocal.rebind(Sapphire.toSpace());
    }

    /**
   * Allocate space (for an object)
   *
   * @param bytes The size of the space to be allocated (in bytes)
   * @param align The requested alignment.
   * @param offset The alignment offset.
   * @param allocator The allocator number to be used for this allocation
   * @param site Allocation site
   * @return The address of the first byte of the allocated region
   */
    @Inline
    public Address alloc(int bytes, int align, int offset, int allocator, int site) {
        Address addy;
        if (allocator == Sapphire.ALLOC_REPLICATING) addy = fromSpaceLocal.alloc(bytes, align, offset); else addy = super.alloc(bytes, align, offset, allocator, site);
        return addy;
    }

    /**
   * Perform post-allocation actions.  For many allocators none are
   * required.
   *
   * @param object The newly allocated object
   * @param typeRef The type reference for the instance being created
   * @param bytes The size of the space to be allocated (in bytes)
   * @param allocator The allocator number to be used for this allocation
   */
    @Inline
    public void postAlloc(ObjectReference object, ObjectReference typeRef, int bytes, int allocator, int align, int offset) {
        if (allocator == Sapphire.ALLOC_REPLICATING) {
            if (mutatorMustDoubleAllocate) {
                int alignedUpBytes = bytes + (MIN_ALIGNMENT - 1) & ~(MIN_ALIGNMENT - 1);
                Address toSpace = toSpaceLocal.alloc(alignedUpBytes, align, offset);
                ObjectReference newObject = VM.objectModel.fillInBlankDoubleRelica(object, toSpace, bytes);
                if (VM.VERIFY_ASSERTIONS) {
                    VM.assertions._assert(Sapphire.inToSpace(toSpace));
                    VM.assertions._assert(Sapphire.inToSpace(newObject));
                }
                VM.objectModel.writeReplicaPointer(object, newObject);
            }
            return;
        }
        postAlloc(object, typeRef, bytes, allocator);
    }

    public void postAlloc(ObjectReference object, ObjectReference typeRef, int bytes, int allocator) {
        if (mutatorMustDoubleAllocate) {
            if (allocator == Sapphire.ALLOC_CODE) {
                Plan.smallCodeSpace.initializeHeader(object, true);
                boolean result = Sapphire.smallCodeSpace.testAndMark(object);
                SegregatedFreeListSpace.markBlock(object);
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(result);
            } else if (allocator == Sapphire.ALLOC_NON_MOVING) {
                Plan.nonMovingSpace.initializeHeader(object, true);
                boolean result = Sapphire.nonMovingSpace.testAndMark(object);
                SegregatedFreeListSpace.markBlock(object);
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(result);
            } else if (allocator == Sapphire.ALLOC_LARGE_CODE) {
                Plan.largeCodeSpace.initializeHeader(object, false);
            } else if (allocator == Sapphire.ALLOC_LOS) {
                Plan.loSpace.initializeHeader(object, false);
            } else if (allocator == Sapphire.ALLOC_IMMORTAL) {
                Plan.immortalSpace.initializeHeader(object);
                Sapphire.immortalSpace.makeBlack(object);
            }
        } else {
            switch(allocator) {
                case Plan.ALLOC_LOS:
                    Plan.loSpace.initializeHeader(object, true);
                    return;
                case Plan.ALLOC_IMMORTAL:
                    Plan.immortalSpace.initializeHeader(object);
                    return;
                case Plan.ALLOC_CODE:
                    Plan.smallCodeSpace.initializeHeader(object, true);
                    return;
                case Plan.ALLOC_LARGE_CODE:
                    Plan.largeCodeSpace.initializeHeader(object, true);
                    return;
                case Plan.ALLOC_NON_MOVING:
                    Plan.nonMovingSpace.initializeHeader(object, true);
                    return;
                default:
                    VM.assertions.fail("No such allocator");
            }
        }
    }

    /**
   * Return the allocator instance associated with a space
   * <code>space</code>, for this plan instance.
   *
   * @param space The space for which the allocator instance is desired.
   * @return The allocator instance associated with this plan instance
   * which is allocating into <code>space</code>, or <code>null</code>
   * if no appropriate allocator can be established.
   */
    public Allocator getAllocatorFromSpace(Space space) {
        if (space == Sapphire.repSpace0 || space == Sapphire.repSpace1) return fromSpaceLocal;
        return super.getAllocatorFromSpace(space);
    }

    /**
   * Perform a per-mutator collection phase.
   *
   * @param phaseId The collection phase to perform
   * @param primary Perform any single-threaded activities using this thread.
   */
    @Inline
    public void collectionPhase(short phaseId, boolean primary) {
        if (phaseId == Sapphire.PREPARE) {
            super.collectionPhase(phaseId, primary);
            if (Sapphire.currentTrace == 0) {
            } else if (Sapphire.currentTrace == 1) {
                assertRemsetsFlushed();
            } else {
                VM.assertions.fail("Unknown Sapphire.currentTrace value");
            }
            return;
        }
        if (phaseId == Sapphire.PRE_TRACE_LINEAR_SCAN) {
            if (Sapphire.currentTrace == 0) {
                Log.writeln("Mutator # running preFirstPhaseFromSpaceLinearSanityScan and preFirstPhaseToSpaceLinearSanityScan ", getId());
                fromSpaceLocal.linearScan(Sapphire.preFirstPhaseFromSpaceLinearSanityScan);
                toSpaceLocal.linearScan(Sapphire.preFirstPhaseToSpaceLinearSanityScan);
                return;
            }
            if (Sapphire.currentTrace == 2) {
                Log.writeln("Mutator # running preSecondPhaseFromSpaceLinearSanityScan and preSecondPhaseToSpaceLinearSanityScan ", getId());
                fromSpaceLocal.linearScan(Sapphire.preSecondPhaseFromSpaceLinearSanityScan);
                toSpaceLocal.linearScan(Sapphire.preSecondPhaseToSpaceLinearSanityScan);
                return;
            }
        }
        if (phaseId == Sapphire.POST_TRACE_LINEAR_SCAN) {
            if (Sapphire.currentTrace == 1) {
                Log.writeln("Mutator # running postFirstPhaseFromSpaceLinearSanityScan and postFirstPhaseToSpaceLinearSanityScan ", getId());
                fromSpaceLocal.linearScan(Sapphire.postFirstPhaseFromSpaceLinearSanityScan);
                toSpaceLocal.linearScan(Sapphire.postFirstPhaseToSpaceLinearSanityScan);
                return;
            }
            if (Sapphire.currentTrace == 2) {
                Log.writeln("Mutator # running postSecondPhaseFromSpaceLinearSanityScan and postSecondPhaseToSpaceLinearSanityScan ", getId());
                fromSpaceLocal.linearScan(Sapphire.postSecondPhaseFromSpaceLinearSanityScan);
                toSpaceLocal.linearScan(Sapphire.postSecondPhaseToSpaceLinearSanityScan);
                return;
            }
        }
        if (phaseId == Simple.PREPARE_STACKS) {
            if (Options.verbose.getValue() >= 8) Log.writeln("Deferring preparing stack until we want to scan thread");
            return;
        }
        if (phaseId == Sapphire.RELEASE) {
            super.collectionPhase(phaseId, primary);
            if (Sapphire.currentTrace == 1) {
                mutatorMustReplicate = globalViewMutatorMustReplicate = true;
                mutatorMustDoubleAllocate = globalViewMutatorMustDoubleAllocate = true;
                insertionBarrier = globalViewInsertionBarrier = false;
                assertRemsetsFlushed();
            } else if (Sapphire.currentTrace == 2) {
                fromSpaceLocal.rebind(Sapphire.toSpace());
                toSpaceLocal.rebind(Sapphire.fromSpace());
                assertRemsetsFlushed();
            }
            return;
        }
        if (phaseId == Sapphire.COMPLETE) {
            super.collectionPhase(phaseId, primary);
            mutatorMustReplicate = globalViewMutatorMustReplicate = false;
            mutatorMustDoubleAllocate = globalViewMutatorMustDoubleAllocate = false;
            insertionBarrier = globalViewInsertionBarrier = false;
            assertRemsetsFlushed();
            return;
        }
        super.collectionPhase(phaseId, primary);
    }

    /**
   * Show the status of each of the allocators.
   */
    public final void show() {
        fromSpaceLocal.show();
        toSpaceLocal.show();
        los.show();
        immortal.show();
    }

    /**
   * The mutator is about to be cleaned up, make sure all local data is returned.
   */
    public void deinitMutator() {
        Sapphire.deadBumpPointersLock.acquire();
        Sapphire.deadFromSpaceBumpPointers.tackOn(fromSpaceLocal);
        Sapphire.deadToSpaceBumpPointers.tackOn(toSpaceLocal);
        Sapphire.deadBumpPointersLock.release();
        flushRememberedSets();
        super.deinitMutator();
    }

    /**
   * Write a boolean. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new boolean
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void booleanWrite(ObjectReference src, Address slot, boolean value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.booleanWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.booleanWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * A number of booleans are about to be copied from object <code>src</code> to object <code>dst</code> (as in an array copy).
   * Thus, <code>dst</code> is the mutated object. Take appropriate write barrier actions.
   * <p>
   * @param src The source array
   * @param srcOffset The starting source offset
   * @param dst The destination array
   * @param dstOffset The starting destination offset
   * @param bytes The number of bytes to be copied
   * @return True if the update was performed by the barrier, false if left to the caller
   */
    public boolean booleanBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
        return false;
    }

    /**
   * Write a byte. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new byte
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void byteWrite(ObjectReference src, Address slot, byte value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.byteWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.byteWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * A number of bytes are about to be copied from object <code>src</code> to object <code>dst</code> (as in an array copy). Thus,
   * <code>dst</code> is the mutated object. Take appropriate write barrier actions.
   * <p>
   * @param src The source array
   * @param srcOffset The starting source offset
   * @param dst The destination array
   * @param dstOffset The starting destination offset
   * @param bytes The number of bytes to be copied
   * @return True if the update was performed by the barrier, false if left to the caller
   */
    public boolean byteBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
        return false;
    }

    /**
   * Write a char. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new char
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void charWrite(ObjectReference src, Address slot, char value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.charWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.charWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * A number of chars are about to be copied from object <code>src</code> to object <code>dst</code> (as in an array copy). Thus,
   * <code>dst</code> is the mutated object. Take appropriate write barrier actions.
   * <p>
   * @param src The source array
   * @param srcOffset The starting source offset
   * @param dst The destination array
   * @param dstOffset The starting destination offset
   * @param bytes The number of bytes to be copied
   * @return True if the update was performed by the barrier, false if left to the caller
   */
    public boolean charBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
        return false;
    }

    /**
   * Write a double. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new double
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void doubleWrite(ObjectReference src, Address slot, double value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.doubleWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.doubleWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * A number of doubles are about to be copied from object <code>src</code> to object <code>dst</code> (as in an array copy). Thus,
   * <code>dst</code> is the mutated object. Take appropriate write barrier actions.
   * <p>
   * @param src The source array
   * @param srcOffset The starting source offset
   * @param dst The destination array
   * @param dstOffset The starting destination offset
   * @param bytes The number of bytes to be copied
   * @return True if the update was performed by the barrier, false if left to the caller
   */
    public boolean doubleBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
        return false;
    }

    /**
   * Write a float. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new float
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void floatWrite(ObjectReference src, Address slot, float value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.floatWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.floatWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * A number of floats are about to be copied from object <code>src</code> to object <code>dst</code> (as in an array copy). Thus,
   * <code>dst</code> is the mutated object. Take appropriate write barrier actions.
   * <p>
   * @param src The source array
   * @param srcOffset The starting source offset
   * @param dst The destination array
   * @param dstOffset The starting destination offset
   * @param bytes The number of bytes to be copied
   * @return True if the update was performed by the barrier, false if left to the caller
   */
    public boolean floatBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
        return false;
    }

    /**
   * Write a int. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new int
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void intWrite(ObjectReference src, Address slot, int value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.intWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.intWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * A number of ints are about to be copied from object <code>src</code> to object <code>dst</code> (as in an array copy). Thus,
   * <code>dst</code> is the mutated object. Take appropriate write barrier actions.
   * <p>
   * @param src The source array
   * @param srcOffset The starting source offset
   * @param dst The destination array
   * @param dstOffset The starting destination offset
   * @param bytes The number of bytes to be copied
   * @return True if the update was performed by the barrier, false if left to the caller
   */
    public boolean intBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
        return false;
    }

    /**
   * Attempt to atomically exchange the value in the given slot with the passed replacement value.
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param old The old int to be swapped out
   * @param value The new int
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   * @return True if the swap was successful.
   */
    public boolean intTryCompareAndSwap(ObjectReference src, Address slot, int old, int value, Word metaDataA, Word metaDataB, int mode) {
        if (VM.VERIFY_ASSERTIONS) {
            VM.assertions._assert(!Sapphire.inToSpace(slot));
            if (Sapphire.inFromSpace(slot)) VM.assertions.fail("Warning attempting intTryCompareAndSwap on object in Sapphire fromSpace");
        }
        return VM.barriers.intTryCompareAndSwap(src, old, value, metaDataA, metaDataB, mode);
    }

    /**
   * Write a long. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new long
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void longWrite(ObjectReference src, Address slot, long value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.longWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.longWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * A number of longs are about to be copied from object <code>src</code> to object <code>dst</code> (as in an array copy). Thus,
   * <code>dst</code> is the mutated object. Take appropriate write barrier actions.
   * <p>
   * @param src The source array
   * @param srcOffset The starting source offset
   * @param dst The destination array
   * @param dstOffset The starting destination offset
   * @param bytes The number of bytes to be copied
   * @return True if the update was performed by the barrier, false if left to the caller
   */
    public boolean longBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
        return false;
    }

    /**
   * Attempt to atomically exchange the value in the given slot with the passed replacement value.
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param old The old long to be swapped out
   * @param value The new long
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   * @return True if the swap was successful.
   */
    public boolean longTryCompareAndSwap(ObjectReference src, Address slot, long old, long value, Word metaDataA, Word metaDataB, int mode) {
        if (VM.VERIFY_ASSERTIONS) {
            VM.assertions._assert(!Sapphire.inToSpace(slot));
            if (Sapphire.inFromSpace(slot)) VM.assertions.fail("Warning attempting longTryCompareAndSwap on object in Sapphire fromSpace");
        }
        return VM.barriers.longTryCompareAndSwap(src, old, value, metaDataA, metaDataB, mode);
    }

    /**
   * Write a short. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new short
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void shortWrite(ObjectReference src, Address slot, short value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.shortWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.shortWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * A number of shorts are about to be copied from object <code>src</code> to object <code>dst</code> (as in an array copy). Thus,
   * <code>dst</code> is the mutated object. Take appropriate write barrier actions.
   * <p>
   * @param src The source array
   * @param srcOffset The starting source offset
   * @param dst The destination array
   * @param dstOffset The starting destination offset
   * @param bytes The number of bytes to be copied
   * @return True if the update was performed by the barrier, false if left to the caller
   */
    public boolean shortBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
        return false;
    }

    /**
   * Write a Word. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new Word
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void wordWrite(ObjectReference src, Address slot, Word value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.wordWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.wordWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * Write a Address during GC into toSpace. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new Address
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void addressWriteDuringGC(ObjectReference src, Address slot, Address value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.addressWrite(src, value, metaDataA, metaDataB, mode);
        if (VM.VERIFY_ASSERTIONS) {
            VM.assertions._assert(mutatorMustDoubleAllocate);
        }
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.addressWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * Attempt to atomically exchange the value in the given slot with the passed replacement value.
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param old The old long to be swapped out
   * @param value The new long
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   * @return True if the swap was successful.
   */
    public boolean wordTryCompareAndSwap(ObjectReference src, Address slot, Word old, Word value, Word metaDataA, Word metaDataB, int mode) {
        if (VM.VERIFY_ASSERTIONS) {
            VM.assertions._assert(!Sapphire.inToSpace(slot), "Warning attempting wordTryCompareAndSwap on object in Sapphire toSpace");
            VM.assertions._assert(!Sapphire.inFromSpace(slot), "Warning attempting wordTryCompareAndSwap on object in Sapphire fromSpace");
        }
        return VM.barriers.wordTryCompareAndSwap(src, old, value, metaDataA, metaDataB, mode);
    }

    /**
   * Stuff for address based hashing LPJH: nasty quick hack
   */
    Word HASH_STATE_UNHASHED = Word.zero();

    Word HASH_STATE_HASHED = Word.one().lsh(8);

    Word HASH_STATE_HASHED_AND_MOVED = Word.fromIntZeroExtend(3).lsh(8);

    Word HASH_STATE_MASK = HASH_STATE_UNHASHED.or(HASH_STATE_HASHED).or(HASH_STATE_HASHED_AND_MOVED);

    public boolean tryStatusWordCompareAndSwap(ObjectReference src, Word old, Word value) {
        if (VM.VERIFY_ASSERTIONS) {
            VM.assertions._assert(!Sapphire.inToSpace(src));
        }
        if (Sapphire.inFromSpace(src)) {
            Word debugPrevValue = ForwardingWord.atomicMarkBusy(src);
            if (VM.VERIFY_ASSERTIONS) {
                VM.assertions._assert(Sapphire.inFromSpace(src));
            }
            old = old.or(Word.fromIntZeroExtend(ForwardingWord.BUSY));
            value = value.or(Word.fromIntZeroExtend(ForwardingWord.BUSY));
            if (VM.barriers.statusWordTryCompareAndSwap(src, old, value)) {
                if (VM.VERIFY_ASSERTIONS) {
                    VM.assertions._assert(ForwardingWord.isBusy(src));
                }
                ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
                if (forwarded != null) {
                    if (VM.VERIFY_ASSERTIONS) {
                        VM.assertions._assert(Sapphire.inToSpace(forwarded));
                        VM.assertions._assert(!ForwardingWord.isBusy(forwarded));
                        Word fromStatusHashState = VM.objectModel.readAvailableBitsWord(src).and(HASH_STATE_MASK);
                        Word toStatusHashState = VM.objectModel.readAvailableBitsWord(forwarded).and(HASH_STATE_MASK);
                        if (fromStatusHashState.EQ(HASH_STATE_HASHED)) {
                            VM.assertions._assert(toStatusHashState.EQ(HASH_STATE_HASHED_AND_MOVED));
                        } else if (fromStatusHashState.EQ(HASH_STATE_HASHED_AND_MOVED)) {
                            VM.assertions._assert(toStatusHashState.EQ(HASH_STATE_HASHED_AND_MOVED));
                        } else {
                        }
                    }
                    Word diff = old.xor(value);
                    Word toSpaceStatusWord;
                    do {
                        toSpaceStatusWord = VM.objectModel.readAvailableBitsWord(forwarded);
                    } while (!VM.barriers.statusWordTryCompareAndSwap(forwarded, toSpaceStatusWord, toSpaceStatusWord.xor(diff)));
                    if (VM.VERIFY_ASSERTIONS) {
                        VM.assertions._assert(!ForwardingWord.isBusy(forwarded));
                        Word fromStatusHashState = VM.objectModel.readAvailableBitsWord(src).and(HASH_STATE_MASK);
                        Word toStatusHashState = VM.objectModel.readAvailableBitsWord(forwarded).and(HASH_STATE_MASK);
                        if (fromStatusHashState.EQ(HASH_STATE_HASHED)) {
                            VM.assertions._assert(toStatusHashState.EQ(HASH_STATE_HASHED_AND_MOVED));
                        } else if (fromStatusHashState.EQ(HASH_STATE_HASHED_AND_MOVED)) {
                            VM.assertions._assert(toStatusHashState.EQ(HASH_STATE_HASHED_AND_MOVED));
                        } else {
                        }
                    }
                }
                ForwardingWord.markNotBusy(src, value);
                return true;
            } else {
                ForwardingWord.markNotBusy(src, debugPrevValue);
                return false;
            }
        } else {
            if (VM.VERIFY_ASSERTIONS) {
                VM.assertions._assert(!Sapphire.inToSpace(src));
                VM.assertions._assert(!Sapphire.inFromSpace(src));
            }
            return VM.barriers.statusWordTryCompareAndSwap(src, old, value);
        }
    }

    /**
   * Write a Address. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new Address
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void addressWrite(ObjectReference src, Address slot, Address value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.addressWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.addressWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * Write a Extent. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new Extent
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void extentWrite(ObjectReference src, Address slot, Extent value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.extentWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.extentWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * Write a Offset. Take appropriate write barrier actions.
   * <p>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new Offset
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void offsetWrite(ObjectReference src, Address slot, Offset value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.offsetWrite(src, value, metaDataA, metaDataB, mode);
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.offsetWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * Read a reference. Take appropriate read barrier action, and return the value that was read.
   * <p>
   * This is a <b>substituting<b> barrier. The call to this barrier takes the place of a load.
   * <p>
   * @param src The object reference holding the field being read.
   * @param slot The address of the slot being read.
   * @param metaDataA A value that assists the host VM in creating a load
   * @param metaDataB A value that assists the host VM in creating a load
   * @param mode The context in which the load occurred
   * @return The reference that was read.
   */
    @Inline
    @Override
    public ObjectReference objectReferenceRead(ObjectReference src, Address slot, Word metaDataA, Word metaDataB, int mode) {
        ObjectReference obj = VM.barriers.objectReferenceRead(src, metaDataA, metaDataB, mode);
        if (VM.VERIFY_ASSERTIONS) {
            if (!obj.isNull()) VM.assertions._assert(!Sapphire.inToSpace(obj));
        }
        return obj;
    }

    /**
   * Write an object reference. Take appropriate write barrier actions.
   * <p>
   * <b>By default do nothing, override if appropriate.</b>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param value The value of the new reference
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   */
    public void objectReferenceWrite(ObjectReference src, Address slot, ObjectReference value, Word metaDataA, Word metaDataB, int mode) {
        VM.barriers.objectReferenceWrite(src, value, metaDataA, metaDataB, mode);
        checkAndEnqueueReference(value);
        if (VM.VERIFY_ASSERTIONS && !Sapphire.gcInProgress()) {
            VM.assertions._assert(!Sapphire.inToSpace(slot));
            if (!value.isNull()) VM.assertions._assert(!Sapphire.inToSpace(value));
        }
        writeBarrierAssertions(slot, src);
        if (mutatorMustReplicate && Sapphire.inFromSpace(slot)) {
            ObjectReference forwarded = ForwardingWord.getReplicaPointer(src);
            if (forwarded != null) {
                if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Sapphire.inToSpace(forwarded));
                VM.barriers.objectReferenceWrite(forwarded, value, metaDataA, metaDataB, mode);
            }
        }
    }

    /**
   * A number of references are about to be copied from object <code>src</code> to object <code>dst</code> (as in an array copy).
   * Thus, <code>dst</code> is the mutated object. Take appropriate write barrier actions.
   * <p>
   * @param src The source array
   * @param srcOffset The starting source offset
   * @param dst The destination array
   * @param dstOffset The starting destination offset
   * @param bytes The number of bytes to be copied
   * @return True if the update was performed by the barrier, false if left to the caller (always false in this case).
   */
    public boolean objectReferenceBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
        return false;
    }

    /**
   * Attempt to atomically exchange the value in the given slot with the passed replacement value. If a new reference is created, we
   * must then take appropriate write barrier actions.
   * <p>
   * <b>By default do nothing, override if appropriate.</b>
   * @param src The object into which the new reference will be stored
   * @param slot The address into which the new reference will be stored.
   * @param old The old reference to be swapped out
   * @param tgt The target of the new reference
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   * @param mode The context in which the store occurred
   * @return True if the swap was successful.
   */
    public boolean objectReferenceTryCompareAndSwap(ObjectReference src, Address slot, ObjectReference old, ObjectReference tgt, Word metaDataA, Word metaDataB, int mode) {
        if (VM.VERIFY_ASSERTIONS) {
            VM.assertions._assert(!Sapphire.inToSpace(slot));
            if (Sapphire.inFromSpace(slot)) VM.assertions.fail("Warning attempting objectTryCompareAndSwap on object in Sapphire fromSpace");
            if (!tgt.isNull()) VM.assertions._assert(!Sapphire.inToSpace(tgt));
        }
        boolean result = VM.barriers.objectReferenceTryCompareAndSwap(src, old, tgt, metaDataA, metaDataB, mode);
        if (result) checkAndEnqueueReference(tgt);
        return result;
    }

    /**
   * Flush per-mutator remembered sets into the global remset pool.
   */
    public final void flushRememberedSets() {
        if (Options.verbose.getValue() >= 8) Log.writeln("Flushing #", getId());
        mutatorLocalTraceWriteBuffer.flush();
        assertRemsetsFlushed();
    }

    /**
   * Assert that the remsets have been flushed. This is critical to correctness. We need to maintain the invariant that remset
   * entries do not accrue during GC. If the host JVM generates barrier entires it is its own responsibility to ensure that they are
   * flushed before returning to MMTk.
   */
    public final void assertRemsetsFlushed() {
        if (VM.VERIFY_ASSERTIONS) {
            VM.assertions._assert(mutatorLocalTraceWriteBuffer.isFlushed());
        }
    }

    /**
   * Process a reference that may require being enqueued as part of a concurrent
   * collection.
   *
   * @param ref The reference to check.
   */
    public void checkAndEnqueueReference(ObjectReference ref) {
        if (insertionBarrier && !ref.isNull()) {
            if (Sapphire.inFromSpace(ref)) Sapphire.fromSpace().traceObject(mutatorLocalTraceWriteBuffer, ref); else if (Space.isInSpace(Sapphire.IMMORTAL, ref)) Sapphire.immortalSpace.traceObject(mutatorLocalTraceWriteBuffer, ref); else if (Space.isInSpace(Sapphire.LOS, ref)) Sapphire.loSpace.traceObject(mutatorLocalTraceWriteBuffer, ref); else if (Space.isInSpace(Sapphire.NON_MOVING, ref)) Sapphire.nonMovingSpace.traceObject(mutatorLocalTraceWriteBuffer, ref); else if (Space.isInSpace(Sapphire.SMALL_CODE, ref)) Sapphire.smallCodeSpace.traceObject(mutatorLocalTraceWriteBuffer, ref); else if (Space.isInSpace(Sapphire.LARGE_CODE, ref)) Sapphire.largeCodeSpace.traceObject(mutatorLocalTraceWriteBuffer, ref); else if (VM.VERIFY_ASSERTIONS) {
                VM.assertions._assert(!Sapphire.inToSpace(ref));
                VM.assertions._assert(Space.isInSpace(Sapphire.VM_SPACE, ref));
            }
            if (VM.VERIFY_ASSERTIONS) {
                VM.assertions._assert(VM.objectModel.validRef(ref));
                if (!Plan.gcInProgress()) {
                    if (Space.isInSpace(Sapphire.SS0, ref)) VM.assertions._assert(Sapphire.repSpace0.isLive(ref)); else if (Space.isInSpace(Sapphire.SS1, ref)) VM.assertions._assert(Sapphire.repSpace1.isLive(ref)); else if (Space.isInSpace(Sapphire.IMMORTAL, ref)) VM.assertions._assert(Sapphire.immortalSpace.isLive(ref)); else if (Space.isInSpace(Sapphire.LOS, ref)) VM.assertions._assert(Sapphire.loSpace.isLive(ref)); else if (Space.isInSpace(Sapphire.NON_MOVING, ref)) VM.assertions._assert(Sapphire.nonMovingSpace.isLive(ref)); else if (Space.isInSpace(Sapphire.SMALL_CODE, ref)) VM.assertions._assert(Sapphire.smallCodeSpace.isLive(ref)); else if (Space.isInSpace(Sapphire.LARGE_CODE, ref)) VM.assertions._assert(Sapphire.largeCodeSpace.isLive(ref));
                }
            }
        }
    }

    /**
   * A new reference is about to be created in a location that is not
   * a regular heap object.  Take appropriate write barrier actions.<p>
   *
   * In this case, we remember the address of the source of the
   * pointer if the new reference points into the nursery from
   * non-nursery space.
   *
   * @param slot The address into which the new reference will be stored.
   * @param tgt The target of the new reference
   * @param metaDataA A value that assists the host VM in creating a store
   * @param metaDataB A value that assists the host VM in creating a store
   */
    @Inline
    public final void objectReferenceNonHeapWrite(Address slot, ObjectReference tgt, Word metaDataA, Word metaDataB) {
        checkAndEnqueueReference(tgt);
        VM.barriers.objectReferenceNonHeapWrite(slot, tgt, metaDataA, metaDataB);
    }

    private void writeBarrierAssertions(Address slot, ObjectReference src) {
        if (VM.VERIFY_ASSERTIONS) {
            if (Sapphire.inToSpace(slot)) writeBarrierAssertionFailure(slot, src);
            if (Sapphire.inFromSpace(slot)) {
                if (!ForwardingWord.getReplicaPointer(src).isNull()) {
                    if (Sapphire.currentTrace == 0) writeBarrierAssertionFailure(slot, src); else if (Sapphire.currentTrace == 1) {
                        if (!insertionBarrier) writeBarrierAssertionFailure(slot, src);
                        if (!MutatorContext.globalViewInsertionBarrier) writeBarrierAssertionFailure(slot, src);
                        if (!MutatorContext.globalViewMutatorMustDoubleAllocate) writeBarrierAssertionFailure(slot, src);
                    } else {
                        if (Sapphire.currentTrace != 2) writeBarrierAssertionFailure(slot, src);
                        if (!mutatorMustDoubleAllocate) writeBarrierAssertionFailure(slot, src);
                        if (!MutatorContext.globalViewMutatorMustDoubleAllocate) writeBarrierAssertionFailure(slot, src);
                        if (insertionBarrier) writeBarrierAssertionFailure(slot, src);
                        if (MutatorContext.globalViewInsertionBarrier) writeBarrierAssertionFailure(slot, src);
                    }
                }
            }
        }
    }

    private void writeBarrierAssertionFailure(Address slot, ObjectReference src) {
        Log.write("Thread #", getId());
        Log.write(" writing to slot ", slot);
        Log.write(" of object ");
        Log.write(src);
        Log.write(" which has a FP value of ");
        Log.writeln(ForwardingWord.getReplicaPointer(src));
        Log.write("MutatorContext.globalViewMutatorMustDoubleAllocate is ");
        Log.writeln(MutatorContext.globalViewMutatorMustDoubleAllocate ? 1 : 0);
        Log.write("MutatorContext.globalViewMutatorInsertionBarrier is ");
        Log.writeln(MutatorContext.globalViewInsertionBarrier ? 1 : 0);
        Log.write("MutatorContext.globalViewMutatorMustReplicate is ");
        Log.writeln(MutatorContext.globalViewMutatorMustReplicate ? 1 : 0);
        Log.writeln("Insertion barrier is ", insertionBarrier ? 1 : 0);
        Log.writeln("Double alloc barrier is ", mutatorMustDoubleAllocate ? 1 : 0);
        Log.writeln("Replication barrier is ", mutatorMustReplicate ? 1 : 0);
        VM.assertions.fail("writeBarrierAssertionFailure - look at call site for cause");
    }

    /**
   * Read a reference type. In a concurrent collector this may
   * involve adding the referent to the marking queue.
   *
   * @param ref The referent being read.
   * @return The new referent.
   */
    @Inline
    @Override
    public ObjectReference javaLangReferenceReadBarrier(ObjectReference ref) {
        if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(false);
        return ObjectReference.nullReference();
    }
}
