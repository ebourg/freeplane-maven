/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2018 dimitry
 *
 *  This file author is dimitry
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
package org.freeplane.plugin.collaboration.client.event.children;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.SingleNodeStructureManipulator;
import org.freeplane.plugin.collaboration.client.event.TestObjects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Dimitry Polivaev
 * Jan 6, 2018
 */
@RunWith(MockitoJUnitRunner.class)
public class NodeMovedProcessorSpec {
	@Mock
	private SingleNodeStructureManipulator manipulator;
	@InjectMocks
	private NodeMovedProcessor uut;
	
	MapStructureEventFactory factory = new MapStructureEventFactory();
	
	final private TestObjects testObjects = new TestObjects();
	final private MapModel map = testObjects.map;
	final private NodeModel parent = testObjects.parent;
	final private NodeModel child = testObjects.child;
	
	@Test
	public void eventClass() throws Exception {
		assertThat(uut.eventClass()).isEqualTo(NodeMoved.class);
	}

	@Test
	public void movesRightToRightFirstLevelNode() throws Exception {
		parent.insert(child);
		when(map.getRootNode()).thenReturn(parent);
		
		uut.onUpdate(map, factory.createNodeMovedEvent(child));
		verify(manipulator).moveNode(child, parent, 0, false, false);
	}
	
	@Test
	public void movesLeftFirstLevelNode() throws Exception {
		child.setLeft(true);
		parent.insert(child);
		when(map.getRootNode()).thenReturn(parent);
		
		NodeMoved event = factory.createNodeMovedEvent(child);
		uut.onUpdate(map, event);
		verify(manipulator).moveNode(child, parent, 0, true, false);
	}

	@Test
	public void movesRightToLeftFirstLevelNode() throws Exception {
		parent.insert(child);
		when(map.getRootNode()).thenReturn(parent);
		
		child.setLeft(true);
		NodeMoved event = factory.createNodeMovedEvent(child);
		child.setLeft(false);
		uut.onUpdate(map, event);
		verify(manipulator).moveNode(child, parent, 0, true, true);
	}

	@Test
	public void movesRightNonFirstLevelNode() throws Exception {
		parent.insert(child);
		
		uut.onUpdate(map, factory.createNodeMovedEvent(child));
		verify(manipulator).moveNode(child, parent, 0, false, false);
	}
	
	@Test
	public void movesLeftNonFirstLevelNode() throws Exception {
		parent.setLeft(true);
		parent.insert(child);
		
		uut.onUpdate(map, factory.createNodeMovedEvent(child));
		verify(manipulator).moveNode(child, parent, 0, true, false);
	}
}
