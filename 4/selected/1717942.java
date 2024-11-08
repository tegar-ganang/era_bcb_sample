package yarfraw.io;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import yarfraw.core.datamodel.ChannelFeed;
import yarfraw.core.datamodel.FeedFormat;
import yarfraw.core.datamodel.ItemEntry;
import yarfraw.core.datamodel.YarfrawException;

/**
 * Provides a set of function to facilitate modifications to an RSS 2.0 feed.
 * <br/>
 * *Note* This class is not thread safe.
 * @author jliang
 *
 */
public class FeedAppender {

    private FeedWriter _writer;

    private FeedReader _reader;

    private int _numItemToKeep = -1;

    public FeedAppender(File file, FeedFormat format) {
        _writer = new FeedWriter(file);
        _reader = new FeedReader(file);
        setFormat(format);
    }

    public FeedAppender(String pathName, FeedFormat format) {
        this(new File(pathName), format);
    }

    public FeedAppender(URI uri, FeedFormat format) {
        this(new File(uri), format);
    }

    public FeedAppender(File file) {
        this(file, FeedFormat.RSS20);
    }

    public FeedAppender(String pathName) {
        this(new File(pathName), FeedFormat.RSS20);
    }

    public FeedAppender(URI uri) {
        this(new File(uri), FeedFormat.RSS20);
    }

    /**
   * The {@link FeedFormat} this writer should be using.<br/>
   * if this is not set, the default is RSS 2.0 format. <code>null</code> format is ignored  
   * @return a {@link FeedFormat} enum
   */
    public FeedFormat getFormat() {
        return _reader.getFormat();
    }

    /**
   * The {@link FeedFormat} this writer should be using.<br/>
   * if this is not set, the default is RSS 2.0 format. <code>null</code> format is ignored  
   * @param format a {@link FeedFormat} enum
   */
    public void setFormat(FeedFormat format) {
        if (format != null) {
            _reader.setFormat(format);
            _writer.setFormat(format);
        }
    }

    /**
   * Maximum number of items to keep in a feed. If the number of actual items
   * If <code>numItemToKeep</code> is non-negative and the actual number of {@link ItemEntry} (after appends)
   * in the feed greater than <code>numItemToKeep</code>, the appender will remove items from the END
   * of the feed to ensure there is at most <code>numItemToKeep</code> items in the feed.
   * @return the current <code>numberItemToKepp</code>, -1 if the value has not yet been set or there's no limit.
   */
    public int getNumItemToKeep() {
        return _numItemToKeep;
    }

    /**
   * Maximum number of items to keep in a feed. If the number of actual items
   * If <code>numItemToKeep</code> is non-negative and the actual number of {@link ItemEntry} (after appends)
   * in the feed greater than <code>numItemToKeep</code>, the appender will remove items from the END
   * of the feed to ensure there is at most <code>numItemToKeep</code> items in the feed.   
   * @param numItemToKeep number of items to keep in the current feed. no limit if it's negative.
   * @return this
   */
    public FeedAppender setNumItemToKeep(int numItemToKeep) {
        _numItemToKeep = numItemToKeep < 0 ? -1 : numItemToKeep;
        return this;
    }

    private List<ItemEntry> trimItemsList(List<ItemEntry> items) {
        if (_numItemToKeep != -1 && CollectionUtils.isNotEmpty(items) && items.size() > _numItemToKeep) {
            return items.subList(0, _numItemToKeep);
        }
        return items;
    }

    /**
   * Appends all of the {@link ItemEntry} in the specified collection into the current feed
   * at the specified position (optional operation).  Shifts the
   * {@link ItemEntry} currently at that position (if any) and any subsequent
   * {@link ItemEntry} to the right (increases their indices).
   * <br/>
   * As in  <code>feedItemList.addAll(index, inputlist)</code>
   * <br/>
   * If <code>numItemToKeep</code> is set and the resulting number of {@link ItemEntry} (after the append)
   * in the feed greater than <code>numItemToKeep</code>, the appender will remove items from the END
   * of the feed to ensure there is at most <code>numItemToKeep</code> items in the feed.
   * 
   * @param index index to append to
   * @param items any number of {@link yarfraw.core.datamodel.ItemEntry}
   * @return this
   * @throws YarfrawException if operation failed.
   */
    public FeedAppender appendAllItemsAt(int index, List<ItemEntry> items) throws YarfrawException {
        ChannelFeed ch = readChannel();
        List<ItemEntry> old = ch.getItems();
        if (old == null) {
            old = new ArrayList<ItemEntry>();
            ch.setItems(old);
        }
        old.addAll(index, items);
        ch.setItems(trimItemsList(old));
        _writer = new FeedWriter(_reader._file);
        _writer.writeChannel(ch);
        return this;
    }

