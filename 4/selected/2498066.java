package de.nava.informa.utils.manager.hibernate;

import de.nava.informa.core.ChannelGroupIF;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.hibernate.Channel;
import de.nava.informa.impl.hibernate.ChannelGroup;
import de.nava.informa.impl.hibernate.Item;
import de.nava.informa.utils.InformaUtils;
import de.nava.informa.utils.manager.PersistenceManagerException;
import de.nava.informa.utils.manager.PersistenceManagerIF;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of persistence manager interface, talking with Hibernate. This implementation
 * is not 100% usable becase it isn't confirming to the rule of using the same instances. This
 * means that each time it looks for object (for example, using method <code>getGroups()</code>)
 * it returns new instances of group objects (<code>group1 != group2</code>, but
 * <code>group1.getId() == group2.getId()</code>). Persistence Manager implementation should
 * operate with the same instances all the way and it's carefully checked by acceptance test.
 * <p>
 * There's another implementation wrapping this one -- <code>PersistenceManager</code>. It
 * conforms to the rule.</p>
 *
 * @author Aleksey Gureev (spyromus@noizeramp.com)
 *
 * @see PersistenceManager
 */
class NonCachingPersistenceManager implements PersistenceManagerIF {

    private static final Logger LOG = Logger.getLogger(NonCachingPersistenceManager.class.getName());

