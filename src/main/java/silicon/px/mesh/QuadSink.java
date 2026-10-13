package silicon.px.mesh;

@FunctionalInterface
public interface QuadSink {
	void quad(int face, int layer, int u, int v, int w, int h, int id);
}
