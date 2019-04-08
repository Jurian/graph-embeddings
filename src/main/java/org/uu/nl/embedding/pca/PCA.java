package org.uu.nl.embedding.pca;

import com.github.fommil.netlib.LAPACK;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.math.util.FastMath;
import org.netlib.util.intW;
import org.uu.nl.embedding.Settings;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.IntStream;

/**
 * <p>
 *  Principal component analysis (PCA) is a statistical procedure that uses an orthogonal transformation to convert a set
 *  of observations of possibly correlated variables (entities each of which takes on various numerical values) into a
 *  set of values of linearly uncorrelated variables called principal components.
 * </p>
 *
 * <p>
 *  This class is useful because it turns out that most of the time, the vast majority of principal components can
 *  be discarded while keeping 95% of variance.
 * </p>
 *
 * @author Jurian Baas
 */
public class PCA {

    static {
        // Tell the LAPACK logger to shut up
        final Logger logger = Logger.getLogger(LAPACK.class.getName());
        logger.setLevel(Level.OFF);
    }

    public static void main(String[] args) {

        double[] vectors = new double[] {0.709021285176277, 0.0989727054256946, 0.944028885336593, 0.131136209238321, 0.627006936818361, 0.284031824674457, 0.299683759687468, 0.704704837407917, 0.316494380356744, 0.316406882135198, 0.538645294960588, 0.180753013351932, 0.881154121831059, 0.0677755326032639, 0.391978225670755, 0.0379904755391181, 0.987172610359266, 0.762468789936975, 0.484379547648132, 0.787211643299088, 0.23481377819553, 0.098848425084725, 0.970515887485817, 0.147545246174559, 0.176420934265479, 0.529135998338461, 0.911328493384644, 0.979128144215792, 0.809106114786118, 0.960821788525209, 0.284524141578004, 0.360719811171293, 0.627841244451702, 0.230615806300193, 0.821070936974138, 0.967762595973909, 0.606781761394814, 0.931929770857096, 0.220125934574753, 0.049749348545447, 0.453613267978653, 0.12004346284084, 0.211779947625473, 0.766710783354938, 0.475467096082866, 0.435741536319256, 0.670769516611472, 0.00508560473099351, 0.319875477347523, 0.54805191908963, 0.548542238073424, 0.945220392663032, 0.543160151224583, 0.509918361436576, 0.107692875666544, 0.813819201430306, 0.76229839771986, 0.19540986744687, 0.493820405565202, 0.459453582298011, 0.50717785791494, 0.828551664017141, 0.411244156537578, 0.441769652301446, 0.956746453652158, 0.474191799294204, 0.920670317718759, 0.572604786138982, 0.73090164992027, 0.15609525074251, 0.694328867364675, 0.205678805941716, 0.550567029742524, 0.330725627951324, 0.503993728896603, 0.163677414646372, 0.625064524356276, 0.873371527297422, 0.839583723573014, 0.189996645087376, 0.650658410508186, 0.453810701379552, 0.236313452012837, 0.912110481178388, 0.326313535915688, 0.333147555589676, 0.524009252665564, 0.234035331057385, 0.991670460673049, 0.585701904492453};
        int dim = 3;

        PCA pca = new PCA(vectors, dim);
        Projection projection = pca.project(0.5);
        System.out.println(pca);
        System.out.println("Projection:\n" + projection);

    }

    private static final DecimalFormat df = new DecimalFormat("####0.0000");
    private static final Settings settings = Settings.getInstance();

    /**
     * Write a matrix as a pretty String
     * @param data The data of the matrix
     * @param nCols The number of columns of the matrix
     * @return Formatted String
     */
    private static String toStringMatrix(double[] data, int nCols) {

        StringBuilder out = new StringBuilder(data.length * 7 + nCols * 2);

        for(int i = 0; i < data.length; i ++) {
            out.append(" ").append(df.format(data[i]));
            if((i % nCols) == nCols-1) out.append("\n");
        }

        return out.toString();
    }

