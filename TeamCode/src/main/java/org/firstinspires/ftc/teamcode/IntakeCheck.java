package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

/**
 * Finds out why the intake "ghosts". Nothing runs unless you hold a button, so if the intake
 * moves while this says "commanded 0", the cause is wiring or configuration, not the code.
 *
 *   A (hold)      intake in, slowly
 *   B (hold)      intake out, slowly
 *   X             switch the intake between BRAKE and FLOAT at zero power
 *
 * If the intake's encoder cable is plugged into the Expansion Hub, "moved" counts how far it
 * turned by itself while it was told to stop.
 */
@TeleOp(name = "Intake Check", group = "Test")
public class IntakeCheck extends LinearOpMode {

    static final double TEST_POWER = 0.3;

    @Override
    public void runOpMode() {
        Robot robot = new Robot(hardwareMap);
        DcMotorEx intake = robot.intake;

        telemetry.addLine("Intake Check: nothing moves until you hold A or B.");
        telemetry.addData("intake is", "%s", intake.getConnectionInfo());
        telemetry.addData("flywheel is", "%s", robot.flywheel.getConnectionInfo());
        telemetry.addLine("The intake must say port 1 and the flywheel port 0, both on the Expansion Hub.");
        telemetry.update();
        waitForStart();
        if (isStopRequested()) return;

        robot.stopAll();
        boolean brake = true;
        int stillSince = intake.getCurrentPosition();
        int maxDrift = 0;

        while (opModeIsActive()) {
            if (gamepad1.xWasPressed()) {
                brake = !brake;
                intake.setZeroPowerBehavior(brake ? DcMotor.ZeroPowerBehavior.BRAKE : DcMotor.ZeroPowerBehavior.FLOAT);
            }

            double command = gamepad1.a ? TEST_POWER : gamepad1.b ? -TEST_POWER : 0;
            intake.setPower(command);

            // Everything else stays off, so nothing else can be mistaken for the intake.
            robot.drive(0, 0, 0);
            robot.stopFlywheel();
            robot.setFeeder(0);

            int position = intake.getCurrentPosition();
            if (command != 0) {
                stillSince = position;
            } else {
                maxDrift = Math.max(maxDrift, Math.abs(position - stillSince));
            }

            telemetry.addData("intake commanded", "%.2f", command);
            telemetry.addData("intake power read back", "%.2f", intake.getPower());
            telemetry.addData("intake speed", "%.0f ticks/s", intake.getVelocity());
            telemetry.addData("moved while told to stop", "%d ticks", maxDrift);
            telemetry.addData("at zero power", brake ? "BRAKE (X to change)" : "FLOAT (X to change)");
            telemetry.addData("intake is", "%s", intake.getConnectionInfo());
            telemetry.update();
        }

        robot.stopAll();
    }
}
