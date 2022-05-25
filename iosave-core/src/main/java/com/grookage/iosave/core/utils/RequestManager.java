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
import lombok.experimental.UtilityClass;

@SuppressWarnings("unused")
@UtilityClass
public class RequestManager {

  private static final ThreadLocal<RequestEntity> currentMessage = new ThreadLocal<>();

  public static void endMessageProcessing() {
    currentMessage.remove();
  }

  public static RequestEntity getCurrentMessage() {
    return currentMessage.get();
  }

  public static void setCurrentMessage(RequestEntity message) {
    currentMessage.set(message);
  }
}
