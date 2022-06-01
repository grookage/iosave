package com.grookage.iosave.bundle.filters;

import static com.grookage.iosave.bundle.utils.JerseyInboundMessageFilterUtils.extractResponseHeaders;
import static com.grookage.iosave.bundle.utils.JerseyInboundMessageFilterUtils.getRequestBody;
import static com.grookage.iosave.bundle.utils.JerseyInboundMessageFilterUtils.isLoggingEnabled;
import static com.grookage.iosave.bundle.utils.JerseyInboundMessageFilterUtils.readFromResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.iosave.as.repository.ASRequestRepository;
import com.grookage.iosave.bundle.annotations.CustomInbound;
import com.grookage.iosave.bundle.interfaces.CustomInboundRequest;
import com.grookage.iosave.core.entities.RequestEntity;
import com.grookage.iosave.core.exception.IOSaveException;
import com.grookage.iosave.core.filters.BaseInboundRequestFilter;
import com.grookage.iosave.core.models.RequestMeta;
import com.grookage.iosave.core.models.ResponseMeta;
import com.grookage.iosave.core.services.RequestReceiverService;
import com.grookage.iosave.core.utils.RequestManager;
import com.grookage.iosave.core.utils.RequestUtils;
import java.util.HashMap;
import java.util.UUID;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import lombok.Builder;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class CustomInboundMessageFilter<T extends CustomInboundRequest>
    extends BaseInboundRequestFilter
    implements ContainerRequestFilter, ContainerResponseFilter {

  private final Class<T> requestBodyClass;
  private final CustomInbound customInbound;

  @Builder
  public CustomInboundMessageFilter(
      final ASRequestRepository messageRepository,
      final ObjectMapper mapper,
      Class<T> requestBodyClass,
      CustomInbound customInbound) {
    super(new RequestReceiverService(messageRepository), mapper);
    this.requestBodyClass = requestBodyClass;
    this.customInbound = customInbound;
  }


  @Override
  public void filter(ContainerRequestContext requestContext) {
    final var requestBody = getRequestBody(requestContext, null);
    final var requestId = extractRequestIdFromRequestBody(requestBody);
    final var loggingEnabled = isLoggingEnabled(requestContext);
    if (RequestUtils.loggingEnabled(loggingEnabled, requestId)) {
      validateEmbeddedRequestId(requestId);
      super.requestFilter(
          RequestMeta.builder()
              .requestId(requestId)
              .traceId(getTraceId(requestContext))
              .requestBody(getRequestBody(requestContext, null))
              .build(),
          shouldSaveBody()
      );
    }
  }


  @Override
  public void filter(ContainerRequestContext requestContext,
      ContainerResponseContext responseContext) {

    final var inboundMessage = RequestManager.getCurrentMessage();
    final var requestBody = getRequestBody(requestContext, inboundMessage);
    final var requestId = extractRequestIdFromRequestBody(requestBody);
    final var loggingEnabled = isLoggingEnabled(requestContext);
    if (RequestUtils.loggingEnabled(loggingEnabled, requestId)) {
      super.responseFilter(
          RequestMeta.builder()
              .requestId(requestId)
              .traceId(getTraceId(requestContext))
              .requestBody(getRequestBody(requestContext, RequestManager.getCurrentMessage()))
              .build(),
          ResponseMeta.builder()
              .responseStatus(responseContext.getStatus())
              .responseHeaders(extractResponseHeaders(responseContext))
              .responseBody(readFromResponse(responseContext, getMapper()))
              .build(),
          shouldSaveBody()
      );
    }
  }

  @Override
  @SneakyThrows
  protected void handleProcessedMessage(final RequestEntity message) {
    HashMap<String, String> responseHeaders = new HashMap<>();
    if (message.getResponseHeaders() != null) {
      try {
        responseHeaders = getMapper()
            .readValue(message.getResponseHeaders(), new TypeReference<>() {
            });
      } catch (final Exception e) {
        log.error("Unable to parse response headers with value {}.", message.getResponseHeaders());
      }
    }
    final var builder = Response
        .status(Response.Status.fromStatusCode(message.getResponseStatus()))
        .entity(message.getResponseBody());
    responseHeaders.forEach(builder::header);
    //If message already processed, return
    throw IOSaveException.error(IOSaveException.ErrorCode.MESSAGE_ALREADY_PROCESSED);
  }

  private boolean shouldSaveBody() {
    return customInbound.saveRequestBody();
  }

  @SneakyThrows
  private String extractRequestIdFromRequestBody(String requestBody) {
    return getMapper().readValue(requestBody, requestBodyClass).getRequestId();
  }

  private String getTraceId(final ContainerRequestContext requestContext) {
    if (null == customInbound.traceId()) {
      return "TXN-" + UUID.randomUUID();
    }
    return requestContext.getHeaderString(customInbound.traceId());
  }

  private void validateEmbeddedRequestId(String requestId) {
    if (null == requestId) {
      throw IOSaveException.error(IOSaveException.ErrorCode.MESSAGE_UNPROCESSED);
    }
  }


}
