/*
 * This file is part of LanternServer, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://github.com/LanternPowered/LanternServer>
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
package org.lanternpowered.server.world.rules;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.lanternpowered.server.game.LanternGame;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.world.ChangeWorldGameRuleEvent;
import org.spongepowered.api.util.Coerce;
import org.spongepowered.api.world.World;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LanternGameRules implements GameRules {

    private final Map<String, LanternGameRule> rules = Maps.newHashMap();
    @Nullable private World world;

    /**
     * Sets the world of the game rules.
     * 
     * @param world the world
     */
    public void setWorld(@Nullable World world) {
        this.world = world;
    }

    @Override
    public GameRule newRule(String name) {
        checkNotNull(name, "name");
        if (this.rules.containsKey(name)) {
            return this.rules.get(name);
        }
        LanternGameRule rule = new LanternGameRule(name);
        this.rules.put(name, rule);
        return rule;
    }

    @Override
    public Optional<GameRule> getRule(String name) {
        return Optional.ofNullable(this.rules.get(checkNotNull(name, "name")));
    }

    @Override
    public Map<String, String> getValues() {
        return Collections.unmodifiableMap(Maps.transformValues(this.rules,
                rule -> rule.value == null ? "" : rule.value));
    }

    @Override
    public List<GameRule> getRules() {
        return ImmutableList.<GameRule>copyOf(this.rules.values());
    }

    private class LanternGameRule implements GameRule {

        private final String name;

        // The value of the game rule
        protected String value;

        // Optional possible types
        protected Boolean valueBoolean = false;
        protected Number valueNumber = 0;

        public LanternGameRule(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public <T> void set(T object) {
            this.set(object, Cause.of(world));
        }

        @Override
        public <T> void set(T object, Cause cause) {
            String oldValue = this.value;
            if (object == null) {
                this.value = null;
                this.valueBoolean = null;
                this.valueNumber = null;
            } else {
                this.value = Coerce.asString(object).get();
                this.valueBoolean = Coerce.asBoolean(object).orElse(null);
                this.valueNumber = Coerce.asDouble(object).orElse(null);
            }
            if (world != null && !Objects.equals(this.value, oldValue)) {
                ChangeWorldGameRuleEvent event = SpongeEventFactory.createChangeWorldGameRuleEvent(
                        cause, oldValue == null ? "" : oldValue, this.value == null ? "" : this.value, this.name, world);
                LanternGame.get().getEventManager().post(event);
            }
        }

        @Override
        public Optional<String> asString() {
            return Optional.ofNullable(this.value);
        }

        @Override
        public Optional<Boolean> asBoolean() {
            return Optional.ofNullable(this.valueBoolean);
        }

        @Override
        public Optional<Double> asDouble() {
            return this.valueNumber == null ? Optional.empty() : Optional.of(this.valueNumber.doubleValue());
        }

        @Override
        public Optional<Float> asFloat() {
            return this.valueNumber == null ? Optional.empty() : Optional.of(this.valueNumber.floatValue());
        }

        @Override
        public Optional<Integer> asInt() {
            return this.valueNumber == null ? Optional.empty() : Optional.of(this.valueNumber.intValue());
        }
    }
}
