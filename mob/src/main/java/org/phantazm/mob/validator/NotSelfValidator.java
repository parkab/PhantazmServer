package org.phantazm.mob.validator;

import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

@Model("mob.target_validator.not_self")
public class NotSelfValidator implements TargetValidator {

    @FactoryMethod
    public NotSelfValidator() {

    }

    @Override
    public boolean valid(@NotNull Entity targeter, @NotNull Entity entity) {
        return entity != targeter;
    }
}
