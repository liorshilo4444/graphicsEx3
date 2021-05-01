package edu.cg.scene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cg.Logger;
import edu.cg.algebra.Hit;
import edu.cg.algebra.Ops;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;
import edu.cg.scene.camera.PinholeCamera;
import edu.cg.scene.lightSources.Light;
import edu.cg.scene.objects.Surface;

public class Scene {
	private String name = "scene";
	private int maxRecursionLevel = 1;
	private int antiAliasingFactor = 1; //gets the values of 1, 2 and 3
	private boolean renderRefractions = false;
	private boolean renderReflections = false;
	
	private PinholeCamera camera;
	private Vec ambient = new Vec(0.1, 0.1, 0.1); //white
	private Vec backgroundColor = new Vec(0, 0.5, 1); //blue sky
	private List<Light> lightSources = new LinkedList<>();
	private List<Surface> surfaces = new LinkedList<>();
	
	
	//MARK: initializers
	public Scene initCamera(Point eyePoistion, Vec towardsVec, Vec upVec,  double distanceToPlain) {
		this.camera = new PinholeCamera(eyePoistion, towardsVec, upVec,  distanceToPlain);
		return this;
	}

	public Scene initCamera(PinholeCamera pinholeCamera) {
		this.camera = pinholeCamera;
		return this;
	}
	
	public Scene initAmbient(Vec ambient) {
		this.ambient = ambient;
		return this;
	}
	
	public Scene initBackgroundColor(Vec backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}
	
	public Scene addLightSource(Light lightSource) {
		lightSources.add(lightSource);
		return this;
	}
	
	public Scene addSurface(Surface surface) {
		surfaces.add(surface);
		return this;
	}
	
	public Scene initMaxRecursionLevel(int maxRecursionLevel) {
		this.maxRecursionLevel = maxRecursionLevel;
		return this;
	}
	
	public Scene initAntiAliasingFactor(int antiAliasingFactor) {
		this.antiAliasingFactor = antiAliasingFactor;
		return this;
	}
	
	public Scene initName(String name) {
		this.name = name;
		return this;
	}
	
	public Scene initRenderRefractions(boolean renderRefractions) {
		this.renderRefractions = renderRefractions;
		return this;
	}
	
	public Scene initRenderReflections(boolean renderReflections) {
		this.renderReflections = renderReflections;
		return this;
	}
	
	//MARK: getters
	public String getName() {
		return name;
	}
	
	public int getFactor() {
		return antiAliasingFactor;
	}
	
	public int getMaxRecursionLevel() {
		return maxRecursionLevel;
	}
	
	public boolean getRenderRefractions() {
		return renderRefractions;
	}
	
	public boolean getRenderReflections() {
		return renderReflections;
	}
	
	@Override
	public String toString() {
		String endl = System.lineSeparator(); 
		return "Camera: " + camera + endl +
				"Ambient: " + ambient + endl +
				"Background Color: " + backgroundColor + endl +
				"Max recursion level: " + maxRecursionLevel + endl +
				"Anti aliasing factor: " + antiAliasingFactor + endl +
				"Light sources:" + endl + lightSources + endl +
				"Surfaces:" + endl + surfaces;
	}
	
	private transient ExecutorService executor = null;
	private transient Logger logger = null;

	private void initSomeFields(int imgWidth, int imgHeight, double planeWidth, Logger logger) {
		this.logger = logger;
	}
	
	
	public BufferedImage render(int imgWidth, int imgHeight, double planeWidth ,Logger logger)
			throws InterruptedException, ExecutionException, IllegalArgumentException {
		
		initSomeFields(imgWidth, imgHeight, planeWidth, logger);
		
		BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
		camera.initResolution(imgHeight, imgWidth, planeWidth);
		int nThreads = Runtime.getRuntime().availableProcessors();
		nThreads = nThreads < 2 ? 2 : nThreads;
		this.logger.log("Initialize executor. Using " + nThreads + " threads to render " + name);
		executor = Executors.newFixedThreadPool(nThreads);
		
		@SuppressWarnings("unchecked")
		Future<Color>[][] futures = (Future<Color>[][])(new Future[imgHeight][imgWidth]);
		
		this.logger.log("Starting to shoot " +
			(imgHeight*imgWidth*antiAliasingFactor*antiAliasingFactor) +
			" rays over " + name);
		
		for(int y = 0; y < imgHeight; ++y)
			for(int x = 0; x < imgWidth; ++x)
				futures[y][x] = calcColor(x, y);
		
		this.logger.log("Done shooting rays.");
		this.logger.log("Wating for results...");
		
		for(int y = 0; y < imgHeight; ++y)
			for(int x = 0; x < imgWidth; ++x) {
				Color color = futures[y][x].get();
				img.setRGB(x, y, color.getRGB());
			}
		
		executor.shutdown();
		
		this.logger.log("Ray tracing of " + name + " has been completed.");
		
		executor = null;
		this.logger = null;
		
		return img;
	}
	
