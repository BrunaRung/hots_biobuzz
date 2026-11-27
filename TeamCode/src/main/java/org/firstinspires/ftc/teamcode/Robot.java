package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * All of the robot's hardware, shared by the TeleOp, the autonomous and the test OpMode.
 *
 * Driver Station configuration (names must match exactly):
 *
 *   Control Hub    motor 0   rightBack     REV UltraPlanetary HD Hex
 *   Control Hub    motor 1   leftBack      REV UltraPlanetary HD Hex
 *   Control Hub    motor 2   rightFront    REV UltraPlanetary HD Hex
 *   Control Hub    motor 3   leftFront     REV UltraPlanetary HD Hex
 *   Control Hub    servo 0   feeder        Continuous Rotation Servo
 *   Control Hub    servo 1   pollenPusher  Servo
 *   Expansion Hub  motor 0   flywheel      REV UltraPlanetary HD Hex
 *   Expansion Hub  motor 1   intake        REV Core Hex
 *
 * "Forward" always means the intake side of the robot.
 */
public class Robot {

    // ---- Directions. Run "Motor Test" and flip any that turn the wrong way. ----
    static final DcMotorSimple.Direction LEFT_DRIVE_DIRECTION = DcMotorSimple.Direction.FORWARD;
    static final DcMotorSimple.Direction RIGHT_DRIVE_DIRECTION = DcMotorSimple.Direction.REVERSE;
    static final DcMotorSimple.Direction FLYWHEEL_DIRECTION = DcMotorSimple.Direction.FORWARD;
    static final DcMotorSimple.Direction INTAKE_DIRECTION = DcMotorSimple.Direction.REVERSE;   // flipped after testing on the robot
    static final DcMotorSimple.Direction FEEDER_DIRECTION = DcMotorSimple.Direction.FORWARD;

    // ---- Tuning. Starting values are the ones REV publishes for this starter bot. ----
    /** Flywheel speed for a shot, in encoder ticks per second at the motor. */
    static final double DEFAULT_TARGET_VELOCITY = 1160;
    static final double MIN_TARGET_VELOCITY = 400;
    static final double MAX_TARGET_VELOCITY = 2400;
    /** Feeding starts once the flywheel is this close to target... */
    static final double FEED_START_TOLERANCE = 20;
    /** ...and pauses when it is further away than this, so it does not stutter. */
    static final double FEED_STOP_TOLERANCE = 50;
    static final double FLYWHEEL_REVERSE_POWER = -0.55;

    static final double PUSHER_UP = 1.0;
    static final double PUSHER_DOWN = 0.0;

    final DcMotorEx rightBack;
    final DcMotorEx leftBack;
    final DcMotorEx rightFront;
    final DcMotorEx leftFront;
    final DcMotorEx flywheel;
    final DcMotorEx intake;
    final CRServo feeder;
    final Servo pollenPusher;

    private double targetVelocity = DEFAULT_TARGET_VELOCITY;
    private boolean feeding = false;

    /** Finds and configures the hardware. Nothing moves: robots must stay still after INIT. */
    public Robot(HardwareMap hardwareMap) {
        rightBack = hardwareMap.get(DcMotorEx.class, "rightBack");
        leftBack = hardwareMap.get(DcMotorEx.class, "leftBack");
        rightFront = hardwareMap.get(DcMotorEx.class, "rightFront");
        leftFront = hardwareMap.get(DcMotorEx.class, "leftFront");
        flywheel = hardwareMap.get(DcMotorEx.class, "flywheel");
        intake = hardwareMap.get(DcMotorEx.class, "intake");
        feeder = hardwareMap.get(CRServo.class, "feeder");
        pollenPusher = hardwareMap.get(Servo.class, "pollenPusher");

        leftFront.setDirection(LEFT_DRIVE_DIRECTION);
        leftBack.setDirection(LEFT_DRIVE_DIRECTION);
        rightFront.setDirection(RIGHT_DRIVE_DIRECTION);
        rightBack.setDirection(RIGHT_DRIVE_DIRECTION);
        flywheel.setDirection(FLYWHEEL_DIRECTION);
        intake.setDirection(INTAKE_DIRECTION);
        feeder.setDirection(FEEDER_DIRECTION);

        for (DcMotorEx motor : new DcMotorEx[]{leftFront, leftBack, rightFront, rightBack, intake}) {
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        // The flywheel is velocity controlled, so its encoder cable must be plugged in.
        flywheel.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        flywheel.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        // One bulk read per loop instead of one read per sensor.
        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
    }

    // ---- Drivetrain ----

    /** Robot-relative mecanum drive. Positive: forward (intake side), strafe right, turn clockwise. */
    public void drive(double forward, double strafe, double turn) {
        double lf = forward + strafe + turn;
        double rf = forward - strafe - turn;
        double lb = forward - strafe + turn;
        double rb = forward + strafe - turn;

        // Scale everything down together if any wheel is over full power.
        double max = Math.max(1.0, Math.max(
                Math.max(Math.abs(lf), Math.abs(rf)),
                Math.max(Math.abs(lb), Math.abs(rb))));

        leftFront.setPower(lf / max);
        rightFront.setPower(rf / max);
        leftBack.setPower(lb / max);
        rightBack.setPower(rb / max);
    }

    // ---- Launcher ----

    public void spinUp() {
        flywheel.setVelocity(targetVelocity);
    }

    public void reverseFlywheel() {
        flywheel.setPower(FLYWHEEL_REVERSE_POWER);
    }

    /** Call every loop while launching. Returns true while pollen should be fed. */
    public boolean spinUpAndCheckReady() {
        spinUp();
        double error = Math.abs(getFlywheelVelocity() - targetVelocity);
        feeding = error <= (feeding ? FEED_STOP_TOLERANCE : FEED_START_TOLERANCE);
        return feeding;
    }

    public void stopFlywheel() {
        feeding = false;
        flywheel.setPower(0);
    }

    /** Forgets that we were feeding, so the next launch waits for full speed again. */
    public void resetFeeding() {
        feeding = false;
    }

    public double getFlywheelVelocity() {
        return flywheel.getVelocity();
    }

    public double getTargetVelocity() {
        return targetVelocity;
    }

    public void adjustTargetVelocity(double change) {
        targetVelocity = Math.max(MIN_TARGET_VELOCITY,
                Math.min(MAX_TARGET_VELOCITY, targetVelocity + change));
    }

    // ---- Intake, feeder, pusher ----

    public void setIntake(double power) {
        intake.setPower(power);
    }

    public void setFeeder(double power) {
        feeder.setPower(power);
    }

    public void pusherUp() {
        pollenPusher.setPosition(PUSHER_UP);
    }

    public void pusherDown() {
        pollenPusher.setPosition(PUSHER_DOWN);
    }

    public void stopAll() {
        drive(0, 0, 0);
        stopFlywheel();
        setIntake(0);
        setFeeder(0);
    }
}
