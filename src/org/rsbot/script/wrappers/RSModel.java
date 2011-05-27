package org.rsbot.script.wrappers;

import org.rsbot.client.Model;
import org.rsbot.script.methods.MethodContext;
import org.rsbot.script.methods.MethodProvider;
import org.rsbot.script.util.Filter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * A screen space model.
 *
 * @author Jacmob
 * @author SpeedWing
 */
public abstract class RSModel extends MethodProvider {
	/**
	 * Returns a filter that matches against the array of point indices for the
	 * A vertices of each triangle. Use in scripts is discouraged.
	 *
	 * @param vertex_a The array of indices for A vertices.
	 * @return The vertex point index based model filter.
	 */
	public static Filter<RSModel> newVertexFilter(final short[] vertex_a) {
		return new Filter<RSModel>() {
			public boolean accept(final RSModel m) {
				return Arrays.equals(m.indices1, vertex_a);
			}
		};
	}

	protected int[] xPoints;
	protected int[] yPoints;
	protected int[] zPoints;

	protected short[] indices1;
	protected short[] indices2;
	protected short[] indices3;

	public RSModel(final MethodContext ctx, final Model model) {
		super(ctx);
		xPoints = model.getXPoints();
		yPoints = model.getYPoints();
		zPoints = model.getZPoints();
		indices1 = model.getIndices1();
		indices2 = model.getIndices2();
		indices3 = model.getIndices3();
	}

	protected abstract int getLocalX();

	protected abstract int getLocalY();

	protected abstract void update();

