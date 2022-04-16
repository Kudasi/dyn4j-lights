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
import com.badlogic.gdx.utils.Pools;

public class PointLight extends Light implements Attachable {
	
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
	
	private float softness = 0.2f;
	public void setSoftness(float softness) {
		this.softness = softness;
	}
	public float getSoftness() {
		return softness;
	}
	
	private Body body;
	private final Vector2 tmpPosition = new Vector2();

	public PointLight(int rays, Color color, Vector2 position, double radius) {
		super(rays, GL20.GL_TRIANGLE_FAN, GL20.GL_TRIANGLE_STRIP, color);
		this.position = position;
		this.radius = radius;
		lightMesh = new Mesh(false, rays+2, 0, new VertexAttribute(Usage.Position, 2, "a_position"),
											   new VertexAttribute(Usage.ColorPacked, 4, "a_color"),
											   new VertexAttribute(Usage.Generic, 1, "dist"));
		light = new float[(rays+2)*4];
		
		shadowMesh = new Mesh(false, (rays+1)*2, 0, new VertexAttribute(Usage.Position, 2, "a_position"),
												new VertexAttribute(Usage.ColorPacked, 4, "a_color"),
												new VertexAttribute(Usage.Generic, 1, "dist"));
		shadow = new float[(rays+1)*8];
	}

	@Override
	protected void updateVertices(int lodDiv) {
		tmpPosition.set(position);
		if(body!=null) tmpPosition.set(body.getTransform().getTransformed(tmpPosition));

		int idx = 0;
		light[idx++] = (float) tmpPosition.x;
		light[idx++] = (float) tmpPosition.y;
		light[idx++] = packedColor;
		light[idx++] = 1;
		Ray ray = Pools.obtain(Ray.class);
		for (int i = 0; i <= rays/lodDiv; i++) {
			double angle = ((float) i) / rays * Math.PI * 2 * lodDiv;
			ray.setStart(tmpPosition);
			ray.setDirection(angle);
			double r = manager.raycast(ray, radius, filter) / radius;

			light[idx++] = (float) (tmpPosition.x + Math.cos(angle) * r * radius);
			light[idx++] = (float) (tmpPosition.y + Math.sin(angle) * r * radius);
			light[idx++] = (float) packedColor;
			light[idx++] = (float) (1 - r);
		}
		Pools.free(ray);
		
		idx = 0;
		for (int i = 0; i <= rays/lodDiv; i++) {
			shadow[idx++] = light[(i+1)*4];
			shadow[idx++] = light[(i+1)*4+1];
			shadow[idx++] = packedColor;
			shadow[idx++] = light[(i+1)*4+3];

			shadow[idx++] = light[(i+1)*4] + (float)((light[(i+1)*4]-tmpPosition.x) * light[(i+1)*4+3] * softness);
			shadow[idx++] = light[(i+1)*4+1] + (float)((light[(i+1)*4+1]-tmpPosition.y) * light[(i+1)*4+3] * softness);
			shadow[idx++] = 0;
			shadow[idx++] = 0;
		}
		
		lightMesh.setVertices(light, 0, (2+rays/lodDiv) * 4);
		shadowMesh.setVertices(shadow, 0, (1+rays/lodDiv) * 8);
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
