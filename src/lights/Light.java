package com.stellusinfinitum.lights;

import org.dyn4j.collision.Filter;
import org.dyn4j.geometry.AABB;
import org.dyn4j.world.DetectFilter;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public abstract class Light {
	
	protected float[] light, shadow;
	protected Mesh lightMesh, shadowMesh;
	
	protected int rays;
	public int getRays() {
		return rays;
	}
	
	private int lightRenderType, shadowRenderType;
	private boolean culled = false;
	public boolean isCulled() {
		return culled;
	}
	
	private boolean active = true;
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	
	protected LightManager manager;
	void setLightManager(LightManager manager) {
		this.manager = manager;
	}
	
	protected final Color color = new Color();
	protected float packedColor;
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color.set(color);
		packedColor = color.toFloatBits();
	}
	public void setColor(float r, float g, float b, float a) {
		this.color.set(r,g,b,a);
		packedColor = color.toFloatBits();
	}
	public void setColor(int intColor) {
		this.color.set(intColor);
		packedColor = color.toFloatBits();
	}
	
	protected DetectFilter filter = new DetectFilter(true, true, Filter.DEFAULT_FILTER);
	public void setFilter(DetectFilter filter) {
		this.filter = filter;
	}
	
	protected Light(int rays, int lightRenderType, int shadowRenderType, Color color) {
		this.rays = rays;
		this.lightRenderType = lightRenderType;
		this.shadowRenderType = shadowRenderType;
		setColor(color);
	}
	
	protected abstract void updateVertices(int lodDivisor);
	
	public abstract boolean inBounds(double x, double y, double width, double height);
	
	public abstract double lightnessInPoint(double x, double y);
	
	public abstract void drawDebug(ShapeRenderer sr);
	
	public boolean inBounds(OrthographicCamera cam) {
		return inBounds(cam.position.x-cam.viewportWidth/2*cam.zoom, cam.position.y-cam.viewportHeight/2*cam.zoom, cam.viewportWidth*cam.zoom, cam.viewportHeight*cam.zoom);
	}
	
	public boolean inBounds(AABB aabb) {
		return inBounds(aabb.getMinX(), aabb.getMinY(), aabb.getWidth(), aabb.getHeight());
	}
	
	public boolean inBounds(Rectangle aabb) {
		return inBounds(aabb.getX(), aabb.getY(), aabb.getWidth(), aabb.getHeight());
	}

	public void update(OrthographicCamera cam, int lodDivisor) {
		updateVertices(lodDivisor);
		culled = !inBounds(cam);
	}
	
	public void draw(ShaderProgram lightShader, ShaderProgram shadowShader, int lodDivisor) {
		if(!active||culled) return;
		
		lightShader.bind();
		lightMesh.render(lightShader, lightRenderType);
		
		shadowShader.bind();
		shadowMesh.render(shadowShader, shadowRenderType);
	}
}
