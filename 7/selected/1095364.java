package bplustree;

import java.util.ArrayList;

/**
 * Esta clase representa el arreglo para los elementos de los nodos intermedios del arbol B+.
 * 
 */
public class InternalNodeArrayMap {

    protected String[] keys;

    protected IBPlusTreeNode[] nodes;

    protected int currentSize = 0;

    public static int binarySearch(String[] strings, String key, int from, int to) {
        int low = from;
        int high = to;
        for (; low <= high; ) {
            int mid = (low + high) >> 1;
            String midVal = strings[mid];
            if (midVal.compareTo(key) < 0) low = mid + 1; else if (midVal.compareTo(key) > 0) high = mid - 1; else return mid;
        }
        return -(low + 1);
    }

    public InternalNodeArrayMap(int n) {
        keys = new String[n + 1];
        nodes = new IBPlusTreeNode[n + 2];
        nodes[0] = InternalNode.NULL;
    }

    public String getMidKey() {
        return keys[currentSize / 2];
    }

    /**
	 * Realiza un split del array. Mantiene los elementos desde el 0 hasta (mid-1) y devuelve
	 * un nuevo array con los elementos desde (mid+1) hasta el �ltimo.
	 * La clave en la posici�n "mid", no queda en ninguno de los dos arrays, y tiene que ser
	 * promovida.
	 * 
	 * @return
	 */
    public InternalNodeArrayMap split() {
        InternalNodeArrayMap newMap = new InternalNodeArrayMap(keys.length);
        final int mid = currentSize / 2;
        int count = 0;
        newMap.nodes[0] = nodes[mid + 1];
        for (int i = mid + 1; i < currentSize; i++) {
            newMap.keys[count] = keys[i];
            newMap.nodes[++count] = nodes[i + 1];
        }
        for (int i = mid; i < currentSize; i++) {
            nodes[i + 1] = null;
        }
        newMap.currentSize = currentSize - mid - 1;
        currentSize = mid;
        return newMap;
    }

    /**
	 * Coloca la clave asociada al nodo derecho en el array
	 * 
	 * @param key
	 * @param rightNode
	 * @return
	 */
    public void put(String key, IBPlusTreeNode rightNode) {
        if (currentSize == 0) {
            keys[0] = key;
            nodes[1] = rightNode;
            currentSize++;
            return;
        }
        int pos = binarySearch(keys, key, 0, currentSize - 1);
        if (pos >= 0) {
            keys[pos] = key;
            nodes[pos + 1] = rightNode;
        } else {
            pos = -(pos + 1);
            if (pos < currentSize) {
                System.arraycopy(keys, pos, keys, pos + 1, currentSize - pos);
                System.arraycopy(nodes, pos + 1, nodes, pos + 2, currentSize - pos);
                keys[pos] = key;
                nodes[pos + 1] = rightNode;
                currentSize++;
            } else {
                keys[currentSize] = key;
                nodes[currentSize + 1] = rightNode;
                currentSize++;
            }
        }
    }

    /**
	 * Coloca la clave asociada al nodo izquierdo en el array
	 * 
	 * @param key
	 * @param leftNode
	 */
    public void putLeft(String key, IBPlusTreeNode leftNode) {
        if (currentSize == 0) {
            keys[0] = key;
            nodes[0] = leftNode;
            currentSize++;
            return;
        }
        int pos = binarySearch(keys, key, 0, currentSize - 1);
        if (pos >= 0) {
            keys[pos] = key;
            nodes[pos] = leftNode;
        } else {
            pos = -(pos + 1);
            if (pos < currentSize) {
                System.arraycopy(keys, pos, keys, pos + 1, currentSize - pos);
                System.arraycopy(nodes, pos, nodes, pos + 1, currentSize - pos);
                keys[pos] = key;
                nodes[pos] = leftNode;
                currentSize++;
            } else {
                keys[currentSize] = key;
                nodes[currentSize] = leftNode;
                currentSize++;
            }
        }
    }

    /**
	 * Devuelve el nodo que contiene un intervalo de claves en el cual entra
	 * la clave especificada.
	 * 
	 * @param key
	 * @return
	 */
    public IBPlusTreeNode get(String key) {
        int pos = getIntervalPosition(key);
        if (pos == -1) return null; else return nodes[pos];
    }

    /**
	 * Devuelve la posicion en el array de nodos, que representa el intervalo en el cual
	 * entra la clave especificada.
	 * 
	 * @param key
	 * @return
	 */
    public int getIntervalPosition(String key) {
        if (currentSize == 0) {
            return -1;
        } else {
            int pos = binarySearch(keys, key, 0, currentSize - 1);
            if (pos < 0) {
                pos = -(pos + 1);
            } else {
                pos++;
            }
            return pos;
        }
    }

