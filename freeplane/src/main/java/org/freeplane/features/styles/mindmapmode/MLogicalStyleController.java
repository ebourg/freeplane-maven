/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2009 Dimitry
 *
 *  This file author is Dimitry
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
package org.freeplane.features.styles.mindmapmode;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.IUserInputListenerFactory;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.menubuilders.generic.ChildActionEntryRemover;
import org.freeplane.core.ui.menubuilders.generic.Entry;
import org.freeplane.core.ui.menubuilders.generic.EntryAccessor;
import org.freeplane.core.ui.menubuilders.generic.EntryVisitor;
import org.freeplane.core.ui.menubuilders.generic.PhaseProcessor.Phase;
import org.freeplane.core.undo.IActor;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.filter.condition.ASelectableCondition;
import org.freeplane.features.filter.condition.ICondition;
import org.freeplane.features.icon.mindmapmode.MIconController.Keys;
import org.freeplane.features.map.*;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.note.NoteController;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.styles.*;
import org.freeplane.features.styles.ConditionalStyleModel.Item;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.view.swing.features.filepreview.MapBackgroundClearAction;
import org.freeplane.view.swing.features.filepreview.MapBackgroundImageAction;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * @author Dimitry Polivaev
 * 28.09.2009
 */
public class MLogicalStyleController extends LogicalStyleController {

	public enum NodeProperty {CONDITIONAL_STYLES}

	private static final String STYLE_ACTIONS = "styleActions";
	private static final String NEW_NODE_STYLE_ACTIONS = "newNodeStyleActions";

	private final class RemoveConditionalStyleActor implements IActor {
		private final MapModel changedMap;
		private final NodeModel changedNode;
		private final int index;
		private final ConditionalStyleModel conditionalStyleModel;
		Item item = null;

		private RemoveConditionalStyleActor(MapModel changedMap, NodeModel changedNode, ConditionalStyleModel conditionalStyleModel, int index) {
			this.changedMap = changedMap;
			this.changedNode = changedNode;
			this.index = index;
			this.conditionalStyleModel = conditionalStyleModel;
		}

		private RemoveConditionalStyleActor(NodeModel changedNode, ConditionalStyleModel conditionalStyleModel, int index) {
			this(null, changedNode, conditionalStyleModel, index);
		}

		private RemoveConditionalStyleActor(MapModel changedMap, ConditionalStyleModel conditionalStyleModel, int index) {
			this(changedMap, null, conditionalStyleModel, index);
		}

		private RemoveConditionalStyleActor(ConditionalStyleModel conditionalStyleModel, int index) {
			this(null, null, conditionalStyleModel, index);
		}

		@Override
		public void undo() {
			MLogicalStyleController.super.insertConditionalStyle(conditionalStyleModel, index, item.isActive(), item.getCondition(),
					item.getStyle(), item.isLast());
			if (changedNode != null)
				Controller.getCurrentModeController().getMapController().nodeChanged(changedNode, NodeModel.UNKNOWN_PROPERTY, null, null);
			if (changedMap != null)
				LogicalStyleController.getController().refreshMap(changedMap);
		}

		@Override
		public String getDescription() {
			return "RemoveConditionalStyle";
		}

		@Override
		public void act() {
			item = MLogicalStyleController.super.removeConditionalStyle(conditionalStyleModel, index);
			if (changedNode != null)
				Controller.getCurrentModeController().getMapController().nodeChanged(changedNode, NodeModel.UNKNOWN_PROPERTY, null, null);
			if (changedMap != null)
				LogicalStyleController.getController().refreshMap(changedMap);
		}
	}

	private final class AddConditionalStyleActor implements IActor {
		private final MapModel changedMap;
		private final NodeModel changedNode;
		private final ConditionalStyleModel conditionalStyleModel;
		private final int index;
		private final boolean isActive;
		private final ASelectableCondition condition;
		private final IStyle style;
		private final boolean isLast;

		public AddConditionalStyleActor(MapModel changedMap, NodeModel changedNode, final ConditionalStyleModel conditionalStyleModel, int index, boolean isActive, ASelectableCondition condition, IStyle style, boolean isLast) {
			this.changedMap = changedMap;
			this.changedNode = changedNode;
			this.conditionalStyleModel = conditionalStyleModel;
			this.index = index;
			this.isActive = isActive;
			this.condition = condition;
			this.style = style;
			this.isLast = isLast;
		}

