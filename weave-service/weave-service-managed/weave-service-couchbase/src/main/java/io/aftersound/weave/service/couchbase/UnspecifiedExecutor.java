package io.aftersound.weave.service.couchbase;

import io.aftersound.weave.service.ServiceContext;
import io.aftersound.weave.service.request.ParamValueHolders;

class UnspecifiedExecutor extends Executor {

    @Override
    public Object execute(CouchbaseExecutionControl executionControl, ParamValueHolders request, ServiceContext context) {
        context.getMessages().addMessage(Messages.EXECUTION_CONTROL_MALFORMED);
        return null;
    }

}
