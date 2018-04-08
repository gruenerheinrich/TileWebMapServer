package tileviewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.centauron.geotools.TiledWebMapServer;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.junit.Test;

public class TiledWebMapServerTest {


	
	@Test
	public void testTiledWebMapServer() throws Exception {
			//URL url = new URL("http://ows.terrestris.de/osm/service?REQUEST=GetCapabilities");
			URL url = new URL("http://129.206.228.72/cached/osm?REQUEST=GetCapabilities");
		
			WebMapServer wms = new TiledWebMapServer(url,TiledWebMapServer.WORKMODE.ADJUSTSCALE_DOWN);
			WMSCapabilities caps = wms.getCapabilities();

			Layer layer = null;
			for( Iterator i = caps.getLayerList().iterator(); i.hasNext();){
				Layer test = (Layer) i.next();
				if( test.getName() != null && test.getName().length() != 0 ){
					layer = test;
					break;
				}
			}
			GetMapRequest mapRequest = wms.createGetMapRequest();		
			mapRequest.addLayer(layer);
			
			mapRequest.setDimensions("600", "400");
			mapRequest.setFormat("image/png");
			mapRequest.setSRS("EPSG:4326");
			CRSEnvelope bbox = new CRSEnvelope("EPSG:4326",52.9, 12.85, 53, 13 );
			mapRequest.setBBox(bbox); 
			System.out.println(mapRequest.getFinalURL());
			
			GetMapResponse response = wms.issueRequest( mapRequest );		
			System.out.println(response.getContentType());
			BufferedImage image = ImageIO.read(response.getInputStream());
			ImageIO.write(image,"PNG", new File("c:/temp/out.png"));
	}
}
