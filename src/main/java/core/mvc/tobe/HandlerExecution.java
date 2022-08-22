package core.mvc.tobe;

import core.annotation.web.RequestMapping;
import core.mvc.ModelAndView;
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
        List<Object> parameters = extractParameters(request, response);
        if (parameters == null || parameters.size() < 1) {
            return getModelAndView(method.invoke(declaredObject));
        }
        return getModelAndView(method.invoke(declaredObject, parameters.toArray()));
    }

    private List<Object> extractParameters(HttpServletRequest request, HttpServletResponse response) {
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
            Class<?> parameterType = parameterTypes[i];

            String parameter = request.getParameter(parameterName);
            if (parameter != null) {
                result.add(ParameterTypeEnum.casting(parameter, parameterType));
                continue;
            }

            String pathVariable = pathVariables.keySet().stream().filter(key -> key.equals(parameterName))
                    .map(pathVariables::get).findFirst().orElse(null);
            if (pathVariable != null) {
                result.add(ParameterTypeEnum.casting(pathVariable, parameterType));
                continue;
            }

            Object attribute = request.getAttribute(parameterName);
            if (attribute != null) {
                result.add(attribute);
                continue;
            }

            if (parameterType.getSimpleName().equals("HttpServletRequest")) {
                result.add(request);
                continue;
            }

            if (parameterType.getSimpleName().equals("HttpServletResponse")) {
                result.add(response);
            }
        }
        return result;
    }

    private Map<String, String> pathVariables(HttpServletRequest request) {
        RequestMapping requestMappingAnnotation = method.getAnnotation(RequestMapping.class);
        UriPathPattern uriPathPattern = new UriPathPattern(requestMappingAnnotation.value());
        return uriPathPattern.matchAndExtract(request.getRequestURI());
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
