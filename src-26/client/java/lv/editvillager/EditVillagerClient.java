package lv.editvillager;

import net.fabricmc.api.ClientModInitializer;

public class EditVillagerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TradeFileClientNetworking.init();
	}
}