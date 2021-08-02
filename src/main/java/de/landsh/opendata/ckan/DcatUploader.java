package de.landsh.opendata.ckan;

import de.landsh.opendata.DCATAPde;
import de.landsh.opendata.Locn;
import de.landsh.opendata.SPDX;
import de.landsh.opendata.SchemaOrg;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

/**
 * Fügt mit einem DCAT-AP.de Upload Datensätze zu CKAN hinzu.
 */
@RequiredArgsConstructor
public class DcatUploader {

    private static final String portalBaseURL = "https://opendata.schleswig-holstein.de";
    private final CkanAPI ckanAPI;

    private static String getString(Resource resource, Property property) {
        if (!resource.hasProperty(property)) return null;
        return getString(resource.getProperty(property));
    }

    /**
     * Liefert den Wert (wenn es ein Literal ist) oder den URI (wenn es eine URIResource) ist des Objekts eines
     * Statements zurück.
     */
    private static String getString(Statement statement) {
        RDFNode object = statement.getObject();
        if (object.isLiteral()) {
            return object.asLiteral().getString();
        } else if (object.isURIResource()) {
            return object.asResource().getURI();
        }
        return null;
    }

    private static void setExtraValue(JSONObject dataset, String key, String value) {
        final JSONObject entry = new JSONObject();
        entry.put("key", key);
        entry.put("value", value);

        int foundAtPosition = -1;
        JSONArray extras = dataset.getJSONArray("extras");
        for (int i = 0; i < extras.length(); i++) {
            JSONObject it = (JSONObject) extras.get(i);
            if (key.equals(it.getString("key"))) {
                foundAtPosition = i;
            }
        }
        if (foundAtPosition > -1) {
            extras.put(foundAtPosition, entry);
        } else {
            // Es gibt noch keinen entsprechenden Eintrag in "extras"
            extras.put(entry);
        }
    }

    public String upload(Resource dataset) throws IOException {

        final String packageName;
        if (dataset.getURI().startsWith(portalBaseURL + "/dataset/")) {
            // desired package name
            packageName = StringUtils.substringAfter(dataset.getURI(), portalBaseURL + "/dataset/");
        } else {
            packageName = UUID.randomUUID().toString();
        }

        final JSONObject json = new JSONObject();
        json.put("extras", new JSONArray());

        json.put("title", getString(dataset, DCTerms.title));
        json.put("notes", getString(dataset, DCTerms.description));
        json.put("license_id", getString(dataset, DCTerms.license));
        json.put("name", packageName);

        final String ownerOrg = StringUtils.substringAfterLast(getString(dataset, DCTerms.publisher), "/organization/");
        json.put("owner_org", ownerOrg);

        if (dataset.hasProperty(DCAT.theme)) {
            final JSONArray groups = new JSONArray();
            json.put("groups", groups);
            final StmtIterator it = dataset.listProperties(DCAT.theme);
            while (it.hasNext()) {
                final Statement stmt = it.next();
                final String theme = StringUtils.lowerCase(StringUtils.substringAfter(getString(stmt), "http://publications.europa.eu/resource/authority/data-theme/"));
                final JSONObject group = new JSONObject();
                group.put("name", theme);
                groups.put(group);
            }
        }

        if (dataset.hasProperty(DCAT.keyword)) {
            final JSONArray tags = new JSONArray();
            json.put("tags", tags);
            final StmtIterator it = dataset.listProperties(DCAT.keyword);
            while (it.hasNext()) {
                final Statement stmt = it.next();
                final JSONObject tag = new JSONObject();
                tag.put("name", getString(stmt));
                tags.put(tag);
            }
        }

        if (dataset.hasProperty(DCTerms.temporal)) {
            final Resource temporal = dataset.getPropertyResourceValue(DCTerms.temporal);
            if (temporal.hasProperty(SchemaOrg.startDate)) {
                setExtraValue(json, "temporal_start",
                        temporal.getProperty(SchemaOrg.startDate)
                                .getObject().asLiteral().getString());
            }
            if (temporal.hasProperty(SchemaOrg.endDate)) {
                setExtraValue(json, "temporal_end",
                        temporal.getProperty(SchemaOrg.endDate)
                                .getObject().asLiteral().getString());
            }
        }

        if (dataset.hasProperty(DCTerms.modified)) {
            setExtraValue(json, "modified", dataset.getProperty(DCTerms.modified).getObject().asLiteral().getString());
        }
        if (dataset.hasProperty(DCTerms.issued)) {
            setExtraValue(json, "issued", getString(dataset, DCTerms.issued));
        }
        if (dataset.hasProperty(DCATAPde.licenseAttributionByText)) {
            setExtraValue(json, "licenseAttributionByText", getString(dataset, DCATAPde.licenseAttributionByText));
        }
        if (dataset.hasProperty(DCTerms.accrualPeriodicity)) {
            setExtraValue(json, "frequency", getString(dataset, DCTerms.accrualPeriodicity));
        }
        if (dataset.hasProperty(DCATAPde.politicalGeocodingURI)) {
            setExtraValue(json, "spatial_uri", getString(dataset, DCATAPde.politicalGeocodingURI));
        }
        if (dataset.hasProperty(DCATAPde.politicalGeocodingLevelURI)) {
            setExtraValue(json, "politicalGeocodingLevelURI", getString(dataset, DCATAPde.politicalGeocodingLevelURI));
        }
        if (dataset.hasProperty(DCTerms.spatial)) {
            final Resource location = dataset.getPropertyResourceValue(DCTerms.spatial);
            final StmtIterator it = location.listProperties(Locn.geometry);

            while (it.hasNext()) {
                final Statement stmt = it.next();
                if (stmt.getObject().isLiteral()) {
                    final String datatype = stmt.getObject().asLiteral().getDatatypeURI();
                    if ("https://www.iana.org/assignments/media-types/application/vnd.geo+json".equals(datatype)) {
                        setExtraValue(json, "spatial", stmt.getObject().asLiteral().getString());
                    }
                }
            }

        }

        final String packageId = ckanAPI.createPackage(json);

        uploadResources(dataset, packageId);

        if (dataset.hasProperty(DCTerms.isVersionOf)) {
            addToCollection(dataset, packageId);
        }

        return packageId;
    }