	/**
	 * @param p A point on the screen
	 * @return true of the point is within the bounds of the model
	 */
	private boolean contains(final Point p) {
		if (this == null) {
			return false;
		}

		final Polygon[] triangles = getTriangles();
		for (final Polygon poly : triangles) {
			if (poly.contains(p)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Clicks the RSModel.
	 *
	 * @param leftClick if true it left clicks.
	 * @return true if clicked.
	 */
	public boolean doClick(final boolean leftClick) {
		try {
			for (int i = 0; i < 10; i++) {
				methods.mouse.move(getPoint());
				if (contains(methods.mouse.getLocation())) {
					methods.mouse.click(leftClick);
					return true;
				}
			}
		} catch (final Exception ignored) {
		}
		return false;
	}

	/**
	 * Clicks the RSModel and clicks the menu action
	 *
	 * @param action the action to be clicked in the menu
	 * @param option the option of the action to be clicked in the menu
	 * @return true if clicked, false if failed.
	 */
	public boolean doAction(final String action, final String option) {
		try {
			for (int i = 0; i < 10; i++) {
				methods.mouse.move(getPoint());
				if (contains(methods.mouse.getLocation())) {
					if (methods.menu.doAction(action, option)) {
						return true;
					}
				}
			}
		} catch (final Exception ignored) {
		}
		return false;
	}

	/**
	 * Clicks the RSModel and clicks the menu action
	 *
	 * @param action the action to be clicked in the menu
	 * @return true if clicked, false if failed.
	 */
	public boolean doAction(final String action) {
		return doAction(action, null);
	}

	/**
	 * Returns a random screen point.
	 *
	 * @return A screen point, or Point(-1, -1) if the model is not on screen.
	 * @see #getCentralPoint()
	 * @see #getPointOnScreen()
	 */
	public Point getPoint() {
		update();
		final int len = indices1.length;
		final int sever = random(0, len);
		Point point = getPointInRange(sever, len);
		if (point != null) {
			return point;
		}
		point = getPointInRange(0, sever);
		if (point != null) {
			return point;
		}
		return new Point(-1, -1);
	}

	/**
	 * Returns all the screen points.
	 *
	 * @return All the points that are on the screen, if the model is not on the
	 *         screen it will return null.
	 */
	public Point[] getPoints() {
		if (this == null) {
			return null;
		}
		final Polygon[] polys = getTriangles();
		final Point[] points = new Point[polys.length * 3];
		int index = 0;
		for (final Polygon poly : polys) {
			for (int i = 0; i < 3; i++) {
				points[index++] = new Point(poly.xpoints[i], poly.ypoints[i]);
			}
		}
		return points;
	}

	/**
	 * Gets a point on a model that is on screen.
	 *
	 * @return First point that it finds on screen else a random point on screen
	 *         of an object.
	 */
	public Point getPointOnScreen() {
		final ArrayList<Point> list = new ArrayList<Point>();
		try {
			final Polygon[] tris = getTriangles();
			for (final Polygon p : tris) {
				for (int j = 0; j < p.xpoints.length; j++) {
					final Point firstPoint = new Point(p.xpoints[j], p.ypoints[j]);
					if (methods.calc.pointOnScreen(firstPoint)) {
						return firstPoint;
					} else {
						list.add(firstPoint);
					}
				}
			}
		} catch (final Exception ignored) {
		}
		return list.size() > 0 ? list.get(random(0, list.size())) : null;
	}

	/**
	 * Generates a rough central point. Performs the calculation by first
	 * generating a rough point, and then finding the point closest to the rough
	 * point that is actually on the RSModel.
	 *
	 * @return The rough central point.
	 */
	public Point getCentralPoint() {
		try {
			/* Add X and Y of all points, to get a rough central point */
			int x = 0, y = 0, total = 0;
			for (final Polygon poly : getTriangles()) {
				for (int i = 0; i < poly.npoints; i++) {
					x += poly.xpoints[i];
					y += poly.ypoints[i];
					total++;
				}
			}
			final Point central = new Point(x / total, y / total);
			/*
			 * Find a real point on the character that is closest to the central
			 * point
			 */
			Point curCentral = null;
			double dist = 20000;
			for (final Polygon poly : getTriangles()) {
				for (int i = 0; i < poly.npoints; i++) {
					final Point p = new Point(poly.xpoints[i], poly.ypoints[i]);
					if (!methods.calc.pointOnScreen(p)) {
						continue;
					}
					final double dist2 = methods.calc.distanceBetween(central, p);
					if (curCentral == null || dist2 < dist) {
						curCentral = p;
						dist = dist2;
					}
				}
			}
			return curCentral;
		} catch (final Exception ignored) {
		}
		return new Point(-1, -1);
	}

	/**
	 * Returns an array of triangles containing the screen points of this model.
	 *
	 * @return The on screen triangles of this model.
	 */
	public Polygon[] getTriangles() {
		update();
		final LinkedList<Polygon> polygons = new LinkedList<Polygon>();
		final int locX = getLocalX();
		final int locY = getLocalY();
		final int len = indices1.length;
		final int height = methods.calc.tileHeight(locX, locY);
		for (int i = 0; i < len; ++i) {
			final Point one = methods.calc.worldToScreen(locX + xPoints[indices1[i]],
					locY + zPoints[indices1[i]], height + yPoints[indices1[i]]);
			final Point two = methods.calc.worldToScreen(locX + xPoints[indices2[i]],
					locY + zPoints[indices2[i]], height + yPoints[indices2[i]]);
			final Point three = methods.calc.worldToScreen(locX
					+ xPoints[indices3[i]], locY + zPoints[indices3[i]], height
					+ yPoints[indices3[i]]);

			if (one.x >= 0 && two.x >= 0 && three.x >= 0) {
				polygons.add(new Polygon(new int[]{one.x, two.x, three.x},
						new int[]{one.y, two.y, three.y}, 3));
			}
		}
		return polygons.toArray(new Polygon[polygons.size()]);
	}

	/**
	 * Moves the mouse onto the RSModel.
	 */
	public void hover() {
		methods.mouse.move(getPoint());
	}

	/**
	 * Returns true if the provided object is an RSModel with the same x, y and
	 * z points as this model. This method compares all of the values in the
	 * three vertex arrays.
	 *
	 * @return <tt>true</tt> if the provided object is a model with the same
	 *         points as this.
	 */
	@Override
	public boolean equals(final Object o) {
		if (o instanceof RSModel) {
			final RSModel m = (RSModel) o;
			return Arrays.equals(indices1, m.indices1)
					&& Arrays.equals(xPoints, m.xPoints)
					&& Arrays.equals(yPoints, m.yPoints)
					&& Arrays.equals(zPoints, m.zPoints);
		}
		return false;
	}

	private Point getPointInRange(final int start, final int end) {
		final int locX = getLocalX();
		final int locY = getLocalY();
		final int height = methods.calc.tileHeight(locX, locY);
		for (int i = start; i < end; ++i) {
			final Point one = methods.calc.worldToScreen(locX + xPoints[indices1[i]],
					locY + zPoints[indices1[i]], height + yPoints[indices1[i]]);
			int x = -1, y = -1;
			if (one.x >= 0) {
				x = one.x;
				y = one.y;
			}
			final Point two = methods.calc.worldToScreen(locX + xPoints[indices2[i]],
					locY + zPoints[indices2[i]], height + yPoints[indices2[i]]);
			if (two.x >= 0) {
				if (x >= 0) {
					x = (x + two.x) / 2;
					y = (y + two.y) / 2;
				} else {
					x = two.x;
					y = two.y;
				}
			}
			final Point three = methods.calc.worldToScreen(locX
					+ xPoints[indices3[i]], locY + zPoints[indices3[i]], height
					+ yPoints[indices3[i]]);
			if (three.x >= 0) {
				if (x >= 0) {
					x = (x + three.x) / 2;
					y = (y + three.y) / 2;
				} else {
					x = three.x;
					y = three.y;
				}
			}
			if (x >= 0) {
				return new Point(x, y);
			}
		}
		return null;
	}
}