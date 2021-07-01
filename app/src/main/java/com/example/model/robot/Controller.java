package com.example.model.robot;

import java.util.Map;

public interface Controller {

    /**
     * uses given controller input to send command to robot
     *
     * @param input input
     */
    void sendInput(int... input);

    Map<String, Integer> getControlCounts();

}
