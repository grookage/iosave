package com.grookage.iosave.bundle.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.iosave.core.entities.RequestEntity;
import com.grookage.iosave.core.entities.RequestHeaders;
import com.grookage.iosave.core.exception.IOSaveException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.server.ContainerException;

@UtilityClass
public class JerseyInboundMessageFilterUtils {

  public static String isLoggingEnabled(ContainerRequestContext requestContext) {
    return requestContext.getHeaderString(
        RequestHeaders.LOGGING_ENABLED.getHeaderName()
    );
  }

  public static Map<String, String> extractResponseHeaders(ContainerResponseContext responseContext) {
    final Map<String, String> responseHeaders = new HashMap<>();
    responseContext.getHeaders().forEach((key, value) -> responseHeaders.put(key,
        value.stream().map(String::valueOf).collect(Collectors.joining(","))));
    return responseHeaders;
  }

  public static String getRequestBody(ContainerRequestContext context,
      RequestEntity requestEntity) {
    return Objects.isNull(requestEntity) ? readFromRequest(context)
        : requestEntity.getRequestBody();
  }

  private static String readFromRequest(ContainerRequestContext request) {
    final var out = new ByteArrayOutputStream();
    final var in = request.getEntityStream();
    byte[] requestEntity = new byte[0];
    try {
      if (in.available() > 0) {
        ReaderWriter.writeTo(in, out);
        requestEntity = out.toByteArray();
        request.setEntityStream(
            new ByteArrayInputStream(requestEntity));  //Write back so that it gets consumed later
      }
      return new String(requestEntity);
    } catch (IOException ex) {
      throw new ContainerException(ex);
    }
  }

  @SneakyThrows
  public static String readFromResponse(ContainerResponseContext response, ObjectMapper mapper) {
    final var entity = response.getEntity();
    try {
      return mapper.writeValueAsString(entity);   //Deserialize anyway
    } catch (IOSaveException e) {
      return entity.toString();
    }
  }

}
