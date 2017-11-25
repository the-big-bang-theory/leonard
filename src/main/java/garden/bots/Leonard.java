package garden.bots;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.Record;

import java.util.Optional;

public class Leonard extends AbstractVerticle {
  
  private ServiceDiscovery discovery;
  private Record record;
  
  public void start() {
    
    discovery = Parameters.getServiceDiscovery(vertx);
    record = Parameters.getMicroServiceRecord();
    Integer httpPort = Parameters.getHttpPort();
    
    System.out.println("🎃  " + record.toJson().encodePrettily());
    
    /* add some metadata to the record */
    record.setMetadata(new JsonObject()
      .put("description", "Hello 🌍 I'm Leonard")
    );
    
    /* Define routes */
    Router router = Router.router(vertx);
    
    router.route().handler(BodyHandler.create());
    
    router.get("/hi").handler(context -> {
      
      context.response()
        .putHeader("content-type", "application/json;charset=UTF-8")
        .end(new JsonObject().put("quote", "People get things they don't deserve all the time. Like me with you.").encodePrettily());
      
    });
    
  
    /* 🚑 */
    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
    
    healthCheckHandler.register("how_are_you", future ->
      discovery.getRecord(r -> r.getRegistration().equals(record.getRegistration()), ar -> {
        if(ar.succeeded()) {
          future.complete();
        } else {
          System.out.println("😡 not in a good shape");
          ar.cause().printStackTrace();
          future.fail(ar.cause());
        }
      })
    );
    
    /* 🚑 */
    router.get("/health").handler(healthCheckHandler);
    
    
    /* serve static assets, see /resources/webroot directory */
    router.route("/*").handler(StaticHandler.create());
    
    /* Start the server */
    HttpServer server = vertx.createHttpServer();
  
    server
      .requestHandler(router::accept).listen(httpPort, result -> {
    
      if(result.succeeded()) {
        System.out.println("🌍 Listening on " + httpPort);
          
        /* then publish the microservice to the discovery backend */
        discovery.publish(record, asyncResult -> {
          if(asyncResult.succeeded()) {
            System.out.println("😃 Microservice is published! " + record.getRegistration());
          } else {
            System.out.println("😡 Not able to publish the microservice: " + asyncResult.cause().getMessage());
          }
        });
      
      } else {
        System.out.println("😡 Houston, we have a problem: " + result.cause().getMessage());
      }
    
    });
  }
  
  public void stop(Future<Void> stopFuture) {
  
    discovery.unpublish(record.getRegistration(), asyncResult -> {
      if(asyncResult.succeeded()) {
        System.out.println("👋 bye bye " + record.getRegistration());
      } else {
        System.out.println("😡 Not able to unpublish the microservice: " + asyncResult.cause().getMessage());
      }
      stopFuture.complete();
    });
  }
}
