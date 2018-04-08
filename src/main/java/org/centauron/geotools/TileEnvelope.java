package org.centauron.geotools;

import java.awt.Dimension;

import org.geotools.data.ows.CRSEnvelope;

public class TileEnvelope {
	private CRSEnvelope envelope;
	private Dimension	dimension;
	private boolean		xyswitched=true;
	public TileEnvelope(CRSEnvelope env,Dimension dim) {
		envelope=env;
		dimension=dim;
	}
	public CRSEnvelope getEnvelope() {
		return envelope;
	}
	public void setEnvelope(CRSEnvelope envelope) {
		this.envelope = envelope;
	}
	public Dimension getDimension() {
		return dimension;
	}
	public void setDimension(Dimension dimension) {
		this.dimension = dimension;
	}
	
	public Dimension getOffset(TileEnvelope e) {
		
		double w=(this.getEnvelope().getMinX()-e.getEnvelope().getMinX())/this.getPixelXSize();
		double h=(this.getEnvelope().getMinY()-e.getEnvelope().getMinY())/this.getPixelYSize();
		if (xyswitched) {
			return new Dimension((int)h,(int)w);
		} else {
			return new Dimension((int)w,(int)h);
		}
	}
	
	public double getPixelXSize() {
		if (xyswitched) {
			return (this.getEnvelope().getMaxX()-this.getEnvelope().getMinX()) /this.getDimension().getHeight();
		} else {
			return (this.getEnvelope().getMaxX()-this.getEnvelope().getMinX()) /this.getDimension().getWidth();
		}
	}
	
	public TileEnvelope setScale(double scale) {
		//KEEP CENTER
		double xlength=0;
		double ylength=0;
		
		if (xyswitched) {
			xlength=scale*this.getDimension().getHeight();
			ylength=scale*this.getDimension().getWidth();			
		} else {
			ylength=scale*this.getDimension().getHeight();
			xlength=scale*this.getDimension().getWidth();			
		}
		double x_delta=(xlength-(this.getEnvelope().getMaxX()-this.getEnvelope().getMinX()))/2;
		double y_delta=(ylength-(this.getEnvelope().getMaxY()-this.getEnvelope().getMinY()))/2;
		
		this.getEnvelope().setMinX(this.getEnvelope().getMinX()-x_delta);
		this.getEnvelope().setMaxX(this.getEnvelope().getMaxX()+x_delta);
		this.getEnvelope().setMinY(this.getEnvelope().getMinY()-y_delta);
		this.getEnvelope().setMaxY(this.getEnvelope().getMaxY()+y_delta);
		
		return this;
	}
	
	public double getPixelYSize() {
		if (xyswitched) {		
			return (this.getEnvelope().getMaxY()-this.getEnvelope().getMinY())/this.getDimension().getWidth();
		} else {
			return (this.getEnvelope().getMaxY()-this.getEnvelope().getMinY())/this.getDimension().getHeight();
			
		}
	}
	public TileEnvelope adjustToScale(double scale) {
		double new_xpixel=(this.getEnvelope().getMaxX()-this.getEnvelope().getMinX())/scale;
		double new_ypixel=(this.getEnvelope().getMaxY()-this.getEnvelope().getMinY())/scale;
		Dimension d;
		if (xyswitched) {
			d=new Dimension((int)new_ypixel,(int)new_xpixel);
		} else {
			d=new Dimension((int)new_xpixel,(int)new_ypixel);
			
		}
		return new TileEnvelope(this.getEnvelope(),d);
	}	
}
