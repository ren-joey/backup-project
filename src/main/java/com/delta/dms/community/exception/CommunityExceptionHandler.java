package com.delta.dms.community.exception;

import java.io.IOException;
import java.util.NoSuchElementException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailSendException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

@ControllerAdvice
public class CommunityExceptionHandler extends ResponseEntityExceptionHandler {

  private LogUtil log = LogUtil.getInstance();

  @ExceptionHandler(
      value = {
        IOException.class,
        NullPointerException.class,
        GroupException.class,
        DataHiveException.class,
        CommunityException.class
      })
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public ResponseBean<JsonNode> handleInternalServerErrorException(Throwable e) {
    return handleError(e, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(
      value = {IllegalArgumentException.class, PqmException.class, EerpException.class})
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  @ResponseBody
  public ResponseBean<JsonNode> handleIllegalArgumentException(Throwable e) {
    return handleError(e, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(
      value = {
        MalformedJwtException.class,
        SecurityException.class,
        ExpiredJwtException.class,
        SignatureException.class,
        AuthenticationException.class
      })
  @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
  @ResponseBody
  public ResponseBean<JsonNode> handleAuthenticationException(Throwable e) {
    return handleError(e, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(value = {DuplicationException.class})
  @ResponseStatus(value = HttpStatus.ACCEPTED)
  @ResponseBody
  public ResponseBean<JsonNode> handleDuplicationException(Throwable e) {
    return handleError(e, HttpStatus.ACCEPTED);
  }

  @ExceptionHandler(value = {MailSendException.class, AddressException.class})
  @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
  @ResponseBody
  public ResponseBean<JsonNode> handleMailException(Throwable e) {
    return handleError(e, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(value = {UnauthorizedException.class})
  @ResponseStatus(value = HttpStatus.FORBIDDEN)
  @ResponseBody
  public ResponseBean<JsonNode> handleUnauthorizedException(Throwable e) {
    return handleError(e, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(
      value = {CreationException.class, UpdateFailedException.class, NoSuchElementException.class})
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  @ResponseBody
  public ResponseBean<JsonNode> handleCreationException(Throwable e) {
    return handleError(e, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(value = {UpdateConflictException.class})
  @ResponseStatus(value = HttpStatus.CONFLICT)
  @ResponseBody
  public ResponseBean<JsonNode> handleUpdateConflictException(Throwable e) {
    return handleError(e, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(value = {NoContentException.class})
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  @ResponseBody
  public ResponseBean<JsonNode> handleNoContentException(Throwable e) {
    return handleError(e, HttpStatus.NO_CONTENT);
  }

  @ExceptionHandler(value = {MaxUploadSizeExceededException.class})
  @ResponseStatus(value = HttpStatus.PAYLOAD_TOO_LARGE)
  @ResponseBody
  public ResponseBean<JsonNode> handleMaxUploadSizeExceededException(Throwable e) {
    return handleError(e, HttpStatus.PAYLOAD_TOO_LARGE);
  }

  @ExceptionHandler(value = {HttpClientErrorException.class})
  @ResponseBody
  public ResponseBean<JsonNode> handleHttpClientErrorException(
      HttpServletRequest request, HttpServletResponse response, Throwable e) {
    response.setStatus(((HttpClientErrorException) e).getStatusCode().value());
    return handleError(e, ((HttpClientErrorException) e).getStatusCode());
  }

  @ExceptionHandler(value = {RuntimeException.class})
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public ResponseBean<JsonNode> handleRunTimeException(Throwable e) {
    return handleError(e, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    return new ResponseEntity<>(ex.getMessage(), headers, status);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    return new ResponseEntity<>(ex.getMessage(), headers, status);
  }

  private ResponseBean<JsonNode> handleError(Throwable e, HttpStatus status) {
    log.error(null, null, String.valueOf(status), e.getMessage(), e);
    ResponseBean<JsonNode> res = new ResponseBean<>();
    if (HttpStatus.INTERNAL_SERVER_ERROR == status) {
      // filter all message if it's an internal server error
      res.setMessage("Oops, something went wrong, please try again later.");
    } else if (e instanceof HttpClientErrorException) {
      // filter status code prefix thrown by HttpClientErrorException
      res.setMessage(StringUtils.substringAfter(e.getMessage(), " "));
    } else {
      res.setMessage(e.getMessage());
    }
    return res;
  }

  public static void handleFilterException(HttpServletResponse response, String message)
      throws IOException {
    ResponseBean<JsonNode> res = new ResponseBean<>();
    res.setMessage(message);
    response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
    response.setStatus(HttpStatus.BAD_REQUEST.value());
    response.getWriter().write(new ObjectMapper().writeValueAsString(res));
    response.getWriter().flush();
  }
}
