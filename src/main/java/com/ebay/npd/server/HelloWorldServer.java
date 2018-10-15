package com.ebay.npd.server;

import com.ebay.npd.helloworld.GreeterGrpc;
import com.ebay.npd.helloworld.HelloReply;
import com.ebay.npd.helloworld.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class HelloWorldServer {
    private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());

    private static String self = "";


    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                HelloWorldServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        self = InetAddress.getLocalHost().getHostName();
        final HelloWorldServer server = new HelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            logger.info("sayHello: Got request with name : "+ request.getName()+ " from "+self);
            responseObserver.onCompleted();
        }

        @Override
        public void sayHelloResponseStreaming(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            logger.info("sayHelloResponseStreaming: Got request with name : "+ request.getName()+ " from "+self);
            IntStream.range(0, 10).forEach(
                    i -> {
                        HelloReply reply = HelloReply.newBuilder().setMessage(i + ": Hello " + request.getName() + " from "+self).build();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        responseObserver.onNext(reply);
                    }
            );
            responseObserver.onCompleted();
        }
    }

}
