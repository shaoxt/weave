package io.aftersound.weave.service.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aftersound.weave.actor.ActorFactory;
import io.aftersound.weave.jackson.ObjectMapperBuilder;
import io.aftersound.weave.service.ServiceContext;
import io.aftersound.weave.service.message.MessageData;
import io.aftersound.weave.service.message.MessageRegistry;
import io.aftersound.weave.service.metadata.param.Constraint;
import io.aftersound.weave.service.metadata.param.DeriveControl;
import io.aftersound.weave.service.metadata.param.ParamField;
import io.aftersound.weave.service.metadata.param.ParamFields;
import io.aftersound.weave.service.metadata.param.ParamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CoreParameterProcessor extends ParameterProcessor<HttpServletRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreParameterProcessor.class);
    private static final Pattern SLASH_SPLITTER = Pattern.compile("/");
    private static final ObjectMapper MAPPER = ObjectMapperBuilder.forJson().build();

    private static final Deriver NULL_DERIVER = new NullDeriver();
    private final ActorFactory<DeriveControl, Deriver, ParamValueHolder> paramDeriverFactory;

    public CoreParameterProcessor(ActorFactory<DeriveControl, Deriver, ParamValueHolder> paramDeriverFactory) {
        this.paramDeriverFactory = paramDeriverFactory;
    }

    @Override
    protected List<ParamValueHolder> process(HttpServletRequest request, ServiceContext context) {
        Map<String, ParamValueHolder> headerParamValues = extractHeaderParamValues(request, context);
        Map<String, ParamValueHolder> pathParamValues = extractPathParamValues(request, context);
        Map<String, ParamValueHolder> queryParamValues = extractQueryParamValues(request, context);
        Map<String, ParamValueHolder> bodyParamValues = extractBodyParamValues(request, context);

        Map<String, ParamValueHolder> derivedParamValues =
                extractDerivedParamValues(headerParamValues, pathParamValues, queryParamValues, context);

        List<ParamValueHolder> paramValueHolders = new ArrayList<>();
        paramValueHolders.addAll(headerParamValues.values());
        paramValueHolders.addAll(pathParamValues.values());
        paramValueHolders.addAll(queryParamValues.values());
        paramValueHolders.addAll(bodyParamValues.values());
        paramValueHolders.addAll(derivedParamValues.values());
        return paramValueHolders;
    }

    private Map<String, ParamValueHolder> extractHeaderParamValues(HttpServletRequest request, ServiceContext context) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            List<String> values = new ArrayList<>();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                values.add(headerValues.nextElement());
            }

            headers.put(headerName, values);
        }

        // Defined Header Parameters
        ParamFields headerParamFields = paramFields.withParamType(ParamType.Header);

        Map<String, ParamValueHolder> paramValueHolders = new LinkedHashMap<>();
        for (ParamField paramField : headerParamFields.all()) {
            ParamValueHolder paramValueHolder = extractAndParseParamValue(paramField.getName(), paramField, headers, context);
            if (paramValueHolder != null) {
                paramValueHolders.put(paramValueHolder.getParamName(), paramValueHolder);
            }
        }

        return paramValueHolders;
    }

    private Map<String, ParamValueHolder> extractPathParamValues(HttpServletRequest request, ServiceContext context) {
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/")) {
            requestURI = requestURI.substring(1);
        }
        String[] path = SLASH_SPLITTER.split(requestURI);

        ParamFields orderedPathParamFields = paramFields.withParamType(ParamType.Path);
        List<String> orderedPathParamNames = new ArrayList<>(orderedPathParamFields.paramNames());

        if (path.length != orderedPathParamFields.all().size()) {
            String pathParams = String.join("/", orderedPathParamNames);
            context.getMessages().addMessage(MessageRegistry.RESOURCE_PATH_MISMATCH.error(pathParams, request.getRequestURI()));
            return Collections.emptyMap();
        }

        Map<String, List<String>> pathParameters = new LinkedHashMap<>();
        for (int index = 0; index < orderedPathParamNames.size(); index++) {
            String pathParamName = orderedPathParamNames.get(index);
            List<String> pathParamValues = Collections.singletonList(path[index]);
            pathParameters.put(pathParamName, pathParamValues);
        }

        Map<String, ParamValueHolder> paramValueHolders = new LinkedHashMap<>(orderedPathParamNames.size());
        for (ParamField paramField : orderedPathParamFields.all()) {
            ParamValueHolder paramValueHolder = extractAndParseParamValue(paramField.getName(), paramField, pathParameters, context);
            if (paramValueHolder != null) {
                paramValueHolders.put(paramValueHolder.getParamName(), paramValueHolder);
            }
        }

        return paramValueHolders;
    }

    private Map<String, ParamValueHolder> extractQueryParamValues(HttpServletRequest request, ServiceContext context) {
        Map<String, String[]> paramMap = request.getParameterMap();
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            parameters.put(paramName, Arrays.asList(paramValues));
        }

        // Defined Query Parameters
        ParamFields queryParamFields = paramFields.withParamType(ParamType.Query);

        ParamFields namedParamFields = queryParamFields.named();
        ParamField dynamicParamField = queryParamFields.unnamed();

        Map<String, ParamValueHolder> paramValueHolders = new LinkedHashMap<>();

        // process named query parameters
        for (ParamField paramField : namedParamFields.all()) {
            ParamValueHolder paramValueHolder = extractAndParseParamValue(paramField.getName(), paramField, parameters, context);
            if (paramValueHolder != null) {
                paramValueHolders.put(paramValueHolder.getParamName(), paramValueHolder);
            }
        }

        // process unnamed query parameters
        if (dynamicParamField != null) {
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                if (!namedParamFields.contains(entry.getKey())) {
                    ParamValueHolder paramValueHolder = extractAndParseParamValue(entry.getKey(), dynamicParamField, parameters, context);
                    if (paramValueHolder != null) {
                        paramValueHolders.put(paramValueHolder.getParamName(), paramValueHolder);
                    }
                }
            }
        }

        return paramValueHolders;
    }

    private Map<String, ParamValueHolder> extractBodyParamValues(HttpServletRequest request, ServiceContext context) {
        Map<String, ParamValueHolder> paramValueHolders = new HashMap<>();
        ParamField paramField = paramFields.firstIfExists(ParamType.Body);
        if (paramField != null) {
            try {
                String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                if (body != null && body.length() > 0) {
                    Object obj;
                    if (JsonNode.class.getName().equals(paramField.getName())) {
                        obj = MAPPER.readTree(body);
                    } else {
                        Class<?> type = Class.forName(paramField.getValueType());
                        obj = MAPPER.readValue(body, type);
                    }

                    paramValueHolders.put(
                            paramField.getName(),
                            ParamValueHolder.singleValuedScoped(
                                    ParamType.Body.name(),
                                    paramField.getName(),
                                    paramField.getValueType(),
                                    obj
                            )
                    );
                } else {
                    context.getMessages().addMessage(
                            MessageRegistry.MISSING_REQUIRED_PARAMETER.error(paramField.getName(), paramField.getType().name())
                    );
                }
            } catch (Exception e) {
                context.getMessages().addMessage(unableToReadRequestBodyError());
            }
        }
        return paramValueHolders;
    }

    private Map<String, ParamValueHolder> extractDerivedParamValues(
            Map<String, ParamValueHolder> headerParamValues,
            Map<String, ParamValueHolder> pathParamValues,
            Map<String, ParamValueHolder> queryParamValues,
            ServiceContext context) {

        // Defined Derived Parameters
        ParamFields derivedParamFields = paramFields.withParamType(ParamType.Derived);

        Map<String, ParamValueHolder> queryParamValueHolders = new LinkedHashMap<>();

        for (ParamField paramField : derivedParamFields.all()) {
            String originalKey = paramField.getDeriveControl().getFrom();
            if (!headerParamValues.containsKey(originalKey) &&
                !pathParamValues.containsKey(originalKey) &&
                !queryParamValues.containsKey(originalKey)) {
                continue;
            }

            // should only have 1 source to derive the final value from
            ParamValueHolder orginalParamValueHolder = queryParamValues.get(originalKey);

            if (orginalParamValueHolder == null) {
                orginalParamValueHolder = pathParamValues.get(originalKey);
            }

            if (orginalParamValueHolder == null) {
                orginalParamValueHolder = headerParamValues.get(originalKey);
            }

            if (orginalParamValueHolder == null) {
                continue;
            }

            List<String> derivedValues = derive(paramField, orginalParamValueHolder);
            if (derivedValues == null || derivedValues.isEmpty()) {
                continue;
            }

            Map<String, List<String>> derivedValueMap = new HashMap<>();
            derivedValueMap.put(paramField.getName(), derivedValues);
            ParamValueHolder paramValueHolder = extractAndParseParamValue(paramField.getName(), paramField, derivedValueMap, context);
            if (paramValueHolder != null) {
                queryParamValueHolders.put(paramValueHolder.getParamName(), paramValueHolder);
            }
        }

        return queryParamValueHolders;
    }

    private List<String> derive(ParamField paramField, ParamValueHolder sourceValueHolder) {
        DeriveControl deriveControl = paramField.getDeriveControl();
        try {
            Deriver deriver = paramDeriverFactory.createActor(deriveControl.getType(), NULL_DERIVER);
            return deriver.derive(deriveControl, sourceValueHolder);
        } catch (Exception e) {
            LOGGER.error(
                    "{} : exception occurred when trying to derive {} from {}",
                    e,
                    paramField.getName(),
                    sourceValueHolder.getParamName()
            );
            return Collections.emptyList();
        }
    }

    private static ParamValueHolder extractAndParseParamValue(
            String paramName,
            ParamField paramField,
            Map<String, List<String>> parameters,
            ServiceContext context) {
        Constraint.Type constraintType = paramField.getConstraint().getType();

        // ParamField without name is considered optional
        if (paramField.getName() == null) {
            constraintType = Constraint.Type.Optional;
        }
        switch (constraintType) {
            case Predefined:
                return extractAndParsePredefinedParamValue(paramName, paramField, context);
            case Required:
                return extractAndParseRequiredParamValue(paramName, paramField, parameters, context);
            case SoftRequired:
                return extractAndParseSoftRequiredParamValue(paramName, paramField, parameters, context);
            case Optional:
                return extractAndParseOptionalParamValue(paramName, paramField, parameters, context);
            default:
                return null;
        }
    }

    private static ParamValueHolder extractAndParsePredefinedParamValue(
            String paramName,
            ParamField paramField,
            ServiceContext context) {
        if (paramField.getDefaultValue() == null) {
            context.getMessages().addMessage(predefinedParamMissingValueError(paramField));
            return null;
        }

        List<String> rawValues = new ArrayList<>();
        if (paramField.getValueDelimiter() != null) {
            String[] splitted = paramField.getDefaultValue().split(paramField.getValueDelimiter());
            rawValues.addAll(Arrays.asList(splitted));
        } else {
            rawValues.add(paramField.getDefaultValue());
        }
        ParamValueHolder valueHolder = ParamValueParser.parse(paramField, paramName, rawValues);
        if (valueHolder.getValue() == null) {
            context.getMessages().addMessage(invalidParamValueError(paramField, Collections.singletonList(paramField.getDefaultValue())));
            return null;
        }
        return valueHolder;
    }

    private static ParamValueHolder extractAndParseRequiredParamValue(
            String paramName,
            ParamField paramField,
            Map<String, List<String>> parameters,
            ServiceContext context) {
        List<String> rawValues = extractRawValues(paramName, paramField, parameters);
        ParamValueHolder valueHolder = ParamValueParser.parse(paramField, paramName, rawValues);
        if (valueHolder == null) {
            context.getMessages().addMessage(missingRequiredParamError(paramField));
            return null;
        } else {
            if (valueHolder.getValue() == null) {
                context.getMessages().addMessage(invalidParamValueError(paramField, parameters.get(paramField.getName())));
                return null;
            }
        }

        return valueHolder;
    }

    private static ParamValueHolder extractAndParseSoftRequiredParamValue(
            String paramName,
            ParamField paramField,
            Map<String, List<String>> parameters,
            ServiceContext context) {
        List<String> rawValues = extractRawValues(paramName, paramField, parameters);
        ParamValueHolder valueHolder = ParamValueParser.parse(paramField, paramName, rawValues);
        if (valueHolder != null && valueHolder.getValue() != null) {
            return valueHolder;
        }
        // else validation is needed
        Constraint.When requiredWhen = paramField.getConstraint().getRequiredWhen();
        // required when is undefined, fine
        if (requiredWhen == null) {
            return valueHolder;
        }

        String[] otherParamNames = requiredWhen.getOtherParamNames();
        // does not involve other parameters, as if it is Optional
        if (otherParamNames == null || otherParamNames.length == 0) {
            return valueHolder;
        }

        Constraint.Condition condition = requiredWhen.getCondition();
        // condition is not specified, as if it's Optional
        if (condition == null) {
            return valueHolder;
        }

        // all of other parameters present
        if (condition == Constraint.Condition.AllOtherExist) {
            boolean allExist = true;
            for (String otherParamName : otherParamNames) {
                if (parameters.get(otherParamName) == null || parameters.get(otherParamName).isEmpty()) {
                    allExist = false;
                    break;
                }
            }
            if (allExist) {
                context.getMessages().addMessage(
                        MessageRegistry.MISSING_SOFT_REQUIRED_PARAMETER_ALL_OTHER_EXIST.error(
                                paramField.getName(),
                                paramField.getValueType(),
                                String.join("|", otherParamNames)
                        )
                );
            }
        }

        // any of other parameters presents
        if (condition == Constraint.Condition.AnyOtherExists) {
            boolean anyExists = false;
            for (String otherParamName : otherParamNames) {
                if (parameters.get(otherParamName) != null && !parameters.get(otherParamName).isEmpty()) {
                    anyExists = true;
                    break;
                }
            }
            if (anyExists) {
                context.getMessages().addMessage(
                        MessageRegistry.MISSING_SOFT_REQUIRED_PARAMETER_ANY_OTHER_EXISTS.error(
                                paramField.getName(),
                                paramField.getValueType(),
                                String.join("|", otherParamNames)
                        )
                );
            }
        }

        // all of other parameters don't present
        if (condition == Constraint.Condition.AllOtherNotExist) {
            boolean allNotExist = true;
            for (String otherParamName : otherParamNames) {
                if (parameters.get(otherParamName) != null && !parameters.get(otherParamName).isEmpty()) {
                    allNotExist = false;
                    break;
                }
            }
            if (allNotExist) {
                context.getMessages().addMessage(
                        MessageRegistry.MISSING_SOFT_REQUIRED_PARAMETER_ALL_OTHER_NOT_EXIST.error(
                                paramField.getName(),
                                paramField.getValueType(),
                                String.join("|", otherParamNames)
                        )
                );
            }
        }

        // any of other parameters doesn't present
        if (condition == Constraint.Condition.AnyOtherNotExist) {
            boolean anyNotExist = false;
            for (String otherParamName : otherParamNames) {
                if (parameters.get(otherParamName) == null || parameters.get(otherParamName).isEmpty()) {
                    anyNotExist = true;
                    break;
                }
            }
            if (anyNotExist) {
                context.getMessages().addMessage(
                        MessageRegistry.MISSING_SOFT_REQUIRED_PARAMETER_ANY_OTHER_NOT_EXIST.error(
                                paramField.getName(),
                                paramField.getValueType(),
                                String.join("|", otherParamNames)
                        )
                );
            }
        }

        return valueHolder;
    }

    private static ParamValueHolder extractAndParseOptionalParamValue(
            String paramName,
            ParamField paramField,
            Map<String, List<String>> parameters,
            ServiceContext context) {
        List<String> rawValues = extractRawValues(paramName, paramField, parameters);

        if ((rawValues == null || rawValues.isEmpty()) && paramField.getDefaultValue() != null) {
            rawValues = new ArrayList<>();
            if (paramField.getValueDelimiter() != null) {
                String[] splitted = paramField.getDefaultValue().split(paramField.getValueDelimiter());
                rawValues.addAll(Arrays.asList(splitted));
            } else {
                rawValues.add(paramField.getDefaultValue());
            }
        }

        ParamValueHolder valueHolder = ParamValueParser.parse(paramField, paramName, rawValues);
        if (valueHolder == null) {
            return null;
        }
        if (valueHolder.getValue() == null) {
            context.getMessages().addMessage(invalidParamValueWarning(paramField, parameters.get(paramField.getName())));
            return null;
        }
        return valueHolder;
    }

    private static List<String> extractRawValues(String paramName, ParamField paramField, Map<String, List<String>> parameters) {
        List<String> rawValues = parameters.get(paramName);
        if (rawValues == null || rawValues.isEmpty()) {
            return rawValues;
        }

        if (paramField.getValueDelimiter() == null) {
            return rawValues;
        }

        List<String> expanded = new ArrayList<>();
        for (String rawValue : rawValues) {
            String[] splitted = rawValue.split(paramField.getValueDelimiter());
            expanded.addAll(Arrays.asList(splitted));
        }
        return expanded;
    }

    private static MessageData unableToReadRequestBodyError() {
        return MessageRegistry.UNABLE_TO_READ_REQUEST_BODY.error();
    }

    private static MessageData predefinedParamMissingValueError(ParamField paramMetadata) {
        return MessageRegistry.PREDEFINED_PARAMETER_MISSING_VALUE.error(paramMetadata.getName(), paramMetadata.getValueType());
    }

    private static MessageData missingRequiredParamError(ParamField paramMetadata) {
        return MessageRegistry.MISSING_REQUIRED_PARAMETER.error(paramMetadata.getName(), paramMetadata.getValueType());
    }

    private static MessageData invalidParamValueError(ParamField paramMetadata, List<String> values) {
        return MessageRegistry.INVALID_PARAMETER_VALUE.error(
                paramMetadata.getName(),
                paramMetadata.getValueType(),
                String.join("|", values));
    }

    private static MessageData invalidParamValueWarning(ParamField paramMetadata, List<String> values) {
        return MessageRegistry.INVALID_PARAMETER_VALUE.warning(
                paramMetadata.getName(),
                paramMetadata.getValueType(),
                String.join("|", values));
    }
}