    /**
   * Appends all of the {@link ItemEntry} to the <b>beginning</b> of the item list in  the current feed.
   * Shifts the {@link ItemEntry} currently at that position (if any) and any subsequent
   * {@link ItemEntry} to the right (increases their indices).
   * <br/>
   * This is equivalent to <code>appendAllItemsAt(0, inputlist)</code>
   * <br/>
   * If <code>numItemToKeep</code> is set and the resulting number of {@link ItemEntry} (after the append)
   * in the feed greater than <code>numItemToKeep</code>, the appender will remove items from the END
   * of the feed to ensure there is at most <code>numItemToKeep</code> items in the feed.
   *
   * @param items
   * @return
   * @throws YarfrawException if the appender failed to read or write the feed file.
   */
    public FeedAppender appendAllItemsToBeginning(List<ItemEntry> items) throws YarfrawException {
        return appendAllItemsAt(0, items);
    }

    /**
   * Appends all of the {@link ItemEntry} to the <b>beginning</b> of the item list in the current feed.
   * Shifts the {@link ItemEntry} currently at that position (if any) and any subsequent
   * {@link ItemEntry} to the right (increases their indices).
   * <br/>
   * This is equivalent to <code>appendAllItemsAt(0, Arrays.asList(item1, item2, item3))</code>
   * <br/>
   * If <code>numItemToKeep</code> is set and the resulting number of {@link ItemEntry} (after the append)
   * in the feed greater than <code>numItemToKeep</code>, the appender will remove items from the END
   * of the feed to ensure there is at most <code>numItemToKeep</code> items in the feed.
   *
   * @param items
   * @return
   * @throws YarfrawException if the appender failed to read or write the feed file.
   */
    public FeedAppender appendAllItemsToBeginning(ItemEntry... items) throws YarfrawException {
        return appendAllItemsToBeginning(Arrays.asList(items));
    }

    /**
   * Appends all of the {@link ItemEntry} to the <b>end</b> of the item list in current feed.
   * Shifts the {@link ItemEntry} currently at that position (if any) and any subsequent
   * {@link ItemEntry} to the right (increases their indices).
   * <br/>
   * If <code>numItemToKeep</code> is set and the resulting number of {@link ItemEntry} (after the append)
   * in the feed greater than <code>numItemToKeep</code>, the appender will remove items from the END
   * of the feed to ensure there is at most <code>numItemToKeep</code> items in the feed.
   *
   * @param items
   * @return
   * @throws YarfrawException if the appender failed to read or write the feed file.
   */
    public FeedAppender appendAllItemsToEnd(List<ItemEntry> items) throws YarfrawException {
        ChannelFeed ch = readChannel();
        List<ItemEntry> old = ch.getItems();
        if (old == null) {
            old = new ArrayList<ItemEntry>();
            ch.setItems(old);
        }
        old.addAll(items);
        ch.setItems(trimItemsList(old));
        _writer = new FeedWriter(_reader._file);
        _writer.writeChannel(ch);
        return this;
    }

    /**
   * Appends all of the {@link ItemEntry} to the <b>end</b> of the item list in the current feed.
   * Shifts the {@link ItemEntry} currently at that position (if any) and any subsequent
   * {@link ItemEntry} to the right (increases their indices).
   * <br/>
   * If <code>numItemToKeep</code> is set and the resulting number of {@link ItemEntry} (after the append)
   * in the feed greater than <code>numItemToKeep</code>, the appender will remove items from the END
   * of the feed to ensure there is at most <code>numItemToKeep</code> items in the feed.
   *
   * @param items
   * @return
   * @throws YarfrawException if the appender failed to read or write the feed file.
   */
    public FeedAppender appendAllItemsToEnd(ItemEntry... items) throws YarfrawException {
        return appendAllItemsToEnd(Arrays.asList(items));
    }

    /**
   * * Remove the item at index <code>index</code> from the feed.
   * @param index
   * @return
   * @throws YarfrawException if the appender failed to read or write the feed file.
   */
    public FeedAppender removeItem(int index) throws YarfrawException {
        ChannelFeed ch = readChannel();
        ch.getItems().remove(index);
        ch.setItems(trimItemsList(ch.getItems()));
        _writer.writeChannel(ch);
        return this;
    }

    /**
   * Set the item at index <code>index</code> to be the input <code>item</code>
   * @param index
   * @param item
   * @return
   * @throws YarfrawException if the appender failed to read or write the feed file.
   */
    public FeedAppender setItem(int index, ItemEntry item) throws YarfrawException {
        ChannelFeed ch = readChannel();
        ch.getItems().set(index, item);
        _writer.writeChannel(ch);
        return this;
    }

    private ChannelFeed readChannel() throws YarfrawException {
        ChannelFeed ch = _reader.readChannel();
        if (ch.getItems() == null) {
            ch.setItems(new ArrayList<ItemEntry>());
        }
        return ch;
    }
}
