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
    static final double TRIGGER_THRESHOLD = 0.2;

    private Robot robot;

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
        if (gamepad1.right_trigger > TRIGGER_THRESHOLD) {
            robot.setIntake(-1);
        } else if (gamepad1.right_bumper || feedingShot) {
            // The intake also carries pollen up to the feeder, so run it while launching.
            robot.setIntake(1);
        } else {
            robot.setIntake(0);
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
        telemetry.update();
    }
}
