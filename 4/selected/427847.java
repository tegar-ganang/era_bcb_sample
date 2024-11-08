package com.syntazo.coffeegrains;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.python.core.PyFile;

public class Iteration {

    public static final Iterator<Object> Null = new Iterator<Object>() {

        public boolean hasNext() {
            return false;
        }

        public Object next() {
            return null;
        }

        public void remove() {
        }
    };

    public abstract static interface Lambda {

        public Object process(final Object... args);
    }

    public static class Tuple {

        public Object[] values;

        public Tuple(Object... args) {
            values = args;
        }

        @Override
        public String toString() {
            return "(" + str(values) + ")";
        }
    }

    public static class Enum {

        public final int index;

        public final Object value;

        public Enum(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        @Override
        public String toString() {
            return "(" + index + "," + value + ")";
        }
    }

    public static Object[] tuple(final Object... args) {
        return args;
    }

    public static interface IteratorMaker<T> {

        public Iterator<T> iterator();
    }

    public static Iterable<Object> buildObjectIterable(final IteratorMaker<Object> iteratorMaker) {
        return new Iterable<Object>() {

            public Iterator<Object> iterator() {
                return iteratorMaker.iterator();
            }

            @Override
            public String toString() {
                return "(" + str(this) + ")";
            }
        };
    }

    public static Iterable<Integer> buildIntegerIterable(final IteratorMaker<Integer> iteratorMaker) {
        return new Iterable<Integer>() {

            public Iterator<Integer> iterator() {
                return iteratorMaker.iterator();
            }

            @Override
            public String toString() {
                return "(" + str(this) + ")";
            }
        };
    }

    /**
     * Returns an <code>Iterable</code> object from the <code>varargs</code> or a given array.
     * Should an array be passed, it will be used as the underlying.
     *
     * @param  elements a vararg of objects or types.
     * @return an iterable.
     */
    public static Iterable iterable(final Object... elements) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    int index = 0;

                    public boolean hasNext() {
                        return index < elements.length;
                    }

                    public Object next() {
                        return elements[index++];
                    }