	private Future<Color> calcColor(int x, int y) {
		return executor.submit(() -> {
			Point pointOnScreen = camera.transform(x, y);
			Vec color = new Vec(0.0);

			Ray ray = new Ray(camera.getCameraPosition(), pointOnScreen);
			color = color.add(calcColor(ray, 0));

			return color.toColor();
		});
	}

	private Vec calcColor(Ray ray, int recusionLevel) {
		Hit closetIntersection = getClosestIntersection(ray);
		if (closetIntersection == null) {
			return backgroundColor;
		}
		Surface surface = closetIntersection.getSurface();

		Surface hitSurface = surface;
		Point hittingPoint = ray.getHittingPoint(closetIntersection);

		Vec color = ambient.mult(hitSurface.Ka());

		for (Light lightSource : lightSources) {
			Ray rayToLight = lightSource.rayToLight(hittingPoint);
			if (isLightIncluded(hittingPoint,lightSource)) {
				Vec il = lightSource.intensity(hittingPoint, rayToLight);
				Vec df = calculateDeffuse(hitSurface, lightSource, hittingPoint, closetIntersection);
				Vec sp = calculateSpecular(hitSurface, ray, rayToLight, closetIntersection);
				color  = color.add((df.add(sp)).mult(il));
			}
		}

		if (++recusionLevel > this.maxRecursionLevel){
			return color;
		}

		if(renderReflections && surface.isReflecting()){
			Ray reflactive = new Ray(hittingPoint, Ops.reflect(ray.direction(),closetIntersection.getNormalToSurface()));
			color = color.add(calcColor(reflactive, recusionLevel).mult(surface.Kr()));
		}

		if(renderRefractions && surface.isTransparent()){
			double n1 = surface.n1(closetIntersection);
			double n2 = surface.n2(closetIntersection);
			Vec refractVector = Ops.refract(ray.direction(), closetIntersection.getNormalToSurface(),n1 , n2);
			Ray refraction = new Ray(hittingPoint, refractVector);
			Vec kT = surface.Kt();
			color = color.add(calcColor(refraction, recusionLevel).mult(kT));
		}

		return color;
	}

	private Hit getClosestIntersection(Ray ray) {
		Hit closetIntersection = null;
		for (Surface surface : this.surfaces) {
			Hit intersection = surface.intersect(ray);
			if (intersection != null) {
				closetIntersection = closetIntersection == null || (intersection.compareTo(closetIntersection) < 0) ? intersection : closetIntersection;
			}
		}
		return closetIntersection;
	}

	private boolean isLightIncluded(Point hitPoint, Light lightSource){
		boolean result = true;
		for(Surface surface: this.surfaces){
			if(lightSource.isOccludedBy(surface, lightSource.rayToLight(hitPoint))){
				result = false;
			}
		}
		return result;
	}

	private Vec calculateSpecular(Surface hitSurface, Ray ray, Ray rayToLight, Hit minIntersectionValue) {
		Vec V = ray.direction();
		Vec R = Ops.reflect(rayToLight.direction(),minIntersectionValue.getNormalToSurface());
		int n = hitSurface.shininess();
		Vec kS = hitSurface.Ks();
		double dot = V.dot(R);
		return dot > 0 ? kS.mult(Math.pow(dot,n)) : new Vec();
	}

	private Vec calculateDeffuse(Surface hitSurface, Light lightSource, Point hittingPoint, Hit minIntersectionValue) {
		Vec N = minIntersectionValue.getNormalToSurface();
		Vec L = lightSource.rayToLight(hittingPoint).direction().normalize();
		Vec kD = hitSurface.Kd();
		return kD.mult(N.dot(L));
	}
}
