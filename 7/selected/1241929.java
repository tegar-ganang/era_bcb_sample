package org.op4j.target;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.op4j.exceptions.ExecutionException;
import org.op4j.functions.IFunction;
import org.op4j.target.Target.Structure;

/**
 * 
 * @since 1.0
 * 
 * @author Daniel Fern&aacute;ndez
 *
 */
final class ExecutionTargetMapIfNotNullOperation implements ExecutionTargetOperation {

    private final Structure structure;

    private final IFunction<Object, Object> executable;

    private final Class<?> arrayComponentClass;

    @SuppressWarnings("unchecked")
    public ExecutionTargetMapIfNotNullOperation(final Structure structure, final IFunction<?, ?> executable, final Class<?> arrayComponentClass) {
        super();
        this.structure = structure;
        this.executable = (IFunction<Object, Object>) executable;
        this.arrayComponentClass = arrayComponentClass;
    }

    private static int[] addIndex(final int[] indexes, final int newIndex, final boolean excludeFirstIndex) {
        int[] newIndices = null;
        if (excludeFirstIndex) {
            newIndices = new int[indexes.length];
            for (int i = 0, z = indexes.length - 1; i < z; i++) {
                newIndices[i] = indexes[i + 1];
            }
        } else {
            newIndices = new int[indexes.length + 1];
            for (int i = 0, z = indexes.length; i < z; i++) {
                newIndices[i] = indexes[i];
            }
        }
        newIndices[newIndices.length - 1] = newIndex;
        return newIndices;
    }

    public Object execute(final Object target, final ExecutionTargetOperation[][] operations, final int[] indexes) {
        if (target == null) {
            throw new IllegalArgumentException("Cannot iterate on null: the \"map if not null\"" + "operation allows any element of the iterated structure to be null, but " + "not the structure being iterated itself.");
        }
        try {
            switch(this.structure) {
                case ARRAY:
                    final Object[] arrayTarget = (Object[]) target;
                    final Object[] arrayResult = (Object[]) Array.newInstance(this.arrayComponentClass, arrayTarget.length);
                    for (int i = 0, z = arrayTarget.length; i < z; i++) {
                        arrayResult[i] = (arrayTarget[i] == null ? null : this.executable.execute(arrayTarget[i], new ExecCtxImpl(addIndex(indexes, i, false))));
                    }
                    return arrayResult;
                case LIST:
                    final List<?> listTarget = (List<?>) target;
                    final List<Object> listResult = new ArrayList<Object>();
                    int iList = 0;
                    for (final Object element : listTarget) {
                        listResult.add((element == null ? null : this.executable.execute(element, new ExecCtxImpl(addIndex(indexes, iList, false)))));
                        iList++;
                    }
                    return listResult;
                case SET:
                    final Set<?> setTarget = (Set<?>) target;
                    final Set<Object> setResult = new LinkedHashSet<Object>();
                    int iSet = 0;
                    for (final Object element : setTarget) {
                        setResult.add((element == null ? null : this.executable.execute(element, new ExecCtxImpl(addIndex(indexes, iSet, false)))));
                        iSet++;
                    }
                    return setResult;
                default:
                    throw new IllegalStateException("Unsupported structure: " + this.structure);
            }
        } catch (ExecutionException e) {
            throw e;
        } catch (Throwable t) {
            throw new ExecutionException(t);
        }
    }
}
