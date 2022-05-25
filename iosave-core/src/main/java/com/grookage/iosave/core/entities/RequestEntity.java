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
package com.grookage.iosave.core.entities;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("unused")
public class RequestEntity {

  protected String requestId;
  protected String traceId;
  protected String requestBody;
  protected Date createdAt;
  protected Date processedAt;
  protected Date updatedAt;
  protected RequestStatus processed = RequestStatus.PROCESSING;
  protected int retryCount = 0;
  protected int responseStatus;
  protected String responseHeaders;
  protected String responseBody;
  protected int duplicateRequestCount = 0;


  public RequestEntity(RequestEntity requestEntity) {
    this.requestId = requestEntity.getRequestId();
    this.createdAt = requestEntity.getCreatedAt();
    this.updatedAt = requestEntity.getUpdatedAt();
    this.traceId = requestEntity.getTraceId();
    this.responseBody = requestEntity.getResponseBody();
    this.responseHeaders = requestEntity.getResponseHeaders();
    this.requestBody = requestEntity.getRequestBody();
    this.responseStatus = requestEntity.getResponseStatus();
  }

  public void loadFromMessage(RequestEntity requestEntity) {
    setRequestId(requestEntity.getRequestId());
    setRetryCount(requestEntity.getRetryCount());
    setResponseStatus(requestEntity.getResponseStatus());
    setResponseBody(requestEntity.getResponseBody());
    setResponseHeaders(requestEntity.getResponseHeaders());
    setProcessed(requestEntity.getProcessed());
    setProcessedAt(requestEntity.getProcessedAt());
    setDuplicateRequestCount(requestEntity.getDuplicateRequestCount());
    setRequestBody(requestEntity.getRequestBody());

    if (requestEntity.getCreatedAt() != null) {
      setCreatedAt(requestEntity.getCreatedAt());
    }

    if (requestEntity.getUpdatedAt() != null) {
      setUpdatedAt(requestEntity.getUpdatedAt());
    }
  }

  public void incrementDuplicateRequestCount() {
    this.duplicateRequestCount += 1;
  }
}
