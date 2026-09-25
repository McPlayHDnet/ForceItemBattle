package forceitembattle.manager;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.achievements.AchievementManager;
import forceitembattle.ceremony.ResultStage;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ResultCeremony;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundClock;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Team;
import forceitembattle.randomevents.RandomEventManager;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibMatchHistoryClient;
import forceitembattle.service.StatisticsWrites;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Which participants a finished round is recorded for. */
class GamemanagerFinishTest {

    private ServerMock server;
    private Roster roster;
    private GameSettings settings;
    private TeamsManager teams;
    private StatisticsWrites stats;
    private Gamemanager gamemanager;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.roster = new Roster();
        this.settings = mock(GameSettings.class);
        this.teams = mock(TeamsManager.class);
        this.stats = mock(StatisticsWrites.class);
        when(this.settings.isSettingEnabled(GameSetting.STATS)).thenReturn(true);

        FIBServiceClient fibService = mock(FIBServiceClient.class);
        when(fibService.matchHistory()).thenReturn(mock(FibMatchHistoryClient.class));
        when(fibService.statisticsWrites()).thenReturn(this.stats);

        this.gamemanager = new Gamemanager(MockBukkit.createMockPlugin(), this.roster, new RoundPhase(),
                this.settings, new RoundClock(), mock(ResultCeremony.class), mock(ResultStage.class),
                mock(ItemDifficultiesManager.class), mock(BackpackManager.class), mock(RecipeManager.class),
                mock(PositionManager.class), mock(ScoreboardManager.class), this.teams,
                mock(WanderingTraderManager.class), mock(RandomEventManager.class),
                mock(AchievementManager.class), fibService, location -> { });
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ForceItemPlayer join(String name) {
        PlayerMock player = this.server.addPlayer(name);
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, 0, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    @Test
    void aParticipantWhoLeftBeforeTheEndIsStillRecorded() {
        ForceItemPlayer stayed = join("Understudy1");
        ForceItemPlayer left = join("Understudy2");
        ((PlayerMock) left.player()).disconnect();

        this.gamemanager.finishGame();

        verify(this.stats).recordRoundFinished(eq(stayed), anyString(), anyInt(), anyLong(), anyBoolean());
        verify(this.stats).recordRoundFinished(eq(left), eq("Understudy2"), anyInt(), anyLong(), anyBoolean());
    }

    /** The team's win is written by one member only; if that member is away, it must still be written. */
    @Test
    void anAbsentTeammateIsRecordedAsHavingWon() {
        ForceItemPlayer stayed = join("Understudy1");
        ForceItemPlayer left = join("Understudy2");
        Team team = new Team(1, Material.DIRT, 5, 0, stayed, left);
        stayed.setCurrentTeam(team);
        left.setCurrentTeam(team);
        when(this.settings.isSettingEnabled(GameSetting.TEAM)).thenReturn(true);
        when(this.teams.getTeams()).thenReturn(List.of(team));
        ((PlayerMock) left.player()).disconnect();

        this.gamemanager.finishGame();

        verify(this.stats).recordRoundFinished(eq(left), anyString(), eq(5), anyLong(), eq(true));
    }

    @Test
    void aSpectatorIsNotRecorded() {
        join("Understudy1");
        ForceItemPlayer spectator = join("Understudy2");
        spectator.setSpectator(true);

        this.gamemanager.finishGame();

        verify(this.stats, never()).recordRoundFinished(eq(spectator), anyString(), anyInt(), anyLong(),
                anyBoolean());
    }
}
