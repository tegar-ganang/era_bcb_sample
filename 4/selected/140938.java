package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.dsm2.input.model.Channel;
import gov.ca.dsm2.input.model.Channels;
import gov.ca.dsm2.input.model.Node;
import gov.ca.dsm2.input.model.Nodes;
import gov.ca.dsm2.input.model.XSection;
import gov.ca.dsm2.input.model.XSectionLayer;
import gov.ca.dsm2.input.model.XSectionProfile;
import gov.ca.modeling.maps.elevation.client.model.DataPoint;
import gov.ca.modeling.maps.elevation.client.model.GeomUtils;
import gov.ca.modeling.maps.elevation.client.model.Geometry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.gwt.maps.client.geom.LatLng;

/**
 * FIXME: figure out how these methods can move back to the model. the only map
 * dependency seems to LatLng which is easy to break with a convention of a
 * double[] array of length=2 of lat,lng
 * 
 * @author nsandhu
 * 
 */
public class ModelUtils {

    private static int length;

    /**
	 * Return the flowline of the entire channel, combining the internal points
	 * with the upnode and downode points
	 * 
	 * @param channel
	 * @param upNode
	 * @param downNode
	 * @return
	 */
    public static LatLng[] getPointsForChannel(Channel channel, Node upNode, Node downNode) {
        LatLng[] points = null;
        LatLng upPoint = LatLng.newInstance(upNode.getLatitude(), upNode.getLongitude());
        LatLng downPoint = LatLng.newInstance(downNode.getLatitude(), downNode.getLongitude());
        List<double[]> latLngPoints = channel.getLatLngPoints();
        if ((latLngPoints != null) && (latLngPoints.size() > 0)) {
            int size = latLngPoints.size();
            points = new LatLng[size + 2];
            for (int i = 1; i < points.length - 1; i++) {
                double[] ds = latLngPoints.get(i - 1);
                points[i] = LatLng.newInstance(ds[0], ds[1]);
            }
        } else {
            points = new LatLng[2];
        }
        points[0] = upPoint;
        points[points.length - 1] = downPoint;
        return points;
    }

    /**
	 * @param channel
	 * @param upNode
	 * @param downNode
	 * @return
	 * @deprecated
	 * @see #getPointsForChannel(Channel, Node, Node)
	 */
    public static LatLng[] getChannelOutlinePoints(Channel channel, Node upNode, Node downNode) {
        return getPointsForChannel(channel, upNode, downNode);
    }

    /**
	 * Calculates the profile from the xsection characteristics, the flow line
	 * of the channel and using the relative distance along channel length to
	 * calculate the position of the xsection line end points
	 * 
	 * @param xSection
	 * @param channel
	 * @param upNode
	 * @param downNode
	 * @return
	 */
    public static XSectionProfile calculateProfileFrom(XSection xSection, Channel channel, Node upNode, Node downNode) {
        XSectionProfile profile = new XSectionProfile();
        profile.setChannelId(Integer.parseInt(xSection.getChannelId()));
        profile.setDistance(xSection.getDistance());
        List<double[]> endPoints = new ArrayList<double[]>();
        profile.setEndPoints(endPoints);
        LatLng[] endPointLatLngs = calculateEndPoints(xSection, channel, upNode, downNode);
        for (LatLng endPointLatLng : endPointLatLngs) {
            double[] point = new double[2];
            point[0] = endPointLatLng.getLatitude();
            point[1] = endPointLatLng.getLongitude();
            endPoints.add(point);
        }
        List<double[]> profilePoints = new ArrayList<double[]>();
        profile.setProfilePoints(profilePoints);
        double maxWidth = getMaxTopWidth(xSection);
        for (XSectionLayer layer : xSection.getLayers()) {
            double w = layer.getTopWidth();
            double[] point1 = new double[2];
            double[] point2 = new double[2];
            point1[0] = maxWidth / 2 - w / 2;
            point1[1] = layer.getElevation();
            if (w > 0) {
                point2[0] = maxWidth / 2 + w / 2;
                point2[1] = layer.getElevation();
            }
            profilePoints.add(0, point1);
            if (w > 0) {
                profilePoints.add(profilePoints.size(), point2);
            }
        }
        return profile;
    }

