package cunei.optimize;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import cunei.config.BooleanParameter;
import cunei.config.Configuration;
import cunei.config.StringSetParameter;
import cunei.config.SystemConfig;
import cunei.document.BufferedDocuments;
import cunei.document.Phrase;
import cunei.evaluation.AbstractEvaluation;
import cunei.evaluation.Evaluator;
import cunei.evaluation.Metric;
import cunei.evaluation.MetricExpectation;
import cunei.model.Feature;
import cunei.model.Features;
import cunei.model.Gradient;
import cunei.model.LogFeatureModel;
import cunei.optimize.ProjectedEvaluation.ProjectedSentenceEvaluation;
import cunei.util.Log;
import cunei.util.Time;

public class Optimizer {

    private static final Features FEATURES = Features.getInstance();

    private static final Configuration CONFIG = SystemConfig.getInstance();

    protected static StringSetParameter CFG_OPT_IGNORE = StringSetParameter.get(CONFIG, "Optimize.Ignore", Collections.<String>emptySet());

    private static BooleanParameter CFG_OPT_NORM = BooleanParameter.get(CONFIG, "Optimize.Normalize", false);

    private static class GradientPoint extends Point {

        private static final long serialVersionUID = 1L;

        protected double x;

        protected Gradient gradient;

        protected double dot;

        public GradientPoint(ProjectedEvaluation evaluation, PrintStream output) {
            super(evaluation, output);
            init();
        }

        public GradientPoint(GradientPoint point, Gradient direction) {
            score = point.score;
            weights = point.weights;
            averageDistance = point.averageDistance;
            x = 0;
            gradient = point.gradient;
            dot = direction.dot(gradient);
        }

        public GradientPoint(ProjectedEvaluation evaluation, GradientPoint point, Gradient direction, double x, PrintStream output) {
            weights = Arrays.copyOf(point.weights, point.weights.length);
            for (int i = 0; i < weights.length; i++) {
                final double delta = x * direction.get(i);
                weights[i] += delta;
            }
            setWeightsAndRescore(evaluation);
            load(evaluation, output);
            this.x = x;
            dot = direction.dot(gradient);
        }

        public GradientPoint(ProjectedEvaluation evaluation, Point point, PrintStream output) {
            weights = point.weights;
            setWeightsAndRescore(evaluation);
            load(evaluation, output);
            init();
        }

        public GradientPoint(ProjectedEvaluation evaluation, Point point, float gamma, PrintStream output) {
            weights = Arrays.copyOf(point.weights, point.weights.length);
            weights[LogFeatureModel.FEAT_MODEL_GAMMA.getId()] = gamma;
            setWeightsAndRescore(evaluation);
            load(evaluation, output);
            init();
        }

        public final double getDotProduct() {
            return dot;
        }

        public final Gradient getGradient() {
            return gradient;
        }

        public final double getX() {
            return x;
        }

        private void init() {
            x = 0;
            dot = gradient.dot(gradient);
        }

        protected void load(ProjectedEvaluation evaluation, PrintStream output) {
            MetricExpectation expectation = evaluation.getExpectation();
            averageDistance = evaluation.getAverageDistance();
            loadGradient(expectation);
            loadScore(expectation, output);
        }

        private void loadGradient(MetricExpectation expectation) {
            gradient = expectation.getGradient();
            final double norm = CFG_OPT_NORM.getValue() ? Math.sqrt(gradient.dot(gradient)) : 1;
            final Collection<String> ignore = CFG_OPT_IGNORE.getValue();
            for (int id = 0; id < FEATURES.size(); id++) if (ignore.contains(FEATURES.getName(id))) gradient.clear(id); else if (norm != 1) gradient.set(id, gradient.get(id) / norm);
        }

        public final void write(PrintStream output) {
            output.format("%-33s [%-+13.6g]  %-+13.7g  %-+13.7g%n", "POINT", getDotProduct(), getAverageDistance(), getScore());
            output.flush();
        }
    }

    protected static class Point implements Serializable {

        private static final long serialVersionUID = 1L;

        protected double score;

        protected float[] weights;

        protected float averageDistance;

