package org.lanternpowered.server.inventory.neww;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.lanternpowered.server.inventory.*;
import org.lanternpowered.server.inventory.neww.filter.EquipmentItemFilter;
import org.lanternpowered.server.inventory.neww.filter.ItemFilter;
import org.lanternpowered.server.inventory.neww.filter.PropertyItemFilters;
import org.lanternpowered.server.util.collect.Lists2;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.AcceptsItems;
import org.spongepowered.api.item.inventory.property.ArmorSlotType;
import org.spongepowered.api.item.inventory.property.EquipmentSlotType;
import org.spongepowered.api.item.inventory.property.InventoryCapacity;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

@SuppressWarnings("unchecked")
public abstract class AbstractSlot extends AbstractMutableInventory implements ISlot {

    public static final int DEFAULT_MAX_STACK_SIZE = 64;

    /**
     * Constructs a new {@link Builder}.
     *
     * @return The builder
     */
    public static Builder<?> builder() {
        return new Builder();
    }

    /**
     * The {@link LanternItemStack} that is stored in this slot.
     */
    @Nullable private LanternItemStack itemStack;

    /**
     * The maximum stack size that can fit in this slot.
     */
    private int maxStackSize = DEFAULT_MAX_STACK_SIZE;

    /**
     * All the {@link LanternContainer}s this slot is attached to, all
     * these containers will be notified if anything changes. A weak
     * set is used to avoid leaks when a container isn't properly cleaned up.
     */
    private final Set<SlotChangeTracker> trackers = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * {@link SlotChangeListener}s may track slot changes, these listeners
     * have to be removed manually after they are no longer needed.
     */
    private final List<SlotChangeListener> changeListeners = Lists2.nonNullArrayList();

    /**
     * The {@link ItemFilter} that defines which {@link ItemStack}s can be put in this slot.
     */
    @Nullable private ItemFilter itemFilter;

    /**
     * Adds the {@link SlotChangeTracker}.
     *
     * @param tracker The slot change tracker
     */
    public void addTracker(SlotChangeTracker tracker) {
        this.trackers.add(tracker);
    }

    /**
     * Removes the {@link SlotChangeTracker}.
     *
     * @param tracker The slot change tracker
     */
    public void removeTracker(SlotChangeTracker tracker) {
        this.trackers.remove(tracker);
    }

    /**
     * Gets the raw {@link LanternItemStack}. Does not make a copy.
     *
     * @return The raw item stack
     */
    @Nullable
    LanternItemStack getRawItemStack() {
        return this.itemStack;
    }

    /**
     * Sets the raw {@link LanternItemStack}. Does not make a copy.
     *
     * @param itemStack The raw item stack
     */
    void setRawItemStack(@Nullable ItemStack itemStack) {
        itemStack = itemStack == null || itemStack.isEmpty() ? null : itemStack;
        if (!Objects.equals(this.itemStack, itemStack)) {
            queueUpdate();
        }
        this.itemStack = (LanternItemStack) itemStack;
    }

    void init(@Nullable ItemFilter itemFilter) {
        this.itemFilter = itemFilter;
    }

    @Nullable
    ItemFilter getFilter() {
        return this.itemFilter;
    }

    /**
     * Queues this slot to be updated and trigger the listeners.
     */
    private void queueUpdate() {
        for (SlotChangeListener listener : this.changeListeners) {
            listener.accept(this);
        }
        for (SlotChangeTracker tracker : this.trackers) {
            tracker.queueSlotChange(this);
        }
    }

    @Override
    protected void setCarrier(Carrier carrier) {
    }

    @Override
    public void addChangeListener(SlotChangeListener listener) {
        checkNotNull(listener, "listener");
        this.changeListeners.add(listener);
    }

    @Override
    protected List<AbstractSlot> getSlotInventories() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public int getStackSize() {
        return LanternItemStack.isEmpty(this.itemStack) ? 0 : this.itemStack.getQuantity();
    }

    @Override
    public boolean isValidItem(ItemStack stack) {
        checkNotNull(stack, "stack");
        return this.itemFilter == null || this.itemFilter.isValid(stack);
    }

    @Override
    public Optional<ItemStack> poll(Predicate<ItemStack> matcher) {
        checkNotNull(matcher, "matcher");
        if (this.itemStack == null || !matcher.test(this.itemStack)) {
            return Optional.empty();
        }
        final ItemStack itemStack = this.itemStack;
        // Just remove the item, the complete stack was
        // being polled
        this.itemStack = null;
        queueUpdate();
        return Optional.of(itemStack);
    }

