package com.amsort.server;

import com.amsort.Vote;

import java.util.TimerTask;

/**
 * this class exist only to pass parameters to TimerTask
 */
public class VoteTerminator extends TimerTask {
    Vote vote;
    Server.ClientHandler client;

    /**
     *
     * @param vote vote to terminate
     * @param clientHandler handler which terminates voting
     */
    public VoteTerminator(Vote vote, Server.ClientHandler clientHandler){
        this.vote = vote;
        this.client = clientHandler;
    }


    @Override
    public void run() {
        this.client.terminateVote(vote);
    }
}
