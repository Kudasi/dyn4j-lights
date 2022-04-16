package com.stellusinfinitum.lights;

import org.dyn4j.dynamics.PhysicsBody;
import org.dyn4j.geometry.Ray;
import org.dyn4j.world.DetectFilter;
import org.dyn4j.world.World;
import org.dyn4j.world.result.RaycastResult;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

public class LightManager {
	
	private Array<Light> lights = new Array<>();
	
	static {
		ShaderProgram.pedantic = false;
	}
	private final ShaderProgram defaultLightShader = new ShaderProgram(Gdx.files.internal("shaders/light.vert"), Gdx.files.internal("shaders/light.frag"));
	private final ShaderProgram defaultShadowShader = new ShaderProgram(Gdx.files.internal("shaders/light.vert"), Gdx.files.internal("shaders/light.frag"));
	private final ShaderProgram postShader = new ShaderProgram(Gdx.files.internal("shaders/frameShader.vert"), Gdx.files.internal("shaders/post.frag"));
	
	private ShaderProgram lightShader = defaultLightShader;
	public void setLightShader(ShaderProgram lightShader) {
		this.lightShader = lightShader;
	}
	public void setDefaultLightShader() {
		this.lightShader = defaultLightShader;
	}
	
	private ShaderProgram shadowShader = defaultShadowShader;
	public void setShadowShader(ShaderProgram shadowShader) {
		this.shadowShader = shadowShader;
	}
	public void setDefaultShadowShader() {
		this.shadowShader = defaultShadowShader;
	}
	
	private OrthographicCamera camera;
	public OrthographicCamera getCamera() {
		return camera;
	}
	public void setCamera(OrthographicCamera camera) {
		this.camera = camera;
	}
	
	private World<? extends PhysicsBody> world;
	public World<? extends PhysicsBody> getWorld() {
		return world;
	}
	public void setWorld(World<? extends PhysicsBody> world) {
		this.world = world;
	}

	private final Color ambientColor = new Color(0f,0,0f,1);
	public void setAmbient(Color color) {
		this.ambientColor.set(color);
	}	
	public void setAmbient(float r,float g,float b,float a) {
		this.ambientColor.set(r,g,b,a);
	}	
	public void setAmbient(int color) {
		this.ambientColor.set(color);
	}
	
	private ShapeRenderer debug = new ShapeRenderer();
	
	public LightManager(World<? extends PhysicsBody> world, OrthographicCamera camera) {
		this.world = world;
		this.camera = camera;
		debug.setAutoShapeType(true);
		lightMesh.setVertices(new float[] {-1,-1,0,0,1,-1,1,0,1,1,1,1,-1,1,0,1});
		postShader.bind();
		postShader.setUniformf("pixel", 1f/Gdx.graphics.getWidth(), 1f/Gdx.graphics.getHeight());
		postShader.setUniformi("samples", blur);
		postShader.setUniformf("ambient", ambientColor);
		
	}

	private int lodDiv;
	public void setLodDivisor(int lodDiv) {
		this.lodDiv = (int) Math.pow(2, lodDiv-1);
	}
	
	private int blur = 2;
	public void setBlur(int blur) {
		this.blur = blur;
	}
	
	public void update() {
		for(int i = lights.size-1;i>=0;i--)
			lights.get(i).update(camera, lodDiv);
	}
	
	public double ligtnessInPoint(double x, double y) {
		double l = 0;
		for(int i = lights.size-1;i>=0;i--)
			l=Math.max(lights.get(i).lightnessInPoint(x, y), l);
		return l;
	}
	
	private FrameBuffer fbo = new FrameBuffer(Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
	private Mesh lightMesh = new Mesh(true, 4, 0, new VertexAttribute(Usage.Position, 2, "a_position"), new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord"));
	
	public void draw() {		
		lightShader.bind();
		lightShader.setUniformMatrix("u_projTrans", camera.combined);
		
		shadowShader.bind();
		shadowShader.setUniformMatrix("u_projTrans", camera.combined);

		Gdx.gl.glEnable(GL20.GL_BLEND);
		fbo.begin();
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(0x4000);

		for (int i = lights.size - 1; i >= 0; i--)
			lights.get(i).draw(lightShader, shadowShader, lodDiv);

		fbo.end();
		fbo.getColorBufferTexture().bind(0);

		Gdx.gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_ZERO);
			
		postShader.bind();
		postShader.setUniformf("ambient", ambientColor);
		postShader.setUniformi("samples", blur);
		lightMesh.render(postShader, GL20.GL_TRIANGLE_FAN);
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}
	
	public void drawDebug() {
		debug.setProjectionMatrix(camera.combined);
		debug.begin();
		for(int i = lights.size-1;i>=0;i--)
			lights.get(i).drawDebug(debug);;
		debug.end();
	}

	double raycast(Ray ray, double distance, DetectFilter filter) {
		RaycastResult res = world.raycastClosest(ray, distance, filter);
		if(res!=null) return res.getRaycast().getDistance();
		return distance;
	}
	
	public void add(Light l) {
		l.setLightManager(this);
		lights.add(l);
	}
	
	public void remove(Light l) {
		l.setLightManager(null);
		lights.removeValue(l, true);
	}
	
	public void remove(int idx) {
		lights.get(idx).setLightManager(null);
		lights.removeIndex(idx);
	}
}
