package de.landsh.opendata.ckan;

import de.landsh.opendata.ckan.ApiKey;
import de.landsh.opendata.ckan.CkanAPI;
import de.landsh.opendata.ckan.Resource;
import de.landsh.opendata.ckan.RestClient;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class CkanApiTest {

    private final ApiKey apiKey = new ApiKey("dummy");
    private final RestClient restClient = Mockito.mock(RestClient.class);
    private CkanAPI ckanAPI;

    private JSONObject datasetNotFound;

    @Before
    public void setUp() {
        ckanAPI = new CkanAPI("http://localhost", apiKey);
        ckanAPI.setRestClient(restClient);

        datasetNotFound = new JSONObject();
        datasetNotFound.put("success", false);
        datasetNotFound.put("error", new JSONObject());
        datasetNotFound.getJSONObject("error").put("message", "Nicht gefunden");
    }

    @Test
    public void readDataset() throws Exception {
        JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__kindertagesstatten1.json"), StandardCharsets.UTF_8));

        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeHttpRequest(argument.capture())).thenReturn(json);

        final JSONObject dataset = ckanAPI.readDataset("kindertagesstatten1");
        Assert.assertNotNull(dataset);

        Assert.assertEquals(new URI("http://localhost/api/3/action/package_show?id=kindertagesstatten1"), argument.getValue().getURI());
    }

    @Test
    public void getCollection() throws Exception {
        JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__kindertagesstatten1.json"), StandardCharsets.UTF_8));

        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeHttpRequest(argument.capture())).thenReturn(json);

        String result = ckanAPI.getCollection("kindertagesstatten1");
        Assert.assertEquals("ed667223-6205-43f6-a2da-0acba4d53ddd", result);
    }

    @Test
    public void getCollection2() throws Exception {
        JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__dataset_in_collection.json"), StandardCharsets.UTF_8));

        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeHttpRequest(argument.capture())).thenReturn(json);

        String result = ckanAPI.getCollection("badegewasser-infrastruktur1");
        Assert.assertEquals("6f30a595-9210-4f24-8873-b52c72401468", result);
    }

    /**
     * It is possible the the "object_package_id" field is null.
     */
    @Test
    public void getCollectionNull() throws Exception {
        JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__relationship_null.json"), StandardCharsets.UTF_8));

        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeHttpRequest(argument.capture())).thenReturn(json);

        Assert.assertNull( ckanAPI.getCollection("geschaftsverteilungsplan-melund-stand-15-07-2020"));
    }



    @Test
    public void getOrganization() throws Exception {
        JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__kindertagesstatten1.json"), StandardCharsets.UTF_8));

        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeHttpRequest(argument.capture())).thenReturn(json);

        String result = ckanAPI.getOrganization("kindertagesstatten1");
        Assert.assertEquals("f2d024c8-dbcc-4786-837e-d4eca1a23a57", result);
    }

    @Test
    public void getAccessURL() throws IOException {
        JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__kindertagesstatten1.json"), StandardCharsets.UTF_8));

        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeHttpRequest(argument.capture())).thenReturn(json);

        String result = ckanAPI.getAccessURL("kindertagesstatten1");
        Assert.assertEquals("http://185.223.104.6/data/sozmin/kita_2019-09-18.csv", result);
    }

    @Test
    public void findNewestDataset() throws Exception {
        CloseableHttpResponse mockResponse = Mockito.mock(CloseableHttpResponse.class);
        Mockito.when(mockResponse.getFirstHeader("Location")).thenReturn(new BasicHeader("Location", "http://opendata.sh/dataset/mydata"));

        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeRawHttpRequest(argument.capture())).thenReturn(mockResponse);

        String result = ckanAPI.findNewestDataset("mycollection");
        Assert.assertEquals("mydata", result);
        Assert.assertEquals(new URI("http://localhost/collection/mycollection/aktuell"), argument.getValue().getURI());
    }

    @Test
    public void getResource() throws Exception {
        final JSONObject dataset = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__kindertagesstatten1.json"), StandardCharsets.UTF_8));

        final Resource result = ckanAPI.getResource(dataset, true);
        Assert.assertNotNull(result);
        Assert.assertEquals("kita.csv", result.getName());
        Assert.assertEquals("96948a3b-b1ca-407c-a33a-60a9ebc49c78", result.getId());
        Assert.assertEquals("CSV", result.getFormat());
        Assert.assertEquals("http://185.223.104.6/data/sozmin/kita_2019-09-18.csv", result.getAccessURL());
        Assert.assertEquals(300618, result.getByteSize());
        Assert.assertEquals("text/csv", result.getMimeType());
        Assert.assertNull(result.getChecksum());
    }

    @Test
    public void getResource_2() throws IOException {
        final JSONObject dataset = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__badegewasser-stammdaten1.json"), StandardCharsets.UTF_8));
        final Resource result = ckanAPI.getResource(dataset, true);
        Assert.assertNotNull(result);
    }

    @Test
    public void getResource_noResource() throws Exception {
        final JSONObject dataset = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__kindertagesstaetten.json"), StandardCharsets.UTF_8));
        final Resource result = ckanAPI.getResource(dataset, true);

        Assert.assertNull(result);
    }

    @Test
    public void putDatasetInCollection_missingCollection() throws IOException, URISyntaxException {
        JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__kindertagesstatten1.json"), StandardCharsets.UTF_8));

        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeHttpRequest(argument.capture())).thenReturn(json, datasetNotFound);

        try {
            ckanAPI.putDatasetInCollection("mydataset", "mycollection");
            Assert.fail();
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("mycollection"));
        }

        Assert.assertEquals(new URI("http://localhost/api/3/action/package_show?id=mydataset"), argument.getAllValues().get(0).getURI());
        Assert.assertEquals(new URI("http://localhost/api/3/action/package_show?id=mycollection"), argument.getAllValues().get(1).getURI());
    }

    @Test
    public void putDatasetInCollection_missingDataset() throws IOException, URISyntaxException {
        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeHttpRequest(argument.capture())).thenReturn(datasetNotFound);

        try {
            ckanAPI.putDatasetInCollection("mydataset", "mycollection");
            Assert.fail();
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("mydataset"));
        }

        Assert.assertEquals(new URI("http://localhost/api/3/action/package_show?id=mydataset"), argument.getAllValues().get(0).getURI());
    }

    /**
     * The specified dataset id does not belong to a collection.
     */
    @Test
    public void putDatasetInCollection_noCollection() throws IOException, URISyntaxException {
        JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/package_show__kindertagesstatten1.json"), StandardCharsets.UTF_8));

        final ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.when(restClient.executeHttpRequest(argument.capture())).thenReturn(json, json);

        try {
            ckanAPI.putDatasetInCollection("mydataset", "mycollection");
            Assert.fail();
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("mycollection"));
            Assert.assertTrue(expected.getMessage().contains("is no collection"));
        }

        Assert.assertEquals(new URI("http://localhost/api/3/action/package_show?id=mydataset"), argument.getAllValues().get(0).getURI());
        Assert.assertEquals(new URI("http://localhost/api/3/action/package_show?id=mycollection"), argument.getAllValues().get(1).getURI());
    }

}
