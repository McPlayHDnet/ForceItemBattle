package forceitembattle.commands;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandsManager {

    /**
     * Commands list used for /help
     */
    private final List<CustomCommand> commands = new ArrayList<>();

    public void registerCommand(CustomCommand command) {
        this.commands.add(command);
    }

}
