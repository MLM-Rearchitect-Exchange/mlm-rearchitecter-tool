package importer;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

public interface Format {

	public void openImportFile(URI destinationURI) throws IOException;
	
	public boolean importHierarchy(Resource multilevelResource) throws IOException;

	public void closeImportFile() throws IOException;

}
