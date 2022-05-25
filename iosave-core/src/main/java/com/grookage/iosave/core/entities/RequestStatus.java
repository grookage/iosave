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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RequestStatus {
  FAILED(0) {
    @Override
    public <T> T accept(MessageTypeVisitor<T> visitor) {
      return visitor.failed();
    }
  },

  PROCESSED(1) {
    @Override
    public <T> T accept(MessageTypeVisitor<T> visitor) {
      return visitor.processed();
    }
  },

  PROCESSING(null) {
    @Override
    public <T> T accept(MessageTypeVisitor<T> visitor) {
      return visitor.processing();
    }
  };   //As they are in DB

  private static final Map<Integer, RequestStatus> lookup = new HashMap<>();    //Reverse map from code to ENUM

  static {
    EnumSet.allOf(RequestStatus.class).forEach(s -> lookup.put(s.getCode(), s));
  }

  private Integer code;

  public static RequestStatus get(Integer code) {
    return lookup.get(code);
  }

  public abstract <T> T accept(MessageTypeVisitor<T> visitor);

  public interface MessageTypeVisitor<T> {

    T failed();

    T processed();

    T processing();
  }
}
