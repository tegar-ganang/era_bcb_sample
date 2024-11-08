package com.prossys.dao;

import com.prossys.model.Company;
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
public class CompanyDAO extends BaseDAO {

    Logger logger = Logger.getLogger(this.getClass().getName());

    public Company getCompanyByUsernameAndPassword(String username, String password) throws PersistenceException {
        logger.info("Getting company by username and password...");
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
        Company company = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select c from Company c where " + "c.username = :username and c.password = :password");
            query.setParameter("username", username);
            query.setParameter("password", password);
            company = (Company) query.getSingleResult();
        } catch (NoResultException ex) {
            String warning = "Company with username and password was not found.";
            logger.warn(warning);
            return null;
        } catch (Exception ex) {
            String error = "Error getting company by username and password: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Company with username and password was found");
        return company;
    }

    public void insertCompany(Company company) throws PersistenceException {
        logger.info("Inserting company...");
        if (company == null) {
            String error = "Company is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(company);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error inserting company: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Company inserted successfully.");
    }

    public void addProduct(Product product) throws PersistenceException {
        logger.info("Adding product...");
        if (product == null) {
            String error = "Product is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(product);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error adding: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Product added successfully.");
    }

    public Company searchCompanyById(Integer id) throws PersistenceException {
        logger.info("Searching company by Id...");
        EntityManager em = getEntityManager();
        Company company = null;
        try {
            em.getTransaction().begin();
            company = em.find(Company.class, id);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error searching company by Id: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Company " + company.getCnpj() + " found.");
        return company;
    }

    public Product searchProductById(Integer id) throws PersistenceException {
        logger.info("Searching product by Id...");
        EntityManager em = getEntityManager();
        Product product = null;
        try {
            em.getTransaction().begin();
            product = em.find(Product.class, id);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error searching product by Id: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Product " + product.getName() + " found.");
        return product;
    }

    public Store searchStoreById(Integer id) throws PersistenceException {
        logger.info("Searching store by Id...");
        EntityManager em = getEntityManager();
        Store store = null;
        try {
            em.getTransaction().begin();
            store = em.find(Store.class, id);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error searching store by Id: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Store " + store.getTradingName() + " found.");
        return store;
    }

    public void removeProduct(Product product) throws PersistenceException {
        logger.info("Removing product...");
        if (product == null) {
            String error = "Product is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("delete from Product p where " + " p = :product ");
            query.setParameter("product", product);
            query.executeUpdate();
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error removing product: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Product removed successfully.");
    }

    public void updateProduct(Product product) throws PersistenceException {
        logger.info("Updating product...");
        if (product == null) {
            String error = "Product is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(product);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String error = "Error updating product: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("Product updated successfully.");
    }

    public List<Product> getListOfProducts(Company company) throws PersistenceException {
        logger.info("Getting list of products...");
        if (company == null) {
            String error = "Company is null.";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        EntityManager em = getEntityManager();
        List<Product> listProducts = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select p from Product p where " + " p.company = :company ");
            query.setParameter("company", company);
            listProducts = query.getResultList();
        } catch (Exception ex) {
            String error = "Error getting list of products: " + ex.getMessage();
            logger.error(error);
            em.getTransaction().rollback();
            throw new PersistenceException(ex);
        } finally {
            em.close();
        }
        logger.info("List of products got successfully.");
        return listProducts;
    }
}