		public AddConditionalStyleActor(final ConditionalStyleModel conditionalStyleModel, int index, boolean isActive, ASelectableCondition condition, IStyle style, boolean isLast) {
			this(null, null, conditionalStyleModel, index, isActive, condition, style, isLast);
		}

		public AddConditionalStyleActor(NodeModel changedNode, final ConditionalStyleModel conditionalStyleModel, int index, boolean isActive, ASelectableCondition condition, IStyle style, boolean isLast) {
			this(null, changedNode, conditionalStyleModel, index, isActive, condition, style, isLast);
		}

		public AddConditionalStyleActor(MapModel changedMap, final ConditionalStyleModel conditionalStyleModel, int index, boolean isActive, ASelectableCondition condition, IStyle style, boolean isLast) {
			this(changedMap, null, conditionalStyleModel, index, isActive, condition, style, isLast);
		}

		@Override
		public void undo() {
			int idx = this.index == -1 ? conditionalStyleModel.getStyleCount() - 1 : this.index;
			MLogicalStyleController.super.removeConditionalStyle(conditionalStyleModel, idx);
			if (changedNode != null)
				Controller.getCurrentModeController().getMapController().nodeChanged(changedNode, NodeModel.UNKNOWN_PROPERTY, null, null);
			if (changedMap != null)
				LogicalStyleController.getController().refreshMap(changedMap);
		}

		@Override
		public String getDescription() {
			return "AddConditionalStyle";
		}

		@Override
		public void act() {
			if (index == -1)
				MLogicalStyleController.super.addConditionalStyle(conditionalStyleModel, isActive, condition, style, isLast);
			else
				MLogicalStyleController.super.insertConditionalStyle(conditionalStyleModel, index, isActive, condition, style, isLast);
			if (changedNode != null)
				Controller.getCurrentModeController().getMapController().nodeChanged(changedNode, NodeModel.UNKNOWN_PROPERTY, null, null);
			if (changedMap != null)
				LogicalStyleController.getController().refreshMap(changedMap);
		}
	}

	private static class StyleRemover implements INodeChangeListener {
		public StyleRemover() {
		}

		@Override
		public void nodeChanged(final NodeChangeEvent event) {
			final ModeController modeController = Controller.getCurrentModeController();
			final NodeModel node = event.getNode();
			final MapModel map = node.getMap();
			if (modeController == null || map.isUndoActionRunning()) {
				return;
			}
			if (!event.getProperty().equals(LogicalStyleModel.class)) {
				return;
			}
			final IStyle styleKey = (IStyle) event.getNewValue();
			final MapStyleModel mapStyles = MapStyleModel.getExtension(map);
			final NodeModel styleNode = mapStyles.getStyleNode(styleKey);
			if (styleNode == null) {
				return;
			}
			modeController.undoableRemoveExtensions(LogicalStyleKeys.NODE_STYLE, node, styleNode);
		}
	}

	;

	private static class ExtensionCopier implements IExtensionCopier {
		@Override
		public void copy(final Object key, final NodeModel from, final NodeModel to) {
			if (!key.equals(LogicalStyleKeys.LOGICAL_STYLE)) {
				return;
			}
			copy(from, to);
		}

		public void copy(final NodeModel from, final NodeModel to) {
			final LogicalStyleModel fromStyle = from.getExtension(LogicalStyleModel.class);
			if (fromStyle == null) {
				return;
			}
			final LogicalStyleModel toStyle = LogicalStyleModel.createExtension(to);
			toStyle.setStyle(fromStyle.getStyle());
		}

		@Override
		public void remove(final Object key, final NodeModel from) {
			if (!key.equals(LogicalStyleKeys.LOGICAL_STYLE)) {
				return;
			}
			from.removeExtension(LogicalStyleModel.class);
		}

		@Override
		public void remove(final Object key, final NodeModel from, final NodeModel which) {
			if (!key.equals(LogicalStyleKeys.LOGICAL_STYLE)) {
				return;
			}
			final LogicalStyleModel whichStyle = which.getExtension(LogicalStyleModel.class);
			if (whichStyle == null) {
				return;
			}
			final LogicalStyleModel fromStyle = from.getExtension(LogicalStyleModel.class);
			if (fromStyle == null) {
				return;
			}
			from.removeExtension(fromStyle);
		}
	}

	final private List<AFreeplaneAction> actions;
	private final ModeController modeController;

