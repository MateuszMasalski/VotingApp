package com.amsort;

import com.amsort.server.Server;

import java.util.List;

public class Vote {
    private final String voteName;
    private final String voteContent;
    private int yesVotes = 0;
    private int noVotes = 0;
    private final List<Server.ClientHandler> eligibleClients;

    public String getVoteName() {
        return voteName;
    }

    public List<Server.ClientHandler> getEligibleClients() {
        return eligibleClients;
    }

    public String getVoteContent() {
        return voteContent;
    }

    public Vote (String voteName, String initialVote, String voteContent, List<Server.ClientHandler> eligibleClients ){
            this.voteName = voteName;
            this.voteContent = voteContent;
            this.eligibleClients = eligibleClients;
            makeVote(initialVote);
    }

    public void makeVote(String vote) {
        if(vote.equalsIgnoreCase("Y"))
            yesVotes++;
        else
            noVotes++;
    }


    public String getResult(){
        if(yesVotes > noVotes)
            return "Y";
        return "N";
    }

    public int getNumberOfVotes(){
        return  yesVotes + noVotes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vote vote = (Vote) o;

        return this.voteName.equals(vote.voteName);
    }

    @Override
    public int hashCode() {
        int result = voteName != null ? voteName.hashCode() : 0;
        result = 31 * result + (voteContent != null ? voteContent.hashCode() : 0);
        return result;
    }
}
