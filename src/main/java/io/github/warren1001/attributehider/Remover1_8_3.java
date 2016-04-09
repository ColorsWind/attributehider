package io.github.warren1001.attributehider;

import java.lang.reflect.Field;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import net.minecraft.server.v1_8_R3.ContainerMerchant;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityVillager;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.MerchantRecipe;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;

public class Remover1_8_3 extends Remover {
	
	@Override
	public void remove(Player p) {
		EntityPlayer player = ((CraftPlayer)p).getHandle();
		try {
			Field field = ContainerMerchant.class.getDeclaredField("merchant");
			field.setAccessible(true);
			for(MerchantRecipe recipe : ((EntityVillager)field.get(player.activeContainer)).getOffers(player)) {
				remove(recipe.getBuyItem1(), recipe.getBuyItem2(), recipe.getBuyItem3());
			}
		}
		catch(NoSuchFieldException | SecurityException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	private void remove(ItemStack... items) {
		for(ItemStack item : items) {
			if(item == null || Item.getId(item.getItem()) == 386) continue;
			NBTTagCompound tag = item.hasTag() ? item.getTag() : new NBTTagCompound();
			tag.set("AttributeModifiers", new NBTTagList());
			item.setTag(tag);
		}
	}
	
}
