package model;

/** Immutable record of a completed move, kept for history and observer notifications. */
public final class Move {

    private final Position from;
    private final Position to;
    private final PieceType movedPieceType;
    private final Color movedBy;
    private final PieceType capturedPieceType;
    private final PieceType promotedTo;

    public Move(Position from, Position to, PieceType movedPieceType, Color movedBy,
            PieceType capturedPieceType, PieceType promotedTo) {
        this.from = from;
        this.to = to;
        this.movedPieceType = movedPieceType;
        this.movedBy = movedBy;
        this.capturedPieceType = capturedPieceType;
        this.promotedTo = promotedTo;
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public PieceType getMovedPieceType() {
        return movedPieceType;
    }

    public Color getMovedBy() {
        return movedBy;
    }

    public PieceType getCapturedPieceType() {
        return capturedPieceType;
    }

    public PieceType getPromotedTo() {
        return promotedTo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(movedBy).append(' ').append(movedPieceType).append(' ')
                .append(from.toAlgebraic()).append(capturedPieceType != null ? "x" : "-").append(to.toAlgebraic());
        if (capturedPieceType != null) {
            sb.append(" (captures ").append(capturedPieceType).append(')');
        }
        if (promotedTo != null) {
            sb.append(" (promotes to ").append(promotedTo).append(')');
        }
        return sb.toString();
    }
}
