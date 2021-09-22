/*
 *  Copyright (C) 2010-2021 JPEXS
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.lewon.dofus;

import com.jpexs.decompiler.flash.EventListener;
import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SWFSourceInfo;
import com.jpexs.decompiler.flash.configuration.Configuration;
import com.jpexs.decompiler.flash.exporters.modes.ScriptExportMode;
import com.jpexs.decompiler.flash.exporters.settings.ScriptExportSettings;
import com.jpexs.decompiler.flash.treeitems.SWFList;
import com.jpexs.helpers.CancellableWorker;
import com.jpexs.helpers.Helper;
import fr.lewon.dofus.export.builder.VldbExportPackTaskBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class VldbProtocolUpdater {

    private VldbProtocolUpdater() {
    }

    public static void updateManagers(List<VldbExportPackTaskBuilder> taskBuilders) throws IOException {
        List<String> gameLocCmdResult = execCmd("cmd", "/c", "where Dofus.exe");
        if (gameLocCmdResult.size() != 1) {
            throw new RuntimeException("Unable to find Dofus.exe, is it in your path?");
        }
        String gameLocStr = gameLocCmdResult.get(0);
        File gameDir = new File(gameLocStr).getParentFile();
        File swfFile = new File(gameDir, "DofusInvoker.swf");
        if (!swfFile.exists() || !swfFile.isFile()) {
            throw new RuntimeException("Unable to find Dofus.exe, is it in your path?");
        }
        updateManagers(swfFile, taskBuilders);
    }

    private static List<String> execCmd(String... command) throws IOException {
        List<String> lines = new ArrayList<>();
        Process process = new ProcessBuilder(command).start();
        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = input.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    public static void updateManagers(File swfFile, List<VldbExportPackTaskBuilder> taskBuilders) {
        if (!swfFile.exists()) {
            throw new RuntimeException("Input SWF file does not exist!");
        }

        try {
            long startTimeSwf = System.currentTimeMillis();
            System.out.println("Start exporting " + swfFile.getName());

            SWFSourceInfo sourceInfo = new SWFSourceInfo(null, swfFile.getAbsolutePath(), swfFile.getName());
            SWF swf = new SWF(new FileInputStream(swfFile), sourceInfo.getFile(), sourceInfo.getFileTitle(), Configuration.parallelSpeedUp.get());

            swf.swfList = new SWFList();
            swf.swfList.sourceInfo = sourceInfo;

            swf.addEventListener(new EventListener() {
                @Override
                public void handleExportingEvent(String type, int index, int count, Object data) {
                }

                @Override
                public void handleExportedEvent(String type, int index, int count, Object data) {
                    String text = "Exported ";
                    if (type != null && type.length() > 0) {
                        text += type + " ";
                    }
                    System.out.println(text + index + "/" + count + " " + data);
                }

                @Override
                public void handleEvent(String event, Object data) {
                }
            });

            ScriptExportSettings scriptExportSettings = new ScriptExportSettings(ScriptExportMode.AS);
            System.out.println("Exporting scripts...");

            swf.exportActionScript(scriptExportSettings, swf.getExportEventListener(), taskBuilders);

            long stopTimeSwf = System.currentTimeMillis();
            long time = stopTimeSwf - startTimeSwf;
            System.out.println("Export finished: " + swfFile.getName() + " Export time: " + Helper.formatTimeSec(time));

            swf.clearAllCache();
            CancellableWorker.cancelBackgroundThreads();
        } catch (OutOfMemoryError |
                Exception ex) {
            System.err.print("FAIL: Exporting Failed on Exception - ");
            CancellableWorker.cancelBackgroundThreads();
        }
    }

}