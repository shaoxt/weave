package io.aftersound.weave.service.couchbase;

import io.aftersound.weave.data.DataFormatRegistry;
import io.aftersound.weave.dataclient.DataClientRegistry;
import io.aftersound.weave.resources.ResourceType;

class ResourceTypes {

    static final ResourceType<DataClientRegistry> DATA_CLIENT_REGISTRY = new ResourceType(
            DataClientRegistry.class.getName(),
            DataClientRegistry.class
    );

    static final ResourceType<DataFormatRegistry> DATA_FORMAT_FACTORY = new ResourceType(
            DataFormatRegistry.class.getName(),
            DataFormatRegistry.class
    );

}