    /**
   * Creates new group of channels in persistent storage.
   *
   * @param title title of the group.
   * @return initialized and persisted group object.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public ChannelGroupIF createGroup(String title) throws PersistenceManagerException {
        final ChannelGroupIF group = new ChannelGroup(title);
        HibernateUtil.saveObject(group);
        return group;
    }

    /**
   * Updates data in storage with data from the group object.
   *
   * @param group group object
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public void updateGroup(ChannelGroupIF group) throws PersistenceManagerException {
        HibernateUtil.updateObject(group);
    }

    /**
   * Deletes group from persistent storage.
   *
   * @param group group to delete.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public void deleteGroup(ChannelGroupIF group) throws PersistenceManagerException {
        deleteGroup(group, null);
        group.setId(-1);
    }

    /**
   * Takes channels from the <code>second</code> group and put them all in <code>first</code>
   * group. Then <code>second</code> group is deleted.
   *
   * @param first  first group of channels.
   * @param second second group of channels.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public void mergeGroups(ChannelGroupIF first, ChannelGroupIF second) throws PersistenceManagerException {
        Transaction tx = null;
        try {
            final Session session = HibernateUtil.openSession();
            tx = session.beginTransaction();
            HibernateUtil.lock(first, session);
            HibernateUtil.lock(second, session);
            mergeGroups(first, second, session);
            tx.commit();
            second.setId(-1);
        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                }
            }
            LOG.log(Level.SEVERE, "Could not merge groups.", e);
            throw new PersistenceManagerException("Could not merge groups.", e);
        } finally {
            HibernateUtil.closeSession();
        }
    }

    /**
   * Returns the list of groups available in database.
   *
   * @return list of groups.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public ChannelGroupIF[] getGroups() throws PersistenceManagerException {
        ChannelGroupIF[] groups = null;
        try {
            final Session session = HibernateUtil.openSession();
            groups = getGroups(session);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not read the list of groups.", e);
            throw new PersistenceManagerException("Could not read the list of groups.", e);
        } finally {
            HibernateUtil.closeSession();
        }
        return groups == null ? new ChannelGroupIF[0] : groups;
    }

    /**
   * Creates new channel object and persists it into storage.
   *
   * @param title    title of the channel.
   * @param location location of channel data resource.
   *
   * @return newly created object.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public ChannelIF createChannel(String title, URL location) throws PersistenceManagerException {
        final ChannelIF channel = new Channel(title);
        channel.setLocation(location);
        HibernateUtil.saveObject(channel);
        return channel;
    }

    /**
   * Updates data in database with data from channel object.
   *
   * @param channel channel object.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public void updateChannel(ChannelIF channel) throws PersistenceManagerException {
        HibernateUtil.updateObject(channel);
    }

    /**
   * Adds <code>channel</code> to the <code>group</code>.
   *
   * @param channel channel to add.
   * @param group   group to use.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public void addChannelToGroup(ChannelIF channel, ChannelGroupIF group) throws PersistenceManagerException {
        channel.hashCode();
        Transaction tx = null;
        try {
            final Session session = HibernateUtil.openSession();
            tx = session.beginTransaction();
            HibernateUtil.lock(channel, session);
            group.add(channel);
            HibernateUtil.updateObject(group, session);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                }
            }
            LOG.log(Level.SEVERE, "Could add channel to group.", e);
            throw new PersistenceManagerException("Could add channel to group.", e);
        } finally {
            HibernateUtil.closeSession();
        }
    }

    /**
   * Deletes <code>channel</code> from the <code>group</code>.
   * This method doesn't delete channel from persistent storage. It only
   * breaks the association between channel and group.
   *
   * @param channel channel to delete.
   * @param group   group to use.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public void removeChannelFromGroup(ChannelIF channel, ChannelGroupIF group) throws PersistenceManagerException {
        Transaction tx = null;
        try {
            final Session session = HibernateUtil.openSession();
            tx = session.beginTransaction();
            HibernateUtil.lock(channel, session);
            group.remove(channel);
            HibernateUtil.updateObject(group, session);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                }
            }
            LOG.log(Level.SEVERE, "Could add channel to group.", e);
            throw new PersistenceManagerException("Could add channel to group.", e);
        } finally {
            HibernateUtil.closeSession();
        }
    }

    /**
   * Deletes channel from persistent storage.
   *
   * @param channel channel to delete.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public void deleteChannel(ChannelIF channel) throws PersistenceManagerException {
        final ChannelGroupIF[] groups = getGroups();
        ItemIF[] items = null;
        Transaction tx = null;
        try {
            final Session session = HibernateUtil.openSession();
            tx = session.beginTransaction();
            HibernateUtil.lock(channel, session);
            items = deleteChannel(channel, groups, session);
            tx.commit();
            channel.setId(-1);
            for (int i = 0; i < items.length; i++) {
                items[i].setId(-1);
            }
        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                }
            }
            LOG.log(Level.SEVERE, "Could not delete channel.", e);
            throw new PersistenceManagerException("Could not delete channel.", e);
        } finally {
            HibernateUtil.closeSession();
        }
    }

    /**
   * Creates new item in the channel.
   *
   * @param channel channel to put new item into.
   * @param title   title of new item.
   *
   * @return new item object.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public ItemIF createItem(ChannelIF channel, String title) throws PersistenceManagerException {
        final ItemIF item = new Item(channel, title, null, null);
        saveCreatedItem(channel, item);
        return item;
    }

    /**
   * Creates new item using specified object as ethalon.
   * <b>Note that application <i>could</i> already add object to the channel and
   * only persistent modifications required.</b>
   *
   * @param channel channel to put new item into.
   * @param ethalon object to copy properties values from.
   *
   * @return new item object.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public ItemIF createItem(ChannelIF channel, ItemIF ethalon) throws PersistenceManagerException {
        final ItemIF item = new Item(channel, null, null, null);
        InformaUtils.copyItemProperties(ethalon, item);
        saveCreatedItem(channel, item);
        return item;
    }

    /**
   * Updates data in database with data from item object.
   *
   * @param item item object.
   *
   * @throws PersistenceManagerException in case of any errors.
   */
    public void updateItem(ItemIF item) throws PersistenceManagerException {
        HibernateUtil.updateObject(item);
    }

    /**
   * Deletes the item from the persistent storage.
   *
   * @param item item to delete.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    public void deleteItem(ItemIF item) throws PersistenceManagerException {
        Transaction tx = null;
        try {
            final Session session = HibernateUtil.openSession();
            tx = session.beginTransaction();
            deleteItem(item, session);
            tx.commit();
            item.setId(-1);
        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                }
            }
            LOG.log(Level.SEVERE, "Could not delete item.", e);
            throw new PersistenceManagerException("Could not delete item.", e);
        } finally {
            HibernateUtil.closeSession();
        }
    }

    /**
   * Merges two groups by moving channels from second to first.
   *
   * @param first   first group.
   * @param second  second group.
   * @param session session to use or NULL.
   *
   * @throws PersistenceManagerException in case of any problems with Hibernate.
   */
    private static void mergeGroups(ChannelGroupIF first, ChannelGroupIF second, final Session session) throws PersistenceManagerException {
        first.getAll().addAll(second.getAll());
        HibernateUtil.updateObject(first, session);
        deleteGroup(second, session);
    }

