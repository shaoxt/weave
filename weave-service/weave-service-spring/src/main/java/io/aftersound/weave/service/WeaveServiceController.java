package io.aftersound.weave.service;

import io.aftersound.weave.actor.ActorFactory;
import io.aftersound.weave.service.metadata.param.DeriveControl;
import io.aftersound.weave.service.request.Deriver;
import io.aftersound.weave.service.request.ParamValueHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Service controller for dynamically realized services
 */
@RestController
public class WeaveServiceController {

    @Autowired
    @Qualifier("serviceMetadataManager")
    ServiceMetadataManager serviceMetadataManager;

    @Autowired
    @Qualifier("serviceExecutorFactory")
    ServiceExecutorFactory serviceExecutorFactory;

    @Autowired
    @Qualifier("paramDeriverFactory")
    ActorFactory<DeriveControl, Deriver, ParamValueHolder> paramDeriverFactory;

    @RequestMapping("**")
    @ResponseBody
    public Object serve(HttpServletRequest request) {
        return new ServiceDelegate(
                serviceMetadataManager,
                serviceExecutorFactory,
                paramDeriverFactory
        ).serve(request);
    }

}
