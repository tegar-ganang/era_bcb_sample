package com.prossys.dao;

import com.prossys.model.Company;
import com.prossys.model.Price;
import com.prossys.model.PricePk;
import com.prossys.model.Product;
import com.prossys.model.Store;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.apache.log4j.Logger;

/**
 * 
 * @author Victor
 */
public class StoreDAO extends BaseDAO {

    Logger logger = Logger.getLogger(this.getClass().getName());

    public Store getStoreByUsernameAndPassword(String username, String password) throws PersistenceException {
        logger.info("Getting store by username and password...");
        if (username == null) {
            String error = "Username is empty.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        if (password == null) {
            String error = "Password is empty.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        Store store = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select s from Store s where " + "s.username = :username and s.password = :password");
            query.setParameter("username", username);
            query.setParameter("password", password);
            store = (Store) query.getSingleResult();
        } catch (NoResultException ex) {
            String warning = "Store with username and password was not found.";
            logger.warn(warning);
            return null;
        } catch (Exception ex) {
            String error = "Error getting store by username and password: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Store with username and password was found.");
        return store;
    }

    public void insertStore(Store store) throws PersistenceException {
        logger.info("Inserting store...");
        if (store == null) {
            String error = "Store is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(store);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error inserting store: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Store inserted successfully.");
    }

    public void removeStore(Store store) throws PersistenceException {
        logger.info("Removing store...");
        if (store == null) {
            String error = "Store is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.merge(store));
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error removing store: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Store removed successfully.");
    }

    public List<Store> getListOfStores(Company company) throws PersistenceException {
        logger.info("Getting list of stores...");
        if (company == null) {
            String error = "Company is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        List<Store> listStores = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select s from Store s where " + " s.company = :company ");
            query.setParameter("company", company);
            listStores = query.getResultList();
        } catch (Exception ex) {
            String error = "Error getting list of stores: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("List of stores got successfully.");
        return listStores;
    }

    public void insertPrice(Price price) throws PersistenceException {
        logger.info("Including price for a product's store...");
        if (price == null) {
            String error = "Price is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(price);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error inserting price: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Price inserted successfully.");
    }

    public void updateStore(Store store) throws PersistenceException {
        logger.info("Updating store...");
        if (store == null) {
            String error = "Store is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(store);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error updating store: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Store updated successfully.");
    }

    public List<Price> getListOfPrices(Store store) throws PersistenceException {
        logger.info("Getting list of prices...");
        if (store == null) {
            String error = "Store is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        List<Price> listPrices = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select p from Price p where " + " p.id.store = :store ");
            query.setParameter("store", store);
            listPrices = query.getResultList();
        } catch (Exception ex) {
            String error = "Error getting list of prices: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("List of prices got successfully.");
        return listPrices;
    }

    public void removePrice(Price price) throws PersistenceException {
        logger.info("Removing price...");
        if (price == null) {
            String error = "Price is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("delete from Price p where " + " p = :price ");
            query.setParameter("price", price);
            query.executeUpdate();
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error removing price: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Price removed successfully.");
    }

    public void updatePrice(Price price) throws PersistenceException {
        logger.info("Updating price...");
        if (price == null) {
            String error = "Price is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(price);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error updating price: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Price updated successfully.");
    }

    public Price getPriceById(PricePk pk) throws PersistenceException {
        logger.info("Getting price...");
        if (pk == null) {
            String error = "Id is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        Price price = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select p from Price p where " + " p.id = :id ");
            query.setParameter("id", pk);
            price = (Price) query.getSingleResult();
        } catch (Exception ex) {
            String error = "Error getting price: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Price got successfully.");
        return price;
    }
}
