package org.openrdf.jena;

import java.rmi.server.ServerCloneException;
import java.util.logging.Logger;
import java.io.File;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.sail.Sail;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.StackableSail;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.nativerdf.NativeStore;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailFactory;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.sail.SailRepository;
import info.aduna.collections.iterators.CloseableIterator;
import info.aduna.collections.iterators.EmptyIterator;
import info.aduna.collections.iterators.IteratorWrapper;
import info.aduna.iteration.CloseableIterationBase;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.graph.GraphUtil;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.AllCapabilities;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.shared.AddDeniedException;
import com.hp.hpl.jena.shared.DeleteDeniedException;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import net.datao.utils.StringHelper;

/**
 * This class wraps a Sesame repository and presents it as a Jena Graph
 * 
 * NOTE: This class does NOT support remove'ing triples from the iterators returned by find!
 * This means essentially everything, removing can only be done by performRemove call!
 * 
 * @author wf
 * @author grimnes
 * TODO: Think about and check if re-ification works.
 */
public class GraphSesame extends GraphBase {

    protected boolean writeable = true;

    protected Repository repository;

    protected Logger logger = Logger.getLogger(GraphSesame.class.getName());

    private static ValueFactory sesameFactory;

    /**
	 * Get a Sesame ValueFactory used to create Sesame URI, blank node, literal, etc. 
	 * @return Sesame ValueFactory
	 */
    public static ValueFactory getValueFactory() {
        return sesameFactory;
    }

    private void setCapabilities() {
        capabilities = new AllCapabilities() {

            public boolean iteratorRemoveAllowed() {
                return false;
            }
        };
    }

    /**
	 * Create a Jena graph from an existing repository
	 *
	 */
    public GraphSesame(Repository repository) {
        setCapabilities();
        this.repository = repository;
        try {
            repository.initialize();
            sesameFactory = repository.getValueFactory();
        } catch (Exception e) {
        }
        sesameFactory = repository.getValueFactory();
    }

    /**
	 * Create a Jena Graph, which represents a Sesame in-memory local repository
	 * that does not have persistent support, synchroinzation support and inference capability.
	 */
    public GraphSesame() {
        this(false, false, null, null);
    }

    /**
	 * Create a Jena Graph, which represents a Sesame in-memory local repository 
	 * that has the following customizable features. It will firstly create a 
	 * Sesame repository, then create a Sesame graph from the repository.
	 * @param inferencing  the Sesame repository supports inference or not.   
	 * @param sync         the the Sesame repository is synchronized or not.
	 * @param fileName     the name of the file provides the persistent storage.
	 *                     Null means no persistent support required.
	 * @param format       the RDF data format in the persistent storage.
	 */
    public GraphSesame(boolean inferencing, boolean sync, String fileName, String format) {
        super();
        setCapabilities();
        MemoryStore m = null;
        if (fileName != null && !StringHelper.isEmpty(fileName)) {
            m = new MemoryStore(new File(fileName));
            m.setPersist(true);
        } else m = new MemoryStore();
        if (sync) m.setSyncDelay(1000);
        if (inferencing) repository = new SailRepository(new ForwardChainingRDFSInferencer(m)); else repository = new SailRepository(m);
        try {
            repository.initialize();
            sesameFactory = repository.getValueFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unable to initialize MemStore for Jena2Sesame");
        }
    }

    /**
	 * Create a Jena Graph, which represents a Sesame native local repository.
	 * @param sync  the the Sesame repository is synchronized or not.
	 * @param dir   specifies the directory that can be used by the native sail to store its files.
	 */
    public GraphSesame(boolean sync, String dir) {
        super();
        setCapabilities();
        String indexes = "spoc,posc,cosp";
        repository = new SailRepository(new NativeStore(new File(dir), indexes));
        try {
            repository.initialize();
            sesameFactory = repository.getValueFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unable to initialize MemStore for Jena2Sesame");
        }
    }

    public Statement jena2sesame(Triple t) {
        Resource subject = null;
        URI predicate = null;
        Value object = null;
        try {
            subject = (Resource) ValueToNode.reverse(t.getSubject());
            predicate = (URI) ValueToNode.reverse(t.getPredicate());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("You can't create triples with literals as subjects or predicates, or with BlankNodes as predicates.");
        }
        object = ValueToNode.reverse(t.getObject());
        return new StatementImpl(subject, predicate, object);
    }

    /**
	 * 
	 * 
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#graphBaseFind(com.hp.hpl.jena.graph.TripleMatch)
	 */
    protected ExtendedIterator graphBaseFind(TripleMatch tm) {
        Statement stri = null;
        try {
            stri = jena2sesame(tm.asTriple());
        } catch (IllegalArgumentException ce) {
            return new ExtendedStatementIterator(new EmptyIterator<Statement>());
        }
        try {
            RepositoryResult<Statement> stit = repository.getConnection().getStatements(stri.getSubject(), stri.getPredicate(), stri.getObject(), false, new Resource[0]);
            return new ExtendedStatementIterator(new IteratorWrapper<Statement>(stit.asList().iterator()));
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /** 
	 * @see com.hp.hpl.jena.graph.impl.GraphWithPerform#performAdd(com.hp.hpl.jena.graph.Triple)
	 */
    public void performAdd(Triple t) {
        if (!writeable) throw new AddDeniedException("graph is readonly");
        Statement stri = jena2sesame(t);
        try {
            repository.getConnection().add(stri.getSubject(), stri.getPredicate(), stri.getObject());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * @see com.hp.hpl.jena.graph.impl.GraphWithPerform#performDelete(com.hp.hpl.jena.graph.Triple)
	 */
    public void performDelete(Triple t) {
        if (!writeable) throw new DeleteDeniedException("graph is readonly");
        Statement stri = jena2sesame(t);
        try {
            repository.getConnection().remove(stri.getSubject(), stri.getPredicate(), stri.getObject());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public Repository getRepository() {
        return repository;
    }

    public void close() {
        try {
            logger.finer("closing a GraphSesame, also closes repository: " + repository);
            super.close();
            repository.shutDown();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * We override this, maybe Sesame's version is more efficient. 
	 *@Override 
	 */
    protected int graphBaseSize() {
        ExtendedIterator it = GraphUtil.findAll(this);
        int tripleCount = 0;
        while (it.hasNext()) {
            it.next();
            tripleCount += 1;
        }
        it.close();
        return tripleCount;
    }

    @Override
    public BulkUpdateHandler getBulkUpdateHandler() {
        return new SesameBulkUpdateHandler(this);
    }

    public boolean isWriteable() {
        return writeable;
    }

    public void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }
}
