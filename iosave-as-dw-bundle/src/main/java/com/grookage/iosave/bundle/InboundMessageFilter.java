/*
 * Copyright 2022 Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.grookage.iosave.bundle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.iosave.as.repository.ASRequestRepository;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import lombok.Builder;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.internal.process.MappableException;

@Singleton
@Slf4j
@Setter
@Inbound
public class InboundMessageFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private final RequestReceiverService requestReceiverService;
  private final ObjectMapper mapper;
  @Context
  private ResourceInfo resourceInfo;

  @Builder
  public InboundMessageFilter(
      final ASRequestRepository messageRepository,
      final ObjectMapper mapper
  ) {
    this.requestReceiverService = new RequestReceiverService(messageRepository);
    this.mapper = mapper;
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

  private String getTraceId(final ContainerRequestContext requestContext, Inbound inbound) {
    if (null == inbound.traceId()) {
      return "TXN-" + UUID.randomUUID();
    }
    return requestContext.getHeaderString(inbound.traceId());
  }

  private String getRequestId(ContainerRequestContext requestContext, Inbound inbound) {
    if (null == inbound.requestId()) {
      return null;
    }
    return requestContext.getHeaderString(inbound.requestId());
  }

  private boolean mandateRequestId(Inbound inbound) {
    return inbound.mandateRequestId();
  }

  private boolean shouldSaveBody(Inbound inbound) {
    return inbound.saveRequestBody();
  }

  private Optional<Inbound> getInbound() {
    final var resourceMethod = resourceInfo.getResourceMethod();
    return null == resourceMethod ? Optional.empty() : Optional.ofNullable(
        resourceMethod.getAnnotation(Inbound.class)
    );
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

  @SneakyThrows
  private String readFromResponse(ContainerResponseContext response) {
    final var entity = response.getEntity();
    try {
      return mapper.writeValueAsString(entity);   //Deserialize anyway
    } catch (IOSaveException e) {
      return entity.toString();
    }
  }

  private String getRequestBody(ContainerRequestContext context, RequestEntity requestEntity) {
    return Objects.isNull(requestEntity) ? readFromRequest(context)
        : requestEntity.getRequestBody();
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    final var inbound = getInbound().orElse(null);
    if (null == inbound) {
      return;
    }
    final boolean saveRequestBody = shouldSaveBody(inbound);
    final boolean mandateRequestId = mandateRequestId(inbound);
    final var requestId = getRequestId(requestContext, inbound);
    if (mandateRequestId && null == requestId) {
      throw IOSaveException.error(IOSaveException.ErrorCode.MESSAGE_UNPROCESSED);
    }
    final var traceId = getTraceId(requestContext, inbound);
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
  @SneakyThrows
  public void filter(final ContainerRequestContext requestContext,
      final ContainerResponseContext responseContext) {
    final var inbound = getInbound().orElse(null);
    if (null == inbound) {
      return;
    }
    final boolean saveRequestBody = shouldSaveBody(inbound);
    final var requestId = getRequestId(requestContext, inbound);
    final var loggingEnabled = requestContext.getHeaderString(
        RequestHeaders.LOGGING_ENABLED.getHeaderName());
    final var traceId = getTraceId(requestContext, inbound);
    if (RequestUtils.loggingEnabled(loggingEnabled, requestId)) {
      final var inboundMessage = RequestManager.getCurrentMessage();
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
}
