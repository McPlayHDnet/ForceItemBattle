package forceitembattle.ceremony;

import static forceitembattle.ceremony.StageTimelineTest.backToBack;
import static forceitembattle.ceremony.StageTimelineTest.joker;
import static forceitembattle.ceremony.StageTimelineTest.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.BackToBack;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Rarity;
import forceitembattle.model.ScoreOwner;
import forceitembattle.model.Team;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class SummaryCardTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ScoreOwner solo(ForceItem... items) {
        ForceItemPlayer player = new ForceItemPlayer(this.server.addPlayer("Solo"), Material.STONE, 0, 0);
        for (ForceItem item : items) {
            player.scoreOwner().record(item);
        }
        return player.scoreOwner();
    }

    private static ForceItem found(Material material) {
        return new ForceItem(material, "00:42", 0L, new BackToBack(false), false, null);
    }

    @Test
    void countsItemsAndChainsByRarity() {
        ScoreOwner owner = solo(plain(), joker(), backToBack(Rarity.RARE), backToBack(Rarity.RARE), backToBack(Rarity.LEGENDARY));

        List<String> lines = SummaryCard.linesOf(owner, List.of(1L, 2L, 3L, 4L, 5L));

        assertTrue(lines.contains("<gray>Items <white>5"));
        assertFalse(lines.stream().anyMatch(line -> line.contains("Joker")));
        assertTrue(lines.contains("<gray>Back-to-backs <white>3"));
        assertTrue(lines.contains("  " + Rarity.RARE.displayName() + " <white>×2"));
        assertTrue(lines.contains("  " + Rarity.LEGENDARY.displayName() + " <white>×1"));
        assertFalse(lines.stream().anyMatch(line -> line.contains(Rarity.EPIC.displayName())));
    }

    @Test
    void namesTheLongestAndTheFastestFind() {
        ScoreOwner owner = solo(found(Material.STONE), found(Material.DIAMOND), found(Material.DIRT));

        List<String> lines = SummaryCard.linesOf(owner, List.of(90L, 754L, 0L));

        assertTrue(lines.contains("<gray>Longest <white>Diamond <gold>12m 34s"), lines.toString());
        assertTrue(lines.contains("<gray>Fastest <white>Dirt <gold>0s"), lines.toString());
    }

    /** A back-to-back always takes 0s, so counting it would make every fastest find one. */
    @Test
    void theFastestFindIsOneThatWasEarned() {
        ScoreOwner owner = solo(found(Material.STONE), backToBack(Rarity.RARE), joker(), found(Material.DIRT));

        List<String> lines = SummaryCard.linesOf(owner, List.of(300L, 0L, 5L, 45L));

        assertTrue(lines.contains("<gray>Fastest <white>Dirt <gold>45s"), lines.toString());
    }

    @Test
    void onlyChainsAndJokersMeansNoFastest() {
        ScoreOwner owner = solo(backToBack(Rarity.RARE), joker());

        List<String> lines = SummaryCard.linesOf(owner, List.of(0L, 90L));

        assertTrue(lines.stream().anyMatch(line -> line.startsWith("<gray>Longest")));
        assertFalse(lines.stream().anyMatch(line -> line.contains("Fastest")), lines.toString());
    }

    @Test
    void noFindsMeansNoLongestOrFastest() {
        List<String> lines = SummaryCard.linesOf(solo(), List.of());

        assertTrue(lines.contains("<gray>Items <white>0"));
        assertFalse(lines.stream().anyMatch(line -> line.contains("Longest") || line.contains("Fastest")));
    }

    @Test
    void aTeamListsEachMembersFinds() {
        PlayerMock alice = this.server.addPlayer("Alice");
        PlayerMock bob = this.server.addPlayer("Bob");
        Team team = new Team(1, Material.STONE, 0, 0,
                new ForceItemPlayer(alice, Material.STONE, 0, 0),
                new ForceItemPlayer(bob, Material.STONE, 0, 0));
        team.record(new ForceItem(Material.STONE, "00:42", 0L, new BackToBack(false), false, alice.getUniqueId()));
        team.record(new ForceItem(Material.DIRT, "00:42", 0L, new BackToBack(false), false, alice.getUniqueId()));

        List<String> lines = SummaryCard.linesOf(team, List.of(1L, 1L));

        int finds = lines.indexOf("<gray>Finds");
        assertEquals(List.of("  <white>Alice <gold>2", "  <white>Bob <gold>0"), lines.subList(finds + 1, finds + 3));
    }
}
