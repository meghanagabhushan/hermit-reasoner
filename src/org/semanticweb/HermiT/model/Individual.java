package org.semanticweb.HermiT.model;

import org.semanticweb.HermiT.*;

/**
 * Represents an individual in a DL clause.
 */
public class Individual extends Term {
    private static final long serialVersionUID=2791684055390160959L;

    protected final String m_uri;
    
    protected Individual(String uri) {
        m_uri=uri;
    }
    public String getURI() {
        return m_uri;
    }
    public String toString() {
        return toString(Namespaces.EMPTY_INSTANCE);
    }
    protected Object readResolve() {
        return s_interningManager.intern(this);
    }
    public String toString(Namespaces namespaces) {
        return namespaces.abbreviateAsNamespace(m_uri);
    }

    protected static InterningManager<Individual> s_interningManager=new InterningManager<Individual>() {
        protected boolean equal(Individual object1,Individual object2) {
            return object1.m_uri.equals(object2.m_uri);
        }
        protected int getHashCode(Individual object) {
            return object.m_uri.hashCode();
        }
    };
    
    public static Individual create(String uri) {
        return s_interningManager.intern(new Individual(uri));
    }
}
