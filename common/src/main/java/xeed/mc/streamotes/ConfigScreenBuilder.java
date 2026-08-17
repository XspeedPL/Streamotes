package xeed.mc.streamotes;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreenBuilder {
	public static Screen createConfigScreen(Screen parent) {
		ModConfigModel.reload();
		final var def = ModConfigModel.getDefaults();
		final var config = ModConfigModel.getInstance();
		return YetAnotherConfigLib.createBuilder()
			.title(Component.translatable("text.config.streamotes.title"))
			.save(ModConfigModel::save)
			.category(ConfigCategory.createBuilder()
				.name(Component.translatable("text.config.streamotes.category.main"))
				.tooltip(Component.translatable("text.config.streamotes.option.notice"))
				.option(ListOption.<String>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.emoteChannels"))
					.binding(def.emoteChannels, () -> config.emoteChannels, val -> config.emoteChannels = val)
					.controller(StringControllerBuilder::create)
					.initial("")
					.build())
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.twitchGlobalEmotes"))
					.binding(def.twitchGlobalEmotes, () -> config.twitchGlobalEmotes, val -> config.twitchGlobalEmotes = val)
					.controller(BooleanControllerBuilder::create)
					.build())
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.twitchSubscriberEmotes"))
					.binding(def.twitchSubscriberEmotes, () -> config.twitchSubscriberEmotes, val -> config.twitchSubscriberEmotes = val)
					.controller(BooleanControllerBuilder::create)
					.build())
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.bttvEmotes"))
					.binding(def.bttvEmotes, () -> config.bttvEmotes, val -> config.bttvEmotes = val)
					.controller(BooleanControllerBuilder::create)
					.build())
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.bttvChannelEmotes"))
					.binding(def.bttvChannelEmotes, () -> config.bttvChannelEmotes, val -> config.bttvChannelEmotes = val)
					.controller(BooleanControllerBuilder::create)
					.build())
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.ffzEmotes"))
					.binding(def.ffzEmotes, () -> config.ffzEmotes, val -> config.ffzEmotes = val)
					.controller(BooleanControllerBuilder::create)
					.build())
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.ffzChannelEmotes"))
					.binding(def.ffzChannelEmotes, () -> config.ffzChannelEmotes, val -> config.ffzChannelEmotes = val)
					.controller(BooleanControllerBuilder::create)
					.build())
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.x7tvEmotes"))
					.binding(def.x7tvEmotes, () -> config.x7tvEmotes, val -> config.x7tvEmotes = val)
					.controller(BooleanControllerBuilder::create)
					.build())
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.x7tvChannelEmotes"))
					.binding(def.x7tvChannelEmotes, () -> config.x7tvChannelEmotes, val -> config.x7tvChannelEmotes = val)
					.controller(BooleanControllerBuilder::create)
					.build())
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.colorEmotes"))
					.binding(def.colorEmotes, () -> config.colorEmotes, val -> config.colorEmotes = val)
					.controller(BooleanControllerBuilder::create)
					.build())
				.option(Option.<ActivationOption>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.activationMode"))
					.binding(def.activationMode, () -> config.activationMode, val -> config.activationMode = val)
					.controller(opt -> EnumControllerBuilder.create(opt).enumClass(ActivationOption.class))
					.build())
				.option(Option.<ReportOption>createBuilder()
					.name(Component.translatable("text.config.streamotes.option.errorReporting"))
					.binding(def.errorReporting, () -> config.errorReporting, val -> config.errorReporting = val)
					.controller(opt -> EnumControllerBuilder.create(opt).enumClass(ReportOption.class))
					.build())
				.build())
			.build().generateScreen(parent);
	}
}
