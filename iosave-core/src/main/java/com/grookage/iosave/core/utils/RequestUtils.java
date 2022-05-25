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
package com.grookage.iosave.core.utils;

import com.grookage.iosave.core.entities.RequestEntity;
import com.grookage.iosave.core.entities.RequestStatus;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("unused")
public class RequestUtils {

  public static boolean loggingEnabled(final String enableLogHeader,
      final String requestId) {
    return null != requestId
        && (null == enableLogHeader || Boolean.parseBoolean(enableLogHeader));
  }

  public static void setupTracing(String traceId) {
    TracingManager.startTracing(traceId);
  }

  public static void endTransaction() {
    TracingManager.endTracing();
  }

  public static RequestEntity createInboundMessage(String messageId,
      String traceId,
      boolean saveRequestBody,
      String requestBody,
      String responseBody,
      RequestEntity inboundMessage
  ) {
    final var requestEntity = RequestEntity.builder()
        .requestId(messageId)
        .traceId(traceId)
        .responseBody(responseBody)
        .processed(RequestStatus.PROCESSING)
        .build();
    if (saveRequestBody) {
      requestEntity.setRequestBody(Objects.isNull(inboundMessage) ? requestBody
          : inboundMessage.getRequestBody());
    }
    return requestEntity;
  }

  public static RequestEntity createFromPreviousResponse(
      String messageId,
      String traceId,
      String requestBody,
      RequestEntity inboundMessage,
      Boolean saveRequestBody
  ) {
    final var message = createInboundMessage(messageId, traceId, saveRequestBody, requestBody,
        null, inboundMessage);
    message.setResponseStatus(inboundMessage.getResponseStatus());
    message.setResponseBody(inboundMessage.getResponseBody());
    message.setResponseHeaders(inboundMessage.getResponseHeaders());
    return message;
  }

  @SneakyThrows
  public static RequestEntity createInboundMessage(String messageId,
      String traceId,
      String requestBody,
      String responseBody,
      Boolean saveRequestBody,
      RequestEntity inboundMessage,
      int responseStatus,
      String responseHeaders
  ) {
    final var message = createInboundMessage(
        messageId,
        traceId,
        saveRequestBody,
        requestBody,
        responseBody,
        inboundMessage
    );
    message.setResponseStatus(responseStatus);
    message.setResponseBody(responseBody);

    message.setResponseHeaders(responseHeaders);
    return message;
  }
}
