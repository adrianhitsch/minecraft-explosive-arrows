package explosivearrows.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.joml.Vector3f;

/**
 * Static triangle-free mesh (quads; triangles are quads with a repeated vertex) that is built once and drawn with
 * {@code submitCustomGeometry}. Model units are blocks; the main axis is +Z (nose).
 */
final class Mesh {
	/** x, y, z, u, v, nx, ny, nz per vertex, 4 vertices per quad. */
	private static final int STRIDE = 8;

	private final float[] data;

	private Mesh(float[] data) {
		this.data = data;
	}

	void render(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
		for (int i = 0; i < data.length; i += STRIDE) {
			buffer.addVertex(pose, data[i], data[i + 1], data[i + 2])
					.setColor(color)
					.setUv(data[i + 3], data[i + 4])
					.setOverlay(overlay)
					.setLight(light)
					.setNormal(pose, data[i + 5], data[i + 6], data[i + 7]);
		}
	}

	static final class Builder {
		private final FloatArrayList data = new FloatArrayList();
		private final float textureWidth;
		private final float textureHeight;

		/** UVs passed to the builder are in texture pixels. */
		Builder(int textureWidth, int textureHeight) {
			this.textureWidth = textureWidth;
			this.textureHeight = textureHeight;
		}

		Mesh build() {
			return new Mesh(data.toFloatArray());
		}

		private void vertex(Vector3f pos, float u, float v, Vector3f normal) {
			data.add(pos.x);
			data.add(pos.y);
			data.add(pos.z);
			data.add(u / textureWidth);
			data.add(v / textureHeight);
			data.add(normal.x);
			data.add(normal.y);
			data.add(normal.z);
		}

		/**
		 * Rotationally symmetric surface around the Z axis (lathe). Each profile point is {z, radius, texture row};
		 * a radius of 0 closes the surface (caps). Points marked sharp get a hard edge, all others are shaded
		 * smoothly. The texture wraps once around the circumference between columns {@code u0} and {@code u1}.
		 */
		Builder lathe(float[][] profile, boolean[] sharp, int segments, float u0, float u1) {
			return lathe(profile, sharp, segments, u0, u1, new Vector3f());
		}

		/** Like {@link #lathe(float[][], boolean[], int, float, float)}, around an axis parallel to Z through {@code offset}. */
		Builder lathe(float[][] profile, boolean[] sharp, int segments, float u0, float u1, Vector3f offset) {
			int n = profile.length;
			// Outward normal of each profile segment in the (radius, z) plane
			float[][] segmentNormals = new float[n - 1][];
			for (int i = 0; i < n - 1; i++) {
				float dz = profile[i + 1][0] - profile[i][0];
				float dr = profile[i + 1][1] - profile[i][1];
				float length = (float) Math.sqrt(dz * dz + dr * dr);
				segmentNormals[i] = new float[] {-dz / length, dr / length};
			}
			for (int i = 0; i < n - 1; i++) {
				float[] start = pointNormal(segmentNormals, sharp, i, i);
				float[] end = pointNormal(segmentNormals, sharp, i, i + 1);
				for (int s = 0; s < segments; s++) {
					float a0 = (float) (2.0 * Math.PI * s / segments);
					float a1 = (float) (2.0 * Math.PI * (s + 1) / segments);
					float uA = u0 + (u1 - u0) * s / segments;
					float uB = u0 + (u1 - u0) * (s + 1) / segments;
					latheVertex(profile[i], start, a0, uA, offset);
					latheVertex(profile[i + 1], end, a0, uA, offset);
					latheVertex(profile[i + 1], end, a1, uB, offset);
					latheVertex(profile[i], start, a1, uB, offset);
				}
			}
			return this;
		}

		/** Normal of segment {@code segment} at profile point {@code point} (averaged with the neighbour unless sharp). */
		private static float[] pointNormal(float[][] segmentNormals, boolean[] sharp, int segment, int point) {
			int other = point == segment ? segment - 1 : segment + 1;
			if (sharp[point] || other < 0 || other >= segmentNormals.length) {
				return segmentNormals[segment];
			}
			float r = segmentNormals[segment][0] + segmentNormals[other][0];
			float z = segmentNormals[segment][1] + segmentNormals[other][1];
			float length = (float) Math.sqrt(r * r + z * z);
			return new float[] {r / length, z / length};
		}

		private void latheVertex(float[] point, float[] normal, float angle, float u, Vector3f offset) {
			float cos = (float) Math.cos(angle);
			float sin = (float) Math.sin(angle);
			vertex(new Vector3f(point[1] * cos, point[1] * sin, point[0]).add(offset), u, point[2], new Vector3f(normal[0] * cos, normal[0] * sin, normal[1]));
		}

		/**
		 * Flat plate with thickness, e.g. a fin. {@code corners} are four points (counter-clockwise) in the plate's
		 * own 2D coordinates (a, b), which map to {@code origin + a * axisA + b * axisB}; uv[i] are their texture
		 * pixels. The narrow edges get the texture pixel {@code edgeUv}.
		 */
		Builder plate(Vector3f origin, Vector3f axisA, Vector3f axisB, float thickness, float[][] corners, float[][] uv, float[] edgeUv) {
			Vector3f normal = new Vector3f(axisA).cross(axisB).normalize();
			Vector3f offset = new Vector3f(normal).mul(thickness / 2.0F);
			Vector3f[] top = new Vector3f[4];
			Vector3f[] bottom = new Vector3f[4];
			for (int i = 0; i < 4; i++) {
				Vector3f p = new Vector3f(origin).add(new Vector3f(axisA).mul(corners[i][0])).add(new Vector3f(axisB).mul(corners[i][1]));
				top[i] = new Vector3f(p).add(offset);
				bottom[i] = new Vector3f(p).sub(offset);
			}
			Vector3f inverted = new Vector3f(normal).negate();
			for (int i = 0; i < 4; i++) {
				vertex(top[i], uv[i][0], uv[i][1], normal);
			}
			for (int i = 3; i >= 0; i--) {
				vertex(bottom[i], uv[i][0], uv[i][1], inverted);
			}
			for (int i = 0; i < 4; i++) {
				int j = (i + 1) % 4;
				Vector3f edge = new Vector3f(top[j]).sub(top[i]);
				Vector3f edgeNormal = edge.cross(normal, new Vector3f()).normalize();
				vertex(top[i], edgeUv[0], edgeUv[1], edgeNormal);
				vertex(bottom[i], edgeUv[0], edgeUv[1], edgeNormal);
				vertex(bottom[j], edgeUv[0], edgeUv[1], edgeNormal);
				vertex(top[j], edgeUv[0], edgeUv[1], edgeNormal);
			}
			return this;
		}

		/** Axis-aligned box from {@code min} to {@code max}, every face uses the texture rectangle u0/v0..u1/v1. */
		Builder box(Vector3f min, Vector3f max, float u0, float v0, float u1, float v1) {
			// A plate in the X/Z plane with thickness Y gives top, bottom and the four sides
			float sizeX = max.x - min.x;
			float sizeZ = max.z - min.z;
			float[][] corners = {{0, 0}, {sizeX, 0}, {sizeX, sizeZ}, {0, sizeZ}};
			float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
			Vector3f origin = new Vector3f(min.x, (min.y + max.y) / 2.0F, min.z);
			return plate(origin, new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), max.y - min.y, corners, uv, new float[] {u0, v0});
		}
	}
}
