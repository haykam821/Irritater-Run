package io.github.haykam821.irritaterrun.event;

import io.github.haykam821.irritaterrun.entity.IrritaterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface IrritaterCatchEntityEvent {
	StimulusEvent<IrritaterCatchEntityEvent> EVENT = StimulusEvent.create(IrritaterCatchEntityEvent.class, context -> {
		return (irritater, target) -> {
			try {
				for (IrritaterCatchEntityEvent listener : context.getListeners()) {
					ActionResult result = listener.onIrritaterCatchEntity(irritater, target);

					if (result != ActionResult.PASS) {
						return result;
					}
				}
			} catch (Throwable throwable) {
				context.handleException(throwable);
			}

			return ActionResult.PASS;
		};
	});

	ActionResult onIrritaterCatchEntity(IrritaterEntity irritater, Entity target);
}
