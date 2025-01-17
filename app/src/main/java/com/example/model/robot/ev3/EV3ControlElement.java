package com.example.model.robot.ev3;

import android.util.Log;

import java.util.Arrays;

/**
 * An {@code EV3ControlElement} converts control inputs into either integer values or subcommands for
 * use in direct commands.
 */
public abstract class EV3ControlElement {

    public final int[] port;
    final int maxPower;

    /**
     * Constructor for an EV3ControlElement
     *
     * @param ports    EV3 motor ports, e.g. 1, 2, 4 or 8 for port A, B, C and D respectively
     * @param maxPower maximum motor power, capped to [0, 100]
     */
    protected EV3ControlElement(int[] ports, int maxPower) {
        this.maxPower = Math.max(Math.min(maxPower, 100), 0); // limit between 0 and 100
        this.port = ports;
    }

    /**
     * @param value an int
     * @return int as byte array
     */
    static byte[] toByteArray(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
    }

    /**
     * @param value param value
     * @return parameter for direct command as byte array
     */
    static byte[] LCX(int value) {
        byte[] r;
        byte[] t = toByteArray(value);
        if (-32767 <= value && value <= 32767) {
            r = new byte[]{
                    (byte) 0x82, t[2], t[3]
            };
        } else {
            r = new byte[]{
                    (byte) 0x83, t[0], t[1], t[2], t[3] // TODO DO THESE HAVE TO BE LITTLE ENDIAN OR BIG???
            };
        }
        return r;
    }

    /**
     * @param input controlling input
     * @return power for use in ev3 direct command
     */
    protected abstract byte[] getMotorPower(int... input);

    /**
     * @param input controlling input
     * @return part of direct command
     */
    protected abstract byte[] getCommand(int... input);

    /**
     * @return type of control element, e.g. {@code "joystick"}, {@code "slider"} or {@code "button"}
     */
    public abstract String getType();

    public abstract int[] getPort();

    /**
     * @param x value in [0,1], percentage of maxPower
     * @return scaled power
     */
    final byte scalePower(float x) {
        return (byte) (x * maxPower);
    }

    protected static class Joystick extends EV3ControlElement {

        /**
         * Constructor for a joystick {@code EV3ControlElement}
         *
         * @param ports    EV3 motor ports, e.g. 1, 2, 4 or 8 for port A, B, C and D respectively, two values are required
         * @param maxPower maximum motor power, capped to [0, 100]
         */
        Joystick(int[] ports, int maxPower) {
            super(ports, maxPower);
        }

        @Override
        protected byte[] getMotorPower(int... input) {
            Log.d("Joystick", Arrays.toString(input));
            float right = 0.0f;
            float left = 0.0f;

            int angle = input[0], strength = input[1];

            if (angle >= 0 &&
                    angle < 90) { //0°-89°
                right = -100 + angle * 20 / 9.0f; //-100 to 100
                left = 100; //100 to 100

            } else if (angle >= 90 &&
                    angle < 180) { //90°-179°
                right = 100; //100 to 100
                left = 100 - (angle - 90) * 20 / 9.0f; //100 to -100

            } else if (angle >= 180 &&
                    angle < 270) { //180°-269°
                right = 100 - (angle - 180) * 20 / 9.0f; //50 to -100
                left = -100; //-100 to -100

            } else if (angle >= 270 &&
                    angle <= 360) {//270°-359°
                right = -100; //-100 to -100
                left = -100 + (angle - 270) * 20 / 9.0f; //-100 to 100
            }

            byte[] r = new byte[2];

            r[0] = scalePower(right * strength / 10000);
            r[1] = scalePower(left * strength / 10000);

            return r;
        }

        @Override
        protected byte[] getCommand(int... input) {
            // A4|00|0p|81:po
            //  0  1  2  3  4
            // 0 opcode for output power
            // 1 filler
            // 2 p = port
            // 3 predefined prefix
            // 4 po = power

            byte[] power = getMotorPower(input);

            byte[] r = new byte[10];
            r[0] = (byte) 0xA4;
            r[1] = (byte) 0x00;
            r[2] = (byte) port[0];
            r[3] = (byte) 0x81;
            r[4] = power[0];
            r[5] = (byte) 0xA4;
            r[6] = (byte) 0x00;
            r[7] = (byte) port[1];
            r[8] = (byte) 0x81;
            r[9] = power[1];
            return r;
        }

        @Override
        public String getType() {
            return "joystick";
        }

        @Override
        public int[] getPort() {
            return port;
        }

    }

    protected static class Slider extends EV3ControlElement {

        /**
         * Constructor for a slider {@code EV3ControlElement}
         *
         * @param ports    EV3 motor ports, e.g. 1, 2, 4 or 8 for port A, B, C and D respectively, one value are required
         * @param maxPower maximum motor power, capped to [0, 100]
         */
        Slider(int[] ports, int maxPower) {
            super(ports, maxPower);
        }

        @Override
        protected byte[] getMotorPower(int... input) {
            float tmp = (input[0] - 50) / 50.0f;
            return new byte[]{
                    scalePower(tmp)
            };
        }

        @Override
        protected byte[] getCommand(int... input) {
            Log.d("slider", input[0] + "");
            byte[] power = getMotorPower(input);

            byte[] r = new byte[5];
            r[0] = (byte) 0xA4;
            r[1] = (byte) 0x00;
            r[2] = (byte) port[0];
            r[3] = (byte) 0x81;
            r[4] = power[0];

            return r;
        }

        @Override
        public String getType() {
            return "slider";
        }

        @Override
        public int[] getPort() {
            return port;
        }

    }

    protected static class Button extends EV3ControlElement {

        private final int duration;
        /**
         * duration parameter in direct command may be of variable length
         * (i.e. 100ms would be 1 byte, 5000 would be 3 bytes(?))
         */
        private final byte[] t;
        /**
         * Time button was last pressed
         */
        private long pressedT = -1;

        /**
         * Constructor for a joystick {@code EV3ControlElement}
         *
         * @param ports    EV3 motor ports, e.g. 1, 2, 4 or 8 for port A, B, C and D respectively, one value are required
         * @param maxPower maximum motor power, capped to [0, 100]
         * @param duration duration of motor action, currently between 1 and 5000 ms
         */
        Button(int[] ports, int maxPower, int duration) {
            super(ports, maxPower);
            this.duration = Math.max(Math.min(duration, 5000), 1);
            t = LCX(duration - 100);
        }

        @Override
        protected byte[] getMotorPower(int... input) {
            return new byte[]{0};
        }

        @Override
        protected byte[] getCommand(int... input) {
            long current = System.currentTimeMillis();
            if (input[0] == 1 && (current - pressedT >= duration))
            // doesn't create command, if last sent less than duration ago (can't stack button commands)
            {
                pressedT = current;

                byte[] r = new byte[9 + t.length];

                r[0] = (byte) 0xAD; // op code for motor output for a specified duration
                r[1] = (byte) 0x00;
                r[2] = (byte) port[0];
                r[3] = (byte) 0x81;
                r[4] = (byte) maxPower;
                r[5] = (byte) 0x81;
                r[6] = 100; // ramp up of 100ms
                System.arraycopy(t, 0, r, 7, t.length); // rest of duration
                r[7 + t.length] = 0;
                r[8 + t.length] = 1; // 1 is BRAKE, 0 is FLOAT

                return r;
            }
            return new byte[]{
                    0x01 // nop
            };
        }

        @Override
        public String getType() {
            return "button";
        }

        @Override
        public int[] getPort() {
            return port;
        }

    }
}
