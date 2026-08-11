package services;

import command.BallCommand;
import command.WicketBallCommand;
import exceptions.IllegalMatchOperationException;
import factory.BallCommandFactory;
import java.util.List;
import model.Innings;
import model.Match;
import model.MatchStatus;
import model.Team;
import observer.MatchListener;
import state.MatchState;
import state.NotStartedState;

/**
 * The single public entry point. Owns the Match, the current MatchState
 * (match-phase lifecycle, held once here since there's one match per
 * service instance), and dispatches each delivery through a BallCommand
 * built by BallCommandFactory.
 */
public class CricInfoService {
    private Team teamA;
    private Team teamB;
    private Match match;
    private MatchState state = NotStartedState.INSTANCE;
    private final List<MatchListener> listeners = new java.util.ArrayList<>();

    public void addListener(MatchListener listener) {
        listeners.add(listener);
    }

    public void setTeams(String teamAName, List<String> playersA, String teamBName, List<String> playersB) {
        this.teamA = new Team(teamAName, playersA);
        this.teamB = new Team(teamBName, playersB);
    }

    public void startMatch(int oversLimit) {
        if (teamA == null || teamB == null) {
            throw new IllegalMatchOperationException("Cannot start a match before both teams are set");
        }
        state = state.startFirstInnings();
        match = new Match(teamA, teamB, oversLimit);
        match.setInnings1(new Innings(teamA, teamB.getName(), oversLimit, null));
    }

    public void recordBall(String type, int runs) {
        state.requireInningsInProgress();
        Innings innings = currentInnings();
        BallCommand command = BallCommandFactory.create(type, runs);
        command.execute(innings);
        if (command instanceof WicketBallCommand) {
            notifyWicketFallen(innings);
        }
        if (innings.isAllOut() || innings.isOversComplete() || innings.hasReachedTarget()) {
            notifyInningsComplete(innings);
            state = state.endInnings();
            if (state.getStatus() == MatchStatus.COMPLETED) {
                computeAndNotifyResult();
            }
        }
    }

    public void startSecondInnings() {
        state = state.startSecondInnings();
        Innings innings2 = new Innings(teamB, teamA.getName(), match.getOversLimit(), match.getInnings1().getRuns());
        match.setInnings2(innings2);
    }

    public String getScorecard() {
        StringBuilder sb = new StringBuilder();
        sb.append("Status: ").append(state.getStatus()).append('\n');
        appendInningsSummary(sb, match.getInnings1());
        if (match.getInnings2() != null) {
            appendInningsSummary(sb, match.getInnings2());
        }
        if (match.getResult() != null) {
            sb.append("Result: ").append(match.getResult());
        }
        return sb.toString().stripTrailing();
    }

    private void appendInningsSummary(StringBuilder sb, Innings innings) {
        sb.append(innings.getBattingTeam().getName()).append(": ")
                .append(innings.getRuns()).append('/').append(innings.getWickets())
                .append(" (").append(innings.getOversDisplay()).append(" overs)\n");
    }

    private Innings currentInnings() {
        return state.getStatus() == MatchStatus.INNINGS_1 ? match.getInnings1() : match.getInnings2();
    }

    private void computeAndNotifyResult() {
        int runs1 = match.getInnings1().getRuns();
        int runs2 = match.getInnings2().getRuns();
        String resultSummary;
        if (runs2 > runs1) {
            int wicketsInHand = (teamB.getPlayers().size() - 1) - match.getInnings2().getWickets();
            resultSummary = teamB.getName() + " won by " + wicketsInHand + " wicket(s)";
        } else if (runs1 > runs2) {
            resultSummary = teamA.getName() + " won by " + (runs1 - runs2) + " run(s)";
        } else {
            resultSummary = "Match tied";
        }
        match.setResult(resultSummary);
        notifyMatchComplete(resultSummary);
    }

    private void notifyWicketFallen(Innings innings) {
        for (MatchListener listener : listeners) {
            listener.onWicketFallen(innings.getBattingTeam().getName(), innings.getWickets(), innings.getRuns());
        }
    }

    private void notifyInningsComplete(Innings innings) {
        for (MatchListener listener : listeners) {
            listener.onInningsComplete(innings.getBattingTeam().getName(), innings.getRuns(),
                    innings.getWickets(), innings.getOversDisplay());
        }
    }

    private void notifyMatchComplete(String resultSummary) {
        for (MatchListener listener : listeners) {
            listener.onMatchComplete(resultSummary);
        }
    }
}
