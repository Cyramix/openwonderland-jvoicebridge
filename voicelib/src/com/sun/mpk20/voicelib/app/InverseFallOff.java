/*
 * Copyright 2007 Sun Microsystems, Inc.
 *
 * This file is part of jVoiceBridge.
 *
 * jVoiceBridge is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License version 2 as 
 * published by the Free Software Foundation and distributed hereunder 
 * to you.
 *
 * jVoiceBridge is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Sun designates this particular file as subject to the "Classpath"
 * exception as provided by Sun in the License file that accompanied this 
 * code. 
 */

package com.sun.mpk20.voicelib.app;

public class InverseFallOff extends FallOffFunction {

    public double getVolume(double distance) {
	/*
	 * Calculate how far distance is from 
	 * the zero volume radius.  Values range from 0 to 1.
	 */
	double d = distance / attenuationDistance;

	/*
	 * Multiply the distance by 100 and convert to an int
	 * so that a distance of 0 will become 0, .01, 1, etc.
	 *
	 * Now apply a function which is 1 when distance is 0
	 * and zero (or near zero) when distance is 1.  
	 *
	 * f(0) = 1;
	 * f(1) = fallOff * f(0);
	 * f(2) = fallOff * f(1);
	 * ...
	 * 
	 * In general f(x) = fallOff ** x;
	 *
	 * When fallOff is < 1, f(x) will decrease as x increases.
	 */
	int iD = (int) (d * 100);

	double v = Math.pow(fallOff, iD);

	logger.finer("InverseFallOff d " + round(distance) 
	   + " fvr " + fullVolumeRadius + " zvr " + zeroVolumeRadius
	   + " a " + round(attenuationDistance) 
	   + " d/a " + round(distance / attenuationDistance) 
	   + " falloff " + round(fallOff) + " iD " + iD 
	   + " v " + round(v));

        return v;
    }
	
}