	public MLogicalStyleController(ModeController modeController) {
		super(modeController);
		this.modeController = modeController;
		modeController.getMapController().addUINodeChangeListener(new INodeChangeListener() {
			@Override
			public void nodeChanged(NodeChangeEvent event) {
				if (event.getProperty() == NodeProperty.CONDITIONAL_STYLES)
					return;
				NodeModel node = event.getNode();
				ConditionalStyleModel mapStyles = MapStyleModel.getExtension(node.getMap()).getConditionalStyleModel();
				if (mapStyles.dependsOnCondition(ICondition::checksDescendants)) {
					delayedRefreshParent(node, true);
				} else if (mapStyles.dependsOnCondition(ICondition::checksChildren)) {
					delayedRefreshParent(node, false);

				}
				if (mapStyles.dependsOnCondition(ICondition::checksAncestors)) {
					delayedRefreshChildren(node, true);
				} else if (mapStyles.dependsOnCondition(ICondition::checksParent)) {
					delayedRefreshChildren(node, false);
				} else {
					for (NodeModel child : node.getChildren()) {
						ConditionalStyleModel nodeStyles = node.getExtension(ConditionalStyleModel.class);
						if (nodeStyles != null && nodeStyles.dependsOnCondition(ICondition::checksParent))
							modeController.getMapController().delayedNodeRefresh(child, NodeProperty.CONDITIONAL_STYLES,
									null, null);
					}
				}
			}

			private void delayedRefreshChildren(NodeModel node, boolean withDescendants) {
				for (NodeModel child : node.getChildren()) {
					if (child.hasViewers()) {
						MLogicalStyleController.this.modeController.getMapController().delayedNodeRefresh(child, NodeProperty.CONDITIONAL_STYLES,
								null, null);
						if (withDescendants)
							delayedRefreshChildren(child, true);
					}
				}
			}

			private void delayedRefreshParent(NodeModel node, boolean withAncestors) {
				NodeModel parent = node.getParentNode();
				if (parent != null) {
					if (withAncestors)
						delayedRefreshParent(parent, true);
					if (parent.hasViewers())
						MLogicalStyleController.this.modeController.getMapController().delayedNodeRefresh(parent, NodeProperty.CONDITIONAL_STYLES,
								null, null);
				}
			}
		});

//		this.modeController = modeController;
		actions = new LinkedList<AFreeplaneAction>();
	}

	public void initS() {
		final ModeController modeController = Controller.getCurrentModeController();
		modeController.addAction(new ManageNodeConditionalStylesAction());
	}

