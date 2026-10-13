package explosivearrows.client;

import java.util.Arrays;

import explosivearrows.ExplosiveArrows;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/**
 * Procedurally built, round meshes of the CBU-87 (SUU-65 dispenser) and the BLU-97 bomblet, true to scale in blocks
 * (1 block = 1 m). Main axis +Z = nose, origin in the middle of the length.
 * The texture rows/columns used here must match {@code scripts/generate-textures.py}.
 */
final class CbuModels {
	static final Identifier BOMB_TEXTURE = ExplosiveArrows.id("textures/entity/cbu87.png");
	static final Identifier BOMBLET_TEXTURE = ExplosiveArrows.id("textures/entity/blu97.png");

	/** Length 2.34 m, diameter 0.40 m. */
	static final float BOMB_LENGTH = 2.34F;
	static final float BOMB_RADIUS = 0.2F;
	static final float BOMBLET_LENGTH = 0.37F;
	static final float BOMBLET_RADIUS = 0.07F;

	static final Mesh BOMB = buildBomb(true);
	/** Bomb as it sits in the launcher: fins folded flush, no suspension lugs (they would stick through the tube). */
	static final Mesh BOMB_FOLDED = buildBomb(false);
	static final Mesh BOMBLET = buildBomblet();
	/** Inflated decelerator behind the bomblet, only while falling. */
	static final Mesh BALLUTE = buildBallute();
	static final Identifier LAUNCHER_TEXTURE = ExplosiveArrows.id("textures/entity/cbu87_launcher.png");
	/** Recoilless tube that the bomb (r = 0.20) fits into; length 2.6 m, muzzle at +Z. */
	static final float LAUNCHER_LENGTH = 2.6F;
	static final float SCOPE_X = 0.28F;
	static final float SCOPE_Y = 0.16F;
	static final float SCOPE_Z = 0.40F;
	static final Mesh LAUNCHER = buildLauncher();

	private static final int SEGMENTS = 32;

	private CbuModels() {
	}

	/** z coordinate of a point at distance {@code d} behind the nose tip. */
	private static float bombZ(float d) {
		return BOMB_LENGTH / 2.0F - d;
	}

	private static Mesh buildBomb(boolean fins) {
		// Profile from the fuze cap to the tail: {z, radius, texture row}; texture 128x128, body in columns 0..64
		float[][] front = {
				{bombZ(0.0F), 0.0F, 0.0F},
				{bombZ(0.0F), 0.065F, 3.0F},
				{bombZ(0.07F), 0.065F, 7.0F},
		};
		// Nose dome: quarter ellipse from r=0.09 to the full radius
		int domeSteps = 6;
		float[][] profile = new float[front.length + domeSteps + 1 + 4][];
		boolean[] sharp = new boolean[profile.length];
		int n = 0;
		for (float[] point : front) {
			sharp[n] = true;
			profile[n++] = point;
		}
		for (int i = 0; i <= domeSteps; i++) {
			double theta = Math.PI / 2.0 * i / domeSteps;
			float r = 0.09F + 0.11F * (float) Math.sin(theta);
			float d = 0.07F + 0.25F * (1.0F - (float) Math.cos(theta));
			profile[n++] = new float[] {bombZ(d), r, 8.0F + 14.0F * i / domeSteps};
		}
		// Cylinder up to 1.80 m, boat tail, tail section, rear cover
		profile[n++] = new float[] {bombZ(1.80F), BOMB_RADIUS, 95.0F};
		profile[n++] = new float[] {bombZ(2.12F), 0.115F, 111.0F};
		sharp[n] = true;
		profile[n++] = new float[] {bombZ(2.34F), 0.10F, 122.0F};
		sharp[n] = true;
		profile[n++] = new float[] {bombZ(2.34F), 0.0F, 128.0F};

		Mesh.Builder builder = new Mesh.Builder(128, 128).lathe(profile, sharp, SEGMENTS, 0.0F, 64.0F);
		if (!fins) {
			return builder.build();
		}

		// Four fins in X configuration, slightly canted so that the bomb spins
		float cant = (float) Math.toRadians(6.0);
		float chordCenter = bombZ(2.17F);
		// {radial distance, offset along the chord}, counter-clockwise
		float[][] fin = {
				{0.10F, bombZ(2.00F) - chordCenter},
				{0.10F, bombZ(2.34F) - chordCenter},
				{0.40F, bombZ(2.42F) - chordCenter},
				{0.40F, bombZ(2.10F) - chordCenter},
		};
		float[][] finUv = new float[4][];
		for (int i = 0; i < 4; i++) {
			finUv[i] = new float[] {64.0F + (fin[i][0] - 0.1F) / 0.3F * 64.0F, (0.21F - fin[i][1]) / 0.42F * 64.0F};
		}
		for (int i = 0; i < 4; i++) {
			double phi = Math.PI / 4.0 + Math.PI / 2.0 * i;
			Vector3f radial = new Vector3f((float) Math.cos(phi), (float) Math.sin(phi), 0.0F);
			Vector3f tangent = new Vector3f((float) -Math.sin(phi), (float) Math.cos(phi), 0.0F);
			Vector3f chord = new Vector3f(0.0F, 0.0F, (float) Math.cos(cant)).add(tangent.mul((float) Math.sin(cant)));
			builder.plate(new Vector3f(0.0F, 0.0F, chordCenter), radial, chord, 0.02F, fin, finUv, new float[] {64.5F, 0.5F});
		}

		// Suspension lugs on top (+Y), 14 inches apart
		for (float d : new float[] {0.95F, 1.31F}) {
			float z = bombZ(d);
			builder.box(new Vector3f(-0.03F, 0.19F, z - 0.035F), new Vector3f(0.03F, 0.235F, z + 0.035F), 64.0F, 64.0F, 80.0F, 80.0F);
		}
		return builder.build();
	}

