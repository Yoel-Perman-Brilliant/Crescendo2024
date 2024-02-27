package frc.robot.commands;

import com.spikes2212.command.genericsubsystem.commands.MoveGenericSubsystem;
import com.spikes2212.command.genericsubsystem.commands.smartmotorcontrollergenericsubsystem.MoveSmartMotorControllerGenericSubsystem;
import com.spikes2212.dashboard.RootNamespace;
import com.spikes2212.dashboard.SpikesLogger;
import com.spikes2212.util.UnifiedControlMode;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj2.command.*;
import frc.robot.Robot;
import frc.robot.subsystems.Drivetrain;
import frc.robot.subsystems.Shooter;
import frc.robot.subsystems.ShooterAdjuster;
import frc.robot.subsystems.Storage;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;


public class Shoot extends ParallelDeadlineGroup {

    private static final Function<Double, Double> DISTANCE_TO_HEIGHT = x -> -3.8 * x + 28.8;
    private static final double MAX_FORMULA_DISTANCE = 2.285;
    private static final double MIN_HEIGHT = 17;

    private static final double STORAGE_SPEED = -0.5;
    private static final double WAIT_TIME = 3;

    public static final RootNamespace ROOT = new RootNamespace("shoot");
    private static final Supplier<Double> LEFT_SPEED = ROOT.addConstantDouble("shoot left speed", 0);
    private static final Supplier<Double> RIGHT_SPEED = ROOT.addConstantDouble("shoot right speed", 0);
    private static final Supplier<Double> HEIGHT = ROOT.addConstantDouble("shoot height", 0);

    //@TODO CHANGE!
    private static final Pose2d SPEAKER_POSE = new Pose2d(0, 0, new Rotation2d());

    private static final SpikesLogger LOGGER = new SpikesLogger();

    private Pose2d speakerPose = new Pose2d(0.52, 5.59, new Rotation2d());

    public Shoot(Shooter shooter, Drivetrain drivetrain, ShooterAdjuster adjuster, Storage storage) {

        super(new InstantCommand());

//        Supplier<Pose2d> pose = drivetrain::getPose;

        RotateSwerveWithPID rotateCommand = new RotateSwerveWithPID(drivetrain, getRequiredShooterAngle(Pose2d::new),
                drivetrain::getAngle, drivetrain.rotateToTargetPIDSettings,
                drivetrain.rotateToTargetFeedForwardSettings) {
            @Override
            public boolean isFinished() {
                return true;
            }
        };

        SpeedUpShooter speedUpCommand = new SpeedUpShooter(shooter, getRequiredLeftSpeed(), getRequiredRightSpeed());

        MoveSmartMotorControllerGenericSubsystem adjustCommand =
                new MoveSmartMotorControllerGenericSubsystem(adjuster, adjuster.getPIDSettings(),
                        adjuster.getFeedForwardSettings(), UnifiedControlMode.POSITION, getRequiredShooterAngle(Pose2d::new)) {

                    @Override
                    public boolean isFinished() {
                        return super.isFinished() || !adjuster.wasReset();
                    }
                };

        InstantCommand setSpeakerPoseCommand = new InstantCommand(() -> {
            Optional<DriverStation.Alliance> alliance = DriverStation.getAlliance();
            if (alliance.isEmpty() || alliance.get() == DriverStation.Alliance.Blue) {
                speakerPose = new Pose2d(0.52, 5.59, new Rotation2d());
            } else {
                speakerPose = new Pose2d(16.07, 5.59, new Rotation2d());
            }
        }
        );

        ROOT.putBoolean("rotate finished", rotateCommand::isFinished);
        ROOT.putBoolean("adjust finished", adjustCommand::isFinished);
        ROOT.putBoolean("left on target", () -> shooter.getLeftFlywheel().onTarget(UnifiedControlMode.VELOCITY,
                shooter.getLeftFlywheel().pidSettings.getTolerance(), getRequiredLeftSpeed().get()));
        ROOT.putBoolean("right on target", () -> shooter.getRightFlywheel().onTarget(UnifiedControlMode.VELOCITY,
                shooter.getRightFlywheel().pidSettings.getTolerance(), getRequiredRightSpeed().get()));

        addCommands(setSpeakerPoseCommand, speedUpCommand, adjustCommand);
        setDeadline(
                new SequentialCommandGroup(
                        new WaitUntilCommand(() ->
                                rotateCommand.isFinished() && adjustCommand.isFinished() &&
                                        shooter.getLeftFlywheel().onTarget(UnifiedControlMode.VELOCITY,
                                                shooter.getLeftFlywheel().pidSettings.getTolerance(), getRequiredLeftSpeed().get()) &&
                                        shooter.getRightFlywheel().onTarget(UnifiedControlMode.VELOCITY,
                                                shooter.getRightFlywheel().pidSettings.getTolerance(), getRequiredRightSpeed().get())),
                        LOGGER.logCommand("got here"),
                        new MoveGenericSubsystem(storage, STORAGE_SPEED).withTimeout(WAIT_TIME),
                        new InstantCommand(shooter::stop)
                ));
    }

    public Supplier<Double> getRequiredShooterAngle(Supplier<Pose2d> robotPose) {
//        double rx = robotPose.get().getX();
//        double ry = robotPose.get().getY();
//        double sx = speakerPose.getX();
//        double sy = speakerPose.getY();
//        double distance = Math.sqrt((rx - sx) * (rx - sx) + (ry - sy) * (ry - sy));
//        ROOT.putNumber("distance", distance);
//        return distance <= MAX_FORMULA_DISTANCE ? () -> DISTANCE_TO_HEIGHT.apply(distance) : () -> MIN_HEIGHT;
        return () -> 24.8;
    }

    public double getRequiredRobotAngle(Pose2d robotPose) {
        return Math.atan2(SPEAKER_POSE.minus(robotPose).getX(), SPEAKER_POSE.minus(robotPose).getY());
    }

    public Supplier<Double> getRequiredLeftSpeed() {
//        return LEFT_SPEED;
        return () -> 3700.0;
    }

    public Supplier<Double> getRequiredRightSpeed() {
//        return RIGHT_SPEED;
        return () -> 3700.0;
    }
}
