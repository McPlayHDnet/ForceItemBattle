package forceitembattle.fairplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import java.lang.reflect.Constructor;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class OpTransparencyListenerTest {

    private ServerMock server;
    private RoundPhase roundPhase;
    private OpTransparencyListener listener;
    private PlayerMock op;
    private PlayerMock bystander;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        this.server = MockBukkit.mock();
        for (String name : List.of("give", "locate", "msg", "help", "checkmods", "settings", "start")) {
            StubCommand command = new StubCommand(name, name.equals("msg") ? List.of("tell", "w") : List.of());
            command.setPermission("minecraft.command." + name);
            this.server.getCommandMap().register("minecraft", command);
        }
        StubCommand list = new StubCommand("list", List.of());
        list.setPermission("minecraft.command.list");
        this.server.getPluginManager().addPermission(new Permission("minecraft.command.list", PermissionDefault.TRUE));
        this.server.getCommandMap().register("minecraft", list);
        this.server.getCommandMap().register("minecraft", new StubCommand("unguarded", List.of()));
        Plugin plugin = MockBukkit.createMockPlugin();
        this.server.getCommandMap().register("fib", pluginCommand("bed", plugin, new StubCustomCommand("bed", List.of())));
        this.server.getCommandMap().register("fib", pluginCommand("skip", plugin, new StubCustomCommand("skip", List.of(Precondition.OP))));
        this.roundPhase = new RoundPhase();
        this.roundPhase.moveTo(GameState.MID_GAME);
        this.listener = new OpTransparencyListener(this.roundPhase);
        this.op = this.server.addPlayer("Admin");
        this.op.setOp(true);
        this.bystander = this.server.addPlayer("Bystander");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void type(PlayerMock player, String message) {
        this.listener.onPlayerCommand(new PlayerCommandPreprocessEvent(player, message));
    }

    private String nextBroadcast() {
        Component message = this.bystander.nextComponentMessage();
        return message == null ? null : PlainTextComponentSerializer.plainText().serialize(message);
    }

    @Test
    void anOpCommandIsReportedWithItsArguments() {
        type(this.op, "/locate structure village");

        assertEquals("» Fair Play ┃ Admin used /locate structure village", nextBroadcast());
    }

    @Test
    void aNamespacedCommandIsReportedToo() {
        type(this.op, "/minecraft:give Admin diamond 64");

        assertEquals("» Fair Play ┃ Admin used /minecraft:give Admin diamond 64", nextBroadcast());
    }

    @Test
    void theConsoleIsReported() {
        this.listener.onServerCommand(new ServerCommandEvent(this.server.getConsoleSender(), "give Bystander diamond"));

        assertEquals("» Fair Play ┃ Console used /give Bystander diamond", nextBroadcast());
    }

    @Test
    void aNonOpIsNotReported() {
        type(this.bystander, "/locate structure village");

        assertNull(nextBroadcast());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/msg Bystander hi", "/tell Bystander hi", "/minecraft:w Bystander hi", "/help", "/checkmods", "/settings", "/start", "/nosuchcommand"})
    void privateHarmlessAndUnknownCommandsAreNotReported(String message) {
        type(this.op, message);

        assertNull(nextBroadcast());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/bed", "/fib:bed", "/list", "/unguarded"})
    void commandsEveryoneMayRunAreNotReported(String message) {
        type(this.op, message);

        assertNull(nextBroadcast());
    }

    @Test
    void anOpOnlyPluginCommandIsReported() {
        type(this.op, "/skip");

        assertEquals("» Fair Play ┃ Admin used /skip", nextBroadcast());
    }

    @Test
    void nothingIsReportedAfterTheRound() {
        this.roundPhase.moveTo(GameState.END_GAME);

        type(this.op, "/give Admin diamond");

        assertNull(nextBroadcast());
    }

    @ParameterizedTest
    @EnumSource(value = GameState.class, names = {"PRE_GAME", "STARTING", "MID_GAME", "PAUSED_GAME"})
    void everyPhaseUpToTheEndIsReported(GameState state) {
        this.roundPhase.moveTo(state);

        type(this.op, "/give Admin diamond");

        assertEquals("» Fair Play ┃ Admin used /give Admin diamond", nextBroadcast());
    }

    @Test
    void theCommandCannotInjectFormatting() {
        type(this.op, "/give Admin <red>fake</red>");

        assertEquals("» Fair Play ┃ Admin used /give Admin <red>fake</red>", nextBroadcast());
    }

    private static PluginCommand pluginCommand(String name, Plugin owner, CommandExecutor executor) throws ReflectiveOperationException {
        Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
        constructor.setAccessible(true);
        PluginCommand command = constructor.newInstance(name, owner);
        command.setExecutor(executor);
        return command;
    }

    private static final class StubCustomCommand extends CustomCommand {
        private final List<Precondition> preconditions;

        StubCustomCommand(String name, List<Precondition> preconditions) {
            super(name);
            this.preconditions = preconditions;
        }

        @Override
        protected List<Precondition> preconditions() {
            return this.preconditions;
        }

        @Override
        public void onPlayerCommand(Player player, String label, String[] args) {
        }
    }

    private static final class StubCommand extends Command {
        StubCommand(String name, List<String> aliases) {
            super(name, "", "/" + name, aliases);
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return true;
        }
    }
}
