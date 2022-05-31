package com.grookage.iosave.bundle.filters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.iosave.as.repository.ASRequestRepository;
import com.grookage.iosave.bundle.annotations.CustomInbound;
import com.grookage.iosave.bundle.interfaces.CustomInboundRequest;
import com.grookage.iosave.core.entities.RequestEntity;
import com.grookage.iosave.core.entities.RequestHeaders;
import com.grookage.iosave.core.entities.RequestStatus;
import com.grookage.iosave.core.exception.IOSaveException;
import com.grookage.iosave.core.services.RequestReceiverService;
import com.grookage.iosave.core.utils.RequestManager;
import com.grookage.iosave.core.utils.RequestUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import lombok.Builder;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.internal.process.MappableException;

@Slf4j
@Setter
public class CustomInboundMessageFilter<T extends CustomInboundRequest> implements
    ContainerRequestFilter, ContainerResponseFilter {

  private final RequestReceiverService requestReceiverService;
  private final ObjectMapper mapper;
  private final Class<T> requestBodyClass;
  private final CustomInbound customInbound;

  @Builder
  public CustomInboundMessageFilter(
      final ASRequestRepository messageRepository,
      final ObjectMapper mapper,
      Class<T> requestBodyClass,
      CustomInbound customInbound) {
    this.requestReceiverService = new RequestReceiverService(messageRepository);
    this.mapper = mapper;
    this.requestBodyClass = requestBodyClass;
    this.customInbound = customInbound;
  }


  @Override
  public void filter(ContainerRequestContext requestContext) {
    final boolean saveRequestBody = shouldSaveBody();
    final var requestBody = getRequestBody(requestContext, null);
    final var requestId = extractRequestIdFromRequestBody(requestBody);
    if (null == requestId) {
      throw IOSaveException.error(IOSaveException.ErrorCode.MESSAGE_UNPROCESSED);
    }
    final var traceId = getTraceId(requestContext);
    final var loggingEnabled = requestContext.getHeaderString(
        RequestHeaders.LOGGING_ENABLED.getHeaderName()
    );
    if (RequestUtils.loggingEnabled(loggingEnabled, requestId)) {
      RequestUtils.setupTracing(traceId);
      try {
        final var message = RequestUtils.createInboundMessage(
            requestId,
            traceId,
            saveRequestBody,
            getRequestBody(requestContext, null),
            null,
            null
        );
        log.info("Received message with id {} ", message.getRequestId());
        RequestManager.setCurrentMessage(message);
        final var inboundMessage = requestReceiverService.preHandle(message);
        if (inboundMessage.getProcessed() == RequestStatus.PROCESSED) {
          handleProcessedMessage(inboundMessage);
        }
      } catch (final IOSaveException e) {
        log.error("Unable to process message. REASON: {}", e.getMessage());
        throw e;
      } catch (final Exception e) {
        log.error("Unable to process message. REASON: {}", e.getMessage());
        throw IOSaveException.error(IOSaveException.ErrorCode.MESSAGE_UNPROCESSED);
      }
    }
  }


  @Override
  public void filter(ContainerRequestContext requestContext,
      ContainerResponseContext responseContext) {

    final boolean saveRequestBody = shouldSaveBody();
    final var inboundMessage = RequestManager.getCurrentMessage();
    final var requestBody = getRequestBody(requestContext, inboundMessage);
    final var requestId = extractRequestIdFromRequestBody(requestBody);
    final var loggingEnabled = requestContext.getHeaderString(
        RequestHeaders.LOGGING_ENABLED.getHeaderName());
    final var traceId = getTraceId(requestContext);
    if (RequestUtils.loggingEnabled(loggingEnabled, requestId)) {
      RequestEntity message = null;
      try {
        if (inboundMessage != null && inboundMessage.getProcessed() == RequestStatus.PROCESSED) {
          log.info("Message with id {} already processed. Returning previous response",
              inboundMessage.getRequestId());

          //Message already processed, do not set from app, but set data from DB
          message = RequestUtils.createFromPreviousResponse(
              requestId,
              traceId,
              getRequestBody(requestContext, inboundMessage),
              inboundMessage,
              saveRequestBody
          );
        } else {
          final Map<String, String> responseHeaders = new HashMap<>();
          responseContext.getHeaders().forEach((key, value) -> responseHeaders.put(key,
              value.stream().map(String::valueOf).collect(Collectors.joining(","))));
          message = RequestUtils.createInboundMessage(requestId,
              traceId,
              getRequestBody(requestContext, inboundMessage),
              readFromResponse(responseContext),
              saveRequestBody,
              inboundMessage,
              responseContext.getStatus(),
              mapper.writeValueAsString(responseHeaders)
          );
          requestReceiverService.postHandle(message);
        }
      } catch (final MappableException e) {
        log.warn("There is a mappable exception while trying to process the request");
      } catch (final Exception e) {
        log.warn("Possible duplicate request can creep in.");
      } finally {
        if (message != null) {
          log.info("Finished processing message with id {}. Response Code {}",
              message.getRequestId(), message.getResponseStatus());
        }
        RequestManager.endMessageProcessing();
        RequestUtils.endTransaction();
      }
    }

  }

  @SneakyThrows
  private void handleProcessedMessage(final RequestEntity message) {
    HashMap<String, String> responseHeaders = new HashMap<>();
    if (message.getResponseHeaders() != null) {
      try {
        responseHeaders = mapper.readValue(message.getResponseHeaders(), new TypeReference<>() {
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
    return mapper.readValue(requestBody, requestBodyClass).getRequestId();
  }

  private String getRequestBody(ContainerRequestContext context, RequestEntity requestEntity) {
    return Objects.isNull(requestEntity) ? readFromRequest(context)
        : requestEntity.getRequestBody();
  }

  private static String readFromRequest(ContainerRequestContext request) {
    final var out = new ByteArrayOutputStream();
    final var in = request.getEntityStream();
    byte[] requestEntity = new byte[0];
    try {
      if (in.available() > 0) {
        ReaderWriter.writeTo(in, out);
        requestEntity = out.toByteArray();
        request.setEntityStream(
            new ByteArrayInputStream(requestEntity));  //Write back so that it gets consumed later
      }
      return new String(requestEntity);
    } catch (IOException ex) {
      throw new ContainerException(ex);
    }
  }

  private String getTraceId(final ContainerRequestContext requestContext) {
    if (null == customInbound.traceId()) {
      return "TXN-" + UUID.randomUUID();
    }
    return requestContext.getHeaderString(customInbound.traceId());
  }

  @SneakyThrows
  private String readFromResponse(ContainerResponseContext response) {
    final var entity = response.getEntity();
    try {
      return mapper.writeValueAsString(entity);   //Deserialize anyway
    } catch (IOSaveException e) {
      return entity.toString();
    }
  }


}
