package io.aftersound.weave.service.couchbase;

import java.util.Map;

/**
 * ByKey supports
 *  1.
 */
public class ByKey {

    private String keyTemplate;
    private String schemaSelector;

    private Map<String, DataSchema> schemas;

    public String getKeyTemplate() {
        return keyTemplate;
    }

    public void setKeyTemplate(String keyTemplate) {
        this.keyTemplate = keyTemplate;
    }

    public String getSchemaSelector() {
        return schemaSelector;
    }

    public void setSchemaSelector(String schemaSelector) {
        this.schemaSelector = schemaSelector;
    }

    public Map<String, DataSchema> getSchemas() {
        return schemas;
    }

    public void setSchemas(Map<String, DataSchema> schemas) {
        this.schemas = schemas;
    }
}