    /**
	 * Este m�todo elimina la clava indicada. No toca el nodo mas a la izquierda en el array 
	 * (ya que tienen claves mas peque�as que la clave mas a la izquierda).
	 * 
	 * @param key
	 * @return true si se pudo borrar, o false en caso de no encontrar la clave.
	 */
    public boolean delete(String key) {
        if (currentSize == 0) {
            return false;
        }
        int pos = binarySearch(keys, key, 0, currentSize - 1);
        if (pos >= 0) {
            deleteAtPos(pos);
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Elimina el par clave-nodo de la posicion especificada
	 * 
	 * @param pos
	 */
    public void deleteAtPos(int pos) {
        System.arraycopy(keys, pos + 1, keys, pos, currentSize - pos);
        System.arraycopy(nodes, pos + 2, nodes, pos + 1, currentSize - pos);
        nodes[currentSize] = null;
        currentSize--;
    }

    /**
	 * Reemplaza una clave por otra
	 * 
	 * @param keyToReplace La clave que hay que reemplazar
	 * @param j  El nuevo valor que tomara la clave
	 */
    public void replaceKey(String keyToReplace, String j) {
        int i = 0;
        int pos = 0;
        boolean encontrado = false;
        while ((i < this.keys.length) && !encontrado) {
            encontrado = this.keys[i] == keyToReplace;
            if (encontrado) pos = i;
            i++;
        }
        if (encontrado) this.keys[pos] = j;
    }

    /**
	 * Elimina la clave innecesaria y reordena los punteros a nodos luego de haberse producido 
	 * un merge en el nivel inferior
	 * 
	 * @param key clave a volar del array
	 * @param rightPointer true si se debe actualizar el puntero a derecha, false si es a izquierda
	 */
    public void fixKeyNodePointers(String key, boolean rightPointer) {
        int i = 0;
        int pos = 0;
        boolean encontrado = false;
        while ((i < this.keys.length) && !encontrado) {
            encontrado = this.keys[i] == key;
            if (encontrado) pos = i;
            i++;
        }
        if (encontrado) {
            clearKeyNodeAtPos(pos, rightPointer);
        }
    }

    /**
	 * Elimina el par clave-nodo de la posicion especificada
	 * 
	 * @param pos posicion de la clave a eliminar
	 * @param rightPointer true si hay que eliminar el puntero a derecha, o false si es el izquierdo
	 */
    private void clearKeyNodeAtPos(int pos, boolean rightPointer) {
        System.arraycopy(keys, pos + 1, keys, pos, currentSize - pos);
        if (rightPointer) {
            System.arraycopy(nodes, pos + 2, nodes, pos + 1, currentSize - pos);
        } else {
            System.arraycopy(nodes, pos + 1, nodes, pos, currentSize - pos);
        }
        nodes[currentSize] = null;
        currentSize--;
    }

    /**
	 * Devuelve un ArrayList conteniendo la primer clave del nodo en el primer lugar, y puntero al nodo hijo izquierdo de la clave
	 * en la segunda posicion.
	 * Elimina la clave y el puntero izquierdo del nodo
	 * 
	 * @return
	 */
    public ArrayList<Object> getFirstKeyNode() {
        String key;
        key = this.keys[0];
        System.arraycopy(this.keys, 1, this.keys, 0, this.currentSize);
        IBPlusTreeNode leftNode = this.nodes[0];
        System.arraycopy(this.nodes, 1, this.nodes, 0, this.currentSize);
        this.currentSize--;
        ArrayList<Object> list = new ArrayList<Object>();
        list.add(new Integer(key));
        list.add(leftNode);
        return list;
    }

    /**
	 * Devuelve un ArrayList conteniendo la ultima clave del nodo en el primer lugar, y un puntero al nodo hijo derecho de la 
	 * clave en la segunda posicion.
	 * Elimina la clave y el puntero derecho del nodo
	 * 
	 * @return
	 */
    public ArrayList<Object> getLastKeyNode() {
        String key;
        key = this.keys[this.currentSize - 1];
        IBPlusTreeNode rightNode = this.nodes[this.currentSize];
        this.keys[this.currentSize - 1] = "";
        this.nodes[this.currentSize] = null;
        this.currentSize--;
        ArrayList<Object> list = new ArrayList<Object>();
        list.add(new Integer(key));
        list.add(rightNode);
        return list;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (nodes[0] == InternalNode.NULL) {
            sb.append("NULL | ");
        } else {
            String nodeValue = nodes[0] == null ? null : Integer.toString(nodes[0].hashCode());
            sb.append(nodeValue + " | ");
        }
        for (int i = 0; i < currentSize; i++) {
            sb.append(keys[i] + " | ");
            String nodeValue = nodes[i + 1] == null ? null : Integer.toString(nodes[i + 1].hashCode());
            sb.append(nodeValue);
            if (i + 1 < currentSize) sb.append(" | ");
        }
        return sb.toString();
    }

    public int getSize() {
        return currentSize;
    }

    public String[] getKeys() {
        return keys;
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    public IBPlusTreeNode[] getNodes() {
        return nodes;
    }

    public void setNodes(IBPlusTreeNode[] nodes) {
        this.nodes = nodes;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(int currentSize) {
        this.currentSize = currentSize;
    }
}
