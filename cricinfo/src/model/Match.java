package model;

public class Match {
    private final Team teamA;
    private final Team teamB;
    private final int oversLimit;
    private Innings innings1;
    private Innings innings2;
    private String result;

    public Match(Team teamA, Team teamB, int oversLimit) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.oversLimit = oversLimit;
    }

    public Team getTeamA() {
        return teamA;
    }

    public Team getTeamB() {
        return teamB;
    }

    public int getOversLimit() {
        return oversLimit;
    }

    public Innings getInnings1() {
        return innings1;
    }

    public void setInnings1(Innings innings1) {
        this.innings1 = innings1;
    }

    public Innings getInnings2() {
        return innings2;
    }

    public void setInnings2(Innings innings2) {
        this.innings2 = innings2;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
