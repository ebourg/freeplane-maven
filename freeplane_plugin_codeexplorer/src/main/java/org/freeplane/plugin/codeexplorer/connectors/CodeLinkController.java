/*
 * Created on 9 Nov 2023
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.connectors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.freeplane.core.extension.Configurable;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.Hyperlink;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.filter.Filter.FilteredElement;
import org.freeplane.features.link.ConnectorArrows;
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.link.ConnectorShape;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.NodeLinkModel;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeRelativePath;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodestyle.NodeStyleModel;
import org.freeplane.features.styles.MapStyleModel;
import org.freeplane.plugin.codeexplorer.CodeModeController;
import org.freeplane.plugin.codeexplorer.dependencies.CodeDependency;
import org.freeplane.plugin.codeexplorer.dependencies.DependencyVerdict;
import org.freeplane.plugin.codeexplorer.map.CodeNode;
import org.freeplane.plugin.codeexplorer.map.DependencySelection;
import org.freeplane.view.swing.map.MapView;

import com.tngtech.archunit.core.domain.JavaClass;

public class CodeLinkController extends LinkController {

    private static final EnumMap<DependencyVerdict, Color> connectorColors;
    static {
        connectorColors = new EnumMap<DependencyVerdict, Color>(DependencyVerdict.class);
        connectorColors.put(DependencyVerdict.ALLOWED, Color.GREEN);
        connectorColors.put(DependencyVerdict.FORBIDDEN, Color.RED);
        connectorColors.put(DependencyVerdict.IGNORED, Color.GRAY);
    }

    private static final EnumMap<DependencyVerdict, Point> backwardsConnectorStartInclinations;
    private static final EnumMap<DependencyVerdict, Point> upwardsConnectorStartInclinations;
    private static final EnumMap<DependencyVerdict, Point> backwardsConnectorEndInclinations;
    private static final EnumMap<DependencyVerdict, Point> upwardsConnectorEndInclinations;
    static {
        backwardsConnectorStartInclinations = new EnumMap<DependencyVerdict, Point>(DependencyVerdict.class);
        backwardsConnectorStartInclinations.put(DependencyVerdict.ALLOWED, new Point(150, 5));
        backwardsConnectorStartInclinations.put(DependencyVerdict.FORBIDDEN, new Point(200, 5));
        backwardsConnectorStartInclinations.put(DependencyVerdict.IGNORED, new Point(250, 5));
        upwardsConnectorStartInclinations = new EnumMap<DependencyVerdict, Point>(DependencyVerdict.class);
        backwardsConnectorEndInclinations = new EnumMap<DependencyVerdict, Point>(DependencyVerdict.class);
        upwardsConnectorEndInclinations = new EnumMap<DependencyVerdict, Point>(DependencyVerdict.class);
        for(Entry<DependencyVerdict, Point> entry : backwardsConnectorStartInclinations.entrySet()) {
           DependencyVerdict verdict = entry.getKey();
            Point backwardsConnectorStartInclination = entry.getValue();
            Point upwardsConnectorStartInclination = new Point(-backwardsConnectorStartInclination.x, -backwardsConnectorStartInclination.y);
            Point backwardsConnectorEndInclination = new Point(backwardsConnectorStartInclination.x, -backwardsConnectorStartInclination.y);
            Point upwardsConnectorEndInclination = new Point(upwardsConnectorStartInclination.x, -upwardsConnectorStartInclination.y);
            upwardsConnectorStartInclinations.put(verdict, upwardsConnectorStartInclination);
            backwardsConnectorEndInclinations.put(verdict, backwardsConnectorEndInclination);
            upwardsConnectorEndInclinations.put(verdict, upwardsConnectorEndInclination);
        }
    }

    public CodeLinkController(ModeController modeController) {
        super(modeController);
    }

    @Override
    public Color getColor(ConnectorModel connector) {
        if (isConnectorSelected(connector)) {
            DependencyVerdict dependencyVerdict = ((CodeConnectorModel)connector).dependencyVerdict();
            Color color = connectorColors.get(dependencyVerdict);
            return UITools.isLightLookAndFeelInstalled() ?  color.darker() : color.brighter();
        } else
            return getMapDefaultNodeTextColor(connector);
    }

    private Color getMapDefaultNodeTextColor(ConnectorModel connector) {
        return MapStyleModel.getExtension(connector.getSource().getMap()).getDefaultStyleNode().getExtension(NodeStyleModel.class).getColor();
    }

    @Override
    public int[] getDashArray(ConnectorModel connector) {
        return getStandardDashArray();

    }

    @Override
    public int getWidth(ConnectorModel connector) {
        CodeConnectorModel codeConnector = (CodeConnectorModel)connector;
        return  codeConnector.dependencyVerdict() != DependencyVerdict.IGNORED
                    && isConnectorSelected(codeConnector) ?
                1 + (int) (3 * Math.log10(codeConnector.weight()))
                : 1;

    }

    @Override
    public int getOpacity(ConnectorModel connector) {
        CodeConnectorModel codeConnector = (CodeConnectorModel)connector;
        return codeConnector.dependencyVerdict() != DependencyVerdict.IGNORED
                && isConnectorSelected(codeConnector) ? 128 : 30;
    }

    @Override
    public String getMiddleLabel(ConnectorModel connector) {
        return isConnectorSelected(connector) ?
                Integer.toString(((CodeConnectorModel)connector).weight()) :
                    "";
    }

    private boolean isConnectorSelected(ConnectorModel connector) {
        Controller controller = Controller.getCurrentController();
        if (controller.getModeController().getModeName() != CodeModeController.MODENAME)
            return false;
        IMapSelection selection = controller.getSelection();
        if(selection == null)
            return false;
        return isConnectorSelected((CodeConnectorModel)connector, selection);
    }

    private boolean isConnectorSelected(CodeConnectorModel connector, IMapSelection selection) {
        CodeNode source = (CodeNode) connector.getSource();
        CodeNode target = (CodeNode) connector.getTarget();
        boolean connectorSelected = new DependencySelection(selection).isConnectorSelected(source, target);
        return connectorSelected;
    }

    @Override
    public String getSourceLabel(ConnectorModel connector) {
       return "";
    }

    @Override
    public String getTargetLabel(ConnectorModel connector) {
        return "";
    }

    @Override
    public String getLabelFontFamily(ConnectorModel connector) {
        return getStandardLabelFontFamily();

    }

    @Override
    public int getLabelFontSize(ConnectorModel connector) {
        return 8;
    }

    @Override
    public int getLabelFontStyle(ConnectorModel connector) {
       return Font.BOLD;
    }

    @Override
    public Color getLabelColor(ConnectorModel connector) {
       return getMapDefaultNodeTextColor(connector);
    }

    @Override
    public ConnectorShape getShape(ConnectorModel connector) {
        return ConnectorShape.CUBIC_CURVE;
    }

    @Override
    public ConnectorArrows getArrows(ConnectorModel connector) {
        return isConnectorSelected(connector) ? ConnectorArrows.FORWARD : ConnectorArrows.NONE;
    }

    @Override
    public String getLinkShortText(NodeModel node) {
        return null;
    }



    @Override
    public boolean hasNodeLinks(MapModel map, JComponent component) {
       return true;
    }

    @Override
    public Collection<? extends NodeLinkModel> getLinksTo(NodeModel node, Configurable component) {
        IMapSelection selection = ((MapView)component).getMapSelection();
        if (node.isLeaf() || selection.isFolded(node)) {
            DependencySelection dependencySelection = new DependencySelection(selection);
            CodeNode codeNode = (CodeNode) node;
            Stream<CodeDependency> codeDependencies = codeNode.incomingCodeDependenciesWithKnownOrigins()
                    .filter(dep -> dependencySelection.getVisibleNode(dep.getTargetClass()) != null);
            Map<DependencyVerdict, Map<CodeNode, Long>> countedDependencies = countCodeDependencies(codeNode, dependencySelection, codeDependencies, CodeDependency::getOriginClass);
            List<CodeConnectorModel> connectors = countedDependencies.entrySet().stream()
            .flatMap(targetsByVerdict ->
                targetsByVerdict.getValue().entrySet().stream()
                    .map(countedTargets -> createConnector(countedTargets.getKey(), node.getID(), targetsByVerdict.getKey(), countedTargets.getValue().intValue()))
            )
            .collect(Collectors.toList());
            return connectors;
        }
        else
            return Collections.emptyList();
    }


    @Override
    public Collection<? extends NodeLinkModel> getLinksFrom(NodeModel node,
            Configurable component) {
        IMapSelection selection = ((MapView)component).getMapSelection();
        if (node.isLeaf() || selection.isFolded(node)) {
            DependencySelection dependencySelection = new DependencySelection(selection);
            CodeNode codeNode = (CodeNode) node;
            Stream<CodeDependency> codeDependencies = codeNode.outgoingCodeDependenciesWithKnownTargets()
                    .filter(dep -> dependencySelection.getVisibleNode(dep.getOriginClass()) != null);
            Map<DependencyVerdict, Map<CodeNode, Long>> countedDependencies = countCodeDependencies(codeNode, dependencySelection, codeDependencies, CodeDependency::getTargetClass);
            List<CodeConnectorModel> connectors = countedDependencies.entrySet().stream()
            .flatMap(targetsByVerdict ->
                targetsByVerdict.getValue().entrySet().stream()
                    .map(countedTargets -> createConnector(node, countedTargets.getKey().getID(), targetsByVerdict.getKey(), countedTargets.getValue().intValue()))
            )
            .collect(Collectors.toList());

            return connectors;
        }
        else
            return Collections.emptyList();
    }

    private Map<DependencyVerdict, Map<CodeNode, Long>> countCodeDependencies(CodeNode node,
            DependencySelection dependencySelection, Stream<CodeDependency> codeDependencies,
            Function<CodeDependency, JavaClass> dependencyToJavaClass) {
        Map<DependencyVerdict, Map<CodeNode, Long>> countedDependencies = codeDependencies
                .map(dep -> new AbstractMap.SimpleEntry<>(dep.dependencyVerdict(),
                        dependencySelection.getVisibleNode(dependencyToJavaClass.apply(dep))))
                .filter(e -> e.getValue() != null && ! e.getValue().equals(node))
                .collect(Collectors.groupingBy(
                        Entry::getKey,
                        Collectors.groupingBy(
                            Entry::getValue,
                            Collectors.counting()
                        )
                    )
                );
        return countedDependencies;
    }

    @Override
    public Component getPopupForModel(Object obj) {
        if(obj instanceof CodeConnectorModel)
            return new JLabel("To be done");
        else
            return null;

    }

    @Override
    public Icon getLinkIcon(Hyperlink link, NodeModel model) {
        return null;
    }

    @Override
    public Point getStartInclination(ConnectorModel connector) {
        CodeConnectorModel codeConnector = (CodeConnectorModel)connector;
        return (codeConnector.goesUp() ? upwardsConnectorStartInclinations : backwardsConnectorStartInclinations)
                .get(codeConnector.dependencyVerdict());
    }

    @Override
    public Point getEndInclination(ConnectorModel connector) {
        CodeConnectorModel codeConnector = (CodeConnectorModel)connector;
        return (codeConnector.goesUp() ? upwardsConnectorEndInclinations : backwardsConnectorEndInclinations)
                .get(codeConnector.dependencyVerdict());
    }


    private CodeConnectorModel createConnector(NodeModel source, String targetId, DependencyVerdict verdict, int weight) {
        NodeModel target = source.getMap().getNodeForID(targetId);
        NodeRelativePath nodeRelativePath = new NodeRelativePath(source, target);
        boolean goesUp = nodeRelativePath.compareNodePositions() > 0;
        return new CodeConnectorModel(source, targetId, weight, verdict, goesUp);
    }


}