	private static Mesh buildLauncher() {
		// Closed loop: muzzle face, outer tube, rear venturi flare, then the inner wall back to the muzzle.
		// Texture 128x128, tube in columns 0..64 (column 16 = top).
		float[][] profile = {
				{1.30F, 0.215F, 0.0F},
				{1.30F, 0.26F, 2.0F},
				{1.22F, 0.26F, 6.0F},
				{1.20F, 0.24F, 7.0F},
				{-0.95F, 0.24F, 100.0F},
				{-1.30F, 0.30F, 116.0F},
				{-1.30F, 0.275F, 117.0F},
				{-0.95F, 0.215F, 120.0F},
				{1.30F, 0.215F, 128.0F},
		};
		boolean[] sharp = new boolean[profile.length];
		Arrays.fill(sharp, true);
		Mesh.Builder builder = new Mesh.Builder(128, 128).lathe(profile, sharp, SEGMENTS, 0.0F, 64.0F);
		// Accessories; texture regions: grips 64..80, shoulder pad 80..96, scope 96..112, metal 112..128 (rows 0..16)
		builder.box(new Vector3f(-0.03F, -0.42F, 0.18F), new Vector3f(0.03F, -0.23F, 0.28F), 64, 0, 80, 16);
		builder.box(new Vector3f(-0.035F, -0.40F, -0.22F), new Vector3f(0.035F, -0.23F, -0.12F), 64, 0, 80, 16);
		builder.box(new Vector3f(-0.01F, -0.31F, -0.09F), new Vector3f(0.01F, -0.23F, -0.06F), 112, 0, 128, 16);
		builder.box(new Vector3f(-0.1F, -0.29F, -0.75F), new Vector3f(0.1F, -0.21F, -0.40F), 80, 0, 96, 16);
		// Round scope on the left (+X, towards the middle of the screen in first person) on a mount
		float[][] scope = {
				{0.20F, 0.0F, 0.0F},
				{0.20F, 0.055F, 2.0F},
				{0.12F, 0.055F, 4.0F},
				{0.09F, 0.04F, 5.0F},
				{-0.10F, 0.04F, 12.0F},
				{-0.13F, 0.05F, 13.0F},
				{-0.20F, 0.05F, 14.0F},
				{-0.20F, 0.0F, 16.0F},
		};
		boolean[] scopeSharp = {true, true, true, false, false, true, true, true};
		builder.lathe(scope, scopeSharp, 16, 96.0F, 112.0F, new Vector3f(SCOPE_X, SCOPE_Y, SCOPE_Z));
		builder.box(new Vector3f(0.19F, 0.06F, SCOPE_Z - 0.07F), new Vector3f(0.30F, 0.13F, SCOPE_Z + 0.07F), 112, 0, 128, 16);
		builder.box(new Vector3f(-0.012F, 0.23F, 1.04F), new Vector3f(0.012F, 0.31F, 1.08F), 112, 0, 128, 16);
		return builder.build();
	}

	private static Mesh buildBomblet() {
		// Texture 32x32, body in columns 0..16: standoff probe, body, rear cover
		float[][] profile = {
				{0.185F, 0.0F, 0.0F},
				{0.185F, 0.015F, 1.0F},
				{0.13F, 0.015F, 4.0F},
				{0.13F, 0.062F, 7.0F},
				{0.118F, BOMBLET_RADIUS, 8.0F},
				{-0.17F, BOMBLET_RADIUS, 29.0F},
				{-0.185F, 0.06F, 30.0F},
				{-0.185F, 0.0F, 32.0F},
		};
		boolean[] sharp = {true, true, true, true, true, true, true, true};
		return new Mesh.Builder(32, 32).lathe(profile, sharp, 16, 0.0F, 16.0F).build();
	}

	private static Mesh buildBallute() {
		// Columns 16..32 of the bomblet texture
		float[][] profile = {
				{-0.18F, 0.05F, 0.0F},
				{-0.26F, 0.12F, 12.0F},
				{-0.32F, 0.16F, 20.0F},
				{-0.30F, 0.10F, 26.0F},
				{-0.33F, 0.0F, 32.0F},
		};
		boolean[] sharp = {true, false, true, false, true};
		return new Mesh.Builder(32, 32).lathe(profile, sharp, 16, 16.0F, 32.0F).build();
	}
}
