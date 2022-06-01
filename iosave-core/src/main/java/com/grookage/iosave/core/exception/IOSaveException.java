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
package com.grookage.iosave.core.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("unused")
@Getter
public class IOSaveException extends RuntimeException {

  private final ErrorCode errorCode;
  private final transient Map<String, Object> context;

  protected IOSaveException(ErrorCode errorCode, Map<String, Object> context) {
    super();
    this.errorCode = errorCode;
    this.context = context;
  }

  private IOSaveException(Throwable cause, ErrorCode errorCode) {
    super(cause);
    this.errorCode = errorCode;
    this.context = Collections.singletonMap("message", cause.getLocalizedMessage());
  }

  public static IOSaveException propagate(ErrorCode errorCode, Throwable t) {
    return new IOSaveException(t, errorCode);
  }

  public static IOSaveException error(ErrorCode errorCode, Map<String, Object> context) {
    return new IOSaveException(errorCode, context);
  }

  public static IOSaveException error(ErrorCode errorCode) {
    return new IOSaveException(errorCode, new HashMap<>());
  }

  @Getter
  @AllArgsConstructor
  public enum ErrorCode {

    ENTITY_NOT_FOUND(400),

    MESSAGE_UNPROCESSED(400),

    MESSAGE_ALREADY_PROCESSED(400),

    BAD_REQUEST(400),

    DUPLICATE_MESSAGE(417),

    INVALID_USE_OF_CUSTOM_INBOUND(412);

    int responseCode;
  }
}