    /**
   * Removes all associations with channels and deletes group object.
   *
   * @param group   object to delete.
   * @param session session to use or NULL.
   *
   * @throws PersistenceManagerException in case of any problems with Hibernate.
   */
    private static void deleteGroup(ChannelGroupIF group, Session session) throws PersistenceManagerException {
        if (session != null) {
            HibernateUtil.lock(group, session);
        }
        group.getAll().clear();
        HibernateUtil.deleteObject(group, session);
    }

    /**
   * Returns list of groups available in database using given session.
   *
   * @param session session to use.
   *
   * @return list of groups.
   *
   * @throws PersistenceManagerException in case of any problems with Hibernate.
   */
    private static ChannelGroupIF[] getGroups(final Session session) throws PersistenceManagerException {
        ChannelGroupIF[] groups;
        try {
            final List<?> groupsList = session.createQuery("from ChannelGroup").list();
            groups = groupsList.toArray(new ChannelGroupIF[0]);
            for (int i = 0; i < groups.length; i++) {
                ChannelGroupIF group = groups[i];
                initGroupCollections(group);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not read the list of groups.", e);
            throw new PersistenceManagerException("Could not read the list of groups.", e);
        }
        return groups;
    }

    /**
   * Lads collections of group.
   *
   * @param group group collections.
   */
    private static void initGroupCollections(ChannelGroupIF group) {
        group.getChildren().size();
        ChannelIF[] channels = (ChannelIF[]) group.getAll().toArray(new ChannelIF[0]);
        for (int i = 0; i < channels.length; i++) {
            ChannelIF channel = channels[i];
            channel.getCategories().size();
            ((Channel) channel).getGroups().size();
            for (Iterator it = channel.getItems().iterator(); it.hasNext(); ) {
                ((ItemIF) it.next()).getCategories().size();
            }
        }
    }

    /**
   * Deletes channel and all its items. Also removes associations with groups.
   *
   * @param channel channel to delete.
   * @param groups  list of all present group. We can't get this list here because we require
   *                to get it from separate session.
   * @param session session to use or NULL.
   *
   * @return list of deleted items.
   *
   * @throws PersistenceManagerException in case of any problems with Hibernate.
   */
    private ItemIF[] deleteChannel(ChannelIF channel, ChannelGroupIF[] groups, final Session session) throws PersistenceManagerException {
        for (int i = 0; i < groups.length; i++) {
            ChannelGroupIF group = groups[i];
            if (group.getAll().contains(channel)) {
                group.remove(channel);
                HibernateUtil.updateObject(group, session);
            }
        }
        final ItemIF[] items = (ItemIF[]) channel.getItems().toArray(new ItemIF[0]);
        for (int i = 0; i < items.length; i++) {
            ItemIF item = items[i];
            channel.removeItem(item);
            HibernateUtil.deleteObject(item, session);
        }
        HibernateUtil.deleteObject(channel, session);
        return items;
    }

    /**
   * Saves created item to storage and associates it with channel using give session.
   *
   * @param item    item to save.
   * @param channel channel to put item in.
   * @param session session to use or NULL.
   *
   * @throws PersistenceManagerException in case of any problems with Hibernate.
   */
    private static void createItem(final ItemIF item, ChannelIF channel, Session session) throws PersistenceManagerException {
        channel.addItem(item);
    }

    /**
   * Deletes item from persistent storage using sinle session object.
   *
   * @param item    item to delete.
   * @param session session to use or NULL.
   *
   * @throws PersistenceManagerException in case of any problems with Hibernate.
   */
    private static void deleteItem(ItemIF item, Session session) throws PersistenceManagerException {
        final ChannelIF channel = item.getChannel();
        if (channel != null) {
            HibernateUtil.lock(channel, session);
            channel.removeItem(item);
        } else {
            LOG.severe("Item didn't belong to any channel: " + item);
        }
        HibernateUtil.deleteObject(item, session);
    }

    /**
   * Saves created item.
   *
   * @param channel channel to assign to.
   * @param item    item to save.
   *
   * @throws PersistenceManagerException in case of any problems.
   */
    private void saveCreatedItem(ChannelIF channel, final ItemIF item) throws PersistenceManagerException {
        Transaction tx = null;
        try {
            final Session session = HibernateUtil.openSession();
            tx = session.beginTransaction();
            HibernateUtil.lock(channel, session);
            createItem(item, channel, session);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                }
            }
            LOG.log(Level.SEVERE, "Could not create item.", e);
            throw new PersistenceManagerException("Could not create item.", e);
        } finally {
            HibernateUtil.closeSession();
        }
    }
}
