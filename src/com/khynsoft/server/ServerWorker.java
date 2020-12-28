package com.khynsoft.server;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerWorker implements Runnable {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final BufferedReader in;
    private final PrintWriter out;
    private final Socket socket;

    private String username;
    private final int id;

    public ServerWorker(Socket client, int id) throws IOException {
        socket = client;
        this.id = id;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);
    }

    public void start() {
        Thread worker = new Thread(this);
        worker.start();
        running.set(true);
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");

        try {
            //Username init
            out.println("[Server]: What is your name? [Enter you name]");
            username = "";
            
            while (running.get()) {
                username = in.readLine();
                if(username.isEmpty() || Server.usernames.stream().anyMatch(name -> name.contains(username))){
                    out.println("This username is already in the server, please choose one that is unique.");
                    username = "";
                } else {
                    Server.usernames.add(username);
                    break;
                }
            }

            System.out.printf("[%s] is now connected to the server.\n", username);
            System.out.printf("There are now %d online user/s in the server.\n", Server.userOutputStreams.size());

            out.println("\n##############  ##############  #################\n" +
                    "##############  ##############  #################\n" +
                    "[[===========]][Group Chat Server][[===========]]\n" +
                    "##############  ##############  #################\n" +
                    "##############  ##############  #################\n");
            out.println("[[====]] Howdy " + username +
                    ", there are " + Server.userOutputStreams.size() +
                    " online user/s, say Hi to them! [[====]]\n");

            //Notify that the user joined the chat
            for (PrintWriter usersOut : Server.userOutputStreams.values()) {
                usersOut.printf("[%s] %s joined the chat!\n",
                        timeFormatter.format(LocalDateTime.now()),
                        username);
            }
            System.out.printf("[%s] %s logged joined the chat!\n",
                    timeFormatter.format(LocalDateTime.now()),
                    username);
        } catch (IOException e) {
            System.err.printf("[%s] [%s] closed its socket, logging user out...\n",
                    timeFormatter.format(LocalDateTime.now()),
                    username);
            Server.userOutputStreams.remove(id);
            Server.usernames.remove(username);
            stop();
        } catch (Exception exception) {
            Server.userOutputStreams.remove(id);
            Server.removeUsername(username);
            Thread.currentThread().interrupt();
            running.set(false);
        }

        while (running.get() && !socket.isClosed()) {
            try {
                String msg = in.readLine();
                if (msg.equals("/stopServer")) {
                    for (PrintWriter usersOut : Server.userOutputStreams.values()) {
                        usersOut.printf("[%s] %s is killing the server.\n",
                                timeFormatter.format(LocalDateTime.now()),
                                username);
                    }
                    System.err.printf("[%s] %s is killing the server.\n",
                            timeFormatter.format(LocalDateTime.now()),
                            username);
                    Server.stop();
                    break;
                } else if (msg.equals("/logout")) {
                    for (PrintWriter usersOut : Server.userOutputStreams.values()) {
                        usersOut.printf("[%s] %s logged out in the server.\n",
                                timeFormatter.format(LocalDateTime.now()),
                                username);
                    }
                    Server.userOutputStreams.remove(id);
                    Server.removeUsername(username);
                    Thread.currentThread().interrupt();
                    break;
                } else {
                    for (PrintWriter usersOut : Server.userOutputStreams.values()) {
                        usersOut.printf("[%s] %s: %s\n",
                                timeFormatter.format(LocalDateTime.now()),
                                username, msg);
                    }
                    System.out.printf("[%s] %s: %s\n",
                            timeFormatter.format(LocalDateTime.now()),
                            username, msg);
                }
            } catch (IOException e) {
                Thread.currentThread().interrupt();
                System.err.println("Client: " + username + " forcibly left from the server due to " + e.getMessage());
                Server.userOutputStreams.remove(id);
                Server.removeUsername(username);
                stop();
            } catch (Exception exception) {
                Server.userOutputStreams.remove(id);
                Server.removeUsername(username);
                Thread.currentThread().interrupt();
                running.set(false);
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.printf("[%s] Server: %s has been disconnected to the server.\n",
                timeFormatter.format(LocalDateTime.now()),
                username);
        System.out.println("["+ timeFormatter.format(LocalDateTime.now()) +
                "] Server: There are only " + Server.userOutputStreams.size() + " online user/s left in the server.");
    }
}
