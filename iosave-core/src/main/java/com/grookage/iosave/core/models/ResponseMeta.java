package com.grookage.iosave.core.models;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ResponseMeta {

  private final int responseStatus;
  private final Map<String, String> responseHeaders;
  private final String responseBody;

}
