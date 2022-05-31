package com.grookage.iosave.example.entities;

import com.grookage.iosave.bundle.interfaces.CustomInboundRequest;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SampleCustomInboundRequest implements CustomInboundRequest {

  @NotNull
  private String transactionId;

  @Override
  public String getRequestId() {
    return transactionId;
  }
}
