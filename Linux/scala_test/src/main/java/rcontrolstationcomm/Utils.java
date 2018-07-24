/*
    Copyright 2018 Benjamin Vedder	benjamin@vedder.se

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */

package rcontrolstationcomm;

import java.util.ArrayList;
import java.util.List;
import org.bridj.Pointer;
import static java.lang.System.out;

public class Utils {
	public static List<ROUTE_POINT> getRoute(int car, int mapRoute, int timeoutMs) {
		List<ROUTE_POINT> ret = new ArrayList<ROUTE_POINT>();
		
		Pointer<ROUTE_POINT> route = Pointer.allocateArray(ROUTE_POINT.class,  500);
		Pointer<Integer> len = Pointer.pointerToInts(0);
		RControlStationCommLibrary.rcsc_getRoutePoints(car, route, 
				len, 500, mapRoute, timeoutMs);
		
		int lenRes = len.get(0);
		for (int i = 0;i < lenRes;i++) {
			// Creating a new object here is necessary for some reason, otherwise
			// bridj crashes in some situation where a route is used.
			ROUTE_POINT a = new ROUTE_POINT();
			a.px(route.get(i).px());
			a.py(route.get(i).py());
			a.speed(route.get(i).speed());
			a.time(route.get(i).time());
			ret.add(a);
		}
		
		return ret;
	}
	
	public static boolean addRoute(int car, List<ROUTE_POINT> route, boolean replace,
			boolean mapOnly, int mapRoute, int timeoutMs) {
		Pointer<ROUTE_POINT> routePtr = Pointer.allocateArray(ROUTE_POINT.class, route.size());
				
		for (int i = 0;i < route.size();i++) {
			routePtr.set(i, route.get(i));
		}
		
		return RControlStationCommLibrary.rcsc_addRoutePoints(car, routePtr, route.size(), 
				replace, mapOnly, mapRoute, timeoutMs);
	}
	
	public static CAR_STATE getCarState(int car, int timeoutMs) {
		CAR_STATE st = new CAR_STATE();
		RControlStationCommLibrary.rcsc_getState(car, Pointer.pointerTo(st), timeoutMs);
		return st;
	}
	
	public static String terminalCmd(int car, String cmd, int timeoutMs) {
		String res = new String();
		
		Pointer<Byte> cmdPtr = Pointer.allocateBytes(cmd.length() + 1);
		for (int i = 0;i < cmd.length();i++) {
			cmdPtr.set(i, (byte)cmd.charAt(i));
		}
		cmdPtr.set(cmd.length(), (byte)0);
		
		Pointer<Byte> reply = Pointer.allocateBytes(500);
		reply.set(0, (byte)0);
		RControlStationCommLibrary.rcsc_sendTerminalCmd(car, cmdPtr, reply, timeoutMs);
		
		for (int i = 0;i < 500;i++) {
			if (reply.get(i) == 0) {
				break;
			}
			
			res += (char)reply.get(i).byteValue();
		}
		
		return res;
	}
	
	/**
	 * Add a fault
	 * 
	 * @param car
	 * The ID of the car
	 * 
	 * @param probe
	 * The probe to inject on
	 * 
	 * @param type
	 * The fault type. Can be
	 * NONE
	 * BITFLIP
	 * OFFSET
	 * AMPLIFICATION
	 * SET_TO
	 * 
	 * @param param
	 * The fault parameter, e.g. amplification factor, the bit to flip.
	 * 
	 * @param start
	 * The iteration where the fault should be triggered. Iterations are
	 * counted up when the fault injection function is called, usually
	 * before the variable to inject the fault on is used.
	 * 
	 * @param duration
	 * The duration of the fault in iterations.
	 * 
	 * @timeoutMs
	 * Timeout for acknowledgement in milliseconds.
	 * 
	 * @return
	 * true for success, false otherwise.
	 */
	public static boolean fiAddFault(int car, String probe, String type,
			double param, int start, int duration, int timeoutMs) {
		terminalCmd(car,
				"fi_add_fault " +
						probe + " " +
						type + " " +
						param + " " +
						start + " " +
						duration,
				timeoutMs);
		
		return true;
	}
	
	public static boolean fiSetEnabled(int car, boolean enabled, int timeoutMs) {
		String en = "0";
		if (enabled) {
			en = "1";
		}
		terminalCmd(car, "fi_set_enabled " + en, timeoutMs);
		return true;
	}
	
	public static boolean fiClearFaults(int car, int timeoutMs) {
		terminalCmd(car, "fi_clear_faults", timeoutMs);
		return true;
	}
	
	/**
	 * Reset fault iteration counter.
	 * 
	 * @param car
	 * Car ID.
	 * 
	 * @param timeoutMs
	 * Timeout in milliseconds.
	 * 
	 * @return
	 * true for success, false otherwise
	 */
	public static boolean fiResetCnt(int car, int timeoutMs) {
		terminalCmd(car, "fi_reset_cnt", timeoutMs);
		return true;
	}
	
	public static boolean resetUwbPos(int car, int timeoutMs) {
		terminalCmd(car, "pos_uwb_reset_pos", timeoutMs);
		return true;
	}
	
	public static boolean waitUntilRouteAlmostEnded(int car) {
		boolean res = true;
		double maxUwbDiff = 0.0;
		
		CAR_STATE st = getCarState(car, 1000);
		double uwbDiff = Math.sqrt(Math.pow((st.px() - st.px_uwb()), 2) +
				Math.pow((st.py() - st.py_uwb()), 2));
		if (uwbDiff > maxUwbDiff) {
			maxUwbDiff = uwbDiff;
		}
		
		while (st.ap_route_left() > 4) {
			try {
				Thread.sleep(50);
				st = getCarState(car, 1000);
				uwbDiff = Math.sqrt(Math.pow((st.px() - st.px_uwb()), 2) +
						Math.pow((st.py() - st.py_uwb()), 2));
				if (uwbDiff > maxUwbDiff) {
					maxUwbDiff = uwbDiff;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
		
		out.println("Max UWB diff: " + maxUwbDiff + " m");
		
		if (maxUwbDiff > 1.0) {
			res = false;
			out.println("[Error] Too large difference between the UWB-based"
					+ " and RTKGNSS-based positions.");
		}
		
		return res;
	}
	
	public static void followRecoveryRoute(int car, int recoveryRoute) {
		// TODO: Make sure that the route does not cross the edge when
		// the edges are defined by a convex polygon.
		
		List<ROUTE_POINT> rec = getRoute(car, recoveryRoute, 1000);
		CAR_STATE st = getCarState(car, 1000);
		ROUTE_POINT first = new ROUTE_POINT();
		first.px(st.px());
		first.py(st.py());
		first.speed(rec.get(0).speed());
		rec.add(0, first);
				
		RControlStationCommLibrary.rcsc_setAutopilotActive(car, false, 1000);
		addRoute(car, rec, false, false, -2, 1000);
		RControlStationCommLibrary.rcsc_setAutopilotActive(car, true, 1000);
		waitPolling(car, 500);
		waitUntilRouteAlmostEnded(car);
		waitPolling(car, 3000);
		
		RControlStationCommLibrary.rcsc_setAutopilotActive(car, false, 1000);
	}
	
	public static void waitPolling(int car, int ms) {
		int timeLeft = ms;
		
		try {
			while (timeLeft > 0) {
				int sleep = 50;
				if (sleep > timeLeft) {
					sleep = timeLeft;
				}
				
				Thread.sleep(sleep);
				getCarState(car, 1000);
				timeLeft -= sleep;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
