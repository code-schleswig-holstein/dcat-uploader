package de.landsh.opendata.ckan;

import lombok.Data;

@Data
public class Resource {
    String id;
    String accessURL;
    String name;
    String checksum;
    long byteSize;
    String format;
    String mimeType;
}
