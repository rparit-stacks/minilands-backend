/**
 * Service layer contracts (SOLID):
 * <ul>
 *   <li><b>S</b> — one interface per business capability</li>
 *   <li><b>O</b> — extend via new implementations, not by changing interfaces</li>
 *   <li><b>L</b> — implementations honor contracts; use DTOs consistently</li>
 *   <li><b>I</b> — investor vs admin surfaces split (e.g. {@code UserAuthService} / {@code AdminAuthService})</li>
 *   <li><b>D</b> — controllers and schedulers depend on these interfaces, not concrete classes</li>
 * </ul>
 */
package com.minilands.backend.service;
