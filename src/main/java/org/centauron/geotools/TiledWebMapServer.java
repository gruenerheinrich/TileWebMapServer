package org.centauron.geotools;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import java.awt.Dimension;
import org.apache.log4j.Logger;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.ows.ServiceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;

public class TiledWebMapServer extends WebMapServer {

	final static Logger logger = Logger.getLogger(TiledWebMapServer.class);
	private Hashtable<String,double[]> m_resolutions;
	public enum WORKMODE {SCALE_MUST_MATCH,SCALEIMAGE_DOWN,SCALEIMAGE_BOTH,ADJUSTSCALE_DOWN,ADJUSTSCALE_UP,ADJUSTSCALE_BOTH};
	private WORKMODE workMode = WORKMODE.SCALEIMAGE_DOWN;
	public TiledWebMapServer(URL serverURL, WORKMODE mode) throws Exception {
		super(serverURL);
		workMode=mode;
		doInit();
	}
	public TiledWebMapServer(URL serverURL) throws Exception {
		this(serverURL,WORKMODE.SCALEIMAGE_DOWN);
	}

	@Override
    public GetMapResponse issueRequest(GetMapRequest request) throws IOException, ServiceException {
        
		String bbox=request.getProperties().getProperty("BBOX");
		String width=request.getProperties().getProperty("WIDTH");
		String height=request.getProperties().getProperty("HEIGHT");

		logger.info("BBOX is:"+bbox);
		TileEnvelope envelope=TiledWebMapServer.getEnvelopeFromBBoxAndSrs(bbox, "EPSG:4326",width,height);
		BufferedImage image=new BufferedImage(envelope.getDimension().width,envelope.getDimension().height,BufferedImage.TYPE_INT_ARGB);
		BufferedImage store=null;
		double scale=envelope.getPixelXSize();
		double caching_scale=0;
		try {
			caching_scale=this.getBestMatchingScale(scale,"EPSG:4326");
		} catch (Exception e) {
			throw new ServiceException("SCALE");
		}
		if (needsStoreImage()) {
			//CREATE A NEW ENVELOPE THAT MATCHES SCALE
			envelope=envelope.adjustToScale(caching_scale);
			scale=envelope.getPixelXSize();
			store=image;
			image=new BufferedImage(envelope.getDimension().width,envelope.getDimension().height,BufferedImage.TYPE_INT_ARGB);
		}
		if (needsScaleAdjustment()) {
			envelope=envelope.setScale(caching_scale);
			scale=caching_scale;
		}
		Vector<TileEnvelope> requestEnvelopes=this.getEnvelopes(caching_scale,envelope,scale);
		Graphics g=image.getGraphics();
		int i=0;
		for (TileEnvelope e:requestEnvelopes) {
			request.setBBox(e.getEnvelope());
			request.setDimensions(e.getDimension());
			System.out.println(request.getFinalURL());
			GetMapResponse response=(GetMapResponse)this.internalIssueRequest(request);
			BufferedImage subimage = ImageIO.read(response.getInputStream());
			ImageIO.write(subimage,"PNG", new File("c:/temp/sub_"+i +".png"));					
			Dimension offset=e.getOffset(envelope);
			System.out.println("Offset:"+offset.width+" - " +offset.height);
			g.drawImage(subimage,offset.width,envelope.getDimension().height- offset.height-e.getDimension().height,null);
			ImageIO.write(image,"PNG", new File("c:/temp/out_"+i +".png"));					
			i=i+1;
		}
		if (store!=null) {
			//SCALE image to store
			ImageIO.write(image,"PNG", new File("c:/temp/store.png"));
			Graphics2D g2=(Graphics2D)store.getGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	
			g2.drawImage(image, 0, 0, store.getWidth(),store.getHeight(), null);
			image=store;
		}
		GetMapResponse resp=new GetMapResponse(new TiledWebMapServerHttpResponse(image));
		
		return resp;
    }

	private boolean needsScaleAdjustment() {
		return this.workMode==WORKMODE.ADJUSTSCALE_DOWN || this.workMode==WORKMODE.ADJUSTSCALE_UP || this.workMode==WORKMODE.ADJUSTSCALE_BOTH; 
	}

	private boolean needsStoreImage() {
		return this.workMode==WORKMODE.SCALEIMAGE_DOWN || this.workMode==WORKMODE.SCALEIMAGE_BOTH; 
	}	
	
