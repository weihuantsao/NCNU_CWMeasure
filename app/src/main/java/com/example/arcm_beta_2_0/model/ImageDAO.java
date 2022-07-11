package com.example.arcm_beta_2_0.model;

import android.graphics.Bitmap;

public class ImageDAO {

	private String name;
	private Float realPixelUnit;
	private String Image_type;
	private Double result;
	private String message;
	private Bitmap bitmap;
	private Bitmap afterProcessBitmap;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Float getRealPixelUnit() {
		return realPixelUnit;
	}

	public void setRealPixelUnit(Float realPixelUnit) {
		this.realPixelUnit = realPixelUnit;
	}

	public String getImage_type() {
		return Image_type;
	}

	public void setImage_type(String Image_type) {
		this.Image_type = Image_type;
	}

	public Double getResult() {
		return result;
	}

	public void setResult(Double result) {
		this.result = result;
	}

	public String getMessage() {
		return message == null? "" : message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public Bitmap getAfterProcessBitmap() {
		return afterProcessBitmap;
	}

	public void setAfterProcessBitmap(Bitmap afterProcessBitmap) {
		this.afterProcessBitmap = afterProcessBitmap;
	}
	
}
