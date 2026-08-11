package observer;

public class ConsoleMatchListener implements MatchListener {
    @Override
    public void onWicketFallen(String battingTeam, int wickets, int runs) {
        System.out.println("[listener] WICKET! " + battingTeam + " " + wickets + " down, " + runs + " runs");
    }

    @Override
    public void onInningsComplete(String battingTeam, int runs, int wickets, String overs) {
        System.out.println("[listener] innings complete: " + battingTeam + " " + runs + "/" + wickets + " (" + overs + " overs)");
    }

    @Override
    public void onMatchComplete(String resultSummary) {
        System.out.println("[listener] match complete: " + resultSummary);
    }
}
