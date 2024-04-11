package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItem;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.Teams;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TeamsManager {

    private final ForceItemBattle forceItemBattle;

    private final Map<ForceItemPlayer, Teams> pendingInvite;
    @Getter
    private final List<Teams> teamsList;
    @Getter
    private final int maxTeamSize;

    public TeamsManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.pendingInvite = new ConcurrentHashMap<>();
        this.teamsList = new ArrayList<>();
        this.maxTeamSize = 2;
    }

    public void autoTeams() {
        List<ForceItemPlayer> playersWithoutTeam = this.forceItemBattle.getGamemanager().forceItemPlayerMap().values().stream()
                .filter(player -> player.currentTeam() == null)
                .collect(Collectors.toList());

        Collections.shuffle(playersWithoutTeam);

        int teamSizeLimit = this.getMaxTeamSize();
        while (playersWithoutTeam.size() >= teamSizeLimit) {
            List<ForceItemPlayer> teamPlayers = playersWithoutTeam.subList(0, teamSizeLimit);
            playersWithoutTeam = playersWithoutTeam.subList(teamSizeLimit, playersWithoutTeam.size());

            Teams randomTeam = new Teams(this.teamsList.size() + 1, new ArrayList<>(), null, 0, 0, teamPlayers.toArray(new ForceItemPlayer[0]));
            this.teamsList.add(randomTeam);

            for (ForceItemPlayer player : teamPlayers) {
                player.setCurrentTeam(randomTeam);
            }
        }

        for (ForceItemPlayer player : playersWithoutTeam) {
            Teams singlePlayerTeam = new Teams(this.teamsList.size() + 1, new ArrayList<>(), null, 0, 0, player);
            this.teamsList.add(singlePlayerTeam);

            player.setCurrentTeam(singlePlayerTeam);
        }
    }

    public boolean alreadyInTeam(Teams team, ForceItemPlayer player) {
        return team.getPlayers().contains(player);
    }

    public boolean alreadyInvited(ForceItemPlayer player) {
        return this.pendingInvite.containsKey(player);
    }

    public boolean isTeamFull(Teams team) {
        return team.getPlayers().size() >= this.getMaxTeamSize();
    }

    public void invite(ForceItemPlayer player, ForceItemPlayer target) {
        Teams team = new Teams(this.teamsList.size() + 1, new ArrayList<>(), null, 0, 0, player);
        if(player.currentTeam() != null) team = player.currentTeam();
        else player.setCurrentTeam(team);

        if(player == target) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>You cannot interact with yourself :("));
            return;
        }
        if(this.isTeamFull(team)) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>Your team is already full"));
            return;
        }
        if(this.alreadyInTeam(team, target)) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<yellow>" + target.player().getName() + " <red>is already in a team"));
            return;
        }
        if(this.alreadyInvited(target)) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<yellow>" + target.player().getName() + " <red>already got invited"));
            return;
        }

        player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_aqua>You invited <yellow>" + target.player().getName() + " <dark_aqua>to your team"));
        target.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_aqua>You got an invite from <yellow>" + player.player().getName() +
                " <click:run_command:/teams accept " + player.player().getName() + "><gray>[<green>Accept<gray>]</click>" +
                " <click:run_command:/teams decline " + player.player().getName() + "><gray>[<red>Decline<gray>]</click>"
        ));
        this.pendingInvite.put(target, team);
        this.teamsList.remove(team);
        this.teamsList.add(team);

        player.player().playerListName(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<yellow>[#" + team.getTeamId() + "] <white>" + player.player().getName()));
    }

    public void accept(ForceItemPlayer player, ForceItemPlayer target) {
        if(!this.alreadyInvited(player)) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>You have no invite from <yellow>" + target.player().getName()));
            return;
        }
        if(player == target) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>You cannot interact with yourself :("));
            return;
        }
        Teams teamInvite = this.pendingInvite.get(player);
        if(teamInvite != null) {
            if(this.isTeamFull(teamInvite)) {
                player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>This team is already full"));
                return;
            }
            this.addToTeam(teamInvite, player);
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_aqua>You <green>accepted <dark_aqua>the invite from <yellow>" + target.player().getName()));
            target.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<yellow>" + player.player().getName() + " <dark_aqua>joined your team"));
            player.player().playerListName(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<yellow>[#" + teamInvite.getTeamId() + "] <white>" + player.player().getName()));
            this.pendingInvite.remove(player);
        } else {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>You have no invite from <yellow>" + target.player().getName()));

        }
    }

    public void decline(ForceItemPlayer player, ForceItemPlayer target) {
        if(!this.alreadyInvited(player)) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>You have no invite from <yellow>" + target.player().getName()));
            return;
        }
        if(player == target) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>You cannot interact with yourself :("));
            return;
        }
        player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_aqua>You <red>declined <dark_aqua>the invite from <yellow>" + target.player().getName()));
        target.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<yellow>" + player.player().getName() + " <dark_aqua>declined your invite"));
        this.pendingInvite.remove(player);
    }

    public void leave(ForceItemPlayer player) {
        if(player.currentTeam() == null) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>You are not in a team"));
            return;
        }
        this.removeFromTeam(player.currentTeam(), player);
        player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_aqua>You <red>left <dark_aqua>the team"));
        player.player().playerListName(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(player.player().getName()));
        if(this.getTeamsList().contains(player.currentTeam())) {
            player.currentTeam().getPlayers().forEach(teamPlayers -> {
                teamPlayers.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<yellow>" + player.player().getName() + " <dark_aqua>left your team"));
            });
        }
    }

    public void showTeamList(ForceItemPlayer player) {
        if(player.currentTeam() == null) {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>You are not in a team"));
            return;
        }
        player.player().sendMessage(" ");
        player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(" <dark_gray>● <gray>Your team:"));
        player.currentTeam().getPlayers().forEach(teamPlayers -> {
            player.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>» <gold>" + teamPlayers.player().getName()));
        });
        player.player().sendMessage(" ");
    }

    public void clearAllTeams() {
        this.forceItemBattle.getGamemanager().forceItemPlayerMap().values().forEach(players -> {
            if(players.currentTeam() != null) {
                players.setCurrentTeam(null);
                players.player().playerListName(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(players.player().getName()));
            }
        });
        this.pendingInvite.clear();
        this.getTeamsList().clear();
    }

    private void disbandTeam(Teams team) {
        if(team.getPlayers().isEmpty()) {
            this.pendingInvite.forEach((pendingInvitees, inviteesTeam) -> {
                if(inviteesTeam == team) {
                    pendingInvitees.player().sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>The invite expired, the team got disbanded"));
                    this.pendingInvite.remove(pendingInvitees);
                    pendingInvitees.player().playerListName(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize(pendingInvitees.player().getName()));
                }
            });
            this.getTeamsList().remove(team);
        }
    }

    private void addToTeam(Teams team, ForceItemPlayer player) {
        team.addPlayer(player);
        player.setCurrentTeam(team);
    }

    private void removeFromTeam(Teams team, ForceItemPlayer player) {
        team.removePlayer(player);
        this.disbandTeam(team);
        player.setCurrentTeam(null);
    }
}
