package ch.epfl.gameboj.gui;

import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import javafx.scene.layout.BorderPane;
import javafx.animation.AnimationTimer;

public final class Main extends Application {

    private static final HashMap<String, Joypad.Key> mapLetters = new HashMap<>(
            4);
    private static final HashMap<KeyCode, Joypad.Key> mapArrows = new HashMap<>(
            4);

    private static GameBoy gb;

    public static void main(String[] args) {
        Application.launch(args);
    }

    public void start(Stage stage) throws Exception {

        //
        // List<String> cmdArgs = getParameters().getRaw();
        // Preconditions.checkArgument(cmdArgs.size() <= 2, () ->
        // System.exit(1));
        //
        // String fileName = cmdArgs.get(0);
        // String saveFileName = cmdArgs.size() == 2 ? cmdArgs.get(1) : null;
        //
        // gameboj = saveFileName == null ? new GameBoy(Cartridge.ofFile(new
        // File(fileName)))
        // : new GameBoy(Cartridge.ofFile(new File(fileName)), saveFileName); //
        // FIXME

        if (getParameters().getRaw().size() != 1) {
            System.exit(1);
        }

        mapLetters.put("a", Key.A);
        mapLetters.put("b", Key.B);
        mapLetters.put("s", Key.START);
        mapLetters.put(" ", Key.SELECT);

        mapArrows.put(KeyCode.UP, Key.UP);
        mapArrows.put(KeyCode.DOWN, Key.DOWN);
        mapArrows.put(KeyCode.RIGHT, Key.RIGHT);
        mapArrows.put(KeyCode.LEFT, Key.LEFT);

        String gameName = getParameters().getRaw().get(0);
        File romFile = new File(gameName);
        gb = new GameBoy(Cartridge.ofFile(romFile));
        Joypad joypad = gb.getJoyPad();

        MenuBar bar = new MenuBar();
        Menu menu = new Menu("Options");
        Menu speedMenu = new Menu("Speed");
        Menu colorMenu = new Menu("Colors");
        MenuItem screenshot = new MenuItem("Screenshot");
        screenshot.setOnAction(e -> {
            LcdImage li = gb.getLcdController().currentImage();
            Date date = new Date();
            BufferedImage image = new BufferedImage(li.getWidth(),
                    li.getHeight(), BufferedImage.TYPE_INT_RGB);
            try {
                ImageIO.write(
                        SwingFXUtils.fromFXImage(ImageConverter.convert(li),
                                image),
                        "png",
                        new File(Long.toString(date.getTime()) + ".png"));
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });

        /// SPEED

        ImageView imageView = new ImageView();
        imageView.setImage(
                ImageConverter.convert(gb.getLcdController().currentImage()));
        BorderPane border = new BorderPane();
        border.setTop(bar);
        border.setCenter(imageView);
        Scene scene = new Scene(border);

        imageView.fitWidthProperty().bind(scene.widthProperty());
        imageView.fitHeightProperty().bind(scene.heightProperty());

        stage.setWidth(2 * LcdController.LCD_WIDTH);
        stage.setHeight(2 * LcdController.LCD_HEIGHT);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.minWidthProperty().bind(scene.heightProperty());
        stage.minHeightProperty().bind(scene.widthProperty());
        stage.setTitle("gameboj");
        stage.show();

        scene.setOnKeyPressed(e -> {
            Joypad.Key key = mapArrows.get(e.getCode());
            Joypad.Key keyLetter = mapLetters.get(e.getText());
            if (key != null) {
                joypad.keyPressed(key);
            } else {
                if (mapLetters.get(e.getText()) != null) {
                    joypad.keyPressed(keyLetter);
                } else {
                }

            }

        });

        scene.setOnKeyReleased(e -> {
            Joypad.Key key = mapArrows.get(e.getCode());
            Joypad.Key keyLetter = mapLetters.get(e.getText());
            if (key != null) {
                joypad.keyReleased(key);
            } else {
                if (mapLetters.get(e.getText()) != null) {
                    joypad.keyReleased(keyLetter);
                } else {

                }

            }
        });

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gb.runUntil(17556 + gb.cycles());
                imageView.setImage(ImageConverter
                        .convert(gb.getLcdController().currentImage()));

            }
        };
        timer.start();

    }

}