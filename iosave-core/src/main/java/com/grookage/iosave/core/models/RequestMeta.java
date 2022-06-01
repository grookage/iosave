package com.grookage.iosave.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class RequestMeta {

  private final String requestId;
  private final String traceId;
  private final String requestBody;

}
