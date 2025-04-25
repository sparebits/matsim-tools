/*
 * Node
 * @author : neiko.neikov
 * @created : 3.03.25 г., Monday
 */
package com.sparebits.matsim.model;

import java.io.Serializable;


public record Node(long id, double x, double y) implements Serializable {
}
