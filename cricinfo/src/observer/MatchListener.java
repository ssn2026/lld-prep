package observer;

public interface MatchListener {
    void onWicketFallen(String battingTeam, int wickets, int runs);

    void onInningsComplete(String battingTeam, int runs, int wickets, String overs);

    void onMatchComplete(String resultSummary);
}
