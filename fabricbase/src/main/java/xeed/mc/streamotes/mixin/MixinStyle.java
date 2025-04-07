package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xeed.mc.streamotes.EmotedStyle;
import xeed.mc.streamotes.emoticon.Emoticon;

import java.util.Objects;

@Mixin(Style.class)
public abstract class MixinStyle implements EmotedStyle {
	@Unique
	private Emoticon emote;

	@Inject(method = "*", at = @At("TAIL"))
	private void atAnyEnd(CallbackInfoReturnable<?> callback) {
		if (!Objects.equals(callback.getId(), "withEmote") && callback.getReturnValue() instanceof Style style) {
			style.setEmote(emote);
		}
	}

	@Shadow
	public abstract Style withBold(Boolean bold);

	@Shadow
	public abstract boolean isBold();

	@Override
	public Emoticon getEmote() {
		return emote;
	}

	/// Remark: This should only be used on new objects, so immutability is not violated (too much)
	@Override
	public void setEmote(Emoticon emote) {
		this.emote = emote;
	}

	/// Remark: This is a very ugly and hacky way to adhere to immutability rules
	@Override
	public Style withEmote(Emoticon emote) {
		// TODO: Maybe figure out a cleaner way to do this? Maybe
		boolean bold = isBold();
		var self = withBold(bold);

		if (Objects.equals(self.getEmote(), emote)) return self;

		self = withBold(!bold);
		self.setEmote(emote);
		return self.withBold(bold);
	}

	@WrapMethod(method = "hashCode")
	private int atGetHashCode(Operation<Integer> original) {
		return Objects.hash(original.call(), emote);
	}

	@WrapMethod(method = "equals")
	private boolean atEquals(Object o, Operation<Boolean> original) {
		return original.call(o) && o instanceof Style other && Objects.equals(emote, other.getEmote());
	}
}
