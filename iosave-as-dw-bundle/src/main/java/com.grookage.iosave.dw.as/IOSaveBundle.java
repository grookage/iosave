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
package com.grookage.iosave.dw.as;

import com.codahale.metrics.health.HealthCheck;
import com.grookage.iosave.as.client.AerospikeClient;
import com.grookage.iosave.as.config.IOSaveAerospikeConfig;
import com.grookage.iosave.as.repository.ASRequestRepository;
import com.grookage.iosave.as.utils.AerospikeClientUtils;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class IOSaveBundle<T extends Configuration> implements ConfiguredBundle<T> {

  public abstract IOSaveAerospikeConfig getConfig(T configuration);

  public abstract void preBundle(T configuration);

  @Override
  public void run(T configuration, Environment environment) {
    final var aerospikeConfig = getConfig(configuration);
    final var iAeroClient = AerospikeClientUtils.provideIAerospikeClient(aerospikeConfig);
    final var aeroClient = AerospikeClient.builder()
        .client(iAeroClient)
        .namespace(aerospikeConfig.getNamespace())
        .storeType(aerospikeConfig.getDefaultSet())
        .ttl(aerospikeConfig.getTtl())
        .mapper(environment.getObjectMapper())
        .build();
    final var messageRepository = new ASRequestRepository(aeroClient);
    environment.jersey().register(InboundMessageFilter.builder()
        .messageRepository(messageRepository)
        .mapper(environment.getObjectMapper())
        .build());
    environment.healthChecks().register("iosave-as-health", new HealthCheck() {
      @Override
      protected Result check() {
        return messageRepository.connected() ?
            HealthCheck.Result.healthy("IOSave is healthy")
            : HealthCheck.Result.unhealthy("IOSave is unhealthy");
      }
    });
  }
}
