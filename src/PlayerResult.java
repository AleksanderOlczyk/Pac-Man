import java.io.Serializable;

public class PlayerResult implements Serializable, Comparable<PlayerResult> {

    private String playerName;
    private int score;

    public PlayerResult(String playerName, int score) {
        this.playerName = playerName;
        this.score = score;
    }

    public String getName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    @Override
    public int compareTo(PlayerResult other) {
        return Integer.compare(other.score, this.score);
    }
}
