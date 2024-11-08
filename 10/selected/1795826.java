package org.t2framework.samples.guice.service.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import org.t2framework.samples.guice.entity.Person;
import org.t2framework.samples.guice.service.PersonService;
import com.google.inject.Inject;

/**
 * PersonServiceImpl
 * 
 * @author yone098
 * @author shot
 */
public class PersonServiceImpl implements PersonService {

    @Inject
    private EntityManager entityManager;

    @Override
    public Person getPersonById(Integer id) {
        return entityManager.find(Person.class, id);
    }

    @Override
    public Person getPersonByName(String name) {
        try {
            return (Person) entityManager.createQuery("select p from Person p where p.name = :name").setParameter("name", name).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Person> getPersons() {
        return (List<Person>) entityManager.createQuery("SELECT p FROM Person p order by p.id").getResultList();
    }

    @Override
    public void save(Person person) {
        EntityTransaction transaction = this.entityManager.getTransaction();
        try {
            transaction.begin();
            if (person.getId() == null) {
                this.entityManager.persist(person);
            } else {
                this.entityManager.merge(person);
            }
            this.entityManager.flush();
            transaction.commit();
        } catch (Throwable t) {
            transaction.rollback();
        }
    }

    @Override
    public int removeById(Integer id) {
        EntityTransaction transaction = this.entityManager.getTransaction();
        int ret = 0;
        try {
            transaction.begin();
            ret = entityManager.createQuery("delete from Person p where p.id = :id").setParameter("id", id).executeUpdate();
            this.entityManager.flush();
            transaction.commit();
        } catch (Throwable t) {
            transaction.rollback();
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Person> getPersonsByName(String name) {
        try {
            return (List<Person>) entityManager.createQuery("select p from Person p where p.name like :name order by p.id").setParameter("name", "%" + name + "%").getResultList();
        } catch (NoResultException ex) {
            return null;
        }
    }
}
