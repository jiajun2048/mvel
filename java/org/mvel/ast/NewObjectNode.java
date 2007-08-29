package org.mvel.ast;

import org.mvel.ASTNode;
import static org.mvel.AbstractParser.getCurrentThreadParserContext;
import org.mvel.Accessor;
import org.mvel.CompileException;
import org.mvel.ParserContext;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import static org.mvel.optimizers.OptimizerFactory.getThreadAccessorOptimizer;
import static org.mvel.util.ArrayTools.findFirst;

/**
 * @author Christopher Brock
 */
public class NewObjectNode extends ASTNode {
    private transient Accessor newObjectOptimizer;
    private String className;

    public NewObjectNode(char[] expr, int fields) {
        super(expr, fields);

        int endRange = findFirst('(', expr);
        if (endRange == -1) {
            className = new String(expr);
        }
        else {
            className = new String(expr, 0, findFirst('(', expr));
        }

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            ParserContext pCtx = getCurrentThreadParserContext();
            if (pCtx != null && pCtx.hasImport(className)) {
                egressType = pCtx.getImport(className);
            }
            else {
                try {
                    egressType = Thread.currentThread().getContextClassLoader().loadClass(className);
                    //        egressType = Class.forName(className);
                }
                catch (ClassNotFoundException e) {
                    //           throw new CompileException("class not found: " + name, e);
                }
            }

            if (egressType != null) {
                rewriteClassReferenceToFQCN();
            }
        }

    }

    private void rewriteClassReferenceToFQCN() {
        String FQCN = egressType.getName();

        if (!className.equals(FQCN)) {
            int idx = FQCN.lastIndexOf('$');
            if (idx != -1 && className.lastIndexOf('$') == -1) {
                this.name = (FQCN.substring(0, idx + 1) + new String(this.name)).toCharArray();
            }
            else {
                this.name = (FQCN.substring(0, FQCN.lastIndexOf('.') + 1) + new String(this.name)).toCharArray();
            }
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (newObjectOptimizer == null) {
            if (egressType == null) {
                /**
                 * This means we couldn't resolve the type at the time this AST node was created, which means
                 * we have to attempt runtime resolution.
                 */

                if (factory != null && factory.isResolveable(className)) {
                    try {
                        egressType = (Class) factory.getVariableResolver(className).getValue();
                        rewriteClassReferenceToFQCN();
                    }
                    catch (ClassCastException e) {
                        throw new CompileException("cannot construct object: " + className + " is not a class reference", e);
                    }
                }
            }


            AccessorOptimizer optimizer = getThreadAccessorOptimizer();
            newObjectOptimizer = optimizer.optimizeObjectCreation(name, ctx, thisValue, factory);

            /**
             * Check to see if the optimizer actually produced the object during optimization.  If so,
             * we return that value now.
             */
            if (optimizer.getResultOptPass() != null) {
                egressType = optimizer.getEgressType();
                return optimizer.getResultOptPass();
            }
        }

        return newObjectOptimizer.getValue(ctx, thisValue, factory);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }


    public Accessor getNewObjectOptimizer() {
        return newObjectOptimizer;
    }
}