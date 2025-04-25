/*
 * Link
 * @author : neiko.neikov
 * @created : 3.03.25 г., Monday
 */
package com.sparebits.matsim.model;

import java.io.Serializable;


public record Link(long id, Node from, Node to) implements Serializable {
}
