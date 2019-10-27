package io.aftersound.weave.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import io.aftersound.weave.common.NamedType;
import io.aftersound.weave.dataclient.DataClientFactory;
import io.aftersound.weave.dataclient.DataClientRegistry;
import io.aftersound.weave.dataclient.Endpoint;

import java.util.Map;

/**
 * A {@link DataClientFactory} that creates/destroys Couchbase {@link Bucket} on instruction.
 * It depends on {@link Cluster} created and registered by {@link CouchbaseClusterFactory} to
 * create objects of {@link Bucket}.
 */
public class CouchbaseBucketFactory extends DataClientFactory<Bucket> {

    public static final NamedType<Endpoint> COMPANION_CONTROL_TYPE = NamedType.of("CouchbaseBucket", Endpoint.class);
    public static final NamedType<Object> COMPANION_PRODUCT_TYPE = NamedType.of("CouchbaseBucket", Bucket.class);

    public CouchbaseBucketFactory(DataClientRegistry dataClientRegistry) {
        super(dataClientRegistry);
    }

    /**
     * Create an object of {@link Bucket} as data client by using {@link Cluster} instance
     * with specified id.
     * @param options
     *          - options required to create {@link Bucket}
     * @return an instance of {@link Bucket} as data client
     */
    @Override
    protected Bucket createDataClient(Map<String, Object> options) {
        Settings settings = Settings.from(options);
        Cluster cluster = dataClientRegistry.getClient(settings.getClusterId());
        return cluster.openBucket(settings.getBucket());
    }

    @Override
    protected void destroyDataClient(Bucket bucket) {
        if (bucket != null) {
            bucket.close();
        }
    }
}
