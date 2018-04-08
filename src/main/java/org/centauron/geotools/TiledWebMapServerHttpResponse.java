package org.centauron.geotools;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.geotools.data.ows.HTTPResponse;

public class TiledWebMapServerHttpResponse implements HTTPResponse {

	private BufferedImage img;

	public TiledWebMapServerHttpResponse(BufferedImage image) {
		// TODO Auto-generated constructor stub
		this.img=image;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getContentType() {
		return "image/png";
	}

	@Override
	public String getResponseHeader(String headerName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	  public InputStream getResponseStream() {
    	ByteArrayOutputStream os = new ByteArrayOutputStream();
    	try {
			ImageIO.write(img, "PNG", os);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	InputStream is = new ByteArrayInputStream(os.toByteArray());
    	return is;
    }

	@Override
	public String getResponseCharset() {
		// TODO Auto-generated method stub
		return null;
	}

}