    @Override
    public Optional<ItemStack> poll(int limit, Predicate<ItemStack> matcher) {
        checkNotNull(matcher, "matcher");
        checkArgument(limit >= 0, "Limit may not be negative");
        ItemStack itemStack = this.itemStack;
        // There is no item available
        if (itemStack == null || !matcher.test(itemStack)) {
            return Optional.empty();
        }
        // Split the stack if needed
        if (limit < itemStack.getQuantity()) {
            itemStack.setQuantity(itemStack.getQuantity() - limit);
            // Clone the item to be returned
            itemStack = itemStack.copy();
            itemStack.setQuantity(limit);
        } else {
            this.itemStack = null;
        }
        queueUpdate();
        return Optional.of(itemStack);
    }

    @Override
    public Optional<ItemStack> peek(Predicate<ItemStack> matcher) {
        checkNotNull(matcher, "matcher");
        return Optional.ofNullable(this.itemStack == null || !matcher.test(this.itemStack) ? null : this.itemStack.copy());
    }

    @Override
    public Optional<ItemStack> peek(int limit, Predicate<ItemStack> matcher) {
        checkNotNull(matcher, "matcher");
        checkArgument(limit >= 0, "Limit may not be negative");
        ItemStack itemStack = this.itemStack;
        // There is no item available
        if (itemStack == null || !matcher.test(itemStack)) {
            return Optional.empty();
        }
        itemStack = itemStack.copy();
        // Split the stack if needed
        if (limit < itemStack.getQuantity()) {
            itemStack.setQuantity(limit);
        }
        return Optional.of(itemStack);
    }

    @Override
    public FastOfferResult offerFast(ItemStack stack) {
        checkNotNull(stack, "stack");
        if (LanternItemStack.toNullable(stack) == null) {
            return new FastOfferResult(stack, false);
        }
        final int maxStackSize = Math.min(stack.getMaxStackQuantity(), this.maxStackSize);
        if (this.itemStack != null && (!this.itemStack.similarTo(stack) ||
                this.itemStack.getQuantity() >= maxStackSize) || !isValidItem(stack)) {
            return new FastOfferResult(stack, false);
        }
        // Get the amount of space we have left
        final int availableSpace = this.itemStack == null ? maxStackSize :
                maxStackSize - this.itemStack.getQuantity();
        final int quantity = stack.getQuantity();
        if (quantity > availableSpace) {
            if (this.itemStack == null) {
                this.itemStack = (LanternItemStack) stack.copy();
            }
            this.itemStack.setQuantity(maxStackSize);
            stack = stack.copy();
            stack.setQuantity(quantity - availableSpace);
            queueUpdate();
            return new FastOfferResult(stack, true);
        } else {
            if (this.itemStack == null) {
                this.itemStack = (LanternItemStack) stack.copy();
            } else {
                this.itemStack.setQuantity(this.itemStack.getQuantity() + quantity);
            }
            queueUpdate();
            return FastOfferResult.SUCCESS_NO_REJECTED_ITEM;
        }
    }

    @Override
    public InventoryTransactionResult set(@Nullable ItemStack stack) {
        stack = LanternItemStack.toNullable(stack);
        boolean fail = false;
        if (stack != null) {
            if (stack.getQuantity() <= 0) {
                stack = null;
            } else {
                fail = !isValidItem(stack);
            }
        }
        if (fail) {
            return InventoryTransactionResult.builder()
                    .type(InventoryTransactionResult.Type.FAILURE)
                    .reject(stack)
                    .build();
        }
        InventoryTransactionResult.Builder resultBuilder = InventoryTransactionResult.builder()
                .type(InventoryTransactionResult.Type.SUCCESS);
        if (this.itemStack != null) {
            resultBuilder.replace(this.itemStack);
        }
        if (stack != null) {
            stack = stack.copy();
            final int maxStackSize = Math.min(stack.getMaxStackQuantity(), this.maxStackSize);
            final int quantity = stack.getQuantity();
            if (quantity > maxStackSize) {
                stack.setQuantity(maxStackSize);
                // Create the rest stack that was rejected,
                // because the inventory doesn't allow so many items
                stack = stack.copy();
                stack.setQuantity(quantity - maxStackSize);
                resultBuilder.reject(stack);
            }
        }
        this.itemStack = (LanternItemStack) stack;
        queueUpdate();
        return resultBuilder.build();
    }

    @Override
    public <T extends Inventory> Iterable<T> slots() {
        return Collections.emptyList();
    }

    @Override
    public void clear() {
        if (this.itemStack != null) {
            this.itemStack = null;
            queueUpdate();
        }
    }

