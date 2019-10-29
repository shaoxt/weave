package io.aftersound.weave.cassandra;

import com.datastax.oss.driver.api.core.session.Session;
import io.aftersound.weave.dataclient.DataClientFactory;
import io.aftersound.weave.dataclient.DataClientRegistry;

import java.util.Map;

public class CassandraSessionFactory extends DataClientFactory<Session> {

    public CassandraSessionFactory(DataClientRegistry dataClientRegistry) {
        super(dataClientRegistry);
    }

    @Override
    protected Session createDataClient(Map<String, Object> options) {
        return null;
    }

    @Override
    protected void destroyDataClient(Session session) {

    }
}
