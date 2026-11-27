package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Leaves the wall and launches the 4 pre-loaded POLLEN. Works for both alliances.
 *
 * Setup: intake side against the perimeter wall, launcher lined up with the HIVE.
 * During INIT, dpad up / down sets a start delay. Use one if your partner should launch first
 * and tip the HIVE so that your CELL is the upward-facing one.
 */
@Autonomous(name = "BIOBUZZ Auto", group = "BIOBUZZ", preselectTeleOp = "BIOBUZZ TeleOp")
public class BioBuzzAuto extends LinearOpMode {

    // Driving is timed, so check these on the field with a full battery.
    static final double LEAVE_POWER = -0.25;   // negative = away from the intake side
    static final double LEAVE_SECONDS = 0.7;

    static final double LAUNCH_SECONDS = 15;
    static final double MAX_DELAY_SECONDS = 12;
    static final double AUTO_END_SECONDS = 29; // stop a little before the 30 s buzzer

    @Override
    public void runOpMode() {
        Robot robot = new Robot(hardwareMap);
        double delay = 0;

        while (opModeInInit()) {
            if (gamepad1.dpadUpWasPressed()) delay = Math.min(MAX_DELAY_SECONDS, delay + 1);
            if (gamepad1.dpadDownWasPressed()) delay = Math.max(0, delay - 1);
            telemetry.addData("Start delay", "%.0f s  (dpad up / down)", delay);
            telemetry.addLine("Intake against the wall, 4 pollen loaded.");
            telemetry.update();
        }
        if (isStopRequested()) return;

        ElapsedTime autoTime = new ElapsedTime();
        robot.pusherUp();

        // 1. Optional wait for the alliance partner.
        while (opModeIsActive() && autoTime.seconds() < delay) {
            telemetry.addData("Waiting", "%.1f s", delay - autoTime.seconds());
            telemetry.update();
        }

        // 2. Leave the wall, spinning the flywheel up on the way.
        ElapsedTime stepTime = new ElapsedTime();
        while (opModeIsActive() && stepTime.seconds() < LEAVE_SECONDS) {
            robot.drive(LEAVE_POWER, 0, 0);
            robot.spinUp();
        }
        robot.drive(0, 0, 0);

        // 3. Launch. Feed only while the flywheel is at speed; once the first shot goes,
        //    the intake keeps running instead of starting and stopping with every dip.
        stepTime.reset();
        boolean launchStarted = false;
        while (opModeIsActive()
                && stepTime.seconds() < LAUNCH_SECONDS
                && autoTime.seconds() < AUTO_END_SECONDS) {
            boolean ready = robot.spinUpAndCheckReady();
            launchStarted |= ready;
            robot.setFeeder(ready ? 1 : 0);
            robot.setIntake(launchStarted ? 1 : 0);

            telemetry.addData("Flywheel", "%.0f / %.0f ticks/s",
                    robot.getFlywheelVelocity(), robot.getTargetVelocity());
            telemetry.addData("Feeding", ready ? "YES" : "no");
            telemetry.update();
        }

        // 4. Nothing may keep moving into the transition period.
        robot.stopAll();
    }
}