                    public void remove() {
                    }
                };
            }
        });
    }

    /**
     * Returns an <code>Iterable</code> of Integers in the given the range from (0..stop] where the step is 1.
     * There is no underlying.
     *
     * @param  stop, an integer.
     * @return an <code>Iterable</code> of Integers.
     * 
     * @see range(start, stop, step)
     */
    public static Iterable<Integer> range(final int stop) {
        return range(0, stop);
    }

    /**
     * Returns an <code>Iterable</code> of Integers in the given the range from (start..stop] where the step is 1.
     * There is no underlying.
     *
     * @param  start, an integer.
     * @param  stop, an integer.
     * @return an <code>Iterable</code> of Integers.
     * 
     * @see range(start, stop, step)
     */
    public static Iterable<Integer> range(final int start, final int stop) {
        return range(start, stop, 1);
    }

    /**
     * Returns an <code>Iterable</code> of Integers in the given the range from (start..stop] where the step is given.
     * There is no underlying.
     *
     * @param  start, an integer.
     * @param  stop, an integer.
     * @return an <code>Iterable</code> of Integers.
     * 
     * @see range(start, stop, step)
     */
    public static Iterable<Integer> range(final int start, final int stop, final int step) {
        return buildIntegerIterable(new IteratorMaker<Integer>() {

            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    int index = start;

                    public boolean hasNext() {
                        return index < stop;
                    }

                    public Integer next() {
                        int i = index;
                        index += step;
                        return i;
                    }

                    public void remove() {
                    }
                };
            }
        });
    }

    /**
     * Returns an <code>Iterable</code> of <code>Enums</code> given an Iterable.

     *
     * @param  iterable, an <code>Iterable</code>
     * @return an <code>Iterable</code> of <code>Enums</code>.
     * 
     * @see <code>Enums</code>.
     */
    public static Iterable<Enum> enumerate(final Iterable<?> iterable) {
        return new Iterable<Enum>() {

            public Iterator<Enum> iterator() {
                return new Iterator<Enum>() {

                    int index = 0;

                    Iterator<?> iterator = iterable != null ? iterable.iterator() : Null;

                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public Enum next() {
                        return new Enum(index++, iterator.next());
                    }

                    public void remove() {
                    }
                };
            }

            @Override
            public String toString() {
                return "(" + str(this) + ")";
            }
        };
    }

    /**
     * Makes an iterable that returns selected elements from the underlying iterable. 
     * If stop is -1, then iteration continues until the iterator is exhausted, 
     * if at all; otherwise, it stops at the specified position. 
     * Step = 1.
     *
     * @param  iterable, an <code>Iterable</code>
     * @param  stop.
     * @return an <code>Iterable</code>.
     * 
     * @see <code>slice</code>.
     */
    public static Iterable<Object> slice(Iterable<?> iterable, int stop) {
        return slice(iterable, 0, stop);
    }

    /**
     * Makes an iterable that returns selected elements from the underlying iterable. 
     * If start is non-zero, then elements from the iterable are skipped until start 
     * is reached. Afterward, elements are returned consecutively unless step is set 
     * higher than one which results in items being skipped. 
     * If stop is -1, then iteration continues until the iterator is exhausted, 
     * if at all; otherwise, it stops at the specified position. 
     *
     * @param  iterable, an <code>Iterable</code>
     * @param  start.
     * @param  stop.
     * @return an <code>Iterable</code>.
     * 
     * @see <code>slice</code>.
     */
    public static Iterable<Object> slice(Iterable<?> iterable, int start, int stop) {
        return slice(iterable, start, stop, 1);
    }

    /**
     * Makes an iterable that returns selected elements from the underlying iterable. 
     * If start is non-zero, then elements from the iterable are skipped until start 
     * is reached. Afterward, elements are returned consecutively unless step is set 
     * higher than one which results in items being skipped.     *
     * @param  iterable, an <code>Iterable</code>
     * @param  start.
     * @param  stop.
     * @param  step.
     * @return an <code>Iterable</code>.
     * 
     * @see <code>slice</code>.
     */
    public static Iterable<Object> slice(final Iterable<?> iterable, final int start, final int stop, final int step) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    Iterator<Enum> iterator = enumerate(iterable).iterator();

                    Iterator<Integer> range = range(start, stop, step).iterator();

                    public boolean hasNext() {
                        return range.hasNext() && iterator.hasNext();
                    }

                    public Object next() {
                        int i = range.next();
                        do {
                            Enum tuple = iterator.next();
                            if (i == tuple.index) return tuple.value;
                        } while (iterator.hasNext());
                        throw new NoSuchElementException();
                    }

                    public void remove() {
                    }
                };
            }
        });
    }

    /**
         * return an Iterable whose iterator will with every call to next apply lambda  whose arguments will be an item from each given iterable.
         * Map will stops when the shortest iterable is exhausted. 
         * @param  lambda, an <code>Lamda</code>
         * @param  iterable, an <code>Iterable</code>
         * @return an <code>Iterable</code>.
         * <pre>
         * -----------------------------------------------------
         * Collection<Object> a = toCollection(new ArrayList<Object>(), 1, 2, 3, 4, 5, 6, 3, 3, 10);
         * Iterable<Integer> b = range(2, 10, 3);
         * System.out.println("a = " + str(a));
         * System.out.println("b = " + b);
         * 
         * System.out.println("testing map(lambda x: x*10, b): "
         * + map(new Lambda() {
         *  	public Object process(Object... args) {
         *  		return ((Integer) args[0]) * 10;
         *  	}
         *  }, b));
         *  
         *  System.out.println("testing map(lambda x,y: x*10+y, a, b): "
         *  + map(new Lambda() {
         *  	public Object process(Object... args) {
         *			int x = (Integer) args[0];
         *			int y = (Integer) args[1];
         *			return x+y;
         *  	}
         *  }, a, b));
         * .....................................................
         * a = 1,2,3,4,5,6,3,3,10
         * b = (2,5,8)
         * testing map(lambda x: x*10, b): (20,50,80)
         * testing map(lambda x,y: x*10+y, a, b): (12,25,38)
         * -----------------------------------------------------
         * <pre>
         */
    public static Iterable<Object> map(final Lambda lambda, final Iterable<?>... iterables) {
        final Iterator<?>[] iterators = new Iterator[iterables.length];
        for (int i = 0; i < iterables.length; i++) iterators[i] = iterables[i] != null ? iterables[i].iterator() : Null;
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    public boolean hasNext() {
                        for (Iterator<?> iterator : iterators) {
                            if (iterator.hasNext() == false) return false;
                        }
                        return true;
                    }

                    public Object next() {
                        return lambda.process(map(iterators, new Lambda() {

                            public Object process(Object... args) {
                                return ((Iterator<?>) args[0]).next();
                            }
                        }));
                    }

                    public void remove() {
                    }
                };
            }
        });
    }

    /**
     * This function returns a Iterable  of Iterable, where the i-th Iterable 
     * contains the i-th element from each of the argument iterables.
     * The returned Iterable is truncated in length to the length of the shortest 
     * argument Iterable.
     * 
     * @param  iterables, an number of <code>Iterable</code>s
     * @return an <code>Iterable</code>.
     * 
     * @see <code>map</code>.
     */
    public static Iterable<Object> zip(final Iterable<?>... iterables) {
        final Iterator<?>[] iterators = new Iterator[iterables.length];
        for (int i = 0; i < iterables.length; i++) iterators[i] = iterables[i] != null ? iterables[i].iterator() : Null;
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    public boolean hasNext() {
                        for (Iterator<?> iterator : iterators) {
                            if (iterator.hasNext() == false) return false;
                        }
                        return true;
                    }

                    public Object next() {
                        return iterable(map(iterators, new Lambda() {

                            public Object process(Object... args) {
                                return ((Iterator<?>) args[0]).next();
                            }
                        }));
                    }

                    public void remove() {
                    }
                };
            }
        });
    }

    /**
     * This function returns a Iterable  from those elements of the given iterable for which 
     * predicate returns not null. 
     * 
     * @param  predicate, a Lambda
     * @param  iterable
     * @return an <code>Iterable</code>.
     * 
     * @see  #map(com.syntazo.coffeegrains.Iteration.Lambda, Iterable...)
     */
    public static Iterable<Object> filter(final Lambda predicate, final Iterable<?> iterable) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    Iterator<?> iterator = iterable != null ? iterable.iterator() : Null;

                    Object nextObj = null;

                    public boolean hasNext() {
                        nextObj = getNext(nextObj);
                        return nextObj != null;
                    }

                    public Object getNext(Object obj) {
                        while (obj == null && iterator.hasNext()) {
                            obj = predicate.process(iterator.next());
                        }
                        return obj;
                    }

                    public void remove() {
                        iterator.remove();
                    }

                    public Object next() {
                        Object obj = getNext(nextObj);
                        nextObj = null;
                        return obj;
                    }
                };
            }
        });
    }

    /**
     * This function makes a Iterable  whose iterator whose iterator  
     * returns elements from the given iterable as 
     * long as the predicate is true. 
     * 
     * @param  predicate, a Lambda
     * @param  iterable
     * @return an <code>Iterable</code>.
     * 
     * @see #filter(com.syntazo.coffeegrains.Iteration.Lambda, Iterable)
     * @see #dropwhile(com.syntazo.coffeegrains.Iteration.Lambda, Iterable)
     */
    public static Iterable<Object> takewhile(final Lambda lambda, final Iterable<?> iterable) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    Iterator<?> iterator = iterable != null ? iterable.iterator() : Null;

                    Object nextObj = null;

                    public boolean hasNext() {
                        nextObj = getNext(nextObj);
                        return nextObj != null;
                    }

                    public Object getNext(Object obj) {
                        if (obj == null && iterator.hasNext()) {
                            obj = lambda.process(iterator.next());
                        }
                        return obj;
                    }

                    public void remove() {
                        iterator.remove();
                    }

                    public Object next() {
                        Object obj = getNext(nextObj);
                        nextObj = null;
                        return obj;
                    }
                };
            }
        });
    }

    /**
     * This function makes a Iterable  whose  iterator  drops elements from the 
     * given iterable as long as the predicate is true.
     * Afterwards it  returns every element. 
     * 
     * Note, the iterator does not produce any output until the predicate 
     * is true, so it may have a lengthy start-up time
     * 
     * @param  predicate, a Lambda
     * @param  iterable
     * @return an <code>Iterable</code>.
     * 
     * @see <code>filter, takewhile</code>.
     */
    public static Iterable<Object> dropwhile(final Lambda lambda, final Iterable<?> iterable) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    Iterator<?> iterator = iterable != null ? iterable.iterator() : Null;

                    boolean dropZone = true;

                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public void remove() {
                        iterator.remove();
                    }

                    public Object next() {
                        Object obj = iterator.next();
                        while (dropZone && iterator.hasNext()) {
                            dropZone = (Boolean) lambda.process(obj);
                            if (dropZone) obj = iterator.next();
                        }
                        dropZone = false;
                        return obj;
                    }
                };
            }
        });
    }

    /**
     * Makes an Iterable whoe iterator applies the lambda  function of two arguments 
     * cumulatively to the items of the goven iterable, 
     * from left to right, so as to reduce the iterable to a single value. 
     * 
     * The example the code below  calculates ((((1+2)+3)+4)+5). 
     *  <pre>      
     * -----------------------------------------------------
     * 
     * System.out.println("testing reduce(lambda x,y: x+y, a,0): "
     * + reduce(new Lambda() {
     * public Object process(Object... args) {
     *	int x = (Integer) args[0];
     *	int y = (Integer) args[1];
     *	return x+y;
     * }
     * }, iterable(1,2,3,4,5), 0));
     * 
     * .....................................................
     * testing reduce(lambda x,y: x+y, a,0): 15 
     * -----------------------------------------------------
     * </pre>
     * 
     * @param  predicate, a Lambda
     * @param  iterable
     * @param start, The initial start value.
     * @return an <code>Iterable</code>.
     * 
     * @see <code>filter, takewhile</code>.
     */
    public static Object reduce(Lambda lambda, final Iterable<Object> iterable, Object start) {
        Object result = start;
        for (Object obj : iterable) {
            result = lambda.process(result, obj);
        }
        return result;
    }

    /**
     * Make an iterator that returns elements from the first iterable until it
     * is exhausted, then proceeds to the next iterable, until all of the
     * iterables are exhausted. Used for treating consecutive sequences as a
     * single sequence.
     * 
     * @param iterables, a varialble number of iterables to chain through.
     * @return an <code>Iterable</code>.
     */
    public static Iterable<Object> chain(final Iterable<?>... iterables) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    int iterableIndex = 0;

                    Iterator<?> iterator = iterables[0] != null ? iterables[0].iterator() : Null;

                    public boolean hasNext() {
                        boolean more = iterator.hasNext();
                        while (more == false && iterableIndex < iterables.length - 1) {
                            iterator = iterables[iterableIndex++] == null ? Null : iterables[iterableIndex].iterator();
                            more = iterator.hasNext();
                        }
                        return more;
                    }

                    public Object next() {
                        hasNext();
                        return iterator.next();
                    }

                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        });
    }

    public static Iterable<Object> iterate(final int count, final Iterable<?> iterable) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    int number = 0;

                    Iterator<?> iterator = iterable != null ? iterable.iterator() : Null;

                    public boolean hasNext() {
                        return number < count && iterator.hasNext();
                    }

                    public Object next() {
                        number++;
                        return iterator.next();
                    }

                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        });
    }

    /**
     * Make an Iterable whose iterator  returns consecutive integers starting with start.
     * 
     * @param start.
     * @return an <code>Iterable</code>.
     */
    public static Iterable<Integer> count(final int start) {
        return buildIntegerIterable(new IteratorMaker<Integer>() {

            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    int iterableIndex = start;

                    public boolean hasNext() {
                        return true;
                    }

                    public Integer next() {
                        return iterableIndex++;
                    }

                    public void remove() {
                    }
                };
            }
        });
    }

    /**
     * Make an Iterable whose iterator  endlessly returning elements from the given iterable.
     * 
     * @param iterable.
     * @return an <code>Iterable</code>.
     */
    public static Iterable<Object> cycle(final Iterable<?> iterable) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    Iterator<?> iterator = iterable != null ? iterable.iterator() : Null;

                    public boolean hasNext() {
                        return true;
                    }

                    public Object next() {
                        if (iterator.hasNext() == false) iterator = iterable.iterator();
                        return iterator.next();
                    }

                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        });
    }

    /**
     * Make an Iterable whose iterator  endlessly returning elements from the given iterable.
     * 
     * @param iterable.
     * @return an <code>Iterable</code>.
     */
    public static Iterable<Object> repeat(final Object object) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    public boolean hasNext() {
                        return true;
                    }

                    public Object next() {
                        return object;
                    }

                    public void remove() {
                    }
                };
            }
        });
    }

    /**
     * Make an Iterable whose iterator returns an iterable of the given objects  repeating,  count, number of times.
     * 
     * @param count, the number of times to repeat.
     * @param objects, a variable number of Objects.
     * @return an <code>Iterable</code>.
     */
    public static Iterable<Object> repeat(int count, Object... objects) {
        return repeat(count, iterable(objects));
    }

    public static Iterable<Object> repeat(final int count, final Iterable<?> iterable) {
        return buildObjectIterable(new IteratorMaker<Object>() {

            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    Iterator<?> iterator = iterable != null ? iterable.iterator() : Null;

                    int cycleNo = 0;

                    public boolean hasNext() {
                        if (iterator.hasNext() == false) {
                            iterator = iterable.iterator();
                            cycleNo++;
                        }
                        return cycleNo < count;
                    }

                    public Object next() {
                        return iterator.next();
                    }

                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        });
    }

    /**
     * A test method that return true if the predicate evaluates true for all elements 
     * returned by the iterable, otherwise false.
     * 
     * @param predicate.
     * @param iterable.
     * @return boolean, true if the predicate evaluates true for all elements 
     * returned by the iterable, otherwise false.
     */
    public static boolean all(Lambda predicate, Iterable<?> iterable) {
        for (Object obj : iterable) {
            if (predicate.process(obj) == null) return false;
        }
        return true;
    }

    /**
     * A test method that return true if the  predicate evaluates true for any element 
     * returned by the iterable, otherwise false.
     * 
     * @param predicate.
     * @param iterable.
     * @return boolean, true if the predicate evaluates true for any element 
     * returned by the iterable, otherwise false.
     */
    public static boolean any(Lambda lambda, Iterable<?> iterable) {
        for (Object obj : iterable) {
            if (lambda.process(obj) == null) return true;
        }
        return false;
    }

    private static Object[] map(Object[] array, Lambda lambda) {
        return map(array, lambda, 0, array.length);
    }

    private static Object[] map(Object[] array, Lambda lambda, int start, int stop) {
        Object[] result = new Object[stop - start];
        for (int i = start; i < stop; i++) result[i] = lambda.process(array[i]);
        return result;
    }

    public static boolean any(Object[] array, Lambda lambda) throws Exception {
        for (Object obj : array) {
            if ((Boolean) lambda.process(obj) == false) return true;
        }
        return false;
    }

    public static String str(Iterable<?> parts) {
        return join(parts, ",");
    }

    public static String str(Object[] parts) {
        return join(parts, ",");
    }

    public static String join(Iterable<?> parts, String del) {
        StringBuffer result = new StringBuffer();
        int i = 0;
        for (Object part : parts) {
            result.append(part);
            result.append(del);
            i++;
        }
        return i > 0 ? result.substring(0, result.length() - del.length()) : result.toString();
    }

    public static String join(Object[] parts, String del) {
        StringBuffer result = new StringBuffer();
        int i = 0;
        for (Object part : parts) {
            result.append(part);
            result.append(del);
            i++;
        }
        return i > 0 ? result.substring(0, result.length() - del.length()) : result.toString();
    }

    public static Collection<Object> toCollection(Collection<Object> container, Object... elements) {
        for (Object obj : elements) container.add(obj);
        return container;
    }

    public static Collection<Object> toCollection(Collection<Object> container, Iterable<?> iterable) {
        for (Object obj : iterable) container.add(obj);
        return container;
    }

    public static void main(String[] args) {
        (new PyFile("c:\\Extract-7.txx", "w", 4000)).write((new PyFile("c:\\Extract-7.txt", "r", 4000)).read());
        Collection<Object> a = toCollection(new ArrayList<Object>(), 1, 2, 3, 4, 5, 6, 3, 3, 10);
        System.out.println("Collection<Object> a = toCollection(new ArrayList<Object>(), 1,2,3,4,5,6,3,3,10): " + str(a));
        Iterable<Integer> b = range(2, 10, 3);
        System.out.println("Iterable<Integer> b = irange(2,10,3): " + b);
        Iterable<Object> c = iterable(1, 2, 3, 4);
        System.out.println("Iterable c = iterable(1,2,3): " + c);
        Iterable<Object> d = iterable(new int[] { 1, 2, 3, 4 });
        System.out.println("Iterable d = iterable(new Integer []{1,2,3,4}): " + c);
        System.out.println("testing ienum(b): " + enumerate(c));
        Iterator forever = repeat(4).iterator();
        System.out.println("Iterable forever = repeat(4).iterator()");
        System.out.println("testing forever.next(), forever.next(): " + forever.next() + "," + forever.next());
        System.out.println("testing repeat(3, iterable(1,2,3)): " + repeat(3, iterable(1, 2, 3)));
        System.out.println("testing repeat(3, iterable(iterable(1,2,3))): " + repeat(3, iterable(iterable(1, 2, 3))));
        System.out.println("testing map(lambda x: x*10, b): " + map(new Lambda() {

            public Object process(Object... args) {
                return ((Integer) args[0]) * 10;
            }
        }, b));
        System.out.println("testing map(lambda x,y: x*10+y, a, b): " + map(new Lambda() {

            public Object process(Object... args) {
                return ((Integer) args[0]) * 10 + ((Integer) args[1]);
            }
        }, a, b));
        System.out.println("testing filter(lambda x: x%2==0, a): " + filter(new Lambda() {

            public Object process(Object... args) {
                return ((Integer) args[0]) % 2 == 0 ? args[0] : null;
            }
        }, a));
        System.out.println("testing reduce(lambda x,y: x+y, a,0): " + reduce(new Lambda() {

            public Object process(Object... args) {
                int x = (Integer) args[0];
                int y = (Integer) args[1];
                return x + y;
            }
        }, iterable(1, 2, 3, 4, 5), 0));
        System.out.println("testing slice(" + str(a) + "\n\t, 1,10,3): " + slice(a, 1, 10, 3));
        System.out.println("testing chain(a,b): " + chain(a, b));
        System.out.println("testing chain(a,b,a): " + chain(a, b, a));
        System.out.println("testing iterate(10, count(20)): " + iterate(10, count(20)));
        System.out.println("testing iterate(10, cycle(b)): " + iterate(10, cycle(b)));
        System.out.println("testing all(lambda x: x==2, " + b + ": " + all(new Lambda() {

            public Object process(Object... args) {
                return (Integer) args[0] == 2;
            }
        }, b));
        System.out.println("testing any(lambda x: x==2, " + b + ": " + any(new Lambda() {

            public Object process(Object... args) {
                return (Integer) args[0] == 2;
            }
        }, b));
        System.out.println("testing any(lambda x: x==2, repeat(10, iterable(1)): " + any(new Lambda() {

            public Object process(Object... args) {
                return (Integer) args[0] == 2;
            }
        }, repeat(10, iterable(1))));
        System.out.println("testing all(lambda x: x==1, repeat(10, iterable(1)): " + all(new Lambda() {

            public Object process(Object... args) {
                return (Integer) args[0] == 2;
            }
        }, repeat(10, iterable(1))));
        System.out.println("testing zip(a,b,c): " + zip(a, b, c));
        System.out.println("testing takewhile(lambda x: x< 4, a): " + takewhile(new Lambda() {

            public Object process(Object... args) {
                return (Integer) args[0] < 4 ? args[0] : null;
            }
        }, a));
        System.out.println("testing dropwhile(lambda x: x< 4, a): " + dropwhile(new Lambda() {

            public Object process(Object... args) {
                return (Integer) args[0] < 4;
            }
        }, a));
        System.out.println("example enumerate = zip(count().iterator(), a): " + zip(count(0), a));
        System.out.println("example dot product of (1,2,3,4) * (4,5,6,7): " + reduce(new Lambda() {

            public Object process(Object... args) {
                return (Integer) args[0] + (Integer) args[1];
            }
        }, map(new Lambda() {

            public Object process(Object... args) {
                return (Integer) args[0] * (Integer) args[1];
            }
        }, iterable(1, 2, 3, 4), iterable(4, 5, 6, 7)), 0));
    }
}
