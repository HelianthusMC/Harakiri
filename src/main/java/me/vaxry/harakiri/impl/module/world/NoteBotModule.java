package me.vaxry.harakiri.impl.module.world;

import me.vaxry.harakiri.Harakiri;
import me.vaxry.harakiri.api.event.EventStageable;
import me.vaxry.harakiri.api.event.network.EventReceivePacket;
import me.vaxry.harakiri.api.event.player.EventUpdateWalkingPlayer;
import me.vaxry.harakiri.api.event.world.EventLoadWorld;
import me.vaxry.harakiri.api.module.Module;
import me.vaxry.harakiri.api.module.notebot.Note;
import me.vaxry.harakiri.api.module.notebot.NotePlayer;
import me.vaxry.harakiri.api.task.rotation.RotationTask;
import me.vaxry.harakiri.api.util.MathUtil;
import me.vaxry.harakiri.api.util.Timer;
import me.vaxry.harakiri.api.value.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.server.SPacketBlockAction;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author noil
 */
public final class NoteBotModule extends Module {

    private final Value<BotState> state = new Value<BotState>("State", new String[]{"State", "s"}, "Current state of the note-bot.", BotState.IDLE);
    private final Value<Mode> mode = new Value<Mode>("Mode", new String[]{"mod", "m"}, "Current mode of the note-bot.", Mode.NORMAL);
    private final Value<Boolean> rotate = new Value<Boolean>("Rotate", new String[]{"rot", "r"}, "Rotate the player's head & body for each note-bot function.", true);
    private final Value<Boolean> swing = new Value<Boolean>("Swing", new String[]{"swingarm", "armswing", "sa"}, "Swing the player's hand for each note-bot function.", true);
    private final Value<Float> discoverDelay = new Value<Float>("DiscoverDelay", new String[]{"Discover Delay", "discover-delay", "ddelay", "ddel", "dd", "d"}, "Delay(ms) to wait between left clicks.", 50.0f, 0.0f, 1000.0f, 1.0f);
    private final Value<Float> tuneDelay = new Value<Float>("TuneDelay", new String[]{"Tune Delay", "tune-delay", "tdelay", "tdel", "td"}, "Delay(ms) to wait between right clicks.", 200.0f, 0.0f, 1000.0f, 1.0f);

    private final RotationTask rotationTask = new RotationTask("NoteBot", 2);

    private BlockPos currentBlock;
    private int currentNote;

    private final int[] positionOffsets = new int[]{2, 1, 2};

    private final NotePlayer notePlayer = new NotePlayer();
    private final Timer discoverTimer = new Timer();
    private final Timer tuneTimer = new Timer();
    private final Timer stateTimer = new Timer();

    private final List<BlockPos> blocks = new ArrayList<>();
    private final List<BlockPos> tunedBlocks = new ArrayList<>();
    private final Map<BlockPos, Note> discoveredBlocks = new HashMap<>();

    private final int BLOCK_AREA = 25;
    private final Minecraft mc = Minecraft.getMinecraft();