    @Override
    public int size() {
        return this.itemStack == null || this.itemStack.isEmpty() ? 0 : 1;
    }

    @Override
    public int totalItems() {
        return this.itemStack == null || this.itemStack.isEmpty() ? 0 : this.itemStack.getQuantity();
    }

    @Override
    public int capacity() {
        return 1;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean contains(ItemStack stack) {
        checkNotNull(stack, "stack");
        return containsAny(stack) && this.itemStack.getQuantity() >= stack.getQuantity();
    }

    @Override
    public boolean containsAny(ItemStack stack) {
        checkNotNull(stack, "stack");
        return !LanternItemStack.isEmpty(this.itemStack) && LanternItemStack.areSimilar(this.itemStack, stack);
    }

    @Override
    public boolean contains(ItemType type) {
        checkNotNull(type, "type");
        return !LanternItemStack.isEmpty(this.itemStack) && this.itemStack.getType().equals(type);
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStackSize;
    }

    @Override
    public void setMaxStackSize(int size) {
        checkArgument(size > 0, "Size must be greater then 0");
        this.maxStackSize = size;
    }

    @Override
    public boolean containsInventory(Inventory inventory) {
        return inventory == this;
    }

    @Override
    public Iterator<Inventory> iterator() {
        return Collections.emptyIterator();
    }

    public static final class Builder<T extends AbstractSlot> extends AbstractBuilder<T, AbstractSlot, Builder<T>> {

        private static final Supplier<AbstractSlot> DEFAULT_SUPPLIER = DefaultSlot::new;
        @Nullable private ItemFilter itemFilter;

        @Nullable private ItemFilter cachedResultItemFilter;
        private boolean hasItemFilter;

        private Builder() {
            typeSupplier(DEFAULT_SUPPLIER);
        }

        /**
         * Sets the {@link ItemFilter}.
         *
         * @param itemFilter The item filter
         * @return This builder, for chaining
         */
        public Builder<T> filter(ItemFilter itemFilter) {
            checkNotNull(itemFilter, "itemFilter");
            this.itemFilter = itemFilter;
            // Regenerate the result item filter
            this.hasItemFilter = true;
            this.cachedResultItemFilter = null;
            invalidateCachedArchetype();
            return this;
        }

        @Override
        public Builder<T> property(InventoryProperty<String, ?> property) {
            checkArgument(!(property instanceof SlotIndex), "The slot index may not be set through a property.");
            checkArgument(!(property instanceof InventoryCapacity), "The slot capacity cannot be set.");
            super.property(property);
            // Regenerate the result item filter
            if (property instanceof EquipmentSlotType || property instanceof AcceptsItems) {
                this.hasItemFilter = true;
                this.cachedResultItemFilter = null;
                invalidateCachedArchetype();
            }
            return this;
        }

        @Override
        protected void build(AbstractSlot inventory) {
            if (this.cachedResultItemFilter == null && this.hasItemFilter) {
                ItemFilter itemFilter = null;
                // Attempt to generate the ItemFilter
                final AcceptsItems acceptsItems = (AcceptsItems) this.properties.get(AcceptsItems.class);
                if (acceptsItems != null) {
                    itemFilter = PropertyItemFilters.of(acceptsItems);
                }
                final EquipmentSlotType equipmentSlotType = (EquipmentSlotType) this.properties.get(EquipmentSlotType.class);
                if (equipmentSlotType != null) {
                    EquipmentItemFilter equipmentItemFilter = EquipmentItemFilter.of(equipmentSlotType);
                    if (itemFilter != null) {
                        equipmentItemFilter = equipmentItemFilter.andThen(itemFilter);
                    }
                    itemFilter = equipmentItemFilter;
                }
                final ArmorSlotType armorSlotType = (ArmorSlotType) this.properties.get(ArmorSlotType.class);
                if (armorSlotType != null) {
                    EquipmentItemFilter equipmentItemFilter = EquipmentItemFilter.of(armorSlotType);
                    if (itemFilter != null) {
                        equipmentItemFilter = equipmentItemFilter.andThen(itemFilter);
                    }
                    itemFilter = equipmentItemFilter;
                }
                this.cachedResultItemFilter = itemFilter;
            }
            inventory.init(this.cachedResultItemFilter);
        }

        @Override
        protected Builder<T> copy() {
            final Builder<T> copy = new Builder<>();
            copy.supplier = this.supplier;
            copy.itemFilter = this.itemFilter;
            copy.hasItemFilter = this.hasItemFilter;
            copy.cachedResultItemFilter = this.cachedResultItemFilter;
            return copy;
        }
    }
}
