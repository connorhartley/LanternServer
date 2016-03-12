/*
 * This file is part of LanternServer, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://github.com/LanternPowered>
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the Software), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lanternpowered.server.scoreboard;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.lanternpowered.server.network.message.Message;
import org.lanternpowered.server.network.vanilla.message.type.play.MessagePlayOutScoreboardObjective;
import org.lanternpowered.server.network.vanilla.message.type.play.MessagePlayOutScoreboardScore;
import org.lanternpowered.server.text.LanternTexts;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criterion;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayMode;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;
import org.spongepowered.api.text.Text;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LanternObjective implements Objective {

    private final String name;
    private final Criterion criterion;
    final Map<Text, Score> scores = Maps.newHashMap();
    final Set<Scoreboard> scoreboards = Sets.newHashSet();
    private ObjectiveDisplayMode displayMode;
    private Text displayName;

    LanternObjective(String name, Criterion criterion, ObjectiveDisplayMode displayMode, Text displayName) {
        this.displayName = displayName;
        this.displayMode = displayMode;
        this.criterion = criterion;
        this.name = name;
    }

    void addScoreboard(Scoreboard scoreboard) {
        this.scoreboards.add(scoreboard);
    }

    void removeScoreboard(Scoreboard scoreboard) {
        this.scoreboards.remove(scoreboard);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Text getDisplayName() {
        return this.displayName;
    }

    @Override
    public void setDisplayName(Text displayName) throws IllegalArgumentException {
        String legacy = LanternTexts.toLegacy(checkNotNull(displayName, "displayName"));
        checkArgument(legacy.length() <= 32, "displayName may not be longer then 32 characters (in legacy format)");
        this.displayName = displayName;
        this.sendObjectiveUpdate();
    }

    private void sendObjectiveUpdate() {
        if (!this.scoreboards.isEmpty()) {
            List<Message> message = Collections.singletonList(new MessagePlayOutScoreboardObjective.Update(this.name,
                    this.displayName, this.displayMode));
            for (Scoreboard scoreboard : this.scoreboards) {
                ((LanternScoreboard) scoreboard).sendToPlayers(() -> message);
            }
        }
    }

    @Override
    public Criterion getCriterion() {
        return this.criterion;
    }

    @Override
    public ObjectiveDisplayMode getDisplayMode() {
        return this.displayMode;
    }

    @Override
    public void setDisplayMode(ObjectiveDisplayMode displayMode) {
        this.displayMode = checkNotNull(displayMode, "displayMode");
        this.sendObjectiveUpdate();
    }

    @Override
    public Map<Text, Score> getScores() {
        return ImmutableMap.copyOf(this.scores);
    }

    @Override
    public boolean hasScore(Text name) {
        return this.scores.containsKey(checkNotNull(name, "name"));
    }

    @Override
    public void addScore(Score score) throws IllegalArgumentException {
        if (this.scores.containsKey(checkNotNull(score, "score").getName())) {
            throw new IllegalArgumentException(String.format("A score with the name %s already exists!",
                    LanternTexts.toLegacy(score.getName())));
        }
        this.scores.put(score.getName(), score);
        ((LanternScore) score).addObjective(this);
        if (!this.scoreboards.isEmpty()) {
            List<Message> message = Collections.singletonList(new MessagePlayOutScoreboardScore.CreateOrUpdate(this.getName(),
                    LanternTexts.toLegacy(score.getName()), score.getScore()));
            for (Scoreboard scoreboard : this.scoreboards) {
                ((LanternScoreboard) scoreboard).sendToPlayers(() -> message);
            }
        }
    }

    @Override
    public Score getOrCreateScore(Text name) {
        return this.scores.computeIfAbsent(name, name1 -> {
            LanternScore score = new LanternScore(name1);
            score.addObjective(this);
            return score;
        });
    }

    @Override
    public boolean removeScore(Score score) {
        if (this.scores.remove(checkNotNull(score, "score").getName(), score)) {
            ((LanternScore) score).removeObjective(this);
            this.updateClientAfterRemove(score);
            return true;
        }
        return false;
    }

    private void updateClientAfterRemove(Score score) {
        Map<Objective, Message> messages = Maps.newHashMap();
        for (Scoreboard scoreboard : this.scoreboards) {
            ((LanternScoreboard) scoreboard).sendToPlayers(() -> Collections.singletonList(
                    messages.computeIfAbsent(this, obj -> new MessagePlayOutScoreboardScore.Remove(
                            this.getName(), LanternTexts.toLegacy(score.getName())))));
        }
    }

    @Override
    public boolean removeScore(Text name) {
        Score score = this.scores.remove(checkNotNull(name, "name"));
        if (score != null) {
            ((LanternScore) score).removeObjective(this);
            this.updateClientAfterRemove(score);
            return true;
        }
        return false;
    }

    @Override
    public Set<Scoreboard> getScoreboards() {
        return ImmutableSet.copyOf(this.scoreboards);
    }
}