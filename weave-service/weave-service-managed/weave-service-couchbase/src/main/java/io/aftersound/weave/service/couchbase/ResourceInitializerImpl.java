package io.aftersound.weave.service.couchbase;

import io.aftersound.weave.config.Config;
import io.aftersound.weave.resources.ManagedResources;
import io.aftersound.weave.resources.ResourceInitializer;
import io.aftersound.weave.resources.ResourceType;

public class ResourceInitializerImpl implements ResourceInitializer {

    @Override
    public ResourceType<?>[] getDependingResourceTypes() {
        return new ResourceType[] {
                ResourceTypes.DATA_CLIENT_REGISTRY,
                ResourceTypes.DATA_FORMAT_FACTORY
        };
    }

    @Override
    public ResourceType<?>[] getShareableResourceTypes() {
        return new ResourceType[0];
    }

    @Override
    public ResourceType<?>[] getResourceTypes() {
        return new ResourceType[0];
    }

    @Override
    public void initializeResources(ManagedResources managedResources, Config resourceConfig) {
    }
}
