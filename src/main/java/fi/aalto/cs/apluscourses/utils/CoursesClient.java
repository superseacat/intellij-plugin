package fi.aalto.cs.apluscourses.utils;

import fi.aalto.cs.apluscourses.model.InvalidAuthenticationException;
import fi.aalto.cs.apluscourses.model.UnexpectedResponseException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class with methods for getting resources from a remote. For most use cases, the {@link
 * CoursesClient#fetch} methods are sufficient. The {@link CoursesClient#fetchAndMap} and {@link
 * CoursesClient#fetchAndConsume} methods can be used when direct access to the input stream of the
 * response is needed.
 */
public class CoursesClient {

  /**
   * Makes a GET request to the given URL and returns the response body in a
   * {@link ByteArrayInputStream}.
   *
   * @param url          The URL to which the request is made.
   * @return             A {@link ByteArrayInputStream} containing the response body.
   * @throws IOException If an error (e.g. network error) occurs while downloading the file. This is
   *                     an instance of {@link UnexpectedResponseException} if the status code of
   *                     the response isn't 2xx or the response is missing a body.
   */
  @NotNull
  public static ByteArrayInputStream fetch(@NotNull URL url) throws IOException {
    return fetchAndMap(url, null,
        entity -> new ByteArrayInputStream(EntityUtils.toByteArray(entity)));
  }

  /**
   * Makes a GET request to the given URL with the given authentication in the request and returns
   * the response body in a {@link ByteArrayInputStream}.
   *
   * @param url            The URL to which the request is made.
   * @param authentication The authentication that gets added to the request.
   * @return               A {@link ByteArrayInputStream} containing the response body.
   * @throws IOException   If an error (e.g. network error) occurs while downloading the file. This
   *                       is an instance of {@link InvalidAuthenticationException} if the response
   *                       status code is 401 or 403.
   */
  @NotNull
  public static ByteArrayInputStream fetch(@NotNull URL url,
                                           @NotNull Authentication authentication)
      throws IOException {
    return fetchAndMap(url, authentication,
        entity -> new ByteArrayInputStream(EntityUtils.toByteArray(entity)));
  }

  /**
   * Downloads a file from the given URL into the given file.
   *
   * @throws IOException                 If an error (e.g. network error) occurs while downloading
   *                                     the file. This is an instance of {@link
   *                                     UnexpectedResponseException} if the status code of the
   *                                     response isn't 2xx or the response is missing a body.
   */
  public static void fetch(@NotNull URL url, @NotNull File file) throws IOException {
    fetchAndConsume(url, null,
        entity -> FileUtils.copyInputStreamToFile(entity.getContent(), file));
  }

  /**
   * A functional interface for adding authentication to an HTTP request.
   */
  @FunctionalInterface
  public interface Authentication {
    void addToRequest(HttpRequest request);
  }

  /**
   * A functional interface for functions that map a {@link HttpEntity} to a desired result. See
   * {@link EntityUtils} for useful methods for working with {@link HttpEntity} instances.
   */
  @FunctionalInterface
  public interface EntityMapper<T> {
    T map(@NotNull HttpEntity entity) throws IOException;
  }

  /**
   * A functional interface for functions that consume a {@link HttpEntity} and use it for
   * side-effects.
   */
  @FunctionalInterface
  public interface EntityConsumer {
    void consume(@NotNull HttpEntity entity) throws IOException;
  }

  /**
   * Makes a GET request to the given URL and returns the response body.
   *
   * @param url            The URL to which the GET request is made.
   * @param authentication An instance of {@link Authentication} that gets added to the request, or
   *                       null if no authentication should be added.
   * @param mapper         A {@link EntityMapper} that converts the {@link HttpEntity} containing
   *                       the response body to the desired format.
   * @return The result of {@code mapper.map(response)}, where response is a {@link HttpEntity}
   *         containing the response body.
   * @throws IOException If an issue occurs while making the request, which includes cases such as
   *                     an unknown host. This is an instance of {@link UnexpectedResponseException}
   *                     if the status code isn't 2xx or the response is missing a body.
   */
  public static <T> T fetchAndMap(@NotNull URL url,
                                  @Nullable Authentication authentication,
                                  @NotNull EntityMapper<T> mapper) throws IOException {
    HttpGet request = new HttpGet(url.toString());
    if (authentication != null) {
      authentication.addToRequest(request);
    }
    return mapResponseBody(request, mapper);
  }

  /**
   * Makes a GET request to the given URL and consumes the response body.
   *
   * @param url            The URL to which the GET request is made.
   * @param authentication An instance of {@link Authentication} that gets added to the request, or
   *                       null if no authentication should be added.
   * @param consumer       A {@link EntityConsumer} that consumes the {@link HttpEntity} containing
   *                       the response body.
   * @throws IOException If an issue occurs while making the request, which includes cases such as
   *                     an unknown host. This is an instance of {@link UnexpectedResponseException}
   *                     if the status code isn't 2xx or the response is missing a body.
   */
  public static void fetchAndConsume(@NotNull URL url,
                                     @Nullable Authentication authentication,
                                     @NotNull EntityConsumer consumer) throws IOException {
    HttpGet request = new HttpGet(url.toString());
    if (authentication != null) {
      authentication.addToRequest(request);
    }
    consumeResponseBody(request, consumer);
  }

  /**
   * Executes the given request, performs some checks on the response and returns the result of
   * passing the response body to the given mapper.
   */
  private static <T> T mapResponseBody(@NotNull HttpUriRequest request,
                                       @NotNull EntityMapper<T> mapper) throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault();
         CloseableHttpResponse response = client.execute(request)) {
      requireSuccessStatusCode(response);
      requireResponseEntity(response);
      return mapper.map(response.getEntity());
    }
  }

  /**
   * Executes the given request, performs some checks on the response and passes the response body
   * to the given consumer.
   */
  private static void consumeResponseBody(@NotNull HttpUriRequest request,
                                          @NotNull EntityConsumer consumer) throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault();
         CloseableHttpResponse response = client.execute(request)) {
      requireSuccessStatusCode(response);
      requireResponseEntity(response);
      consumer.consume(response.getEntity());
    }
  }

  /**
   * Throws a {@link InvalidAuthenticationException} if the response status code is 401 or 403.
   * Throws a {@link UnexpectedResponseException} if the response status code isn't 2xx. Otherwise
   * does nothing.
   */
  @NotNull
  private static void requireSuccessStatusCode(@NotNull HttpResponse response)
      throws UnexpectedResponseException {
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == 401 || statusCode == 403) { // TODO: should 403 be removed from this check?
      throw new InvalidAuthenticationException(response, "Invalid authentication");
    } else if (statusCode < 200 || statusCode >= 300) {
      throw new UnexpectedResponseException(response, "Status code doesn't indicate success");
    }
  }

  /**
   * Throws a {@link UnexpectedResponseException} if the response entity is null.
   */
  private static void requireResponseEntity(@NotNull HttpResponse response)
      throws UnexpectedResponseException {
    if (response.getEntity() == null) {
      throw new UnexpectedResponseException(response, "Response is missing body");
    }
  }
}
