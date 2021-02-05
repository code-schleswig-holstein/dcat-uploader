package de.landsh.opendata.ckan;

import de.landsh.opendata.DCATAPde;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DcatUploaderTest {

    private static final String geometryInJSON = "{\"type\":\"Polygon\",\"coordinates\":[[[10.753384,54.033113],[10.754036,54.033269],[10.750559,54.039196],[10.75143,54.039301],[10.753384,54.033113]]]}";
    private static final Property geometry = ResourceFactory.createProperty("http://www.w3.org/ns/locn#", "geometry");
    private final CkanAPI ckanAPI = Mockito.mock(CkanAPI.class);
    private DcatUploader dcatUploader;

    private static String getExtrasValue(JSONObject dataset, String key) {
        final JSONArray extras = dataset.getJSONArray("extras");
        for (Object o : extras) {
            JSONObject extra = (JSONObject) o;
            if (key.equals(extra.getString("key"))) {
                return extra.getString("value");
            }
        }
        return null;
    }

    @Before
    public void setUp() {
        dcatUploader = new DcatUploader(ckanAPI);
    }

    private org.apache.jena.rdf.model.Resource generateDataset() {
        Model model = ModelFactory.createDefaultModel();

        LocalDate date = LocalDate.of(2020, 10, 13);

        final Resource dataset = model.createResource("https://opendata.schleswig-holstein.de/dataset/demo");
        dataset.addProperty(RDF.type, DCAT.Dataset);

        final org.apache.jena.rdf.model.Resource temporal = model.createResource();
        temporal.addProperty(RDF.type, DCTerms.PeriodOfTime);
        temporal.addLiteral(model.createProperty("http://schema.org/", "startDate"),
                model.createTypedLiteral(date.minusDays(2).format(DateTimeFormatter.ISO_DATE), XSDDatatype.XSDdate));
        temporal.addLiteral(model.createProperty("http://schema.org/", "endDate"),
                model.createTypedLiteral(date.minusDays(1).format(DateTimeFormatter.ISO_DATE), XSDDatatype.XSDdate));
        dataset.addProperty(DCTerms.temporal, temporal);

        dataset.addLiteral(DCTerms.title, "My first dataset");
        dataset.addLiteral(DCTerms.issued, model.createTypedLiteral(date.format(DateTimeFormatter.ISO_DATE), XSDDatatype.XSDdate));
        dataset.addLiteral(DCTerms.modified, model.createTypedLiteral(date.format(DateTimeFormatter.ISO_DATE), XSDDatatype.XSDdate));
        dataset.addProperty(DCTerms.license, ResourceFactory.createResource("http://dcat-ap.de/def/licenses/cc-by/4.0"));
        dataset.addProperty(DCATAPde.licenseAttributionByText, "ZIT-SH");
        dataset.addProperty(DCAT.theme, ResourceFactory.createResource("http://publications.europa.eu/resource/authority/data-theme/TRAN"));

        dataset.addProperty(DCTerms.accrualPeriodicity, ResourceFactory.createResource("http://publications.europa.eu/resource/authority/frequency/DAILY"));

        final Resource loc = model.createResource("http://dcat-ap.de/def/politicalGeocoding/regionalKey/010550044044");
        loc.addProperty(RDF.type, DCTerms.Location);
        dataset.addProperty(DCTerms.spatial, loc);

        dataset.addProperty(DCATAPde.politicalGeocodingURI, ResourceFactory.createResource("http://dcat-ap.de/def/politicalGeocoding/regionalKey/010550044044"));
        dataset.addProperty(DCATAPde.politicalGeocodingLevelURI, ResourceFactory.createResource("http://dcat-ap.de/def/politicalGeocoding/Level/municipality"));
        dataset.addLiteral(DCTerms.description, "This is a description of my dataset.");
        dataset.addProperty(DCAT.keyword, "demo");
        dataset.addProperty(DCAT.keyword, "my dataset");
        dataset.addProperty(DCTerms.publisher, ResourceFactory.createResource("https://opendata.schleswig-holstein.de/organization/2a6d6241-fdfd-4d9a-9106-8c658be43a27"));

        final Resource distribution = model.createResource("https://opendata.schleswig-holstein.de/dataset/demo/resource");
        distribution.addProperty(RDF.type, DCAT.Distribution);
        distribution.addProperty(DCTerms.license, ResourceFactory.createResource("http://dcat-ap.de/def/licenses/cc-by/4.0"));
        distribution.addLiteral(DCATAPde.licenseAttributionByText, "ZIT-SH");
        distribution.addLiteral(DCTerms.title, "data.csv");
        distribution.addLiteral(DCAT.byteSize, model.createTypedLiteral(500, XSDDatatype.XSDdecimal));
        distribution.addProperty(DCTerms.format, ResourceFactory.createResource("http://publications.europa.eu/resource/authority/file-type/CSV"));
        final Resource url = model.createResource("http://example.org/data.csv");
        distribution.addProperty(DCAT.accessURL, url);
        distribution.addProperty(DCAT.downloadURL, url);
        dataset.addProperty(DCAT.distribution, distribution);

        return dataset;
    }

    private org.apache.jena.rdf.model.Resource generateDatasetWithGeometry() {
        final Model model = ModelFactory.createDefaultModel();

        final LocalDate date = LocalDate.of(2020, 10, 13);

        final Resource dataset = model.createResource("https://opendata.schleswig-holstein.de/dataset/demo");
        dataset.addProperty(RDF.type, DCAT.Dataset);

        final org.apache.jena.rdf.model.Resource temporal = model.createResource();
        temporal.addProperty(RDF.type, DCTerms.PeriodOfTime);
        temporal.addLiteral(model.createProperty("http://schema.org/", "startDate"),
                model.createTypedLiteral(date.minusDays(2).format(DateTimeFormatter.ISO_DATE), XSDDatatype.XSDdate));
        temporal.addLiteral(model.createProperty("http://schema.org/", "endDate"),
                model.createTypedLiteral(date.minusDays(1).format(DateTimeFormatter.ISO_DATE), XSDDatatype.XSDdate));
        dataset.addProperty(DCTerms.temporal, temporal);

        dataset.addLiteral(DCTerms.title, "My first dataset");
        dataset.addLiteral(DCTerms.issued, model.createTypedLiteral(date.format(DateTimeFormatter.ISO_DATE), XSDDatatype.XSDdate));
        dataset.addLiteral(DCTerms.modified, model.createTypedLiteral(date.format(DateTimeFormatter.ISO_DATE), XSDDatatype.XSDdate));
        dataset.addProperty(DCTerms.license, ResourceFactory.createResource("http://dcat-ap.de/def/licenses/cc-by/4.0"));
        dataset.addProperty(DCATAPde.licenseAttributionByText, "ZIT-SH");
        dataset.addProperty(DCAT.theme, ResourceFactory.createResource("http://publications.europa.eu/resource/authority/data-theme/TRAN"));

        dataset.addProperty(DCTerms.accrualPeriodicity, ResourceFactory.createResource("http://publications.europa.eu/resource/authority/frequency/DAILY"));

        final Resource loc = model.createResource();
        loc.addProperty(RDF.type, DCTerms.Location);

        loc.addLiteral(geometry, model.createTypedLiteral(
                geometryInJSON,
                "https://www.iana.org/assignments/media-types/application/vnd.geo+json"
        ));
        loc.addLiteral(geometry, model.createTypedLiteral(
                "POLYGON(( 10.753384 54.033113 , 10.754036 54.033269 , 10.750559 54.039196 , 10.75143 54.039301 , 10.753384 54.033113 ))",
                "http://www.opengis.net/ont/geosparql#wktLiteral"
        ));

        dataset.addProperty(DCTerms.spatial, loc);
        dataset.addLiteral(DCTerms.description, "This is a description of my dataset.");
        dataset.addProperty(DCAT.keyword, "demo");
        dataset.addProperty(DCAT.keyword, "my dataset");
        dataset.addProperty(DCTerms.publisher, ResourceFactory.createResource("https://opendata.schleswig-holstein.de/organization/2a6d6241-fdfd-4d9a-9106-8c658be43a27"));

        final Resource distribution = model.createResource("https://opendata.schleswig-holstein.de/dataset/demo/resource");
        distribution.addProperty(RDF.type, DCAT.Distribution);
        distribution.addProperty(DCTerms.license, ResourceFactory.createResource("http://dcat-ap.de/def/licenses/cc-by/4.0"));
        distribution.addLiteral(DCATAPde.licenseAttributionByText, "ZIT-SH");
        distribution.addLiteral(DCTerms.title, "data.csv");
        distribution.addLiteral(DCAT.byteSize, model.createTypedLiteral(500, XSDDatatype.XSDdecimal));
        distribution.addProperty(DCTerms.format, ResourceFactory.createResource("http://publications.europa.eu/resource/authority/file-type/CSV"));
        final Resource url = model.createResource("http://example.org/data.csv");
        distribution.addProperty(DCAT.accessURL, url);
        distribution.addProperty(DCAT.downloadURL, url);
        dataset.addProperty(DCAT.distribution, distribution);

        return dataset;
    }

    @Test
    public void testUpload() throws IOException {
        final ArgumentCaptor<JSONObject> argumentPackage = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.when(ckanAPI.createPackage(argumentPackage.capture())).thenReturn("demo");

        final ArgumentCaptor<JSONObject> argumentResource = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.when(ckanAPI.createResource(argumentResource.capture())).thenReturn("resource");

        // invoke method
        final String packageId = dcatUploader.upload(generateDataset());

        final JSONObject jsonPackage = argumentPackage.getValue();
        final JSONObject jsonResource = argumentResource.getValue();

        Assert.assertNotNull(jsonPackage);
        assertEquals("demo", jsonPackage.getString("name"));
        assertEquals("My first dataset", jsonPackage.getString("title"));
        assertEquals("This is a description of my dataset.", jsonPackage.getString("notes"));
        assertEquals("http://dcat-ap.de/def/licenses/cc-by/4.0", jsonPackage.getString("license_id"));
        assertEquals(1, jsonPackage.getJSONArray("groups").length());
        assertEquals("tran", jsonPackage.getJSONArray("groups").getJSONObject(0).getString("name"));
        assertEquals("2a6d6241-fdfd-4d9a-9106-8c658be43a27", jsonPackage.getString("owner_org"));
        assertEquals(2, jsonPackage.getJSONArray("tags").length());

        assertNotNull(jsonPackage.get("extras"));

        assertEquals("2020-10-13", getExtrasValue(jsonPackage, "issued"));
        assertEquals("2020-10-11", getExtrasValue(jsonPackage, "temporal_start"));
        assertEquals("2020-10-12", getExtrasValue(jsonPackage, "temporal_end"));
        assertEquals("ZIT-SH", getExtrasValue(jsonPackage, "licenseAttributionByText"));
        assertEquals("http://publications.europa.eu/resource/authority/frequency/DAILY", getExtrasValue(jsonPackage, "frequency"));
        assertEquals("http://dcat-ap.de/def/politicalGeocoding/regionalKey/010550044044", getExtrasValue(jsonPackage, "spatial_uri"));
        assertEquals("http://dcat-ap.de/def/politicalGeocoding/Level/municipality", getExtrasValue(jsonPackage, "politicalGeocodingLevelURI"));

        assertEquals("demo", packageId);

        assertNotNull(jsonResource);
        assertEquals("demo", jsonResource.getString("package_id"));
        assertEquals("http://example.org/data.csv", jsonResource.getString("url"));
        assertEquals("http://example.org/data.csv", jsonResource.getString("access_url"));
        assertEquals("data.csv", jsonResource.getString("name"));
        assertEquals("CSV", jsonResource.getString("format"));
        assertEquals("http://dcat-ap.de/def/licenses/cc-by/4.0", jsonResource.getString("license"));
        assertEquals("ZIT-SH", jsonResource.getString("licenseAttributionByText"));
    }

    /**
     * Verify that the dataset is added to a collection.
     */
    @Test
    public void testUpload_Collection() throws IOException {
        final Resource dataset = generateDataset();
        dataset.addProperty(DCTerms.isVersionOf, ResourceFactory.createResource("https://opendata.schleswig-holstein.de/dataset/mycollection"));

        final String expectedPackageId = UUID.randomUUID().toString();
        final ArgumentCaptor<JSONObject> argumentPackage = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.when(ckanAPI.createPackage(argumentPackage.capture())).thenReturn(expectedPackageId);

        // invoke method
        dcatUploader.upload(dataset);

        Mockito.verify(ckanAPI).putDatasetInCollection(expectedPackageId, "mycollection");
    }

    /**
     * This dataset has a polygon as spatial extent.
     */
    @Test
    public void testUpload_Polygon() throws IOException {
        final ArgumentCaptor<JSONObject> argumentPackage = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.when(ckanAPI.createPackage(argumentPackage.capture())).thenReturn("demo");

        // invoke method
        dcatUploader.upload(generateDatasetWithGeometry());

        final JSONObject jsonPackage = argumentPackage.getValue();
        assertNotNull(jsonPackage);

        assertEquals(geometryInJSON, getExtrasValue(jsonPackage, "spatial"));
    }
}