    /**
	 * Returns the lat/lng of the endpoints that would defined the xsection.
	 * <p>
	 * This method is useful when the profile is not available and the end
	 * points of a pseudo-profile needs to be generated from the xsection. This
	 * can be used to maintain the physical location of the xsections when the
	 * flowline is changed.
	 * </p>
	 * 
	 * @param xSection
	 * @param channel
	 * @param upNode
	 * @param downNode
	 * @return an array of the beginning and ending lat/lng (length=2) of the
	 *         xsection line on a map
	 */
    public static LatLng[] calculateEndPoints(XSection xSection, Channel channel, Node upNode, Node downNode) {
        LatLng[] channelOutlinePoints = getChannelOutlinePoints(channel, upNode, downNode);
        double distance = xSection.getDistance();
        distance = channel.getLength() * distance;
        int segmentIndex = GeomUtils.findSegmentAtDistance(channelOutlinePoints, distance);
        LatLng point1 = channelOutlinePoints[segmentIndex];
        LatLng point2 = channelOutlinePoints[segmentIndex + 1];
        double segmentDistance = GeomUtils.findDistanceUptoSegment(segmentIndex, channelOutlinePoints);
        LatLng point0 = GeomUtils.findPointAtDistance(point1, point2, distance - segmentDistance);
        double slope = GeomUtils.getSlopeBetweenPoints(point1, point2);
        double width = getMaxTopWidth(xSection);
        return GeomUtils.getLineWithSlopeOfLengthAndCenteredOnPoint(-1 / slope, width, point0);
    }

    /**
	 * Calculates the maximum top width in this xsection FIXME: needs to move to
	 * the model class XSection
	 * 
	 * @param xSection
	 * @return the top width
	 */
    public static double getMaxTopWidth(XSection xSection) {
        double width = Double.MIN_VALUE;
        for (XSectionLayer layer : xSection.getLayers()) {
            width = Math.max(width, layer.getTopWidth());
        }
        return width;
    }