        private Point() {
        }

        public Point(ProjectedEvaluation evaluation, PrintStream output) {
            weights = new float[FEATURES.size()];
            for (int id = 0; id < weights.length; id++) weights[id] = FEATURES.getValue(id);
            evaluation.rescore(weights);
            load(evaluation, output);
        }

        public final double getScore() {
            return score;
        }

        public final float[] getWeights() {
            return weights;
        }

        public final float getAverageDistance() {
            return averageDistance;
        }

        protected void load(ProjectedEvaluation evaluation, PrintStream output) {
            loadScore(evaluation.getExpectation(), output);
            averageDistance = evaluation.getAverageDistance();
        }

        protected final void loadScore(MetricExpectation expectation, PrintStream output) {
            score = expectation.getScore();
            if (output != null) {
                expectation.write(output);
                output.println();
                output.format("%-50s  %-+13.7g%n", "Score", score);
                output.println();
                output.flush();
            }
        }

        public final void setWeightsAndRescore(ProjectedEvaluation evaluation) {
            for (int id = 0; id < weights.length; id++) FEATURES.setValue(id, weights[id]);
            evaluation.rescore(weights);
        }
    }

    private static final float SCORE_EPSILON = 0.00001f;

    private static final float PARAM_EPSILON = 0.0025f;

    private static final float GAMMA_EPSILON = 0.1f;

    private static final float DIST_EPSILON = 2f;

    protected static boolean isConverged(double score, double prevScore, float tolerance, float epsilon) {
        return 2.0 * Math.abs(score - prevScore) <= tolerance * (Math.abs(score) + Math.abs(prevScore)) + epsilon;
    }

    private final PrintStream output;

    private final Evaluator evaluator;

    private final ProjectedEvaluation evaluation;

    public Optimizer(PrintStream output, Evaluator evaluator, ProjectedEvaluation evaluation) {
        this.output = output;
        this.evaluator = evaluator;
        this.evaluation = evaluation;
    }

