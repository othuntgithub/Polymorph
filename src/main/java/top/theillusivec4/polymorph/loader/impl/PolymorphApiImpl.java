/*
 * Copyright (c) 2020 C4
 *
 * This file is part of Polymorph, a mod made for Minecraft.
 *
 * Polymorph is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * Polymorph is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Polymorph.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package top.theillusivec4.polymorph.loader.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import top.theillusivec4.polymorph.api.PolymorphApi;
import top.theillusivec4.polymorph.api.type.CraftingProvider;
import top.theillusivec4.polymorph.api.type.FurnaceProvider;
import top.theillusivec4.polymorph.api.type.PersistentSelector;
import top.theillusivec4.polymorph.api.type.PolyProvider;
import top.theillusivec4.polymorph.api.type.RecipeSelector;
import top.theillusivec4.polymorph.core.client.selector.CraftingRecipeSelector;
import top.theillusivec4.polymorph.core.client.selector.FurnaceRecipeSelector;

public class PolymorphApiImpl implements PolymorphApi {

  public static final PolymorphApi INSTANCE = new PolymorphApiImpl();

  private static final List<Function<ScreenHandler, PolyProvider<? extends Inventory, ? extends Recipe<?>>>>
      providerFunctions = new ArrayList<>();

  private static final List<Function<BlockEntity, PersistentSelector>> entityFunctions =
      new ArrayList<>();

  @Override
  public void addProvider(Function<ScreenHandler, PolyProvider<?, ?>> providerFunction) {
    providerFunctions.add(providerFunction);
  }

  @Override
  public void addEntityProvider(Function<BlockEntity, PersistentSelector> entityFunction,
                                Function<ScreenHandler, PolyProvider<?, ?>> providerFunction) {
    this.addProvider(providerFunction);
    entityFunctions.add(entityFunction);
  }

  @Override
  public Optional<PersistentSelector> getSelector(BlockEntity te) {

    for (Function<BlockEntity, PersistentSelector> entityFunction : entityFunctions) {
      PersistentSelector selector = entityFunction.apply(te);

      if (selector != null) {
        return Optional.of(selector);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<PolyProvider<?, ?>> getProvider(ScreenHandler container) {

    for (Function<ScreenHandler, PolyProvider<? extends Inventory, ? extends Recipe<?>>> function : providerFunctions) {
      PolyProvider<?, ?> polyProvider = function.apply(container);

      if (polyProvider != null) {
        return Optional.of(polyProvider);
      }
    }
    Slot resultSlot = null;
    CraftingInventory craftingInventory = null;

    for (Slot slot : container.slots) {

      if (resultSlot == null && slot.inventory instanceof CraftingResultInventory) {
        resultSlot = slot;
      } else if (craftingInventory == null && slot.inventory instanceof CraftingInventory) {
        craftingInventory = (CraftingInventory) slot.inventory;
      }

      if (resultSlot != null && craftingInventory != null) {
        break;
      }
    }
    return Optional.ofNullable(resultSlot != null && craftingInventory != null ?
        new SimpleCraftingProvider(container, craftingInventory, resultSlot) : null);
  }

  @Override
  public RecipeSelector<CraftingInventory, CraftingRecipe> createCraftingSelector(
      HandledScreen<?> screen, CraftingProvider provider) {
    return new CraftingRecipeSelector(screen, provider);
  }

  @Override
  public RecipeSelector<Inventory, AbstractCookingRecipe> createFurnaceSelector(
      HandledScreen<?> screen, FurnaceProvider provider) {
    return new FurnaceRecipeSelector(screen, provider);
  }

  private static class SimpleCraftingProvider implements CraftingProvider {

    final ScreenHandler container;
    final CraftingInventory craftingInventory;
    final Slot resultSlot;

    public SimpleCraftingProvider(ScreenHandler container, CraftingInventory craftingInventory,
                                  Slot resultSlot) {
      this.container = container;
      this.craftingInventory = craftingInventory;
      this.resultSlot = resultSlot;
    }

    @Override
    public ScreenHandler getScreenHandler() {
      return this.container;
    }

    @Override
    public CraftingInventory getInventory() {
      return this.craftingInventory;
    }

    @Override
    public Slot getOutputSlot() {
      return this.resultSlot;
    }
  }
}
