package com.ebay.npd.client;

import com.ebay.npd.helloworld.GreeterGrpc;
import com.ebay.npd.helloworld.HelloReply;
import com.ebay.npd.helloworld.HelloRequest;
import com.ebay.npd.server.HelloWorldServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /** Construct client connecting to HelloWorld server at {@code host:port}. */
    public HelloWorldClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build());
    }

    /** Construct client for accessing HelloWorld server using the existing channel. */
    HelloWorldClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Say hello to server. */
    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }

    /** Say hello to server and receive a streaming reply. */
    public void streamingGreet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        Iterator<HelloReply> responseIterator;
        try {
            responseIterator = blockingStub.sayHelloResponseStreaming(request);
            while(responseIterator.hasNext()) {
                HelloReply reply = responseIterator.next();
                logger.info("Greeting: " + reply.getMessage());
            }

        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws Exception {
        String serverAddr = System.getenv().getOrDefault("SERVER_ADDR", "localhost:50051");
        String[] serverAddrParams = serverAddr.split(":");
        HelloWorldClient client = new HelloWorldClient(serverAddrParams[0], Integer.parseInt(serverAddrParams[1]));
        try {
            /* Access a service running on the local machine on port 50051 */
            final String user = "world";
            logger.info("Sending a greet and expect receiving one greet");
            client.greet(user);

            IntStream.range(0,100).forEach(
                    i -> {
                        logger.info("Sending a greet and expect receiving multiple greets");
                        client.streamingGreet(user+i);
                    }
            );

        } finally {
            client.shutdown();
        }
    }
}
