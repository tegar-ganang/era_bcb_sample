package com.skjolberg.ddr.useragent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserAgentMatcher {

    /** matcher cache map */
    private Map<String, UserAgent> userAgentMap = new ConcurrentHashMap<String, UserAgent>();

    private Collection<UserAgent> userAgents;

    public UserAgentMatcher(Collection<UserAgent> userAgents) {
        this.userAgents = userAgents;
    }

    public UserAgent match(UserAgent candidate) {
        HashSet<UserAgent> compatible = new HashSet<UserAgent>();
        for (UserAgent ua : userAgents) {
            if (candidate.isCompatible(ua)) {
                compatible.add(ua);
            }
        }
        if (compatible.isEmpty()) {
            return null;
        }
        if (candidate.markers == null || candidate.markers.length == 0) {
            for (UserAgent userAgent : compatible) {
                return userAgent;
            }
            return null;
        }
        Set<UserAgent> subversions = getLower(compatible, candidate);
        boolean upper = subversions.isEmpty();
        if (upper) {
            subversions.addAll(compatible);
        }
        int languageCount = 0;
        int maxVersionCount = 0;
        for (int i = 0; i < candidate.getMarkerCount(); i++) {
            Marker marker = candidate.getMarker(i);
            if (marker instanceof Language) {
                languageCount++;
            } else {
                if (maxVersionCount < marker.length()) {
                    maxVersionCount = marker.length();
                }
            }
        }
        if (languageCount > 0) {
            maxVersionCount++;
        }
        long[] spanMinimum = new long[maxVersionCount];
        long[] spanMaximum = new long[maxVersionCount];
        for (int spanIndex = 0; spanIndex < maxVersionCount; spanIndex++) {
            spanMaximum[spanIndex] = 0;
            spanMinimum[spanIndex] = Long.MAX_VALUE;
            for (int markerIndex = 0; markerIndex < candidate.getMarkerCount(); markerIndex++) {
                if (candidate.getMarker(markerIndex) instanceof Language) {
                    continue;
                }
                Version version = candidate.getVersion(markerIndex);
                long value;
                if (version.length() > spanIndex) {
                    value = version.getNumericValue(spanIndex);
                } else {
                    value = 0;
                }
                if (value > spanMaximum[spanIndex]) {
                    spanMaximum[spanIndex] = value;
                }
                if (value < spanMinimum[spanIndex]) {
                    spanMinimum[spanIndex] = value;
                }
                for (UserAgent userAgent : subversions) {
                    version = userAgent.getVersion(markerIndex);
                    if (version.length() > spanIndex) {
                        value = version.getNumericValue(spanIndex);
                    } else {
                        value = 0;
                    }
                    if (value > spanMaximum[spanIndex]) {
                        spanMaximum[spanIndex] = value;
                    }
                    if (value < spanMinimum[spanIndex]) {
                        spanMinimum[spanIndex] = value;
                    }
                }
            }
        }
        if (languageCount > 0) {
            spanMaximum[maxVersionCount - 1] = 2 * languageCount;
            spanMinimum[maxVersionCount - 1] = 0;
        }
        long[] space = getSpace(spanMinimum, spanMaximum);
        long bestScore = Long.MAX_VALUE;
        UserAgent best = null;
        for (UserAgent userAgent : subversions) {
            long score = 0;
            for (int spaceIndex = 0; spaceIndex < space.length; spaceIndex++) {
                for (int markerIndex = 0; markerIndex < candidate.getMarkerCount(); markerIndex++) {
                    Marker candidateMarker = candidate.getMarker(markerIndex);
                    Marker marker = userAgent.getMarker(markerIndex);
                    long value;
                    if (candidateMarker instanceof Language) {
                        if (spaceIndex == space.length - 1) {
                            value = ((Language) candidateMarker).getDistance((Language) marker);
                        } else {
                            continue;
                        }
                    } else {
                        Version version = (Version) marker;
                        Version candidateVersion = (Version) candidateMarker;
                        if (version.length() > spaceIndex && candidateVersion.length() > spaceIndex) {
                            value = Math.abs(candidateVersion.getNumericValue(spaceIndex) - version.getNumericValue(spaceIndex));
                        } else if (candidateVersion.length() > spaceIndex) {
                            if (upper) {
                                value = spanMaximum[spaceIndex] - candidateVersion.getNumericValue(spaceIndex);
                            } else {
                                value = candidateVersion.getNumericValue(spaceIndex) - spanMinimum[spaceIndex];
                            }
                        } else if (version.length() > spaceIndex) {
                            if (upper) {
                                value = version.getNumericValue(spaceIndex) - spanMinimum[spaceIndex];
                            } else {
                                value = spanMaximum[spaceIndex] - version.getNumericValue(spaceIndex);
                            }
                        } else {
                            value = 0;
                        }
                    }
                    if (value > 0) {
                        if (score < Long.MAX_VALUE - value * space[spaceIndex]) {
                            score += value * space[spaceIndex];
                        } else {
                            score = Long.MAX_VALUE;
                        }
                    }
                }
            }
            if (score < bestScore) {
                bestScore = score;
                best = userAgent;
            }
        }
        return best;
    }

    public static Set<UserAgent> getLower(Set<UserAgent> userAgents, UserAgent candidate) {
        HashSet<UserAgent> subversions = new HashSet<UserAgent>();
        subversions.addAll(userAgents);
        for (int i = 0; i < candidate.getMarkerCount() && !subversions.isEmpty(); i++) {
            HashSet<UserAgent> nextSubversions = new HashSet<UserAgent>();
            Marker candidateMarker = candidate.getMarker(i);
            if (candidateMarker instanceof Language) {
                continue;
            } else {
                Version candidateVersion = (Version) candidateMarker;
                for (UserAgent ua : subversions) {
                    if (ua.getMarkerCount() > i) {
                        Version version = ua.getVersion(i);
                        int length = Math.min(candidate.getMarkerCount(), ua.getMarkerCount());
                        boolean below = true;
                        boolean equal = true;
                        for (int k = 0; k < length; k++) {
                            long compare = version.compareNumericValue(candidateVersion, k);
                            if (compare < 0) {
                                below = false;
                                equal = false;
                            } else if (compare > 0) {
                                equal = false;
                            }
                        }
                        if (equal) {
                            if (version.length() <= candidateVersion.length()) {
                                nextSubversions.add(ua);
                            } else {
                                if (version.isZero()) {
                                    nextSubversions.add(ua);
                                } else {
                                }
                            }
                        } else if (below) {
                            nextSubversions.add(ua);
                        } else {
                        }
                    } else {
                        nextSubversions.add(candidate);
                    }
                }
            }
            subversions = nextSubversions;
        }
        return subversions;
    }

    private long[] getSpace(long[] min, long[] max) {
        long[] space = new long[min.length];
        if (max[min.length - 1] - min[min.length - 1] == 0) {
            space[min.length - 1] = 1;
        } else {
            space[min.length - 1] = max[min.length - 1] - min[min.length - 1];
        }
        for (int i = min.length - 2; i >= 0; i--) {
            if (max[i] - min[i] == 0) {
                space[i] = space[i + 1];
            } else {
                space[i] = 1;
                for (int k = i + 1; k < min.length; k++) {
                    if (space[i] <= Long.MAX_VALUE - space[k] * (max[k] - min[k])) {
                        space[i] += space[k] * (max[k] - min[k]);
                    } else {
                        space[i] = Long.MAX_VALUE;
                    }
                }
            }
        }
        return space;
    }
}
