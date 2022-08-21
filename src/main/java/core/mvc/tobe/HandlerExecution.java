package core.mvc.tobe;

import core.annotation.web.RequestMapping;
import core.mvc.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.*;

public class HandlerExecution implements ExecuteHandler {
    private final Object declaredObject;
    private final Method method;

    public HandlerExecution(Object declaredObject, Method method) {
        this.declaredObject = declaredObject;
        this.method = method;
    }

    @Override
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Object> parameters = parameters(request);
        if (parameters == null) {
            return getModelAndView(method.invoke(declaredObject));
        }
        return getModelAndView(method.invoke(declaredObject, parameters.toArray()));
    }

    private List<Object> parameters(HttpServletRequest request) {
        ParameterNameDiscoverer nameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] parameterNames = nameDiscoverer.getParameterNames(method);
        if (parameterNames == null) {
            return null;
        }

        Map<String, String> pathVariables = pathVariables(request);
        Class<?>[] parameterTypes = method.getParameterTypes();

        List<Object> result = new ArrayList<>();
        for (int i = 0; i < parameterNames.length; i++) {
            String parameterName = parameterNames[i];
            String parameter = request.getParameter(parameterName);
            String pathVariable = pathVariables.keySet().stream().filter(key -> key.equals(parameterName))
                    .map(pathVariables::get).findFirst().orElse(null);
            Object attribute = request.getAttribute(parameterName);

            Class<?> parameterType = parameterTypes[i];
            if (parameter != null || pathVariable != null) {
                result.add(ParameterTypeEnum.casting(parameterValue(parameter, pathVariable), parameterType));
            }

            if (attribute != null) {
                result.add(attribute);
            }
        }
        return result;
    }

    private Map<String, String> pathVariables(HttpServletRequest request) {
        RequestMapping requestMappingAnnotation = method.getAnnotation(RequestMapping.class);
        UriPathPattern uriPathPattern = new UriPathPattern(requestMappingAnnotation.value());
        return uriPathPattern.matchAndExtract(request.getRequestURI());
    }

    private String parameterValue(String parameter, String pathVariable) {
        if (pathVariable != null) {
            return pathVariable;
        }
        return parameter;
    }

    private ModelAndView getModelAndView(Object invokeObject) {
        if (invokeObject instanceof ModelAndView) {
            return (ModelAndView) invokeObject;
        }

        if (invokeObject instanceof String) {
            return new ModelAndView((String) invokeObject);
        }
        return null;
    }
}
