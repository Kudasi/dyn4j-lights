package com.stellusinfinitum.lights;

import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Pools;

public class ConeLight extends Light implements Attachable {
	
	private Vector2 position;
	public void setPosition(double x, double y) {
		this.position.set(x, y);
	}
	
	private double radius;
	public void setRadius(double radius) {
		this.radius = radius;
	}
	public double getRadius() {
		return radius;
	}
	
	private Body body;
	private final Vector2 tmpPosition = new Vector2();
	
	private double coneSize;
	public double getConeSize() {
		return coneSize;
	}
	public void setConeSize(double coneSize) {
		this.coneSize = coneSize;
	}

	private double coneAngle;	
	public double getConeAngle() {
		return coneAngle;
	}
	public void setConeAngle(double coneAngle) {
		this.coneAngle = coneAngle;
	}
	
	private float softness = 0.2f;
	public void setSoftness(float softness) {
		this.softness = softness;
	}
	public float getSoftness() {
		return softness;
	}
	
	private float unfocus = 0.9f;
	public void setUnfocus(float unfocus) {
		this.unfocus = unfocus;
	}
	public float getUnfocus() {
		return unfocus;
	}
	
	public ConeLight(int rays, Color color, Vector2 position, double radius, double coneSize, double coneAngle) {
		super(rays, GL20.GL_TRIANGLE_STRIP, GL20.GL_TRIANGLE_STRIP, color);
		this.position = position;
		this.radius = radius;
		this.coneSize = coneSize;
		this.coneAngle = coneAngle;
		lightMesh = new Mesh(true, rays*2, 0, new VertexAttribute(Usage.Position, 2, "a_position"),
											   new VertexAttribute(Usage.ColorPacked, 4, "a_color"),
											   new VertexAttribute(Usage.Generic, 1, "dist"));
		light = new float[rays*8];
		
		shadowMesh = new Mesh(true, rays*2, 0, new VertexAttribute(Usage.Position, 2, "a_position"),
												new VertexAttribute(Usage.ColorPacked, 4, "a_color"),
												new VertexAttribute(Usage.Generic, 1, "dist"));
		shadow = new float[rays*8];
	}

	@Override
	protected void updateVertices(int lodDiv) {
		tmpPosition.set(position);
		double rotation = coneAngle;
		if(body!=null) {
			tmpPosition.set(body.getTransform().getTransformed(tmpPosition));
			rotation += body.getTransform().getRotationAngle();
		}

		Ray ray = Pools.obtain(Ray.class);
		for (int i = 0; i < rays/lodDiv; i++) {
			double angle = ((float) i) / rays * coneSize * lodDiv + rotation - coneSize/2;
			ray.setStart(tmpPosition);
			ray.setDirection(angle);
			
			double f = Math.min((1 - Math.abs(angle-rotation) * 2 / coneSize) / unfocus, 1);	
			f = Interpolation.fade.apply((float)f);
			double r = manager.raycast(ray, radius, filter) / radius;
			
			light[i*8] = (float) tmpPosition.x;
			light[i*8+1] = (float) tmpPosition.y;
			light[i*8+2] = packedColor;
			light[i*8+3] = (float) f;

			light[i*8+4] = (float) (tmpPosition.x + Math.cos(angle) * r * radius);
			light[i*8+5] = (float) (tmpPosition.y + Math.sin(angle) * r * radius);
			light[i*8+6] = packedColor;
			light[i*8+7] = (float) ((1 - r) * f);

			shadow[i*8] = light[i*8+4];
			shadow[i*8+1] = light[i*8+5];
			shadow[i*8+2] = packedColor;
			shadow[i*8+3] = light[i*8+7];

			shadow[i*8+4] = light[i*8+4] + (float)((light[i*8+4]-tmpPosition.x) * light[i*8+7] * softness);
			shadow[i*8+5] = light[i*8+5] + (float)((light[i*8+5]-tmpPosition.y) * light[i*8+7] * softness);
			shadow[i*8+6] = 0;
			shadow[i*8+7] = 0;
		}
		Pools.free(ray);
		
		lightMesh.setVertices(light, 0, rays/lodDiv * 8);
		shadowMesh.setVertices(shadow, 0, rays/lodDiv * 8);
	}

	@Override
	public boolean inBounds(double x, double y, double width, double height) {
		return (tmpPosition.x+radius>x&&tmpPosition.x-radius<x+width&&tmpPosition.y+radius>y&&tmpPosition.y-radius<y+height);
	}

	@Override
	public double lightnessInPoint(double x, double y) {
		return 1-Math.min(position.distance(x, y)/radius, 1);
	}

	@Override
	public void drawDebug(ShapeRenderer sr) {
		if(!isActive()||isCulled()) return;
		for(int i = 1;i < getRays();i++) {
			sr.line((float)light[i*4], (float)light[i*4+1], (float)light[(i+1)*4], (float)light[(i+1)*4+1]);
		}
	}
	
	@Override
	public void attachToBody(Body body) {
		this.body = body;
	}
	
	@Override
	public Body getBody() {
		return body;
	}
}