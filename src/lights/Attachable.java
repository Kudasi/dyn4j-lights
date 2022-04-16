package com.stellusinfinitum.lights;

import org.dyn4j.dynamics.Body;

public interface Attachable {
	public void attachToBody(Body body);
	public Body getBody();
}
