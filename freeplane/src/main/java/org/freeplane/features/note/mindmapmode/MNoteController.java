/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.note.mindmapmode;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.undo.IActor;
import org.freeplane.features.map.IExtensionCopier;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.note.NoteController;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.styles.LogicalStyleKeys;
import org.freeplane.features.styles.MapStyle;
import org.freeplane.features.styles.SetBooleanMapPropertyAction;
import org.freeplane.features.text.TextController;

import com.jgoodies.common.base.Objects;

/**
 * @author Dimitry Polivaev
 */
public class MNoteController extends NoteController {
	private static class ExtensionCopier implements IExtensionCopier {

		@Override
		public void copy(Object key, NodeModel from, NodeModel to) {
			if (!key.equals(LogicalStyleKeys.NODE_STYLE)) {
				return;
			}
	        NoteModel fromNote = NoteModel.getNote(from);
	        if(fromNote == null)
	        	return;
			String contentType = fromNote.getContentType();
			if (contentType == null)
				return;

	        NoteModel oldNote = NoteModel.getNote(to);
	        NoteModel newNote = oldNote == null ? new NoteModel() :  oldNote.copy();
	        newNote.setContentType(contentType);
	        to.putExtension(newNote);
		}

		@Override
		public void remove(Object key, NodeModel from) {
			if (!key.equals(LogicalStyleKeys.NODE_STYLE)) {
				return;
			}
	        NoteModel fromNote = NoteModel.getNote(from);
	        if(fromNote == null)
	        	return;
			String contentType = fromNote.getContentType();
			if (contentType == null)
				return;

	        NoteModel newNote = fromNote.copy();
	        newNote.setContentType(null);
	        from.putExtension(newNote);

		}

		@Override
		public void remove(Object key, NodeModel from, NodeModel which) {
			if (!key.equals(LogicalStyleKeys.NODE_STYLE)) {
				return;
			}
	        NoteModel whichNote = NoteModel.getNote(which);
	        if(whichNote == null || whichNote.getContentType() == null)
				return;
	        remove(key, from);
		}
	}

	final class NoteDocumentListener implements DocumentListener {
		@Override
		public void changedUpdate(final DocumentEvent arg0) {
			docEvent();
		}

		private void docEvent() {
			final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if (focusOwner == null || !SwingUtilities.isDescendingFrom(focusOwner, notePanel)) {
				return;
			}
			final ModeController modeController = Controller.getCurrentModeController();
			final MapController mapController = modeController.getMapController();
			final MapModel map = modeController.getController().getMap();
			if(map != null) {
				mapController.mapSaved(map, false);
			}
		}

		@Override
		public void insertUpdate(final DocumentEvent arg0) {
			docEvent();
		}

		@Override
		public void removeUpdate(final DocumentEvent arg0) {
			docEvent();
		}
	}

	public static final String RESOURCES_REMOVE_NOTES_WITHOUT_QUESTION = "remove_notes_without_question";
	public static final String RESOURCES_USE_DEFAULT_FONT_FOR_NOTES_TOO = "resources_use_default_font_for_notes_too";
	public static final String RESOURCES_USE_MARGIN_TOP_ZERO_FOR_NOTES = "resources_use_margin_top_zero_for_notes";
	static final String RESOURCES_USE_SPLIT_PANE = "use_split_pane";
	private static NotePanel notePanel;

	public static MNoteController getController() {
	    return (MNoteController) NoteController.getController();
	}



	private final NoteManager noteManager;
    private final Set<String> noteContentTypes;
	private MModeController modeController;

	MModeController getModeController() {
        return modeController;
    }

    /**
	 * @param modeController
	 */
	public MNoteController(MModeController modeController) {
		super();
		this.modeController = modeController;
		modeController.registerExtensionCopier(new ExtensionCopier());
		noteManager = new NoteManager(this);
		modeController.getMapController().addMapLifeCycleListener(noteManager);
        noteContentTypes = new LinkedHashSet<>();
        noteContentTypes.add(TextController.CONTENT_TYPE_AUTO);
        noteContentTypes.add(TextController.CONTENT_TYPE_HTML);
		createActions(modeController);
	}

	private void createActions(ModeController modeController) {
	    modeController.addAction(new SelectNoteAction(this));
		modeController.addAction(new ShowHideNoteAction(this));
		modeController.addAction(new EditNoteInDialogAction());
		modeController.addAction(new SetNoteWindowPosition("top"));
		modeController.addAction(new SetNoteWindowPosition( "left"));
		modeController.addAction(new SetNoteWindowPosition("right"));
		modeController.addAction(new SetNoteWindowPosition("bottom"));
		modeController.addAction(new RemoveNoteAction(this));
		modeController.addAction(new SetBooleanMapPropertyAction(SHOW_NOTE_ICONS));
    }

    public boolean addNoteContentType(String e) {
        return noteContentTypes.add(e);
    }

    public String[] getNoteContentTypes() {
        return noteContentTypes.stream().toArray(String[]::new);
    }