	public void initM() {
		final ModeController modeController = Controller.getCurrentModeController();
		modeController.getMapController().addUINodeChangeListener(new StyleRemover());
		modeController.registerExtensionCopier(new ExtensionCopier());
		modeController.addAction(new RedefineStyleAction());
		modeController.addAction(new RedefineStyleUpdateTemplateAction());
		modeController.addAction(new NewUserStyleFromSelectionAction());
		modeController.addAction(new ManageMapConditionalStylesAction());
		modeController.addAction(new ManageNodeConditionalStylesAction());
		modeController.addAction(new CopyStyleExtensionsAction());
		if (modeController.getModeName().equals("MindMap")) {
			modeController.addAction(new MapBackgroundColorAction());
			modeController.addAction(new MapBackgroundImageAction());
			modeController.addAction(new MapBackgroundClearAction());
			modeController.addAction(new SetBooleanMapPropertyAction(MapStyle.FIT_TO_VIEWPORT));
			modeController.addAction(new CopyMapStylesAction());
			modeController.addAction(new ReplaceMapStylesAction());
			modeController.addAction(new ManageAssociatedMindMapsAction());
		}
		if (!GraphicsEnvironment.isHeadless()) {
			StyleMenuBuilder styleBuilder = new StyleMenuBuilder(AssignStyleAction::new);
			styleBuilder.addStyleAction(new ResetStyleAction());
			modeController.addUiBuilder(Phase.ACTIONS, "style_actions", styleBuilder,
					new ChildActionEntryRemover(modeController));
			StyleMenuBuilder newNodeStyleBuilder = new StyleMenuBuilder(SetNewNodeStyleAction::new);
			newNodeStyleBuilder.addStyleAction(new UseCurrentStyleForNewNodesAction());
			newNodeStyleBuilder.addStyleAction(new ResetNewNodeStyleAction());
			modeController.addUiBuilder(Phase.ACTIONS, "new_node_style_actions", newNodeStyleBuilder,
					new ChildActionEntryRemover(modeController));
			final IUserInputListenerFactory userInputListenerFactory = modeController.getUserInputListenerFactory();
			Controller.getCurrentController().getMapViewManager().addMapSelectionListener(new IMapSelectionListener() {
				@Override
				public void afterMapChange(final MapModel oldMap, final MapModel newMap) {
					userInputListenerFactory.rebuildMenus(STYLE_ACTIONS);
					userInputListenerFactory.rebuildMenus(NEW_NODE_STYLE_ACTIONS);
				}
			});
			final MapController mapController = modeController.getMapController();
			mapController.addUIMapChangeListener(new IMapChangeListener() {
				@Override
				public void onPreNodeMoved(NodeMoveEvent nodeMoveEvent) {
				}

				@Override
				public void onPreNodeDelete(NodeDeletionEvent nodeDeletionEvent) {
				}

				@Override
				public void onNodeMoved(NodeMoveEvent nodeMoveEvent) {
				}

				@Override
				public void onNodeInserted(final NodeModel parent, final NodeModel child, final int newIndex) {
				}

				@Override
				public void onNodeDeleted(NodeDeletionEvent nodeDeletionEvent) {
				}

				@Override
				public void mapChanged(final MapChangeEvent event) {
					if (event.getProperty().equals(MapStyle.MAP_STYLES)) {
						userInputListenerFactory.rebuildMenus(STYLE_ACTIONS);
						userInputListenerFactory.rebuildMenus(NEW_NODE_STYLE_ACTIONS);
					}
				}
			});
			mapController.addNodeSelectionListener(new INodeSelectionListener() {
				@Override
				public void onSelect(final NodeModel node) {
					selectActions();
				}

				@Override
				public void onDeselect(final NodeModel node) {
				}
			});
		}
	}

	class StyleMenuBuilder implements EntryVisitor {
		private final Function<IStyle, AFreeplaneAction> styleActionFactory;
		private final List<AFreeplaneAction> additionalActions;

		StyleMenuBuilder(Function<IStyle, AFreeplaneAction> actionFactory) {
			super();
			this.styleActionFactory = actionFactory;
			this.additionalActions = new ArrayList<>();
		}


		boolean addStyleAction(AFreeplaneAction e) {
			return additionalActions.add(e);
		}


		@Override
		public void visit(Entry target) {
			addStyleMenu(target);
		}

		@Override
		public boolean shouldSkipChildren(Entry entry) {
			return true;
		}

		private void addStyleMenu(final Entry target) {
			MapModel map = Controller.getCurrentController().getMap();
			if (map == null) {
				return;
			}
			final MapStyleModel mapStyleModel = MapStyleModel.getExtension(map);
			if (mapStyleModel == null) {
				return;
			}
			actions.clear();
			final EntryAccessor entryAccessor = new EntryAccessor();
			for (AFreeplaneAction action : additionalActions) {
				final AFreeplaneAction addedAction = modeController.addActionIfNotAlreadySet(action);
				if (action == addedAction)
					actions.add(action);
				addedAction.setEnabled(true);
				entryAccessor.addChildAction(target, addedAction);
			}
			for (final IStyle style : mapStyleModel.getNodeStyles()) {
				AFreeplaneAction newAction = styleActionFactory.apply(style);
				final AFreeplaneAction action = modeController.addActionIfNotAlreadySet(newAction);
				if (newAction == action)
					actions.add(newAction);
				action.setEnabled(true);
				entryAccessor.addChildAction(target, action);
			}
		}
	}

	public void setStyle(final NodeModel node, final IStyle style) {
		final ModeController modeController = Controller.getCurrentModeController();
		final IStyle oldStyle = LogicalStyleModel.getStyle(node);
		if (oldStyle != null && oldStyle.equals(style) || oldStyle == style) {
			modeController.getMapController().nodeChanged(node, LogicalStyleModel.class, oldStyle, style);
			return;
		}
		final IActor actor = new IActor() {
			@Override
			public String getDescription() {
				return "setStyle";
			}

			@Override
			public void act() {
				changeStyle(modeController, node, oldStyle, style);
			}

			@Override
			public void undo() {
				changeStyle(modeController, node, style, oldStyle);
			}

			private void changeStyle(final ModeController modeController, final NodeModel node, final IStyle oldStyle,
			                         final IStyle style) {
				if (style != null) {
					final LogicalStyleModel model = LogicalStyleModel.createExtension(node);
					model.setStyle(style);
				} else {
					node.removeExtension(LogicalStyleModel.class);
				}
				modeController.getMapController().nodeChanged(node, LogicalStyleModel.class, oldStyle, style);
				selectActions();
			}
		};
		modeController.execute(actor, node.getMap());
	}

