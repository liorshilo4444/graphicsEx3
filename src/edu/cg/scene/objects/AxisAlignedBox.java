package edu.cg.scene.objects;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.*;


public class AxisAlignedBox extends Shape{
    private final static int NDIM=3; // Number of dimensions
    private Point a = null;
    private Point b = null;
    private double[] aAsArray;
    private double[] bAsArray;

    public AxisAlignedBox(Point a, Point b){
        this.a = a;
        this.b = b;
        // We store the points as Arrays - this could be helpful for more elegant implementation.
        aAsArray = a.asArray();
        bAsArray = b.asArray();
        assert (a.x <= b.x && a.y<=b.y && a.z<=b.z);
    }

    @Override
    public String toString() {
        String endl = System.lineSeparator();
        return "AxisAlignedBox:" + endl +
                "a: " + a + endl +
                "b: " + b + endl;
    }

    public AxisAlignedBox initA(Point a){
        this.a = a;
        aAsArray = a.asArray();
        return this;
    }

    public AxisAlignedBox initB(Point b){
        this.b = b;
        bAsArray = b.asArray();
        return this;
    }

    @Override
    public Hit intersect(Ray ray) {

        double t0x = (a.x - ray.source().x) / ray.direction().x;
        double t1x = (b.x - ray.source().x) / ray.direction().x;

        double t0y = (a.y - ray.source().y) / ray.direction().y;
        double t1y = (b.y - ray.source().y) / ray.direction().y;

        double t0z = (a.z - ray.source().z) / ray.direction().z;
        double t1z = (b.z - ray.source().z) / ray.direction().z;

        double maxA = Math.max(t0x, Math.max(t0y, t0z));
        double minB = Math.min(t1x, Math.min(t1y, t1z));

        if(maxA <= minB) return new Hit(maxA, ray.direction().normalize().neg());

        return null;
    }
}

