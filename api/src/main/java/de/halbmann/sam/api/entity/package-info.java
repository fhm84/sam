/**
 * Core domain model for musical works managed by SAM.
 *
 * <p>
 * The classification of a musical work follows a clear separation of concerns:
 * </p>
 *
 * <ul>
 *   <li>{@link de.halbmann.sam.api.entity.Genre} – what kind of musical form the work is</li>
 *   <li>{@link de.halbmann.sam.api.entity.Style} – how the work sounds stylistically</li>
 *   <li>Tags – contextual or situational metadata</li>
 * </ul>
 */
package de.halbmann.sam.api.entity;
