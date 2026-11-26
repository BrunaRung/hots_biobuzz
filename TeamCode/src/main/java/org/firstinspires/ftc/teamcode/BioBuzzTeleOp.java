package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Driver control, one gamepad (Xbox name / PlayStation name):
 *
 *   Left stick            drive and strafe
 *   Right stick X         turn
 *   Left trigger          slow down (further = slower)
 *   Left bumper  (hold)   launch: spin up, then feed whenever the flywheel is at speed
 *   Right bumper (hold)   intake in
 *   Right trigger (hold)  intake out (always wins, use it to drop a 5th piece)
 *   Y / Triangle (hold)   spin flywheel without feeding
 *   X / Square   (hold)   reverse flywheel to clear a jam
 *   Dpad up / down        feeder forward / reverse
 *   Dpad right / left     flywheel target +20 / -20 ticks/s
 *   A / Cross             pollen pusher down
 *   B / Circle            pollen pusher up
 */
@TeleOp(name = "BIOBUZZ TeleOp", group = "BIOBUZZ")
public class BioBuzzTeleOp extends LinearOpMode {

    static final double VELOCITY_STEP = 20;
    static final double SLOWEST_SPEED = 0.3;
    /** Right trigger must be pressed past half way, so a resting finger or a worn trigger does nothing. */
    static final double TRIGGER_THRESHOLD = 0.5;
    /** Set to false if the intake should only ever run on right bumper / right trigger. */
    static final boolean INTAKE_HELPS_LAUNCH = true;

    private Robot robot;
    /** True from the first shot until left bumper is released, so the intake does not start and stop. */
    private boolean launchStarted = false;
    private String intakeReason = "off";

    @Override
    public void runOpMode() {
        robot = new Robot(hardwareMap);

        telemetry.addLine("Ready. Press START.");
        telemetry.update();
        waitForStart();
        if (isStopRequested()) return;

        robot.pusherUp();

        while (opModeIsActive()) {
            driveControls();
            boolean feedingShot = launcherControls();
            intakeControls(feedingShot);
            pusherControls();
            showTelemetry(feedingShot);
        }

        robot.stopAll();
    }

    private void driveControls() {
        double speed = 1.0 - (1.0 - SLOWEST_SPEED) * gamepad1.left_trigger;
        // Stick Y is negative when pushed up.
        robot.drive(
                -gamepad1.left_stick_y * speed,
                gamepad1.left_stick_x * speed,
                gamepad1.right_stick_x * speed);
    }

    /** Returns true while a launch is feeding pollen. */
    private boolean launcherControls() {
        if (gamepad1.dpadRightWasPressed()) robot.adjustTargetVelocity(VELOCITY_STEP);
        if (gamepad1.dpadLeftWasPressed()) robot.adjustTargetVelocity(-VELOCITY_STEP);

        boolean feedingShot = false;

        if (gamepad1.x) {
            robot.resetFeeding();
            robot.reverseFlywheel();
        } else if (gamepad1.left_bumper) {
            feedingShot = robot.spinUpAndCheckReady();
        } else if (gamepad1.y) {
            robot.resetFeeding();
            robot.spinUp();
        } else {
            robot.stopFlywheel();
        }

        // The manual feeder buttons override the automatic feed.
        if (gamepad1.dpad_up) {
            robot.setFeeder(1);
        } else if (gamepad1.dpad_down) {
            robot.setFeeder(-1);
        } else {
            robot.setFeeder(feedingShot ? 1 : 0);
        }
        return feedingShot;
    }

    private void intakeControls(boolean feedingShot) {
        // Remember that a launch has begun, and forget it as soon as left bumper is let go.
        if (!gamepad1.left_bumper || gamepad1.x) {
            launchStarted = false;
        } else if (feedingShot) {
            launchStarted = true;
        }

        if (gamepad1.right_trigger > TRIGGER_THRESHOLD) {
            robot.setIntake(-1);
            intakeReason = "OUT (right trigger)";
        } else if (gamepad1.right_bumper) {
            robot.setIntake(1);
            intakeReason = "IN (right bumper)";
        } else if (INTAKE_HELPS_LAUNCH && launchStarted) {
            // Carries pollen up to the feeder. Stays on for the whole launch instead of flickering.
            robot.setIntake(1);
            intakeReason = "IN (launching)";
        } else {
            robot.setIntake(0);
            intakeReason = "off";
        }
    }

    private void pusherControls() {
        if (gamepad1.aWasPressed()) {
            robot.pusherDown();
        } else if (gamepad1.bWasPressed()) {
            robot.pusherUp();
        }
    }

    private void showTelemetry(boolean feedingShot) {
        telemetry.addData("Flywheel", "%.0f / %.0f ticks/s",
                robot.getFlywheelVelocity(), robot.getTargetVelocity());
        telemetry.addData("Feeding", feedingShot ? "YES" : "no");
        // If the intake moves while this says "off", the cause is wiring or configuration, not the code.
        telemetry.addData("Intake", "%s   power %.2f   trigger %.2f",
                intakeReason, robot.intake.getPower(), gamepad1.right_trigger);
        telemetry.update();
    }
}
