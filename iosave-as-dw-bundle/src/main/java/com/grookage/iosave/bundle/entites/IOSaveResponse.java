package com.grookage.iosave.bundle.entites;

import com.grookage.iosave.core.exception.IOSaveException.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IOSaveResponse<T> {

  private boolean success;
  private T data;
  private ErrorCode errorCode;
  private String message;

}
