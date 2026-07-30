package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.randomevents.RandomEvent;
import forceitembattle.randomevents.RandomEvents;
import forceitembattle.settings.GameSetting;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class RandomEventManager implements Manager {

    /** One event every 12-20 minutes, i.e. roughly 3-4 per hour. */
    private static final int MIN_GAP_SECONDS = 12 * 60;
    private static final int MAX_GAP_SECONDS = 20 * 60;

    /**
     * The first event can land anywhere in the opening stretch, including the first minutes —
     * the 30s floor only keeps it clear of the /start countdown and the spawn scramble.
     */
    private static final int FIRST_EVENT_MIN_SECONDS = 30;
    private static final int FIRST_EVENT_MAX_SECONDS = MAX_GAP_SECONDS;

    /** No event may start with less than this left on the clock. */
    private static final int END_BUFFER_SECONDS = 5 * 60;

    private final ForceItemBattle plugin;

    /** Remaining fire times, as timeLeft values, in descending order. */
    private final Deque<Integer> schedule = new ArrayDeque<>();

    /** Events already fired this round, for the once-per-game ones. */
    private final Set<RandomEvents> fired = EnumSet.noneOf(RandomEvents.class);

    @Getter
    @Nullable
    private RandomEvents activeType;

    @Nullable
    private RandomEvent activeEvent;

    @Override
    public void disable() {
        this.reset();
    }

    /**
     * Called from /start once the duration is known.
     */
    public void startGame() {
        this.reset();

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.RANDOM_EVENTS)) {
            return;
        }

        // Run Battle is already a race for the first find; a hunt on top of it means nothing.
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.RUN)) {
            return;
        }

        this.planSchedule(this.plugin.getGamemanager().getGameDuration());
    }

    public void reset() {
        if (this.activeEvent != null) {
            this.activeEvent.cancel();
        }
        this.activeEvent = null;
        this.activeType = null;
        this.schedule.clear();
        this.fired.clear();
    }

    private void planSchedule(int gameDuration) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int elapsed = random.nextInt(FIRST_EVENT_MIN_SECONDS, FIRST_EVENT_MAX_SECONDS + 1);

        while (gameDuration - elapsed >= END_BUFFER_SECONDS) {
            this.schedule.add(gameDuration - elapsed);
            elapsed += random.nextInt(MIN_GAP_SECONDS, MAX_GAP_SECONDS + 1);
        }
    }

    /**
     * Called once per second from TimerManager, mid-game only.
     */
    public void tick(int timeLeft) {
        // Drive a running timed event's own clock first; it may conclude here. Because this method
        // is mid-game only, that countdown freezes during pause with no extra handling.
        if (this.activeEvent != null && this.activeEvent.tick()) {
            this.activeEvent = null;
            this.activeType = null;
        }

        Integer next = this.schedule.peek();
        if (next == null || timeLeft > next) {
            return;
        }

        this.schedule.poll();

        // An event has no expiry, so its slot can come round while it is still unresolved.
        // Drop the slot rather than queueing it — only one event runs at a time.
        if (this.activeEvent != null) {
            return;
        }

        // A race needs someone to race against.
        if (this.countParticipants() < 2) {
            return;
        }

        RandomEvents type = this.pickWeighted(timeLeft);
        if (type == null) {
            return; // nothing eligible with enough time left (or all one-shots already fired)
        }

        this.trigger(type);
    }

    /**
     * @return false if an event is already running.
     */
    public boolean trigger(RandomEvents type) {
        if (this.activeEvent != null) {
            return false;
        }

        RandomEvent event = type.create(this.plugin);
        this.fired.add(type);

        this.activeType = type;
        this.activeEvent = event;
        event.start();

        // An instant event has already resolved inside start(); don't let it hold the slot,
        // or it would swallow every remaining slot in the round.
        if (event.isInstant()) {
            this.activeType = null;
            this.activeEvent = null;
        }

        return true;
    }

    public void handleFoundItem(FoundItemEvent foundItemEvent, ForceItemPlayer forceItemPlayer) {
        if (this.activeEvent == null) {
            return;
        }

        if (this.activeEvent.onFoundItem(foundItemEvent, forceItemPlayer)) {
            this.activeEvent = null;
            this.activeType = null;
        }
    }

    @Nullable
    private RandomEvents pickWeighted(int timeLeft) {
        List<RandomEvents> eligible = Arrays.stream(RandomEvents.values())
                .filter(type -> !type.isOncePerGame() || !this.fired.contains(type))
                .filter(type -> type.getWeight() > 0)
                .filter(type -> type.getMinSecondsToRun() <= timeLeft)
                .toList();

        int totalWeight = eligible.stream().mapToInt(RandomEvents::getWeight).sum();
        if (totalWeight <= 0) {
            return null;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (RandomEvents type : eligible) {
            roll -= type.getWeight();
            if (roll < 0) {
                return type;
            }
        }

        return eligible.getLast(); // unreachable — the loop exhausts the weight
    }

    private long countParticipants() {
        return this.plugin.getGamemanager().forceItemPlayerMap().values().stream()
                .filter(forceItemPlayer -> !forceItemPlayer.isSpectator())
                .count();
    }
}
