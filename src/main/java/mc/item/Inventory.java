package mc.item;

/** Player inventory: slots 0-8 are the hotbar, 9-35 the main inventory. */
public final class Inventory {
    public final ItemStack[] slots = new ItemStack[36];
    public int selected;

    public ItemStack held() { return slots[selected]; }

    public void setHeld(ItemStack s) { slots[selected] = s; }

    /** Adds as much of the stack as fits; returns what's left (count 0 if everything fit). */
    public ItemStack add(ItemStack stack) {
        if (ItemStack.isEmpty(stack)) return stack;
        // Merge into existing stacks first (hotbar first, like Minecraft)
        for (int i = 0; i < slots.length && stack.count > 0; i++) {
            ItemStack s = slots[i];
            if (s != null && s.canMerge(stack) && s.count < s.item.maxStack) {
                int n = Math.min(stack.count, s.item.maxStack - s.count);
                s.count += n;
                stack.count -= n;
            }
        }
        for (int i = 0; i < slots.length && stack.count > 0; i++) {
            if (ItemStack.isEmpty(slots[i])) {
                int n = Math.min(stack.count, stack.item.maxStack);
                slots[i] = new ItemStack(stack.item, n, stack.damage);
                stack.count -= n;
            }
        }
        return stack;
    }

    public int count(Item item) {
        int n = 0;
        for (ItemStack s : slots) if (s != null && s.item == item) n += s.count;
        return n;
    }

    public void cleanup() {
        for (int i = 0; i < slots.length; i++) if (slots[i] != null && slots[i].count <= 0) slots[i] = null;
    }

    public void clear() {
        java.util.Arrays.fill(slots, null);
    }
}
