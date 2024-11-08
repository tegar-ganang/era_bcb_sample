package de.nava.informa.utils.cleaner;

import de.nava.informa.utils.toolkit.WorkerThread;
import de.nava.informa.utils.toolkit.ChannelRecord;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;

/**
 * Worker thread performing cleaning operations over channels.
 * <p>
 * Processing of channel consists of checking every item with registered matcher.
 * If checker says that the item matches the rule then there will be event thrown
 * to the observer.</p>
 *
 * @author Aleksey Gureev (spyromus@noizeramp.com)
 *
 * @see WorkerThread
 */
public class CleanerWorkerThread extends WorkerThread {

    private static int seq = 1;

    private CleanerObserverIF observer;

    private CleanerMatcherIF matcher;

    /**
   * Creates new worker thread object.
   *
   * @param observer  observer of the cleaner.
   * @param matcher   matcher of the cleaner.
   */
    public CleanerWorkerThread(CleanerObserverIF observer, CleanerMatcherIF matcher) {
        super("Cleaner " + (seq++));
        this.observer = observer;
        this.matcher = matcher;
    }

    /**
   * Processes record.
   *
   * @param record record to process.
   */
    protected final void processRecord(ChannelRecord record) {
        if (matcher != null && observer != null) {
            final ChannelIF channel = record.getChannel();
            observer.cleaningStarted(channel);
            final ItemIF[] items = (ItemIF[]) channel.getItems().toArray(new ItemIF[0]);
            for (int i = 0; i < items.length; i++) {
                checkItem(items[i], channel);
            }
            observer.cleaningFinished(channel);
        }
    }

    /**
   * Checks single item in matcher and notifies observer if it's unwanted.
   *
   * @param item    item to check.
   * @param channel channel where the item resides.
   */
    private void checkItem(ItemIF item, ChannelIF channel) {
        if (matcher.isMatching(item, channel)) {
            try {
                observer.unwantedItem(item, channel);
            } catch (Exception e) {
            }
        }
    }
}
