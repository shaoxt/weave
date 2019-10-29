package io.aftersound.weave.service.couchbase;

import io.aftersound.weave.data.DataFormat;

public class DataSchema {
    private DataFormat format;
    private String schema;

    public DataFormat getFormat() {
        return format;
    }

    public void setFormat(DataFormat format) {
        this.format = format;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}
