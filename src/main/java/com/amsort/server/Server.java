package com.amsort.server;

import com.amsort.Vote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Server {
    private static final int WAIT_FOR_VOTES = 45;
    private static final int PORT_NUMBER = 8888;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Set<Vote> activeVotes = new HashSet<>();

    /**
     * It starts a server allowing clients to connect
     * server is running on port defined in PORT_NUM.
     * It stores every client in a list
     */
    @SuppressWarnings("InfiniteLoopStatement") //infinite loop is exactly what I want suppressing this for clarity
    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_NUMBER)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                client.start();

            }
        } catch (IOException e) {
            if (clients.isEmpty())
                throw new RuntimeException("Unable to start server"); //if there is none client throw runtime to exit
        }
    }

    /**
     * this class handles communication with clients connected to a server
     */
    private class ClientHandler extends Thread {
        private final Socket clientSocket;
        private String clientName = "";
        PrintWriter out = null;
        BufferedReader in = null;

        @Override
        public void run() {
            String clientData;
            while (true) {
                try {
                    clientData = in.readLine();
                    if (clientData != null) {
                        String[] command = clientData.split(" ");
                        recognizeCommand(command);
                    }
                } catch (IOException e) {
                    System.out.println("Client disconnected");
                    break; //finishing run method,
                }
            }

        }

        public ClientHandler(final Socket socket) {
            clientSocket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        /**
         * checks if received known command, if yes it calls method that execute this command
         *
         * @param command array of command
         */
        private void recognizeCommand(final String[] command) {
            switch (command[0].toUpperCase()) {
                case "PING":
                    sendMessage("PONG");
                    break;
                case "PONG":
                    break;
                case "NODE":
                    if (command.length != 2) {
                        sendNOR("invalid syntax, usage NODE nodeName");
                        break;
                    }
                    setClientName(command[1]);
                    break;
                case "NEW":
                    if (command.length != 4) {
                        sendNOR("invalid syntax, usage NEW voteName initial vote(Y/N) content of the vote");
                        break;
                    }
                    createVote(command[1], command[2], command[3]);
                    break;
                case "VOTE":
                    if (command.length != 3) {
                        sendNOR("invalid syntax, usage VOTE voteName Y/N");
                    }
                default:
                    sendNOR("there is such command as " + command[0] + " available commands : PING, NODE, VOTE, NEW");
            }
        }

        private void setClientName(final String name) {
            final String potentialName = name.toUpperCase();
            boolean isUniqe = clients
                    .stream()
                    .map(client -> client.clientName.toUpperCase())
                    .anyMatch(s -> s.equals(potentialName));
            if (!isUniqe) {
                this.clientName = name;
                sendMessage("Your is set to: " + this.clientName);
            } else {
                sendNOR("Name: " + name + " is already used, pick another");
            }
        }


        private void createVote(final String voteName, final String initialVote, final String content) {

            if (!initialVote.equalsIgnoreCase("Y") && !initialVote.equalsIgnoreCase("N")) {
                sendNOR("Invalid vot, pool not created");
                return;
            }
            Vote vote = new Vote(voteName, initialVote, content);

            if (!activeVotes.add(vote)) {
                sendNOR("This vote is active");
                return;
            }
            sendNew(vote.getVoteName(), vote.getVoteContent());
        }

        private void sendNew(final String voteName, final String voteContent) {
            final String msg = "NEW " + this.clientName + " " + voteName + " " + voteContent;
            sendToAllMessage(msg);
        }

        private void sendToAllMessage(final String msg) {
            clients.forEach(c -> c.sendMessage(msg));
        }

        private void sendNOR(final String msg) {
            sendMessage("NOR " + msg);
        }

        private void sendMessage(final String message) {
            out.println(message);
        }

    }
}
