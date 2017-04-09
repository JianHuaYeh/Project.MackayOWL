package edu.saic.mackay;

public class BiCluster {
    private String id;
    private double[] vec;
    private BiCluster left;
    private BiCluster right;

    public BiCluster(String id, double[] vec, BiCluster left, BiCluster right) {
        this.id = id;
        this.vec = vec;
        this.left = left;
        this.right = right;
    }

    public BiCluster(String id, double[] vec) {
        this(id, vec, null, null);
    }

    public void setId(String id) { this.id = id; }
    public double[] getVec() { return vec; }
    public String getId() { return id; }
    public BiCluster getLeftNode() { return this.left; }
    public BiCluster getRightNode() { return this.right; }
    public String toString() { return this.getId(); }
}
