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
package com.grookage.iosave.as.repository;

import com.grookage.iosave.as.client.AerospikeClient;
import com.grookage.iosave.core.entities.RequestEntity;
import com.grookage.iosave.core.repository.AbstractRequestRepository;
import java.sql.Date;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Builder
public class ASRequestRepository extends AbstractRequestRepository {

  private final AerospikeClient aerospikeClient;

  @Override
  public boolean connected() {
    return null != aerospikeClient &&
        null != aerospikeClient.getClient() &&
        aerospikeClient.getClient().isConnected();
  }

  @Override
  public Optional<RequestEntity> findByMessageId(String messageId) {
    try {
      return aerospikeClient.get(messageId, RequestEntity.class);
    } catch (Exception e) {
      log.error("There is an exception while trying to get the messageId from the store {}",
          messageId);
      return Optional.empty();
    }
  }

  @Override
  public void strictSave(RequestEntity requestEntity) {
    requestEntity.setUpdatedAt(new Date(System.currentTimeMillis()));
    aerospikeClient.strictSave(requestEntity.getRequestId(), requestEntity);
  }

  @Override
  public void save(RequestEntity requestEntity) {
    try {
      requestEntity.setUpdatedAt(new Date(System.currentTimeMillis()));
      aerospikeClient
          .save(requestEntity.getRequestId(), requestEntity);
    } catch (Exception e) {
      log.error(
          "Can't persist the inbound entity into aerospike. Possible duplicates might creep in");
    }
  }
}
