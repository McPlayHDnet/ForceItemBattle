package forceitembattle.manager;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.model.Team;
import forceitembattle.util.TeamPairing;
import forceitembattle.util.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TeamsManager implements Manager {

    /**
     * Its own file rather than a key in config.yml: config.yml is a deployed artifact here — it ships
     * from the website repo because it carries the item descriptions — so anything the plugin writes
     * into it is overwritten by the next deploy, silently degrading the avoidance to a shuffle.
     */
    private static final String HISTORY_FILE = "team-history.yml";
    private static final String PAIRINGS_PATH = "lastPairings";
    private static final String LEGACY_CONFIG_PATH = "teams.lastPairings";

    private final JavaPlugin plugin;
    private final Roster roster;

    private final ScoreboardManager scoreboard;

    private final Map<ForceItemPlayer, Team> pendingInvite;
    @Getter
    private final List<Team> teams;
    @Getter
    private final int maxTeamSize;
    private final Set<String> previousPairings;
    private final Random random;

    public TeamsManager(JavaPlugin plugin, Roster roster, ScoreboardManager scoreboard) {
        this.plugin = plugin;
        this.roster = roster;
        this.scoreboard = scoreboard;
        this.pendingInvite = new ConcurrentHashMap<>();
        this.teams = new ArrayList<>();
        this.maxTeamSize = 2;
        this.previousPairings = new HashSet<>();
        this.random = new Random();
    }

    @Override
    public void enable() {
        this.previousPairings.addAll(this.loadPairings());
        this.plugin.getLogger().info("Loaded " + this.previousPairings.size()
                + " pairing(s) from the previous round; those teams will be avoided.");
    }

    private List<String> loadPairings() {
        File file = new File(this.plugin.getDataFolder(), HISTORY_FILE);
        if (file.isFile()) {
            return YamlConfiguration.loadConfiguration(file).getStringList(PAIRINGS_PATH);
        }

        // Migration off the config.yml key: useful only on the first boot after this change, but it
        // costs one read and beats losing a round.
        return this.plugin.getConfig().getStringList(LEGACY_CONFIG_PATH);
    }

    public void autoTeams() {
        List<ForceItemPlayer> playersWithoutTeam = this.roster.players().values().stream()
                .filter(player -> player.currentTeam() == null)
                .collect(Collectors.toList());

        List<ForceItemPlayer> ordered = this.orderAvoidingPreviousPairings(playersWithoutTeam);

        int teamSizeLimit = this.getMaxTeamSize();
        int next = 0;
        while (ordered.size() - next >= teamSizeLimit) {
            // Copied, not a subList view: Team holds on to what it is given.
            List<ForceItemPlayer> teamPlayers = new ArrayList<>(ordered.subList(next, next + teamSizeLimit));
            next += teamSizeLimit;

            Team randomTeam = new Team(this.teams.size() + 1, null, 0, 0, teamPlayers.toArray(new ForceItemPlayer[0]));
            this.teams.add(randomTeam);

            for (ForceItemPlayer player : teamPlayers) {
                player.setCurrentTeam(randomTeam);
            }
        }

        for (ForceItemPlayer player : ordered.subList(next, ordered.size())) {
            Team singlePlayerTeam = new Team(this.teams.size() + 1, null, 0, 0, player);
            this.teams.add(singlePlayerTeam);

            player.setCurrentTeam(singlePlayerTeam);
            this.scoreboard.updateAllPlayers();
        }

        this.rememberPairings();
        this.scoreboard.updateAllPlayers();
    }

    private List<ForceItemPlayer> orderAvoidingPreviousPairings(List<ForceItemPlayer> players) {
        Map<UUID, ForceItemPlayer> byId = new LinkedHashMap<>();
        for (ForceItemPlayer player : players) {
            if (player.player() == null) continue;
            byId.put(player.player().getUniqueId(), player);
        }

        // Each of these means "pair at random and hope"; say which. Otherwise a lost history is
        // indistinguishable from bad luck.
        String skipReason = null;
        if (this.previousPairings.isEmpty()) {
            skipReason = "no pairings recorded from a previous round";
        } else if (this.getMaxTeamSize() != 2) {
            skipReason = "team size is " + this.getMaxTeamSize() + ", not 2";
        } else if (byId.size() != players.size()) {
            skipReason = "roster holds " + players.size() + " entries but only " + byId.size() + " usable players";
        }

        if (skipReason != null) {
            this.plugin.getLogger().info("Building teams at random (" + skipReason + ").");
            List<ForceItemPlayer> shuffled = new ArrayList<>(players);
            Collections.shuffle(shuffled, this.random);
            return shuffled;
        }

        List<UUID> ordered = TeamPairing.orderAvoidingPairs(
                new ArrayList<>(byId.keySet()), this.previousPairings, this.random);

        int repeats = this.countRepeats(ordered);
        if (repeats > 0) {
            // Unavoidable at small player counts, so a notice rather than a failure.
            this.plugin.getLogger().info("Could not avoid " + repeats + " of the previous round's "
                    + this.previousPairings.size() + " pairing(s) with " + ordered.size() + " players.");
        }

        List<ForceItemPlayer> result = new ArrayList<>(ordered.size());
        for (UUID id : ordered) {
            result.add(byId.get(id));
        }
        return result;
    }

    private int countRepeats(List<UUID> ordered) {
        int repeats = 0;
        for (int i = 0; i + 1 < ordered.size(); i += 2) {
            if (this.previousPairings.contains(TeamPairing.pairKey(ordered.get(i), ordered.get(i + 1)))) {
                repeats++;
            }
        }
        return repeats;
    }

    private void rememberPairings() {
        this.previousPairings.clear();

        for (Team team : this.teams) {
            List<ForceItemPlayer> members = team.getPlayers();
            for (int i = 0; i < members.size(); i++) {
                for (int j = i + 1; j < members.size(); j++) {
                    Player first = members.get(i).player();
                    Player second = members.get(j).player();
                    if (first == null || second == null) continue;

                    this.previousPairings.add(TeamPairing.pairKey(first.getUniqueId(), second.getUniqueId()));
                }
            }
        }

        File file = new File(this.plugin.getDataFolder(), HISTORY_FILE);
        YamlConfiguration history = new YamlConfiguration();
        history.set(PAIRINGS_PATH, new ArrayList<>(this.previousPairings));

        try {
            history.save(file);
            this.plugin.getLogger().info("Stored " + this.previousPairings.size()
                    + " pairing(s) to " + HISTORY_FILE + " for the next round.");
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Failed to store team pairings to " + HISTORY_FILE + "; the next round will pair at random.", e);
        }
    }

    public boolean alreadyInTeam(Team team, ForceItemPlayer player) {
        return team.getPlayers().contains(player);
    }

    public boolean alreadyInvited(ForceItemPlayer player) {
        return this.pendingInvite.containsKey(player);
    }

    public boolean isTeamFull(Team team) {
        return team.getPlayers().size() >= this.getMaxTeamSize();
    }

    public void invite(ForceItemPlayer player, ForceItemPlayer target) {
        // Assigning the inviter a team happens before the self-invite guard below, as it always has:
        // inviting yourself still leaves you in a team of one.
        Team team = player.currentTeam();
        if (team == null) {
            team = new Team(this.teams.size() + 1, null, 0, 0, player);
            player.setCurrentTeam(team);
        }

        if (player == target) {
            player.player().sendMessage(Text.of("<red>You cannot interact with yourself :("));
            return;
        }
        if (this.isTeamFull(team)) {
            player.player().sendMessage(Text.of("<red>Your team is already full"));
            return;
        }
        if (this.alreadyInTeam(team, target)) {
            player.player().sendMessage(Text.of("<yellow>" + target.player().getName() + " <red>is already in a team"));
            return;
        }
        if (this.alreadyInvited(target)) {
            player.player().sendMessage(Text.of("<yellow>" + target.player().getName() + " <red>already got invited"));
            return;
        }

        player.player().sendMessage(Text.of("<dark_aqua>You invited <yellow>" + target.player().getName() + " <dark_aqua>to your team"));
        target.player().sendMessage(Text.of("<dark_aqua>You got an invite from <yellow>" + player.player().getName() +
                " <click:run_command:/teams accept " + player.player().getName() + "><gray>[<green>Accept<gray>]</click>" +
                " <click:run_command:/teams decline " + player.player().getName() + "><gray>[<red>Decline<gray>]</click>"
        ));
        this.pendingInvite.put(target, team);
        this.teams.remove(team);
        this.teams.add(team);

        this.scoreboard.updateAllPlayers();
    }

    public void accept(ForceItemPlayer player, ForceItemPlayer target) {
        if (!this.alreadyInvited(player)) {
            player.player().sendMessage(Text.of("<red>You have no invite from <yellow>" + target.player().getName()));
            return;
        }
        if (player == target) {
            player.player().sendMessage(Text.of("<red>You cannot interact with yourself :("));
            return;
        }
        Team teamInvite = this.pendingInvite.get(player);
        if (teamInvite != null) {
            if (this.isTeamFull(teamInvite)) {
                player.player().sendMessage(Text.of("<red>This team is already full"));
                return;
            }
            this.addToTeam(teamInvite, player);
            player.player().sendMessage(Text.of("<dark_aqua>You <green>accepted <dark_aqua>the invite from <yellow>" + target.player().getName()));
            target.player().sendMessage(Text.of("<yellow>" + player.player().getName() + " <dark_aqua>joined your team"));
            this.pendingInvite.remove(player);
        } else {
            player.player().sendMessage(Text.of("<red>You have no invite from <yellow>" + target.player().getName()));

        }
        this.scoreboard.updateAllPlayers();
    }

    public void create(ForceItemPlayer first, @Nullable ForceItemPlayer second, String name) {
        Team team = new Team(this.teams.size() + 1, null, 0, 0, first);
        team.setName(name);
        first.setCurrentTeam(team);
        if (second != null) this.addToTeam(team, second);

        this.teams.add(team);
        // Never set a playerListName here: the client only applies ScoreboardManager's team
        // prefix/suffix to players who have no tab-list display name of their own, so naming one
        // member makes the two halves of a team render differently.
        this.scoreboard.updateAllPlayers();

        String message = "<dark_aqua>You are now in team <green>" + name + " <dark_aqua>with <yellow>";

        first.player().sendMessage(Text.of(message + ((second != null) ? second.player().getName() : "yourself")));
        if (second != null)
            second.player().sendMessage(Text.of(message + first.player().getName()));

    }

    public void decline(ForceItemPlayer player, ForceItemPlayer target) {
        if (!this.alreadyInvited(player)) {
            player.player().sendMessage(Text.of("<red>You have no invite from <yellow>" + target.player().getName()));
            return;
        }
        if (player == target) {
            player.player().sendMessage(Text.of("<red>You cannot interact with yourself :("));
            return;
        }
        player.player().sendMessage(Text.of("<dark_aqua>You <red>declined <dark_aqua>the invite from <yellow>" + target.player().getName()));
        target.player().sendMessage(Text.of("<yellow>" + player.player().getName() + " <dark_aqua>declined your invite"));
        this.pendingInvite.remove(player);
    }

    public void leave(ForceItemPlayer player) {
        if (player.currentTeam() == null) {
            player.player().sendMessage(Text.of("<red>You are not in a team"));
            return;
        }
        this.removeFromTeam(player.currentTeam(), player);
        player.player().sendMessage(Text.of("<dark_aqua>You <red>left <dark_aqua>the team"));
        if (this.getTeams().contains(player.currentTeam())) {
            player.currentTeam().getPlayers().forEach(teamPlayers -> {
                teamPlayers.player().sendMessage(Text.of("<yellow>" + player.player().getName() + " <dark_aqua>left your team"));
            });
        }
        this.scoreboard.updateAllPlayers();
    }

    public void showTeamList(ForceItemPlayer player) {
        if (player.currentTeam() == null) {
            player.player().sendMessage(Text.of("<red>You are not in a team"));
            return;
        }
        player.player().sendMessage(" ");
        player.player().sendMessage(Text.of(" <dark_gray>● <gray>Your team:"));
        player.currentTeam().getPlayers().forEach(teamPlayers -> {
            player.player().sendMessage(Text.of("  <dark_gray>» <gold>" + teamPlayers.player().getName()));
        });
        player.player().sendMessage(" ");
    }

    public void clearAllTeams() {
        this.roster.players().values().forEach(players -> {
            if (players.currentTeam() != null) {
                players.setCurrentTeam(null);
                // null, not the plain name: any display name at all makes the client skip the
                // scoreboard prefix/suffix for the rest of the session.
                players.player().playerListName(null);
            }
        });
        this.pendingInvite.clear();
        this.getTeams().clear();
        this.scoreboard.updateAllPlayers();
    }

    private void disbandTeam(Team team) {
        if (team.getPlayers().isEmpty()) {
            this.pendingInvite.forEach((pendingInvitees, inviteesTeam) -> {
                if (inviteesTeam == team) {
                    pendingInvitees.player().sendMessage(Text.of("<red>The invite expired, the team got disbanded"));
                    this.pendingInvite.remove(pendingInvitees);
                    pendingInvitees.player().playerListName(null);
                }
            });
            this.getTeams().remove(team);
        }
    }

    private void addToTeam(Team team, ForceItemPlayer player) {
        team.addPlayer(player);
        player.setCurrentTeam(team);
    }

    private void removeFromTeam(Team team, ForceItemPlayer player) {
        team.removePlayer(player);
        this.disbandTeam(team);
        player.setCurrentTeam(null);
    }
}
