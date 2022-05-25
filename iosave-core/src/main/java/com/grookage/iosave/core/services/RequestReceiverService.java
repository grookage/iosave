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
package com.grookage.iosave.core.services;

import com.grookage.iosave.core.entities.RequestEntity;
import com.grookage.iosave.core.entities.RequestStatus;
import com.grookage.iosave.core.entities.RequestStatus.MessageTypeVisitor;
import com.grookage.iosave.core.exception.IOSaveException;
import com.grookage.iosave.core.repository.RequestRepository;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RequestReceiverService {

  private final RequestRepository requestRepository;

  private RequestEntity getMessage(RequestEntity message) {
    return requestRepository
        .findByMessageId(message.getRequestId()).orElse(null);
  }

  @SneakyThrows
  private RequestEntity createMessage(RequestEntity message) {
    final var ibMessage = new RequestEntity(message);
    requestRepository.strictSave(ibMessage);
    return ibMessage;
  }

  private void updateMessage(RequestEntity message) {
    final var status = message.getResponseStatus();
    if (status >= 200 && status < 300) {
      message.setProcessed(RequestStatus.PROCESSED);
      message.setProcessedAt(new Date(System.currentTimeMillis()));
    } else {
      message.setProcessed(RequestStatus.FAILED);
    }
    requestRepository.update(message);
  }

  @SneakyThrows
  private void processExistingMessage(RequestEntity requestEntity) {
    requestEntity.getProcessed().accept(new MessageTypeVisitor<Void>() {
      @Override
      public Void failed() {
        throw IOSaveException.error(IOSaveException.ErrorCode.DUPLICATE_MESSAGE);
      }

      @Override
      public Void processed() {
        log.info("Message already processed in the INBOUND_MESSAGES entitystore: " + requestEntity
            .getRequestId());
        requestEntity.incrementDuplicateRequestCount();
        updateMessage(requestEntity);
        return null;
      }

      @Override
      public Void processing() {
        log.error(
            "Message has a wrong unimplemented processed state: " + requestEntity.getRequestId());
        throw IOSaveException.error(IOSaveException.ErrorCode.MESSAGE_UNPROCESSED);
      }
    });
  }

  public RequestEntity preHandle(RequestEntity message) {
    log.debug("Pre-handling message");
    var inboundMessage = getMessage(message);
    if (null == inboundMessage) {
      try {
        inboundMessage = createMessage(message);
      } catch (Exception e) {
        log.error("Error! Message (" + message.getRequestId()
            + " is already present. You can't callAndSave the same messageId twice.", e);
        throw IOSaveException.error(IOSaveException.ErrorCode.DUPLICATE_MESSAGE);
      }
    } else {
      processExistingMessage(message);
    }
    return inboundMessage;
  }

  public void postHandle(RequestEntity message) {
    log.debug("PostHandling message");
    final var inboundMessage = getMessage(message);
    if (null == inboundMessage) {
      log.error("inbound message not found for messageId: {}", message.getRequestId());
      throw IOSaveException.error(
          IOSaveException.ErrorCode.ENTITY_NOT_FOUND,
          Map.of("cause", String
              .format("inbound message not found for messageId: %s", message.getRequestId())));
    }
    try {
      inboundMessage.setResponseStatus(message.getResponseStatus());
      inboundMessage.setResponseBody(message.getResponseBody());
      inboundMessage.setResponseHeaders(message.getResponseHeaders());
    } finally {
      updateMessage(inboundMessage);
    }
  }
}
