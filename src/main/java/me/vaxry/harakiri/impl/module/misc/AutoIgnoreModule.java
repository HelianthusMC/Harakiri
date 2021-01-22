package me.vaxry.harakiri.impl.module.misc;

import me.vaxry.harakiri.Harakiri;
import me.vaxry.harakiri.api.event.EventStageable;
import me.vaxry.harakiri.api.event.network.EventReceivePacket;
import me.vaxry.harakiri.api.ignore.Ignored;
import me.vaxry.harakiri.api.module.Module;
import me.vaxry.harakiri.api.value.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.TextComponentString;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author Seth
 * 7/1/2019 @ 10:22 PM.
 */
public final class AutoIgnoreModule extends Module {

    private final String REGEX_NAME = "<(\\S+)\\s*(\\S+?)?>\\s(.*)";

    public final Value<Mode> mode = new Value<Mode>("Mode", new String[]{"Mode", "M"}, "The auto ignore mode to use.", Mode.CLIENT);

    private List<String> blacklist = new ArrayList<>();

    public AutoIgnoreModule() {
        super("AutoIgnore", new String[]{"AutomaticIgnore", "AIG", "AIgnore"}, "Automatically ignores someone if they say a certain word or phrase", "NONE", -1, ModuleType.MISC);
    }

    @Override
    public String getMetaData() {
        return this.mode.getValue().name();
    }

    public boolean blacklistContains(String message) {
        for (String s : this.blacklist) {
            if (message.toLowerCase().contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Listener
    public void recievePacket(EventReceivePacket event) {
        if (event.getStage() == EventStageable.EventStage.PRE) {
            if (event.getPacket() instanceof SPacketChat) {
                final SPacketChat packet = (SPacketChat) event.getPacket();
                if (packet.getChatComponent() instanceof TextComponentString) {
                    final TextComponentString component = (TextComponentString) packet.getChatComponent();
                    final String message = StringUtils.stripControlCodes(component.getFormattedText());
                    final boolean serverMessage = component.getFormattedText().startsWith("\247c") || component.getFormattedText().startsWith("\247e") || component.getFormattedText().startsWith("\2475");
                    if (!serverMessage && this.blacklistContains(message)) {
                        Pattern chatUsernamePattern = Pattern.compile(REGEX_NAME);
                        Matcher chatUsernameMatcher = chatUsernamePattern.matcher(message);
                        if (chatUsernameMatcher.find()) {
                            String username = chatUsernameMatcher.group(1).replaceAll(">", "").toLowerCase();
                            final Ignored ignored = Harakiri.INSTANCE.getIgnoredManager().find(username);
                            if (ignored == null && !username.equalsIgnoreCase(Minecraft.getMinecraft().session.getUsername())) {
                                switch (this.mode.getValue()) {
                                    case CLIENT:
                                        Harakiri.INSTANCE.getIgnoredManager().add(username);
                                        Harakiri.INSTANCE.logChat("Added \247c" + username + "\247f to your ignore list");
                                        break;
                                    case SERVER:
                                        Harakiri.INSTANCE.getChatManager().add("/ignore " + username);
                                        break;
                                    case BOTH:
                                        Harakiri.INSTANCE.getChatManager().add("/ignore " + username);
                                        Harakiri.INSTANCE.getIgnoredManager().add(username);
                                        Harakiri.INSTANCE.logChat("Added \247c" + username + "\247f to your ignore list");
                                        break;
                                }
                            }
                        }
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    private enum Mode {
        CLIENT, SERVER, BOTH
    }

    public List<String> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(List<String> blacklist) {
        this.blacklist = blacklist;
    }
}
