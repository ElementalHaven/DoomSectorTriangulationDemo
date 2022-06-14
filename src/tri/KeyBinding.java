package tri;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

public class KeyBinding {
	private static class RunnableAsAction extends AbstractAction {
		private final Runnable action;
		
		private RunnableAsAction(Runnable action) {
			this.action = action;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			action.run();
		}
	}
	
	public final String	id;
	public Action		action;
	public KeyStroke	keystroke;

	public KeyBinding(KeyStroke keystroke, String id, Action action) {
		if(keystroke == null) {
			throw new IllegalArgumentException("Keystroke provided was invalid");
		}
		this.id = id;
		this.keystroke = keystroke;
		this.action = action;
	}

	public KeyBinding(String keystroke, String id, Action action) {
		this(KeyStroke.getKeyStroke(keystroke), id, action);
	}

	public KeyBinding(String keystroke, String id, Runnable action) {
		this(KeyStroke.getKeyStroke(keystroke), id, new RunnableAsAction(action));
	}
}
