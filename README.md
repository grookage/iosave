# iosave [![Build](https://github.com/grookage/iosave/actions/workflows/build.yml/badge.svg)](https://github.com/grookage/iosave/actions/workflows/build.yml)


> A flow of words is a surge sign of duplicity
> - by Honore de Balzac

### Maven Dependency

Use the following maven dependency:
```xml
<dependency>
    <groupId>com.grookage.apps</groupId>
    <artifactId>iosave</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Build instructions
- Clone the source:

      git clone github.com/grookage/iosave

- Build

      mvn install


IOSave provides the following capabilities
- Helps you prevent you sending duplicate requests on a server when an existing one with the same ID is pending
- Configurable headers to select your own requestId header
- Annotation based processing of selectively applying the same filter. 

IOSave is built as a dropwizard bundle, using aerospike as a repository, to demonstrate it at work on a server, you'll need guice along with dropwizard to include it. 
You can also extend the iosave-core and write your own repository and binding.

### Tech

QTrouper uses rabbitMQ as its backend interface and the

* [Dropwizard](https://github.com/dropwizard/dropwizard) - The bundle that got created
* [Aerospike](https://www.aerospike.com/) - Quick, easy to easy KV store

### Example

##Sample Configuration

```
name: example

server:
  maxThreads: 128
  minThreads: 128
  applicationConnectors:
    - type: http
      port: 8080
  adminConnectors:
    - type: http
      port: 8081
  applicationContextPath: /
  requestLog:
    appenders:
      - type: console
        timeZone: IST

logging:
  level: INFO
  loggers:
    com.phonepe.iosave: DEBUG
  appenders:
    - type: console
      threshold: INFO
      timeZone: IST
      logFormat: "%(%-5level) [%date] [%thread] [%logger{0}]: %message%n"

ioSaveConfig:
  hosts:
    - host: localhost
      port: 3000
  retries: 3
  sleepBetweenRetries: 5
  maxConnectionsPerNode: 32
  threadPoolSize: 512
  namespace: iosave
  //tls configured could be specified as well should you wish to
```

## Bundle Inclusion

```
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
        //If you have anything to do as precursor
  }
});
```

## Sample Resource

```
@Singleton
@Getter
@Setter
@Path("/v1")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AppResource {

  @Path("/test")
  @POST
  @ExceptionMetered
  @Timed
  @Inbound(saveRequestBody = true)
  public Response testResponse() {
    return Response.status(200).entity(Map.of("status", "ok")).build();
  }
}
```

LICENSE
-------

Copyright 2022 Koushik R <rkoushik.14@gmail.com>.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


  
