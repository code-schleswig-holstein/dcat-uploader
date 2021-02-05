package de.landsh.opendata;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class DCATAPde {
    public static final String NS = "http://dcat-ap.de/def/dcatde/";
    public static final Resource NAMESPACE;
    public static final Property maintainer;
    public static final Property originator;
    public static final Property contributorID;
    public static final Property politicalGeocodingLevelURI;
    public static final Property politicalGeocodingURI;
    public static final Property plannedAvailability;
    public static final Property licenseAttributionByText;
    private static final Model m_model = ModelFactory.createDefaultModel();

    static {
        NAMESPACE = m_model.createResource(NS);
        maintainer = m_model.createProperty(NS, "maintainer");
        originator = m_model.createProperty(NS, "originator");
        contributorID = m_model.createProperty(NS, "contributorID");
        politicalGeocodingLevelURI = m_model.createProperty(NS, "politicalGeocodingLevelURI");
        politicalGeocodingURI = m_model.createProperty(NS, "politicalGeocodingURI");
        plannedAvailability = m_model.createProperty(NS, "plannedAvailability");
        licenseAttributionByText = m_model.createProperty(NS, "licenseAttributionByText");
    }

    private DCATAPde() {
    }

    public static String getURI() {
        return NS;
    }
}
