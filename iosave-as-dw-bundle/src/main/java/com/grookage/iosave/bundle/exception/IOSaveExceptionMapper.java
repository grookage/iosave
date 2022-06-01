package com.grookage.iosave.bundle.exception;

import com.grookage.iosave.bundle.entites.IOSaveResponse;
import com.grookage.iosave.core.exception.IOSaveException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class IOSaveExceptionMapper implements ExceptionMapper<IOSaveException> {

  @Override
  public Response toResponse(IOSaveException ioSaveException) {
    log.error("Error in request, saw IOSaveException", ioSaveException);
    return Response.status(ioSaveException.getErrorCode().getResponseCode())
        .entity(IOSaveResponse.builder()
            .data(ioSaveException.getContext())
            .message(ioSaveException.getMessage())
            .errorCode(ioSaveException.getErrorCode())
            .build())
        .build();
  }
}
