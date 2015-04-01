package org.freeplane.core.ui.menubuilders.menu;

import java.awt.Component;
import java.awt.Container;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.IFreeplaneAction;
import org.freeplane.core.ui.LabelAndMnemonicSetter;
import org.freeplane.core.ui.MenuSplitter;
import org.freeplane.core.ui.MenuSplitterConfiguration;
import org.freeplane.core.ui.components.JAutoCheckBoxMenuItem;
import org.freeplane.core.ui.components.JFreeplaneMenuItem;
import org.freeplane.core.ui.menubuilders.action.AcceleratebleActionProvider;
import org.freeplane.core.ui.menubuilders.action.IAcceleratorMap;
import org.freeplane.core.ui.menubuilders.generic.Entry;
import org.freeplane.core.ui.menubuilders.generic.EntryAccessor;
import org.freeplane.core.ui.menubuilders.generic.EntryPopupListener;
import org.freeplane.core.ui.menubuilders.generic.EntryVisitor;
import org.freeplane.core.ui.menubuilders.generic.ResourceAccessor;

public class JMenuItemBuilder implements EntryVisitor{

	final private EntryPopupListener popupListener;
	final ResourceAccessor resourceAccessor;
	final private MenuSplitter menuSplitter;
	final private EntryAccessor entryAccessor;
	private IAcceleratorMap accelerators;
	private AcceleratebleActionProvider acceleratebleActionProvider;

	public JMenuItemBuilder(EntryPopupListener popupListener, IAcceleratorMap accelerators,
	                        AcceleratebleActionProvider acceleratebleActionProvider, ResourceAccessor resourceAccessor) {
		this.popupListener = popupListener;
		this.accelerators = accelerators;
		this.acceleratebleActionProvider = acceleratebleActionProvider;
		this.resourceAccessor = resourceAccessor;
		this.entryAccessor = new EntryAccessor(resourceAccessor);
		menuSplitter = new MenuSplitter(resourceAccessor.getIntProperty(
		    MenuSplitterConfiguration.MAX_MENU_ITEM_COUNT_KEY, 10));
	}

	@Override
	public void visit(Entry entry) {
		if (entry.hasChildren() && !entryAccessor.getText(entry).isEmpty())
			addSubmenu(entry);
		else
			addActionItem(entry);
		addPopupMenuListener(entry);
	}

	private void addPopupMenuListener(Entry entry) {
	    final JPopupMenu popup = getPopupMenu(entry);
		if (popup != null)
			addPopupMenuListener(entry, popup);
    }

	private JPopupMenu getPopupMenu(Entry entry) {
		if (entry == null)
			return null;
		final Object component = entryAccessor.getComponent(entry);
		if (component instanceof JMenu)
			return ((JMenu) component).getPopupMenu();
		if (component instanceof Component) {
			final JPopupMenu ancestorPopupMenu = (JPopupMenu) SwingUtilities.getAncestorOfClass(JPopupMenu.class,
			    (Component) component);
			if (ancestorPopupMenu != null)
				return (JPopupMenu) ancestorPopupMenu;
		}
		return getPopupMenu(entry.getParent());
	}

	private void addActionItem(Entry entry) {
		final Component actionComponent = createActionComponent(entry);
		if(actionComponent != null){
			addComponent(entry, actionComponent);
		}
	}

	private void addComponent(Entry entry, final Component component) {
		entryAccessor.setComponent(entry, component);
		final Container container = (Container) entryAccessor.getAncestorComponent(entry);
		menuSplitter.addComponent(container, component);
    }

	private void addSubmenu(final Entry entry) {
		final Component actionComponent = createActionComponent(entry);
		JMenu menu = new JMenu();
		final String rawText = entryAccessor.getText(entry);
		LabelAndMnemonicSetter.setLabelAndMnemonic(menu, rawText);
		final Icon icon = entryAccessor.getIcon(entry);
		if (icon != null) {
			menu.setIcon(icon);
		}
		addComponent(entry, menu);
		if(actionComponent != null){
			menuSplitter.addMenuComponent(menu, actionComponent);
		}

	}

	protected void addPopupMenuListener(final Entry entry, final JPopupMenu popupMenu) {
	    popupMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				popupListener.childEntriesWillBecomeVisible(entry);
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				popupListener.childEntriesWillBecomeInvisible(entry);
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});
    }

	private Component createActionComponent(Entry entry) {
		final AFreeplaneAction action = entryAccessor.getAction(entry);
		if(action != null){
			final JMenuItem actionComponent;
			IFreeplaneAction wrappedAction = acceleratebleActionProvider.wrap(action);
			if (action.isSelectable()) {
				actionComponent = new JAutoCheckBoxMenuItem(wrappedAction);
			}
			else {
				actionComponent = new JFreeplaneMenuItem(wrappedAction);
			}
			final KeyStroke accelerator = accelerators.getAccelerator(entry.getName());
			actionComponent.setAccelerator(accelerator);
			return actionComponent;
		}
		else if(entry.builders().contains("separator")){
			return new JPopupMenu.Separator();
		}
		else
			return null;
	}

	@Override
	public boolean shouldSkipChildren(Entry entry) {
		return false;
	}

}
