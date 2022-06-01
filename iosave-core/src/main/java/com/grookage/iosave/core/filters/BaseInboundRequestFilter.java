package com.grookage.iosave.core.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.iosave.core.entities.RequestEntity;
import com.grookage.iosave.core.entities.RequestStatus;
import com.grookage.iosave.core.exception.IOSaveException;
import com.grookage.iosave.core.models.RequestMeta;
import com.grookage.iosave.core.models.ResponseMeta;
import com.grookage.iosave.core.services.RequestReceiverService;
import com.grookage.iosave.core.utils.RequestManager;
import com.grookage.iosave.core.utils.RequestUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public abstract class BaseInboundRequestFilter {

  private final RequestReceiverService requestReceiverService;
  private final ObjectMapper mapper;

  protected BaseInboundRequestFilter(RequestReceiverService requestReceiverService,
      ObjectMapper mapper) {
    this.requestReceiverService = requestReceiverService;
    this.mapper = mapper;
  }

  protected void requestFilter(RequestMeta requestMeta, boolean saveRequestBody) {
    RequestUtils.setupTracing(requestMeta.getTraceId());
    try {
      final var message = RequestUtils.createInboundMessage(
          requestMeta.getRequestId(),
          requestMeta.getTraceId(),
          saveRequestBody,
          requestMeta.getRequestBody(),
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

  protected void responseFilter(RequestMeta requestMeta, ResponseMeta responseMeta,
      boolean saveRequestBody) {
    final var inboundMessage = RequestManager.getCurrentMessage();
    RequestEntity message = null;
    try {
      if (inboundMessage != null && inboundMessage.getProcessed() == RequestStatus.PROCESSED) {
        log.info("Message with id {} already processed. Returning previous response",
            inboundMessage.getRequestId());

        //Message already processed, do not set from app, but set data from DB
        message = RequestUtils.createFromPreviousResponse(
            requestMeta.getRequestId(),
            requestMeta.getTraceId(),
            requestMeta.getRequestBody(),
            inboundMessage,
            saveRequestBody
        );
      } else {
        message = RequestUtils.createInboundMessage(
            requestMeta.getRequestId(),
            requestMeta.getTraceId(),
            requestMeta.getRequestBody(),
            responseMeta.getResponseBody(),
            saveRequestBody,
            inboundMessage,
            responseMeta.getResponseStatus(),
            getMapper().writeValueAsString(responseMeta.getResponseHeaders())
        );
        getRequestReceiverService().postHandle(message);
      }
//      } catch (final MappableException e) {
//        log.warn("There is a mappable exception while trying to process the request");
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

  protected abstract void handleProcessedMessage(RequestEntity inboundMessage);

}
