package org.phantazm.zombies.map.shop.display;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.map.shop.Shop;

import java.util.Objects;

public abstract class ItemDisplayBase implements ShopDisplay {
    protected final ItemStack itemStack;
    protected final Point offset;

    protected ItemEntity itemEntity;

    public ItemDisplayBase(@NotNull ItemStack itemStack, @NotNull Point offset) {
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
        this.offset = Objects.requireNonNull(offset, "offset");
    }

    @Override
    public void initialize(@NotNull Shop shop) {
        removeItem();

        itemEntity = new ItemEntity(itemStack);
        itemEntity.setMergeable(false);
        itemEntity.setPickable(false);
        itemEntity.setNoGravity(true);
        itemEntity.setInstance(shop.instance(), shop.center().add(offset));
    }

    @Override
    public void destroy(@NotNull Shop shop) {
        removeItem();
    }

    private void removeItem() {
        if (itemEntity != null) {
            itemEntity.remove();
            itemEntity = null;
        }
    }
}
