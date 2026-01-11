package com.github.epivitae.fia;

import ij.IJ;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;

/**
 * Encapsulation of the original Image Stabilizer algorithm by Kang Li.
 * Source: Image_Stabilizer.java (2008-2009)
 * UPDATE v2.3.1: Fixed invert scope & Added Warp Debugging
 */
public class LegacyAligner {

    public static final int TRANSLATION = 0;
    public static final int AFFINE = 1;

    /**
     * Calculate transformation matrix
     */
    public static double[][] estimate(ImageProcessor ip, ImageProcessor ipRef, int transformType, 
                                      int pyramidLevel, int maxIter, double tol) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        ImageProcessor ipFloat = ip.convertToFloat();
        ImageProcessor ipFloatRef = (ipRef instanceof FloatProcessor) ? ipRef : ipRef.convertToFloat();

        ImageProcessor[] ipPyramid = new ImageProcessor[5];
        ImageProcessor[] ipRefPyramid = new ImageProcessor[5];
        
        buildPyramid(ipFloat, ipPyramid, width, height, pyramidLevel);
        buildPyramid(ipFloatRef, ipRefPyramid, width, height, pyramidLevel);

        double[][] wp;
        if (transformType == TRANSLATION) {
            wp = new double[][]{{0.0}, {0.0}}; 
            wp = estimateTranslation(wp, ipPyramid, ipRefPyramid, maxIter, tol, pyramidLevel);
        } else {
            wp = new double[][]{{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}}; 
            wp = estimateAffine(wp, ipPyramid, ipRefPyramid, maxIter, tol, pyramidLevel);
        }
        return wp;
    }

    /**
     * Apply transformation to create a new aligned image
     */
    public static ImageProcessor warp(ImageProcessor ip, double[][] wp, int transformType) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        
        // [DEBUG] Log the matrix application to confirm it's running
        if (wp != null && transformType == TRANSLATION) {
            // IJ.log(String.format("[DEBUG] Warping with dx=%.2f, dy=%.2f", wp[0][0], wp[1][0]));
        }
        
        // Use Float for high-quality interpolation
        ImageProcessor ipFloatIn = ip.convertToFloat();
        
        if (ip instanceof ColorProcessor) {
            ColorProcessor ipOut = new ColorProcessor(width, height);
            if (transformType == TRANSLATION) warpColorTranslation(ipOut, (ColorProcessor)ip, wp);
            else warpColorAffine(ipOut, (ColorProcessor)ip, wp);
            return ipOut;
        } else {
            FloatProcessor ipFloatOut = new FloatProcessor(width, height);
            
            if (transformType == TRANSLATION) warpTranslation(ipFloatOut, ipFloatIn, wp);
            else warpAffine(ipFloatOut, ipFloatIn, wp);

            // Convert back to original type
            if (ip instanceof ByteProcessor) return ipFloatOut.convertToByte(false);
            if (ip instanceof ShortProcessor) return ipFloatOut.convertToShort(false);
            return ipFloatOut; 
        }
    }

    // --- Private Helpers ---

    private static void buildPyramid(ImageProcessor ip, ImageProcessor[] pyramid, int width, int height, int maxLevel) {
        pyramid[0] = ip; 
        if (maxLevel >= 1 && width >= 100 && height >= 100) {
            pyramid[1] = resize(ip, width/2, height/2);
            if (maxLevel >= 2 && width >= 200 && height >= 200) {
                pyramid[2] = resize(ip, width/4, height/4);
                if (maxLevel >= 3 && width >= 400 && height >= 400) {
                    pyramid[3] = resize(ip, width/8, height/8);
                    if (maxLevel >= 4 && width >= 800 && height >= 800) {
                        pyramid[4] = resize(ip, width/16, height/16);
                    }
                }
            }
        }
    }
    
    private static ImageProcessor resize(ImageProcessor ip, int w, int h) {
        ImageProcessor out = new FloatProcessor(w, h);
        resizeLogic(out, ip);
        return out;
    }

    private static void resizeLogic(ImageProcessor ipOut, ImageProcessor ip) {
        int widthOut = ipOut.getWidth();
        int heightOut = ipOut.getHeight();
        double xScale = ip.getWidth() / (double)widthOut;
        double yScale = ip.getHeight() / (double)heightOut;
        float[] pixelsOut = (float[])ipOut.getPixels();
        for (int i = 0, y = 0; y < heightOut; ++y) {
            double ys = y * yScale;
            for (int x = 0; x < widthOut; ++x) {
                pixelsOut[i++] = (float)ip.getInterpolatedPixel(x * xScale, ys);
            }
        }
    }

    private static double[][] estimateTranslation(double[][] wp, ImageProcessor[] ipPyramid, ImageProcessor[] ipRefPyramid, 
                                                  int maxIter, double tol, int maxLevel) {
        for(int i=maxLevel; i>=1; i--) {
            if (ipPyramid[i] != null && ipRefPyramid[i] != null) {
                ImageProcessor g1 = new FloatProcessor(ipPyramid[i].getWidth(), ipPyramid[i].getHeight());
                ImageProcessor g2 = new FloatProcessor(ipRefPyramid[i].getWidth(), ipRefPyramid[i].getHeight());
                gradient(g1, ipPyramid[i]);
                gradient(g2, ipRefPyramid[i]);
                
                wp = estimateTranslationCore(wp, g1, g2, maxIter, tol);
                wp[0][0] *= 2; wp[1][0] *= 2; 
            }
        }
        ImageProcessor g1 = new FloatProcessor(ipPyramid[0].getWidth(), ipPyramid[0].getHeight());
        ImageProcessor g2 = new FloatProcessor(ipRefPyramid[0].getWidth(), ipRefPyramid[0].getHeight());
        gradient(g1, ipPyramid[0]);
        gradient(g2, ipRefPyramid[0]);
        return estimateTranslationCore(wp, g1, g2, maxIter, tol);
    }

    private static double[][] estimateAffine(double[][] wp, ImageProcessor[] ipPyramid, ImageProcessor[] ipRefPyramid, 
                                             int maxIter, double tol, int maxLevel) {
        for(int i=maxLevel; i>=1; i--) {
            if (ipPyramid[i] != null && ipRefPyramid[i] != null) {
                ImageProcessor g1 = new FloatProcessor(ipPyramid[i].getWidth(), ipPyramid[i].getHeight());
                ImageProcessor g2 = new FloatProcessor(ipRefPyramid[i].getWidth(), ipRefPyramid[i].getHeight());
                gradient(g1, ipPyramid[i]);
                gradient(g2, ipRefPyramid[i]);
                
                wp = estimateAffineCore(wp, g1, g2, maxIter, tol);
                wp[0][2] *= 2; wp[1][2] *= 2;
            }
        }
        ImageProcessor g1 = new FloatProcessor(ipPyramid[0].getWidth(), ipPyramid[0].getHeight());
        ImageProcessor g2 = new FloatProcessor(ipRefPyramid[0].getWidth(), ipRefPyramid[0].getHeight());
        gradient(g1, ipPyramid[0]);
        gradient(g2, ipRefPyramid[0]);
        return estimateAffineCore(wp, g1, g2, maxIter, tol);
    }

    private static double[][] estimateAffineCore(double[][] wp, ImageProcessor ip, ImageProcessor ipRef, int maxIter, double tol) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        float[] jx = new float[width * height];
        float[] jy = new float[width * height];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                jx[y * width + x] = (float)x;
                jy[y * width + x] = (float)y;
            }
        }
        float[][] sd = new float[6][];
        sd[4] = dx(ipRef);
        sd[5] = dy(ipRef);
        sd[0] = dot(sd[4], jx);
        sd[1] = dot(sd[5], jx);
        sd[2] = dot(sd[4], jy);
        sd[3] = dot(sd[5], jy);

        ImageProcessor ipOut = ip.duplicate();
        double[] dp = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        double[][] bestWp = new double[2][3];
        for(int i=0;i<2;i++) System.arraycopy(wp[i], 0, bestWp[i], 0, 3);

        double[][] d = { {1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0} };
        double[][] w = { {1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0} };
        double[][] h = new double[6][6];

        for (int y = 0; y < 6; ++y)
            for (int x = 0; x < 6; ++x) h[y][x] = dotSum(sd[x], sd[y]);
        h = invert(h);

        double oldRmse = Double.MAX_VALUE;
        double minRmse = Double.MAX_VALUE;

        for (int iter = 0; iter < maxIter; ++iter) {
            warpAffine(ipOut, ip, wp);
            subtract(ipOut, ipRef);
            double rmse = rootMeanSquare(ipOut);
            if (iter > 0) {
                if (rmse < minRmse) {
                    for(int i=0;i<2;i++) System.arraycopy(wp[i], 0, bestWp[i], 0, 3);
                    minRmse = rmse;
                }
                if (Math.abs((oldRmse - rmse) / (oldRmse + Double.MIN_VALUE)) < tol) break;
            }
            oldRmse = rmse;
            float[] error = (float[])ipOut.getPixels();
            for(int i=0;i<6;i++) dp[i] = dotSum(sd[i], error);
            dp = prod(h, dp);

            d[0][0] = dp[0] + 1.0; d[0][1] = dp[2]; d[0][2] = dp[4];
            d[1][0] = dp[1]; d[1][1] = dp[3] + 1.0; d[1][2] = dp[5];
            d[2][0] = 0.0; d[2][1] = 0.0; d[2][2] = 1.0;

            w[0][0] = wp[0][0] + 1.0; w[0][1] = wp[0][1]; w[0][2] = wp[0][2];
            w[1][0] = wp[1][0]; w[1][1] = wp[1][1] + 1.0; w[1][2] = wp[1][2];
            w[2][0] = 0.0; w[2][1] = 0.0; w[2][2] = 1.0;

            w = prod(w, invert(d));
            wp[0][0] = w[0][0] - 1.0; wp[0][1] = w[0][1]; wp[0][2] = w[0][2];
            wp[1][0] = w[1][0]; wp[1][1] = w[1][1] - 1.0; wp[1][2] = w[1][2];
        }
        return bestWp;
    }

    private static double[][] estimateTranslationCore(double[][] wp, ImageProcessor ip, ImageProcessor ipRef, int maxIter, double tol) {
        float[] dxRef = dx(ipRef);
        float[] dyRef = dy(ipRef);
        ImageProcessor ipOut = ip.duplicate();
        double[] dp = { 0.0, 0.0 };
        double[][] bestWp = new double[2][1];
        bestWp[0][0] = wp[0][0]; bestWp[1][0] = wp[1][0];

        double[][] d = { {1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0} };
        double[][] w = { {1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0} };
        double[][] h = new double[2][2];

        h[0][0] = dotSum(dxRef, dxRef); h[1][0] = dotSum(dxRef, dyRef);
        h[0][1] = dotSum(dyRef, dxRef); h[1][1] = dotSum(dyRef, dyRef);
        h = invert(h);

        double oldRmse = Double.MAX_VALUE;
        double minRmse = Double.MAX_VALUE;

        for (int iter = 0; iter < maxIter; ++iter) {
            warpTranslation(ipOut, ip, wp);
            subtract(ipOut, ipRef);
            double rmse = rootMeanSquare(ipOut);
            if (iter > 0) {
                if (rmse < minRmse) {
                    bestWp[0][0] = wp[0][0]; bestWp[1][0] = wp[1][0];
                    minRmse = rmse;
                }
                if (Math.abs((oldRmse - rmse) / (oldRmse + Double.MIN_VALUE)) < tol) break;
            }
            oldRmse = rmse;
            float[] error = (float[])ipOut.getPixels();
            dp[0] = dotSum(dxRef, error); dp[1] = dotSum(dyRef, error);
            dp = prod(h, dp);

            d[0][0] = 1.0; d[0][1] = 0.0; d[0][2] = dp[0];
            d[1][0] = 0.0; d[1][1] = 1.0; d[1][2] = dp[1];
            d[2][0] = 0.0; d[2][1] = 0.0; d[2][2] = 1.0;

            w[0][0] = 1.0; w[0][1] = 0.0; w[0][2] = wp[0][0];
            w[1][0] = 0.0; w[1][1] = 1.0; w[1][2] = wp[1][0];
            w[2][0] = 0.0; w[2][1] = 0.0; w[2][2] = 1.0;

            w = prod(w, invert(d));
            wp[0][0] = w[0][2]; wp[1][0] = w[1][2];
        }
        return bestWp;
    }

    // --- Core Math ---
    private static void gradient(ImageProcessor ipOut, ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        float[] pixels = (float[])ip.getPixels();
        float[] outPixels = (float[])ipOut.getPixels();
        for (int y = 1; y + 1 < height; ++y) {
            int offset = 1 + y * width;
            double p1 = 0f; double p2 = pixels[offset - width - 1]; double p3 = pixels[offset - width]; double p4 = 0f; double p5 = pixels[offset - 1]; double p6 = pixels[offset]; double p7 = 0f; double p8 = pixels[offset + width - 1]; double p9 = pixels[offset + width];     
            for (int x = 1; x + 1 < width; ++x) {
                p1 = p2; p2 = p3; p3 = pixels[offset - width + 1];
                p4 = p5; p5 = p6; p6 = pixels[offset + 1];
                p7 = p8; p8 = p9; p9 = pixels[offset + width + 1];
                double a = p1 + 2 * p2 + p3 - p7 - 2 * p8 - p9;
                double b = p1 + 2 * p4 + p7 - p3 - 2 * p6 - p9;
                outPixels[offset++] = (float)Math.sqrt(a * a + b * b);
            }
        }
    }

    private static void subtract(ImageProcessor ipOut, ImageProcessor ip) {
        float[] pixels = (float[])ip.getPixels();
        float[] outPixels = (float[])ipOut.getPixels();
        for (int i = 0; i < pixels.length; ++i) outPixels[i] = outPixels[i] - pixels[i];
    }

    private static double rootMeanSquare(ImageProcessor ip) {
        double mean = 0.0;
        float[] pixels = (float[])ip.getPixels();
        for (int i = 0; i < pixels.length; ++i) mean += pixels[i] * pixels[i];
        mean /= pixels.length;
        return Math.sqrt(mean);
    }

    private static void gaussian(double a[][], int index[]) {
        int n = index.length;
        double[] c = new double[n];
        for (int i = 0; i < n; ++i) index[i] = i;
        for (int i = 0; i < n; ++i) {
            double c1 = 0;
            for (int j = 0; j < n; ++j) {
                double c0 = Math.abs(a[i][j]);
                if (c0 > c1) c1 = c0;
            }
            c[i] = c1;
        }
        int k = 0;
        for (int j = 0; j < n-1; ++j) {
            double pi1 = 0;
            for (int i = j; i < n; ++i) {
                double pi0 = Math.abs(a[index[i]][j]);
                pi0 /= c[index[i]];
                if (pi0 > pi1) { pi1 = pi0; k = i; }
            }
            int itmp = index[j]; index[j] = index[k]; index[k] = itmp;
            for (int i= j + 1; i < n; ++i) {
                double pj = a[index[i]][j] / a[index[j]][j];
                a[index[i]][j] = pj;
                for (int l = j + 1; l < n; ++l) a[index[i]][l] -= pj * a[index[j]][l];
            }
        }
    }

    // [Fix] Corrected Scope for 'invert'
    private static double[][] invert(double a[][]) {
        int n = a.length;
        double[][] x = new double[n][n];
        double[][] b = new double[n][n];
        int index[] = new int[n];
        for (int i = 0; i < n; ++i) b[i][i] = 1;
        gaussian(a, index);
        for (int i = 0; i < n - 1; ++i)
            for (int j = i + 1; j < n; ++j)
                for (int k = 0; k < n; ++k) b[index[j]][k] -= a[index[j]][i] * b[index[i]][k];
        for (int i = 0; i < n; ++i) {
            x[n - 1][i] = b[index[n - 1]][i] / a[index[n - 1]][n - 1];
            for (int j = n - 2; j >= 0; --j) {
                x[j][i] = b[index[j]][i];
                for (int k = j + 1; k < n; ++k) x[j][i] -= a[index[j]][k] * x[k][i];
                x[j][i] /= a[index[j]][j];
            }
        }
        return x;
    }

    private static float[] dx(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        float[] pixels = (float[])ip.getPixels();
        float[] outPixels = new float[width * height];
        for (int y = 0; y < height; ++y) {
            outPixels[y * width] = (float)(pixels[y * width + 1] - pixels[y * width]);
            outPixels[y * width + width - 1] = (float)(pixels[y * width + width - 1] - pixels[y * width + width - 2]);
            for (int x = 1; x + 1 < width; ++x) {
                outPixels[y * width + x] = (float)((pixels[y * width + x + 1] - pixels[y * width + x - 1]) * 0.5);
            }
        }
        return outPixels;
    }

    private static float[] dy(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        float[] pixels = (float[])ip.getPixels();
        float[] outPixels = new float[width * height];
        for (int x = 0; x < (int)width; ++x) {
            outPixels[x] = (float)(pixels[width + x] - pixels[x]);
            outPixels[(height - 1) * width + x] = (float)(pixels[width * (height - 1) + x] - pixels[width * (height - 2) + x]);
            for (int y = 1; y + 1 < (int)height; ++y) {
                outPixels[y * width + x] = (float)((pixels[width * (y + 1) + x] - pixels[width * (y - 1) + x]) * 0.5);
            }
        }
        return outPixels;
    }

    private static float[] dot(float[] p1, float[] p2) {
        int n = p1.length < p2.length ? p1.length : p2.length;
        float[] output = new float[n];
        for (int i = 0; i < n; ++i) output[i] = p1[i] * p2[i];
        return output;
    }

    private static double dotSum(float[] p1, float[] p2) {
        double sum = 0.0;
        int n = p1.length < p2.length ? p1.length : p2.length;
        for (int i = 0; i < n; ++i) sum += p1[i] * p2[i];
        return sum;
    }

    private static double[] prod(double[][] m, double[] v) {
        int n = v.length;
        double[] out = new double[n];
        for (int j = 0; j < n; ++j) {
            out[j] = 0.0;
            for (int i = 0; i < n; ++i) out[j] = out[j] + m[j][i] * v[i];
        }
        return out;
    }

    private static double[][] prod(double[][] a, double[][] b) {
        double[][] out = new double[a.length][b[0].length];
        for (int i = 0; i < a.length; ++i) {
            for (int j = 0; j < b[i].length; ++j) {
                out[i][j] = 0.0;
                for (int k = 0; k < a[i].length; ++k) out[i][j] = out[i][j] + a[i][k] * b[k][j];
            }
        }
        return out;
    }

    // --- Warping Functions ---
    private static void warpAffine(ImageProcessor ipOut, ImageProcessor ip, double[][] wp) {
        float[] outPixels = (float[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double xx = (1.0 + wp[0][0]) * x + wp[0][1] * y + wp[0][2];
                double yy = wp[1][0] * x + (1.0 + wp[1][1]) * y + wp[1][2];
                outPixels[p] = (float)ip.getInterpolatedPixel(xx, yy);
                ++p;
            }
        }
    }

    private static void warpColorAffine(ImageProcessor ipOut, ColorProcessor ip, double[][] wp) {
        int[] outPixels = (int[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double xx = (1.0 + wp[0][0]) * x + wp[0][1] * y + wp[0][2];
                double yy = wp[1][0] * x + (1.0 + wp[1][1]) * y + wp[1][2];
                outPixels[p] = (int)ip.getInterpolatedRGBPixel(xx, yy);
                ++p;
            }
        }
    }

    private static void warpTranslation(ImageProcessor ipOut, ImageProcessor ip, double[][] wp) {
        float[] outPixels = (float[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double xx = x + wp[0][0];
                double yy = y + wp[1][0];
                outPixels[p] = (float)ip.getInterpolatedPixel(xx, yy);
                ++p;
            }
        }
    }

    private static void warpColorTranslation(ImageProcessor ipOut, ColorProcessor ip, double[][] wp) {
        int[] outPixels = (int[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                outPixels[p] = (int)ip.getInterpolatedRGBPixel(x + wp[0][0], y + wp[1][0]);
                ++p;
            }
        }
    }
}