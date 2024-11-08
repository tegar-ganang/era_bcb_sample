package hojadetrabajo3;

/**
 * Array class that extends Cola for array generic implementation.
 * @author juan
 */
public class ArrayCola<E> extends Cola<E> {

    Object[] array;

    public ArrayCola(int n) {
        this.howmany = 0;
        array = (E[]) new Object[n];
    }

    /**
     *  adds last element.
     * @param coso
     */
    public void push(E coso) {
        array[howmany + 1] = coso;
        howmany++;
    }

    /**
     * Returns last element;
     */
    public E peek() {
        return (E) array[howmany];
    }

    /**
     * Returns how many elements does array have (not array size).
     */
    public int count() {
        return howmany;
    }

    /**
     * returns true if array is empty, if howmany has value 0. 
     */
    public boolean isEmpty() {
        return howmany == 0;
    }

    /**
     * 
     * @return Last element.
     */
    public E pop() {
        E coso;
        coso = peek();
        remove(howmany);
        return coso;
    }

    /**
     * 
     * @param cual
     * @return The indicated element at position cual.
     */
    public E getValue(int cual) {
        return (E) array[cual];
    }

    /**
     * Removes the indicated element at position cual.
     * @param cual
     */
    public void remove(int cual) {
        if (cual > howmany) {
            System.out.println("No es posible extraer un elemento en esa posición.");
        } else {
            array[cual] = null;
            for (int i = cual; i < array.length - 1; i++) {
                array[i] = array[i + 1];
            }
            array[howmany] = null;
            howmany--;
        }
    }

    /** 
    * Adds first Object element.
     */
    public void setFirst(E coso) {
        this.add(coso, 0);
    }

    public void add(E coso, int donde) {
        try {
            for (int i = donde; i < array.length - 1; i++) {
                array[i + 1] = array[i];
            }
            array[donde] = coso;
        } catch (Exception E) {
            System.out.println("Unadmisible value for array length or object.");
        }
    }

    @Override
    public boolean limit() {
        if (array.length == howmany) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Same as setFirst.
     * @param coso
     */
    public void pushFirst(E coso) {
        setFirst(coso);
    }
}
