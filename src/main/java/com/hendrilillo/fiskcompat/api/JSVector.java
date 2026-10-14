package com.hendrilillo.fiskcompat.api;

/** Vectores inmutables que usan los scripts (JSVector2 / JSVector3 en Fisk). */
public final class JSVector {
    private JSVector() {}

    public static final class Vec3 {
        private final double x, y, z;

        public Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        public Vec2 xy() { return new Vec2(x, y); }
        public Vec2 xz() { return new Vec2(x, z); }
        public Vec2 yx() { return new Vec2(y, x); }
        public Vec2 yz() { return new Vec2(y, z); }
        public Vec2 zx() { return new Vec2(z, x); }
        public Vec2 zy() { return new Vec2(z, y); }
        public boolean equals(double x, double y, double z) { return this.x == x && this.y == y && this.z == z; }
        public boolean equals(Vec3 v) { return v != null && equals(v.x, v.y, v.z); }
        public Vec3 add(double x, double y, double z) { return new Vec3(this.x + x, this.y + y, this.z + z); }
        public Vec3 add(Vec3 v) { return add(v.x, v.y, v.z); }
        public Vec3 subtract(double x, double y, double z) { return new Vec3(this.x - x, this.y - y, this.z - z); }
        public Vec3 subtract(Vec3 v) { return subtract(v.x, v.y, v.z); }
        public Vec3 multiply(double x, double y, double z) { return new Vec3(this.x * x, this.y * y, this.z * z); }
        public Vec3 multiply(double d) { return multiply(d, d, d); }
        public Vec3 multiply(Vec3 v) { return multiply(v.x, v.y, v.z); }
        public double dot(Vec3 v) { return x * v.x + y * v.y + z * v.z; }
        public double length() { return Math.sqrt(x * x + y * y + z * z); }
        public Vec3 normalized() { double l = length(); return l < 1e-4 ? new Vec3(0, 0, 0) : multiply(1 / l); }
        public double squareDistanceTo(double x, double y, double z) { double a = this.x - x, b = this.y - y, c = this.z - z; return a * a + b * b + c * c; }
        public double squareDistanceTo(Vec3 v) { return squareDistanceTo(v.x, v.y, v.z); }
        public double distanceTo(double x, double y, double z) { return Math.sqrt(squareDistanceTo(x, y, z)); }
        public double distanceTo(Vec3 v) { return distanceTo(v.x, v.y, v.z); }
        @Override public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
    }

    public static final class Vec2 {
        private final double x, y;

        public Vec2(double x, double y) { this.x = x; this.y = y; }

        public double x() { return x; }
        public double y() { return y; }
        public boolean equals(double x, double y) { return this.x == x && this.y == y; }
        public boolean equals(Vec2 v) { return v != null && equals(v.x, v.y); }
        public Vec2 add(double x, double y) { return new Vec2(this.x + x, this.y + y); }
        public Vec2 add(Vec2 v) { return add(v.x, v.y); }
        public Vec2 multiply(double x, double y) { return new Vec2(this.x * x, this.y * y); }
        public Vec2 multiply(double d) { return multiply(d, d); }
        public Vec2 multiply(Vec2 v) { return multiply(v.x, v.y); }
        public double squareDistanceTo(double x, double y) { double a = this.x - x, b = this.y - y; return a * a + b * b; }
        public double squareDistanceTo(Vec2 v) { return squareDistanceTo(v.x, v.y); }
        public double distanceTo(double x, double y) { return Math.sqrt(squareDistanceTo(x, y)); }
        public double distanceTo(Vec2 v) { return distanceTo(v.x, v.y); }
        public double angleTo(double x, double y) { return Math.toDegrees(Math.atan2(y - this.y, x - this.x)); }
        public double angleTo(Vec2 v) { return angleTo(v.x, v.y); }
        public double length() { return Math.sqrt(x * x + y * y); }
        @Override public String toString() { return "(" + x + ", " + y + ")"; }
    }
}