	public void copyStyleExtensions(final IStyle style, final NodeModel target) {
		final MTextController textController = MTextController.getController();
		final MapStyleModel extension = MapStyleModel.getExtension(target.getMap());
		final NodeModel styleNode = extension.getStyleNode(style);
		if (styleNode != null) {
			final MAttributeController attributeController = MAttributeController.getController();
			attributeController.copyAttributesToNode(styleNode, target);
			final String detailTextText = DetailModel.getDetailText(styleNode);
			if (detailTextText != null)
				textController.setDetails(target, detailTextText);
			final String noteText = NoteModel.getNoteText(styleNode);
			if (noteText != null) {
				MNoteController noteController = (MNoteController) NoteController.getController();
				noteController.setNoteText(target, noteText);
			}
		}
	}

	void selectActions() {
		for (final AFreeplaneAction action : actions) {
			action.setSelected();
		}
	}

	public void setStyle(final IStyle style) {
		final ModeController modeController = Controller.getCurrentModeController();
		if (MapStyleModel.NEW_STYLE.equals(style)) {
			IMapSelection selection = Controller.getCurrentController().getSelection();
			if (selection == null) {
				return;
			}
			IStyle newStyle = addNewUserStyle(true);
			if (newStyle != null) {
				Set<NodeModel> nodes = Controller.getCurrentController().getSelection().getSelection();
				for (NodeModel node : nodes) {
					modeController.undoableRemoveExtensions(LogicalStyleKeys.NODE_STYLE, node, node);
				}
				setStyle(newStyle);
			} else {
				NodeModel node = selection.getSelected();
				final IStyle oldStyle = LogicalStyleModel.getStyle(node);
				modeController.getMapController().nodeChanged(node, LogicalStyleModel.class, oldStyle, oldStyle);
			}
			return;
		}
		final Collection<NodeModel> selectedNodes = modeController.getMapController().getSelectedNodes();
		for (final NodeModel selected : selectedNodes) {
			setStyle(selected, style);
		}
	}


	public void moveConditionalStyleDown(final MapModel map, final ConditionalStyleModel conditionalStyleModel, final int index) {
		int maxIndex = conditionalStyleModel.getStyleCount() - 1;
		if (index < 0 || index >= maxIndex) {
			return;
		}
		IActor actor = new IActor() {
			@Override
			public String getDescription() {
				return "moveConditionalStyleDown";
			}

			@Override
			public void act() {
				MLogicalStyleController.super.moveConditionalStyleDown(conditionalStyleModel, index);
			}

			@Override
			public void undo() {
				MLogicalStyleController.super.moveConditionalStyleUp(conditionalStyleModel, index + 1);
			}
		};
		Controller.getCurrentModeController().execute(actor, map);
	}


	public void moveConditionalStyleUp(final MapModel map, final ConditionalStyleModel conditionalStyleModel, final int index) {
		int maxIndex = conditionalStyleModel.getStyleCount() - 1;
		if (index <= 0 || index > maxIndex) {
			return;
		}
		IActor actor = new IActor() {
			@Override
			public String getDescription() {
				return "moveConditionalStyleUp";
			}

			@Override
			public void act() {
				MLogicalStyleController.super.moveConditionalStyleUp(conditionalStyleModel, index);
			}

			@Override
			public void undo() {
				MLogicalStyleController.super.moveConditionalStyleDown(conditionalStyleModel, index - 1);
			}
		};
		Controller.getCurrentModeController().execute(actor, map);
	}

	private final class MoveConditionalStyleActor implements IActor {
		final MapModel changedMap;
		final NodeModel changedNode;
		final ConditionalStyleModel conditionalStyleModel;
		final int index;
		final int toIndex;

		MoveConditionalStyleActor(MapModel changedMap, NodeModel changedNode, ConditionalStyleModel conditionalStyleModel, int index, int toIndex) {
			this.changedMap = changedMap;
			this.changedNode = changedNode;
			this.conditionalStyleModel = conditionalStyleModel;
			this.index = index;
			this.toIndex = toIndex;
		}

