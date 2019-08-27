/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package taxi.od.solver;

import java.io.Serializable;

/**
 *
 * @author Luciano
 */
public class taxi_trip_array implements Serializable {
    
    private taxi_Trip_Instance [] taxi_trips;
    
    public taxi_trip_array(taxi_Trip_Instance [] t){
    
        taxi_trips = t;
    
    }

    /**
     * @return the taxi_trips
     */
    public taxi_Trip_Instance[] getTaxi_trips() {
        return taxi_trips;
    }

    /**
     * @param taxi_trips the taxi_trips to set
     */
    public void setTaxi_trips(taxi_Trip_Instance[] taxi_trips) {
        this.taxi_trips = taxi_trips;
    }
    
    
}
