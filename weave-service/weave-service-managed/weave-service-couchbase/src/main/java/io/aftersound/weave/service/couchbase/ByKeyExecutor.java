package io.aftersound.weave.service.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.BinaryDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import io.aftersound.weave.couchbase.GetControl;
import io.aftersound.weave.couchbase.Repository;
import io.aftersound.weave.data.DataFormat;
import io.aftersound.weave.dataclient.DataClientRegistry;
import io.aftersound.weave.jackson.ObjectMapperBuilder;
import io.aftersound.weave.service.ServiceContext;
import io.aftersound.weave.service.request.ParamValueHolders;

import java.util.concurrent.TimeUnit;

class ByKeyExecutor extends Executor {

    private static final ObjectMapper JSON_MAPPER = ObjectMapperBuilder.forJson().build();
    private static final ObjectMapper SMILE_MAPPER = new ObjectMapper(new SmileFactory())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public Object execute(CouchbaseExecutionControl executionControl, ParamValueHolders request, ServiceContext context) {
        ByKey byKey = executionControl.getByKey();

        String key = render(byKey.getKeyTemplate(), request);
        String schemaId = render(byKey.getSchemaSelector(), request);
        DataSchema dataSchema = byKey.getSchemas().get(schemaId);

        Repository repository = executionControl.getRepository();
        GetControl getControl = repository.getGetControl();

        DataClientRegistry dcr = managedResources.getResource(Constants.DATA_CLIENT_REGISTRY_RESOURCE_TYPE);
        Bucket bucket = dcr.getClient(repository.getId());

        // TODO: creating weave-schema-core
        // TODO: how to handle raw type

        if (DataFormat.JSON == dataSchema.getFormat()) {
            JsonDocument jsonDoc;
            if (getControl == null) {
                jsonDoc = bucket.get(key);
            } else {
                jsonDoc = bucket.get(key, getControl.getTimeout(), TimeUnit.MILLISECONDS);
            }

            try {
                Class<?> type = Class.forName(dataSchema.getSchema());
                return JSON_MAPPER.readValue(jsonDoc.content().toString(), type);
            } catch (Exception e) {
                // TODO
                //context.getMessages().addMessage();
                return null;
            }
        }

        if (DataFormat.Smile == dataSchema.getFormat()) {
            BinaryDocument binaryDoc;

            BinaryDocument proto = BinaryDocument.create(key);
            if (getControl == null) {
                binaryDoc = bucket.get(proto);
            } else {
                binaryDoc = bucket.get(proto, getControl.getTimeout(), TimeUnit.MILLISECONDS);
            }

            try {
                Class<?> type = Class.forName(dataSchema.getSchema());
                return SMILE_MAPPER.readValue(binaryDoc.content().array(), type);
            } catch (Exception e) {
                // TODO
                //context.getMessages().addMessage();
                return null;
            }
        }

        // TODO:
        // context.getMessages().addMessage(error);
        return null;
    }

}

