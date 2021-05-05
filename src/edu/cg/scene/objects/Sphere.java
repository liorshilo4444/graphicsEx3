package edu.cg.scene.objects;

import edu.cg.algebra.Hit;
import edu.cg.algebra.Ops;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;

public class Sphere extends Shape {
	private Point center;
	private double radius;

	public Sphere(Point center, double radius) {
		this.center = center;
		this.radius = radius;
	}

	public Sphere() {
		this(new Point(0, -0.5, -6), 0.5);
	}

	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Sphere:" + endl +
				"Center: " + center + endl +
				"Radius: " + radius + endl;
	}


	public Sphere initCenter(Point center) {
		this.center = center;
		return this;
	}

	public Sphere initRadius(double radius) {
		this.radius = radius;
		return this;
	}

	@Override
	public Hit intersect(Ray ray) {
		Vec sub = ray.source().sub(center);
		double b = 2 * ray.direction().dot(sub);
		double c = sub.dot(sub) - radius * radius;

		double sqrtFormula = Math.sqrt(Math.pow(b, 2) - (4 * c));
		if(Double.isNaN(sqrtFormula)) return null;

		double s2 = ((-1 * b) + sqrtFormula) / 2;
		double s1 = ((-1 * b) - sqrtFormula) / 2;

		if(s1 > Ops.epsilon && s1 < Ops.infinity){
			Vec normal = ray.add(s1).sub(center).normalize();
			return new Hit(s1, normal);
		}else if(s2 > Ops.epsilon){
			Vec normal = ray.add(s2).sub(center).normalize();
			return s2 > Ops.epsilon & s2 < Ops.infinity ? new Hit(s2, normal) : null;
		}

		return null;
	}
}