		MoveConditionalStyleActor(NodeModel changedNode, ConditionalStyleModel conditionalStyleModel, int index, int toIndex) {
			this(null, changedNode, conditionalStyleModel, index, toIndex);
		}

		MoveConditionalStyleActor(MapModel changedMap, ConditionalStyleModel conditionalStyleModel, int index, int toIndex) {
			this(changedMap, null, conditionalStyleModel, index, toIndex);
		}

		@Override
		public String getDescription() {
			return "MoveConditionalStyle";
		}

		@Override
		public void act() {
			MLogicalStyleController.super.moveConditionalStyle(conditionalStyleModel, index, toIndex);
			if (changedNode != null)
				Controller.getCurrentModeController().getMapController().nodeChanged(changedNode, NodeModel.UNKNOWN_PROPERTY, null, null);
			if (changedMap != null)
				LogicalStyleController.getController().refreshMap(changedMap);
		}

		@Override
		public void undo() {
			MLogicalStyleController.super.moveConditionalStyle(conditionalStyleModel, toIndex, index);
			if (changedNode != null)
				Controller.getCurrentModeController().getMapController().nodeChanged(changedNode, NodeModel.UNKNOWN_PROPERTY, null, null);
			if (changedMap != null)
				LogicalStyleController.getController().refreshMap(changedMap);
		}
	}

	public void moveConditionalStyleAndCallNodeChanged(NodeModel changedNode, final ConditionalStyleModel conditionalStyleModel, final int index, final int toIndex) {
		MoveConditionalStyleActor actor = new MoveConditionalStyleActor(changedNode, conditionalStyleModel, index, toIndex);
		Controller.getCurrentModeController().execute(actor, changedNode.getMap());
	}

	public void moveConditionalStyleAndRefreshMap(MapModel changedMap, final ConditionalStyleModel conditionalStyleModel, final int index, final int toIndex) {
		MoveConditionalStyleActor actor = new MoveConditionalStyleActor(changedMap, conditionalStyleModel, index, toIndex);
		Controller.getCurrentModeController().execute(actor, changedMap);
	}

	public static MLogicalStyleController getController() {
		return (MLogicalStyleController) LogicalStyleController.getController();
	}


	public void addConditionalStyle(final MapModel map, final ConditionalStyleModel conditionalStyleModel, boolean isActive, ASelectableCondition condition, IStyle style, boolean isLast) {
		AddConditionalStyleActor actor = new AddConditionalStyleActor(conditionalStyleModel, -1, isActive, condition, style, isLast);
		Controller.getCurrentModeController().execute(actor, map);
	}

	public void addConditionalStyleAndCallNodeChanged(NodeModel changedNode, final ConditionalStyleModel conditionalStyleModel, boolean isActive, ASelectableCondition condition, IStyle style, boolean isLast) {
		AddConditionalStyleActor actor = new AddConditionalStyleActor(changedNode, conditionalStyleModel, -1, isActive, condition, style, isLast);
		Controller.getCurrentModeController().execute(actor, changedNode.getMap());
	}

	public void addConditionalStyleAndRefreshMap(MapModel changedMap, final ConditionalStyleModel conditionalStyleModel, boolean isActive, ASelectableCondition condition, IStyle style, boolean isLast) {
		AddConditionalStyleActor actor = new AddConditionalStyleActor(changedMap, conditionalStyleModel, -1, isActive, condition, style, isLast);
		Controller.getCurrentModeController().execute(actor, changedMap);
	}

	public void insertConditionalStyleAndCallNodeChanged(NodeModel changedNode, final ConditionalStyleModel conditionalStyleModel, int index, boolean isActive, ASelectableCondition condition, IStyle style, boolean isLast) {
		AddConditionalStyleActor actor = new AddConditionalStyleActor(changedNode, conditionalStyleModel, index, isActive, condition, style, isLast);
		Controller.getCurrentModeController().execute(actor, changedNode.getMap());
	}

	public void insertConditionalStyleAndRefreshMap(MapModel changedMap, final ConditionalStyleModel conditionalStyleModel, int index, boolean isActive, ASelectableCondition condition, IStyle style, boolean isLast) {
		AddConditionalStyleActor actor = new AddConditionalStyleActor(changedMap, conditionalStyleModel, index, isActive, condition, style, isLast);
		Controller.getCurrentModeController().execute(actor, changedMap);
	}

