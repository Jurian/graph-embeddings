package org.uu.nl.embedding;

import org.apache.log4j.Logger;
import org.uu.nl.embedding.bca.BookmarkColoring;
import org.uu.nl.embedding.convert.Rdf2GrphConverter;
import org.uu.nl.embedding.kale.KaleRunner;
import org.uu.nl.embedding.opt.*;
import org.uu.nl.embedding.opt.grad.AMSGrad;
import org.uu.nl.embedding.opt.grad.Adagrad;
import org.uu.nl.embedding.opt.grad.Adam;
import org.uu.nl.embedding.util.CoOccurrenceMatrix;
import org.uu.nl.embedding.util.InMemoryRdfGraph;
import org.uu.nl.embedding.util.config.Configuration;
import org.uu.nl.embedding.util.read.ConfigReader;
import org.uu.nl.embedding.util.read.JenaReader;
import org.uu.nl.embedding.util.write.EmbeddingTextWriter;
import org.uu.nl.embedding.util.write.EmbeddingWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Jurian Baas
 */
public class Main {

    private final static Logger logger = Logger.getLogger("Graph Embeddings");

    private static void runProgram(Configuration config) throws IOException {

        logger.info("Starting the embedding creation process with following settings:");
        logger.info("Graph File: " + config.getGraph());
        logger.info("Embedding dimensions: " + config.getDim());
        logger.info("Threads: " + config.getThreads());
        logger.info("BCA Alpha: " + config.getBca().getAlpha());
        logger.info("BCA Epsilon: " + config.getBca().getEpsilon());
        logger.info("BCA Type: " + config.getBca().getType());
        logger.info("BCA Normalize: " + config.getBca().getNormalize());
        logger.info("Gradient Descent Algorithm: " + config.getOpt().getMethod());
        logger.info(config.getMethod() + " Tolerance: " + config.getOpt().getTolerance());
        logger.info(config.getMethod() + " Maximum Iterations: " + config.getOpt().getMaxiter());

        if(config.usingPca()) logger.info("PCA Minimum Variance: " + config.getPca().getVariance());
        else logger.info("No PCA will be performed");

        if(config.usingWeights()) {
            logger.info("Using weights, predicates that are not listed are ignored:");
            config.getWeights().forEach((k, v) -> logger.info( k + ": " + v));
        } else logger.info("No weights specified, using linear weight");

        if(config.usingSimilarity()) {
            logger.info("Using the following similarity metrics:");
            config.getSimilarity().forEach(s -> logger.info(s.toString()));
        } else logger.info("No similarity matching will be performed");

        String outFileName = config.getOutput().getName();
        if(outFileName == null || outFileName.isEmpty()) {
            outFileName = createFileName(config);
        }
        logger.info("Writing files with prefix: " + outFileName);

        Configuration.setThreadLocalRandom();

        final JenaReader loader = new JenaReader();

        final Rdf2GrphConverter converter = new Rdf2GrphConverter(config);

        final InMemoryRdfGraph graph = converter.convert(loader.load(config.getGraphFile()));

        //final CoOccurrenceMatrix bca = new BookmarkColoring(graph, config);
        
        try { 
	        if (config.isKale()) {
	        	KaleRunner kaleRunner = new KaleRunner(graph, config);
	        	/*
	        	CoOccurrenceMatrix kaleBca = kaleRunner.getKaleVectors();
	            final IOptimizer optimizerKale = createOptimizer(config, kaleBca);
	            final Optimum optimumKale = optimizerKale.optimize();
	            final EmbeddingWriter writerKale = new EmbeddingTextWriter(outFileName, config);
	            writerKale.write(optimumKale, kaleBca, Paths.get("").toAbsolutePath().resolve("out"));
	            */
	        }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //final IOptimizer optimizer = createOptimizer(config, bca);

        //final Optimum optimum = optimizer.optimize();

        //final EmbeddingWriter writer = new EmbeddingTextWriter(outFileName, config);
        //writer.write(optimum, bca, Paths.get("").toAbsolutePath().resolve("out"));
    }

    private static String createFileName(Configuration config) {
        String outFileName = config.getGraphFile().getName().toLowerCase();
        if(outFileName.contains(".")) {
            outFileName = outFileName.substring(0, outFileName.lastIndexOf("."));
        }
        outFileName += "_" + config.getMethod().toLowerCase();

        if(config.getSimilarity() != null && !config.getSimilarity().isEmpty()) {
            outFileName += "_partial";
        } else {
            outFileName += "_exact";
        }

        outFileName += "_" +  config.getBca().getType().toLowerCase();

        outFileName += "_" + config.getBca().getAlpha() + "_" + config.getBca().getEpsilon();
        outFileName += "_" + config.getOpt().getMethod();
        if(config.usingPca()) outFileName += "_pca_" + config.getDim();
        else outFileName += "_" + config.getDim();

        return outFileName;
    }

    private static IOptimizer createOptimizer(final Configuration config, final CoOccurrenceMatrix coMatrix) {

        CostFunction cf;
        switch (config.getMethodEnum()) {
            default:
                throw new IllegalArgumentException("Invalid cost function");
            case GLOVE:
                cf = new GloveCost();
                break;
            case PGLOVE:
                cf = new PGloveCost();
                break;
        }

        switch(config.getOpt().getMethodEnum()) {
            default:
                throw new IllegalArgumentException("Invalid optimization method");
            case ADAGRAD:
                return new Adagrad(coMatrix, config, cf);
            case ADAM:
                return new Adam(coMatrix, config, cf);
            case AMSGRAD:
                return new AMSGrad(coMatrix, config, cf);
        }
    }

    public static void main(String[] args) {

        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-c")) {
                if(i + 1 < args.length) {
                    try {

                        final File configFile = Paths.get("").toAbsolutePath().resolve( args[i + 1]).toFile();

                        if(!configFile.exists() || !configFile.isFile()) {
                            logger.error("Cannot find configuration file + " + configFile.getPath());
                            System.exit(1);
                        } else {
                            Configuration config = new ConfigReader().load(configFile);
                            Configuration.check(config);
                            runProgram(config);
                        }
                    } catch (IOException | Configuration.InvalidConfigurationException e) {
                        logger.error(e.getMessage(), e);
                        System.exit(1);
                    }
                } else {
                    logger.error("No configuration file specified, exiting...");
                    System.exit(1);
                }
            }
        }
    }
}
