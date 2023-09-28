package io.github.haykam821.irritaterrun.event;

import io.github.haykam821.irritaterrun.entity.IrritaterEntity;
import net.minecraft.entity.Entity;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface IrritaterSelectTargetEvent {
	StimulusEvent<IrritaterSelectTargetEvent> EVENT = StimulusEvent.create(IrritaterSelectTargetEvent.class, context -> {
		return irritater -> {
			try {
				for (IrritaterSelectTargetEvent listener : context.getListeners()) {
					Entity target = listener.onIrritaterSelectTarget(irritater);

					if (target != null) {
						return target;
					}
				}
			} catch (Throwable throwable) {
				context.handleException(throwable);
			}

			return null;
		};
	});

	Entity onIrritaterSelectTarget(IrritaterEntity irritater);
}
