package com.khynsoft.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private static AtomicBoolean running = new AtomicBoolean(false);

    public static HashMap<Integer, PrintWriter> userOutputStreams;
    public static Set<String> usernames = new HashSet<>();;

    public Server(int port) throws IOException {
        System.out.println("Establishing server...");
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server is now online.");
        running.set(true);

        userOutputStreams = new HashMap<>();
        int userId = 1;

        while(running.get()) {
            Socket client = server.accept();
            userOutputStreams.put(userId, new PrintWriter(client.getOutputStream(), true));
            new ServerWorker(client, userId++).start();
        }

    }

    public static void removeUsername(String username) {
        usernames.remove(username);
    }

    public static synchronized void stop() {
        System.out.println("Server is now offline.");
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            new Server(60000);
        } catch (IOException e) {
            System.err.println("Failed to establish the server. Probably there is another server is currently running right now.");
        }
    }

}
