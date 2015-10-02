package org.lanternpowered.server.command;

import static org.spongepowered.api.util.command.args.GenericArguments.optional;

import org.lanternpowered.server.game.LanternGame;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;

public class CommandStop implements Command {

    private final LanternGame game;

    public CommandStop(LanternGame game) {
        this.game = game;
    }

    @Override
    public CommandSpec build() {
        return CommandSpec.builder()
                .arguments(optional(ArgumentRemainingText.of(Texts.of("kickMessage"))))
                .permission("minecraft.command.stop")
                .description(Texts.of(this.game.getRegistry().getTranslationManager().get("commands.stop.description")))
                .executor(new CommandExecutor() {

                    @Override
                    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                        if (args.hasAny("kickMessage")) {
                            game.getServer().shutdown(args.<Text>getOne("kickMessage").get());
                        } else {
                            game.getServer().shutdown();
                        }
                        return CommandResult.success();
                    }

                }).build();
    }
}