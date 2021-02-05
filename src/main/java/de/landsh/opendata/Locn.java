package de.landsh.opendata;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class Locn {
    public static final String NS = "http://www.w3.org/ns/locn#";
    public static final Resource NAMESPACE;
    public static final Property geometry;
    private static final Model m_model = ModelFactory.createDefaultModel();

    static {
        NAMESPACE = m_model.createResource(NS);
        geometry = m_model.createProperty(NS, "geometry");
    }

    private Locn() {
    }

    public static String getURI() {
        return NS;
    }
}
