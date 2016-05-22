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
package org.lanternpowered.server.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import org.lanternpowered.server.util.collect.EmptyIterator;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.EmptyInventory;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.translation.Translation;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Bottom type / empty results set for inventory queries.
 */
public class EmptyInventoryImpl extends InventoryBase implements EmptyInventory {

    public EmptyInventoryImpl(@Nullable Inventory parent) {
        super(parent, null);
    }

    @Override
    public <T extends Inventory> Iterable<T> slots() {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T first() {
        return (T) this;
    }

    @Override
    public Optional<ItemStack> poll(Predicate<ItemStack> matcher) {
        return Optional.empty();
    }

    @Override
    public Optional<ItemStack> poll(int limit, Predicate<ItemStack> matcher) {
        return Optional.empty();
    }

    @Override
    public Optional<ItemStack> peek(Predicate<ItemStack> matcher) {
        return Optional.empty();
    }

    @Override
    public Optional<ItemStack> peek(int limit, Predicate<ItemStack> matcher) {
        return Optional.empty();
    }

    @Override
    public void clear() {
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int totalItems() {
        return 0;
    }

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean contains(ItemStack stack) {
        return false;
    }

    @Override
    public boolean contains(ItemType type) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 0;
    }

    @Override
    public void setMaxStackSize(int size) {
    }

    @Override
    public <T extends InventoryProperty<?, ?>> Collection<T> getProperties(Inventory child, Class<T> property) {
        return Collections.emptyList();
    }

    @Override
    public <T extends InventoryProperty<?, ?>> Collection<T> getProperties(Class<T> property) {
        return Collections.emptyList();
    }

    @Override
    public <T extends InventoryProperty<?, ?>> Optional<T> getProperty(Inventory child, Class<T> property, Object key) {
        return Optional.empty();
    }

    @Override
    public <T extends InventoryProperty<?, ?>> Optional<T> getProperty(Class<T> property, Object key) {
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(Class<?>... types) {
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(ItemType... types) {
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(ItemStack... types) {
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(InventoryProperty<?, ?>... props) {
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(Translation... names) {
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(String... names) {
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(Object... args) {
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(Predicate<Inventory> matcher) {
        return (T) this;
    }

    @Override
    public Iterator<Inventory> iterator() {
        return EmptyIterator.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T next() {
        return (T) this;
    }

    @Override
    public FastOfferResult offerFast(ItemStack stack) {
        return new FastOfferResult(checkNotNull(stack, "stack"), false);
    }

    @Override
    public InventoryTransactionResult offer(ItemStack stack) {
        return InventoryTransactionResult.builder().type(InventoryTransactionResult.Type.FAILURE).reject(stack).build();
    }

    @Override
    public InventoryTransactionResult set(ItemStack stack) {
        return InventoryTransactionResult.builder().type(InventoryTransactionResult.Type.FAILURE).reject(stack).build();
    }
}