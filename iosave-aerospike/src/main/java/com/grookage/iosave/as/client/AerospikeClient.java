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
package com.grookage.iosave.as.client;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.iosave.as.utils.CompressionUtils;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@SuppressWarnings("unused")
public class AerospikeClient {

  private static final String DEFAULT_BIN = "default";

  private final String namespace;
  private final String storeType;
  private final int ttl;
  private final ObjectMapper mapper;
  private final IAerospikeClient client;

  @Builder
  public AerospikeClient(String namespace,
      final String storeType,
      final int ttl,
      final IAerospikeClient client,
      final ObjectMapper mapper
  ) {
    this.namespace = namespace;
    this.storeType = storeType;
    this.ttl = ttl;
    this.mapper = mapper;
    this.client = client;
  }

  private Key getKey(String cacheKey) {
    return new Key(namespace, String.format("%s", storeType), cacheKey);
  }

  private Bin getBin(Object value) {
    return new Bin(DEFAULT_BIN, value);
  }

  @SneakyThrows
  private <T> void put(String cacheKey, T value,
      RecordExistsAction recordExistsAction) {
    final var key = getKey(cacheKey);
    final var bin = getBin(CompressionUtils.compressAndEncode(mapper.writeValueAsBytes(value)));
    putIntoStore(key, bin, recordExistsAction);
  }

  private void putIntoStore(Key key, Bin bin,
      RecordExistsAction recordExistsAction) {
    var writePolicy = new WritePolicy(client.getWritePolicyDefault());
    writePolicy.recordExistsAction = recordExistsAction;
    if (ttl > 0) {
      writePolicy.expiration = ttl;
    }
    client.put(writePolicy, key, bin);
  }

  public <T> void save(String cacheKey, T value) {
    put(cacheKey, value, RecordExistsAction.REPLACE);
  }

  public <T> void strictSave(String cacheKey, T value) {
    put(cacheKey, value, RecordExistsAction.CREATE_ONLY);
  }

  private Record getFromStore(Key key) {
    return client.get(null, key, DEFAULT_BIN);
  }

  @SneakyThrows
  public <T> Optional<T> get(String cacheKey, Class<T> tClass) {
    final var key = getKey(cacheKey);
    final var storedRecord = getFromStore(key);

    if (null == storedRecord) {
      return Optional.empty();
    }
    return Optional.of(
        mapper.readValue(CompressionUtils.decodeAndDecompress(storedRecord.getString(DEFAULT_BIN)),
            tClass));
  }

  public void delete(String cacheKey) {
    final var key = getKey(cacheKey);
    client.delete(client.getWritePolicyDefault(), key);
  }

}
