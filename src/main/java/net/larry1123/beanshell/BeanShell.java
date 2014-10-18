//Copyright (C) 2011  Ryan Michela
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

/*
 * Copyright 2014 ElecEntertainment
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.larry1123.beanshell;

import bsh.Interpreter;
import net.canarymod.Canary;
import net.canarymod.commandsys.CommandDependencyException;
import net.canarymod.plugin.Plugin;
import net.larry1123.util.CanaryUtil;
import net.larry1123.util.api.plugin.UtilPlugin;
import net.larry1123.util.api.plugin.commands.Command;

import java.io.File;
import java.util.logging.Logger;

public class BeanShell extends UtilPlugin {

    public Interpreter bsh;
    public Logger log;
    public File dataFolder = new File("pluginData/beanShell/");

    protected BshCommand bshCommand;
    protected BeanShellConfig config;

    @Override
    public boolean enable() {
        config = new BeanShellConfig(getConfig());
        log = getLogger();
        log.info("Starting BeanShell");

        // Initialize the data folder
        if (!dataFolder.getParentFile().exists()) {
            dataFolder.getParentFile().mkdir();
        }
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        bsh = new Interpreter();
        try {
            // Set up debug environment with globals
            bsh.set("server", Canary.getServer());
            bsh.set("bans", Canary.bans());
            bsh.set("channels", Canary.channels());
            bsh.set("commands", Canary.commands());
            bsh.set("userAndGroup", Canary.usersAndGroups());
            bsh.set("warps", Canary.warps());
            bsh.set("kits", Canary.kits());
            bsh.set("whiteList", Canary.whitelist());
            bsh.set("ops", Canary.ops());
            bsh.set("reserveList", Canary.reservelist());
            bsh.set("hooks", Canary.hooks());
            bsh.set("db", Canary.db());
            bsh.set("pluginManager", Canary.manager());
            bsh.set("permissions", Canary.permissionManager());
            bsh.set("help", Canary.help());
            bsh.set("factory", Canary.factory());
            bsh.set("scoreboards", Canary.scoreboards());
            bsh.set("motd", Canary.motd());
            bsh.set("playerSelector", Canary.playerSelector());
            bsh.set("logger", getLogger());
            bsh.set("classLoader", getClass().getClassLoader());

            // Create an alias for each plugin name using its class name
            for (Plugin p : Canary.manager().getPlugins()) {
                String[] cn = p.getClass().getName().split("\\.");
                log.info("Regisering object " + cn[cn.length - 1]);
                bsh.set(cn[cn.length - 1], p);
            }

            // Source any .bsh files in the plugin directory
            if (dataFolder.listFiles() != null) {
                for (File file : dataFolder.listFiles()) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".bsh")) {
                        log.info("Sourcing file " + fileName);
                        bsh.source(file.getPath());
                    }
                    else {
                        log.info("*** skipping " + file.getAbsolutePath());
                    }
                }
            }

            bsh.eval("setAccessibility(true)"); // turn off access restrictions

            if (config.getServerPort() != 0) {
                bsh.eval("server(" + config.getServerPort() +  ")");
                log.info("BeanShell web console at http://localhost:" + config.getServerPort());
                log.info("BeanShell telnet console at localhost:" + (config.getServerPort() + 1));
            }

            // Register the bsh command
            bshCommand = new BshCommand(bsh);
            if (!regCommand(bshCommand)) {
                String message = "Unable to register Command!";
                log.severe(message);
                enableFailed(message);
            }

        }
        catch (Exception e) {
            log.severe("Error in BeanShell. " + e.toString());
            enableFailed(e.toString());
            return false;
        }
        return true;
    }

    @Override
    public void disable() {
        // TODO likely something to do here
    }

    protected boolean regCommand(Command command) {
        try {
            CanaryUtil.commands().registerCommand(command, this);
            command.setLoaded(true);
            return true;
        }
        catch (CommandDependencyException e) {
            getLogger().logCustom("Commands", "Failed to add command: " + command.getCommandData().getAliases()[0], e);
            command.setLoaded(false);
            return false;
        }
    }

}