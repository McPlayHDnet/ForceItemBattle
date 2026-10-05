package forceitembattle.model;

import java.util.List;
import org.bukkit.Material;

/** Every player holds one all round, so leaving a team falls back to exactly the values they had before. */
public class SoloScore extends ScoreState {

    private final ForceItemPlayer player;

    SoloScore(ForceItemPlayer player, Material currentMaterial, int remainingJokers, int currentScore) {
        super(currentMaterial, currentScore, remainingJokers);
        this.player = player;
    }

    @Override
    public List<ForceItemPlayer> members() {
        return List.of(this.player);
    }
}
