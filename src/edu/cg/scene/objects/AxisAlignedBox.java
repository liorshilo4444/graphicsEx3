package edu.cg.scene.objects;

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
//
    @Override
    public Hit intersect(Ray ray) {
        boolean[] negateByDim = new boolean[3];
        double[] leftIntervals = new double[3];
        double[] rightIntervals = new double[3];

        for(int currentDim = 0; currentDim < 3; currentDim++) {
            double srcPoint = ray.source().getCoordinate(currentDim);
            double direction = ray.direction().getCoordinate(currentDim);

            if (Math.abs(direction) <= Ops.epsilon) {
                if (srcPoint <= this.aAsArray[currentDim] || srcPoint >= this.bAsArray[currentDim]) {
                    return null;
                }

                leftIntervals[currentDim] = Double.NEGATIVE_INFINITY;
                rightIntervals[currentDim] = Double.POSITIVE_INFINITY;
            } else {

                double t1 = (this.aAsArray[currentDim] - srcPoint) / direction;
                double t2 = (this.bAsArray[currentDim] - srcPoint) / direction;

                if (t2 < t1) {
                    negateByDim[currentDim] = true;
                } else {
                    negateByDim[currentDim] = false;
                }

                leftIntervals[currentDim] = Math.min(t1, t2);
                rightIntervals[currentDim] = Math.max(t1, t2);
            }
        }

        double maxA = findMax(leftIntervals);
        double minB = findMin(rightIntervals);

        if (maxA > minB || minB <= Ops.epsilon) return null;

        int maxDim = findMaxDim(leftIntervals);
        Vec normal = this.getNormalByDim(maxDim);
        if (!negateByDim[maxDim]) {
            normal = normal.neg();
        }
        return new Hit(maxA, normal);
    }

    private double findMax(double[] array) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < array.length; i++) {
            max = array[i] > max ? array[i] : max;
        }
        return max;
    }

    private int findMaxDim(double[] array) {
        int maxAt = 0;

        for (int i = 0; i < array.length; i++) {
            maxAt = array[i] > array[maxAt] ? i : maxAt;
        }
        return maxAt;
    }

    private double findMin(double[] array) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < array.length; i++) {
            min = array[i] < min ? array[i] : min;
        }
        return min;
    }

    private Vec getNormalByDim(int dim) {
        switch(dim) {
            case 0:
                return new Vec(1, 0, 0);
            case 1:
                return new Vec(0, 1, 0);
            case 2:
                return new Vec(0, 0, 1);
            default:
                return null;
        }
    }
}

