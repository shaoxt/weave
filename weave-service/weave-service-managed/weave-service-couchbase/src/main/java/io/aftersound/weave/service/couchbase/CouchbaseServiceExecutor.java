package io.aftersound.weave.service.couchbase;

import io.aftersound.weave.common.NamedType;
import io.aftersound.weave.service.ServiceContext;
import io.aftersound.weave.service.ServiceExecutor;
import io.aftersound.weave.service.metadata.ExecutionControl;
import io.aftersound.weave.service.metadata.ServiceMetadata;
import io.aftersound.weave.service.request.ParamValueHolders;
import io.aftersound.weave.resources.ManagedResources;
import io.aftersound.weave.resources.ResourceInitializer;

/**
 * A {@link ServiceExecutor} which reads data from target Couchbase cluster
 * specified in {@link CouchbaseExecutionControl}, by
 *  1.get by key
 *  2.query by view query
 *  3.query by N1QL query
 */
public class CouchbaseServiceExecutor extends ServiceExecutor {

    public static final NamedType<ExecutionControl> COMPANION_CONTROL_TYPE = CouchbaseExecutionControl.TYPE;
    public static final ResourceInitializer RESOURCE_INITIALIZER = new ResourceInitializerImpl();

    public CouchbaseServiceExecutor(ManagedResources managedResources) {
        super(managedResources);
    }

    @Override
    public String getType() {
        return COMPANION_CONTROL_TYPE.name();
    }

    @Override
    public Object execute(ServiceMetadata serviceMetadata, ParamValueHolders request, ServiceContext context) {

        if (!validate(serviceMetadata, context)) {
            return null;
        }

        CouchbaseExecutionControl executionControl = serviceMetadata.getExecutionControl();

        Executor executor;
        if (executionControl.getByKey() != null) {
            executor = new ByKeyExecutor();
        } else if (executionControl.getByViewQuery() != null) {
            executor = new ByViewQueryExecutor();
        } else if (executionControl.getByN1QL() != null) {
            executor = new ByN1QLExecutor();
        } else {
            executor = new UnspecifiedExecutor();
        }

        return executor.managedResources(managedResources).execute(executionControl, request, context);
    }

    private boolean validate(ServiceMetadata serviceMetadata, ServiceContext context) {
        if (!(serviceMetadata.getExecutionControl() instanceof CouchbaseExecutionControl)) {
            context.getMessages().addMessage(Messages.EXECUTION_CONTROL_UNEXPECTED);
            return false;
        }

        CouchbaseExecutionControl executionControl = serviceMetadata.getExecutionControl();
        if (executionControl.getRepository() == null) {
            context.getMessages().addMessage(Messages.REPOSITORY_MISSING);
            return false;
        }

        return true;
    }
}