    /**
     * Create a String of a vector ordered to the eigen values
     * @param vector The vector to create a String version of
     * @return The vector but ordered to the eigen values
     */
    private String toStringOrderedVector(double[] vector) {
        StringBuilder out = new StringBuilder(vector.length * 7 );

        for(int i = 0; i < vector.length; i ++) {
            out.append(" ").append(df.format(vector[sortedIndices[i]]));
        }

        return out.toString();
    }

    /**
     * Contains the real and imaginary parts of the eigenvalues
     */
    private final double[] eigenValuesReal;
    private final double[] varPercentage;
    private final double[] sd;
    private final double[] data;
    private final double[] leftEigenVectors;
    private final int nRows, nCols, numThreads;
    private int[] sortedIndices;

    /**
     * Calculate all necessary prerequisites for projecting the input matrix into a lower dimensional space. Will create
     * a new matrix and not overwrite the input matrix.
     * @param vectors Input matrix
     * @param dim Number of columns of the input matrix
     */
    private PCA(double[] vectors, int dim) {
        this(vectors, dim, false);
    }

    /**
     * Calculate all necessary prerequisites for projecting the input matrix into a lower dimensional space
     * @param vectors Input matrix
     * @param dim Number of columns of the input matrix
     * @param inPlace Whether to overwrite the input matrix (true) or create a copy (false)
     */
    public PCA(double[] vectors, int dim, boolean inPlace)  {

        assert vectors.length != 0;
        assert vectors.length % dim == 0;

        this.numThreads = settings.threads();

        this.nCols = dim;
        this.nRows = vectors.length / dim;

        // Allocate space for the decomposition
        eigenValuesReal = new double[nCols];
        double[] eigenValuesImaginary = new double[nCols];
        sd = new double[nCols];
        varPercentage = new double[nCols];
        leftEigenVectors = new double[nCols * nCols];

        if(inPlace)
            this.data = vectors;
        else
            this.data = Arrays.copyOf(vectors, vectors.length);

        center(this.data, nCols);
        double[] covarianceMatrix = covariance();

        // Find the needed workspace
        double[] workSize = new double[1];
        intW info = new intW(0);
        int ld = FastMath.max(1, nCols);
        LAPACK.getInstance().dgeev("V", "N", nCols,
                new double[0], ld, new double[0], new double[0],
                new double[0], ld, new double[0], ld,
                workSize, -1, info);

        // Allocate workspace
        int workSpace;
        if (info.val != 0) {
            workSpace = 4 * nRows;
        } else
            workSpace = (int) workSize[0];

        workSpace = Math.max(1, workSpace);
        double[] work = new double[workSpace];

        info = new intW(0);
        ld = FastMath.max(1, nCols);
        LAPACK.getInstance().dgeev("V", "N", nCols,
                covarianceMatrix, ld, eigenValuesReal, eigenValuesImaginary,
                leftEigenVectors, ld, new double[0],
                ld, work, work.length, info);

        sortedIndices = IntStream.range(0, eigenValuesReal.length)
                .boxed().sorted(Comparator.comparingDouble(i -> -eigenValuesReal[i]))
                .mapToInt(i -> i).toArray();

        double sum = 0;
        for (double v : eigenValuesReal) sum += FastMath.abs(v);

        for(int i = 0; i < eigenValuesReal.length; i++) {
            varPercentage[i] = FastMath.abs(eigenValuesReal[i] / sum);
            sd[i] = FastMath.sqrt(FastMath.abs(eigenValuesReal[i]));
        }

    }

    @Override
    public String toString() {

        return
                "Variance as percentage:\n" + toStringOrderedVector(varPercentage) +
                "\n\nStandard deviation:\n" + toStringOrderedVector(sd) +
                "\n\nEigen-values:\n" + toStringOrderedVector(eigenValuesReal);
                //"\nEigen-vectors:\n" + toStringMatrix(leftEigenVectors, nCols);
    }

