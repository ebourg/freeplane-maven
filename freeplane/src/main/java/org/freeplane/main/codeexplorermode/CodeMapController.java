package org.freeplane.main.codeexplorermode;

import java.awt.EventQueue;
import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;

import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.styles.MapStyleModel;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.view.swing.map.MapView;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;

class CodeMapController extends MapController {
	CodeMapController(CodeModeController modeController) {
		super(modeController);
	}

	public CodeModeController getCodeModeController() {
		return (CodeModeController) Controller.getCurrentModeController();
	}

	@Override
    public MapModel newMap() {
	    final CodeMapModel codeMapModel = new CodeMapModel(getModeController().getMapController().duplicator());
	    fireMapCreated(codeMapModel);
	    createMapView(codeMapModel);
	    return codeMapModel;
	}

	@Override
	protected void fireFoldingChanged(final NodeModel node) {/**/}

	void explore(CodeExplorerConfiguration codeExplorerConfiguration) {
	    CodeMapModel map = (CodeMapModel) Controller.getCurrentController().getSelection().getMap();
	    EmptyNodeModel emptyRoot = new EmptyNodeModel(map, "Loading...");
	    map.setRoot(emptyRoot);

	    IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
	    MapView mapView = (MapView) mapViewManager.getMapViewComponent();
	    mapView.setRootNode(map.getRootNode());
	    mapViewManager.updateMapViewName();
	    new Thread(() -> {
	        JavaPackage rootPackage;
	        if(codeExplorerConfiguration != null) {
	            rootPackage = codeExplorerConfiguration.importPackages();
	        }
	        else {
	            ClassFileImporter classFileImporter = new ClassFileImporter();
	            JavaClasses importedClasses  = classFileImporter.importPackages("org.freeplane");
	            rootPackage = importedClasses.getPackage("org.freeplane");
	        }

	        EventQueue.invokeLater(() -> {
	            PackageNodeModel newRoot = new PackageNodeModel(rootPackage, map, rootPackage.getName());
	            newRoot.setFolded(false);
	            map.setRoot(newRoot);
	            mapView.setRootNode(map.getRootNode());
	            mapViewManager.updateMapViewName();
	        });
	    }, "Load explored packages").start();

	}

}
