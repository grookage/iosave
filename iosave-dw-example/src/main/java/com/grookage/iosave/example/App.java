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
package com.grookage.iosave.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Stage;
import com.grookage.iosave.as.config.IOSaveAerospikeConfig;
import com.grookage.iosave.bundle.IOSaveBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class App extends Application<AppConfiguration> {

  public static void main(String args[]) throws Exception {
    new App().run(args);
  }

  public void initialize(Bootstrap<AppConfiguration> bootstrap) {
    bootstrap.addBundle(GuiceBundle.builder()
        .enableAutoConfig(getClass().getPackage().getName())
        .build(Stage.PRODUCTION));
    bootstrap.addBundle(new IOSaveBundle<>() {
      @Override
      public IOSaveAerospikeConfig getConfig(AppConfiguration configuration) {
        return configuration.getIoSaveConfig();
      }

      @Override
      public void preBundle(AppConfiguration configuration) {

      }
    });
  }

  @Override
  public void run(AppConfiguration appConfiguration, Environment environment) {
    environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    environment.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    environment.getObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    environment.getObjectMapper()
        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    environment.getObjectMapper()
        .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
    environment.jersey().register(new AppResource());
  }
}