	private Vector<TileEnvelope> getEnvelopes(double caching_scale,TileEnvelope envelope,double scale) {
		int tileWidth=256;
		int tileHeight=256;
		Dimension dim=new Dimension(tileWidth,tileHeight);
		Vector<TileEnvelope> envelopes=new Vector<TileEnvelope>();
		double x1=envelope.getEnvelope().getMinX()/scale/tileWidth;
		x1=Math.floor(x1);
		double x2=envelope.getEnvelope().getMaxX()/scale/tileWidth;
		x2=Math.floor(x2+2);
		double y1=envelope.getEnvelope().getMinY()/scale/tileHeight;
		y1=Math.floor(y1);
		double y2=envelope.getEnvelope().getMaxY()/scale/tileHeight;
		y2=Math.floor(y2+2);

		for (int x=(int)x1;x<x2;x++) {
			for (int y=(int)y1;y<y2;y++) {
				TileEnvelope e=new TileEnvelope(new CRSEnvelope(envelope.getEnvelope().getSRSName(),x*tileWidth*caching_scale,y*tileHeight*caching_scale,(x+1)*tileWidth*caching_scale,(y+1)*tileHeight*caching_scale),dim);
				envelopes.add(e);
			}
		}
		
		return envelopes;
	}
	
	
	private double getBestMatchingScale(double scale,String srsname) throws Exception {
		
		double best_scale;
		double[] res=m_resolutions.get(srsname);
		if (res==null) {
			throw new Exception("no matching SRS");
		}
		switch (this.workMode) {
			case SCALEIMAGE_BOTH:
			case ADJUSTSCALE_BOTH:
				double b=res[0];
				for (double d:res) {
					if (Math.abs(d-scale)<Math.abs(b-scale)) {
						b=d;
					}
				}
				return b;
			case SCALE_MUST_MATCH:
				for (double d:res) {
					if (d==scale) return scale;
				}
				break;
			case SCALEIMAGE_DOWN:
			case ADJUSTSCALE_UP:				
				for (double d:res) {
					if (d<scale) return d;
				}
				break;
			case ADJUSTSCALE_DOWN:				
				double d_last=res[0];
				for (double d:res) {
					if (d<scale) return d_last;
					d_last=d;
				}
				break;				
		}
		throw new Exception("blubb");
	}
	
	private static TileEnvelope getEnvelopeFromBBoxAndSrs(String bbox,String srs,String w,String h) {
		String splitted_bbox[]=bbox.split(",");
		double[] numeric_bbox=new double[splitted_bbox.length];
		for (int i=0;i<splitted_bbox.length;i++) {
			numeric_bbox[i]=Double.parseDouble(splitted_bbox[i]);
		}
		CRSEnvelope cbbox = new CRSEnvelope(srs,numeric_bbox[0], numeric_bbox[1],numeric_bbox[2], numeric_bbox[3] );
		TileEnvelope env=new TileEnvelope(cbbox,new Dimension(Integer.parseInt(w),Integer.parseInt(h)));
		return env;
	}
	
	private static Element getFirstElementByTagName(Element e,String name) {
		NodeList nlsrs=e.getElementsByTagName(name);
		Element first=(Element)nlsrs.item(0);
		return first;
		
	}
	

	private static Vector<String> getAllTextContentsByTagName(Element el, String name) {
		Vector<String> v=new Vector();
		NodeList nl=el.getElementsByTagName(name);
		for (int i=0;i<nl.getLength();i++) {
			Element e=(Element)nl.item(i);
			v.add(e.getTextContent());
		}
		return v;
	}	
	private void doInit() throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		InputStream is=TiledWebMapServer.class.getClassLoader().getResourceAsStream("gridsets.xml");
		Document configuration = dBuilder.parse(is);		
		
		this.m_resolutions=new Hashtable();
		//PICK ALL GRIDSETS
		NodeList nl=configuration.getElementsByTagName("gridSet");
		for (int i=0;i<nl.getLength();i++) {
			Element n=(Element)nl.item(i);
			Element srsnumber=this.getFirstElementByTagName(n, "srs");
			String number=this.getFirstElementByTagName(srsnumber, "number").getTextContent();
			String srsname="EPSG:"+number;
			Element resolutions=this.getFirstElementByTagName(n,"resolutions");
			Vector<String> res_texts=this.getAllTextContentsByTagName(resolutions,"double");
			double[] res=new double[res_texts.size()];
			for (int ii=0;ii<res_texts.size();ii++) {
				res[ii]=Double.parseDouble(res_texts.get(ii));
			}
			this.m_resolutions.put(srsname, res);
		}
	}

}