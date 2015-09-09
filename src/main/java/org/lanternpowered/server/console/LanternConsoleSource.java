package org.lanternpowered.server.console;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.lanternpowered.server.game.LanternGame;
import org.lanternpowered.server.permission.SubjectBase;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.sink.MessageSink;
import org.spongepowered.api.text.sink.MessageSinks;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.source.ConsoleSource;

import com.google.common.base.Optional;

public class LanternConsoleSource extends SubjectBase implements ConsoleSource {

    public static final ConsoleSource INSTANCE = new LanternConsoleSource();

    private MessageSink messageSink;

    @Override
    public String getName() {
        return "Console";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(Text... messages) {
        for (Text message : messages) {
            if (message != null) {
                LanternGame.log().info(Texts.legacy().to(message));
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(Iterable<Text> messages) {
        Iterator<Text> it = messages.iterator();
        while (it.hasNext()) {
            Text message = it.next();
            if (message != null) {
                LanternGame.log().info(Texts.legacy().to(message));
            }
        }
    }

    @Override
    public String getIdentifier() {
        return this.getName();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.<CommandSource>of(this);
    }

    @Override
    protected String getSubjectCollectionIdentifier() {
        return PermissionService.SUBJECTS_SYSTEM;
    }

    @Override
    protected Tristate getPermissionDefault(String permission) {
        checkNotNull(permission, "permission");
        return Tristate.TRUE;
    }

    @Override
    public MessageSink getMessageSink() {
        if (this.messageSink == null) {
            this.messageSink = MessageSinks.toAll();
        }
        return this.messageSink;
    }

    @Override
    public void setMessageSink(MessageSink sink) {
        this.messageSink = checkNotNull(sink, "sink");
    }
}