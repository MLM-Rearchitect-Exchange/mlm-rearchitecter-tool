package exporter;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

public interface Format {

	public void openExportFile(URI destinationURI) throws IOException;
	
	public boolean export(Resource multilevelResource) throws IOException;

	public void closeExportFile() throws IOException;

}
