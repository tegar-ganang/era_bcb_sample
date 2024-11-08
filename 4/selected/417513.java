package org.antlride.internal.core.model.statement.builder;

import org.antlride.core.model.Rewrite;
import org.antlride.core.model.SemanticPredicate;
import org.antlride.core.model.SourceElement;
import org.antlride.core.model.builder.RewriteBuilder;
import org.antlride.core.model.builder.StatementBuilder;
import org.antlride.internal.core.model.statement.RewriteImpl;
import org.eclipse.core.runtime.Assert;

/**
 * Creates a new {@link Rewrite} builder.
 * 
 * @author Edgar Espina
 * @since 2.1.0
 */
public class RewriteBuilderImpl extends StatementBuilderImpl implements RewriteBuilder<SourceElement> {

    /**
   * The final product.
   */
    private final RewriteImpl rewrite;

    /**
   * Creates a new {@link RewriteBuilderImpl}.
   * 
   * @param statement The target statement. Cannot be null.
   */
    public RewriteBuilderImpl(RewriteImpl statement) {
        super(statement);
        rewrite = statement;
    }

    /**
   * {@inheritDoc}
   */
    public RewriteBuilder<SourceElement> withStatement(SemanticPredicate semanticPredicate) {
        Assert.isNotNull(semanticPredicate);
        rewrite.add(semanticPredicate);
        return this;
    }

    /**
   * {@inheritDoc}
   */
    public RewriteBuilder<SourceElement> withStatement(StatementBuilder<SourceElement> statement) {
        if (rewrite.getStatement() != null) {
            throw new IllegalStateException("The rewrite statement was already set.");
        }
        rewrite.setStatement(statement.build());
        return this;
    }

    @Override
    public Rewrite build() {
        return rewrite;
    }
}
