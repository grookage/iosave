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
package com.grookage.iosave.bundle.filters;

import static com.grookage.iosave.bundle.utils.JerseyInboundMessageFilterUtils.extractResponseHeaders;
import static com.grookage.iosave.bundle.utils.JerseyInboundMessageFilterUtils.getRequestBody;
import static com.grookage.iosave.bundle.utils.JerseyInboundMessageFilterUtils.isLoggingEnabled;
import static com.grookage.iosave.bundle.utils.JerseyInboundMessageFilterUtils.readFromResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.iosave.as.repository.ASRequestRepository;
import com.grookage.iosave.bundle.annotations.Inbound;
import com.grookage.iosave.core.entities.RequestEntity;
import com.grookage.iosave.core.exception.IOSaveException;
import com.grookage.iosave.core.filters.BaseInboundRequestFilter;
import com.grookage.iosave.core.models.RequestMeta;
import com.grookage.iosave.core.models.ResponseMeta;
import com.grookage.iosave.core.services.RequestReceiverService;
import com.grookage.iosave.core.utils.RequestManager;
import com.grookage.iosave.core.utils.RequestUtils;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
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

@Singleton
@Slf4j
@Setter
@Inbound
public class InboundMessageFilter extends BaseInboundRequestFilter
    implements ContainerRequestFilter, ContainerResponseFilter {

  @Context
  private ResourceInfo resourceInfo;

  @Builder
  public InboundMessageFilter(
      final ASRequestRepository messageRepository,
      final ObjectMapper mapper
  ) {
    super(new RequestReceiverService(messageRepository), mapper);
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    final var inbound = getInbound().orElse(null);
    if (null == inbound) {
      return;
    }
    final var requestId = getRequestId(requestContext, inbound);
    final var loggingEnabled = isLoggingEnabled(requestContext);
    if (RequestUtils.loggingEnabled(loggingEnabled, requestId)) {
      validateEmbeddedRequestId(inbound, requestId);
      super.requestFilter(
          RequestMeta.builder()
              .traceId(getTraceId(requestContext, inbound))
              .requestId(requestId)
              .requestBody(getRequestBody(requestContext, null))
              .build(),
          shouldSaveBody(inbound)
      );
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
    final var requestId = getRequestId(requestContext, inbound);
    final var loggingEnabled = isLoggingEnabled(requestContext);
    if (RequestUtils.loggingEnabled(loggingEnabled, requestId)) {
      super.responseFilter(
          RequestMeta.builder()
              .requestId(getRequestId(requestContext, inbound))
              .traceId(getTraceId(requestContext, inbound))
              .requestBody(getRequestBody(requestContext, RequestManager.getCurrentMessage()))
              .build(),
          ResponseMeta.builder()
              .responseStatus(responseContext.getStatus())
              .responseHeaders(extractResponseHeaders(responseContext))
              .responseBody(readFromResponse(responseContext, getMapper()))
              .build(),
          shouldSaveBody(inbound)
      );
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

  private void validateEmbeddedRequestId(Inbound inbound, String requestId) {
    final boolean mandateRequestId = mandateRequestId(inbound);
    if (mandateRequestId && null == requestId) {
      throw IOSaveException.error(IOSaveException.ErrorCode.MESSAGE_UNPROCESSED);
    }
  }


}
