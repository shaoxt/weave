package io.aftersound.weave.couchbase;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import io.aftersound.weave.common.NamedType;
import io.aftersound.weave.dataclient.DataClientFactory;
import io.aftersound.weave.dataclient.DataClientRegistry;
import io.aftersound.weave.dataclient.Endpoint;
import io.aftersound.weave.dataclient.Signature;

import java.util.Map;

/**
 * A {@link DataClientFactory} that creates/destroys Couchbase {@link Cluster} on instruction.
 */
public class CouchbaseClusterFactory extends DataClientFactory<Cluster> {

    public static final NamedType<Endpoint> COMPANION_CONTROL_TYPE = NamedType.of("CouchbaseCluster", Endpoint.class);
    public static final NamedType<Object> COMPANION_PRODUCT_TYPE = NamedType.of("CouchbaseCluster", Cluster.class);

    public CouchbaseClusterFactory(DataClientRegistry dataClientRegistry) {
        super(dataClientRegistry);
    }

    /**
     * Create an object of {@link Cluster} as data client
     * @param options
     *          - options required to establish connection to targeted Couchbase cluster
     * @return an object of {@link Cluster} as data client
     */
    @Override
    protected Cluster createDataClient(Map<String, Object> options) {
        Settings settings = Settings.from(options);
        Cluster cluster = CouchbaseCluster.create(settings.getNodes());
        cluster.authenticate(settings.getUsername(), settings.getPassword());
        return cluster;
    }

    @Override
    protected void destroyDataClient(Cluster cluster) {
        // Note:
        //  disconnect Couchbase cluster will close all open buckets and
        //  this will make Bucket objects in dataClientRegistry unusable
        if (cluster != null) {
            cluster.disconnect();
        }
    }
}
