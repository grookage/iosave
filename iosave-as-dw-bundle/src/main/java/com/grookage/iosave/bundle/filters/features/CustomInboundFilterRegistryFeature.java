package com.grookage.iosave.bundle.filters.features;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.iosave.as.repository.ASRequestRepository;
import com.grookage.iosave.bundle.annotations.CustomInbound;
import com.grookage.iosave.bundle.annotations.RequestBody;
import com.grookage.iosave.bundle.filters.CustomInboundMessageFilter;
import com.grookage.iosave.bundle.interfaces.CustomInboundRequest;
import com.grookage.iosave.core.exception.IOSaveException;
import com.grookage.iosave.core.exception.IOSaveException.ErrorCode;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@Provider
@SuppressWarnings({"rawtypes", "unchecked"})
public class CustomInboundFilterRegistryFeature implements DynamicFeature {

  private final ASRequestRepository repository;
  private final ObjectMapper mapper;

  @Builder
  public CustomInboundFilterRegistryFeature(ASRequestRepository repository, ObjectMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
    val resourceMethod = resourceInfo.getResourceMethod();
    if (isCustomInboundAnnotated(resourceMethod)) {
      Class<?> requestBodyClass = getAnnotatedParameterClass(resourceMethod);
      featureContext.register(
          new CustomInboundMessageFilter(
              repository,
              mapper,
              requestBodyClass,
              resourceMethod.getAnnotation(CustomInbound.class)
          )
      );
      log.debug("Registered Custom Inbound filter on method: {} with request body: {}",
          resourceInfo.getResourceMethod().getName(), requestBodyClass.getName());
    }
  }

  private boolean isCustomInboundAnnotated(Method resourceMethod) {
    val methodAnnotations = Arrays.asList(resourceMethod.getDeclaredAnnotations());
    return methodAnnotations.stream()
        .anyMatch(annotation -> annotation.annotationType().equals(CustomInbound.class));
  }

  private Class<?> getAnnotatedParameterClass(Method method) {
    final var annotations = method.getParameterAnnotations();
    for (int i = 0; i < annotations.length; i++) {
      for (Annotation annotation : annotations[i]) {
        if (annotation instanceof RequestBody
            && CustomInboundRequest.class.isAssignableFrom(method.getParameterTypes()[i])) {
          return method.getParameterTypes()[i];
        }
      }
    }
    throw IOSaveException.error(ErrorCode.INVALID_USE_OF_CUSTOM_INBOUND);
  }
}
