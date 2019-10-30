package io.aftersound.weave.service.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.BinaryDocument;
import com.couchbase.client.java.document.JsonDocument;
import io.aftersound.weave.couchbase.GetControl;
import io.aftersound.weave.couchbase.Repository;
import io.aftersound.weave.data.DataFormat;
import io.aftersound.weave.data.DataFormatRegistry;
import io.aftersound.weave.data.Deserializer;
import io.aftersound.weave.dataclient.DataClientRegistry;
import io.aftersound.weave.service.ServiceContext;
import io.aftersound.weave.service.request.ParamValueHolders;

import java.util.concurrent.TimeUnit;

class ByKeyExecutor extends Executor {

    @Override
    public Object execute(CouchbaseExecutionControl executionControl, ParamValueHolders request, ServiceContext context) {
        ByKey byKey = executionControl.getByKey();

        String key = render(byKey.getKeyTemplate(), request);
        String schemaId = render(byKey.getSchemaSelector(), request);
        DataSchema dataSchema = byKey.getSchemas().get(schemaId);

        Repository repository = executionControl.getRepository();
        GetControl getControl = repository.getGetControl();

        DataClientRegistry dcr = managedResources.getResource(ResourceTypes.DATA_CLIENT_REGISTRY);
        Bucket bucket = dcr.getClient(repository.getId());

        DataFormatRegistry dataFormatRegistry = managedResources.getResource(ResourceTypes.DATA_FORMAT_FACTORY);

        DataFormat dataFormat = dataFormatRegistry.getDataFormat(dataSchema.getFormat());
        Class<?> type = getType(dataSchema.getSchema());
        Deserializer deserializer = dataFormat.deserializer();

        // TODO: how about simple/native types?
        try {
            if ("JSON".equals(dataSchema.getFormat())) {
                return getAndDeserializeJsonDoc(bucket, key, getControl, deserializer, type);
            } else {
                return getAndDeserializeBinaryDoc(bucket, key, getControl, deserializer, type);
            }
        } catch (Exception e) {
            context.getMessages().addMessage(Messages.GET_AND_DESERIALIZE_EXCEPTION_OCCURRED);
            return null;
        }
    }

    private Class<?> getType(String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Object getAndDeserializeJsonDoc(
            Bucket bucket,
            String key,
            GetControl getControl,
            Deserializer deserializer,
            Class<?> targetType) throws Exception {
        JsonDocument jsonDoc;
        if (getControl == null) {
            jsonDoc = bucket.get(key);
        } else {
            jsonDoc = bucket.get(key, getControl.getTimeout(), TimeUnit.MILLISECONDS);
        }

        return deserializer.fromString(jsonDoc.content().toString(), targetType);
    }

    private Object getAndDeserializeBinaryDoc(
            Bucket bucket,
            String key,
            GetControl getControl,
            Deserializer deserializer,
            Class<?> targetType) throws Exception {
        BinaryDocument binaryDoc;
        BinaryDocument proto = BinaryDocument.create(key);
        if (getControl == null) {
            binaryDoc = bucket.get(proto);
        } else {
            binaryDoc = bucket.get(proto, getControl.getTimeout(), TimeUnit.MILLISECONDS);
        }

        return deserializer.fromBytes(binaryDoc.content().array(), targetType);
    }

}

