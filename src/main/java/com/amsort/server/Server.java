package com.amsort.server;

import com.amsort.Vote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class Server {
    /**
     * how many second should Server wait for votes
     */
    private static final int WAIT_FOR_VOTES = 45;
    /**
     * how many second should Server wait for message from client
     * after that time client gets disconnected
     */
    private static final int INACTIVE_TIME = 60;
    private static final int PORT_NUMBER = 8888;
    private static final int MINIMAL_NUMBER_OF_CLIENTS = 3;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Set<Vote> activeVotes = new HashSet<>();
    private VoteTerminator voteTerminator;
    private int numberOfNamedClients;

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
    public class ClientHandler extends Thread {
        private final Socket clientSocket;
        private String clientName = "";
        PrintWriter out = null;
        BufferedReader in = null;
        private final Timer terminate = new Timer();

        @Override
        public void run() {
            while (true) {
                try {
                   clientSocket.setSoTimeout(INACTIVE_TIME*1000);
                   String clientData = in.readLine();
                    if (clientData != null) {
                        String[] command = clientData.split(" ");
                        recognizeCommand(command);
                    }
                } catch (IOException e) {
                    System.out.println("Client disconnected");
                    numberOfNamedClients--;
                    clients.remove(this); //remove client from client list
                    break; //finishing run method,
                }
            }

        }

        /**
         * Create class representing connection to a client
         * @param socket socket connected to client socket
         */
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
                        sendNOK("invalid syntax, usage NODE nodeName");
                        break;
                    }
                    setClientName(command[1]);
                    break;
                case "NEW":
                    if (command.length < 4) {
                        sendNOK("invalid syntax, usage NEW voteName initial vote(Y/N) content of the vote");
                        break;
                    }
                    StringBuilder voteContent = new StringBuilder();
                    for(int i = 3; i < command.length; i++){
                        voteContent.append(command[i]);
                        if(i != command.length-1)
                            voteContent.append(" ");
                    }
                    createVote(command[1], command[2], voteContent.toString());
                    break;
                case "VOTE":
                    if (command.length != 3) {
                        sendNOK("invalid syntax, usage VOTE voteName Y/N");
                    }
                    vote(command[1], command[2]);
                default:
                    sendNOK("there is such command as " + command[0] + " available commands : PING, NODE, VOTE, NEW");
            }
        }

        private void vote(final String voteName, final String clientVote) {
            if(this.clientName.equals("")){
                sendNOK("Before you can vote, set your name using NODE [name] command");
                return;
            }
            Vote vote = findVoteByName(voteName);
            if(vote == null){
                sendNOK("There is no vote for " + voteName);
                return;
            }
            vote.makeVote(clientVote);
            sendVOTE(vote.getVoteName(),this.clientName, clientVote);
            if(vote.getNumberOfVotes() == clients.size()){
                terminateVote(vote);
            }
        }

        private void sendVOTE(String voteName, String clientName, String clientVote) {
            sendToAllMessage("VOTE " + voteName + " " + clientName + " " + clientVote);
        }

        private Vote findVoteByName(final String voteName) {
            for(Vote vote : activeVotes){
                if(vote.getVoteName().equalsIgnoreCase(voteName))
                    return vote;
            }
            return null;
        }
        /**
         * checks if name given is uniqe
         * if is uniqe assign it to {@link ClientHandler#clientName}
         * @param name name to assign
         */
        private void setClientName(final String name) {
            final String potentialName = name.toUpperCase();
            boolean isUniqe = clients
                    .stream()
                    .map(client -> client.clientName.toUpperCase())
                    .anyMatch(s -> s.equals(potentialName));
            if (!isUniqe) {
                this.clientName = name;
                sendMessage("Your name is set to: " + this.clientName);
                numberOfNamedClients++;
            } else {
                sendNOK("Name: " + name + " is already used, pick another");
            }
        }

        private void createVote(final String voteName, final String initialVote, final String content) {
            if(numberOfNamedClients < MINIMAL_NUMBER_OF_CLIENTS){
                sendNOK("Minimal number of clients required to start a vote is " + MINIMAL_NUMBER_OF_CLIENTS);
                return;
            }
            if (!initialVote.equalsIgnoreCase("Y") && !initialVote.equalsIgnoreCase("N")) {
                sendNOK("Invalid vot, pool not created");
                return;
            }
            Vote vote = new Vote(voteName, initialVote, content);
            if (!activeVotes.add(vote)) {
                sendNOK("Vote for " +vote.getVoteName()+ " is active");
                return;
            }
            sendNew(vote.getVoteName(), vote.getVoteContent());
            voteTerminator = new VoteTerminator(vote,this);
            terminate.schedule(voteTerminator,WAIT_FOR_VOTES * 1000);
        }


        /**
         * check if vote is still active, if yes remove it from {@link Server#activeVotes}
         * @param vote vot to terminate
         */
        public synchronized void terminateVote(Vote vote){
            if(!activeVotes.contains(vote))
                return;

            voteTerminator.cancel();
            terminate.purge();
            String voteResult = vote.getResult();
            activeVotes.remove(vote);
            sendResult(voteResult, vote);
        }

        private void sendNew(final String voteName, final String voteContent) {
            final String msg = "NEW " + this.clientName + " " + voteName + " " + voteContent;
            sendToAllMessage(msg);
        }

        private void sendToAllMessage(final String msg) {
            clients.forEach(c -> c.sendMessage(msg));
        }

        private void sendNOK(final String msg) {
            sendMessage("NOK " + msg);
        }

        private void sendMessage(final String message) {
            out.println(message);
        }

        private void sendResult(String voteResult, Vote vote) {
            sendToAllMessage("Result " + vote.getVoteName() + " " + voteResult);
        }
    }
}
