import java.net.URI;
import java.net.http.*;
import java.time.Duration;
/** Container health probe; no curl installation or shell dependency in the runtime. */
public final class HealthCheck {
 public static void main(String[] args) throws Exception {
  var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/actuator/health")).timeout(Duration.ofSeconds(5)).build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()!=200 || !response.body().contains("UP"))System.exit(1);
 }
}
