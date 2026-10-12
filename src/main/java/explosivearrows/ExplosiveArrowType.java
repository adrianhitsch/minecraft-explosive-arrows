package explosivearrows;

public enum ExplosiveArrowType {
	SMALL(4.0f, false),
	NORMAL(8.6f, false),
	GOD(35.0f, true),
	DOOM(80.0f, true);

	private final float explosionPower;
	private final boolean createsFire;

	ExplosiveArrowType(float explosionPower, boolean createsFire) {
		this.explosionPower = explosionPower;
		this.createsFire = createsFire;
	}

	public float explosionPower() {
		return explosionPower;
	}

	public boolean createsFire() {
		return createsFire;
	}
}