    /**
     * Project the data using the pre-calculated eigenvectors
     * @param minVariance Used to determine the number of principal components that have to be used
     * @return The projected matrix
     */
    public Projection project(double minVariance) {

        final int maxEigenCols = getCumulativeVarianceIndex(minVariance);

        final double[] projection = new double[nRows * maxEigenCols];
        final int nRowsData = data.length / nCols;
        final int nColsEigenVectors = nCols;

        // Note that LAPACK returns the eigenvectors in transposed fashion
        // So we have to keep that in mind while indexing
        final ExecutorService es = Executors.newWorkStealingPool(numThreads);
        final CompletionService<Void> cs = new ExecutorCompletionService<>(es);

        try(ProgressBar pb = settings.progressBar("Projecting", maxEigenCols, "columns")) {
            for (int c = 0; c < maxEigenCols; c++) {
                final int constC = c;
                cs.submit(() -> {
                    for (int r = 0; r < nRowsData; r++) {
                        for (int k = 0; k < nColsEigenVectors; k++) {
                            projection[constC + r * maxEigenCols] +=
                                    data[k + r * nCols] * leftEigenVectors[k + sortedIndices[constC] * nColsEigenVectors];
                        }
                    }
                    return null;
                });
            }

            int done = 0;
            while(done < maxEigenCols) {
                cs.take();
                done++;
                pb.step();
            }

        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            es.shutdown();
        }

        center(projection, maxEigenCols);

        return new Projection(projection, maxEigenCols);
    }

    /**
     * Calculate the number of columns (principal components) we need to include in the projection
     * in order to explain at least the minimum variance given as the argument
     * @param minVariance The minimum variance
     * @return A number ranging from 1 and the number of eigenvalues + 1
     */
    private int getCumulativeVarianceIndex(double minVariance) {
        double cumulativeVariance = 0;
        int i = 0;
        while(cumulativeVariance < minVariance) {
            cumulativeVariance += varPercentage[sortedIndices[i++]];
        }
        return i;
    }

    /**
     * Calculate the mean of every column
     * @return An array of means
     */
    private double[] colMeans(double[] data, int nCols) {
        double[] means = new double[nCols];
        for(int i = 0; i < data.length; i++) {
            int row = i / nCols, col = i % nCols;
            means[col] = (data[i] + row * means[col]) / (row + 1);
        }
        return means;
    }

    /**
     * Center the data around 0, will replace the data matrix with new values
     */
    private void center(double[] data, int nCols) {
        double[] means = colMeans(data, nCols);
        for(int i = 0; i < data.length; i++) {
            data[i] -= means[i % nCols];
        }
    }

    /**
     * Calculate the covariance of an input matrix
     * @return A new square matrix of size nCols*nCols
     */
    private double[] covariance() {

        double[] covMatrix = new double[nCols*nCols];

        // Note that at this point the data has been centered, which means that
        // we do not have to subtract the column means (as they are all 0)
        final ExecutorService es = Executors.newWorkStealingPool(numThreads);
        final CompletionService<Void> cs = new ExecutorCompletionService<>(es);

        try(ProgressBar pb = settings.progressBar("Covariance Matrix", nCols, "columns")) {

            for(int col1 = 0; col1 < nCols; col1++) {
                final int constCol1 = col1;
                cs.submit(() -> {
                    for(int col2 = 0; col2 < nCols; col2++) {

                        if(constCol1 < col2) continue;

                        double cov = 0;
                        int offset;
                        for(int i = 0; i < nRows; i++) {
                            offset = i * nCols;
                            cov += data[offset + constCol1] * data[offset + col2] ;
                        }
                        cov /= (nRows - 1);
                        covMatrix[constCol1 + col2 * nCols] = cov;
                        covMatrix[col2 + constCol1 * nCols] = cov;
                    }
                    return null;
                });
            }

            int done = 0;
            while(done < nCols) {
                cs.take();
                done++;
                pb.step();
            }

        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            es.shutdown();
        }


        return covMatrix;
    }

    /**
     * Wrapper for a projection, we need to store the data and also the number of columns
     */
    public class Projection {

        final double[] projection;
        final int nCols;

        private Projection(double[] projection, int nCols) {
            this.projection = projection;
            this.nCols = nCols;
        }

        @Override
        public String toString() {
            return PCA.toStringMatrix(projection, nCols);
        }

        public double[] getProjection() {
            return projection;
        }

        public int getnCols() {
            return nCols;
        }
    }


}