	void hideNotesPanel() {
	    noteManager.saveNote();
		notePanel.setVisible(false);
		Controller.getCurrentModeController().getController().getViewController().removeSplitPane();
		ResourceController.getResourceController().setProperty(MNoteController.RESOURCES_USE_SPLIT_PANE, "false");
	}

	@Override
	protected void onWrite(final MapModel map) {
		final ModeController modeController = Controller.getCurrentModeController();
		final Controller controller = modeController.getController();
		final IMapSelection selection = controller.getSelection();
		if (selection == null || notePanel == null) {
			return;
		}
		final NodeModel selected = selection.getSelected();
		noteManager.saveNote(selected);
	}

	public void setNoteText(final NodeModel node, final String newText) {
        if("".equals(newText)) {
            setNoteText(node, null);
            return;
        }

        final String oldText = NoteModel.getNoteText(node);
        if (oldText == newText || null != oldText && oldText.equals(newText)) {
            return;
        }

        NoteModel oldNote = NoteModel.getNote(node);
        NoteModel newNote= oldNote == null ? new NoteModel() :  oldNote.copy();
        newNote.setText(newText);

        if(oldNote == null || ! Objects.equals(oldNote.getXml(), newNote.getXml()))
            setNote(node, oldNote, newNote, "setNoteText");
	}


    public void setNoteContentType(final NodeModel node, final String newContentType) {
        final String oldContentType = NoteModel.getNoteContentType(node);
        if (oldContentType == newContentType || null != oldContentType && oldContentType.equals(newContentType)) {
            return;
        }

        NoteModel oldNote = NoteModel.getNote(node);
        NoteModel newNote= oldNote == null ? new NoteModel() :  oldNote.copy();
        newNote.setContentType(newContentType);

        setNote(node, oldNote, newNote, "setNoteContentType");
    }

    private void setNote(final NodeModel node, NoteModel oldNote, NoteModel newNote, String description) {
        final IActor actor = new IActor() {
            @Override
            public void act() {
                setNote(newNote);
                Controller.getCurrentModeController().getMapController()
                    .nodeChanged(node, NodeModel.NOTE_TEXT, oldNote, newNote);
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public void undo() {
                setNote(oldNote);
                Controller.getCurrentModeController().getMapController()
                    .nodeChanged(node, NodeModel.NOTE_TEXT, newNote, oldNote);
            }
            private void setNote(final NoteModel note) {
                if(note == null || note.isEmpty()) {
                    node.removeExtension(NoteModel.class);
                }
                else
                    node.putExtension(note);
			}
		};
		Controller.getCurrentModeController().execute(actor, node.getMap());
    }



	private boolean shouldUseSplitPane() {
		return "true".equals(ResourceController.getResourceController().getProperty(
		    MNoteController.RESOURCES_USE_SPLIT_PANE));
	}

	void showNotesPanel() {
		if (notePanel == null) {
			notePanel = new NotePanel(noteManager, new NoteDocumentListener());
			noteManager.updateEditor();
		}
		ResourceController.getResourceController().setProperty(MNoteController.RESOURCES_USE_SPLIT_PANE, "true");
		Controller.getCurrentModeController().getController().getViewController().insertComponentIntoSplitPane(notePanel);
		notePanel.setVisible(true);
		notePanel.revalidate();
	}

	public void stopEditing() {
		if(isEditing()) {
			noteManager.saveNote();
			modeController.forceNewTransaction();
		}
	}

	boolean isEditing() {
		final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focusOwner != null && notePanel != null && SwingUtilities.isDescendingFrom(focusOwner, notePanel);
	}

	 void setFocusToMap() {
		final Controller controller = Controller.getCurrentModeController().getController();
		final NodeModel node = controller.getSelection().getSelected();
		controller.getMapViewManager().getComponent(node).requestFocus();
	}

	public void shutdownController() {
		Controller.getCurrentModeController().getMapController().removeNodeSelectionListener(noteManager);
		Controller.getCurrentController().getMapViewManager().removeMapSelectionListener(noteManager);
		if (notePanel == null) {
			return;
		}
		notePanel.getActionMap().remove("jumpToMapAction");
		if (shouldUseSplitPane()) {
			hideNotesPanel();
			notePanel = null;
		}
	}

	public void startupController() {
		final ModeController modeController = Controller.getCurrentModeController();
		if (shouldUseSplitPane()) {
			showNotesPanel();
		}
		modeController.getMapController().addNodeSelectionListener(noteManager);
		Controller.getCurrentController().getMapViewManager().addMapSelectionListener(noteManager);
	}

	boolean isNoteEditorShowing() {
		return ResourceController.getResourceController().getBooleanProperty(
		    MNoteController.RESOURCES_USE_SPLIT_PANE);
	}

	public void setShowNotesInMap(final MapModel model, final boolean show) {
		MapStyle.getController().setProperty(model, SHOW_NOTES_IN_MAP, Boolean.toString(show));
	}

	public void editNoteInDialog(final NodeModel nodeModel) {
		new NoteDialogStarter().editNoteInDialog(nodeModel);
	}

	NotePanel getNotePanel() {
		return notePanel;
	}

}
