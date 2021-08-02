package de.landsh.opendata.ckan;

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
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DcatUploaderTest {

    private static final String geometryInJSON = "{\"type\":\"Polygon\",\"coordinates\":[[[10.753384,54.033113],[10.754036,54.033269],[10.750559,54.039196],[10.75143,54.039301],[10.753384,54.033113]]]}";
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

    /**
     * Liest eine RDF-Resource aus einer Datei im resources/ Verzeichnis ein.
     */
    private org.apache.jena.rdf.model.Resource loadDataset(String resourceName) {
        final Model model = ModelFactory.createDefaultModel();
        model.read(getClass().getResourceAsStream(resourceName), "https://opendata.schleswig-holstein.de/");
        return model.listSubjectsWithProperty(RDF.type, DCAT.Dataset).nextResource();
    }


    @Test
    public void testUpload() throws IOException {
        final ArgumentCaptor<JSONObject> argumentPackage = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.when(ckanAPI.createPackage(argumentPackage.capture())).thenReturn("demo");

        final ArgumentCaptor<JSONObject> argumentResource = ArgumentCaptor.forClass(JSONObject.class);
        Mockito.when(ckanAPI.createResource(argumentResource.capture())).thenReturn("resource");

        // invoke method
        final String packageId = dcatUploader.upload(loadDataset("/dataset.xml"));

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
        assertEquals("17f9aec62e8398c358b3d3a2deaef2a5", jsonResource.getString("hash"));
        assertEquals("http://dcat-ap.de/def/hashAlgorithms/md/5", jsonResource.getString("hash_algorithm"));

    }

    /**
     * Verify that the dataset is added to a collection.
     */
    @Test
    public void testUpload_Collection() throws IOException {
        final Resource dataset = loadDataset("/dataset.xml");
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
        dcatUploader.upload(loadDataset("/dataset-with-geometry.xml"));

        final JSONObject jsonPackage = argumentPackage.getValue();
        assertNotNull(jsonPackage);

        assertEquals(geometryInJSON, getExtrasValue(jsonPackage, "spatial"));
    }
}