    /**
	 * Returns the distance from up node of channel to the intersection of the
	 * channel flowline and the profile endPoints line.
	 * 
	 * @param profile
	 * @param channel
	 * @param upNode
	 * @param downNode
	 * @return distance or -1 if no intersection found
	 */
    public static double getIntersectionDistanceFromUpstream(XSectionProfile profile, Channel channel, Node upNode, Node downNode) {
        double distance = -1;
        LatLng[] pointsForChannel = getPointsForChannel(channel, upNode, downNode);
        List<double[]> endPoints = profile.getEndPoints();
        double xp0 = endPoints.get(0)[0];
        double yp0 = endPoints.get(0)[1];
        double xp1 = endPoints.get(1)[0];
        double yp1 = endPoints.get(1)[1];
        double[] intersection = new double[2];
        for (int i = 1; i < pointsForChannel.length; i++) {
            LatLng p1 = pointsForChannel[i - 1];
            LatLng p2 = pointsForChannel[i];
            int findLineSegmentIntersection = Geometry.findLineSegmentIntersection(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), xp0, yp0, xp1, yp1, intersection);
            LatLng ip = LatLng.newInstance(intersection[0], intersection[1]);
            if (findLineSegmentIntersection == 1) {
                LatLng intersectionPoint = LatLng.newInstance(intersection[0], intersection[1]);
                distance = GeomUtils.findDistanceUptoSegment(i, pointsForChannel);
                distance = distance - getLengthInFeet(p2.distanceFrom(intersectionPoint));
                break;
            }
        }
        return distance;
    }

    public static double getTopWidthAtDepth(XSection xsection, double depth) {
        ArrayList<XSectionLayer> layers = xsection.getLayers();
        double bottomElevation = layers.get(0).getElevation();
        return bottomElevation + depth;
    }

    public static double getMaxDepth(XSection xsection) {
        ArrayList<XSectionLayer> layers = xsection.getLayers();
        double minElevation = layers.get(0).getElevation();
        double maxElevation = layers.get(layers.size() - 1).getElevation();
        return maxElevation - minElevation;
    }

    public static double getTopWidthAtElevation(XSection xsection, double elevation) {
        ArrayList<XSectionLayer> layers = xsection.getLayers();
        double previousElevation = 0;
        double previousTopWidth = 0;
        for (XSectionLayer xSectionLayer : layers) {
            if (elevation < xSectionLayer.getElevation()) {
                return interpolateLinearly(elevation, xSectionLayer.getTopWidth(), xSectionLayer.getElevation(), previousElevation, previousTopWidth);
            }
            previousElevation = xSectionLayer.getElevation();
            previousTopWidth = xSectionLayer.getTopWidth();
        }
        return 0;
    }

    public static double interpolateLinearly(double elevation, double thisTopWidth, double thisElevation, double previousElevation, double previousTopWidth) {
        return (elevation - previousElevation) * (thisTopWidth - previousTopWidth) / (thisElevation - previousElevation) + previousTopWidth;
    }

    public static double getLengthInFeet(double length) {
        return Math.round(length * 3.2808399 * 100) / 100;
    }

    /**
	 * Returns a list of comma separated channel ids for the given node. with
	 * down channels followed by upchannels
	 * 
	 * @param node
	 * @return
	 */
    public static String getChannelsConnectedTo(Channels channels, Node node) {
        String upChannels = channels.getUpChannels(node.getId());
        String downChannels = channels.getDownChannels(node.getId());
        if ((upChannels == null) && (downChannels == null)) {
            return null;
        }
        if (upChannels == null) {
            return downChannels;
        }
        if (downChannels == null) {
            return upChannels;
        }
        return downChannels + "," + upChannels;
    }

    /**
	 * Get channels that have the given nodes as their end points
	 * 
	 * @param channel
	 * @param nodes
	 * @param xSection
	 * @param oldLength
	 */
    public static ArrayList<String> getChannelsWithNodes(Node node1, Node node2, Channels channels) {
        ArrayList<String> list = new ArrayList<String>();
        String channels1 = getChannelsConnectedTo(channels, node1);
        if (channels1 == null) {
            return null;
        }
        String[] list1 = channels1.split(",");
        String channels2 = getChannelsConnectedTo(channels, node2);
        if (channels2 == null) {
            return null;
        }
        String[] list2 = channels2.split(",");
        HashMap<String, String> commonList = new HashMap<String, String>();
        for (String element : list1) {
            commonList.put(element, element);
        }
        for (String element : list2) {
            if (commonList.containsKey(element)) {
                list.add(element);
            }
        }
        return list;
    }

    public static void updateXSectionPosition(Channel channel, Nodes nodes, XSection xSection, double oldLength) {
        XSectionProfile profile = xSection.getProfile();
        Node upNode = nodes.getNode(channel.getUpNodeId());
        Node downNode = nodes.getNode(channel.getDownNodeId());
        if (profile == null) {
            int newLength = channel.getLength();
            channel.setLength((int) oldLength);
            profile = calculateProfileFrom(xSection, channel, upNode, downNode);
            channel.setLength(newLength);
        }
        double distance = ModelUtils.getIntersectionDistanceFromUpstream(profile, channel, upNode, downNode);
        if ((distance >= 0) && (distance <= channel.getLength())) {
            double dratio = distance / channel.getLength();
            dratio = Math.round(dratio * 1000) / 1000.0;
            profile.setDistance(dratio);
            xSection.setDistance(dratio);
        }
    }

    public static Channel getChannelForXSection(XSection xSection, Channels channels) {
        String channelId = xSection.getChannelId();
        return channels.getChannel(channelId);
    }

    /**
	 * Generates a list of xsections arranged from upnode to down of the
	 * channel. <br/>
	 * 
	 * The xsections returned are rectangular 1000 ft channels from -10 ft
	 * bottom elevation to 10 ft elevation layer (only 2 layers).<br/>
	 * 
	 * Each channel has a xsection at upnode (0.05 normalized distance) and
	 * downnode (0.95 normalized distance) and atleast one xsection in the
	 * middle. The strategy is to have xsections in the middle (other than the
	 * two at each end ) which are less than 5000 ft apart and are equally
	 * spaced. This is based on the DSM2 computation distance and is
	 * configurable.
	 * 
	 * @param channel
	 * @param upNode
	 * @param downNode
	 * @return
	 */
    public static List<XSection> generateCrossSections(Channel channel, Node upNode, Node downNode, double minDistance, double maxWidth) {
        double length = channel.getLength();
        int numberInterior = (int) Math.floor(length / minDistance) + 1;
        double xsectionSpacing = length / (numberInterior + 1);
        ArrayList<XSection> xsections = new ArrayList<XSection>();
        xsections.add(createXSection(channel.getId(), 0.05, maxWidth));
        double distance = 0.0;
        for (int i = 0; i < numberInterior; i++) {
            distance += xsectionSpacing;
            double distanceRatio = Math.round(distance / length * 1000) / 1000.0;
            xsections.add(createXSection(channel.getId(), distanceRatio, maxWidth));
        }
        xsections.add(createXSection(channel.getId(), 0.95, maxWidth));
        return xsections;
    }

    public static XSection createXSection(String channelId, double normalizedDistance, double maxWidth) {
        XSection xsection = new XSection();
        xsection.setChannelId(channelId);
        XSectionLayer layer1 = new XSectionLayer();
        layer1.setArea(0);
        layer1.setElevation(-10);
        layer1.setTopWidth(maxWidth);
        layer1.setWettedPerimeter(maxWidth);
        XSectionLayer layer2 = new XSectionLayer();
        layer2.setArea(20 * maxWidth);
        layer2.setElevation(10);
        layer2.setTopWidth(maxWidth);
        layer2.setWettedPerimeter(maxWidth + 40);
        xsection.addLayer(layer1);
        xsection.addLayer(layer2);
        xsection.setDistance(normalizedDistance);
        return xsection;
    }

    public static List<DataPoint> getTrimmedPoints(List<DataPoint> xsProfile) {
        int trimBeginIndex = extractInflectionIndex(xsProfile, true);
        int trimEndIndex = extractInflectionIndex(xsProfile, false);
        if (trimBeginIndex == trimEndIndex) {
            trimBeginIndex = 0;
            trimEndIndex = xsProfile.size() - 1;
        }
        ArrayList<DataPoint> profile = new ArrayList<DataPoint>();
        for (int i = trimBeginIndex; i <= trimEndIndex; i++) {
            DataPoint p = xsProfile.get(i);
            DataPoint np = new DataPoint();
            np.x = p.x;
            np.y = p.y;
            np.z = p.z;
            profile.add(np);
        }
        return profile;
    }

    public static int extractInflectionIndex(List<DataPoint> profile, boolean forwards) {
        int inflectionIndex = 0;
        if (!forwards) {
            inflectionIndex = profile.size() - 1;
        }
        double diffThreshold = 3;
        double diff = 0.0;
        double lastVal = profile.get(inflectionIndex).z;
        double lastDiff = 0.0;
        int index = 1;
        if (!forwards) {
            index = profile.size() - 1;
        }
        while ((index >= 0) && (index < profile.size())) {
            DataPoint p = profile.get(index);
            diff = p.z - lastVal;
            if (Math.abs(diff - lastDiff) > diffThreshold) {
                if ((diff < 0) && (lastDiff >= 0)) {
                    if (forwards) {
                        inflectionIndex = index - 1;
                    } else {
                        inflectionIndex = index + 1;
                    }
                    break;
                }
            }
            lastDiff = diff;
            lastVal = p.z;
            if (forwards) {
                index++;
            } else {
                index--;
            }
        }
        return inflectionIndex;
    }

    public static XSectionProfile getOrCalculateXSectionalProfile(XSection xsection, Channel channel, Nodes nodes) {
        XSectionProfile profileFrom = xsection.getProfile();
        if (profileFrom == null) {
            Node upNode = nodes.getNode(channel.getUpNodeId());
            Node downNode = nodes.getNode(channel.getDownNodeId());
            profileFrom = ModelUtils.calculateProfileFrom(xsection, channel, upNode, downNode);
            xsection.setProfile(profileFrom);
        }
        return profileFrom;
    }
}