    public NoteBotModule() {
        super("NoteBot", new String[]{"NoteBot+", "MusicBot", "MusicPlayer", "MidiPlayer", "MidiBot"}, "Play .midi files on a 5x5 grid of note-blocks.", "NONE", -1, ModuleType.WORLD);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (mc.world == null)
            return;

        IntStream.range(0, BLOCK_AREA).forEach(note -> {
            int[] area = this.blockArea(note);
            this.blocks.add(new BlockPos(area[0], area[1], area[2]));
        });

        if (this.mode.getValue().equals(Mode.NORMAL)) {
            this.state.setEnumValue("DISCOVERING");
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (mc.world == null)
            return;

        this.clearData();
        Harakiri.INSTANCE.getRotationManager().finishTask(this.rotationTask);
    }

    @Override
    public String getMetaData() {
        return this.state.getValue().name();
    }

    @Listener
    public void onLoadWorld(EventLoadWorld event) {
        if (event.getWorld() != null) {
            this.toggle(); // toggle off
            Harakiri.INSTANCE.logChat("\247rToggled\2477 " + this.getDisplayName() + " \247coff\247r, as you've loaded into a new world.");
        }
    }

    @Listener
    public void onReceivePacket(EventReceivePacket event) {
        if (event.getStage() != EventStageable.EventStage.POST)
            return;

        if (!(event.getPacket() instanceof SPacketBlockAction))
            return;

        SPacketBlockAction packetBlockAction = (SPacketBlockAction) event.getPacket();
        BlockPos position = packetBlockAction.getBlockPosition();
        this.blocks.stream().filter(blockPos -> this.correctPosition(position, this.blocks.indexOf(blockPos))).forEach(blockPos -> {
            final Note note = new Note(this.blocks.indexOf(blockPos), position, packetBlockAction.getData1(), packetBlockAction.getData2());
            this.discoveredBlocks.put(blockPos, note);

            if (!this.tunedBlocks.contains(blockPos) && this.blocks.indexOf(blockPos) == packetBlockAction.getData2()) {
                this.tunedBlocks.add(blockPos);
            }

            if (!this.mode.getValue().equals(Mode.DEBUG) && !this.state.getValue().equals(BotState.PLAYING)) {
                if (!this.state.getValue().equals(BotState.TUNING)) {
                    if (this.discoveredBlocks.size() != BLOCK_AREA) {
                        this.stateTimer.reset();
                    } else if (this.tunedBlocks.size() == BLOCK_AREA) {
                        this.state.setValue(BotState.IDLE);
                    }
                }
            }
        });
    }

    @Listener
    public void onMotionUpdate(EventUpdateWalkingPlayer event) {
        if (mc.world == null || mc.player == null)
            return;

        if (mc.player.capabilities.isCreativeMode) {
            if (this.rotationTask.isOnline())
                Harakiri.INSTANCE.getRotationManager().finishTask(this.rotationTask);

            return;
        }

        switch (event.getStage()) {
            case PRE:
                if (this.state.getValue() == BotState.PLAYING && this.notePlayer.getNotesToPlay().size() > 0) {
                    int playingNote = this.notePlayer.getNotesToPlay().get(this.currentNote) % 24;
                    if (playingNote != -1) {
                        this.setCurrentNoteBlock(this.getPosition(playingNote));
                    }
                }

                if (this.mode.getValue().equals(Mode.NORMAL)) {
                    /*
                    if (this.state.getValue().equals(BotState.PLAYING)) {
                        if (this.getNotePlayer().getNotesToPlay().size() <= 0) {
                            //this.state.setValue(BotState.IDLE);
                        }
                    }
                    */

                    if (this.state.getValue().equals(BotState.TUNING)) {
                        if (this.discoveredBlocks.size() == BLOCK_AREA && this.tunedBlocks.size() == BLOCK_AREA) {
                            this.state.setValue(BotState.IDLE);
                        }
                    }
                }

                if (this.currentBlock == null && !this.state.getValue().equals(BotState.PLAYING)) {
                    this.blocks.stream().filter(blockPos -> (!this.discoveredBlocks.containsKey(blockPos) || !this.tunedBlocks.contains(blockPos))).forEach(blockPos -> {
                        final BlockPos workPos = new BlockPos(this.getPosition(this.blocks.indexOf(blockPos)));
                        if (!this.discoveredBlocks.containsKey(blockPos)) {
                            if (!this.mode.getValue().equals(Mode.DEBUG)) {
                                this.state.setValue(BotState.DISCOVERING);
                            }
                            this.setCurrentNoteBlock(workPos);
                        } else if (!this.tunedBlocks.contains(workPos)) {
                            if (!this.mode.getValue().equals(Mode.DEBUG)) {
                                if (this.stateTimer.passed(1000)) {
                                    this.state.setValue(BotState.TUNING);
                                    this.setCurrentNoteBlock(workPos);
                                }
                            } else {
                                this.setCurrentNoteBlock(workPos);
                            }
                        }
                    });
                }
                break;
            case POST:
                if (this.rotationTask.isOnline() || !this.rotate.getValue()) {
                    final EnumFacing direction = EnumFacing.UP;

                    switch (this.state.getValue()) {
                        case IDLE:
                            if (this.rotationTask.isOnline()) {
                                Harakiri.INSTANCE.getRotationManager().finishTask(this.rotationTask);
                            }
                            break;
                        case DISCOVERING:
                            if (this.discoveredBlocks.size() != BLOCK_AREA) {
                                if (this.discoverTimer.passed(this.discoverDelay.getValue())) {
                                    mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, this.currentBlock, direction));
                                    mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.currentBlock, direction));
                                    if (this.swing.getValue()) {
                                        mc.player.swingArm(EnumHand.MAIN_HAND);
                                    }
                                    this.discoveredBlocks.put(this.currentBlock, new Note(this.blocks.indexOf(this.currentBlock), this.currentBlock, 0, this.blocks.indexOf(this.currentBlock)));
                                    this.currentBlock = null;
                                    this.discoverTimer.reset();
                                }
                            }
                            break;
                        case TUNING:
                            if (!this.mode.getValue().equals(Mode.DEBUG)) {
                                if (this.discoveredBlocks.size() != BLOCK_AREA) {
                                    this.state.setValue(BotState.DISCOVERING);
                                    break;
                                }
                            }
                            if (this.tunedBlocks.size() != BLOCK_AREA && !this.tunedBlocks.contains(this.currentBlock)) {
                                if (this.tuneTimer.passed(this.tuneDelay.getValue())) {
                                    mc.playerController.processRightClickBlock(mc.player, mc.world, this.currentBlock, direction, new Vec3d(0.5F, 0.5F, 0.5F), EnumHand.MAIN_HAND);
                                    if (this.swing.getValue()) {
                                        mc.player.swingArm(EnumHand.MAIN_HAND);
                                    }
                                    this.currentBlock = null;
                                    this.tuneTimer.reset();
                                }
                            } else {
                                this.currentBlock = null;
                            }
                            break;
                        case PLAYING:
                            if (this.currentNote >= this.notePlayer.getNotesToPlay().size()) {
                                this.currentNote = 0;
                                return;
                            }
                            this.currentNote++;
                            if (this.currentNote != -1) {
                                if (this.currentBlock != null) {
                                    mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, this.currentBlock, direction));
                                    mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.currentBlock, direction));
                                    if (this.swing.getValue()) {
                                        mc.player.swingArm(EnumHand.MAIN_HAND);
                                    }
                                    this.currentBlock = null;
                                }
                            }
                            break;
                    }
                }
                break;
        }
    }

    private int[] blockArea(int index) {
        int[] positions = {(int) Math.floor(mc.player.posX) - this.positionOffsets[0], (int) Math.floor(mc.player.posY) - this.positionOffsets[1], (int) Math.floor(mc.player.posZ) - this.positionOffsets[2]};
        return new int[]{positions[0] + index % 5, positions[1], positions[2] + index / 5};
    }

    private void lookAtPosition(BlockPos position) {
        Harakiri.INSTANCE.getRotationManager().startTask(this.rotationTask);
        if (this.rotationTask.isOnline()) {
            final float[] angle = MathUtil.calcAngle(mc.player.getPositionEyes(mc.getRenderPartialTicks()), new Vec3d(position.getX() + 0.5f, position.getY() + 0.5f, position.getZ() + 0.5f));
            Harakiri.INSTANCE.getRotationManager().setPlayerRotations(angle[0], angle[1]);
        }
    }

    private BlockPos getPosition(int note) {
        int[] blocks = this.blockArea(note);
        return new BlockPos(blocks[0], blocks[1], blocks[2]);
    }

    private boolean correctPosition(BlockPos blockPos, int index) {
        int[] blocks = this.blockArea(index);
        return (blockPos.getX() == blocks[0] && blockPos
                .getY() == blocks[1] && blockPos
                .getZ() == blocks[2]);
    }

    private void setCurrentNoteBlock(BlockPos pos) {
        this.currentBlock = new BlockPos(pos);
        if (this.rotate.getValue()) {
            this.lookAtPosition(pos);
        }
    }

    private void clearData() {
        this.currentBlock = null;
        this.discoveredBlocks.clear();
        if (!this.mode.getValue().equals(Mode.DEBUG)) { // is not debug, so let's wipe our previously tuned blocks data
            this.tunedBlocks.clear();
        }
        this.blocks.clear();
        this.notePlayer.getNotesToPlay().clear();
        //this.notePlayer.end();
    }

    public enum BotState {
        IDLE, DISCOVERING, TUNING, PLAYING;
    }

    public enum Mode {
        NORMAL, DEBUG
    }

    public Value<BotState> getState() {
        return state;
    }

    public NotePlayer getNotePlayer() {
        return notePlayer;
    }

    public int getCurrentNote() {
        return currentNote;
    }

    public void setCurrentNote(int currentNote) {
        this.currentNote = currentNote;
    }
}
