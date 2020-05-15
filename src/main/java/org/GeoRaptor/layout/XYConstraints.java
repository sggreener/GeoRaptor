package org.GeoRaptor.layout;

import java.io.Serializable;

/**
 * Temporary Layout file for preference
 * @author Bessie Gong 
 * @version 10 Oct 2019
 *
 */
public class XYConstraints implements Cloneable, Serializable {
	int x;
	int y;
	int width;
	int height;

	public XYConstraints() {
		this(0, 0, 0, 0);
	}

	public XYConstraints(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public int getX() {
		return this.x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return this.y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return this.width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return this.height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int hashCode() {
		return this.x ^ this.y * 37 ^ this.width * 43 ^ this.height * 47;
	}

	public boolean equals(Object that) {
		if (!(that instanceof XYConstraints)) {
			return false;
		} else {
			XYConstraints other = (XYConstraints) that;
			return other.x == this.x && other.y == this.y && other.width == this.width && other.height == this.height;
		}
	}

	public Object clone() {
		return new XYConstraints(this.x, this.y, this.width, this.height);
	}

	public String toString() {
		return "XYConstraints[" + this.x + "," + this.y + "," + this.width + "," + this.height + "]";
	}
}