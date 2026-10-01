/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.controller;

import eu.midnightdust.midnightcontrols.MidnightControls;
import eu.midnightdust.midnightcontrols.client.MidnightControlsClient;
import eu.midnightdust.midnightcontrols.client.MidnightControlsConfig;
import eu.midnightdust.midnightcontrols.client.controller.backend.GamepadState;
import eu.midnightdust.midnightcontrols.client.controller.backend.InputBackend;
import eu.midnightdust.midnightcontrols.client.controller.backend.InputBackends;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import static eu.midnightdust.midnightcontrols.client.MidnightControlsClient.showToastMessage;
import static org.lwjgl.BufferUtils.createByteBuffer;

/**
 * Represents a controller.
 * <p>
 * The id is a stable slot in {@code 0..GamepadConstants.JOYSTICK_LAST} (GLFW joystick id semantics);
 * platform access goes through the {@link InputBackend}.
 *
 * @author LambdAurora
 * @version 1.13.0
 * @since 1.0.0
 */
public record Controller(int id) {
    private static final Map<Integer, Controller> CONTROLLERS = new HashMap<>();

    private static @NotNull InputBackend backend() {
        return InputBackends.get();
    }

    /**
     * Gets the controller's globally unique identifier.
     *
     * @return the controller's GUID
     */
    public String getGuid() {
        return backend().getGuid(this.id);
    }

    /**
     * Returns whether this controller is connected or not.
     *
     * @return true if this controller is connected, else false
     */
    public boolean isConnected() {
        return backend().isConnected(this.id);
    }

    /**
     * Returns whether this controller is a gamepad or not.
     *
     * @return true if this controller is a gamepad, else false
     */
    public boolean isGamepad() {
        return this.isConnected() && backend().isGamepad(this.id);
    }

    /**
     * Gets the name of the controller.
     *
     * @return the controller's name
     */
    public @NotNull String getName() {
        var name = backend().getName(this.id);
        return name == null ? String.valueOf(this.id()) : name;
    }

    /**
     * Gets the state of the controller.
     *
     * @return the state of the controller input
     */
    public GamepadState getState() {
        var state = new GamepadState();
        backend().readState(this.id, state);
        return state;
    }

    public static Controller byId(int id) {
        if (id > GamepadConstants.JOYSTICK_LAST) {
            MidnightControls.log("Controller '" + id + "' doesn't exist.");
            id = GamepadConstants.JOYSTICK_LAST;
        }
        Controller controller;
        if (CONTROLLERS.containsKey(id))
            return CONTROLLERS.get(id);
        else {
            controller = new Controller(id);
            CONTROLLERS.put(id, controller);
            return controller;
        }
    }

    public static Optional<Controller> byGuid(@NotNull String guid) {
        return CONTROLLERS.values().stream().filter(Controller::isConnected)
                .filter(controller -> controller.getGuid().equals(guid))
                .max(Comparator.comparingInt(Controller::id));
    }

    /**
     * Reads the specified resource and returns the raw data as a ByteBuffer.
     *
     * @param resource the resource to read
     * @return the resource data
     * @throws IOException If an IO error occurs.
     */
    private static ByteBuffer ioResourceToBuffer(String resource) throws IOException {
        ByteBuffer buffer = null;

        var path = Paths.get(resource);
        if (Files.isReadable(path)) {
            try (var fc = Files.newByteChannel(path)) {
                buffer = createByteBuffer((int) fc.size() + 2);
                while (fc.read(buffer) != -1) ;
                buffer.put((byte) 0);
            }
        }

        if (buffer != null) buffer.flip(); // Force Java 8 >.<
        return buffer;
    }

    /**
     * Updates the controller mappings.
     */
    public static void updateMappings() {
        CompletableFuture.supplyAsync(Controller::updateMappingsSync);
    }

    private static boolean updateMappingsSync() {
        try {
            MidnightControls.log("Updating controller mappings...");
            Optional<File> databaseFile = getDatabaseFile();
            if (databaseFile.isPresent()) {
                var database = ioResourceToBuffer(databaseFile.get().getPath());
                if (database != null && !backend().addMappings(database))
                    MidnightControls.warn("Failed to load the controller database.");
            }
            if (!MidnightControlsClient.MAPPINGS_FILE.exists())
                return false;
            var buffer = ioResourceToBuffer(MidnightControlsClient.MAPPINGS_FILE.getPath());
            if (buffer != null && !backend().addMappings(buffer))
                MidnightControls.warn("Failed to load the custom controller mappings.");
        } catch (IOException e) {
            e.fillInStackTrace();
        }

        try {
            var error = backend().pollError();
            if (error != null) {
                showToastMessage(Component.translatable("midnightcontrols.controller.mappings.error"), Component.literal(error));
                MidnightControls.log(I18n.get("midnightcontrols.controller.mappings.error") + error);
            }
        } catch (Throwable e) {
            /* Ignored :concern: */
        }

        if (MidnightControlsConfig.debug) {
            for (int i = GamepadConstants.JOYSTICK_1; i <= GamepadConstants.JOYSTICK_LAST; i++) {
                var controller = byId(i);

                if (!controller.isConnected())
                    continue;

                MidnightControls.log(String.format("Controller #%d name: \"%s\"\n GUID: %s\n Gamepad: %s",
                        controller.id,
                        controller.getName(),
                        controller.getGuid(),
                        controller.isGamepad()));
            }
        }
        return true;
    }

    private static Optional<File> getDatabaseFile() {
        File databaseFile = new File("config/gamecontrollerdatabase.txt");
        try {
            BufferedInputStream in = new BufferedInputStream(URI.create("https://raw.githubusercontent.com/gabomdq/SDL_GameControllerDB/master/gamecontrollerdb.txt").toURL().openStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(databaseFile));
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                out.write(dataBuffer, 0, bytesRead);
            }
            out.close();
        } catch (Exception e) {return Optional.empty();}
        return Optional.of(databaseFile);
    }
}
