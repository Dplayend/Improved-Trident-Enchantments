package com.dplayend.ite.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({TridentItem.class})
public class MixTridentItem {
    @Unique TridentItem that = (TridentItem)(Object)this;
    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        ci.cancel();

        if (user instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity)user;
            int i = that.getMaxUseTime(stack) - remainingUseTicks;
            if (i >= 10) {
                int j = EnchantmentHelper.getRiptide(stack);
                if (!world.isClient) {
                    stack.damage(1, playerEntity, (p) -> p.sendToolBreakStatus(user.getActiveHand()));
                    if (EnchantmentHelper.getLevel(Enchantments.RIPTIDE, stack) == 0) {
                        createTrident(stack, world, playerEntity);
                    }
                    if (EnchantmentHelper.getLevel(Enchantments.RIPTIDE, stack) > 0) {
                        if (playerEntity.isTouchingWaterOrRain() && playerEntity.isSneaking()) {
                            createTrident(stack, world, playerEntity);
                        } else if (!playerEntity.isTouchingWaterOrRain()) {
                            createTrident(stack, world, playerEntity);
                        }
                    }
                }
                if (EnchantmentHelper.getLevel(Enchantments.RIPTIDE, stack) > 0 && playerEntity.isTouchingWaterOrRain() && !playerEntity.isSneaking()) {
                    playerEntity.incrementStat(Stats.USED.getOrCreateStat(that));
                    float f = playerEntity.getYaw();
                    float g = playerEntity.getPitch();
                    float h = -MathHelper.sin(f * 0.017453292F) * MathHelper.cos(g * 0.017453292F);
                    float k = -MathHelper.sin(g * 0.017453292F);
                    float l = MathHelper.cos(f * 0.017453292F) * MathHelper.cos(g * 0.017453292F);
                    float m = MathHelper.sqrt(h * h + k * k + l * l);
                    float n = 3.0F * ((1.0F + (float)j) / 4.0F);
                    h *= n / m;
                    k *= n / m;
                    l *= n / m;
                    playerEntity.addVelocity(h, k, l);
                    playerEntity.setRiptideTicks(20);
                    if (playerEntity.isOnGround()) {
                        playerEntity.move(MovementType.SELF, new Vec3d(0.0, 1.1999999284744263, 0.0));
                    }

                    SoundEvent soundEvent;
                    if (j >= 3) {
                        soundEvent = SoundEvents.ITEM_TRIDENT_RIPTIDE_3;
                    } else if (j == 2) {
                        soundEvent = SoundEvents.ITEM_TRIDENT_RIPTIDE_2;
                    } else {
                        soundEvent = SoundEvents.ITEM_TRIDENT_RIPTIDE_1;
                    }

                    world.playSoundFromEntity(null, playerEntity, soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        cir.cancel();

        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            cir.setReturnValue(TypedActionResult.fail(itemStack));
        } else {
            user.setCurrentHand(hand);
            cir.setReturnValue(TypedActionResult.consume(itemStack));
        }
    }

    @Unique private void createTrident(ItemStack stack, World world, PlayerEntity playerEntity) {
        TridentEntity tridentEntity = new TridentEntity(world, playerEntity, stack);
        tridentEntity.setProperties(playerEntity, playerEntity.getPitch(), playerEntity.getYaw(), 0.0F, 2.5F, 1.0F);
        if (playerEntity.getAbilities().creativeMode) {
            tridentEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
        }

        world.spawnEntity(tridentEntity);
        world.playSoundFromEntity(null, tridentEntity, SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (!playerEntity.getAbilities().creativeMode) {
            playerEntity.getInventory().removeOne(stack);
        }
    }
}
