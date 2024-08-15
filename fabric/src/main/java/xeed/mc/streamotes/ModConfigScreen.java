package xeed.mc.streamotes;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public class ModConfigScreen implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ModConfigModel.reload();
			final var def = ModConfigModel.getDefaults();
			final var config = ModConfigModel.getInstance();
			return YetAnotherConfigLib.createBuilder()
				.title(Text.translatable("text.config.streamotes.title"))
				.save(ModConfigModel::save)
				.category(ConfigCategory.createBuilder()
					.name(Text.translatable("text.config.streamotes.title"))
					.option(ListOption.<String>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.emoteChannels"))
						.binding(def.emoteChannels, () -> config.emoteChannels, val -> config.emoteChannels = val)
						.controller(StringControllerBuilder::create)
						.initial("")
						.build())
					.option(Option.<Boolean>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.twitchGlobalEmotes"))
						.binding(def.twitchGlobalEmotes, () -> config.twitchGlobalEmotes, val -> config.twitchGlobalEmotes = val)
						.controller(BooleanControllerBuilder::create)
						.build())
					.option(Option.<Boolean>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.twitchSubscriberEmotes"))
						.binding(def.twitchSubscriberEmotes, () -> config.twitchSubscriberEmotes, val -> config.twitchSubscriberEmotes = val)
						.controller(BooleanControllerBuilder::create)
						.build())
					.option(Option.<Boolean>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.bttvEmotes"))
						.binding(def.bttvEmotes, () -> config.bttvEmotes, val -> config.bttvEmotes = val)
						.controller(BooleanControllerBuilder::create)
						.build())
					.option(Option.<Boolean>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.bttvChannelEmotes"))
						.binding(def.bttvChannelEmotes, () -> config.bttvChannelEmotes, val -> config.bttvChannelEmotes = val)
						.controller(BooleanControllerBuilder::create)
						.build())
					.option(Option.<Boolean>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.ffzEmotes"))
						.binding(def.ffzEmotes, () -> config.ffzEmotes, val -> config.ffzEmotes = val)
						.controller(BooleanControllerBuilder::create)
						.build())
					.option(Option.<Boolean>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.ffzChannelEmotes"))
						.binding(def.ffzChannelEmotes, () -> config.ffzChannelEmotes, val -> config.ffzChannelEmotes = val)
						.controller(BooleanControllerBuilder::create)
						.build())
					.option(Option.<Boolean>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.x7tvEmotes"))
						.binding(def.x7tvEmotes, () -> config.x7tvEmotes, val -> config.x7tvEmotes = val)
						.controller(BooleanControllerBuilder::create)
						.build())
					.option(Option.<Boolean>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.x7tvChannelEmotes"))
						.binding(def.x7tvChannelEmotes, () -> config.x7tvChannelEmotes, val -> config.x7tvChannelEmotes = val)
						.controller(BooleanControllerBuilder::create)
						.build())
					.option(Option.<Boolean>createBuilder()
						.name(Text.translatable("text.config.streamotes.option.processColons"))
						.binding(def.processColons, () -> config.processColons, val -> config.processColons = val)
						.controller(BooleanControllerBuilder::create)
						.build())
					.build())
				.build().generateScreen(parent);
		};
	}
}