	public Item removeConditionalStyle(final MapModel map, final ConditionalStyleModel conditionalStyleModel, final int index) {
		RemoveConditionalStyleActor actor = new RemoveConditionalStyleActor(conditionalStyleModel, index);
		Controller.getCurrentModeController().execute(actor, map);
		return actor.item;
	}

	public Item removeConditionalStyleAndCallNodeChanged(NodeModel changedNode, final ConditionalStyleModel conditionalStyleModel, final int index) {
		RemoveConditionalStyleActor actor = new RemoveConditionalStyleActor(changedNode, conditionalStyleModel, index);
		Controller.getCurrentModeController().execute(actor, changedNode.getMap());
		return actor.item;
	}

	public Item removeConditionalStyleAndRefreshMap(MapModel changedMap, final ConditionalStyleModel conditionalStyleModel, final int index) {
		RemoveConditionalStyleActor actor = new RemoveConditionalStyleActor(changedMap, conditionalStyleModel, index);
		Controller.getCurrentModeController().execute(actor, changedMap);
		return actor.item;
	}

	private final class ModifyConditionalStyleActor implements IActor {
		private final MapModel changedMap;
		private final NodeModel changedNode;
		private final ConditionalStyleModel.Item item;
		private final ConditionalStyleModel.Item oldItem;
		private final IStyle style;
		private final ASelectableCondition condition;
		private final boolean isActive;
		private final boolean isLast;
		private boolean isModified;

		ModifyConditionalStyleActor(MapModel changedMap, NodeModel changedNode, ConditionalStyleModel.Item item, IStyle style, ASelectableCondition condition, boolean isActive, boolean isLast) {
			this.changedMap = changedMap;
			this.changedNode = changedNode;
			this.item = item;
			this.oldItem = new ConditionalStyleModel.Item(item);
			this.style = style;
			this.condition = condition;
			this.isActive = isActive;
			this.isLast = isLast;
		}

		ModifyConditionalStyleActor(NodeModel changedNode, ConditionalStyleModel.Item item, IStyle style, ASelectableCondition condition, boolean isActive, boolean isLast) {
			this(null, changedNode, item, style, condition, isActive, isLast);
		}

		ModifyConditionalStyleActor(MapModel changedMap, ConditionalStyleModel.Item item, IStyle style, ASelectableCondition condition, boolean isActive, boolean isLast) {
			this(changedMap, null, item, style, condition, isActive, isLast);
		}

		@Override
		public String getDescription() {
			return "ModifyConditionalStyle";
		}

		@Override
		public void act() {
			isModified = MLogicalStyleController.super.modifyConditionalStyleItem(item, style, condition, isActive, isLast);
			if (isModified) {
				if (changedNode != null)
					Controller.getCurrentModeController().getMapController().nodeChanged(changedNode, NodeModel.UNKNOWN_PROPERTY, null, null);
				if (changedMap != null)
					LogicalStyleController.getController().refreshMap(changedMap);
			}
		}

		@Override
		public void undo() {
			if (!isModified)
				return;
			MLogicalStyleController.super.modifyConditionalStyleItem(item, oldItem.getStyle(), oldItem.getCondition(), oldItem.isActive(), oldItem.isLast());
			if (changedNode != null)
				Controller.getCurrentModeController().getMapController().nodeChanged(changedNode, NodeModel.UNKNOWN_PROPERTY, null, null);
			if (changedMap != null)
				LogicalStyleController.getController().refreshMap(changedMap);
		}
	}

	public void modifyConditionalStyleAndCallNodeChanged(NodeModel changedNode, final ConditionalStyleModel.Item item, IStyle style, ASelectableCondition condition, boolean isActive, boolean isLast) {
		ModifyConditionalStyleActor actor = new ModifyConditionalStyleActor(changedNode, item, style, condition, isActive, isLast);
		Controller.getCurrentModeController().execute(actor, changedNode.getMap());
	}

