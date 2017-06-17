package com.cassiomolin.example;

import com.cassiomolin.example.api.JerseyConfig;
import com.cassiomolin.example.api.endpoints.PushEndpoint;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jboss.weld.environment.servlet.Listener;

import javax.servlet.ServletException;

import java.util.logging.Logger;

import static io.undertow.servlet.Servlets.listener;

/**
 * Application entry point.
 *
 * @author cassiomolin
 */
public class Application {

    private static Undertow server;

    private static DeploymentManager deploymentManager;

    private static final int DEFAULT_PORT = 8080;

    private static final Logger logger = Logger.getLogger(PushEndpoint.class.getName());

    /**
     * Start server on the port 8080.
     *
     * @param args
     */
    public static void main(final String[] args) {
        startServer(DEFAULT_PORT);
    }

    /**
     * Start server on the given port.
     *
     * @param port
     */
    public static void startServer(int port) {

        logger.info(String.format("Starting server on port %d", port));

        PathHandler path = Handlers.path();

        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(path)
                .build();

        server.start();

        logger.info(String.format("Server started on port %d", port));

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(Application.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("application.war")
                .addListeners(listener(Listener.class))
                .addServlets(
                        Servlets.servlet("jerseyServlet", ServletContainer.class)
                                .setLoadOnStartup(1)
                                .addInitParam("javax.ws.rs.Application", JerseyConfig.class.getName())
                                .addMapping("/api/*"))
                .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                                .setBuffers(new DefaultByteBufferPool(true, 100))
                                .addEndpoint(PushEndpoint.class));

        logger.info("Starting application deployment");

        deploymentManager = Servlets.defaultContainer().addDeployment(servletBuilder);
        deploymentManager.deploy();

        try {
            path.addPrefixPath("/", deploymentManager.start());
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

        logger.info("Application deployed");
    }

    /**
     * Stop server.
     */
    public static void stopServer() {

        if (server == null) {
            throw new IllegalStateException("Server has not been started yet");
        }

        logger.info("Stopping server");

        deploymentManager.undeploy();
        server.stop();

        logger.info("Server stopped");
    }
}