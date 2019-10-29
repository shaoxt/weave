package io.aftersound.weave.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TestClusterChecker implements TestRule {

    private final String node;
    private final String cluster;
    private final String user;
    private final String password;
    private final String bucket;

    public TestClusterChecker(
            String node,
            String cluster,
            String user,
            String password,
            String bucket) {
        this.node = node;
        this.cluster = cluster;
        this.user = user;
        this.password = password;
        this.bucket = bucket;
    }

    private boolean connect() {
        try {
            Cluster cluster = CouchbaseCluster.create(node);
            cluster.authenticate(user, password);

            Bucket bkt = cluster.openBucket(bucket);
            if (bkt == null) {
                return false;
            }

            bkt.close();
            cluster.disconnect();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Statement apply(final Statement base, Description description) {

        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                if (!TestClusterChecker.this.connect()) {
                    throw new AssumptionViolatedException("Could not connect. Skipping test!");
                } else {
                    base.evaluate();
                }
            }
        };

    }

}
