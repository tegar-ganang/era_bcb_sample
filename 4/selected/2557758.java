package org.fao.fenix.persistence.gaul;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.fao.fenix.domain.gaul.FenixGaul0;
import org.fao.fenix.domain.gaul.FenixGaul1;
import org.fao.fenix.domain.gaul.FenixGaul2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class FenixGaulDao {

    @PersistenceContext
    private EntityManager entityManager;

    public List<FenixGaul0> findAllGaul0() {
        return entityManager.createQuery("from FenixGaul0").getResultList();
    }

    public List<FenixGaul1> findAllGaul1() {
        return entityManager.createQuery("from FenixGaul1").getResultList();
    }

    public List<FenixGaul2> findAllGaul2() {
        return entityManager.createQuery("from FenixGaul2").getResultList();
    }

    public void restoreGaul() {
        ClassPathResource classpath = new ClassPathResource("gaul_codes/FenixGaul.csv");
        List gaul0List = new ArrayList();
        List gaul1List = new ArrayList();
        List gaul2List = new ArrayList();
        FenixGaul0 gaul0;
        FenixGaul1 gaul1;
        FenixGaul2 gaul2;
        try {
            BufferedReader input = new BufferedReader(new FileReader(createTmpFile(classpath.getInputStream())));
            String line = null;
            while ((line = input.readLine()) != null) {
                gaul0 = createFenixGaul0FromString(line);
                if (!gaul0List.contains(gaul0.getCode())) {
                    gaul0List.add(gaul0.getCode());
                    saveGaul0(gaul0);
                }
                gaul1 = createFenixGaul1FromString(line);
                if (!gaul1List.contains(gaul1.getCode())) {
                    gaul1List.add(gaul1.getCode());
                    saveGaul1(gaul1);
                }
                gaul2 = createFenixGaul2FromString(line);
                if (!gaul2List.contains(gaul2.getCode())) {
                    gaul2List.add(gaul2.getCode());
                    saveGaul2(gaul2);
                }
            }
            input.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File createTmpFile(InputStream io) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "tmp.csv");
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

    public FenixGaul0 createFenixGaul0FromString(String line) {
        FenixGaul0 gaul = new FenixGaul0();
        try {
            StringTokenizer tokenizer = new StringTokenizer(line, ":");
            gaul.setCode(Long.parseLong(tokenizer.nextToken().trim()));
            gaul.setName(tokenizer.nextToken().trim());
            tokenizer.nextToken();
            tokenizer.nextToken();
            tokenizer.nextToken();
            tokenizer.nextToken();
            gaul.setContinent(tokenizer.nextToken().trim());
            if (tokenizer.hasMoreTokens()) gaul.setRegion(tokenizer.nextToken().trim()); else gaul.setRegion("Region Not Available");
        } catch (NoSuchElementException e) {
            System.out.print(".");
        } catch (NumberFormatException e) {
            System.out.print(".");
        }
        return gaul;
    }

    public FenixGaul1 createFenixGaul1FromString(String line) {
        FenixGaul1 gaul = new FenixGaul1();
        try {
            StringTokenizer tokenizer = new StringTokenizer(line, ":");
            gaul.setGaul0Code(Long.parseLong(tokenizer.nextToken().trim()));
            tokenizer.nextToken();
            gaul.setCode(Long.parseLong(tokenizer.nextToken().trim()));
            gaul.setName(tokenizer.nextToken().trim());
            tokenizer.nextToken();
            tokenizer.nextToken();
            gaul.setContinent(tokenizer.nextToken().trim());
            if (tokenizer.hasMoreTokens()) gaul.setRegion(tokenizer.nextToken().trim()); else gaul.setRegion("Region Not Available");
        } catch (NoSuchElementException e) {
            System.out.print(".");
        } catch (NumberFormatException e) {
            System.out.print(".");
        }
        return gaul;
    }

    public FenixGaul2 createFenixGaul2FromString(String line) {
        FenixGaul2 gaul = new FenixGaul2();
        try {
            StringTokenizer tokenizer = new StringTokenizer(line, ":");
            tokenizer.nextToken();
            tokenizer.nextToken();
            gaul.setGaul1Code(Long.parseLong(tokenizer.nextToken().trim()));
            tokenizer.nextToken();
            gaul.setCode(Long.parseLong(tokenizer.nextToken().trim()));
            gaul.setName(tokenizer.nextToken().trim());
            gaul.setContinent(tokenizer.nextToken().trim());
            if (tokenizer.hasMoreTokens()) gaul.setRegion(tokenizer.nextToken().trim()); else gaul.setRegion("Region Not Available");
        } catch (NoSuchElementException e) {
            System.out.print(".");
        } catch (NumberFormatException e) {
            System.out.print(".");
        }
        return gaul;
    }

    @SuppressWarnings("unchecked")
    public List<FenixGaul0> findRegionByContinentName(String name) {
        Query query = entityManager.createQuery("from FenixGaul0 x WHERE x.continent LIKE :name");
        query.setParameter("name", name + "%");
        List<FenixGaul0> list = query.getResultList();
        List<String> regions = new ArrayList();
        List<FenixGaul0> result = new ArrayList();
        for (int i = 0; i < list.size(); i++) if (!regions.contains(list.get(i).getRegion())) {
            regions.add(list.get(i).getRegion());
            result.add(list.get(i));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<FenixGaul0> findNationByRegionName(String name) {
        Query query = entityManager.createQuery("from FenixGaul0 x WHERE x.region LIKE :name");
        query.setParameter("name", name + "%");
        List<FenixGaul0> list = query.getResultList();
        List<String> nations = new ArrayList();
        List<FenixGaul0> result = new ArrayList();
        for (int i = 0; i < list.size(); i++) if (!nations.contains(list.get(i).getName())) {
            nations.add(list.get(i).getName());
            result.add(list.get(i));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<FenixGaul1> findProvenceByNationCode(long gaul0Code) {
        Query query = entityManager.createQuery("from FenixGaul1 x WHERE x.gaul0Code = :gaul0Code");
        query.setParameter("gaul0Code", gaul0Code);
        List<FenixGaul1> list = query.getResultList();
        List<String> nations = new ArrayList();
        List<FenixGaul1> result = new ArrayList();
        for (int i = 0; i < list.size(); i++) if (!nations.contains(list.get(i).getName())) {
            nations.add(list.get(i).getName());
            result.add(list.get(i));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<FenixGaul2> findCityByProvenceCode(long gaul1Code) {
        Query query = entityManager.createQuery("from FenixGaul2 x WHERE x.gaul1Code = :gaul1Code");
        query.setParameter("gaul1Code", gaul1Code);
        List<FenixGaul2> list = query.getResultList();
        List<String> nations = new ArrayList();
        List<FenixGaul2> result = new ArrayList();
        for (int i = 0; i < list.size(); i++) if (!nations.contains(list.get(i).getName())) {
            nations.add(list.get(i).getName());
            result.add(list.get(i));
        }
        return result;
    }

    public FenixGaul0 findGaul0ById(long id) {
        return entityManager.find(FenixGaul0.class, id);
    }

    public FenixGaul1 findGaul1ById(long id) {
        return entityManager.find(FenixGaul1.class, id);
    }

    public FenixGaul2 findGaul2ById(long id) {
        return entityManager.find(FenixGaul2.class, id);
    }

    @SuppressWarnings("unchecked")
    public void saveGaul0(FenixGaul0 gaul) {
        entityManager.persist(gaul);
    }

    @SuppressWarnings("unchecked")
    public void saveGaul1(FenixGaul1 gaul) {
        entityManager.persist(gaul);
    }

    @SuppressWarnings("unchecked")
    public void saveGaul2(FenixGaul2 gaul) {
        entityManager.persist(gaul);
    }

    public FenixGaul0 updateGaul0(FenixGaul0 gaul) {
        return entityManager.merge(gaul);
    }

    public FenixGaul1 updateGaul0(FenixGaul1 gaul) {
        return entityManager.merge(gaul);
    }

    public FenixGaul2 updateGaul0(FenixGaul2 gaul) {
        return entityManager.merge(gaul);
    }

    public void deleteGaul0(FenixGaul0 gaul) {
        entityManager.remove(gaul);
    }

    public void deleteGaul1(FenixGaul1 gaul) {
        entityManager.remove(gaul);
    }

    public void deleteGaul0(FenixGaul2 gaul) {
        entityManager.remove(gaul);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