    private void addToCollection(Resource dataset, String packageId) throws IOException {
        final StmtIterator it = dataset.listProperties(DCTerms.isVersionOf);
        while (it.hasNext()) {
            final Statement stmt = it.next();
            if (stmt.getObject().isResource()) {
                final String uri = stmt.getObject().asResource().getURI();
                if (uri.startsWith(portalBaseURL)) {
                    final String collectionName = StringUtils.substringAfterLast(uri, "/");
                    ckanAPI.putDatasetInCollection(packageId, collectionName);
                }
            }
        }
    }

    private void uploadResources(Resource dataset, String packageId) throws IOException {
        // add resources
        final StmtIterator it = dataset.listProperties(DCAT.distribution);
        while (it.hasNext()) {
            final Statement stmt = it.next();
            final Resource distribution = stmt.getObject().asResource();

            final JSONObject jsonResource = new JSONObject();
            jsonResource.put("package_id", packageId);
            if (distribution.hasProperty(DCAT.downloadURL)) {
                jsonResource.put("url", getString(distribution, DCAT.downloadURL));
            }
            if (distribution.hasProperty(DCAT.accessURL)) {
                jsonResource.put("access_url", getString(distribution, DCAT.accessURL));
            }
            if (distribution.hasProperty(DCTerms.title)) {
                jsonResource.put("name", getString(distribution, DCTerms.title));
            }
            if (distribution.hasProperty(DCTerms.description)) {
                jsonResource.put("description", getString(distribution, DCTerms.description));
            }
            if (distribution.hasProperty(DCAT.mediaType)) {
                jsonResource.put("mimetype", getString(distribution, DCAT.mediaType));
            }
            if (distribution.hasProperty(DCTerms.format)) {
                jsonResource.put("format", StringUtils.substringAfterLast(getString(distribution, DCTerms.format), "/"));
            }
            if (distribution.hasProperty(DCTerms.license)) {
                jsonResource.put("license", getString(distribution, DCTerms.license));
            }
            if (distribution.hasProperty(DCATAPde.licenseAttributionByText)) {
                jsonResource.put("licenseAttributionByText", getString(distribution, DCATAPde.licenseAttributionByText));
            }
            if( distribution.hasProperty(SPDX.checksum)) {
                final Resource checksum = distribution.getPropertyResourceValue(SPDX.checksum);
                jsonResource.put("hash", getString(checksum, SPDX.checksumValue));
                jsonResource.put("hash_algorithm", getString(checksum, SPDX.algorithm));
            }

            ckanAPI.createResource(jsonResource);
        }
    }

}
