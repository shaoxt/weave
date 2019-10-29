package io.aftersound.weave.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import io.aftersound.weave.actor.ActorBindings;
import io.aftersound.weave.common.NamedType;
import io.aftersound.weave.dataclient.DataClientFactory;
import io.aftersound.weave.dataclient.DataClientRegistry;
import io.aftersound.weave.dataclient.Endpoint;
import io.aftersound.weave.utils.OptionsBuilder;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * This unit test needs Couchbase cluster at local. You could easily set up one with docker image
 * at https://hub.docker.com/_/couchbase. Once server is up,
 *  create cluster named as "test"
 *  create bucket named as "test"
 *  create user/password which has all the roles to access bucket "test"
 */
public class CouchbaseClientFactoryTest {

    @ClassRule
    public static TestClusterChecker testClusterChecker = new TestClusterChecker(
            "localhost",    // node
            "test",         // cluster name
            "user",         // bucket user name
            "password",     // bucket password
            "test"          // bucket name
    );

    @Test
    public void createDestroy() throws Exception {
        // 1.prepare bindings object for {Endpoint, DataClientFactory, Object}
        ActorBindings<Endpoint, DataClientFactory<?>, Object> dcfBindings = new ActorBindings<>();
        dcfBindings.register(
                CouchbaseClusterFactory.COMPANION_CONTROL_TYPE,
                CouchbaseClusterFactory.class,
                CouchbaseClusterFactory.COMPANION_PRODUCT_TYPE
        );

        dcfBindings.register(
                CouchbaseBucketFactory.COMPANION_CONTROL_TYPE,
                CouchbaseBucketFactory.class,
                CouchbaseBucketFactory.COMPANION_PRODUCT_TYPE
        );

        // 2.create DataClientRegistry
        DataClientRegistry dcRegistry = new DataClientRegistry(dcfBindings);

        // 3.initialize Cluster object for Couchbase cluster with specified id chosen at client side
        final String clusterId = "cluster123";
        Map<String, Object> clusterOptions = new OptionsBuilder()
                .option("cluster", "test")
                .option("nodes", "localhost")
                .option("username", "user")
                .option("password", "password")
                .build();

        dcRegistry.initializeClient("CouchbaseCluster", clusterId, clusterOptions);

        Cluster cluster = dcRegistry.getClient(clusterId);
        assertNotNull(cluster);

        // 4.initialize Bucket object  with specified id at client side
        Map<String, Object> bucketOptions = new OptionsBuilder()
                .option("clusterId", clusterId)
                .option("bucket", "test")
                .build();

        final String bucketId = "bucket456";
        dcRegistry.initializeClient("CouchbaseBucket", bucketId, bucketOptions);

        Bucket bucket = dcRegistry.getClient(bucketId);
        assertNotNull(bucket);

        JsonObject arthur = JsonObject.create()
                .put("name", "Arthur")
                .put("email", "kingarthur@couchbase.com")
                .put("interests", JsonArray.from("Holy Grail", "African Swallows"));

        // Store the Document
        bucket.upsert(JsonDocument.create("u:king_arthur", arthur));

        // Load the Document and print it
        // Prints Content and Metadata of the stored Document
        assertNotNull(bucket.get("u:king_arthur"));

        assertNotNull(bucket.remove("u:king_arthur"));

        assertNull(bucket.get("u:king_arthur"));
    }

}