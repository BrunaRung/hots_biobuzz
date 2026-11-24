package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Run this FIRST, with the wheels off the ground. Each button runs one device, so you can check
 * both the port wiring and the direction. If one goes the wrong way, flip its direction in Robot.
 */
@TeleOp(name = "Motor Test", group = "Test")
public class MotorTest extends LinearOpMode {

    static final double POWER = 0.3;

    @Override
    public void runOpMode() {
        Robot robot = new Robot(hardwareMap);

        telemetry.addLine("Put the robot on blocks, then press START.");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            robot.leftFront.setPower(gamepad1.dpad_up ? POWER : 0);
            robot.rightFront.setPower(gamepad1.dpad_right ? POWER : 0);
            robot.leftBack.setPower(gamepad1.dpad_left ? POWER : 0);
            robot.rightBack.setPower(gamepad1.dpad_down ? POWER : 0);
            robot.intake.setPower(gamepad1.right_bumper ? 1 : 0);
            robot.flywheel.setPower(gamepad1.left_bumper ? POWER : 0);
            robot.feeder.setPower(gamepad1.y ? 1 : 0);
            if (gamepad1.aWasPressed()) robot.pusherDown();
            if (gamepad1.bWasPressed()) robot.pusherUp();

            telemetry.addLine("Wheels must roll toward the INTAKE side:");
            telemetry.addData("dpad up", "leftFront   (Control 3)  %d",
                    robot.leftFront.getCurrentPosition());
            telemetry.addData("dpad right", "rightFront  (Control 2)  %d",
                    robot.rightFront.getCurrentPosition());
            telemetry.addData("dpad left", "leftBack    (Control 1)  %d",
                    robot.leftBack.getCurrentPosition());
            telemetry.addData("dpad down", "rightBack   (Control 0)  %d",
                    robot.rightBack.getCurrentPosition());
            telemetry.addData("left bumper", "flywheel launches  (Expansion 0)  %.0f ticks/s",
                    robot.getFlywheelVelocity());
            telemetry.addData("right bumper", "intake pulls in  (Expansion 1)");
            telemetry.addData("Y", "feeder toward flywheel  (servo 0)");
            telemetry.addData("A / B", "pusher down / up  (servo 1)");
            telemetry.addLine("Flywheel ticks/s must be POSITIVE while launching.");
            telemetry.update();
        }

        robot.stopAll();
    }
}