	public void modifyConditionalStyleItemAndRefreshMap(MapModel changedMap, final ConditionalStyleModel.Item item, IStyle style, ASelectableCondition condition, boolean isActive, boolean isLast) {
		ModifyConditionalStyleActor actor = new ModifyConditionalStyleActor(changedMap, item, style, condition, isActive, isLast);
		Controller.getCurrentModeController().execute(actor, changedMap);
	}
	public TableModel getConditionalStyleModelAsTableModel(final MapModel map, final ConditionalStyleModel conditionalStyleModel) {
		return new TableModel() {
			private final TableModel tableModel = conditionalStyleModel.asTableModel();

			@Override
			public void addTableModelListener(TableModelListener l) {
				tableModel.addTableModelListener(l);
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return tableModel.getColumnClass(columnIndex);
			}

			@Override
			public int getColumnCount() {
				return tableModel.getColumnCount();
			}

			@Override
			public String getColumnName(int columnIndex) {
				return tableModel.getColumnName(columnIndex);
			}

			@Override
			public int getRowCount() {
				return tableModel.getRowCount();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				return tableModel.getValueAt(rowIndex, columnIndex);
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return tableModel.isCellEditable(rowIndex, columnIndex);
			}

			@Override
			public void removeTableModelListener(TableModelListener l) {
				tableModel.removeTableModelListener(l);
			}

			@Override
			public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
				final Object oldValue = tableModel.getValueAt(rowIndex, columnIndex);
				if (aValue == oldValue || aValue != null && aValue.equals(oldValue)) {
					return;
				}
				IActor actor = new IActor() {

					@Override
					public String getDescription() {
						return "set conditional style table cell value";
					}

					@Override
					public void act() {
						tableModel.setValueAt(aValue, rowIndex, columnIndex);
					}

					@Override
					public void undo() {
						tableModel.setValueAt(oldValue, rowIndex, columnIndex);
					}
				};
				Controller.getCurrentModeController().execute(actor, map);
			}
		};
	}

	IStyle addNewUserStyle(final boolean copyStyleFromSelected) {
		final String styleName = JOptionPane.showInputDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
				TextUtils.getText("enter_new_style_name"), "");
		if (styleName == null || styleName.isEmpty()) {
			return null;
		}

		final MapModel map = Controller.getCurrentController().getMap();
		final MapStyleModel styleModel = MapStyleModel.getExtension(map);
		final MapModel styleMap = styleModel.getStyleMap();
		final IStyle newStyle = StyleFactory.create(styleName);
		if (null != styleModel.getStyleNode(newStyle)) {
			UITools.errorMessage(TextUtils.getText("style_already_exists"));
			return null;
		}
		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
		final NodeModel newNode = new NodeModel(styleMap);
		newNode.setUserObject(newStyle);
		if (copyStyleFromSelected) {
			NodeModel styleSourceNode = Controller.getCurrentController().getSelection().getSelected();
			final ArrayList<IStyle> styles = new ArrayList<IStyle>(getStyles(styleSourceNode, StyleOption.FOR_UNSELECTED_NODE));
			for (int i = styles.size() - 1; i >= 0; i--) {
				IStyle style = styles.get(i);
				if (MapStyleModel.DEFAULT_STYLE.equals(style)) {
					continue;
				}
				final NodeModel styleNode = styleModel.getStyleNode(style);
				if (styleNode == null) {
					continue;
				}
				Controller.getCurrentModeController().copyExtensions(LogicalStyleKeys.NODE_STYLE, styleNode, newNode);
			}
			Controller.getCurrentModeController().copyExtensions(LogicalStyleKeys.NODE_STYLE, styleSourceNode, newNode);
			Controller.getCurrentModeController().copyExtensions(Keys.ICONS, styleSourceNode, newNode);
		}
		NodeModel userStyleParentNode = styleModel.getStyleNodeGroup(styleMap, MapStyleModel.STYLES_USER_DEFINED);
		if (userStyleParentNode == null) {
			userStyleParentNode = new NodeModel(styleMap);
			userStyleParentNode.setUserObject(new StyleTranslatedObject(MapStyleModel.STYLES_USER_DEFINED));
			NodeModel rootNode = styleMap.getRootNode();
			mapController.insertNode(userStyleParentNode, rootNode, rootNode.getChildCount());

		}
		mapController.insertNode(newNode, userStyleParentNode, userStyleParentNode.getChildCount());
		mapController.select(newNode);
		final IActor actor = new IActor() {
			public void undo() {
				styleModel.removeStyleNode(newNode);
				refreshMap(map);
			}

			public String getDescription() {
				return "NewStyle";
			}

			public void act() {
				styleModel.addStyleNode(newNode);
				refreshMap(map);
			}
		};
		Controller.getCurrentModeController().execute(actor, styleMap);
		return newStyle;
	}


}
