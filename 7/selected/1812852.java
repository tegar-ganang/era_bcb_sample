package relex.frame.buffer;

import java.util.LinkedList;
import relex.frame.linkset.DefaultListenerManager;
import relex.frame.linkset.HandlerMethod;
import relex.frame.LogWriter;
import relex.frame.SchedulingItem;
import relex.frame.Sentence;

public class KnowledgeBaseBuffer {

    /**
	 *  max knowledge base buffer size
	 */
    private final int bufferSize = 5;

    public int getBufferSize() {
        return bufferSize;
    }

    private BufferElement[] elements;

    private int position;

    private static KnowledgeBaseBuffer kBuff = new KnowledgeBaseBuffer();

    public static KnowledgeBaseBuffer getInstance() {
        return (kBuff);
    }

    private KnowledgeBaseBuffer() {
        elements = new BufferElement[bufferSize];
        position = 0;
    }

    /**
	 * 
	 * @param kbIndex
	 * @return
	 */
    private BufferElement findElement(int kbIndex) {
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] != null) {
                if (elements[i].getKbaseIndex() == kbIndex) {
                    return elements[i];
                }
            }
        }
        return null;
    }

    /**
	 * 
	 * @param kbIndex
	 * @return
	 */
    private synchronized int findElementIndex(int kbIndex) {
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] != null) {
                if (elements[i].getKbaseIndex() == kbIndex) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isBufferFull() {
        return position == bufferSize;
    }

    private boolean getStatus(int kbIndex) {
        BufferElement elt = findElement(kbIndex);
        if (elt == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
	 * 
	 * @param kbIndex
	 */
    @HandlerMethod(id = "putAKnowledgeBase")
    public synchronized void put(int kbIndex) {
        if (!isBufferFull()) {
            elements[position] = new BufferElement(kbIndex);
            position++;
        }
    }

    /**
	 * 
	 * @param obsoleteIndex
	 * @param priorityQueue
	 */
    @HandlerMethod(id = "handleKBaseObsolete")
    public synchronized void handlekBaseObsolete(int obsoleteIndex, LinkedList<SchedulingItem> priorityQueue) {
        if (getStatus(obsoleteIndex)) {
            for (int i = findElementIndex(obsoleteIndex); i < position; i++) {
                if (elements.length > i + 1) {
                    elements[i] = elements[i + 1];
                }
            }
            position--;
        } else {
        }
        for (int i = 0; i < priorityQueue.size(); i++) {
            int value = priorityQueue.get(i).getIndex();
            BufferElement elt = findElement(value);
            if (elt == null) {
                put(value);
                break;
            }
        }
    }

    @HandlerMethod(id = "getKnowledgeBase")
    public synchronized void getKnowledgeBase(Sentence aSentence, int kbIndex) throws Exception {
        BufferElement elt = findElement(kbIndex);
        DefaultListenerManager setKbaseListener;
        if (elt != null) {
            setKbaseListener = new DefaultListenerManager();
            setKbaseListener.add(aSentence, "setKnowledgeBase");
            setKbaseListener.invokeAll(elt.getValue(), kbIndex, true);
        } else {
            setKbaseListener = new DefaultListenerManager();
            setKbaseListener.add(aSentence, "setKnowledgeBase");
            setKbaseListener.invokeAll(null, kbIndex, false);
        }
    }
}