    private void anneal(final float maxGamma, final float gammaAccel, final float objectiveTolerance, final float objectiveAccel, final float scoreTolerance, final float paramTolerance, final float distanceLimit, final float randomLimit, final long randomSeed, final BufferedDocuments<Phrase> references, final int n, final int maxNbest, File stateFile, boolean keepState) {
        float gamma = 0;
        boolean annealObjective = true;
        double[] convergedScores = new double[n];
        double[] totalLogScores = new double[n];
        boolean[] isConverged = new boolean[n];
        GradientPoint[] initPoints = new GradientPoint[n];
        GradientPoint[] prevInitPoints = new GradientPoint[n];
        GradientPoint[] bestInitPoints = new GradientPoint[n];
        GradientPoint[] prevMinPoints = new GradientPoint[n];
        Random rand = new Random(randomSeed);
        Time time = new Time();
        if (stateFile != null && stateFile.length() > 0) {
            time.reset();
            try {
                ObjectInputStream stream = new ObjectInputStream(new FileInputStream(stateFile));
                gamma = stream.readFloat();
                annealObjective = stream.readBoolean();
                convergedScores = (double[]) stream.readObject();
                totalLogScores = (double[]) stream.readObject();
                isConverged = (boolean[]) stream.readObject();
                initPoints = (GradientPoint[]) stream.readObject();
                prevInitPoints = (GradientPoint[]) stream.readObject();
                bestInitPoints = (GradientPoint[]) stream.readObject();
                prevMinPoints = (GradientPoint[]) stream.readObject();
                rand = (Random) stream.readObject();
                int size = stream.readInt();
                for (int id = 0; id < size; id++) {
                    Feature feature = FEATURES.getRaw(CONFIG, stream.readUTF(), 0f);
                    if (feature.getId() != id) throw new Exception("Features have changed");
                }
                evaluation.read(stream);
                stream.close();
                output.println("# Resuming from previous optimization state (" + time + ")");
                output.println();
            } catch (Exception e) {
                e.printStackTrace();
                Log.getInstance().severe("Failed loading optimization state (" + stateFile + "): " + e.getMessage());
            }
        } else {
            final int evaluations = ProjectedEvaluation.CFG_OPT_HISTORY_SIZE.getValue();
            final GradientPoint[] randPoints = new GradientPoint[n * evaluations];
            for (int i = 0; i < n; i++) {
                evaluation.setParallelId(i);
                for (int j = 0; j < evaluations; j++) {
                    if (i != 0) randPoints[i * n + j] = getRandomPoint(rand, randPoints[0], distanceLimit, null);
                    evaluate(references, i + ":" + j);
                    if (i == 0) {
                        randPoints[0] = new GradientPoint(evaluation, null);
                        gamma = LogFeatureModel.FEAT_MODEL_GAMMA.getValue();
                        break;
                    }
                }
            }
            for (int i = 0; i < randPoints.length; i++) if (randPoints[i] != null) randPoints[i] = new GradientPoint(evaluation, randPoints[i], output);
            for (int i = 0; i < n; i++) {
                prevInitPoints[i] = null;
                initPoints[i] = randPoints[i * n];
                if (i != 0) for (int j = 1; j < evaluations; j++) if (randPoints[i * n + j].getScore() < initPoints[i].getScore()) initPoints[i] = randPoints[i * n + j];
                bestInitPoints[i] = initPoints[i];
                convergedScores[i] = Float.MAX_VALUE;
            }
        }
        for (int searchCount = 1; ; searchCount++) {
            boolean isFinished = true;
            for (int i = 0; i < n; i++) isFinished = isFinished && isConverged[i];
            if (isFinished) {
                output.println("*** N-best list converged. Modifying annealing schedule. ***");
                output.println();
                if (annealObjective) {
                    boolean objectiveConverged = true;
                    for (int i = 0; objectiveConverged && i < n; i++) objectiveConverged = isConverged(bestInitPoints[i].getScore(), convergedScores[i], objectiveTolerance, SCORE_EPSILON);
                    annealObjective = false;
                    for (Metric<ProjectedSentenceEvaluation> metric : AbstractEvaluation.CFG_EVAL_METRICS.getValue()) if (metric.doAnnealing()) {
                        float weight = metric.getWeight();
                        if (weight != 0) if (objectiveConverged) metric.setWeight(0); else {
                            annealObjective = true;
                            metric.setWeight(weight / objectiveAccel);
                        }
                    }
                }
                if (!annealObjective) {
                    if (Math.abs(gamma) >= maxGamma) {
                        GradientPoint bestPoint = bestInitPoints[0];
                        for (int i = 1; i < n; i++) if (bestInitPoints[i].getScore() < bestPoint.getScore()) bestPoint = bestInitPoints[i];
                        output.format("Best Score: %+.7g%n", bestPoint.getScore());
                        output.println();
                        bestPoint = new GradientPoint(evaluation, bestPoint, output);
                        break;
                    }
                    gamma *= gammaAccel;
                    if (Math.abs(gamma) + GAMMA_EPSILON >= maxGamma) gamma = gamma >= 0 ? maxGamma : -maxGamma;
                }
                for (int i = 0; i < n; i++) {
                    convergedScores[i] = bestInitPoints[i].getScore();
                    initPoints[i] = new GradientPoint(evaluation, bestInitPoints[i], gamma, output);
                    bestInitPoints[i] = initPoints[i];
                    prevInitPoints[i] = null;
                    prevMinPoints[i] = null;
                    isConverged[i] = false;
                }
                searchCount = 0;
            }
            for (int i = 0; i < n; i++) {
                if (isConverged[i]) continue;
                if (n > 1) output.println("Minimizing point " + i);
                Gradient gradient = initPoints[i].getGradient();
                for (int id = 0; id < FEATURES.size(); id++) output.format("GRAD %-65s %-+13.7g%n", FEATURES.getName(id), gradient.get(id));
                output.println();
                time.reset();
                GradientPoint minPoint = minimize(initPoints[i], prevInitPoints[i], bestInitPoints[i], scoreTolerance, paramTolerance, distanceLimit, randomLimit, rand);
                final float[] weights = minPoint.getWeights();
                for (int j = 0; j < weights.length; j++) output.format("PARM %-65s %-+13.7g%n", FEATURES.getName(j), weights[j]);
                output.println();
                output.format("Minimum Score: %+.7g (average distance of %.2f)%n", minPoint.getScore(), minPoint.getAverageDistance());
                output.println();
                output.println("# Minimized gradient (" + time + ")");
                output.println();
                output.flush();
                isConverged[i] = weights == initPoints[i].getWeights();
                prevInitPoints[i] = initPoints[i];
                prevMinPoints[i] = minPoint;
                initPoints[i] = minPoint;
            }
            for (int i = 0; i < n; i++) {
                if (isConverged[i]) continue;
                isConverged[i] = isConvergedScore("minimum", prevMinPoints[i], prevInitPoints[i], scoreTolerance) && isConvergedWeights(prevMinPoints[i], prevInitPoints[i], paramTolerance);
                prevMinPoints[i].setWeightsAndRescore(evaluation);
                evaluation.setParallelId(i);
                evaluate(references, Integer.toString(i));
            }
            Set<Point> prunePoints = new HashSet<Point>();
            prunePoints.addAll(Arrays.asList(bestInitPoints));
            prunePoints.addAll(Arrays.asList(prevInitPoints));
            prunePoints.addAll(Arrays.asList(initPoints));
            evaluation.prune(prunePoints, maxNbest, output);
            for (int i = 0; i < n; i++) {
                final boolean bestIsPrev = bestInitPoints[i] == prevInitPoints[i];
                final boolean bestIsInit = bestInitPoints[i] == initPoints[i];
                bestInitPoints[i] = new GradientPoint(evaluation, bestInitPoints[i], bestIsInit ? output : null);
                if (bestIsPrev) prevInitPoints[i] = bestInitPoints[i];
                if (bestIsInit) initPoints[i] = bestInitPoints[i];
                if (!bestIsPrev && prevInitPoints[i] != null) {
                    prevInitPoints[i] = new GradientPoint(evaluation, prevInitPoints[i], null);
                    if (prevInitPoints[i].getScore() <= bestInitPoints[i].getScore()) bestInitPoints[i] = prevInitPoints[i];
                }
                if (!bestIsInit) {
                    initPoints[i] = new GradientPoint(evaluation, initPoints[i], output);
                    if (initPoints[i].getScore() <= bestInitPoints[i].getScore()) bestInitPoints[i] = initPoints[i];
                }
            }
            for (int i = 0; i < n; i++) if (isConverged[i]) if (prevMinPoints[i] == null) {
                output.println("# Convergence failed: no previous minimum is defined");
                output.println();
                isConverged[i] = false;
            } else {
                isConverged[i] = isConvergedScore("best known", bestInitPoints[i], initPoints[i], scoreTolerance) && isConvergedScore("previous minimum", prevMinPoints[i], initPoints[i], scoreTolerance);
            }
            if (stateFile != null) {
                time.reset();
                try {
                    File dir = stateFile.getCanonicalFile().getParentFile();
                    File temp = File.createTempFile("cunei-opt-", ".tmp", dir);
                    ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(temp));
                    stream.writeFloat(gamma);
                    stream.writeBoolean(annealObjective);
                    stream.writeObject(convergedScores);
                    stream.writeObject(totalLogScores);
                    stream.writeObject(isConverged);
                    stream.writeObject(initPoints);
                    stream.writeObject(prevInitPoints);
                    stream.writeObject(bestInitPoints);
                    stream.writeObject(prevMinPoints);
                    stream.writeObject(rand);
                    stream.writeInt(FEATURES.size());
                    for (int id = 0; id < FEATURES.size(); id++) stream.writeUTF(FEATURES.getName(id));
                    evaluation.write(stream);
                    stream.close();
                    if (!temp.renameTo(stateFile)) {
                        FileChannel in = null;
                        FileChannel out = null;
                        try {
                            in = new FileInputStream(temp).getChannel();
                            out = new FileOutputStream(stateFile).getChannel();
                            in.transferTo(0, in.size(), out);
                            temp.delete();
                        } finally {
                            if (in != null) in.close();
                            if (out != null) out.close();
                        }
                    }
                    output.println("# Saved optimization state (" + time + ")");
                    output.println();
                } catch (IOException e) {
                    Log.getInstance().severe("Failed writing optimization state: " + e.getMessage());
                }
            }
        }
        if (stateFile != null && !keepState) stateFile.delete();
    }

    private GradientPoint getRandomPoint(final Random rand, final GradientPoint point, float distanceLimit, final PrintStream output) {
        distanceLimit -= point.getAverageDistance();
        if (distanceLimit <= 0) return point;
        final Collection<String> ignore = CFG_OPT_IGNORE.getValue();
        final Gradient direction = new Gradient();
        for (int id = 0; id < FEATURES.size(); id++) {
            if (!ignore.contains(FEATURES.getName(id))) direction.set(id, rand.nextDouble());
        }
        point.setWeightsAndRescore(evaluation);
        final double max = evaluation.getMaxMovement(direction, distanceLimit);
        return new GradientPoint(evaluation, point, direction, max * rand.nextDouble(), output);
    }

    private GradientPoint minimize(GradientPoint initPoint, GradientPoint prevInitPoint, GradientPoint bestInitPoint, final float scoreTolerance, final float paramTolerance, float distanceLimit, final float randomLimit, Random rand) {
        distanceLimit += initPoint.getAverageDistance();
        output.format("Initial Score: %+.7g [%.2f distance]%n", initPoint.getScore(), initPoint.getAverageDistance());
        output.println();
        GradientPoint result = minimizeByGradient(initPoint, scoreTolerance, paramTolerance, distanceLimit);
        if (prevInitPoint != null) {
            output.format("Previous Initial Score: %+.7g [%.2f distance]%n", prevInitPoint.getScore(), prevInitPoint.getAverageDistance());
            output.println();
            if (prevInitPoint.getScore() < initPoint.getScore()) {
                GradientPoint rePrevPoint = minimizeByGradient(prevInitPoint, scoreTolerance, paramTolerance, distanceLimit);
                if (rePrevPoint.getScore() < result.getScore()) result = rePrevPoint;
            }
        }
        if (bestInitPoint != initPoint && bestInitPoint != prevInitPoint) {
            output.format("Best Initial Score: %+.7g [%.2f distance]%n", bestInitPoint.getScore(), bestInitPoint.getAverageDistance());
            output.println();
            if (bestInitPoint.getScore() < initPoint.getScore()) {
                GradientPoint reBestPoint = minimizeByGradient(bestInitPoint, scoreTolerance, paramTolerance, distanceLimit);
                if (reBestPoint.getScore() < result.getScore()) result = reBestPoint;
            }
        }
        if (result.getAverageDistance() + DIST_EPSILON < distanceLimit) {
            for (int randomCount = 0; ; randomCount++) {
                GradientPoint randPoint = null;
                for (int i = 0; i < 10; i++) {
                    GradientPoint point = getRandomPoint(rand, result, distanceLimit, null);
                    if (randPoint == null || point.getScore() < randPoint.getScore()) randPoint = point;
                }
                output.format("Random Score: %+.7g [%.2f distance]%n", randPoint.getScore(), randPoint.getAverageDistance());
                output.println();
                randPoint = minimizeByGradient(randPoint, scoreTolerance, paramTolerance, distanceLimit);
                if (randPoint.getScore() < result.getScore()) {
                    result = randPoint;
                    break;
                }
                if (rand.nextFloat() > randomLimit) break;
            }
        }
        return result;
    }

    private GradientPoint minimizeByGradient(GradientPoint initPoint, float scoreTolerance, float paramTolerance, final float distanceLimit) {
        Point prevInitPoint = null;
        final Gradient conjGradient = initPoint.getGradient().clone();
        conjGradient.negate();
        for (int iterCount = 1; ; iterCount++) {
            initPoint = new GradientPoint(initPoint, conjGradient);
            GradientPoint minPoint = minimizeByLine(initPoint, conjGradient, scoreTolerance, paramTolerance, distanceLimit);
            output.println();
            output.format("Iteration %d: %+.7g [%.2f%% of limit]%n", iterCount, minPoint.getScore(), 100 * minPoint.getAverageDistance() / distanceLimit);
            output.println();
            output.flush();
            if (minPoint == initPoint) {
                output.println("Failed to find new minimum");
                output.println();
            }
            if (minPoint.getAverageDistance() + DIST_EPSILON >= distanceLimit) return minPoint;
            if (prevInitPoint != null && isConvergedScore("previous initial", prevInitPoint, initPoint, scoreTolerance) && isConvergedScore("minimum", minPoint, initPoint, scoreTolerance) && isConvergedWeights(minPoint, initPoint, paramTolerance)) return minPoint;
            final Gradient minGradient = minPoint.getGradient();
            final Gradient initGradient = initPoint.getGradient();
            prevInitPoint = initPoint;
            initPoint = minPoint;
            double gg = 0;
            double dgg = 0;
            for (int id = 0; id < FEATURES.size(); id++) {
                final double minValue = minGradient.get(id);
                final double initValue = initGradient.get(id);
                dgg += minValue * (minValue - initValue);
                gg += Math.pow(initValue, 2);
            }
            final double gam = dgg / gg;
            if (gg == 0 || Double.isNaN(gam) || Double.isInfinite(gam)) break;
            conjGradient.multiply(gam);
            conjGradient.sum(minGradient, -1);
        }
        return initPoint;
    }

    private GradientPoint minimizeByLine(final GradientPoint start, final Gradient direction, final float scoreTolerance, final float paramTolerance, float distanceLimit) {
        distanceLimit -= start.getAverageDistance();
        if (distanceLimit <= 0) return start;
        start.setWeightsAndRescore(evaluation);
        final double max = evaluation.getMaxMovement(direction, distanceLimit);
        GradientPoint left = start;
        left.write(output);
        GradientPoint right = null;
        int n = 8;
        GradientPoint mid = new GradientPoint(evaluation, start, direction, max / n, null);
        mid.write(output);
        while (Double.isNaN(mid.getScore()) || Double.isNaN(mid.getDotProduct()) || mid.getScore() > left.getScore()) {
            right = mid;
            if (n > 1024) {
                mid = left;
                break;
            }
            n *= 2;
            mid = new GradientPoint(evaluation, start, direction, max / n, null);
            mid.write(output);
        }
        if (right == null) {
            n /= 2;
            right = new GradientPoint(evaluation, start, direction, max / n, null);
            right.write(output);
            while (right.getScore() < mid.getScore()) {
                mid = right;
                if (n == 1) break;
                n /= 2;
                right = new GradientPoint(evaluation, start, direction, max / n, null);
                right.write(output);
            }
        }
        double delta = 0;
        double distance = 0;
        GradientPoint upper = mid;
        GradientPoint lower = mid;
        for (int pointCount = 0; ; pointCount++) {
            if (Double.isNaN(mid.getDotProduct())) break;
            double tolerance = paramTolerance * Math.abs(mid.getX()) + PARAM_EPSILON;
            if (Math.abs(2 * mid.getX() - left.getX() - right.getX()) + right.getX() - left.getX() <= 4 * tolerance && isConverged(left.getScore(), mid.getScore(), scoreTolerance, SCORE_EPSILON) && isConverged(right.getScore(), mid.getScore(), scoreTolerance, SCORE_EPSILON)) break;
            boolean valid = false;
            if (Math.abs(distance) > tolerance) {
                double maxDelta = Math.abs(distance / 2);
                distance = delta;
                delta = maxDelta;
                if (lower != mid) {
                    double d = (lower.getX() - mid.getX()) * mid.getDotProduct() / (mid.getDotProduct() - lower.getDotProduct());
                    if (Math.abs(d) < Math.abs(delta)) {
                        double u = mid.getX() + d;
                        if ((left.getX() - u) * (u - right.getX()) > 0 && d * mid.getDotProduct() <= 0) delta = d;
                    }
                }
                if (upper != mid) {
                    double d = (upper.getX() - mid.getX()) * mid.getDotProduct() / (mid.getDotProduct() - upper.getDotProduct());
                    if (Math.abs(d) < Math.abs(delta)) {
                        double u = mid.getX() + d;
                        if ((left.getX() - u) * (u - right.getX()) > 0 && d * mid.getDotProduct() <= 0) delta = d;
                    }
                }
                valid = Math.abs(delta) < maxDelta;
            }
            if (!valid) {
                distance = mid.getDotProduct() >= 0 ? left.getX() - mid.getX() : right.getX() - mid.getX();
                delta = distance / 2;
            }
            if (delta == 0) break;
            GradientPoint alt = new GradientPoint(evaluation, start, direction, mid.getX() + delta, null);
            alt.write(output);
            valid = Math.abs(delta) < tolerance && isConverged(alt.getScore(), mid.getScore(), scoreTolerance, SCORE_EPSILON);
            if (alt.getScore() <= mid.getScore()) {
                if (alt.getX() >= mid.getX()) left = mid; else right = mid;
                upper = lower;
                lower = mid;
                mid = alt;
            } else {
                if (alt.getX() < mid.getX()) left = alt; else right = alt;
                if (alt.getScore() <= lower.getScore() || lower == mid) {
                    upper = lower;
                    lower = alt;
                } else if (alt.getScore() < upper.getScore() || upper == mid || upper == lower) upper = alt;
            }
            if (valid) break;
        }
        return mid;
    }

    private boolean isConvergedScore(final String name, final Point point, final Point initPoint, final float scoreTolerance) {
        if (isConverged(point.getScore(), initPoint.getScore(), scoreTolerance, SCORE_EPSILON)) return true;
        output.format("# Convergence failed: %s score %.5g is not within %.5g of the initial score %.5g%n", name, point.getScore(), scoreTolerance, initPoint.getScore());
        output.println();
        return false;
    }

    private boolean isConvergedWeights(final Point minPoint, final Point initPoint, final float paramTolerance) {
        boolean result = true;
        final float[] minWeights = minPoint.getWeights();
        final float[] initWeights = initPoint.getWeights();
        final int length = minWeights.length;
        if (length != initWeights.length) {
            output.println("# Convergence failed: a new parameter was introduced");
            result = false;
        } else {
            final int gammaId = LogFeatureModel.FEAT_MODEL_GAMMA.getId();
            for (int id = 0; id < length; id++) {
                if (id != gammaId && !isConverged(minWeights[id], initWeights[id], paramTolerance, PARAM_EPSILON)) {
                    output.format("# Convergence failed: %.5g is not within %.5g of the inital value of %.5g for %s%n", minWeights[id], paramTolerance, initWeights[id], FEATURES.getName(id));
                    result = result && false;
                }
            }
        }
        if (!result) output.println();
        return result;
    }

    private void evaluate(final BufferedDocuments<Phrase> references, String id) {
        Time time = new Time();
        if (references != null) {
            evaluator.run(references);
            output.println("# Evaluated with references (" + time + ")");
            output.println();
            new Point(evaluation, output);
            time.reset();
        }
        evaluator.run();
        output.println("# Evaluated " + id + " (" + time + ")");
        output.println();
    }

    public void run(final float gamma, final float maxGamma, final float gammaAccel, final float objectiveTolerance, final float objectiveAccel, final float scoreTolerance, final float paramTolerance, final float distanceLimit, final float randomLimit, final long randomSeed, final BufferedDocuments<Phrase> references, final int n, final int maxNbest, File stateFile, boolean keepState) {
        final float originalGamma = LogFeatureModel.FEAT_MODEL_GAMMA.getValue();
        LogFeatureModel.FEAT_MODEL_GAMMA.setValue(gamma);
        CFG_OPT_IGNORE.add(LogFeatureModel.FEAT_MODEL_GAMMA.getName());
        anneal(maxGamma, gammaAccel, objectiveTolerance, objectiveAccel, scoreTolerance, paramTolerance, distanceLimit, randomLimit, randomSeed, references, n, maxNbest, stateFile, keepState);
        LogFeatureModel.FEAT_MODEL_GAMMA.setValue(originalGamma);
    }
}
