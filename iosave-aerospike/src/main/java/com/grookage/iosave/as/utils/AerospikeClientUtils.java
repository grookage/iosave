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
package com.grookage.iosave.as.utils;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.CommitLevel;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.ReadModeAP;
import com.aerospike.client.policy.Replica;
import com.aerospike.client.policy.TlsPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.grookage.iosave.as.config.IOSaveAerospikeConfig;
import java.util.concurrent.Executors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class AerospikeClientUtils {

  public static IAerospikeClient provideIAerospikeClient(IOSaveAerospikeConfig config){
    log.info("Starting Aerospike client");

    final var readPolicy = new Policy();
    readPolicy.maxRetries = config.getRetries();
    readPolicy.readModeAP = ReadModeAP.ONE;
    readPolicy.replica = Replica.MASTER_PROLES;
    readPolicy.sleepBetweenRetries = config.getSleepBetweenRetries();
    readPolicy.totalTimeout = config.getTimeout();
    readPolicy.sendKey = true;

    final var writePolicy = new WritePolicy();
    writePolicy.maxRetries = config.getRetries();
    writePolicy.readModeAP = ReadModeAP.ALL;
    writePolicy.replica = Replica.MASTER_PROLES;
    writePolicy.sleepBetweenRetries = config.getSleepBetweenRetries();
    writePolicy.commitLevel = CommitLevel.COMMIT_ALL;
    writePolicy.totalTimeout = config.getTimeout();
    writePolicy.sendKey = true;

    final var clientPolicy = new ClientPolicy();
    clientPolicy.user = config.getUsername();
    clientPolicy.password = config.getPassword();
    clientPolicy.maxConnsPerNode = config.getMaxConnectionsPerNode();
    clientPolicy.readPolicyDefault = readPolicy;
    clientPolicy.writePolicyDefault = writePolicy;
    clientPolicy.failIfNotConnected = true;
    clientPolicy.threadPool = Executors.newFixedThreadPool(
        config.getThreadPoolSize());
    final var localConfig = Boolean.parseBoolean(System.getProperty("localConfig", "false"));

    if (!localConfig) {
      clientPolicy.tlsPolicy = new TlsPolicy();
    }

    return new AerospikeClient(
        clientPolicy, config.getHosts().stream().map
            (
                connection -> new Host(connection.getHost(),
                    connection.getTls(), connection.getPort()
                )
            )
        .toArray(Host[]::new)
    );
  }
}
