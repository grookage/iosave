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
package com.grookage.iosave.as.config;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IOSaveAerospikeConfig {
  @NotEmpty
  private List<AerospikeHost> hosts;
  private String namespace;
  private String username;
  private String password;
  @Builder.Default
  private String defaultSet = "messages";
  @Builder.Default
  private String defaultBin = "default";
  private int maxConnectionsPerNode;
  private int timeout;
  private int retries;
  private int sleepBetweenRetries;
  private int threadPoolSize;
  private int maxSocketIdle;
  private int ttl;
}
