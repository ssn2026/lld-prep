package model;

/**
 * One team's batting innings. Owns the invariants (over completion swaps
 * ends, a wicket brings in the next batsman, all-out/target detection) as
 * plain mutator methods; the actual per-delivery decision of WHICH methods
 * to call in what order lives in command/ (Command pattern), not here.
 */
public class Innings {
    private final Team battingTeam;
    private final String bowlingTeamName;
    private final int oversLimit;
    private final Integer target;

    private int runs = 0;
    private int wickets = 0;
    private int legalBallsThisOver = 0;
    private int completedOvers = 0;
    private int strikerIndex = 0;
    private int nonStrikerIndex = 1;
    private int nextBatsmanIndex = 2;

    public Innings(Team battingTeam, String bowlingTeamName, int oversLimit, Integer target) {
        this.battingTeam = battingTeam;
        this.bowlingTeamName = bowlingTeamName;
        this.oversLimit = oversLimit;
        this.target = target;
    }

    public void addRuns(int amount) {
        runs += amount;
    }

    public void swapEnds() {
        int tmp = strikerIndex;
        strikerIndex = nonStrikerIndex;
        nonStrikerIndex = tmp;
    }

    public void recordLegalBall() {
        legalBallsThisOver++;
        if (legalBallsThisOver == 6) {
            completedOvers++;
            legalBallsThisOver = 0;
            swapEnds();
        }
    }

    public void recordWicket() {
        wickets++;
        if (nextBatsmanIndex < battingTeam.getPlayers().size()) {
            strikerIndex = nextBatsmanIndex;
            nextBatsmanIndex++;
        }
    }

    public boolean isAllOut() {
        return wickets >= battingTeam.getPlayers().size() - 1;
    }

    public boolean isOversComplete() {
        return completedOvers >= oversLimit;
    }

    public boolean hasReachedTarget() {
        return target != null && runs > target;
    }

    public String getOversDisplay() {
        return completedOvers + "." + legalBallsThisOver;
    }

    public Team getBattingTeam() {
        return battingTeam;
    }

    public String getBowlingTeamName() {
        return bowlingTeamName;
    }

    public int getRuns() {
        return runs;
    }

    public int getWickets() {
        return wickets;
    }

    public Integer getTarget() {
        return target;
    }

    public String getStrikerName() {
        return battingTeam.getPlayers().get(strikerIndex);
    }
}
