package org.fao.fenix.persistence.commodity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.fao.fenix.domain.commodity.HS;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class HSDao {

    @PersistenceContext
    private EntityManager entityManager;

    public List<HS> findAll() {
        return entityManager.createQuery("from HS").getResultList();
    }

    public void backup() {
        ClassPathResource classpath = new ClassPathResource("commodity_codes/HarmonizedSystem.csv");
        try {
            List list = findAll();
            FileOutputStream stream = new FileOutputStream(classpath.getFile());
            for (int i = 0; i < list.size(); i++) {
                HS item = (HS) list.get(i);
                String line = "";
                line += (item.getId() + ":").trim();
                line += item.getCode() + ":";
                line += item.getDescription() + "\n";
                stream.write(line.getBytes());
            }
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restore() {
        ClassPathResource classpath = new ClassPathResource("commodity_codes/HarmonizedSystem.csv");
        int counter = 0;
        try {
            BufferedReader input = new BufferedReader(new FileReader(createTmpFile(classpath.getInputStream())));
            String line = null;
            while ((line = input.readLine()) != null) {
                save(createHSFromString(line));
                counter++;
            }
            input.close();
            System.out.println("ADDED " + counter + " HS CODES");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("ADDED " + counter + " HS CODES");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ADDED " + counter + " HS CODES");
        }
    }

    public File createTmpFile(InputStream io) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "tmp_hs.csv");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[256];
            int read = 0;
            while ((read = io.read(buf)) > 0) {
                fos.write(buf, 0, read);
            }
            return file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new File("");
    }

    public HS createHSFromString(String line) {
        HS hs = new HS();
        StringTokenizer tokenizer = new StringTokenizer(line, ":");
        if (tokenizer.hasMoreTokens()) hs.setCode(tokenizer.nextToken().trim());
        if (tokenizer.hasMoreTokens()) hs.setDescription(tokenizer.nextToken().trim());
        return hs;
    }

    @SuppressWarnings("unchecked")
    public List<HS> findByContinentName(String name) {
        Query query = entityManager.createQuery("from HS x WHERE x.continent LIKE :name");
        query.setParameter("name", name + "%");
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<HS> findAllLevel0() {
        List result = new ArrayList();
        List list = findAll();
        for (int i = 0; i < list.size(); i++) if (((HS) list.get(i)).getCode().length() == 2) result.add(list.get(i));
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<HS> findAllLevel1(String codeLevel0) {
        List result = new ArrayList();
        Query query = entityManager.createQuery("from HS x WHERE x.code LIKE :codeLevel0");
        List list = query.setParameter("codeLevel0", codeLevel0 + "%").getResultList();
        for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
            HS hs = (HS) iterator.next();
            if (hs.getCode().length() == 4) result.add(hs);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<HS> findAllLevel2(String codeLevel1) {
        List result = new ArrayList();
        Query query = entityManager.createQuery("from HS x WHERE x.code LIKE :codeLevel1");
        List list = query.setParameter("codeLevel1", codeLevel1 + "%").getResultList();
        for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
            HS hs = (HS) iterator.next();
            if (hs.getCode().length() == 6) result.add(hs);
        }
        return result;
    }

    public HS findById(long id) {
        return entityManager.find(HS.class, id);
    }

    @SuppressWarnings("unchecked")
    public void save(HS hs) {
        entityManager.persist(hs);
    }

    public HS update(HS hs) {
        return entityManager.merge(hs);
    }

    public void delete(HS hs) {
        entityManager.remove(hs);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
