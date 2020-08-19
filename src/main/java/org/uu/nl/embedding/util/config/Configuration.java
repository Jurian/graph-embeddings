package org.uu.nl.embedding.util.config;

import info.debatty.java.stringsimilarity.JaroWinkler;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import info.debatty.java.stringsimilarity.interfaces.StringSimilarity;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

import org.uu.nl.embedding.bca.jobs.ContextWinnowedUndirectedWeighted;
import org.uu.nl.embedding.bca.jobs.DirectedWeighted;
import org.uu.nl.embedding.bca.jobs.HybridWeighted;
import org.uu.nl.embedding.bca.jobs.KaleUndirectedWeighted;
import org.uu.nl.embedding.bca.jobs.KaleUndirectedWeightedNodeBased;
import org.uu.nl.embedding.bca.jobs.KaleUndirectedWeightedSeperated;
import org.uu.nl.embedding.bca.jobs.UndirectedWeighted;
import org.uu.nl.embedding.util.rnd.ExtendedRandom;
import org.uu.nl.embedding.util.rnd.ThreadLocalSeededRandom;
import org.uu.nl.embedding.util.similarity.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Configuration {

    public enum EmbeddingMethod {
        GLOVE, PGLOVE
    }

    public enum OptimizationMethod {
        ADAGRAD, AMSGRAD, ADAM
    }

    public enum SimilarityMethod {
        NGRAM_COSINE, NGRAM_JACCARD, TOKEN_COSINE, TOKEN_JACCARD, JAROWINKLER, LEVENSHTEIN, NUMERIC, DATE_DAYS, DATE_MONTHS, DATE_YEARS
    }

    public enum BCANormalization {
        NONE, UNITY, COUNTS
    }

    public enum BCAType {
        DIRECTED, UNDIRECTED, HYBRID, KALEUNDIRECTED, KALESEPERATED, KALENODEBASED, CONTEXTWINNOWED
    }

    private String graph;

    public File getGraphFile() {
        return Paths.get("").toAbsolutePath().resolve(graph).toFile();
    }

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    private String method;

    public String getMethod() {
        return  method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    private int dim;

    public int getDim() {
        return dim;
    }

    public void setDim(int dim) {
        this.dim = dim;
    }

    private int threads;

    public int getThreads() {
        return threads == 0 ? (Runtime.getRuntime().availableProcessors() -1) : threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public EmbeddingMethod getMethodEnum() {
        return EmbeddingMethod.valueOf(this.method.toUpperCase());
    }

    private Map<String, Float> weights;

    public boolean usingWeights() {
        return weights != null && !weights.isEmpty();
    }

    public Map<String, Float> getWeights() {
        return weights;
    }

    public void setWeights(Map<String, Float> weights) {
        this.weights = weights;
    }

    private List<SimilarityGroup> similarity;

    public List<SimilarityGroup> getSimilarity() {
        return similarity;
    }

    public boolean usingSimilarity() {
        return similarity != null && !similarity.isEmpty();
    }

    public void setSimilarity(List<SimilarityGroup> similarity) {
        this.similarity = similarity;
    }

    private BCA bca;

    public BCA getBca() {
        return bca;
    }

    public void setBca(BCA bca) {
        this.bca = bca;
    }

    private Opt opt;

    public Opt getOpt() {
        return opt;
    }

    public void setOpt(Opt opt) {
        this.opt = opt;
    }

    private PCA pca;

    public boolean usingPca(){
        return this.pca != null;
    }

    public PCA getPca() {
        return pca;
    }

    public void setPca(PCA pca) {
        this.pca = pca;
    }

    private Output output;

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    private static ThreadLocalSeededRandom threadLocalRandom;

    public static void setThreadLocalRandom() {
        threadLocalRandom = new ThreadLocalSeededRandom(System.currentTimeMillis());
    }

    public static void setThreadLocalRandom(long seed) {
        threadLocalRandom = new ThreadLocalSeededRandom(seed);
    }

    public static ExtendedRandom getThreadLocalRandom() {
        return threadLocalRandom.get();
    }

    public static ProgressBar progressBar(String name, long max, String unitName) {
        return new ProgressBar (
                name,
                max,
                250,
                System.out,
                ProgressBarStyle.COLORFUL_UNICODE_BLOCK,
                " " + unitName,
                1,
                false
        );
    }
    
    /**
     * 
     * @return
     * @author Euan Westenbroek
     */
    public boolean isKale() {

		switch (this.getBca().getTypeEnum()) {
			case KALEUNDIRECTED:
				return true;
			case KALESEPERATED:
				return true;
			case KALENODEBASED:
				return true;
				
			default:
				return false;
		}
    }

    public static class SimilarityGroup {

        public enum Time {
            BACKWARDS, FORWARDS, BIDIRECTIONAL
        }

        private String sourcePredicate;
        private String targetPredicate;
        private String method;
        private double threshold;
        private int ngram;
        private double distance;
        private double smooth;
        private String pattern;
        private String time;

        /**
         * Instantiate a similarity object from the configuration information
         */
        public StringSimilarity toFunction() {

            switch (getMethodEnum()) {
                case NUMERIC:
                    return new Numeric(getSmooth(), getDistance());
                case DATE_DAYS:
                    return new DateDays(getPattern(), getSmooth(), getDistance(), getTimeEnum());
                case DATE_MONTHS:
                    return new DateMonths(getPattern(), getSmooth(), getDistance(), getTimeEnum());
                case DATE_YEARS:
                    return new DateYears(getPattern(), getSmooth(), getDistance(), getTimeEnum());
                case LEVENSHTEIN:
                    return new NormalizedLevenshtein();
                case JAROWINKLER:
                    return new JaroWinkler();
                case NGRAM_JACCARD:
                    return new PreComputedNgramJaccard(getNgram());
                case NGRAM_COSINE:
                    return new PreComputedNgramCosine(getNgram());
                case TOKEN_JACCARD:
                    return new PreComputedTokenJaccard();
                case TOKEN_COSINE:
                    return new PreComputedTokenCosine();
                default:
                    throw new IllegalArgumentException("Unsupported similarity method: " + getMethodEnum());
            }
        }

        @Override
        public String toString() {
            String out = getSourcePredicate() + " -> " + getTargetPredicate() + "\n method:" + getMethod() + ", threshold: " + getThreshold();
            switch (getMethodEnum()) {
                default: return out;
                case NGRAM_COSINE:
                case NGRAM_JACCARD: return out + ", ngram: " + getNgram();
                case NUMERIC: return out + ", smooth: " + getSmooth();
                case DATE_DAYS:
                case DATE_MONTHS:
                case DATE_YEARS: return out + ", pattern:" + getPattern() + ", smooth: " + getSmooth() + ", time: " + getTime();
            }
        }

        public String getTime() {
            return time == null ? "bidirectional" : time;
        }

        public Time getTimeEnum() {
            return Time.valueOf(getTime().toUpperCase());
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getTargetPredicate() {
            return targetPredicate;
        }

        public void setTargetPredicate(String targetPredicate) {
            this.targetPredicate = targetPredicate;
        }

        public String getSourcePredicate() {
            return sourcePredicate;
        }

        public void setSourcePredicate(String sourcePredicate) {
            this.sourcePredicate = sourcePredicate;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public SimilarityMethod getMethodEnum() {
            return SimilarityMethod.valueOf(this.method.toUpperCase());
        }

        public String getPattern() {
            return pattern == null ? "iso" : pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public int getNgram() {
            return ngram == 0 ? 3 : ngram;
        }

        public void setNgram(int ngram) {
            this.ngram = ngram;
        }

        public double getSmooth() {return smooth == 0 ? 1 : smooth;}

        public void setSmooth(double smooth) {this.smooth = smooth;}
    }

    public static class BCA {

        private double alpha;
        private double epsilon;
        private String normalize;
        private String type;

        public BCANormalization getNormalizeEnum() {
            return normalize == null ? BCANormalization.NONE : BCANormalization.valueOf(normalize.toUpperCase());
        }

        public String getNormalize() {
            return normalize == null ? "none" : normalize;
        }

        public void setNormalize(String normalize) {
            this.normalize = normalize;
        }

        public double getAlpha() {
            return alpha;
        }

        public void setAlpha(double alpha) {
            this.alpha = alpha;
        }

        public double getEpsilon() {
            return epsilon;
        }

        public void setEpsilon(double epsilon) {
            this.epsilon = epsilon;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public BCAType getTypeEnum(){ return BCAType.valueOf(type.toUpperCase());}

    }

    public static class Opt {

        private String method;
        private double tolerance;
        private int maxiter;

        public OptimizationMethod getMethodEnum() {
            return OptimizationMethod.valueOf(method.toUpperCase());
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public double getTolerance() {
            return tolerance;
        }

        public void setTolerance(double tolerance) {
            this.tolerance = tolerance;
        }

        public int getMaxiter() {
            return maxiter;
        }

        public void setMaxiter(int maxiter) {
            this.maxiter = maxiter;
        }
    }

    public static class PCA {

        private double variance;

        public double getVariance() {
            return variance;
        }

        public void setVariance(double variance) {
            this.variance = variance;
        }
    }

    public static class Output {

        private String name;
        private List<String> uri;
        private List<String> blank;
        private List<String> predicate;
        private List<String> literal;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean outputUriNodes() {
            return uri != null;
        }

        public boolean outputBlankNodes() {
            return blank != null;
        }

        public boolean outputPredicates() {
            return predicate != null;
        }

        public boolean outputLiteralNodes() {
            return literal != null;
        }

        public List<String> getLiteral() {
            return literal;
        }

        public void setLiteral(List<String> literal) {
            this.literal = literal;
        }

        public List<String> getUri() {
            return uri;
        }

        public void setUri(List<String> uri) {
            this.uri = uri;
        }

        public List<String> getBlank() {
            return blank;
        }

        public void setBlank(List<String> blank) {
            this.blank = blank;
        }

        public List<String> getPredicate() {
            return predicate;
        }

        public void setPredicate(List<String> predicate) {
            this.predicate = predicate;
        }
    }

    public static void check(Configuration config) throws InvalidConfigurationException {
        boolean hasDim = config.dim > 0;
        boolean hasGraph = config.graph != null && !config.graph.isEmpty();
        boolean hasMethod = config.method != null && !config.method.isEmpty();
        boolean hasBca = config.bca != null && config.bca.alpha > 0 && config.bca.epsilon > 0;
        boolean hasOut = config.output != null && (
                config.output.outputPredicates() ||
                config.output.outputBlankNodes() ||
                config.output.outputUriNodes() ||
                config.output.outputLiteralNodes());

        if(!hasDim) throw new InvalidConfigurationException("No dimension specified");
        if(!hasGraph) throw new InvalidConfigurationException("No input graph specified");
        if(!hasMethod) throw new InvalidConfigurationException("Invalid method, choose one of: glove, pglove");
        if(!hasBca) throw new InvalidConfigurationException("Invalid BCA parameters, alpha and epsilon are mandatory");
        if(!hasOut) throw new InvalidConfigurationException("Invalid output parameters, specify at least one group");
    }

    public static class InvalidConfigurationException extends Exception {
        public InvalidConfigurationException(String message) {
            super("Invalid configuration: " + message);
        }
    }
}
