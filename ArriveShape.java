package decision;

import processing.core.PApplet;
import processing.core.PVector;

public class ArriveShape {
	PVector position;
	PVector velocity;
	PVector acceleration;
	PVector lastVelocity;

	float orientation = 0;
	float rotation = 5;
	float initial = 1;
	double angular_accel = 0;
	float max_accel = 0.9f;
	float max_angular_accel = 2;
	float max_velocity = 3;
	float max_rotation = 2;

	boolean mouse;
	PApplet parent;

	public ArriveShape(final PApplet p) {
		// initialize variables
		parent = p;
		velocity = new PVector(0, 0);
		lastVelocity = new PVector(0, 0);
		position = new PVector(500, 500);
		acceleration = new PVector(0, 0);

		mouse = false;
	}

	// Display
	public void display() {
		double target_orientation = Math.atan(-velocity.y / velocity.x);

		if (velocity.x < 0) {
			target_orientation += Math.PI;
		}

		if (target_orientation < 0 && velocity.x > 0) {
			target_orientation += 2 * Math.PI;
		}

		final double character_orientation = Math.toRadians(orientation);

		final float desired_rotation = mapToRange(target_orientation - character_orientation);
		final float rot_size = Math.abs(desired_rotation);

		if (rot_size > 0.05) {
			orientation = mapToRange(Math.toDegrees(target_orientation));
		}

		if (orientation >= 360) {
			orientation -= 360;
		}

		final float tempX = position.x;
		final float tempY = position.y;
		parent.stroke(0);
		parent.fill(0);
		parent.ellipse(position.x, position.y, 25, 25);

		parent.fill(0, 0, 0);
		for (int i = 0; i < parent.width; i += 50) {
			parent.rect(i, 0, 5, parent.height);
		}
		for (int i = 0; i < parent.height; i += 50) {
			parent.rect(0, i, parent.width, 5);
		}

		parent.pushMatrix();

		// triangle rotate
		parent.translate(tempX, tempY);
		parent.rotate(PApplet.radians(360 - orientation));
		parent.translate(-tempX, -tempY);
		parent.fill(0);
		parent.triangle(position.x + 2, position.y - 12, position.x + 2, position.y + 12, position.x + 25, position.y);
		parent.popMatrix();

	}

	public void applyUpdate(final float delta_time, final boolean seek) {
		acceleration.limit(max_accel);
		velocity.add(PVector.mult(acceleration, delta_time));
		orientation += rotation * delta_time;
		rotation += angular_accel * delta_time;

		velocity.limit(max_velocity);
		position.add(PVector.mult(velocity, delta_time));

		if (Math.abs(rotation) > max_rotation) {
			rotation = max_rotation * rotation / Math.abs(rotation);
		}

		acceleration.mult(0);

	}

	public void arrive2(final float rod, final float ros, final float timeToTargetVelocity,
			final float timeToTargetRotation, final PVector target_position) {
		// modify translation
		float goalSpeed = 0;
		final PVector accel = new PVector(0, 0);

		final PVector direction = PVector.sub(target_position, position);
		final double distance = Math.abs(direction.mag());
		direction.normalize();

		if (distance > rod) {
			goalSpeed = max_velocity;
		} else {
			goalSpeed = (float) (max_velocity * distance / (rod - ros));
		}

		direction.mult(goalSpeed);
		PVector goalVelocity = PVector.sub(direction, velocity);

		final float speed = goalVelocity.mag();
		final PVector m = new PVector(0, 0);
		m.set(goalVelocity);
		final PVector direction1 = m.normalize();

		if (speed > max_accel) {
			goalVelocity = PVector.mult(direction1, max_accel);
		}
		accel.set(goalVelocity);
		accel.div(timeToTargetVelocity);

		acceleration.add(accel);
	}

	public void wander() {
		final float wanderRadius = 50;
		final double wanderRate = 45;
		final float wanderOffset = 50;
		final double wanderOrientation = (Math.random() * wanderRate - wanderRate * 0.5);
		PVector circleCenter = velocity.copy();
		circleCenter.normalize();
		circleCenter.mult(wanderOffset);

		PVector displacement = new PVector(0, -1);
		displacement.mult(wanderRadius);
		displacement.x = (float) (Math.cos(wanderOrientation) * displacement.mag());
		displacement.y = (float) (Math.sin(wanderOrientation) * displacement.mag());

		final float xoffset = this.position.x > 0 ? this.max_velocity * 3 : -this.max_velocity * 3;
		final float yoffset = this.position.y > 0 ? this.max_velocity * 3 : -this.max_velocity * 3;
		final PVector collisionPoint = new PVector(this.position.x + xoffset, this.position.y + yoffset);

		boolean collision = false;
		if (collisionPoint.x > parent.width || collisionPoint.x < 0 || collisionPoint.y > parent.height
				|| collisionPoint.y < 0) {
			this.orientation += 180;
			final float speed = this.velocity.mag();
			PVector velCop = this.velocity.copy();
			final PVector direction = velCop.normalize();
			direction.mult(-1);
			this.velocity = PVector.mult(direction, speed);
			this.position.add(this.velocity);
			this.acceleration = new PVector(0, 0);

			collision = true;
		}
		PVector steer = new PVector(0, 0);
		if (!collision) {
			steer = circleCenter.add(displacement);
		}

		// this.seek( target, this.t_orientation );

		//		this.arrive2(40, 1, 1, 1, target);
		this.acceleration.add(steer);
	}

	public void seek(PVector target) {
		PVector desired_velocity = PVector.sub(target, this.position);
		desired_velocity.limit(max_velocity);
		this.acceleration.add(desired_velocity);
	}

	public float mapToRange(final double r) {
		// final float R = (float) ( r % ( 2 * Math.PI ) );

		if (Math.abs(r) <= Math.PI) {
			return (float) r;
		} else if (r > Math.PI) {
			return (float) (r - 2 * Math.PI);
		} else {
			return (float) (r + 2 * Math.PI);
		}
	}

}
