# dcat-upload

Ein Hilfsprogramm, mit dem man aus Java-Programmen heraus DCAT-AP.de konforme Metadaten in CKAN hochladen kann.


## Abhängigkeit

```xml
<dependency>
    <groupId>de.landsh.opendata</groupId>
    <artifactId>dcat-uploader</artifactId>
    <version>1.0</version>
</dependency>
```

## Nutzung

Es gibt ein low-level CKAN-API `de.landsh.opendata.ckan.CkanAPI` für die grundlegende Kommunikation mit CKAN. Hier wird auf Basis der CKAN-Konzepten *Package* und *Resource* gearbeitet, die in Java als `org.json.JSONObject` sichtbar werden. Für den Zugriff benötigt man die Basisadresse der des Portals und sein API-Key, den man in seinem CKAN-Profil einsehen kann, z.B. unter https://opendata-stage.schleswig-holstein.de/user/

```java
CkanAPI ckanAPI = new CkanAPI("https://opendata-stage.schleswig-holstein.de", new ApiKey("5f6c2da3-dfdc-4c88-a129-22acb129526b"));
JSONObject dataset = readDataset( "schulen-2021-02-01");
```

Der `de.landsh.opendata.ckan.DcatUploader` arbeitet hingegen mit dem konzeptuellen Modell von DCAT-AP.de. Relevante Klassen sind *Dataset* und *Distribution*, die in Java als `org.apache.jena.rdf.model.Resource` sichtbar werden. Ein `DcatUpload` benötigt ein `CkanAPI` (siehe oben) für die CKAN-Instanz, mit der kommuniziert werden soll.

Die Methode `upload` nimmt eine vollständig gefüllte RDF-Resources eines *Dataset* und legt diese samt *Distributionen* und *Collections* in CKAN an. Dateien von *Distributionen* werden derzeit noch nicht nach CKAN hochgeladen.

```java
CkanAPI ckanAPI = new CkanAPI("https://opendata-stage.schleswig-holstein.de", new ApiKey("5f6c2da3-dfdc-4c88-a129-22acb129526b"));
DcatUploader uploader = new DcatUploader(ckanAPI);
Resourse dataset = ...
uploader.upload(dataset);
```
