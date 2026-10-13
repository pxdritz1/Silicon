package silicon.px.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Catches the vertices the active block renderer (vanilla or Indigo) writes for one block,
 * so lighting and tint come out exactly as that renderer would draw them.
 */
final class QuadRecorder implements VertexConsumer {
	private static final int MAX = 256;

	final float[] x = new float[MAX], y = new float[MAX], z = new float[MAX];
	final float[] u = new float[MAX], v = new float[MAX];
	final float[] nx = new float[MAX], ny = new float[MAX], nz = new float[MAX];
	final int[] color = new int[MAX], overlay = new int[MAX], light = new int[MAX];
	int count = -1;
	boolean overflow;

	void reset() {
		count = -1;
		overflow = false;
	}

	int quads() {
		return (count + 1) / 4;
	}

	void replay(VertexConsumer out, int quad) {
		for (int i = quad * 4; i < quad * 4 + 4; i++)
			out.addVertex(x[i], y[i], z[i], color[i], u[i], v[i], overlay[i], light[i], nx[i], ny[i], nz[i]);
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		if (count < MAX - 1)
			count++;
		else
			overflow = true;
		this.x[count] = x;
		this.y[count] = y;
		this.z[count] = z;
		color[count] = -1;
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		color[count] = (a << 24) | (r << 16) | (g << 8) | b;
		return this;
	}

	@Override
	public VertexConsumer setColor(int argb) {
		color[count] = argb;
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		this.u[count] = u;
		this.v[count] = v;
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		overlay[count] = (v << 16) | (u & 0xFFFF);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		light[count] = (v << 16) | (u & 0xFFFF);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		nx[count] = x;
		ny[count] = y;
		nz[count] = z;
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		return this;
	}
}
