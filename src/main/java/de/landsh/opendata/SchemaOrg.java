package de.landsh.opendata;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class SchemaOrg {
    public static final String NS = "http://schema.org/";
    public static final Resource NAMESPACE;
    public static final Property startDate;
    public static final Property endDate;

    private static final Model m_model = ModelFactory.createDefaultModel();

    static {
        NAMESPACE = m_model.createResource(NS);
        startDate = m_model.createProperty(NS, "startDate");
        endDate = m_model.createProperty(NS, "endDate");
    }

    private SchemaOrg() {
    }

    public static String getURI() {
        return NS;
    }
}
