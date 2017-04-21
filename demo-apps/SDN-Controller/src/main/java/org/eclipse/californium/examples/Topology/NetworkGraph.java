/*
 * Copyright (C) 2015 SDN-WISE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.eclipse.californium.examples.Topology;

import java.util.HashSet;
import java.util.Observable;
import java.util.Set;

import org.eclipse.californium.examples.Model.sdnNode;
import org.eclipse.californium.examples.Model.sdnNode.Neighbour;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

/**
 * Holder of the {@code org.graphstream.graph.Graph} object which represent the
 * Topology of the wireless sensor network. The method updateMap is invoked when
 * a message with Topology updates is sent to the controller.
 *
 * @author Sebastiano Milardo
 */

public class NetworkGraph extends Observable {

    protected final Graph graph;

    public NetworkGraph() {
        graph = new MultiGraph("NetworkGraph");
        graph.setAutoCreate(true);
        graph.setStrict(false);
    }

    /**
     * Adds a edge directed edge between the two given nodes. If directed, the
     * edge goes in the 'from' 'to' direction.
     *
     * @param <T> Extends an edge
     * @param id Unique and arbitrary string identifying the edge.
     * @param from The first node identifier.
     * @param to The second node identifier.
     * @param directed Is the edge directed?
     * @return The newly created edge, an existing edge or {@code null}
     */
    public final <T extends Edge> T addEdge(final String id, final String from,
                                            final String to,
                                            final boolean directed) {
        return graph.addEdge(id, from, to, directed);
    }

    /**
     * Add a node in the graph. This acts as a factory, creating the node
     * instance automatically (and eventually using the node factory provided).
     *
     * @param <T> returns something that extends node
     * @param id Arbitrary and unique string identifying the node.
     * @return The created node (or the already existing node).
     */
    public final <T extends Node> T addNode(final String id) {
        return graph.addNode(id);
    }

    /**
     * Gets an Edge of the Graph.
     *
     * @param <T> the type of edge in the graph.
     * @param id string id value to get an Edge.
     * @return the edge of the graph
     */
    public final <T extends Edge> T getEdge(final String id) {
        return graph.getEdge(id);
    }

    /**
     * Gets the Graph contained in the NetworkGraph.
     *
     * @return returns a Graph object
     */
    public final Graph getGraph() {
        return graph;
    }


    /**
     * Gets a sdnNode of the Graph.
     *
     * @param <T> the type of node in the graph.
     * @param id string id value to get a sdnNode.
     * @return the node of the graph
     */
    public final <T extends Node> T getNode(final String id) {
        return graph.getNode(id);
    }

    /**
     * Removes an edge.
     *
     * @param <T> This method is implicitly generic and returns something which
     * extends Edge.
     * @param edge The edge to be removed
     * @return The removed edge
     */
    public final <T extends Edge> T removeEdge(final Edge edge) {
        return graph.removeEdge(edge);
    }

    /**
     * Removes a node.
     *
     * @param <T> This method is implicitly generic and returns something which
     * extends sdnNode.
     * @param node The node to be removed
     * @return The removed edge
     */
    public final <T extends Node> T removeNode(final Node node) {
        return graph.removeNode(node);
    }

    /**
     * Setups an Edge.
     *
     * @param edge the edge to setup
     * @param newLen the weight of the edge
     */
    public void setupEdge(final Edge edge, final int newLen) {
        edge.addAttribute("length", newLen);
    }

    /**
     * Setups a sdnNode.
     *
     * @param node the node to setup
     * @param batt residual charge of the node
     * @param now last time time the node was alive
     * @param addr sdnNode address
     */
    public void setupNode(final Node node, final int batt, final long now, final String addr) {
        node.addAttribute("battery", batt);
        node.addAttribute("lastSeen", now);
        node.addAttribute("nodeAddress", addr);
    }

    /**
     * Updates an existing Edge.
     *
     * @param edge the edge to setup
     * @param newLen the weight of the edge
     */
    public void updateEdge(final Edge edge, final int newLen) {
        edge.addAttribute("length", newLen);
    }

    /**
     * Invoked when a message with Topology updates is received by the
     * controller. It updates the network Topology according to the message and
     * checks if all the nodes in the network are still alive.
     *
     * @param sdnNode the sdnNode
     */
    public final synchronized void updateMap(final sdnNode sdnNode) {

        long now = sdnNode.getLastUpdate();

        int batt = sdnNode.getBatteryLevel();
        String nodeAddr = sdnNode.getAddress();

        Node node = getNode(nodeAddr);

        if (node == null) {
            node = addNode(nodeAddr);
            setupNode(node, batt, now, nodeAddr);

            for (int i = 0; i < sdnNode.getNeighboursSize(); i++) {
                Neighbour neighbour = sdnNode.getNeighbor(i);
                String neighbourAddress = neighbour.getAddress();
                if (getNode(neighbourAddress) == null) {
                    Node tmp = addNode(neighbourAddress);
                    setupNode(tmp, 0, now, neighbourAddress);
                }

                int etx = 1;
                String edgeId = neighbourAddress + "-" + nodeAddr;
                Edge edge = addEdge(edgeId, neighbourAddress, node.getId(), true);
                setupEdge(edge, etx);
            }

        } else {
            updateNode(node, batt, now);
            Set<Edge> oldEdges = new HashSet<>();
            oldEdges.addAll(node.getEnteringEdgeSet());

            for (int i = 0; i <  sdnNode.getNeighboursSize(); i++) {
                Neighbour neighbour = sdnNode.getNeighbor(i);
                String neighbourAddress = neighbour.getAddress();
                if (getNode(neighbourAddress) == null) {
                    Node tmp = addNode(neighbourAddress);
                    setupNode(tmp, 0, now, neighbourAddress);
                }

                int newEtx = 1;

                String edgeId = neighbourAddress + "-" + nodeAddr;
                Edge edge = getEdge(edgeId);
                if (edge != null) {
                    oldEdges.remove(edge);
                    int oldLen = edge.getAttribute("length");
                    updateEdge(edge, newEtx);
                } else {
                    Edge tmp = addEdge(edgeId, neighbourAddress, node.getId(), true);
                    setupEdge(tmp, newEtx);
                }
            }

            if (!oldEdges.isEmpty()) {
                for(Edge e: (Edge[])oldEdges.toArray()){
                    removeEdge(e);
                }
            }
        }
        setChanged();
        notifyObservers();

    }

    /**
     * Updates a existing sdnNode.
     *
     * @param node the node to setup
     * @param batt residual charge of the node
     * @param now last time time the node was alive
     */
    public void updateNode(final Node node, final int batt, final long now) {
        node.addAttribute("battery", batt);
        node.addAttribute("lastSeen", now);
    }
}